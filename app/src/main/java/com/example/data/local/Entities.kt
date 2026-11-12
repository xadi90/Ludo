package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey val id: Int = 1,
    val playerUid: String = "LUDO-LOCAL-001",
    val nickname: String = "Champion",
    val avatarId: Int = 0,
    val totalGames: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val tokensCaptured: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val coins: Int = 500,
    val gems: Int = 25,
    val xp: Int = 0,
    val unlockedAchievementsCsv: String = "FIRST_STEP"
)

@Entity(tableName = "match_history")
data class MatchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,
    val boardType: String,
    val winnerName: String,
    val winnerColor: String,
    val playerCount: Int,
    val durationSeconds: Int,
    val playerWon: Boolean,
    val tokensCut: Int,
    val timestampMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val musicVolume: Float = 0.45f,
    val sfxVolume: Float = 0.85f,
    val languageCode: String = "en",
    val darkThemeMode: Boolean = true,
    val spawnOnOneOrSix: Boolean = false,
    val extraTurnOnCut: Boolean = true,
    val threeSixesForfeit: Boolean = true,
    val safeStarsEnabled: Boolean = true,
    val allowUndo: Boolean = true,
    val stackProtection: Boolean = false
)

@Entity(tableName = "saved_match")
data class SavedMatchEntity(
    @PrimaryKey val id: Int = 1,
    val serializedJson: String,
    val gameMode: String,
    val boardType: String,
    val turnCounter: Int,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_reward")
data class DailyRewardEntity(
    @PrimaryKey val id: Int = 1,
    val lastClaimDayEpoch: Long = -1L,
    val streakDay: Int = 0
)

@Entity(tableName = "tournament_progress")
data class TournamentEntity(
    @PrimaryKey val id: Int = 1,
    val currentRound: Int = 1, // 1 = Quarterfinal, 2 = Semifinal, 3 = Grand Final
    val difficulty: String = "MEDIUM",
    val playerColor: String = "RED",
    val championshipsWon: Int = 0,
    val isActive: Boolean = false,
    val updatedAtMs: Long = System.currentTimeMillis()
)
