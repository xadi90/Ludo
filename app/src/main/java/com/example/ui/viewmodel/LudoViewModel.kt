package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundManager
import com.example.data.local.AppSettingsEntity
import com.example.data.local.DailyRewardEntity
import com.example.data.local.LudoDatabase
import com.example.data.local.LudoRepository
import com.example.data.local.MatchHistoryEntity
import com.example.data.local.PlayerProfileEntity
import com.example.data.local.SavedMatchEntity
import com.example.data.local.TournamentEntity
import com.example.game.AiDifficulty
import com.example.game.BoardType
import com.example.game.CutEvent
import com.example.game.GameMode
import com.example.game.GameState
import com.example.game.LudoAi
import com.example.game.LudoEngine
import com.example.game.MatchSetupConfig
import com.example.game.PlayerColor
import com.example.game.PlayerConfig
import com.example.game.PowerActivationEvent
import com.example.game.PowerType
import com.example.game.RuleConfig
import com.example.game.TurnPhase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LudoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = LudoDatabase.getInstance(application)
    private val repository = LudoRepository(db.ludoDao())
    val soundManager = SoundManager(application)

    val profile: StateFlow<PlayerProfileEntity> = repository.profileFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerProfileEntity())

    val settings: StateFlow<AppSettingsEntity> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppSettingsEntity())

    val matchHistory: StateFlow<List<MatchHistoryEntity>> = repository.matchHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val savedMatch: StateFlow<SavedMatchEntity?> = repository.savedMatchFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val dailyReward: StateFlow<DailyRewardEntity> = repository.dailyRewardFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DailyRewardEntity())

    val tournament: StateFlow<TournamentEntity> = repository.tournamentFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), TournamentEntity())

    private val _selectedMode = MutableStateFlow(GameMode.OFFLINE_CLASSIC)
    val selectedMode: StateFlow<GameMode> = _selectedMode.asStateFlow()

    private val _pendingSetup = MutableStateFlow(defaultSetupForMode(GameMode.OFFLINE_CLASSIC))
    val pendingSetup: StateFlow<MatchSetupConfig> = _pendingSetup.asStateFlow()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _isDiceRolling = MutableStateFlow(false)
    val isDiceRolling: StateFlow<Boolean> = _isDiceRolling.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _activeCutAnimation = MutableStateFlow<CutEvent?>(null)
    val activeCutAnimation: StateFlow<CutEvent?> = _activeCutAnimation.asStateFlow()

    private val _activePowerAnimation = MutableStateFlow<PowerActivationEvent?>(null)
    val activePowerAnimation: StateFlow<PowerActivationEvent?> = _activePowerAnimation.asStateFlow()

    private val _rewardClaimBanner = MutableStateFlow<String?>(null)
    val rewardClaimBanner: StateFlow<String?> = _rewardClaimBanner.asStateFlow()

    private var aiTurnJob: Job? = null
    private var matchCompletionRecorded = false

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
        viewModelScope.launch {
            settings.collect { s ->
                soundManager.applySettings(
                    musicOn = s.musicEnabled,
                    sfxOn = s.sfxEnabled,
                    vibeOn = s.vibrationEnabled,
                    musicVol = s.musicVolume,
                    sfxVol = s.sfxVolume
                )
            }
        }
    }

    fun selectGameMode(mode: GameMode) {
        soundManager.playButtonClick()
        _selectedMode.value = mode
        val s = settings.value
        val rules = RuleConfig(
            spawnOnOneOrSix = s.spawnOnOneOrSix,
            extraTurnOnSix = true,
            extraTurnOnCapture = s.extraTurnOnCut,
            threeSixesForfeit = s.threeSixesForfeit,
            safeStarsEnabled = s.safeStarsEnabled,
            allowUndoInOffline = s.allowUndo,
            stackProtection = s.stackProtection
        )
        _pendingSetup.value = defaultSetupForMode(mode).copy(rules = rules)
    }

    fun updatePendingSetup(transform: (MatchSetupConfig) -> MatchSetupConfig) {
        _pendingSetup.value = transform(_pendingSetup.value)
    }

    fun startQuickPlay() {
        soundManager.playButtonClick()
        val s = settings.value
        val pName = profile.value.nickname
        val setup = MatchSetupConfig(
            gameMode = GameMode.QUICK_PLAY,
            boardType = BoardType.FOUR_PLAYER,
            players = listOf(
                PlayerConfig(PlayerColor.RED, pName, profile.value.avatarId, isAi = false),
                PlayerConfig(PlayerColor.GREEN, "Bot Nova", 1, isAi = true, aiDifficulty = AiDifficulty.MEDIUM),
                PlayerConfig(PlayerColor.YELLOW, "Bot Rex", 2, isAi = true, aiDifficulty = AiDifficulty.MEDIUM),
                PlayerConfig(PlayerColor.BLUE, "Bot Aria", 3, isAi = true, aiDifficulty = AiDifficulty.MEDIUM)
            ),
            rules = RuleConfig(
                spawnOnOneOrSix = s.spawnOnOneOrSix,
                extraTurnOnCapture = s.extraTurnOnCut,
                threeSixesForfeit = s.threeSixesForfeit,
                safeStarsEnabled = s.safeStarsEnabled,
                allowUndoInOffline = s.allowUndo,
                stackProtection = s.stackProtection
            )
        )
        startMatchWithSetup(setup)
    }

    fun startMatchWithSetup(setup: MatchSetupConfig) {
        aiTurnJob?.cancel()
        matchCompletionRecorded = false
        _isPaused.value = false
        _isDiceRolling.value = false
        _activeCutAnimation.value = null
        _activePowerAnimation.value = null

        val initial = LudoEngine.createInitialState(setup)
        _gameState.value = initial
        viewModelScope.launch {
            repository.saveActiveMatch(initial)
        }
        scheduleAiIfNeeded()
    }

    fun restartCurrentMatch() {
        val current = _gameState.value ?: return
        soundManager.playButtonClick()
        val setup = MatchSetupConfig(
            gameMode = current.gameMode,
            boardType = current.boardType,
            players = current.players.map { it.config },
            rules = current.rules,
            tournamentRound = current.tournamentRound
        )
        startMatchWithSetup(setup)
    }

    fun resumeSavedMatch(onReady: () -> Unit) {
        soundManager.playButtonClick()
        viewModelScope.launch {
            val loaded = repository.loadSavedMatchState()
            if (loaded != null) {
                matchCompletionRecorded = false
                _isPaused.value = false
                _isDiceRolling.value = false
                _gameState.value = loaded
                onReady()
                scheduleAiIfNeeded()
            }
        }
    }

    fun startTournamentMatch(difficulty: AiDifficulty, playerColor: PlayerColor, round: Int) {
        soundManager.playButtonClick()
        viewModelScope.launch {
            if (round == 1) {
                repository.startNewTournament(difficulty, playerColor)
            }
        }
        val order = PlayerColor.fourPlayerOrder
        val otherColors = order.filter { it != playerColor }
        val roundDifficulty = when (round) {
            1 -> difficulty
            2 -> if (difficulty == AiDifficulty.EASY) AiDifficulty.MEDIUM else AiDifficulty.HARD
            else -> AiDifficulty.EXPERT
        }
        val setup = MatchSetupConfig(
            gameMode = GameMode.TOURNAMENT,
            boardType = BoardType.FOUR_PLAYER,
            players = listOf(
                PlayerConfig(playerColor, profile.value.nickname, profile.value.avatarId, isAi = false),
                PlayerConfig(otherColors[0], "QuarterMaster", 1, isAi = true, aiDifficulty = roundDifficulty),
                PlayerConfig(otherColors[1], "GrandShield", 2, isAi = true, aiDifficulty = roundDifficulty),
                PlayerConfig(otherColors[2], "ApexCrown", 3, isAi = true, aiDifficulty = roundDifficulty)
            ),
            rules = RuleConfig(allowUndoInOffline = false),
            tournamentRound = round
        )
        startMatchWithSetup(setup)
    }

    fun setPaused(paused: Boolean) {
        _isPaused.value = paused
        if (!paused) {
            scheduleAiIfNeeded()
        } else {
            aiTurnJob?.cancel()
        }
    }

    fun exitMatchToMenu() {
        aiTurnJob?.cancel()
        _isPaused.value = false
        val state = _gameState.value
        if (state != null && state.phase != TurnPhase.MATCH_OVER) {
            viewModelScope.launch { repository.saveActiveMatch(state) }
        }
    }

    fun onRollDiceClicked() {
        val state = _gameState.value ?: return
        if (_isDiceRolling.value || _isPaused.value) return
        if (state.phase != TurnPhase.WAIT_FOR_ROLL || state.currentPlayer.config.isAi) return
        triggerDiceRoll()
    }

    private fun triggerDiceRoll() {
        if (_isDiceRolling.value) return
        _isDiceRolling.value = true
        soundManager.playDiceRoll()
        viewModelScope.launch {
            delay(360L)
            _isDiceRolling.value = false
            val current = _gameState.value ?: return@launch
            val next = LudoEngine.rollDice(current)
            if (next.lastDiceRoll == 6 && next.phase == TurnPhase.SELECT_TOKEN) {
                soundManager.playNotification()
            }
            applyUpdatedGameState(next)
        }
    }

    fun onTokenTapped(tokenIndex: Int) {
        val state = _gameState.value ?: return
        if (_isDiceRolling.value || _isPaused.value) return
        if (state.phase != TurnPhase.SELECT_TOKEN || state.currentPlayer.config.isAi) return
        if (state.legalMoves.none { it.tokenIndex == tokenIndex }) return
        executeMove(tokenIndex)
    }

    private fun executeMove(tokenIndex: Int) {
        val state = _gameState.value ?: return
        val next = LudoEngine.applyMove(state, tokenIndex)
        soundManager.playTokenMove()

        if (next.lastCutEvent != null) {
            soundManager.playTokenCut()
            triggerCutOverlay(next.lastCutEvent)
        }
        if (next.lastPowerEvent != null) {
            soundManager.playPowerActivate()
            triggerPowerOverlay(next.lastPowerEvent)
        }
        applyUpdatedGameState(next)
    }

    fun onActivatePower(power: PowerType, targetTokenIndex: Int = 0) {
        val state = _gameState.value ?: return
        if (_isPaused.value || state.currentPlayer.config.isAi) return
        val next = LudoEngine.activatePower(state, power, targetTokenIndex)
        if (next != state) {
            soundManager.playPowerActivate()
            next.lastPowerEvent?.let { triggerPowerOverlay(it) }
            next.lastCutEvent?.let {
                soundManager.playTokenCut()
                triggerCutOverlay(it)
            }
            applyUpdatedGameState(next)
        }
    }

    fun onUndoClicked() {
        val state = _gameState.value ?: return
        if (!state.rules.allowUndoInOffline || state.undoSnapshot == null) return
        soundManager.playButtonClick()
        applyUpdatedGameState(LudoEngine.undoLastMove(state))
    }

    private fun applyUpdatedGameState(next: GameState) {
        _gameState.value = next
        viewModelScope.launch {
            repository.saveActiveMatch(next)
        }
        if (next.phase == TurnPhase.MATCH_OVER && !matchCompletionRecorded) {
            matchCompletionRecorded = true
            val humanPlayers = next.players.filter { !it.config.isAi }
            val humanWon = humanPlayers.any { it.config.color in next.winnerColors }
            val humanCuts = humanPlayers.sumOf { it.cutsMade }
            if (humanWon) soundManager.playWinFanfare() else soundManager.playLoseTone()

            viewModelScope.launch {
                repository.recordMatchCompleted(next, humanWon, humanCuts)
                if (next.gameMode == GameMode.TOURNAMENT && next.tournamentRound > 0) {
                    repository.advanceTournamentRound(humanWon)
                }
            }
        } else {
            scheduleAiIfNeeded()
        }
    }

    private fun scheduleAiIfNeeded() {
        aiTurnJob?.cancel()
        val state = _gameState.value ?: return
        if (_isPaused.value || state.phase == TurnPhase.MATCH_OVER) return
        if (!state.currentPlayer.config.isAi) return

        aiTurnJob = viewModelScope.launch {
            val delayMs = state.currentPlayer.config.aiDifficulty.thinkDelayMs
            delay(delayMs)
            val current = _gameState.value ?: return@launch
            if (_isPaused.value || !current.currentPlayer.config.isAi || current.phase == TurnPhase.MATCH_OVER) {
                return@launch
            }

            if (current.phase == TurnPhase.WAIT_FOR_ROLL) {
                val powerToUse = LudoAi.choosePowerToActivate(current)
                val postPower = if (powerToUse != null) {
                    soundManager.playPowerActivate()
                    val pState = LudoEngine.activatePower(current, powerToUse)
                    pState.lastPowerEvent?.let { triggerPowerOverlay(it) }
                    pState
                } else {
                    current
                }
                _gameState.value = postPower

                _isDiceRolling.value = true
                soundManager.playDiceRoll()
                delay(320L)
                _isDiceRolling.value = false
                val rolled = LudoEngine.rollDice(_gameState.value ?: return@launch)
                applyUpdatedGameState(rolled)
            } else if (current.phase == TurnPhase.SELECT_TOKEN) {
                val move = LudoAi.chooseMove(current)
                if (move != null) {
                    executeMove(move.tokenIndex)
                }
            }
        }
    }

    private fun triggerCutOverlay(event: CutEvent) {
        _activeCutAnimation.value = event
        viewModelScope.launch {
            delay(1200L)
            if (_activeCutAnimation.value == event) {
                _activeCutAnimation.value = null
            }
        }
    }

    private fun triggerPowerOverlay(event: PowerActivationEvent) {
        _activePowerAnimation.value = event
        viewModelScope.launch {
            delay(1200L)
            if (_activePowerAnimation.value == event) {
                _activePowerAnimation.value = null
            }
        }
    }

    fun updateProfile(nickname: String, avatarId: Int) {
        soundManager.playButtonClick()
        viewModelScope.launch {
            repository.updateProfileIdentity(nickname, avatarId)
        }
    }

    fun updateSettings(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        viewModelScope.launch {
            repository.updateSettings(transform)
        }
    }

    fun claimTodayReward() {
        soundManager.playPowerActivate()
        val epochDay = System.currentTimeMillis() / 86_400_000L
        viewModelScope.launch {
            val coins = repository.claimDailyReward(epochDay)
            if (coins > 0) {
                _rewardClaimBanner.value = "+$coins Coins claimed!"
                delay(2400L)
                _rewardClaimBanner.value = null
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }

    companion object {
        fun defaultSetupForMode(mode: GameMode): MatchSetupConfig {
            return when (mode) {
                GameMode.TEAM_BATTLE -> MatchSetupConfig(
                    gameMode = mode,
                    boardType = BoardType.FOUR_PLAYER,
                    players = listOf(
                        PlayerConfig(PlayerColor.RED, "You (Team A)", 0, isAi = false, teamId = 1),
                        PlayerConfig(PlayerColor.GREEN, "Bot Viper (Team B)", 1, isAi = true, aiDifficulty = AiDifficulty.MEDIUM, teamId = 2),
                        PlayerConfig(PlayerColor.YELLOW, "Partner Nova (Team A)", 2, isAi = true, aiDifficulty = AiDifficulty.HARD, teamId = 1),
                        PlayerConfig(PlayerColor.BLUE, "Bot Titan (Team B)", 3, isAi = true, aiDifficulty = AiDifficulty.MEDIUM, teamId = 2)
                    )
                )
                GameMode.POWER_MODE -> MatchSetupConfig(
                    gameMode = mode,
                    boardType = BoardType.FOUR_PLAYER,
                    players = listOf(
                        PlayerConfig(PlayerColor.RED, "Player 1", 0, isAi = false),
                        PlayerConfig(PlayerColor.GREEN, "AI Blaze", 1, isAi = true, aiDifficulty = AiDifficulty.MEDIUM),
                        PlayerConfig(PlayerColor.YELLOW, "AI Frost", 2, isAi = true, aiDifficulty = AiDifficulty.MEDIUM),
                        PlayerConfig(PlayerColor.BLUE, "AI Spark", 3, isAi = true, aiDifficulty = AiDifficulty.MEDIUM)
                    ),
                    rules = RuleConfig(allowUndoInOffline = false)
                )
                GameMode.COMPUTER_VS_AI -> MatchSetupConfig(
                    gameMode = mode,
                    boardType = BoardType.FOUR_PLAYER,
                    players = listOf(
                        PlayerConfig(PlayerColor.RED, "You", 0, isAi = false),
                        PlayerConfig(PlayerColor.GREEN, "AI Alpha", 1, isAi = true, aiDifficulty = AiDifficulty.HARD),
                        PlayerConfig(PlayerColor.YELLOW, "AI Beta", 2, isAi = true, aiDifficulty = AiDifficulty.HARD),
                        PlayerConfig(PlayerColor.BLUE, "AI Gamma", 3, isAi = true, aiDifficulty = AiDifficulty.HARD)
                    )
                )
                else -> MatchSetupConfig(
                    gameMode = mode,
                    boardType = BoardType.FOUR_PLAYER,
                    players = listOf(
                        PlayerConfig(PlayerColor.RED, "Player 1", 0, isAi = false),
                        PlayerConfig(PlayerColor.YELLOW, "Player 2", 1, isAi = false)
                    )
                )
            }
        }
    }
}
