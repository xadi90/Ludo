package com.example.game

import java.security.SecureRandom
import java.util.UUID

/**
 * Pure Kotlin Ludo Rules & Power Engine.
 * Handles dice rolling, legal move calculation, exact home entry,
 * safe zones, token cutting, 10 Power Mode abilities, team mode rules,
 * consecutive sixes forfeit, and deterministic undo snapshots.
 */
object LudoEngine {

    private val secureRandom = SecureRandom()

    fun rollSecureDie(): Int = secureRandom.nextInt(6) + 1

    fun createInitialState(setup: MatchSetupConfig): GameState {
        val playerStates = setup.players.map { config ->
            val starterPowers = if (setup.gameMode == GameMode.POWER_MODE) {
                listOf(PowerType.SHIELD, PowerType.DOUBLE_X2, PowerType.ROCKET)
            } else {
                emptyList()
            }
            PlayerMatchState(
                config = config,
                tokens = (0 until 4).map { idx ->
                    TokenState(ownerColor = config.color, tokenIndex = idx, progress = -1)
                },
                activePowerInventory = starterPowers
            )
        }

        val boardPowers = if (setup.gameMode == GameMode.POWER_MODE) {
            generatePowerTiles(setup.boardType, setup.rules.safeStarsEnabled)
        } else {
            emptyMap()
        }

        val firstPlayer = playerStates.first()
        return GameState(
            matchId = UUID.randomUUID().toString(),
            gameMode = setup.gameMode,
            boardType = setup.boardType,
            rules = setup.rules,
            players = playerStates,
            currentTurnIndex = 0,
            phase = TurnPhase.WAIT_FOR_ROLL,
            boardPowers = boardPowers,
            statusBanner = "${firstPlayer.config.name}'s turn — Roll the dice!",
            tournamentRound = setup.tournamentRound
        )
    }

    private fun generatePowerTiles(boardType: BoardType, safeStarsEnabled: Boolean): Map<Int, PowerType> {
        val powers = PowerType.entries
        val candidates = (0 until boardType.trackLength).filter { idx ->
            !BoardGeometry.isSafeGlobalIndex(boardType, idx, safeStarsEnabled)
        }.shuffled(secureRandom)

        val count = if (boardType == BoardType.SIX_PLAYER) 14 else 10
        val map = mutableMapOf<Int, PowerType>()
        candidates.take(count).forEachIndexed { index, tileIdx ->
            map[tileIdx] = powers[index % powers.size]
        }
        return map
    }

    fun rollDice(state: GameState, forcedRoll: Int? = null): GameState {
        // Prevent double execution of dice rolls while waiting for token selection or after match over
        if (state.phase != TurnPhase.WAIT_FOR_ROLL) return state

        val current = state.currentPlayer
        if (current.frozenTurns > 0) {
            val updatedPlayers = state.players.toMutableList()
            updatedPlayers[state.currentTurnIndex] = current.copy(frozenTurns = current.frozenTurns - 1)
            val nextIdx = findNextActivePlayerIndex(updatedPlayers, state.currentTurnIndex, state.boardType)
            val nextName = updatedPlayers[nextIdx].config.name
            return advanceTurnCleanly(
                state.copy(
                    players = updatedPlayers,
                    lastDiceRoll = null,
                    consecutiveSixes = 0,
                    statusBanner = "${current.config.name} is frozen! Turn goes to $nextName"
                ),
                nextIdx
            )
        }

        val roll = forcedRoll?.coerceIn(1, 6) ?: rollSecureDie()
        val newConsecutiveSixes = if (roll == 6) state.consecutiveSixes + 1 else 0

        if (state.rules.threeSixesForfeit && newConsecutiveSixes >= 3) {
            val nextIdx = findNextActivePlayerIndex(state.players, state.currentTurnIndex, state.boardType)
            val nextName = state.players[nextIdx].config.name
            return advanceTurnCleanly(
                state.copy(
                    lastDiceRoll = roll,
                    consecutiveSixes = 0,
                    legalMoves = emptyList(),
                    statusBanner = "Three 6s in a row! ${current.config.name}'s turn forfeited -> $nextName"
                ),
                nextIdx
            )
        }

        val tempState = state.copy(
            lastDiceRoll = roll,
            consecutiveSixes = newConsecutiveSixes
        )
        val moves = calculateLegalMoves(tempState, roll)

        if (moves.isEmpty()) {
            val keepsTurn = roll == 6 && state.rules.extraTurnOnSix
            if (keepsTurn) {
                return tempState.copy(
                    phase = TurnPhase.WAIT_FOR_ROLL,
                    legalMoves = emptyList(),
                    statusBanner = "${current.config.name} rolled 6! Roll again."
                )
            }
            val nextIdx = findNextActivePlayerIndex(state.players, state.currentTurnIndex, state.boardType)
            val nextName = state.players[nextIdx].config.name
            return advanceTurnCleanly(
                tempState.copy(
                    legalMoves = emptyList(),
                    statusBanner = "${current.config.name} rolled $roll (no moves) -> $nextName"
                ),
                nextIdx
            )
        }

        return tempState.copy(
            phase = TurnPhase.SELECT_TOKEN,
            legalMoves = moves,
            lastCutEvent = null,
            lastPowerEvent = null,
            statusBanner = "${current.config.name} rolled $roll! Select a token."
        )
    }

