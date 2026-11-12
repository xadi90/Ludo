package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.game.AiDifficulty
import com.example.game.BoardType
import com.example.game.GameMode
import com.example.game.MatchSetupConfig
import com.example.game.PlayerColor
import com.example.game.PlayerConfig
import com.example.game.PowerType
import com.example.i18n.LocalAppStrings
import com.example.security.AppSecurityManager
import com.example.ui.components.GlossyButton
import com.example.ui.components.GlowingGameCard
import com.example.ui.components.LudoTopBar
import com.example.ui.components.RoyalArenaBackground
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoBlueDark
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoGreenDark
import com.example.ui.theme.LudoOrange
import com.example.ui.theme.LudoPurple
import com.example.ui.theme.LudoRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GameModeScreen(
    setup: MatchSetupConfig,
    darkTheme: Boolean,
    onUpdateSetup: ((MatchSetupConfig) -> MatchSetupConfig) -> Unit,
    onContinueToPlayerSetup: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    BackHandler(onBack = onBack)

    var selectedCount by remember(setup.players.size) { mutableIntStateOf(setup.players.size.coerceIn(2, 6)) }
    var aiDifficulty by remember(setup) {
        mutableStateOf(setup.players.firstOrNull { it.isAi }?.aiDifficulty ?: AiDifficulty.MEDIUM)
    }

    fun applyCountAndMode(count: Int, mode: GameMode, diff: AiDifficulty) {
        selectedCount = count
        aiDifficulty = diff
        val boardType = if (count >= 5) BoardType.SIX_PLAYER else BoardType.FOUR_PLAYER
        val palette = if (boardType == BoardType.SIX_PLAYER) {
            PlayerColor.sixPlayerOrder
        } else {
            PlayerColor.fourPlayerOrder
        }
        val colors = when (count) {
            2 -> listOf(PlayerColor.RED, PlayerColor.YELLOW)
            3 -> listOf(PlayerColor.RED, PlayerColor.GREEN, PlayerColor.YELLOW)
            else -> palette.take(count)
        }
        val players = colors.mapIndexed { idx, color ->
            val isAi = when (mode) {
                GameMode.COMPUTER_VS_AI, GameMode.POWER_MODE -> idx > 0
                GameMode.TEAM_BATTLE -> idx > 0
                else -> false
            }
            val teamId = if (mode == GameMode.TEAM_BATTLE) {
                if (count == 6) (idx % 3) + 1 else (idx % 2) + 1
            } else 0
            PlayerConfig(
                color = color,
                name = if (isAi) "AI ${color.displayName}" else "Player ${idx + 1}",
                avatarId = idx % 8,
                isAi = isAi,
                aiDifficulty = diff,
                teamId = teamId
            )
        }
        onUpdateSetup {
            it.copy(
                gameMode = mode,
                boardType = boardType,
                players = players
            )
        }
    }

    RoyalArenaBackground(darkTheme = darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 700.dp)
                .align(Alignment.TopCenter)
        ) {
            LudoTopBar(title = strings.selectMode, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mode Selection
                GlowingGameCard(accentColor = LudoBlue) {
                    Text(
                        text = "Gameplay Mode",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            GameMode.OFFLINE_CLASSIC to "Offline Pass & Play",
                            GameMode.COMPUTER_VS_AI to "Computer (vs AI)",
                            GameMode.POWER_MODE to "Power Mode",
                            GameMode.TEAM_BATTLE to "Team Mode"
                        ).forEach { (mode, label) ->
                            val selected = setup.gameMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { applyCountAndMode(selectedCount, mode, aiDifficulty) },
                                label = { Text(label, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LudoBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Player Count (2P, 3P, 4P, 5P, 6P)
                GlowingGameCard(accentColor = LudoOrange) {
                    Text(
                        text = "Number of Players",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (selectedCount >= 5) "6-Color Hexagonal Board Active" else "Classic 4-Color 15×15 Board Active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GoldAccent
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 3, 4, 5, 6).forEach { count ->
                            val active = selectedCount == count
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (active) LudoOrange else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.5.dp, if (active) GoldAccent else Color.Transparent),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { applyCountAndMode(count, setup.gameMode, aiDifficulty) }
                                    .testTag("player_count_$count")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${count}P",
                                        fontWeight = FontWeight.Black,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // AI Difficulty
                GlowingGameCard(accentColor = LudoPurple) {
                    Text(
                        text = "AI Bot Difficulty",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AiDifficulty.entries.forEach { diff ->
                            val active = aiDifficulty == diff
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) LudoPurple else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { applyCountAndMode(selectedCount, setup.gameMode, diff) }
                                    .testTag("ai_diff_${diff.name.lowercase()}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = diff.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Power Mode Special Abilities Preview
                if (setup.gameMode == GameMode.POWER_MODE) {
                    GlowingGameCard(accentColor = LudoRed) {
                        Text(
                            text = "Power Mode: 10 Special Abilities",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                        PowerType.entries.forEach { p ->
                            Text(
                                text = "• ${p.title} (${p.shortCode}): ${p.description}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }

                // Configurable Game Rules
                GlowingGameCard(accentColor = LudoGreen) {
                    Text(
                        text = "Match Rules",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    RuleSwitchRow(
                        label = "Spawn on 1 or 6 (otherwise 6 only)",
                        checked = setup.rules.spawnOnOneOrSix,
                        onCheckedChange = { v -> onUpdateSetup { it.copy(rules = it.rules.copy(spawnOnOneOrSix = v)) } }
                    )
                    RuleSwitchRow(
                        label = "Extra Turn on Token Cut",
                        checked = setup.rules.extraTurnOnCapture,
                        onCheckedChange = { v -> onUpdateSetup { it.copy(rules = it.rules.copy(extraTurnOnCapture = v)) } }
                    )
                    RuleSwitchRow(
                        label = "Forfeit Turn on Three 6s",
                        checked = setup.rules.threeSixesForfeit,
                        onCheckedChange = { v -> onUpdateSetup { it.copy(rules = it.rules.copy(threeSixesForfeit = v)) } }
                    )
                    RuleSwitchRow(
                        label = "Star Safe Zones Active",
                        checked = setup.rules.safeStarsEnabled,
                        onCheckedChange = { v -> onUpdateSetup { it.copy(rules = it.rules.copy(safeStarsEnabled = v)) } }
                    )
                    RuleSwitchRow(
                        label = "Protect Stacked Double Tokens",
                        checked = setup.rules.stackProtection,
                        onCheckedChange = { v -> onUpdateSetup { it.copy(rules = it.rules.copy(stackProtection = v)) } }
                    )
                    RuleSwitchRow(
                        label = "Allow Offline Undo (Classic/AI)",
                        checked = setup.rules.allowUndoInOffline,
                        onCheckedChange = { v -> onUpdateSetup { it.copy(rules = it.rules.copy(allowUndoInOffline = v)) } }
                    )
                }
            }

            GlossyButton(
                text = "Choose Players & Colors",
                onClick = onContinueToPlayerSetup,
                topColor = LudoBlue,
                bottomColor = LudoBlueDark,
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                testTag = "continue_to_player_setup",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun RuleSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun PlayerSetupScreen(
    setup: MatchSetupConfig,
    darkTheme: Boolean,
    onUpdateSetup: ((MatchSetupConfig) -> MatchSetupConfig) -> Unit,
    onStartMatch: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    BackHandler(onBack = onBack)

    val availableColors = if (setup.boardType == BoardType.SIX_PLAYER) {
        PlayerColor.sixPlayerOrder
    } else {
        PlayerColor.fourPlayerOrder
    }

    RoyalArenaBackground(darkTheme = darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 700.dp)
                .align(Alignment.TopCenter)
        ) {
            LudoTopBar(title = strings.playerSetup, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                setup.players.forEachIndexed { index, player ->
                    GlowingGameCard(accentColor = player.color.primary) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(player.color.primary)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (player.isAi) Icons.Filled.SmartToy else Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            OutlinedTextField(
                                value = player.name,
                                onValueChange = { raw ->
                                    val clean = raw.take(20)
                                    onUpdateSetup { s ->
                                        val list = s.players.toMutableList()
                                        list[index] = list[index].copy(name = clean)
                                        s.copy(players = list)
                                    }
                                },
                                label = { Text("Seat ${index + 1} Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (player.isAi) LudoPurple else LudoGreen,
                                modifier = Modifier
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onUpdateSetup { s ->
                                            val list = s.players.toMutableList()
                                            list[index] = list[index].copy(isAi = !player.isAi)
                                            s.copy(players = list)
                                        }
                                    }
                                    .testTag("toggle_ai_seat_$index")
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (player.isAi) "AI" else "Human",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // Color picker row (swap if chosen by another player)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Color:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            availableColors.forEach { c ->
                                val selected = player.color == c
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(c.primary)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) GoldAccent else Color.White.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            onUpdateSetup { s ->
                                                val list = s.players.toMutableList()
                                                val otherIdx = list.indexOfFirst { it.color == c }
                                                val prevColor = list[index].color
                                                if (otherIdx >= 0 && otherIdx != index) {
                                                    list[otherIdx] = list[otherIdx].copy(color = prevColor)
                                                }
                                                list[index] = list[index].copy(color = c)
                                                s.copy(players = list)
                                            }
                                        }
                                )
                            }
                            if (setup.gameMode == GameMode.TEAM_BATTLE) {
                                Spacer(Modifier.weight(1f))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (player.teamId == 1) LudoBlue else LudoRed,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onUpdateSetup { s ->
                                                val list = s.players.toMutableList()
                                                val maxTeam = if (s.players.size == 6) 3 else 2
                                                val nextTeam = (player.teamId % maxTeam) + 1
                                                list[index] = list[index].copy(teamId = nextTeam)
                                                s.copy(players = list)
                                            }
                                        }
                                ) {
                                    Text(
                                        text = "Team ${player.teamId.coerceAtLeast(1)}",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            GlossyButton(
                text = strings.startMatch,
                onClick = {
                    onUpdateSetup { s ->
                        s.copy(
                            players = s.players.mapIndexed { idx, p ->
                                p.copy(name = AppSecurityManager.sanitizeNickname(p.name, "Player ${idx + 1}"))
                            }
                        )
                    }
                    onStartMatch()
                },
                topColor = LudoGreen,
                bottomColor = LudoGreenDark,
                icon = Icons.Filled.PlayArrow,
                height = 58.dp,
                testTag = "start_match_button",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}
