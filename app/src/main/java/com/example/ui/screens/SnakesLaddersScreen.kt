package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.SoundManager
import com.example.data.local.AppSettingsEntity
import com.example.game.SnakesLaddersEngine
import com.example.game.SnakesLaddersMode
import com.example.game.SnakesLaddersState
import com.example.game.SnakesPlayer
import com.example.game.SnakesTurnOutcome
import com.example.i18n.LocalAppStrings
import com.example.ui.components.Dice3D
import com.example.ui.components.RoyalArenaBackground
import com.example.ui.components.SnakesLaddersBoardCanvas
import com.example.ui.theme.ArenaCardSurface
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoOrange
import com.example.ui.theme.LudoRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SnakesLaddersScreen(
    playerName: String,
    settings: AppSettingsEntity,
    soundManager: SoundManager,
    onToggleSfx: () -> Unit,
    onToggleVibration: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val scope = rememberCoroutineScope()

    var selectedMode by remember { mutableStateOf(SnakesLaddersMode.VS_COMPUTER) }
    var state by remember { mutableStateOf(SnakesLaddersEngine.createInitialState(selectedMode, playerName)) }
    var isRolling by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    var showExitConfirm by remember { mutableStateOf(false) }

    BackHandler {
        when {
            showRules -> showRules = false
            state.isMatchOver || state.turnCounter <= 1 -> onBack()
            else -> showExitConfirm = true
        }
    }

    fun applyRollResult() {
        val next = SnakesLaddersEngine.rollAndMove(state)
        state = next
        when (next.lastOutcome) {
            SnakesTurnOutcome.LADDER_CLIMB -> soundManager.playPowerActivate()
            SnakesTurnOutcome.SNAKE_BITE -> soundManager.playTokenCut()
            SnakesTurnOutcome.WON -> soundManager.playWinFanfare()
            else -> soundManager.playTokenMove()
        }
    }

    fun triggerHumanRoll() {
        if (isRolling || state.isMatchOver || state.currentPlayer.isAi) return
        isRolling = true
        soundManager.playDiceRoll()
        scope.launch {
            delay(360L)
            isRolling = false
            applyRollResult()
        }
    }

    // Automatic AI turn loop for You vs Com
    LaunchedEffect(state.currentTurnIndex, state.turnCounter, state.isMatchOver, showExitConfirm) {
        if (!state.isMatchOver && state.currentPlayer.isAi && !showExitConfirm) {
            delay(520L)
            if (!state.isMatchOver && state.currentPlayer.isAi && !showExitConfirm) {
                isRolling = true
                soundManager.playDiceRoll()
                delay(340L)
                isRolling = false
                applyRollResult()
            }
        }
    }

    RoyalArenaBackground(darkTheme = settings.darkThemeMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Controls Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = ArenaCardSurface.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (state.turnCounter > 1 && !state.isMatchOver) showExitConfirm = true else onBack()
                            },
                            modifier = Modifier.testTag("snakes_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = strings.snakesAndLadders,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = GoldAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SnakesHudChip(
                        icon = if (settings.sfxEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        desc = "Sound",
                        onClick = onToggleSfx
                    )
                    SnakesHudChip(
                        icon = Icons.Filled.Vibration,
                        desc = "Vibration",
                        tint = if (settings.vibrationEnabled) GoldAccent else Color.White.copy(alpha = 0.4f),
                        onClick = onToggleVibration
                    )
                    SnakesHudChip(
                        icon = Icons.Filled.Refresh,
                        desc = "Restart",
                        onClick = {
                            isRolling = false
                            state = SnakesLaddersEngine.createInitialState(selectedMode, playerName)
                        }
                    )
                    SnakesHudChip(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        desc = "Rules",
                        onClick = { showRules = true }
                    )
                }
            }

            // Mode Selector Chips (You vs Com, 2P, 3P, 4P)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SnakesLaddersMode.entries.forEach { mode ->
                    val active = selectedMode == mode
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (active) LudoOrange else ArenaCardSurface.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, if (active) GoldAccent else Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                soundManager.playButtonClick()
                                selectedMode = mode
                                isRolling = false
                                state = SnakesLaddersEngine.createInitialState(mode, playerName)
                            }
                            .testTag("snakes_mode_${mode.name.lowercase()}")
                    ) {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // 10x10 Snakes & Ladders Board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                SnakesLaddersBoardCanvas(state = state)
            }

            // Turn Status Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ArenaCardSurface.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, state.currentPlayer.color.primary.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.statusMessage,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = GoldAccent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            // Bottom Player + Center Dice Bar (matching Snakes & Ladders screenshot)
            SnakesBottomControlDock(
                state = state,
                isRolling = isRolling,
                onRollClick = ::triggerHumanRoll
            )
        }
    }

    if (showRules) {
        AlertDialog(
            onDismissRequest = { showRules = false },
            title = { Text(strings.snakesAndLadders, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("• Tap the dice on your turn to move across tiles 1 to 100.")
                    Text("• Land at the bottom of a Ladder to climb straight up!")
                    Text("• Land on a Snake's head and you'll slide down to its tail.")
                    Text("• Roll a 6 to earn a bonus roll.")
                    Text("• Reach square 100 with an exact roll to win!")
                }
            },
            confirmButton = {
                Button(onClick = { showRules = false }) { Text("Got It") }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("Leave Snakes & Ladders?", fontWeight = FontWeight.Bold) },
            text = { Text("Return to the main menu?") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LudoRed)
                ) {
                    Text(strings.exitGame, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitConfirm = false }) {
                    Text(strings.resumeGame)
                }
            }
        )
    }

    if (state.isMatchOver) {
        val winner = state.winnerIndex?.let { state.players.getOrNull(it) }
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = strings.victoryTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = GoldAccent
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_trophy_crown),
                        contentDescription = "Victory Trophy",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "${winner?.name ?: "Player"} reached 100!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            isRolling = false
                            state = SnakesLaddersEngine.createInitialState(selectedMode, playerName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LudoGreen)
                    ) {
                        Text(strings.playAgain, color = Color.White)
                    }
                    OutlinedButton(onClick = onBack) {
                        Text(strings.backToHome)
                    }
                }
            }
        )
    }
}