    fun calculateLegalMoves(state: GameState, roll: Int): List<LegalMove> {
        val player = state.currentPlayer
        val boardType = state.boardType
        val multiplier = player.pendingMultiplier.coerceAtLeast(1)
        val bonusSteps = player.pendingBonusSteps.coerceAtLeast(0)
        val effectiveSteps = roll * multiplier + bonusSteps

        val canSpawn = roll == 6 || (state.rules.spawnOnOneOrSix && roll == 1)

        return player.tokens.mapNotNull { token ->
            if (token.isFinished(boardType)) {
                null
            } else if (token.isInBase()) {
                if (canSpawn) {
                    val destProgress = 0
                    val destGlobal = BoardGeometry.toGlobalTrackIndex(boardType, player.config.color, destProgress)
                    val willCapture = destGlobal != null && hasCuttableOpponents(state, player, destGlobal)
                    val landedPower = destGlobal?.let { state.boardPowers[it] }
                    LegalMove(
                        tokenIndex = token.tokenIndex,
                        fromProgress = -1,
                        toProgress = destProgress,
                        effectiveSteps = 1,
                        willCapture = willCapture,
                        willReachHome = false,
                        landedPower = landedPower
                    )
                } else {
                    null
                }
            } else {
                val target = token.progress + effectiveSteps
                if (target > boardType.homeFinishProgress) {
                    null
                } else {
                    val destGlobal = BoardGeometry.toGlobalTrackIndex(boardType, player.config.color, target)
                    val willCapture = destGlobal != null && hasCuttableOpponents(state, player, destGlobal)
                    val willReachHome = target == boardType.homeFinishProgress
                    val landedPower = destGlobal?.let { state.boardPowers[it] }
                    LegalMove(
                        tokenIndex = token.tokenIndex,
                        fromProgress = token.progress,
                        toProgress = target,
                        effectiveSteps = effectiveSteps,
                        willCapture = willCapture,
                        willReachHome = willReachHome,
                        landedPower = landedPower
                    )
                }
            }
        }
    }

    private fun hasCuttableOpponents(
        state: GameState,
        attacker: PlayerMatchState,
        globalIndex: Int
    ): Boolean {
        if (BoardGeometry.isSafeGlobalIndex(state.boardType, globalIndex, state.rules.safeStarsEnabled)) {
            return false
        }
        return state.players.any { other ->
            if (!isOpponent(attacker.config, other.config)) return@any false
            val matchingTokens = other.tokens.filter { t ->
                t.isOnMainTrack(state.boardType) &&
                    BoardGeometry.toGlobalTrackIndex(state.boardType, other.config.color, t.progress) == globalIndex
            }
            if (matchingTokens.isEmpty()) return@any false
            if (state.rules.stackProtection && matchingTokens.size >= 2) return@any false
            matchingTokens.any { it.shieldTurns <= 0 }
        }
    }

    fun isOpponent(a: PlayerConfig, b: PlayerConfig): Boolean {
        if (a.color == b.color) return false
        if (a.teamId != 0 && a.teamId == b.teamId) return false
        return true
    }

