package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.i18n.LocalAppStrings
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
import com.example.ui.theme.LudoPurpleDark

@Composable
fun OnlineMultiplayerScreen(
    darkTheme: Boolean,
    onPlayFriendsOffline: () -> Unit,
    onPlayVsAi: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
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
            LudoTopBar(title = strings.onlineMultiplayer, onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlowingGameCard(accentColor = LudoOrange) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = LudoOrange,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Firebase Backend Required",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Online matchmaking, private 6-character rooms, cloud reconnection, and server-validated dice require Firebase Firestore and Google Sign-In, which were declined during project setup. LUDO does not simulate fake online players.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                GlowingGameCard(accentColor = LudoBlue) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Backend Setup Instructions",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "1. Create a Firebase project and register Android package ID with your release/debug SHA-1 and SHA-256 certificates.\n" +
                            "2. Enable Google Sign-In in Firebase Authentication and provision Cloud Firestore.\n" +
                            "3. Deploy server-authoritative Cloud Functions for dice generation (SecureRandom on server), turn validation, rate limiting, and disconnect timeouts.\n" +
                            "4. Apply zero-trust Firestore Security Rules restricting room writes to authenticated participants (request.auth.uid in resource.data.memberUids).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                GlowingGameCard(accentColor = LudoGreen) {
                    Text(
                        text = "Play Offline Right Now",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "All 2P–6P boards, Computer AI (Easy to Expert), Team Battles, Power Mode, and Tournaments work 100% offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    GlossyButton(
                        text = "Play With Friends (Pass & Play)",
                        onClick = onPlayFriendsOffline,
                        topColor = LudoGreen,
                        bottomColor = LudoGreenDark,
                        icon = Icons.Filled.Group,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    GlossyButton(
                        text = "Play Computer AI",
                        onClick = onPlayVsAi,
                        topColor = LudoPurple,
                        bottomColor = LudoPurpleDark,
                        icon = Icons.Filled.SmartToy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
