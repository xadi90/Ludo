package com.example

import com.example.game.AiDifficulty
import com.example.game.BoardGeometry
import com.example.game.BoardType
import com.example.game.GameMode
import com.example.game.LudoAi
import com.example.game.LudoEngine
import com.example.game.MatchSetupConfig
import com.example.game.PlayerColor
import com.example.game.PlayerConfig
import com.example.game.PowerType
import com.example.game.RuleConfig
import com.example.game.TurnPhase
import com.example.security.AppSecurityManager
import com.example.security.DeveloperIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LudoEngineTest {

    private fun fourPlayerSetup(
        mode: GameMode = GameMode.OFFLINE_CLASSIC,
        rules: RuleConfig = RuleConfig()
    ) = MatchSetupConfig(
        gameMode = mode,
        boardType = BoardType.FOUR_PLAYER,
        players = listOf(
            PlayerConfig(PlayerColor.RED, "Red"),
            PlayerConfig(PlayerColor.GREEN, "Green"),
            PlayerConfig(PlayerColor.YELLOW, "Yellow"),
            PlayerConfig(PlayerColor.BLUE, "Blue")
        ),
        rules = rules
    )

    @Test
    fun `rolling 6 deploys token from base and grants bonus roll`() {
        val state0 = LudoEngine.createInitialState(fourPlayerSetup())
        val rolled = LudoEngine.rollDice(state0, forcedRoll = 6)
        assertEquals(TurnPhase.SELECT_TOKEN, rolled.phase)
        assertEquals(4, rolled.legalMoves.size)

        val moved = LudoEngine.applyMove(rolled, 0)
        assertEquals(0, moved.players[0].tokens[0].progress)
        assertEquals(0, moved.currentTurnIndex) // bonus turn on 6
        assertEquals(TurnPhase.WAIT_FOR_ROLL, moved.phase)
    }

    @Test
    fun `three consecutive sixes forfeit turn`() {
        var state = LudoEngine.createInitialState(fourPlayerSetup())
        state = LudoEngine.rollDice(state, forcedRoll = 6)
        state = LudoEngine.applyMove(state, 0)
        state = LudoEngine.rollDice(state, forcedRoll = 6)
        state = LudoEngine.applyMove(state, 1)
        val third = LudoEngine.rollDice(state, forcedRoll = 6)
        assertEquals(1, third.currentTurnIndex)
        assertEquals(TurnPhase.WAIT_FOR_ROLL, third.phase)
    }

    @Test
    fun `token cutting returns opponent to base and awards cut stat`() {
        var state = LudoEngine.createInitialState(fourPlayerSetup())
        // Put Green token #0 at Green progress 2 => global (13 + 2) = 15
        // Put Red token #0 at Red progress 12 => global 12, roll 3 -> lands on 15!
        val players = state.players.toMutableList()
        players[0] = players[0].copy(
            tokens = players[0].tokens.mapIndexed { i, t -> if (i == 0) t.copy(progress = 12) else t }
        )
        players[1] = players[1].copy(
            tokens = players[1].tokens.mapIndexed { i, t -> if (i == 0) t.copy(progress = 2) else t }
        )
        state = state.copy(players = players)

        val rolled = LudoEngine.rollDice(state, forcedRoll = 3)
        val afterCut = LudoEngine.applyMove(rolled, 0)

        assertNotNull(afterCut.lastCutEvent)
        assertEquals(PlayerColor.GREEN, afterCut.lastCutEvent!!.victimColor)
        assertEquals(-1, afterCut.players[1].tokens[0].progress)
        assertEquals(1, afterCut.players[0].cutsMade)
        assertEquals(0, afterCut.currentTurnIndex) // extra turn on capture
    }

    @Test
    fun `safe star tile protects opponent from being cut`() {
        var state = LudoEngine.createInitialState(fourPlayerSetup())
        // Global index 8 is a Star safe tile. Red progress 8 is global 8.
        // Green progress 47 is (13 + 47) % 52 = 8.
        val players = state.players.toMutableList()
        players[0] = players[0].copy(
            tokens = players[0].tokens.mapIndexed { i, t -> if (i == 0) t.copy(progress = 5) else t }
        )
        players[1] = players[1].copy(
            tokens = players[1].tokens.mapIndexed { i, t -> if (i == 0) t.copy(progress = 47) else t }
        )
        state = state.copy(players = players)

        val rolled = LudoEngine.rollDice(state, forcedRoll = 3)
        val moved = LudoEngine.applyMove(rolled, 0)
        assertEquals(null, moved.lastCutEvent)
        assertEquals(47, moved.players[1].tokens[0].progress)
    }

    @Test
    fun `exact roll required for home entry and four finished tokens wins match`() {
        var state = LudoEngine.createInitialState(fourPlayerSetup())
        val p0Tokens = state.players[0].tokens.mapIndexed { idx, t ->
            if (idx < 3) t.copy(progress = 56) else t.copy(progress = 53)
        }
        val players = state.players.toMutableList()
        players[0] = players[0].copy(tokens = p0Tokens)
        state = state.copy(players = players)

        // Roll 5 -> overshoots 56 (53 + 5 = 58 > 56), so no legal moves!
        val overshoot = LudoEngine.rollDice(state, forcedRoll = 5)
        assertEquals(1, overshoot.currentTurnIndex)

        // Roll 3 -> exact 56 -> WINS!
        val exact = LudoEngine.rollDice(state, forcedRoll = 3)
        val won = LudoEngine.applyMove(exact, 3)
        assertEquals(TurnPhase.MATCH_OVER, won.phase)
        assertEquals(listOf(PlayerColor.RED), won.winnerColors)
    }

    @Test
    fun `six player hexagonal board supports 6 players and 72 track tiles`() {
        val setup = MatchSetupConfig(
            gameMode = GameMode.OFFLINE_CLASSIC,
            boardType = BoardType.SIX_PLAYER,
            players = PlayerColor.sixPlayerOrder.mapIndexed { i, c ->
                PlayerConfig(c, "Player ${i + 1}")
            }
        )
        val state = LudoEngine.createInitialState(setup)
        assertEquals(6, state.players.size)
        assertEquals(72, BoardGeometry.sixPlayerTrackNormalized.size)
        val rolled = LudoEngine.rollDice(state, forcedRoll = 6)
        val moved = LudoEngine.applyMove(rolled, 0)
        assertEquals(0, moved.players[0].tokens[0].progress)
    }

    @Test
    fun `team mode prevents teammates from cutting each other`() {
        val setup = MatchSetupConfig(
            gameMode = GameMode.TEAM_BATTLE,
            boardType = BoardType.FOUR_PLAYER,
            players = listOf(
                PlayerConfig(PlayerColor.RED, "R", teamId = 1),
                PlayerConfig(PlayerColor.GREEN, "G", teamId = 2),
                PlayerConfig(PlayerColor.YELLOW, "Y", teamId = 1),
                PlayerConfig(PlayerColor.BLUE, "B", teamId = 2)
            )
        )
        var state = LudoEngine.createInitialState(setup)
        // Red at 28, Yellow (teammate) at 5 => (26 + 5) = 31. Red rolls 3 -> lands on 31.
        val players = state.players.toMutableList()
        players[0] = players[0].copy(tokens = players[0].tokens.mapIndexed { i, t -> if (i == 0) t.copy(progress = 28) else t })
        players[2] = players[2].copy(tokens = players[2].tokens.mapIndexed { i, t -> if (i == 0) t.copy(progress = 5) else t })
        state = state.copy(players = players)

        val rolled = LudoEngine.rollDice(state, forcedRoll = 3)
        val moved = LudoEngine.applyMove(rolled, 0)
        assertEquals(null, moved.lastCutEvent)
        assertEquals(5, moved.players[2].tokens[0].progress)
    }

    @Test
    fun `power mode shield protects token and rocket advances 5 steps`() {
        var state = LudoEngine.createInitialState(fourPlayerSetup(mode = GameMode.POWER_MODE))
        val players = state.players.toMutableList()
        players[0] = players[0].copy(
            tokens = players[0].tokens.mapIndexed { i, t -> if (i == 0) t.copy(progress = 4) else t },
            activePowerInventory = listOf(PowerType.SHIELD, PowerType.ROCKET, PowerType.DOUBLE_X2)
        )
        state = state.copy(players = players)

        val shielded = LudoEngine.activatePower(state, PowerType.SHIELD, 0)
        assertEquals(2, shielded.players[0].tokens[0].shieldTurns)

        val rocketed = LudoEngine.activatePower(shielded, PowerType.ROCKET, 0)
        assertEquals(9, rocketed.players[0].tokens[0].progress)
    }

    @Test
    fun `ai difficulty levels select valid moves`() {
        AiDifficulty.entries.forEach { diff ->
            val setup = MatchSetupConfig(
                gameMode = GameMode.COMPUTER_VS_AI,
                boardType = BoardType.FOUR_PLAYER,
                players = listOf(
                    PlayerConfig(PlayerColor.RED, "AI", isAi = true, aiDifficulty = diff),
                    PlayerConfig(PlayerColor.YELLOW, "P2", isAi = false)
                )
            )
            val state = LudoEngine.rollDice(LudoEngine.createInitialState(setup), forcedRoll = 6)
            val chosen = LudoAi.chooseMove(state)
            assertNotNull(chosen)
            assertTrue(chosen in state.legalMoves)
        }
    }

    @Test
    fun `developer identity is immutable and security validation works`() {
        assertTrue(AppSecurityManager.verifyDeveloperIdentityIntegrity())
        assertEquals("Xadi Ahmed", DeveloperIdentity.DEVELOPER_NAME)
        assertEquals("xadiahmed23@gmail.com", DeveloperIdentity.DEVELOPER_EMAIL)
        assertEquals("01878864913", DeveloperIdentity.DEVELOPER_PHONE)
        assertTrue(AppSecurityManager.isValidRoomCode("AB12CD"))
        assertFalse(AppSecurityManager.isValidRoomCode("invalid"))
    }
}