    fun applyMove(state: GameState, tokenIndex: Int): GameState {
        if (state.phase != TurnPhase.SELECT_TOKEN) return state
        val move = state.legalMoves.firstOrNull { it.tokenIndex == tokenIndex } ?: return state

        val snapshotForUndo = if (state.rules.allowUndoInOffline && state.gameMode != GameMode.POWER_MODE) {
            state.copy(undoSnapshot = null)
        } else {
            null
        }

        val updatedPlayers = state.players.toMutableList()
        val moverIndex = state.currentTurnIndex
        var mover = updatedPlayers[moverIndex]

        // Move token and reset single-roll multipliers
        val movedTokens = mover.tokens.map { token ->
            if (token.tokenIndex == tokenIndex) token.copy(progress = move.toProgress) else token
        }
        var scoreGain = if (move.willReachHome) 100 else move.effectiveSteps * 2
        mover = mover.copy(
            tokens = movedTokens,
            score = mover.score + scoreGain,
            pendingMultiplier = 1,
            pendingBonusSteps = 0
        )
        updatedPlayers[moverIndex] = mover

        var cutEvent: CutEvent? = null
        var gotCutBonusTurn = false
        var statusMsg = "${mover.config.name} moved token #${tokenIndex + 1}."

        val destGlobal = BoardGeometry.toGlobalTrackIndex(state.boardType, mover.config.color, move.toProgress)
        if (destGlobal != null && !BoardGeometry.isSafeGlobalIndex(state.boardType, destGlobal, state.rules.safeStarsEnabled)) {
            for (i in updatedPlayers.indices) {
                val other = updatedPlayers[i]
                if (!isOpponent(mover.config, other.config)) continue

                val matching = other.tokens.filter { t ->
                    t.isOnMainTrack(state.boardType) &&
                        BoardGeometry.toGlobalTrackIndex(state.boardType, other.config.color, t.progress) == destGlobal
                }
                if (matching.isEmpty()) continue
                if (state.rules.stackProtection && matching.size >= 2) continue

                val cuttableIds = matching.filter { it.shieldTurns <= 0 }.map { it.tokenIndex }.toSet()
                if (cuttableIds.isNotEmpty()) {
                    val victimTokens = other.tokens.map { t ->
                        if (t.tokenIndex in cuttableIds) t.copy(progress = -1, shieldTurns = 0) else t
                    }
                    updatedPlayers[i] = other.copy(tokens = victimTokens)
                    val firstCutIdx = cuttableIds.first()
                    cutEvent = CutEvent(
                        attackerColor = mover.config.color,
                        victimColor = other.config.color,
                        victimTokenIndex = firstCutIdx,
                        globalTileIndex = destGlobal
                    )
                    mover = updatedPlayers[moverIndex].copy(
                        cutsMade = updatedPlayers[moverIndex].cutsMade + cuttableIds.size,
                        score = updatedPlayers[moverIndex].score + 120 * cuttableIds.size
                    )
                    updatedPlayers[moverIndex] = mover
                    if (state.rules.extraTurnOnCapture) gotCutBonusTurn = true
                    statusMsg = "TOKEN CUT! ${mover.config.name} captured ${other.config.name}!"
                    break
                }
            }
        }

        // Power tile pickup & activation in Power Mode
        var updatedPowers = state.boardPowers
        var powerEvent: PowerActivationEvent? = null
        var extraTurnFromPower = false
        if (state.gameMode == GameMode.POWER_MODE && destGlobal != null) {
            val tilePower = state.boardPowers[destGlobal]
            if (tilePower != null) {
                updatedPowers = state.boardPowers.toMutableMap().apply { remove(destGlobal) }
                val currentInv = updatedPlayers[moverIndex].activePowerInventory
                if (tilePower == PowerType.LUCKY_STAR) {
                    updatedPlayers[moverIndex] = updatedPlayers[moverIndex].copy(
                        score = updatedPlayers[moverIndex].score + 150
                    )
                    extraTurnFromPower = true
                    powerEvent = PowerActivationEvent(
                        playerColor = mover.config.color,
                        powerType = tilePower,
                        targetTileIndex = destGlobal,
                        summary = "${mover.config.name} hit Lucky Star! +150 pts & Extra Turn!"
                    )
                    statusMsg = powerEvent.summary
                } else if (currentInv.size < 4) {
                    updatedPlayers[moverIndex] = updatedPlayers[moverIndex].copy(
                        activePowerInventory = currentInv + tilePower
                    )
                    powerEvent = PowerActivationEvent(
                        playerColor = mover.config.color,
                        powerType = tilePower,
                        targetTileIndex = destGlobal,
                        summary = "${mover.config.name} collected ${tilePower.title}!"
                    )
                    statusMsg = powerEvent.summary
                }
            }
        }

        // Check home entry bonus
        var extraTurnHomeEntry = false
        if (move.willReachHome) {
            extraTurnHomeEntry = true
            statusMsg = "${mover.config.name} reached HOME!"
        }

        // Update finish ranks
        val highestRank = updatedPlayers.maxOf { it.finishRank }
        if (updatedPlayers[moverIndex].hasFinishedAll(state.boardType) && updatedPlayers[moverIndex].finishRank == 0) {
            updatedPlayers[moverIndex] = updatedPlayers[moverIndex].copy(
                finishRank = highestRank + 1,
                score = updatedPlayers[moverIndex].score + 500
            )
        }

        // Check Match / Team victory conditions
        val winCheck = evaluateVictory(updatedPlayers, state.boardType, state.gameMode)
        if (winCheck != null) {
            return state.copy(
                players = updatedPlayers,
                phase = TurnPhase.MATCH_OVER,
                legalMoves = emptyList(),
                boardPowers = updatedPowers,
                lastCutEvent = cutEvent,
                lastPowerEvent = powerEvent,
                winnerColors = winCheck.first,
                winningTeamId = winCheck.second,
                statusBanner = "VICTORY! ${winCheck.first.joinToString(" & ") { it.displayName }} wins!",
                undoSnapshot = snapshotForUndo
            )
        }

        val rolledSix = state.lastDiceRoll == 6 && state.rules.extraTurnOnSix
        val getsExtraTurn = (rolledSix || gotCutBonusTurn || extraTurnHomeEntry || extraTurnFromPower) &&
            !updatedPlayers[moverIndex].hasFinishedAll(state.boardType)

        if (getsExtraTurn) {
            return state.copy(
                players = updatedPlayers,
                phase = TurnPhase.WAIT_FOR_ROLL,
                legalMoves = emptyList(),
                boardPowers = updatedPowers,
                lastCutEvent = cutEvent,
                lastPowerEvent = powerEvent,
                statusBanner = "$statusMsg Bonus roll!",
                undoSnapshot = snapshotForUndo
            )
        }

        val nextIdx = findNextActivePlayerIndex(updatedPlayers, moverIndex, state.boardType)
        val cooledPlayers = decrementRoundEffects(updatedPlayers, moverIndex)
        return advanceTurnCleanly(
            state.copy(
                players = cooledPlayers,
                boardPowers = updatedPowers,
                lastCutEvent = cutEvent,
                lastPowerEvent = powerEvent,
                statusBanner = "$statusMsg -> ${cooledPlayers[nextIdx].config.name}'s turn",
                undoSnapshot = snapshotForUndo
            ),
            nextIdx
        )
    }

