package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
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
    fun `home screen displays LUDO and locked developer identity without ownership integrity card`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("play_now_button").assertIsDisplayed()
        composeRule.onNodeWithTag("home_menu_grid")
            .performScrollToNode(hasTestTag("menu_about_dev"))
        composeRule.onNodeWithTag("menu_about_dev").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText(DeveloperIdentity.DEVELOPER_NAME).onFirst().assertIsDisplayed()
        composeRule.onNodeWithText(DeveloperIdentity.DEVELOPER_EMAIL).assertIsDisplayed()
        composeRule.onNodeWithText(DeveloperIdentity.DEVELOPER_PHONE).assertIsDisplayed()
        composeRule.onAllNodesWithText("Ownership Integrity Verified").fetchSemanticsNodes().let {
            assertEquals(0, it.size)
        }
    }

    @Test
    fun `play now launches active game board with 4 corner pods and dice`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("play_now_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ludo_board_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("corner_pod_red").assertIsDisplayed()
        composeRule.onNodeWithTag("corner_pod_green").assertIsDisplayed()
        composeRule.onNodeWithTag("corner_pod_blue").assertIsDisplayed()
        composeRule.onNodeWithTag("corner_pod_yellow").assertIsDisplayed()
        composeRule.onNodeWithTag("dice_roll_button").assertIsDisplayed()
        composeRule.onNodeWithTag("game_pause_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pause_resume_button").assertIsDisplayed()
    }

    @Test
    fun `snakes and ladders launches from home screen with board and 4 corner dice pods`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_snakes_ladders").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("snakes_ladders_board").assertIsDisplayed()
        composeRule.onNodeWithTag("snakes_dice_button").assertIsDisplayed()

        // Switch to 4 Players and verify all 4 corner pods + active dice are displayed
        composeRule.onNodeWithTag("snakes_mode_four_player").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("snakes_corner_pod_0").assertIsDisplayed()
        composeRule.onNodeWithTag("snakes_corner_pod_1").assertIsDisplayed()
        composeRule.onNodeWithTag("snakes_corner_pod_2").assertIsDisplayed()
        composeRule.onNodeWithTag("snakes_corner_pod_3").assertIsDisplayed()
        composeRule.onNodeWithTag("snakes_dice_button").assertIsDisplayed()
    }

    @Test
    fun `secret vault pin setup verification and aes encryption roundtrip`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val vault = com.example.vault.SecretVaultManager(context)
        vault.setOrChangePin("2580")
        assertEquals(true, vault.isPinConfigured())
        assertEquals(false, vault.verifyPin("0000"))
        assertEquals(true, vault.verifyPin("2580"))

        val sampleBytes = "Secret Gallery Photo Bytes 12345".toByteArray()
        val item = vault.importRawBytes(
            displayName = "secret_pic.jpg",
            mimeType = "image/jpeg",
            mediaType = com.example.vault.VaultMediaType.IMAGE,
            plainBytes = sampleBytes
        )
        assertEquals(1, vault.listItems().size)
        val decrypted = vault.readDecryptedBytes(item)
        assertNotNull(decrypted)
        assertEquals(String(sampleBytes), String(decrypted!!))

        vault.deleteItem(item.id)
        assertEquals(0, vault.listItems().size)
    }

    @Test
    fun `long pressing LUDO banner opens secret vault pin setup and unlocks`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("hero_banner_vault_trigger").performTouchInput { longClick() }
        composeRule.waitForIdle()

        // Step 1: Set PIN 1234
        listOf("1", "2", "3", "4", "OK").forEach { key ->
            composeRule.onNodeWithTag("vault_key_$key").performClick()
        }
        composeRule.waitForIdle()

        // Step 2: Confirm PIN 1234
        listOf("1", "2", "3", "4", "OK").forEach { key ->
            composeRule.onNodeWithTag("vault_key_$key").performClick()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("vault_add_media_button").assertIsDisplayed()
        composeRule.onNodeWithTag("vault_add_files_button").assertIsDisplayed()
    }
}
