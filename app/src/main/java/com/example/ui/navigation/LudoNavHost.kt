package com.example.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.game.GameMode
import com.example.ui.screens.AboutDeveloperScreen
import com.example.ui.screens.DailyRewardsScreen
import com.example.ui.screens.GameModeScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LeaderboardScreen
import com.example.ui.screens.OnlineMultiplayerScreen
import com.example.ui.screens.PlayerProfileScreen
import com.example.ui.screens.PlayerSetupScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SnakesLaddersScreen
import com.example.ui.screens.TournamentScreen
import com.example.ui.viewmodel.LudoViewModel

object LudoRoutes {
    const val ROUTE_HOME = "home"
    const val ROUTE_GAME_MODE = "game_mode"
    const val ROUTE_PLAYER_SETUP = "player_setup"
    const val ROUTE_GAME = "game"
    const val ROUTE_ONLINE = "online"
    const val ROUTE_TOURNAMENT = "tournament"
    const val ROUTE_LEADERBOARD = "leaderboard"
    const val ROUTE_DAILY_REWARDS = "daily_rewards"
    const val ROUTE_PROFILE = "profile"
    const val ROUTE_SETTINGS = "settings"
    const val ROUTE_ABOUT_DEVELOPER = "about_developer"
    const val ROUTE_SNAKES_LADDERS = "snakes_ladders"
}

