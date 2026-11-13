package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stairs
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.SavedMatchEntity
import com.example.i18n.LocalAppStrings
import com.example.ui.components.GlossyButton
import com.example.ui.components.GlowingGameCard
import com.example.ui.components.RoyalArenaBackground
import com.example.ui.components.avatarTitle
import com.example.ui.theme.ArenaCardSurface
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoBlueDark
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoGreenDark
import com.example.ui.theme.LudoOrange
import com.example.ui.theme.LudoOrangeDark
import com.example.ui.theme.LudoPurple
import com.example.ui.theme.LudoPurpleDark
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoRedDark
import com.example.ui.theme.LudoYellowDark

@Composable
fun HomeScreen(
    profile: PlayerProfileEntity,
    savedMatch: SavedMatchEntity?,
    darkTheme: Boolean,
    onPlayNow: () -> Unit,
    onResumeSaved: () -> Unit,
    onOnlineMultiplayer: () -> Unit,
    onOfflineMultiplayer: () -> Unit,
    onPlayWithFriends: () -> Unit,
    onComputerMode: () -> Unit,
    onPowerMode: () -> Unit,
    onTeamMode: () -> Unit,
    onSnakesLadders: () -> Unit,
    onTournament: () -> Unit,
    onLeaderboard: () -> Unit,
    onDailyRewards: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onAboutDeveloper: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

    RoyalArenaBackground(darkTheme = darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter)
        ) {
            // Top Profile & Currency HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = ArenaCardSurface.copy(alpha = 0.88f),
                    border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(onClick = onProfile)
                        .testTag("home_profile_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(LudoBlue, LudoPurple))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = strings.profile,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.nickname,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${avatarTitle(profile.avatarId)} • ${profile.wins}W",
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldAccent,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Coins & Gems Pill
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ArenaCardSurface.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🪙 ${profile.coins}",
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldAccent
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "💎 ${profile.gems}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF66EAFF)
                        )
                    }
                }

                Spacer(Modifier.width(6.dp))

                Surface(
                    shape = CircleShape,
                    color = ArenaCardSurface.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    IconButton(
                        onClick = onSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = strings.settings,
                            tint = Color.White
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_menu_grid")
            ) {
                // Hero Logo Banner
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                            .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = LudoBlue)
                            .clip(RoundedCornerShape(24.dp))
                            .border(2.dp, GoldAccent.copy(alpha = 0.7f), RoundedCornerShape(24.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_ludo_hero),
                            contentDescription = "LUDO Arena Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.15f),
                                            Color(0xFF041033).copy(alpha = 0.85f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LUDO",
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontSize = 38.sp,
                                        letterSpacing = 3.sp
                                    ),
                                    color = GoldAccent
                                )
                                Spacer(Modifier.width(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = LudoRed
                                ) {
                                    Text(
                                        text = "3D PRO",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = strings.subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }

                // Resume Saved Match Banner (if an active offline match is saved)
                if (savedMatch != null) {
                    item(span = { GridItemSpan(2) }) {
                        GlowingGameCard(
                            accentColor = LudoGreen,
                            onClick = onResumeSaved,
                            modifier = Modifier.testTag("resume_saved_match_card")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = LudoGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.resumeMatch,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${savedMatch.gameMode.replace("_", " ")} • Turn ${savedMatch.turnCounter}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "▶",
                                    color = LudoGreen,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Primary Play Now CTA
                item(span = { GridItemSpan(2) }) {
                    GlossyButton(
                        text = strings.playNow,
                        onClick = onPlayNow,
                        topColor = LudoGreen,
                        bottomColor = LudoGreenDark,
                        icon = Icons.Filled.Casino,
                        height = 60.dp,
                        testTag = "play_now_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Snakes & Ladders Featured Mode Tile
                item(span = { GridItemSpan(2) }) {
                    HomeMenuTile(
                        title = strings.snakesAndLadders,
                        subtitle = strings.snakesLaddersSub,
                        icon = Icons.Filled.Stairs,
                        topColor = Color(0xFF0EA5E9),
                        bottomColor = Color(0xFF0369A1),
                        testTag = "menu_snakes_ladders",
                        onClick = onSnakesLadders
                    )
                }

                // Mode Tiles
                item {
                    HomeMenuTile(
                        title = strings.onlineMultiplayer,
                        subtitle = "Rooms & Matchmaking",
                        icon = Icons.Filled.Public,
                        topColor = LudoBlue,
                        bottomColor = LudoBlueDark,
                        testTag = "menu_online",
                        onClick = onOnlineMultiplayer
                    )
                }
                item {
                    HomeMenuTile(
                        title = strings.offlineMultiplayer,
                        subtitle = "2P to 6P Board",
                        icon = Icons.Filled.Groups,
                        topColor = LudoOrange,
                        bottomColor = LudoOrangeDark,
                        testTag = "menu_offline",
                        onClick = onOfflineMultiplayer
                    )
                }
                item {
                    HomeMenuTile(
                        title = strings.playWithFriends,
                        subtitle = "Local Pass & Play",
                        icon = Icons.Filled.Group,
                        topColor = Color(0xFF00B894),
                        bottomColor = Color(0xFF007A62),
                        testTag = "menu_friends",
                        onClick = onPlayWithFriends
                    )
                }
                item {
                    HomeMenuTile(
                        title = strings.computerMode,
                        subtitle = "Easy to Expert AI",
                        icon = Icons.Filled.SmartToy,
                        topColor = LudoPurple,
                        bottomColor = LudoPurpleDark,
                        testTag = "menu_computer",
                        onClick = onComputerMode
                    )
                }
                item {
                    HomeMenuTile(
                        title = strings.powerMode,
                        subtitle = "10 Special Abilities",
                        icon = Icons.Filled.Bolt,
                        topColor = LudoRed,
                        bottomColor = LudoRedDark,
                        testTag = "menu_power",
                        onClick = onPowerMode
                    )
                }
                item {
                    HomeMenuTile(
                        title = "Team Mode",
                        subtitle = "2v2 & 3v3 Co-op",
                        icon = Icons.Filled.AutoAwesome,
                        topColor = Color(0xFFE6A100),
                        bottomColor = LudoYellowDark,
                        testTag = "menu_team",
                        onClick = onTeamMode
                    )
                }
                item {
                    HomeMenuTile(
                        title = strings.tournament,
                        subtitle = "3-Round Cup",
                        icon = Icons.Filled.EmojiEvents,
                        topColor = Color(0xFF3E63DD),
                        bottomColor = Color(0xFF1F3A93),
                        testTag = "menu_tournament",
                        onClick = onTournament
                    )
                }
                item {
                    HomeMenuTile(
                        title = strings.dailyRewards,
                        subtitle = "7-Day Streak Bonus",
                        icon = Icons.Filled.CardGiftcard,
                        topColor = Color(0xFFD93870),
                        bottomColor = Color(0xFF8E1B44),
                        testTag = "menu_daily_rewards",
                        onClick = onDailyRewards
                    )
                }

                // Bottom Utility Row
                item {
                    HomeMenuTile(
                        title = strings.leaderboard,
                        subtitle = "Rankings & Stats",
                        icon = Icons.Filled.Leaderboard,
                        topColor = Color(0xFF218C74),
                        bottomColor = Color(0xFF125948),
                        testTag = "menu_leaderboard",
                        onClick = onLeaderboard
                    )
                }
                item {
                    HomeMenuTile(
                        title = strings.aboutDeveloper,
                        subtitle = "Xadi Ahmed",
                        icon = Icons.Filled.Info,
                        topColor = Color(0xFF3C5388),
                        bottomColor = Color(0xFF1F2F56),
                        testTag = "menu_about_dev",
                        onClick = onAboutDeveloper
                    )
                }

                item(span = { GridItemSpan(2) }) {
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(strings.exitAppTitle, fontWeight = FontWeight.Bold) },
            text = { Text(strings.exitAppMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        showExitDialog = false
                        (context as? Activity)?.finish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LudoRed)
                ) {
                    Text(strings.confirmExit, color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitDialog = false }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun HomeMenuTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    topColor: Color,
    bottomColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .testTag(testTag)
            .shadow(8.dp, shape, ambientColor = topColor)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
            .border(1.5.dp, Color.White.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
