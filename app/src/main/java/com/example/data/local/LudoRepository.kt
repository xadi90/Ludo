package com.example.data.local

import com.example.game.AiDifficulty
import com.example.game.GameState
import com.example.game.PlayerColor
import com.example.security.AppSecurityManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class LudoRepository(private val dao: LudoDao) {

    val profileFlow: Flow<PlayerProfileEntity> = dao.observeProfile().map {
        it ?: PlayerProfileEntity()
    }

    val settingsFlow: Flow<AppSettingsEntity> = dao.observeSettings().map {
        it ?: AppSettingsEntity()
    }

    val matchHistoryFlow: Flow<List<MatchHistoryEntity>> = dao.observeMatchHistory()

    val savedMatchFlow: Flow<SavedMatchEntity?> = dao.observeSavedMatch()

    val dailyRewardFlow: Flow<DailyRewardEntity> = dao.observeDailyReward().map {
        it ?: DailyRewardEntity()
    }

    val tournamentFlow: Flow<TournamentEntity> = dao.observeTournament().map {
        it ?: TournamentEntity()
    }

    suspend fun ensureInitialized() {
        if (dao.getProfileOnce() == null) {
            val shortId = UUID.randomUUID().toString().take(6).uppercase()
            dao.upsertProfile(PlayerProfileEntity(playerUid = "LUDO-$shortId"))
        }
        if (dao.getSettingsOnce() == null) {
            dao.upsertSettings(AppSettingsEntity())
        }
        if (dao.getDailyRewardOnce() == null) {
            dao.upsertDailyReward(DailyRewardEntity())
        }
        if (dao.getTournamentOnce() == null) {
            dao.upsertTournament(TournamentEntity())
        }
    }

    suspend fun updateProfileIdentity(nickname: String, avatarId: Int) {
        val current = dao.getProfileOnce() ?: PlayerProfileEntity()
        val safeName = AppSecurityManager.sanitizeNickname(nickname, current.nickname)
        dao.upsertProfile(
            current.copy(
                nickname = safeName,
                avatarId = avatarId.coerceIn(0, 7)
            )
        )
    }

    suspend fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        val current = dao.getSettingsOnce() ?: AppSettingsEntity()
        dao.upsertSettings(transform(current))
    }

    suspend fun saveActiveMatch(state: GameState) {
        if (state.phase == com.example.game.TurnPhase.MATCH_OVER) {
            dao.clearSavedMatch()
            return
        }
        val json = GameStateSerializer.toJson(state)
        dao.upsertSavedMatch(
            SavedMatchEntity(
                id = 1,
                serializedJson = json,
                gameMode = state.gameMode.name,
                boardType = state.boardType.name,
                turnCounter = state.turnCounter,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun loadSavedMatchState(): GameState? {
        val entity = dao.getSavedMatchOnce() ?: return null
        return GameStateSerializer.fromJson(entity.serializedJson)
    }

    suspend fun clearSavedMatch() {
        dao.clearSavedMatch()
    }

    suspend fun recordMatchCompleted(
        state: GameState,
        humanPlayerWon: Boolean,
        humanCuts: Int
    ) {
        val durationSec = ((System.currentTimeMillis() - state.startTimeMs) / 1000L).toInt().coerceAtLeast(1)
        val winnerNames = state.winnerColors.joinToString(" & ") { c ->
            state.players.firstOrNull { it.config.color == c }?.config?.name ?: c.displayName
        }
        dao.insertMatchHistory(
            MatchHistoryEntity(
                mode = state.gameMode.name,
                boardType = state.boardType.name,
                winnerName = winnerNames.ifEmpty { "Winner" },
                winnerColor = state.winnerColors.firstOrNull()?.name ?: PlayerColor.RED.name,
                playerCount = state.players.size,
                durationSeconds = durationSec,
                playerWon = humanPlayerWon,
                tokensCut = humanCuts
            )
        )

        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        val newWins = profile.wins + if (humanPlayerWon) 1 else 0
        val newLosses = profile.losses + if (!humanPlayerWon) 1 else 0
        val newStreak = if (humanPlayerWon) profile.currentStreak + 1 else 0
        val newBest = maxOf(profile.bestStreak, newStreak)
        val newCuts = profile.tokensCaptured + humanCuts
        val coinReward = if (humanPlayerWon) 150 + humanCuts * 20 else 30 + humanCuts * 10
        val xpReward = if (humanPlayerWon) 120 else 40

        val achievements = profile.unlockedAchievementsCsv.split(",").filter { it.isNotBlank() }.toMutableSet()
        achievements.add("FIRST_MATCH")
        if (newWins >= 1) achievements.add("FIRST_WIN")
        if (newWins >= 5) achievements.add("VETERAN_5")
        if (newCuts >= 10) achievements.add("BLADE_MASTER")
        if (newBest >= 3) achievements.add("STREAK_3")
        if (state.boardType == com.example.game.BoardType.SIX_PLAYER && humanPlayerWon) {
            achievements.add("SIX_PLAYER_KING")
        }

        dao.upsertProfile(
            profile.copy(
                totalGames = profile.totalGames + 1,
                wins = newWins,
                losses = newLosses,
                tokensCaptured = newCuts,
                currentStreak = newStreak,
                bestStreak = newBest,
                coins = profile.coins + coinReward,
                xp = profile.xp + xpReward,
                unlockedAchievementsCsv = achievements.joinToString(",")
            )
        )
        dao.clearSavedMatch()
    }

    suspend fun claimDailyReward(todayEpochDay: Long): Int {
        val current = dao.getDailyRewardOnce() ?: DailyRewardEntity()
        if (current.lastClaimDayEpoch == todayEpochDay) return 0

        val nextStreak = if (current.lastClaimDayEpoch == todayEpochDay - 1) {
            (current.streakDay % 7) + 1
        } else {
            1
        }
        val coinsReward = when (nextStreak) {
            1 -> 100
            2 -> 150
            3 -> 250
            4 -> 350
            5 -> 500
            6 -> 750
            else -> 1200
        }
        val gemsReward = if (nextStreak == 7) 15 else 2

        dao.upsertDailyReward(
            DailyRewardEntity(
                id = 1,
                lastClaimDayEpoch = todayEpochDay,
                streakDay = nextStreak
            )
        )

        val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
        dao.upsertProfile(
            profile.copy(
                coins = profile.coins + coinsReward,
                gems = profile.gems + gemsReward
            )
        )
        return coinsReward
    }

    suspend fun startNewTournament(difficulty: AiDifficulty, color: PlayerColor) {
        dao.upsertTournament(
            TournamentEntity(
                id = 1,
                currentRound = 1,
                difficulty = difficulty.name,
                playerColor = color.name,
                championshipsWon = dao.getTournamentOnce()?.championshipsWon ?: 0,
                isActive = true,
                updatedAtMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun advanceTournamentRound(wonRound: Boolean) {
        val current = dao.getTournamentOnce() ?: TournamentEntity()
        if (!wonRound) {
            dao.upsertTournament(current.copy(isActive = false, currentRound = 1))
            return
        }
        if (current.currentRound >= 3) {
            val profile = dao.getProfileOnce() ?: PlayerProfileEntity()
            dao.upsertProfile(profile.copy(coins = profile.coins + 1000, gems = profile.gems + 20))
            dao.upsertTournament(
                current.copy(
                    isActive = false,
                    currentRound = 1,
                    championshipsWon = current.championshipsWon + 1
                )
            )
        } else {
            dao.upsertTournament(
                current.copy(
                    currentRound = current.currentRound + 1,
                    isActive = true
                )
            )
        }
    }
}