@Composable
fun LudoNavHost(
    viewModel: LudoViewModel,
    navController: NavHostController = rememberNavController()
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val matchHistory by viewModel.matchHistory.collectAsStateWithLifecycle()
    val savedMatch by viewModel.savedMatch.collectAsStateWithLifecycle()
    val dailyReward by viewModel.dailyReward.collectAsStateWithLifecycle()
    val tournament by viewModel.tournament.collectAsStateWithLifecycle()
    val pendingSetup by viewModel.pendingSetup.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val isDiceRolling by viewModel.isDiceRolling.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
    val activeCut by viewModel.activeCutAnimation.collectAsStateWithLifecycle()
    val activePower by viewModel.activePowerAnimation.collectAsStateWithLifecycle()
    val rewardClaimBanner by viewModel.rewardClaimBanner.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = LudoRoutes.ROUTE_HOME,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it / 5 }, animationSpec = tween(240)) + fadeIn(tween(220))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 6 }, animationSpec = tween(220)) + fadeOut(tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 6 }, animationSpec = tween(240)) + fadeIn(tween(220))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it / 5 }, animationSpec = tween(220)) + fadeOut(tween(200))
        }
    ) {
        composable(LudoRoutes.ROUTE_HOME) {
            HomeScreen(
                profile = profile,
                savedMatch = savedMatch,
                darkTheme = settings.darkThemeMode,
                onPlayNow = {
                    viewModel.startQuickPlay()
                    navController.navigate(LudoRoutes.ROUTE_GAME)
                },
                onResumeSaved = {
                    viewModel.resumeSavedMatch {
                        navController.navigate(LudoRoutes.ROUTE_GAME)
                    }
                },
                onOnlineMultiplayer = {
                    viewModel.soundManager.playButtonClick()
                    navController.navigate(LudoRoutes.ROUTE_ONLINE)
                },
                onOfflineMultiplayer = {
                    viewModel.selectGameMode(GameMode.OFFLINE_CLASSIC)
                    navController.navigate(LudoRoutes.ROUTE_GAME_MODE)
                },
                onPlayWithFriends = {
                    viewModel.selectGameMode(GameMode.OFFLINE_CLASSIC)
                    navController.navigate(LudoRoutes.ROUTE_GAME_MODE)
                },
                onComputerMode = {
                    viewModel.selectGameMode(GameMode.COMPUTER_VS_AI)
                    navController.navigate(LudoRoutes.ROUTE_GAME_MODE)
                },
                onPowerMode = {
                    viewModel.selectGameMode(GameMode.POWER_MODE)
                    navController.navigate(LudoRoutes.ROUTE_GAME_MODE)
                },
                onTeamMode = {
                    viewModel.selectGameMode(GameMode.TEAM_BATTLE)
                    navController.navigate(LudoRoutes.ROUTE_GAME_MODE)
                },
                onSnakesLadders = {
                    viewModel.soundManager.playButtonClick()
                    navController.navigate(LudoRoutes.ROUTE_SNAKES_LADDERS)
                },
                onTournament = {
                    viewModel.soundManager.playButtonClick()
                    navController.navigate(LudoRoutes.ROUTE_TOURNAMENT)
                },
                onLeaderboard = {
                    viewModel.soundManager.playButtonClick()
                    navController.navigate(LudoRoutes.ROUTE_LEADERBOARD)
                },
                onDailyRewards = {
                    viewModel.soundManager.playButtonClick()
                    navController.navigate(LudoRoutes.ROUTE_DAILY_REWARDS)
                },
                onSettings = {
                    viewModel.soundManager.playButtonClick()
                    navController.navigate(LudoRoutes.ROUTE_SETTINGS)
                },
                onProfile = {
                    viewModel.soundManager.playButtonClick()
                    navController.navigate(LudoRoutes.ROUTE_PROFILE)
                },
                onAboutDeveloper = {
                    viewModel.soundManager.playButtonClick()
                    navController.navigate(LudoRoutes.ROUTE_ABOUT_DEVELOPER)
                }
            )
        }

        composable(LudoRoutes.ROUTE_GAME_MODE) {
            GameModeScreen(
                setup = pendingSetup,
                darkTheme = settings.darkThemeMode,
                onUpdateSetup = viewModel::updatePendingSetup,
                onContinueToPlayerSetup = {
                    viewModel.soundManager.playButtonClick()
                    navController.navigate(LudoRoutes.ROUTE_PLAYER_SETUP)
                },
                onBack = {
                    viewModel.soundManager.playButtonClick()
                    navController.popBackStack()
                }
            )
        }

        composable(LudoRoutes.ROUTE_PLAYER_SETUP) {
            PlayerSetupScreen(
                setup = pendingSetup,
                darkTheme = settings.darkThemeMode,
                onUpdateSetup = viewModel::updatePendingSetup,
                onStartMatch = {
                    viewModel.soundManager.playButtonClick()
                    viewModel.startMatchWithSetup(viewModel.pendingSetup.value)
                    navController.navigate(LudoRoutes.ROUTE_GAME) {
                        popUpTo(LudoRoutes.ROUTE_HOME)
                    }
                },
                onBack = {
                    viewModel.soundManager.playButtonClick()
                    navController.popBackStack()
                }
            )
        }

        composable(LudoRoutes.ROUTE_GAME) {
            GameScreen(
                gameState = gameState,
                settings = settings,
                isDiceRolling = isDiceRolling,
                isPaused = isPaused,
                activeCut = activeCut,
                activePower = activePower,
                onRollDice = viewModel::onRollDiceClicked,
                onTokenTapped = viewModel::onTokenTapped,
                onActivatePower = { power -> viewModel.onActivatePower(power) },
                onUndo = viewModel::onUndoClicked,
                onPauseChanged = viewModel::setPaused,
                onRestartMatch = viewModel::restartCurrentMatch,
                onExitMatch = {
                    viewModel.exitMatchToMenu()
                    navController.popBackStack(LudoRoutes.ROUTE_HOME, inclusive = false)
                },
                onToggleSfx = {
                    viewModel.updateSettings { it.copy(sfxEnabled = !it.sfxEnabled) }
                },
                onToggleVibration = {
                    viewModel.updateSettings { it.copy(vibrationEnabled = !it.vibrationEnabled) }
                },
                onAdvanceTournament = {
                    val nextRound = (gameState?.tournamentRound ?: 1) + 1
                    val diff = runCatching { com.example.game.AiDifficulty.valueOf(tournament.difficulty) }
                        .getOrDefault(com.example.game.AiDifficulty.MEDIUM)
                    val color = runCatching { com.example.game.PlayerColor.valueOf(tournament.playerColor) }
                        .getOrDefault(com.example.game.PlayerColor.RED)
                    viewModel.startTournamentMatch(diff, color, nextRound.coerceAtMost(3))
                }
            )
        }

        composable(LudoRoutes.ROUTE_ONLINE) {
            OnlineMultiplayerScreen(
                darkTheme = settings.darkThemeMode,
                onPlayFriendsOffline = {
                    viewModel.selectGameMode(GameMode.OFFLINE_CLASSIC)
                    navController.navigate(LudoRoutes.ROUTE_GAME_MODE)
                },
                onPlayVsAi = {
                    viewModel.selectGameMode(GameMode.COMPUTER_VS_AI)
                    navController.navigate(LudoRoutes.ROUTE_GAME_MODE)
                },
                onBack = {
                    viewModel.soundManager.playButtonClick()
                    navController.popBackStack()
                }
            )
        }

        composable(LudoRoutes.ROUTE_TOURNAMENT) {
            TournamentScreen(
                tournament = tournament,
                darkTheme = settings.darkThemeMode,
                onStartRound = { diff, color, round ->
                    viewModel.startTournamentMatch(diff, color, round)
                    navController.navigate(LudoRoutes.ROUTE_GAME) {
                        popUpTo(LudoRoutes.ROUTE_HOME)
                    }
                },
                onBack = {
                    viewModel.soundManager.playButtonClick()
                    navController.popBackStack()
                }
            )
        }

        composable(LudoRoutes.ROUTE_LEADERBOARD) {
            LeaderboardScreen(
                profile = profile,
                darkTheme = settings.darkThemeMode,
                onBack = {
                    viewModel.soundManager.playButtonClick()
                    navController.popBackStack()
                }
            )
        }

        composable(LudoRoutes.ROUTE_DAILY_REWARDS) {
            DailyRewardsScreen(
                dailyReward = dailyReward,
                claimBanner = rewardClaimBanner,
                darkTheme = settings.darkThemeMode,
                onClaimToday = viewModel::claimTodayReward,
                onBack = {
                    viewModel.soundManager.playButtonClick()
                    navController.popBackStack()
                }
            )
        }

        composable(LudoRoutes.ROUTE_PROFILE) {
            PlayerProfileScreen(
                profile = profile,
                history = matchHistory,
                darkTheme = settings.darkThemeMode,
                onSaveProfile = viewModel::updateProfile,
                onBack = {
                    viewModel.soundManager.playButtonClick()
                    navController.popBackStack()
                }
            )
        }

        composable(LudoRoutes.ROUTE_SETTINGS) {
            SettingsScreen(
                settings = settings,
                onUpdateSettings = viewModel::updateSettings,
                onAboutDeveloper = {
                    viewModel.soundManager.playButtonClick()
                    navController.navigate(LudoRoutes.ROUTE_ABOUT_DEVELOPER)
                },
                onBack = {
                    viewModel.soundManager.playButtonClick()
                    navController.popBackStack()
                }
            )
        }

        composable(LudoRoutes.ROUTE_ABOUT_DEVELOPER) {
            AboutDeveloperScreen(
                darkTheme = settings.darkThemeMode,
                onBack = {
                    viewModel.soundManager.playButtonClick()
                    navController.popBackStack()
                }
            )
        }

        composable(LudoRoutes.ROUTE_SNAKES_LADDERS) {
            SnakesLaddersScreen(
                playerName = profile.nickname,
                settings = settings,
                soundManager = viewModel.soundManager,
                onToggleSfx = {
                    viewModel.updateSettings { it.copy(sfxEnabled = !it.sfxEnabled) }
                },
                onToggleVibration = {
                    viewModel.updateSettings { it.copy(vibrationEnabled = !it.vibrationEnabled) }
                },
                onBack = {
                    viewModel.soundManager.playButtonClick()
                    navController.popBackStack()
                }
            )
        }
    }
}
