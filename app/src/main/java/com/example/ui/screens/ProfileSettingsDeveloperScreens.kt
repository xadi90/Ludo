package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppSettingsEntity
import com.example.data.local.MatchHistoryEntity
import com.example.data.local.PlayerProfileEntity
import com.example.i18n.AppLanguage
import com.example.i18n.LocalAppStrings
import com.example.security.DeveloperIdentity
import com.example.ui.components.GlossyButton
import com.example.ui.components.GlowingGameCard
import com.example.ui.components.LudoTopBar
import com.example.ui.components.RoyalArenaBackground
import com.example.ui.components.avatarTitle
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoBlueDark
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoGreenDark
import com.example.ui.theme.LudoOrange
import com.example.ui.theme.LudoPurple
import com.example.ui.theme.LudoRed
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlayerProfileScreen(
    profile: PlayerProfileEntity,
    history: List<MatchHistoryEntity>,
    darkTheme: Boolean,
    onSaveProfile: (String, Int) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    BackHandler(onBack = onBack)

    var nickname by remember(profile.nickname) { mutableStateOf(profile.nickname) }
    var selectedAvatar by remember(profile.avatarId) { mutableIntStateOf(profile.avatarId) }
    var savedToast by remember { mutableStateOf(false) }

    val winRate = if (profile.totalGames == 0) 0 else ((profile.wins * 100.0) / profile.totalGames).roundToInt()

    RoyalArenaBackground(darkTheme = darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 700.dp)
                .align(Alignment.TopCenter)
        ) {
            LudoTopBar(title = strings.profile, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlowingGameCard(accentColor = LudoBlue) {
                    Text(
                        text = "Player ID: ${profile.playerUid}",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldAccent
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it.take(20); savedToast = false },
                        label = { Text("Nickname") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_nickname_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("Choose Avatar", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (id in 0 until 8) {
                            val active = selectedAvatar == id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) LudoBlue else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.5.dp, if (active) GoldAccent else Color.Transparent),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedAvatar = id; savedToast = false }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = avatarTitle(id),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    GlossyButton(
                        text = if (savedToast) "Profile Saved!" else "Save Profile",
                        onClick = {
                            onSaveProfile(nickname, selectedAvatar)
                            savedToast = true
                        },
                        topColor = LudoGreen,
                        bottomColor = LudoGreenDark,
                        icon = Icons.Filled.Save,
                        testTag = "save_profile_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Career Stats Grid
                GlowingGameCard(accentColor = GoldAccent) {
                    Text("Career Statistics", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatTile("Games", "${profile.totalGames}")
                        StatTile("Wins", "${profile.wins}")
                        StatTile("Losses", "${profile.losses}")
                        StatTile("Win %", "$winRate%")
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StatTile("Tokens Cut", "${profile.tokensCaptured}")
                        StatTile("Best Streak", "${profile.bestStreak}")
                        StatTile("Coins", "${profile.coins}")
                        StatTile("XP", "${profile.xp}")
                    }
                }

                // Achievements
                GlowingGameCard(accentColor = LudoPurple) {
                    Text("Achievements", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    val unlocked = profile.unlockedAchievementsCsv.split(",").toSet()
                    listOf(
                        "FIRST_STEP" to "Welcome to LUDO Arena",
                        "FIRST_MATCH" to "First Match Completed",
                        "FIRST_WIN" to "First Victory Crown",
                        "VETERAN_5" to "5 Career Wins",
                        "BLADE_MASTER" to "10 Opponent Tokens Cut",
                        "STREAK_3" to "3-Win Streak",
                        "SIX_PLAYER_KING" to "6-Player Hexagonal Champion"
                    ).forEach { (code, title) ->
                        val isEarned = code in unlocked
                        Text(
                            text = if (isEarned) "🏆 $title" else "🔒 $title",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isEarned) GoldAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }

                // Recent Match History
                GlowingGameCard(accentColor = LudoOrange) {
                    Text("Match History", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    if (history.isEmpty()) {
                        Text(
                            text = "No completed matches yet. Play a match to build your record!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        history.take(15).forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.mode.replace("_", " ")} (${item.playerCount}P)",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Text(
                                        text = "Winner: ${item.winnerName} • ${item.tokensCut} cuts • ${item.durationSeconds}s",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (item.playerWon) "WIN" else "LOSS",
                                    color = if (item.playerWon) LudoGreen else LudoRed,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleLarge, color = GoldAccent)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettingsEntity,
    onUpdateSettings: ((AppSettingsEntity) -> AppSettingsEntity) -> Unit,
    onAboutDeveloper: () -> Unit,
    onOpenSecretVault: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    BackHandler(onBack = onBack)

    var infoModalTitle by remember { mutableStateOf<String?>(null) }
    var infoModalBody by remember { mutableStateOf("") }

    RoyalArenaBackground(darkTheme = settings.darkThemeMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 700.dp)
                .align(Alignment.TopCenter)
        ) {
            LudoTopBar(title = strings.settings, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlowingGameCard(accentColor = LudoBlue) {
                    Text("Audio & Haptics", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    SettingsToggle(strings.music, settings.musicEnabled) { v ->
                        onUpdateSettings { it.copy(musicEnabled = v) }
                    }
                    Text("Music Volume: ${(settings.musicVolume * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = settings.musicVolume,
                        onValueChange = { v -> onUpdateSettings { it.copy(musicVolume = v) } },
                        enabled = settings.musicEnabled
                    )
                    SettingsToggle(strings.soundEffects, settings.sfxEnabled) { v ->
                        onUpdateSettings { it.copy(sfxEnabled = v) }
                    }
                    Text("SFX Volume: ${(settings.sfxVolume * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = settings.sfxVolume,
                        onValueChange = { v -> onUpdateSettings { it.copy(sfxVolume = v) } },
                        enabled = settings.sfxEnabled
                    )
                    SettingsToggle(strings.vibration, settings.vibrationEnabled) { v ->
                        onUpdateSettings { it.copy(vibrationEnabled = v) }
                    }
                }

                GlowingGameCard(accentColor = LudoPurple) {
                    Text(strings.language, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppLanguage.entries.forEach { lang ->
                            val active = settings.languageCode == lang.code
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) LudoPurple else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onUpdateSettings { it.copy(languageCode = lang.code) } }
                                    .testTag("lang_${lang.code}")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = lang.displayName,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(strings.theme, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(true to strings.darkArena, false to strings.vividLight).forEach { (isDark, label) ->
                            val active = settings.darkThemeMode == isDark
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) LudoBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onUpdateSettings { it.copy(darkThemeMode = isDark) } }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                GlowingGameCard(accentColor = LudoGreen) {
                    Text("Default Game Rules", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(6.dp))
                    SettingsToggle("Spawn on 1 or 6", settings.spawnOnOneOrSix) { v ->
                        onUpdateSettings { it.copy(spawnOnOneOrSix = v) }
                    }
                    SettingsToggle("Extra Turn on Cut", settings.extraTurnOnCut) { v ->
                        onUpdateSettings { it.copy(extraTurnOnCut = v) }
                    }
                    SettingsToggle("Three 6s Forfeit", settings.threeSixesForfeit) { v ->
                        onUpdateSettings { it.copy(threeSixesForfeit = v) }
                    }
                    SettingsToggle("Star Safe Zones", settings.safeStarsEnabled) { v ->
                        onUpdateSettings { it.copy(safeStarsEnabled = v) }
                    }
                    SettingsToggle("Allow Offline Undo", settings.allowUndo) { v ->
                        onUpdateSettings { it.copy(allowUndo = v) }
                    }
                }

                GlowingGameCard(accentColor = GoldAccent) {
                    Text("Legal & Ownership", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(10.dp))
                    SettingsLinkRow(strings.rules) {
                        infoModalTitle = strings.rules
                        infoModalBody = "1. Roll 6 (or 1 if enabled) to exit Base.\n" +
                            "2. Rolling 6 grants a bonus roll; 3 consecutive 6s forfeit your turn.\n" +
                            "3. Landing on an opponent's unprotected token on a non-safe square cuts it back to Base.\n" +
                            "4. Reach the central Home triangle with an exact roll to finish all 4 tokens!"
                    }
                    SettingsLinkRow(strings.privacyPolicy) {
                        infoModalTitle = strings.privacyPolicy
                        infoModalBody = "LUDO stores your local player profile, match history, saved matches, and audio preferences locally on your device using Room SQLite storage. No private credentials or tracking data are collected offline."
                    }
                    SettingsLinkRow(strings.termsAndConditions) {
                        infoModalTitle = strings.termsAndConditions
                        infoModalBody = "LUDO is owned and developed by ${DeveloperIdentity.DEVELOPER_NAME}. Unauthorized modification or redistribution of ownership metadata is prohibited."
                    }
                    SettingsLinkRow(strings.aboutApp) {
                        infoModalTitle = strings.aboutApp
                        infoModalBody = "${DeveloperIdentity.APP_NAME} v${DeveloperIdentity.APP_VERSION}\nUltimate 3D Offline & AI Ludo Arena supporting Classic 4P, Hexagonal 6P, Team Battles, Power Mode, and Championship Tournaments."
                    }
                    SettingsLinkRow(strings.aboutDeveloper, onClick = onAboutDeveloper)
                    SettingsLinkRow(strings.secretVaultTitle, onClick = onOpenSecretVault)
                }
            }
        }
    }

    if (infoModalTitle != null) {
        AlertDialog(
            onDismissRequest = { infoModalTitle = null },
            title = { Text(infoModalTitle ?: "", fontWeight = FontWeight.Bold) },
            text = { Text(infoModalBody) },
            confirmButton = {
                Button(onClick = { infoModalTitle = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SettingsToggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingsLinkRow(title: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("›", style = MaterialTheme.typography.titleLarge, color = GoldAccent)
        }
    }
}

@Composable
fun AboutDeveloperScreen(
    darkTheme: Boolean,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    RoyalArenaBackground(darkTheme = darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 700.dp)
                .align(Alignment.TopCenter)
        ) {
            LudoTopBar(title = strings.aboutDeveloper, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlowingGameCard(accentColor = GoldAccent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(LudoBlue)
                                .border(2.dp, GoldAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Locked Developer Identity",
                                tint = GoldAccent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = DeveloperIdentity.DEVELOPER_NAME,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Official Application Owner & Developer",
                                style = MaterialTheme.typography.bodyMedium,
                                color = GoldAccent
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    LockedIdentityField(strings.developerLabel, DeveloperIdentity.DEVELOPER_NAME)
                    LockedIdentityField(strings.emailLabel, DeveloperIdentity.DEVELOPER_EMAIL)
                    LockedIdentityField(strings.phoneLabel, DeveloperIdentity.DEVELOPER_PHONE)

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = strings.permanentIdentityNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Working Contact Actions
                GlossyButton(
                    text = strings.contactViaEmail,
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${DeveloperIdentity.DEVELOPER_EMAIL}")
                            putExtra(Intent.EXTRA_SUBJECT, "LUDO App Inquiry")
                        }
                        runCatching { context.startActivity(intent) }
                    },
                    topColor = LudoBlue,
                    bottomColor = LudoBlueDark,
                    icon = Icons.Filled.Email,
                    testTag = "contact_email_button",
                    modifier = Modifier.fillMaxWidth()
                )

                GlossyButton(
                    text = strings.callDeveloper,
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${DeveloperIdentity.DEVELOPER_PHONE}")
                        }
                        runCatching { context.startActivity(intent) }
                    },
                    topColor = LudoGreen,
                    bottomColor = LudoGreenDark,
                    icon = Icons.Filled.Call,
                    testTag = "call_developer_button",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LockedIdentityField(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = GoldAccent, modifier = Modifier.size(18.dp))
        }
    }
}
