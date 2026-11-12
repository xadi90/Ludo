package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DailyRewardEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.TournamentEntity
import com.example.game.AiDifficulty
import com.example.game.PlayerColor
import com.example.i18n.LocalAppStrings
import com.example.ui.components.GlossyButton
import com.example.ui.components.GlowingGameCard
import com.example.ui.components.LudoTopBar
import com.example.ui.components.RoyalArenaBackground
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoGreenDark
import com.example.ui.theme.LudoOrange
import com.example.ui.theme.LudoPurple

@Composable
fun TournamentScreen(
    tournament: TournamentEntity,
    darkTheme: Boolean,
    onStartRound: (AiDifficulty, PlayerColor, Int) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    BackHandler(onBack = onBack)

    var selectedDifficulty by remember {
        mutableStateOf(
            runCatching { AiDifficulty.valueOf(tournament.difficulty) }.getOrDefault(AiDifficulty.MEDIUM)
        )
    }
    var selectedColor by remember {
        mutableStateOf(
            runCatching { PlayerColor.valueOf(tournament.playerColor) }.getOrDefault(PlayerColor.RED)
        )
    }

    val activeRound = if (tournament.isActive) tournament.currentRound else 1

    RoyalArenaBackground(darkTheme = darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 700.dp)
                .align(Alignment.TopCenter)
        ) {
            LudoTopBar(title = strings.tournament, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlowingGameCard(accentColor = GoldAccent) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.img_trophy_crown),
                            contentDescription = "Championship Trophy",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "LUDO Grand Championship",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Trophies Won: ${tournament.championshipsWon}",
                                style = MaterialTheme.typography.titleMedium,
                                color = GoldAccent
                            )
                            Text(
                                text = "Win 3 consecutive 4-player matches for 1,000 Coins + 20 Gems!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Bracket Progress
                GlowingGameCard(accentColor = LudoBlue) {
                    Text(
                        text = "Bracket Progress",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(10.dp))
                    listOf(
                        1 to "Round 1: Quarterfinal",
                        2 to "Round 2: Semifinal",
                        3 to "Round 3: Grand Final"
                    ).forEach { (round, title) ->
                        val isCurrent = activeRound == round
                        val isDone = activeRound > round
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isCurrent) LudoBlue.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.5.dp, if (isCurrent) GoldAccent else Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Filled.EmojiEvents,
                                    contentDescription = null,
                                    tint = if (isDone) LudoGreen else if (isCurrent) GoldAccent else Color.Gray
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Color & Difficulty Selection
                GlowingGameCard(accentColor = LudoPurple) {
                    Text("Your Color", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PlayerColor.fourPlayerOrder.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(c.primary)
                                    .border(
                                        width = if (selectedColor == c) 3.dp else 1.dp,
                                        color = if (selectedColor == c) GoldAccent else Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = c }
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Starting Difficulty", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AiDifficulty.entries.forEach { d ->
                            val active = selectedDifficulty == d
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) LudoPurple else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedDifficulty = d }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = d.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            GlossyButton(
                text = "Play Round $activeRound",
                onClick = { onStartRound(selectedDifficulty, selectedColor, activeRound) },
                topColor = LudoGreen,
                bottomColor = LudoGreenDark,
                icon = Icons.Filled.EmojiEvents,
                testTag = "start_tournament_round",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun DailyRewardsScreen(
    dailyReward: DailyRewardEntity,
    claimBanner: String?,
    darkTheme: Boolean,
    onClaimToday: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    BackHandler(onBack = onBack)

    val todayEpoch = System.currentTimeMillis() / 86_400_000L
    val alreadyClaimedToday = dailyReward.lastClaimDayEpoch == todayEpoch
    val currentDay = if (alreadyClaimedToday) dailyReward.streakDay else (dailyReward.streakDay % 7) + 1

    val rewards = listOf(
        1 to "100 Coins + 2 Gems",
        2 to "150 Coins + 2 Gems",
        3 to "250 Coins + 2 Gems",
        4 to "350 Coins + 2 Gems",
        5 to "500 Coins + 2 Gems",
        6 to "750 Coins + 2 Gems",
        7 to "1,200 Coins + 15 Gems!"
    )

    RoyalArenaBackground(darkTheme = darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .widthIn(max = 700.dp)
                .align(Alignment.TopCenter)
        ) {
            LudoTopBar(title = strings.dailyRewards, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (claimBanner != null) {
                    GlowingGameCard(accentColor = LudoGreen) {
                        Text(
                            text = claimBanner,
                            style = MaterialTheme.typography.titleLarge,
                            color = LudoGreen
                        )
                    }
                }

                rewards.forEach { (day, desc) ->
                    val claimed = day < currentDay || (day == currentDay && alreadyClaimedToday)
                    val isToday = day == currentDay && !alreadyClaimedToday
                    GlowingGameCard(
                        accentColor = if (isToday) GoldAccent else if (claimed) LudoGreen else LudoBlue
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (claimed) Icons.Filled.CheckCircle else Icons.Filled.CardGiftcard,
                                contentDescription = null,
                                tint = if (claimed) LudoGreen else if (isToday) GoldAccent else LudoBlue,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Day $day",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = GoldAccent
                                )
                            }
                            if (claimed) {
                                Text("CLAIMED", style = MaterialTheme.typography.labelMedium, color = LudoGreen)
                            } else if (isToday) {
                                Text("READY", style = MaterialTheme.typography.labelMedium, color = GoldAccent)
                            }
                        }
                    }
                }
            }

            GlossyButton(
                text = if (alreadyClaimedToday) strings.claimedToday else strings.claimReward,
                onClick = onClaimToday,
                enabled = !alreadyClaimedToday,
                topColor = LudoOrange,
                bottomColor = Color(0xFFC25400),
                icon = Icons.Filled.CardGiftcard,
                testTag = "claim_daily_reward_button",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun LeaderboardScreen(
    profile: PlayerProfileEntity,
    darkTheme: Boolean,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    BackHandler(onBack = onBack)

    val playerScore = profile.wins * 150 + profile.tokensCaptured * 25 + profile.xp / 2
    val entries = remember(profile) {
        listOf(
            Triple(profile.nickname, "${profile.wins} Wins • ${profile.tokensCaptured} Cuts", playerScore),
            Triple("Grandmaster Apex (AI)", "Expert League • 14 Wins", 2400),
            Triple("Viper Queen (AI)", "Hard League • 10 Wins", 1750),
            Triple("Nova Knight (AI)", "Medium League • 6 Wins", 980),
            Triple("Rookie Spark (AI)", "Easy League • 2 Wins", 350)
        ).sortedByDescending { it.third }
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
            LudoTopBar(title = strings.leaderboard, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlowingGameCard(accentColor = LudoBlue) {
                    Text(
                        text = "Local Arena Rankings",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Global cloud leaderboards require Firebase provisioning (declined during setup). Your offline career score updates after every match.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                entries.forEachIndexed { idx, (name, meta, score) ->
                    val isMe = name == profile.nickname
                    GlowingGameCard(accentColor = if (isMe) GoldAccent else LudoBlue) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${idx + 1}",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (idx == 0) GoldAccent else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(42.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isMe) "$name (You)" else name,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isMe) GoldAccent else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = meta,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "$score pts",
                                style = MaterialTheme.typography.labelLarge,
                                color = LudoGreen
                            )
                        }
                    }
                }
            }
        }
    }
}
