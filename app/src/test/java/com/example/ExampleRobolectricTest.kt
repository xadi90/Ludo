package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.GameStateSerializer
import com.example.game.BoardType
import com.example.game.GameMode
import com.example.game.LudoEngine
import com.example.game.MatchSetupConfig
import com.example.game.PlayerColor
import com.example.game.PlayerConfig
import com.example.security.DeveloperIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app_name string is LUDO`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LUDO", appName)
    }

    @Test
    fun `game state json roundtrip preserves offline match`() {
        val setup = MatchSetupConfig(
            gameMode = GameMode.POWER_MODE,
            boardType = BoardType.SIX_PLAYER,
            players = PlayerColor.sixPlayerOrder.mapIndexed { i, c ->
                PlayerConfig(color = c, name = "P${i + 1}", isAi = i > 0)
            }
        )
        val initial = LudoEngine.createInitialState(setup)
        val rolled = LudoEngine.rollDice(initial, forcedRoll = 6)
        val moved = LudoEngine.applyMove(rolled, 0)

        val json = GameStateSerializer.toJson(moved)
        val restored = GameStateSerializer.fromJson(json)
        assertNotNull(restored)
        assertEquals(moved.matchId, restored!!.matchId)
        assertEquals(BoardType.SIX_PLAYER, restored.boardType)
        assertEquals(0, restored.players[0].tokens[0].progress)
    }

    @Test
    fun `home screen displays LUDO and locked developer identity`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("play_now_button").assertIsDisplayed()
        composeRule.onNodeWithTag("home_menu_grid")
            .performScrollToNode(hasTestTag("menu_about_dev"))
        composeRule.onNodeWithTag("menu_about_dev").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText(DeveloperIdentity.DEVELOPER_NAME).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(DeveloperIdentity.DEVELOPER_EMAIL).assertIsDisplayed()
        composeRule.onNodeWithText(DeveloperIdentity.DEVELOPER_PHONE).assertIsDisplayed()
    }

    @Test
    fun `play now launches active game board with dice and pause controls`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("play_now_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ludo_board_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("dice_roll_button").assertIsDisplayed()
        composeRule.onNodeWithTag("game_pause_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pause_resume_button").assertIsDisplayed()
    }
}
