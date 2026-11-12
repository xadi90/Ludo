package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoBlueDark
import com.example.ui.theme.LudoBlueLight
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoGreenDark
import com.example.ui.theme.LudoGreenLight
import com.example.ui.theme.LudoOrange
import com.example.ui.theme.LudoOrangeDark
import com.example.ui.theme.LudoOrangeLight
import com.example.ui.theme.LudoPurple
import com.example.ui.theme.LudoPurpleDark
import com.example.ui.theme.LudoPurpleLight
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoRedDark
import com.example.ui.theme.LudoRedLight
import com.example.ui.theme.LudoYellow
import com.example.ui.theme.LudoYellowDark
import com.example.ui.theme.LudoYellowLight

enum class PlayerColor(
    val displayName: String,
    val primary: Color,
    val dark: Color,
    val light: Color
) {
    RED("Red", LudoRed, LudoRedDark, LudoRedLight),
    GREEN("Green", LudoGreen, LudoGreenDark, LudoGreenLight),
    YELLOW("Yellow", LudoYellow, LudoYellowDark, LudoYellowLight),
    BLUE("Blue", LudoBlue, LudoBlueDark, LudoBlueLight),
    PURPLE("Purple", LudoPurple, LudoPurpleDark, LudoPurpleLight),
    ORANGE("Orange", LudoOrange, LudoOrangeDark, LudoOrangeLight);

    companion object {
        val fourPlayerOrder = listOf(RED, GREEN, YELLOW, BLUE)
        val sixPlayerOrder = listOf(RED, GREEN, YELLOW, BLUE, PURPLE, ORANGE)
    }
}

enum class BoardType(
    val trackLength: Int,
    val homeFinishProgress: Int
) {
    FOUR_PLAYER(trackLength = 52, homeFinishProgress = 56),
    SIX_PLAYER(trackLength = 72, homeFinishProgress = 76)
}

enum class GameMode {
    QUICK_PLAY,
    OFFLINE_CLASSIC,
    COMPUTER_VS_AI,
    TEAM_BATTLE,
    POWER_MODE,
    TOURNAMENT
}

enum class AiDifficulty(val label: String, val thinkDelayMs: Long) {
    EASY("Easy", 550L),
    MEDIUM("Medium", 500L),
    HARD("Hard", 450L),
    EXPERT("Expert", 420L)
}

enum class PowerType(
    val title: String,
    val shortCode: String,
    val description: String,
    val cooldownTurns: Int
) {
    ROCKET("Rocket", "RKT", "Propels a token +5 tiles forward.", 3),
    FIRE("Fire", "FIR", "Scorches an opponent within 4 tiles ahead back to base.", 3),
    SHIELD("Shield", "SHD", "Protects a token from capture for 2 full rounds.", 3),
    DOUBLE_X2("Double X2", "2X", "Doubles your next dice movement distance.", 2),
    TRIPLE_X3("Triple X3", "3X", "Triples your next dice movement distance.", 4),
    TNT("TNT Blast", "TNT", "Blasts non-shielded opponents within 2 tiles of your landing square.", 4),
    ICE_TILE("Ice Freeze", "ICE", "Freezes the next opponent so they skip 1 turn.", 3),
    TELEPORT("Teleport", "TLP", "Warps a token to the next safe Star tile ahead.", 4),
    LUCKY_STAR("Lucky Star", "STR", "Awards +150 bonus points and an instant extra turn.", 3),
    SPEED_BOOST("Speed Boost", "SPD", "Grants +3 extra steps on your active move.", 2)
}

data class RuleConfig(
    val spawnOnOneOrSix: Boolean = false,
    val extraTurnOnSix: Boolean = true,
    val extraTurnOnCapture: Boolean = true,
    val threeSixesForfeit: Boolean = true,
    val safeStarsEnabled: Boolean = true,
    val allowUndoInOffline: Boolean = true,
    val stackProtection: Boolean = false
)