    /**
     * Activates one of the 10 Power Mode abilities before rolling or moving.
     */
    fun activatePower(state: GameState, power: PowerType, targetTokenIndex: Int = 0): GameState {
        if (state.gameMode != GameMode.POWER_MODE || state.phase == TurnPhase.MATCH_OVER) return state
        val idx = state.currentTurnIndex
        val player = state.players[idx]
        if (power !in player.activePowerInventory) return state
        if ((player.powerCooldowns[power] ?: 0) > 0) return state

        val updatedPlayers = state.players.toMutableList()
        val newInv = player.activePowerInventory.toMutableList().apply { remove(power) }
        val newCd = player.powerCooldowns.toMutableMap().apply { put(power, power.cooldownTurns) }
        var updatedMover = player.copy(activePowerInventory = newInv, powerCooldowns = newCd)
        var summary = "${player.config.name} activated ${power.title}!"
        var cutEvent: CutEvent? = null
        var targetTile: Int? = null

        when (power) {
            PowerType.SHIELD -> {
                val activeTok = updatedMover.tokens.firstOrNull {
                    it.tokenIndex == targetTokenIndex && it.isOnMainTrack(state.boardType)
                } ?: updatedMover.tokens.firstOrNull { it.isOnMainTrack(state.boardType) }
                if (activeTok != null) {
                    updatedMover = updatedMover.copy(
                        tokens = updatedMover.tokens.map {
                            if (it.tokenIndex == activeTok.tokenIndex) it.copy(shieldTurns = 2) else it
                        }
                    )
                    targetTile = BoardGeometry.toGlobalTrackIndex(state.boardType, player.config.color, activeTok.progress)
                    summary = "${player.config.name} shielded token #${activeTok.tokenIndex + 1} for 2 rounds!"
                }
            }
            PowerType.DOUBLE_X2 -> {
                updatedMover = updatedMover.copy(pendingMultiplier = 2)
                summary = "${player.config.name} primed DOUBLE X2 for next move!"
            }
            PowerType.TRIPLE_X3 -> {
                updatedMover = updatedMover.copy(pendingMultiplier = 3)
                summary = "${player.config.name} primed TRIPLE X3 for next move!"
            }
            PowerType.SPEED_BOOST -> {
                updatedMover = updatedMover.copy(pendingBonusSteps = updatedMover.pendingBonusSteps + 3)
                summary = "${player.config.name} boosted +3 bonus steps!"
            }
            PowerType.LUCKY_STAR -> {
                updatedMover = updatedMover.copy(score = updatedMover.score + 150)
                summary = "${player.config.name} gained +150 Lucky Star points!"
            }
            PowerType.ROCKET -> {
                val tok = updatedMover.tokens.firstOrNull {
                    it.tokenIndex == targetTokenIndex && !it.isInBase() && !it.isFinished(state.boardType)
                } ?: updatedMover.tokens.firstOrNull { !it.isInBase() && !it.isFinished(state.boardType) }
                if (tok != null) {
                    val dest = (tok.progress + 5).coerceAtMost(state.boardType.homeFinishProgress)
                    updatedMover = updatedMover.copy(
                        tokens = updatedMover.tokens.map {
                            if (it.tokenIndex == tok.tokenIndex) it.copy(progress = dest) else it
                        },
                        score = updatedMover.score + 30
                    )
                    targetTile = BoardGeometry.toGlobalTrackIndex(state.boardType, player.config.color, dest)
                    summary = "${player.config.name}'s Rocket blasted token #${tok.tokenIndex + 1} +5 tiles!"
                }
            }
            PowerType.TELEPORT -> {
                val tok = updatedMover.tokens.firstOrNull {
                    it.tokenIndex == targetTokenIndex && it.isOnMainTrack(state.boardType)
                } ?: updatedMover.tokens.firstOrNull { it.isOnMainTrack(state.boardType) }
                if (tok != null) {
                    val currentGlobal = BoardGeometry.toGlobalTrackIndex(state.boardType, player.config.color, tok.progress) ?: 0
                    val stars = BoardGeometry.getAllStarIndices(state.boardType)
                    val nextStar = stars.firstOrNull { it > currentGlobal } ?: stars.first()
                    val delta = (nextStar - currentGlobal + state.boardType.trackLength) % state.boardType.trackLength
                    val dest = (tok.progress + delta).coerceAtMost(state.boardType.trackLength - 2)
                    updatedMover = updatedMover.copy(
                        tokens = updatedMover.tokens.map {
                            if (it.tokenIndex == tok.tokenIndex) it.copy(progress = dest) else it
                        }
                    )
                    targetTile = nextStar
                    summary = "${player.config.name} teleported token #${tok.tokenIndex + 1} to a Star safe zone!"
                }
            }
            PowerType.FIRE, PowerType.TNT -> {
                val radius = if (power == PowerType.FIRE) 4 else 2
                val myTrackTok = updatedMover.tokens.firstOrNull {
                    it.tokenIndex == targetTokenIndex && it.isOnMainTrack(state.boardType)
                } ?: updatedMover.tokens.firstOrNull { it.isOnMainTrack(state.boardType) }
                val originGlobal = myTrackTok?.let {
                    BoardGeometry.toGlobalTrackIndex(state.boardType, player.config.color, it.progress)
                } ?: BoardGeometry.getStartOffset(state.boardType, player.config.color)
                targetTile = originGlobal

                var hitAny = false
                for (i in updatedPlayers.indices) {
                    val other = updatedPlayers[i]
                    if (!isOpponent(player.config, other.config)) continue
                    val hitTok = other.tokens.firstOrNull { t ->
                        if (!t.isOnMainTrack(state.boardType) || t.shieldTurns > 0) return@firstOrNull false
                        val g = BoardGeometry.toGlobalTrackIndex(state.boardType, other.config.color, t.progress) ?: return@firstOrNull false
                        if (BoardGeometry.isSafeGlobalIndex(state.boardType, g, state.rules.safeStarsEnabled)) return@firstOrNull false
                        val dist = (g - originGlobal + state.boardType.trackLength) % state.boardType.trackLength
                        dist in 1..radius || (power == PowerType.TNT && (state.boardType.trackLength - dist) in 1..radius)
                    }
                    if (hitTok != null) {
                        val g = BoardGeometry.toGlobalTrackIndex(state.boardType, other.config.color, hitTok.progress) ?: 0
                        updatedPlayers[i] = other.copy(
                            tokens = other.tokens.map {
                                if (it.tokenIndex == hitTok.tokenIndex) it.copy(progress = -1) else it
                            }
                        )
                        updatedMover = updatedMover.copy(
                            cutsMade = updatedMover.cutsMade + 1,
                            score = updatedMover.score + 100
                        )
                        cutEvent = CutEvent(player.config.color, other.config.color, hitTok.tokenIndex, g)
                        summary = "${power.title}! ${player.config.name} blasted ${other.config.name}'s token back to base!"
                        hitAny = true
                        if (power == PowerType.FIRE) break
                    }
                }
                if (!hitAny) summary = "${player.config.name} unleashed ${power.title} — no unprotected targets in range."
            }
            PowerType.ICE_TILE -> {
                val targetIdx = findNextActivePlayerIndex(updatedPlayers, idx, state.boardType)
                val target = updatedPlayers[targetIdx]
                if (isOpponent(player.config, target.config)) {
                    updatedPlayers[targetIdx] = target.copy(frozenTurns = target.frozenTurns + 1)
                    summary = "ICE FREEZE! ${target.config.name} will miss their next turn!"
                }
            }
        }

        updatedPlayers[idx] = updatedMover
        val refreshedMoves = if (state.phase == TurnPhase.SELECT_TOKEN && state.lastDiceRoll != null) {
            calculateLegalMoves(state.copy(players = updatedPlayers), state.lastDiceRoll)
        } else {
            state.legalMoves
        }

        return state.copy(
            players = updatedPlayers,
            legalMoves = refreshedMoves,
            lastCutEvent = cutEvent,
            lastPowerEvent = PowerActivationEvent(player.config.color, power, targetTile, summary),
            statusBanner = summary
        )
    }

