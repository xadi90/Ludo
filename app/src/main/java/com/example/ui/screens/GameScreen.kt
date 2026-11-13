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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.data.local.AppSettingsEntity
import com.example.game.BoardType
import com.example.game.CutEvent
import com.example.game.GameMode
import com.example.game.GameState
import com.example.game.PlayerColor
import com.example.game.PlayerMatchState
import com.example.game.PowerActivationEvent
import com.example.game.PowerType
import com.example.game.TurnPhase
import com.example.i18n.LocalAppStrings
import com.example.ui.components.Dice3D
import com.example.ui.components.GlossyButton
import com.example.ui.components.LudoBoardCanvas
import com.example.ui.components.RoyalArenaBackground
import com.example.ui.theme.ArenaCardSurface
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoBlueDark
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoGreenDark
import com.example.ui.theme.LudoPurple
import com.example.ui.theme.LudoRed

@Composable
fun GameScreen(
    gameState: GameState?,
    settings: AppSettingsEntity,
    isDiceRolling: Boolean,
    isPaused: Boolean,
    activeCut: CutEvent?,
    activePower: PowerActivationEvent?,
    onRollDice: () -> Unit,
    onTokenTapped: (Int) -> Unit,
    onActivatePower: (PowerType) -> Unit,
    onUndo: () -> Unit,
    onPauseChanged: (Boolean) -> Unit,
    onRestartMatch: () -> Unit,
    onExitMatch: () -> Unit,
    onToggleSfx: () -> Unit,
    onToggleVibration: () -> Unit,
    onAdvanceTournament: () -> Unit
) {
    val strings = LocalAppStrings.current
    var showRulesDialog by remember { mutableStateOf(false) }

    BackHandler {
        if (showRulesDialog) {
            showRulesDialog = false
        } else if (gameState?.phase == TurnPhase.MATCH_OVER) {
            onExitMatch()
        } else if (isPaused) {
            onPauseChanged(false)
        } else {
            onPauseChanged(true)
        }
    }

    if (gameState == null) {
        RoyalArenaBackground(darkTheme = settings.darkThemeMode) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading match...", color = Color.White)
            }
        }
        return
    }

    RoyalArenaBackground(darkTheme = settings.darkThemeMode) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val isWideLandscape = maxWidth > maxHeight && maxWidth >= 600.dp

            if (isWideLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        LudoBoardCanvas(
                            gameState = gameState,
                            activeCut = activeCut,
                            activePower = activePower,
                            onTokenTapped = onTokenTapped,
                            modifier = Modifier.fillMaxHeight(0.96f)
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GameHudTopBar(
                            gameState = gameState,
                            settings = settings,
                            onPause = { onPauseChanged(true) },
                            onUndo = onUndo,
                            onToggleSfx = onToggleSfx,
                            onToggleVibration = onToggleVibration,
                            onShowRules = { showRulesDialog = true }
                        )
                        TurnStatusStrip(gameState = gameState)
                        CornerPodsRow(
                            gameState = gameState,
                            colors = if (gameState.boardType == BoardType.SIX_PLAYER) {
                                listOf(PlayerColor.ORANGE, PlayerColor.RED, PlayerColor.GREEN)
                            } else {
                                listOf(PlayerColor.RED, PlayerColor.GREEN)
                            },
                            isTopRow = true,
                            isDiceRolling = isDiceRolling,
                            onRollDice = onRollDice
                        )
                        CornerPodsRow(
                            gameState = gameState,
                            colors = if (gameState.boardType == BoardType.SIX_PLAYER) {
                                listOf(PlayerColor.PURPLE, PlayerColor.BLUE, PlayerColor.YELLOW)
                            } else {
                                listOf(PlayerColor.BLUE, PlayerColor.YELLOW)
                            },
                            isTopRow = false,
                            isDiceRolling = isDiceRolling,
                            onRollDice = onRollDice
                        )
                        if (gameState.gameMode == GameMode.POWER_MODE) {
                            PowerInventoryBar(
                                gameState = gameState,
                                onActivatePower = onActivatePower
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    GameHudTopBar(
                        gameState = gameState,
                        settings = settings,
                        onPause = { onPauseChanged(true) },
                        onUndo = onUndo,
                        onToggleSfx = onToggleSfx,
                        onToggleVibration = onToggleVibration,
                        onShowRules = { showRulesDialog = true }
                    )

                    Spacer(Modifier.height(4.dp))

                    // Top Side Player Pods (aligned with Top-Left & Top-Right bases)
                    CornerPodsRow(
                        gameState = gameState,
                        colors = if (gameState.boardType == BoardType.SIX_PLAYER) {
                            listOf(PlayerColor.ORANGE, PlayerColor.RED, PlayerColor.GREEN)
                        } else {
                            listOf(PlayerColor.RED, PlayerColor.GREEN)
                        },
                        isTopRow = true,
                        isDiceRolling = isDiceRolling,
                        onRollDice = onRollDice
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LudoBoardCanvas(
                            gameState = gameState,
                            activeCut = activeCut,
                            activePower = activePower,
                            onTokenTapped = onTokenTapped
                        )
                    }

                    if (gameState.gameMode == GameMode.POWER_MODE) {
                        PowerInventoryBar(
                            gameState = gameState,
                            onActivatePower = onActivatePower
                        )
                    }

                    // Bottom Side Player Pods (aligned with Bottom-Left & Bottom-Right bases)
                    CornerPodsRow(
                        gameState = gameState,
                        colors = if (gameState.boardType == BoardType.SIX_PLAYER) {
                            listOf(PlayerColor.PURPLE, PlayerColor.BLUE, PlayerColor.YELLOW)
                        } else {
                            listOf(PlayerColor.BLUE, PlayerColor.YELLOW)
                        },
                        isTopRow = false,
                        isDiceRolling = isDiceRolling,
                        onRollDice = onRollDice
                    )

                    Spacer(Modifier.height(4.dp))

                    TurnStatusStrip(gameState = gameState)
                }
            }
        }
    }

    // Pause / Exit Match Confirmation Dialog
    if (isPaused && gameState.phase != TurnPhase.MATCH_OVER) {
        AlertDialog(
            onDismissRequest = { onPauseChanged(false) },
            title = {
                Text(strings.pauseTitle, fontWeight = FontWeight.Black)
            },
            text = {
                Text("Progress is saved automatically offline. What would you like to do?")
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlossyButton(
                        text = strings.resumeGame,
                        onClick = { onPauseChanged(false) },
                        topColor = LudoGreen,
                        bottomColor = LudoGreenDark,
                        icon = Icons.Filled.PlayArrow,
                        height = 48.dp,
                        testTag = "pause_resume_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                    GlossyButton(
                        text = strings.restartGame,
                        onClick = {
                            onPauseChanged(false)
                            onRestartMatch()
                        },
                        topColor = LudoBlue,
                        bottomColor = LudoBlueDark,
                        icon = Icons.Filled.Refresh,
                        height = 48.dp,
                        testTag = "pause_restart_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedButton(
                        onClick = onExitMatch,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("pause_exit_button")
                    ) {
                        Text(strings.exitGame, color = LudoRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }

    // Victory Celebration Dialog
    if (gameState.phase == TurnPhase.MATCH_OVER) {
        val humanWon = gameState.players.any { !it.config.isAi && it.config.color in gameState.winnerColors }
        AlertDialog(
            onDismissRequest = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.victoryTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = GoldAccent
                    )
                }
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
                            .size(120.dp)
                            .clip(RoundedCornerShape(20.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                    val winnerText = gameState.winnerColors.joinToString(" & ") { c ->
                        gameState.players.firstOrNull { it.config.color == c }?.config?.name ?: c.displayName
                    }
                    Text(
                        text = winnerText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.height(6.dp))
                    gameState.players.forEach { p ->
                        Text(
                            text = "${p.config.name}: ${p.finishedCount(gameState.boardType)}/4 Home • ${p.cutsMade} Cuts • ${p.score} pts",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (gameState.gameMode == GameMode.TOURNAMENT && humanWon && gameState.tournamentRound in 1..2) {
                        Button(
                            onClick = onAdvanceTournament,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                        ) {
                            Text("Next Round", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onRestartMatch,
                            colors = ButtonDefaults.buttonColors(containerColor = LudoGreen)
                        ) {
                            Text(strings.playAgain, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(onClick = onExitMatch) {
                        Text(strings.backToHome)
                    }
                }
            }
        )
    }

    // In-Game Rules & Powers Quick Sheet
    if (showRulesDialog) {
        AlertDialog(
            onDismissRequest = { showRulesDialog = false },
            title = { Text("LUDO Rules & Powers", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("• Tap the active player's corner dice on your side of the board to roll!")
                    Text("• Roll 6 (or 1 if enabled) to deploy a token from Base.")
                    Text("• Rolling a 6 grants a bonus roll (3 sixes in a row forfeits turn).")
                    Text("• Land on an opponent on a non-safe square to cut them back to Base!")
                    Text("• Star tiles and colored Start squares are Safe Zones.")
                    Text("• Reach your Center Home Triangle with an exact dice roll.")
                    if (gameState.gameMode == GameMode.POWER_MODE) {
                        Spacer(Modifier.height(6.dp))
                        Text("Power Mode Abilities:", fontWeight = FontWeight.Bold, color = GoldAccent)
                        PowerType.entries.forEach { p ->
                            Text("• ${p.title}: ${p.description}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showRulesDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }
}

@Composable
private fun CornerPodsRow(
    gameState: GameState,
    colors: List<PlayerColor>,
    isTopRow: Boolean,
    isDiceRolling: Boolean,
    onRollDice: () -> Unit
) {
    val activeColor = gameState.currentPlayer.config.color
    val isSixPlayer = colors.size >= 3

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(if (isSixPlayer) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        colors.forEachIndexed { idx, slotColor ->
            val player = gameState.players.firstOrNull { it.config.color == slotColor }
            val diceOnRight = if (isSixPlayer) idx < 2 else idx == 0

            if (player != null) {
                val isActive = player.config.color == activeColor && gameState.phase != TurnPhase.MATCH_OVER
                CornerPlayerDicePod(
                    player = player,
                    boardType = gameState.boardType,
                    phase = gameState.phase,
                    lastDiceRoll = gameState.lastDiceRoll,
                    isActive = isActive,
                    isDiceRolling = isDiceRolling && isActive,
                    diceOnRight = diceOnRight,
                    compact = isSixPlayer,
                    onRollDice = onRollDice,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Keep opposite corners aligned to their exact corner bases
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CornerPlayerDicePod(
    player: PlayerMatchState,
    boardType: BoardType,
    phase: TurnPhase,
    lastDiceRoll: Int?,
    isActive: Boolean,
    isDiceRolling: Boolean,
    diceOnRight: Boolean,
    compact: Boolean,
    onRollDice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canRoll = isActive && phase == TurnPhase.WAIT_FOR_ROLL && !player.config.isAi
    val finished = player.finishedCount(boardType)
    val podShape = RoundedCornerShape(16.dp)
    val bgBrush = if (isActive) {
        Brush.verticalGradient(
            listOf(
                player.config.color.primary.copy(alpha = 0.88f),
                player.config.color.dark.copy(alpha = 0.96f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                ArenaCardSurface.copy(alpha = 0.85f),
                Color(0xFF0A1638).copy(alpha = 0.88f)
            )
        )
    }
    val diceSize = if (compact) 38.dp else 46.dp

    Surface(
        shape = podShape,
        color = Color.Transparent,
        border = BorderStroke(
            width = if (isActive) 2.5.dp else 1.2.dp,
            color = if (isActive) GoldAccent else player.config.color.primary.copy(alpha = 0.55f)
        ),
        modifier = modifier
            .shadow(
                elevation = if (isActive) 10.dp else 3.dp,
                shape = podShape,
                ambientColor = player.config.color.primary
            )
            .clip(podShape)
            .background(bgBrush)
            .clickable(enabled = canRoll && !isDiceRolling, onClick = onRollDice)
            .testTag("corner_pod_${player.config.color.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            @Composable
            fun PlayerInfoSection(infoModifier: Modifier) {
                Row(
                    modifier = infoModifier,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (compact) 26.dp else 32.dp)
                            .clip(CircleShape)
                            .background(player.config.color.primary)
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (player.config.isAi) Icons.Filled.SmartToy else Icons.Filled.Person,
                            contentDescription = player.config.name,
                            tint = Color.White,
                            modifier = Modifier.size(if (compact) 15.dp else 18.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = player.config.name,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = if (compact) 11.sp else 12.sp,
                                fontWeight = FontWeight.Black
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "★$finished/4 • ⚔${player.cutsMade}" +
                                (if (player.config.teamId > 0) " T${player.config.teamId}" else "") +
                                (if (player.frozenTurns > 0) " ❄" else ""),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = if (compact) 9.sp else 10.sp),
                            color = if (isActive) GoldAccent else Color.White.copy(alpha = 0.78f),
                            maxLines = 1
                        )
                    }
                }
            }

            @Composable
            fun DiceSlotSection() {
                if (isActive) {
                    Dice3D(
                        value = lastDiceRoll,
                        isRolling = isDiceRolling,
                        canRoll = canRoll,
                        accentColor = player.config.color.primary,
                        onRollClick = onRollDice,
                        size = diceSize,
                        testTag = "dice_roll_button"
                    )
                } else {
                    // Resting inactive die tray matching screenshot corner slots
                    Box(
                        modifier = Modifier
                            .size(diceSize)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(
                                width = 1.dp,
                                color = player.config.color.primary.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(player.config.color.primary.copy(alpha = 0.55f))
                        )
                    }
                }
            }

            if (diceOnRight) {
                PlayerInfoSection(Modifier.weight(1f))
                Spacer(Modifier.width(4.dp))
                DiceSlotSection()
            } else {
                DiceSlotSection()
                Spacer(Modifier.width(4.dp))
                PlayerInfoSection(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TurnStatusStrip(gameState: GameState) {
    val strings = LocalAppStrings.current
    val current = gameState.currentPlayer
    val message = when {
        gameState.phase == TurnPhase.SELECT_TOKEN && !current.config.isAi ->
            "${current.config.name}: ${strings.selectToken}"
        current.config.isAi ->
            "${current.config.name} (${strings.aiThinking})"
        else ->
            gameState.statusBanner.ifBlank { "${current.config.name}${strings.yourTurn}" }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ArenaCardSurface.copy(alpha = 0.9f),
        border = BorderStroke(1.2.dp, current.config.color.primary.copy(alpha = 0.75f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(current.config.color.primary)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = GoldAccent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GameHudTopBar(
    gameState: GameState,
    settings: AppSettingsEntity,
    onPause: () -> Unit,
    onUndo: () -> Unit,
    onToggleSfx: () -> Unit,
    onToggleVibration: () -> Unit,
    onShowRules: () -> Unit
) {
    val canUndo = gameState.rules.allowUndoInOffline &&
        gameState.gameMode != GameMode.POWER_MODE &&
        gameState.undoSnapshot != null

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HudIconChip(
                icon = Icons.Filled.Pause,
                desc = "Pause Match",
                testTag = "game_pause_button",
                onClick = onPause
            )
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ArenaCardSurface.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
            ) {
                Text(
                    text = if (gameState.tournamentRound > 0) "Cup R${gameState.tournamentRound}"
                    else gameState.gameMode.name.replace("_", " "),
                    style = MaterialTheme.typography.labelMedium,
                    color = GoldAccent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (canUndo) {
                HudIconChip(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    desc = "Undo",
                    testTag = "game_undo_button",
                    onClick = onUndo
                )
            }
            HudIconChip(
                icon = if (settings.sfxEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                desc = "Sound Toggle",
                testTag = "game_sound_toggle",
                onClick = onToggleSfx
            )
            HudIconChip(
                icon = Icons.Filled.Vibration,
                desc = "Vibration Toggle",
                tint = if (settings.vibrationEnabled) GoldAccent else Color.White.copy(alpha = 0.4f),
                testTag = "game_vibe_toggle",
                onClick = onToggleVibration
            )
            HudIconChip(
                icon = Icons.AutoMirrored.Filled.HelpOutline,
                desc = "Rules",
                testTag = "game_rules_button",
                onClick = onShowRules
            )
        }
    }
}

@Composable
private fun HudIconChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    tint: Color = Color.White,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = ArenaCardSurface.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
        modifier = Modifier.size(44.dp)
    ) {
        IconButton(onClick = onClick, modifier = Modifier.testTag(testTag)) {
            Icon(imageVector = icon, contentDescription = desc, tint = tint, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun PowerInventoryBar(
    gameState: GameState,
    onActivatePower: (PowerType) -> Unit
) {
    val player = gameState.currentPlayer
    if (player.config.isAi || player.activePowerInventory.isEmpty()) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Bolt, contentDescription = "Powers", tint = GoldAccent, modifier = Modifier.size(18.dp))
        player.activePowerInventory.forEach { power ->
            val cd = player.powerCooldowns[power] ?: 0
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (cd == 0) LudoPurple else Color.DarkGray,
                border = BorderStroke(1.dp, if (cd == 0) GoldAccent else Color.Gray),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = cd == 0) { onActivatePower(power) }
            ) {
                Text(
                    text = if (cd == 0) "${power.shortCode} • ${power.title}" else "${power.shortCode} (${cd}t)",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