data class PlayerConfig(
    val color: PlayerColor,
    val name: String,
    val avatarId: Int = 0,
    val isAi: Boolean = false,
    val aiDifficulty: AiDifficulty = AiDifficulty.MEDIUM,
    val teamId: Int = 0 // 0 = solo, 1/2/3 = team IDs
)

data class TokenState(
    val ownerColor: PlayerColor,
    val tokenIndex: Int,
    /**
     * -1 = in home base
     * 0..(trackLength-2) = on main shared track (relative steps from owner's start square)
     * (trackLength-1)..(homeFinishProgress-1) = inside private home column
     * homeFinishProgress = safely finished in center trophy area
     */
    val progress: Int = -1,
    val shieldTurns: Int = 0
) {
    val id: String get() = "${ownerColor.name}_$tokenIndex"
    fun isInBase(): Boolean = progress == -1
    fun isFinished(boardType: BoardType): Boolean = progress >= boardType.homeFinishProgress
    fun isOnMainTrack(boardType: BoardType): Boolean =
        progress in 0..(boardType.trackLength - 2)
    fun isInHomeColumn(boardType: BoardType): Boolean =
        progress in (boardType.trackLength - 1) until boardType.homeFinishProgress
}

data class PlayerMatchState(
    val config: PlayerConfig,
    val tokens: List<TokenState>,
    val score: Int = 0,
    val cutsMade: Int = 0,
    val frozenTurns: Int = 0,
    val finishRank: Int = 0, // 0 if still playing, 1..6 when finished
    val activePowerInventory: List<PowerType> = emptyList(),
    val powerCooldowns: Map<PowerType, Int> = emptyMap(),
    val pendingMultiplier: Int = 1,
    val pendingBonusSteps: Int = 0
) {
    fun finishedCount(boardType: BoardType): Int = tokens.count { it.isFinished(boardType) }
    fun hasFinishedAll(boardType: BoardType): Boolean = finishedCount(boardType) == 4
}

enum class TurnPhase {
    WAIT_FOR_ROLL,
    SELECT_TOKEN,
    MATCH_OVER
}

data class LegalMove(
    val tokenIndex: Int,
    val fromProgress: Int,
    val toProgress: Int,
    val effectiveSteps: Int,
    val willCapture: Boolean = false,
    val willReachHome: Boolean = false,
    val landedPower: PowerType? = null
)

data class CutEvent(
    val attackerColor: PlayerColor,
    val victimColor: PlayerColor,
    val victimTokenIndex: Int,
    val globalTileIndex: Int,
    val timestampMs: Long = System.currentTimeMillis()
)

data class PowerActivationEvent(
    val playerColor: PlayerColor,
    val powerType: PowerType,
    val targetTileIndex: Int?,
    val summary: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class MatchSetupConfig(
    val gameMode: GameMode,
    val boardType: BoardType,
    val players: List<PlayerConfig>,
    val rules: RuleConfig = RuleConfig(),
    val tournamentRound: Int = 0
)

data class GameState(
    val matchId: String,
    val gameMode: GameMode,
    val boardType: BoardType,
    val rules: RuleConfig,
    val players: List<PlayerMatchState>,
    val currentTurnIndex: Int = 0,
    val phase: TurnPhase = TurnPhase.WAIT_FOR_ROLL,
    val lastDiceRoll: Int? = null,
    val consecutiveSixes: Int = 0,
    val legalMoves: List<LegalMove> = emptyList(),
    val boardPowers: Map<Int, PowerType> = emptyMap(),
    val lastCutEvent: CutEvent? = null,
    val lastPowerEvent: PowerActivationEvent? = null,
    val statusBanner: String = "",
    val turnCounter: Int = 1,
    val startTimeMs: Long = System.currentTimeMillis(),
    val winnerColors: List<PlayerColor> = emptyList(),
    val winningTeamId: Int? = null,
    val undoSnapshot: GameState? = null,
    val tournamentRound: Int = 0
) {
    val currentPlayer: PlayerMatchState
        get() = players[currentTurnIndex]
}