    fun undoLastMove(state: GameState): GameState {
        if (!state.rules.allowUndoInOffline || state.gameMode == GameMode.POWER_MODE) return state
        return state.undoSnapshot ?: state
    }

    private fun decrementRoundEffects(
        players: List<PlayerMatchState>,
        actorIndex: Int
    ): List<PlayerMatchState> {
        return players.mapIndexed { index, p ->
            if (index != actorIndex) p
            else p.copy(
                tokens = p.tokens.map { tok ->
                    if (tok.shieldTurns > 0) tok.copy(shieldTurns = tok.shieldTurns - 1) else tok
                },
                powerCooldowns = p.powerCooldowns.mapValues { (_, cd) -> (cd - 1).coerceAtLeast(0) }
            )
        }
    }

    private fun advanceTurnCleanly(state: GameState, nextIndex: Int): GameState {
        return state.copy(
            currentTurnIndex = nextIndex,
            phase = TurnPhase.WAIT_FOR_ROLL,
            consecutiveSixes = 0,
            legalMoves = emptyList(),
            turnCounter = state.turnCounter + 1
        )
    }

    private fun findNextActivePlayerIndex(
        players: List<PlayerMatchState>,
        currentIndex: Int,
        boardType: BoardType
    ): Int {
        val count = players.size
        for (offset in 1..count) {
            val candidate = (currentIndex + offset) % count
            if (!players[candidate].hasFinishedAll(boardType)) {
                return candidate
            }
        }
        return currentIndex
    }

    private fun evaluateVictory(
        players: List<PlayerMatchState>,
        boardType: BoardType,
        gameMode: GameMode
    ): Pair<List<PlayerColor>, Int?>? {
        if (gameMode == GameMode.TEAM_BATTLE || players.any { it.config.teamId != 0 }) {
            val teamGroups = players.filter { it.config.teamId != 0 }.groupBy { it.config.teamId }
            for ((teamId, members) in teamGroups) {
                if (members.all { it.hasFinishedAll(boardType) }) {
                    return members.map { it.config.color } to teamId
                }
            }
        }
        val firstWinner = players.firstOrNull { it.finishRank == 1 }
        if (firstWinner != null) {
            return listOf(firstWinner.config.color) to null
        }
        return null
    }
}
