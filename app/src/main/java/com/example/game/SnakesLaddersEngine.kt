package com.example.game

import androidx.compose.ui.geometry.Offset
import java.security.SecureRandom

enum class SnakesLaddersMode(val label: String, val playerCount: Int) {
    VS_COMPUTER("You vs Com", 2),
    TWO_PLAYER("2 Players", 2),
    THREE_PLAYER("3 Players", 3),
    FOUR_PLAYER("4 Players", 4)
}

data class BoardJump(
    val from: Int,
    val to: Int,
    val isLadder: Boolean
)

data class SnakesPlayer(
    val name: String,
    val color: PlayerColor,
    val isAi: Boolean,
    val square: Int = 0 // 0 = off board start, 1..100 on board
)

enum class SnakesTurnOutcome {
    NONE,
    MOVED,
    LADDER_CLIMB,
    SNAKE_BITE,
    BONUS_SIX,
    OVERSHOOT,
    WON
}

data class SnakesLaddersState(
    val mode: SnakesLaddersMode,
    val players: List<SnakesPlayer>,
    val currentTurnIndex: Int = 0,
    val lastDiceRoll: Int? = null,
    val winnerIndex: Int? = null,
    val lastOutcome: SnakesTurnOutcome = SnakesTurnOutcome.NONE,
    val lastJump: BoardJump? = null,
    val statusMessage: String = "Roll the dice to begin!",
    val turnCounter: Int = 1
) {
    val currentPlayer: SnakesPlayer get() = players[currentTurnIndex]
    val isMatchOver: Boolean get() = winnerIndex != null
}

object SnakesLaddersEngine {

    private val secureRandom = SecureRandom()

    val ladders: List<BoardJump> = listOf(
        BoardJump(4, 25, true),
        BoardJump(13, 46, true),
        BoardJump(33, 49, true),
        BoardJump(42, 63, true),
        BoardJump(50, 69, true),
        BoardJump(62, 81, true),
        BoardJump(74, 92, true),
        BoardJump(85, 97, true)
    )

    val snakes: List<BoardJump> = listOf(
        BoardJump(27, 5, false),
        BoardJump(40, 3, false),
        BoardJump(43, 18, false),
        BoardJump(54, 31, false),
        BoardJump(66, 45, false),
        BoardJump(76, 58, false),
        BoardJump(89, 53, false),
        BoardJump(99, 41, false)
    )

    private val jumpMap: Map<Int, BoardJump> = (ladders + snakes).associateBy { it.from }

    fun createInitialState(
        mode: SnakesLaddersMode,
        humanName: String = "You"
    ): SnakesLaddersState {
        val cleanName = humanName.ifBlank { "You" }.take(12)
        val players = when (mode) {
            SnakesLaddersMode.VS_COMPUTER -> listOf(
                SnakesPlayer(cleanName, PlayerColor.BLUE, isAi = false, square = 1),
                SnakesPlayer("Com", PlayerColor.RED, isAi = true, square = 1)
            )
            SnakesLaddersMode.TWO_PLAYER -> listOf(
                SnakesPlayer("Player 1", PlayerColor.BLUE, isAi = false, square = 1),
                SnakesPlayer("Player 2", PlayerColor.RED, isAi = false, square = 1)
            )
            SnakesLaddersMode.THREE_PLAYER -> listOf(
                SnakesPlayer("Player 1", PlayerColor.BLUE, isAi = false, square = 1),
                SnakesPlayer("Player 2", PlayerColor.RED, isAi = false, square = 1),
                SnakesPlayer("Player 3", PlayerColor.GREEN, isAi = false, square = 1)
            )
            SnakesLaddersMode.FOUR_PLAYER -> listOf(
                SnakesPlayer("Player 1", PlayerColor.BLUE, isAi = false, square = 1),
                SnakesPlayer("Player 2", PlayerColor.RED, isAi = false, square = 1),
                SnakesPlayer("Player 3", PlayerColor.GREEN, isAi = false, square = 1),
                SnakesPlayer("Player 4", PlayerColor.YELLOW, isAi = false, square = 1)
            )
        }
        return SnakesLaddersState(
            mode = mode,
            players = players,
            currentTurnIndex = 0,
            statusMessage = "${players[0].name}: Tap dice to roll!"
        )
    }