@Composable
private fun SnakesBottomControlDock(
    state: SnakesLaddersState,
    isRolling: Boolean,
    onRollClick: () -> Unit
) {
    val canHumanRoll = !state.isMatchOver && !state.currentPlayer.isAi && !isRolling
    val players = state.players

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ArenaCardSurface.copy(alpha = 0.92f),
        border = BorderStroke(1.5.dp, state.currentPlayer.color.primary.copy(alpha = 0.85f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = canHumanRoll, onClick = onRollClick)
    ) {
        if (players.size == 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SnakesPlayerBadge(
                    player = players[0],
                    isActive = state.currentTurnIndex == 0 && !state.isMatchOver,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Dice3D(
                    value = state.lastDiceRoll,
                    isRolling = isRolling,
                    canRoll = canHumanRoll,
                    accentColor = state.currentPlayer.color.primary,
                    onRollClick = onRollClick,
                    size = 54.dp,
                    testTag = "snakes_dice_button"
                )
                Spacer(Modifier.width(8.dp))
                SnakesPlayerBadge(
                    player = players[1],
                    isActive = state.currentTurnIndex == 1 && !state.isMatchOver,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // 3-4 players: Left pair | Center Dice | Right pair
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SnakesPlayerBadge(players[0], state.currentTurnIndex == 0 && !state.isMatchOver)
                    if (players.size >= 3) {
                        SnakesPlayerBadge(players[2], state.currentTurnIndex == 2 && !state.isMatchOver)
                    }
                }
                Dice3D(
                    value = state.lastDiceRoll,
                    isRolling = isRolling,
                    canRoll = canHumanRoll,
                    accentColor = state.currentPlayer.color.primary,
                    onRollClick = onRollClick,
                    size = 54.dp,
                    testTag = "snakes_dice_button"
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SnakesPlayerBadge(players[1], state.currentTurnIndex == 1 && !state.isMatchOver)
                    if (players.size >= 4) {
                        SnakesPlayerBadge(players[3], state.currentTurnIndex == 3 && !state.isMatchOver)
                    }
                }
            }
        }
    }
}

@Composable
private fun SnakesPlayerBadge(
    player: SnakesPlayer,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) player.color.dark.copy(alpha = 0.92f) else Color(0xFF0A1638).copy(alpha = 0.85f),
        border = BorderStroke(
            width = if (isActive) 2.dp else 1.dp,
            color = if (isActive) GoldAccent else player.color.primary.copy(alpha = 0.45f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(player.color.primary)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (player.isAi) Icons.Filled.SmartToy else Icons.Filled.Person,
                    contentDescription = player.name,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(Modifier.width(6.dp))
            Column {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Black),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Tile ${player.square}/100",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = if (isActive) GoldAccent else Color.White.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun SnakesHudChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = ArenaCardSurface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        modifier = Modifier.size(40.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = desc, tint = tint, modifier = Modifier.size(18.dp))
        }
    }
}
