package com.example.game

import kotlin.random.Random

/**
 * Intelligent multi-difficulty AI engine for Computer Mode & AI bots.
 * Evaluates token cuts, safe zone positioning, threat evasion, home entry,
 * team coordination, and Power Mode ability usage.
 */
object LudoAi {

    fun choosePowerToActivate(state: GameState): PowerType? {
        if (state.gameMode != GameMode.POWER_MODE) return null
        val player = state.currentPlayer
        val difficulty = player.config.aiDifficulty
        if (difficulty == AiDifficulty.EASY) return null

        val usable = player.activePowerInventory.filter { (player.powerCooldowns[it] ?: 0) <= 0 }
        if (usable.isEmpty()) return null

        val hasTokenOnBoard = player.tokens.any { it.isOnMainTrack(state.boardType) }
        return when {
            PowerType.FIRE in usable && hasTokenOnBoard && difficulty >= AiDifficulty.HARD -> PowerType.FIRE
            PowerType.TNT in usable && hasTokenOnBoard && difficulty >= AiDifficulty.HARD -> PowerType.TNT
            PowerType.SHIELD in usable && hasTokenOnBoard -> PowerType.SHIELD
            PowerType.ICE_TILE in usable && difficulty >= AiDifficulty.MEDIUM -> PowerType.ICE_TILE
            PowerType.DOUBLE_X2 in usable && hasTokenOnBoard -> PowerType.DOUBLE_X2
            PowerType.ROCKET in usable && hasTokenOnBoard -> PowerType.ROCKET
            PowerType.SPEED_BOOST in usable && hasTokenOnBoard -> PowerType.SPEED_BOOST
            else -> null
        }
    }

    fun chooseMove(state: GameState): LegalMove? {
        val moves = state.legalMoves
        if (moves.isEmpty()) return null
        if (moves.size == 1) return moves.first()

        val player = state.currentPlayer
        val difficulty = player.config.aiDifficulty

        if (difficulty == AiDifficulty.EASY) {
            return moves[Random.nextInt(moves.size)]
        }

        return moves.maxByOrNull { move ->
            scoreMove(state, player, move, difficulty)
        } ?: moves.first()
    }

    private fun scoreMove(
        state: GameState,
        player: PlayerMatchState,
        move: LegalMove,
        difficulty: AiDifficulty
    ): Double {
        var score = 0.0
        val boardType = state.boardType

        // 1. Entering final HOME triangle
        if (move.willReachHome) {
            score += 500.0
        }

        // 2. Capturing an opponent token
        if (move.willCapture) {
            score += when (difficulty) {
                AiDifficulty.MEDIUM -> 220.0
                AiDifficulty.HARD -> 360.0
                AiDifficulty.EXPERT -> 420.0
                else -> 50.0
            }
        }

        // 3. Leaving base on a 6/1
        if (move.fromProgress == -1) {
            score += 180.0
        }

        // 4. Entering private home column (completely safe)
        if (move.fromProgress < boardType.trackLength - 1 && move.toProgress >= boardType.trackLength - 1) {
            score += 240.0
        }

        // 5. Power tile pickup
        if (move.landedPower != null && difficulty >= AiDifficulty.MEDIUM) {
            score += 110.0
        }

        // 6. Landing on safe square vs vulnerable square
        val destGlobal = BoardGeometry.toGlobalTrackIndex(boardType, player.config.color, move.toProgress)
        if (destGlobal != null) {
            if (BoardGeometry.isSafeGlobalIndex(boardType, destGlobal, state.rules.safeStarsEnabled)) {
                score += 130.0
            } else if (difficulty >= AiDifficulty.HARD) {
                val dangerAfter = countThreatsAt(state, player, destGlobal)
                score -= dangerAfter * 75.0
            }
        }

        // 7. Escaping current danger if token is currently threatened
        val currentGlobal = BoardGeometry.toGlobalTrackIndex(boardType, player.config.color, move.fromProgress)
        if (currentGlobal != null &&
            !BoardGeometry.isSafeGlobalIndex(boardType, currentGlobal, state.rules.safeStarsEnabled) &&
            difficulty >= AiDifficulty.HARD
        ) {
            val dangerNow = countThreatsAt(state, player, currentGlobal)
            score += dangerNow * 85.0
        }

        // 8. Prefer advancing further tokens in Expert mode
        if (difficulty == AiDifficulty.EXPERT) {
            score += move.toProgress * 1.4
        }

        // Small deterministic tie-breaker jitter for Medium
        if (difficulty == AiDifficulty.MEDIUM) {
            score += Random.nextDouble(0.0, 25.0)
        }
        return score
    }

    private fun countThreatsAt(
        state: GameState,
        me: PlayerMatchState,
        targetGlobal: Int
    ): Int {
        var threats = 0
        val trackLen = state.boardType.trackLength
        for (other in state.players) {
            if (!LudoEngine.isOpponent(me.config, other.config)) continue
            for (tok in other.tokens) {
                if (!tok.isOnMainTrack(state.boardType)) continue
                val g = BoardGeometry.toGlobalTrackIndex(state.boardType, other.config.color, tok.progress) ?: continue
                val distance = (targetGlobal - g + trackLen) % trackLen
                if (distance in 1..6) {
                    threats++
                }
            }
        }
        return threats
    }
}