    fun rollAndMove(state: SnakesLaddersState, forcedRoll: Int? = null): SnakesLaddersState {
        if (state.isMatchOver) return state
        val roll = forcedRoll ?: (secureRandom.nextInt(6) + 1)
        val current = state.currentPlayer
        val candidate = current.square + roll

        // Exact roll to reach 100
        if (candidate > 100) {
            val nextIdx = (state.currentTurnIndex + 1) % state.players.size
            val need = 100 - current.square
            return state.copy(
                lastDiceRoll = roll,
                currentTurnIndex = nextIdx,
                lastOutcome = SnakesTurnOutcome.OVERSHOOT,
                lastJump = null,
                turnCounter = state.turnCounter + 1,
                statusMessage = "${current.name} rolled $roll (needs $need to win!). ${state.players[nextIdx].name}'s turn."
            )
        }

        val jump = jumpMap[candidate]
        val finalSquare = jump?.to ?: candidate
        val updatedPlayers = state.players.toMutableList()
        updatedPlayers[state.currentTurnIndex] = current.copy(square = finalSquare)

        if (finalSquare == 100) {
            return state.copy(
                players = updatedPlayers,
                lastDiceRoll = roll,
                winnerIndex = state.currentTurnIndex,
                lastOutcome = SnakesTurnOutcome.WON,
                lastJump = jump,
                statusMessage = "${current.name} reached 100 and WINS!"
            )
        }

        val outcome = when {
            jump?.isLadder == true -> SnakesTurnOutcome.LADDER_CLIMB
            jump?.isLadder == false -> SnakesTurnOutcome.SNAKE_BITE
            roll == 6 -> SnakesTurnOutcome.BONUS_SIX
            else -> SnakesTurnOutcome.MOVED
        }

        val bonusTurn = roll == 6
        val nextIdx = if (bonusTurn) state.currentTurnIndex else (state.currentTurnIndex + 1) % updatedPlayers.size
        val nextName = updatedPlayers[nextIdx].name

        val banner = when (outcome) {
            SnakesTurnOutcome.LADDER_CLIMB ->
                "${current.name} rolled $roll! Ladder ${jump!!.from} ➔ ${jump.to}! " +
                    if (bonusTurn) "Bonus roll!" else "$nextName's turn."
            SnakesTurnOutcome.SNAKE_BITE ->
                "${current.name} rolled $roll! Snake ${jump!!.from} ➔ ${jump.to}! " +
                    if (bonusTurn) "Bonus roll!" else "$nextName's turn."
            SnakesTurnOutcome.BONUS_SIX ->
                "${current.name} rolled 6 (${candidate})! Bonus roll!"
            else ->
                "${current.name} moved to $finalSquare. $nextName's turn."
        }

        return state.copy(
            players = updatedPlayers,
            currentTurnIndex = nextIdx,
            lastDiceRoll = roll,
            lastOutcome = outcome,
            lastJump = jump,
            turnCounter = state.turnCounter + 1,
            statusMessage = banner
        )
    }

    /**
     * Converts board square 1..100 into grid (rowFromTop 0..9, colFromLeft 0..9)
     * in classic boustrophedon zig-zag order where 1 is bottom-left and 100 is top-left.
     */
    fun squareToRowCol(square: Int): Pair<Int, Int> {
        val sq = square.coerceIn(1, 100)
        val rowFromBottom = (sq - 1) / 10
        val rowFromTop = 9 - rowFromBottom
        val offset = (sq - 1) % 10
        val colFromLeft = if (rowFromBottom % 2 == 0) offset else 9 - offset
        return rowFromTop to colFromLeft
    }

    fun squareNormalizedCenter(square: Int): Offset {
        val (r, c) = squareToRowCol(square)
        return Offset((c + 0.5f) / 10f, (r + 0.5f) / 10f)
    }
}
