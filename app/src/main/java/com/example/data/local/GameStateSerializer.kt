package com.example.data.local

import com.example.game.AiDifficulty
import com.example.game.BoardType
import com.example.game.GameMode
import com.example.game.GameState
import com.example.game.LudoEngine
import com.example.game.PlayerColor
import com.example.game.PlayerConfig
import com.example.game.PlayerMatchState
import com.example.game.PowerType
import com.example.game.RuleConfig
import com.example.game.TokenState
import com.example.game.TurnPhase
import org.json.JSONArray
import org.json.JSONObject

object GameStateSerializer {

    fun toJson(state: GameState): String {
        val root = JSONObject().apply {
            put("matchId", state.matchId)
            put("gameMode", state.gameMode.name)
            put("boardType", state.boardType.name)
            put("currentTurnIndex", state.currentTurnIndex)
            put("phase", state.phase.name)
            put("lastDiceRoll", state.lastDiceRoll ?: JSONObject.NULL)
            put("consecutiveSixes", state.consecutiveSixes)
            put("statusBanner", state.statusBanner)
            put("turnCounter", state.turnCounter)
            put("startTimeMs", state.startTimeMs)
            put("tournamentRound", state.tournamentRound)
            put("winningTeamId", state.winningTeamId ?: JSONObject.NULL)

            put("rules", JSONObject().apply {
                put("spawnOnOneOrSix", state.rules.spawnOnOneOrSix)
                put("extraTurnOnSix", state.rules.extraTurnOnSix)
                put("extraTurnOnCapture", state.rules.extraTurnOnCapture)
                put("threeSixesForfeit", state.rules.threeSixesForfeit)
                put("safeStarsEnabled", state.rules.safeStarsEnabled)
                put("allowUndoInOffline", state.rules.allowUndoInOffline)
                put("stackProtection", state.rules.stackProtection)
            })

            val playersArr = JSONArray()
            state.players.forEach { p ->
                val tokensArr = JSONArray()
                p.tokens.forEach { t ->
                    tokensArr.put(JSONObject().apply {
                        put("ownerColor", t.ownerColor.name)
                        put("tokenIndex", t.tokenIndex)
                        put("progress", t.progress)
                        put("shieldTurns", t.shieldTurns)
                    })
                }
                val invArr = JSONArray()
                p.activePowerInventory.forEach { invArr.put(it.name) }
                val cdObj = JSONObject()
                p.powerCooldowns.forEach { (k, v) -> cdObj.put(k.name, v) }

                playersArr.put(JSONObject().apply {
                    put("color", p.config.color.name)
                    put("name", p.config.name)
                    put("avatarId", p.config.avatarId)
                    put("isAi", p.config.isAi)
                    put("aiDifficulty", p.config.aiDifficulty.name)
                    put("teamId", p.config.teamId)
                    put("tokens", tokensArr)
                    put("score", p.score)
                    put("cutsMade", p.cutsMade)
                    put("frozenTurns", p.frozenTurns)
                    put("finishRank", p.finishRank)
                    put("inventory", invArr)
                    put("cooldowns", cdObj)
                    put("pendingMultiplier", p.pendingMultiplier)
                    put("pendingBonusSteps", p.pendingBonusSteps)
                })
            }
            put("players", playersArr)

            val powersObj = JSONObject()
            state.boardPowers.forEach { (tileIdx, power) ->
                powersObj.put(tileIdx.toString(), power.name)
            }
            put("boardPowers", powersObj)

            val winnersArr = JSONArray()
            state.winnerColors.forEach { winnersArr.put(it.name) }
            put("winnerColors", winnersArr)
        }
        return root.toString()
    }

    fun fromJson(raw: String): GameState? {
        return try {
            val root = JSONObject(raw)
            val boardType = BoardType.valueOf(root.getString("boardType"))
            val gameMode = GameMode.valueOf(root.getString("gameMode"))
            val rulesObj = root.getJSONObject("rules")
            val rules = RuleConfig(
                spawnOnOneOrSix = rulesObj.optBoolean("spawnOnOneOrSix", false),
                extraTurnOnSix = rulesObj.optBoolean("extraTurnOnSix", true),
                extraTurnOnCapture = rulesObj.optBoolean("extraTurnOnCapture", true),
                threeSixesForfeit = rulesObj.optBoolean("threeSixesForfeit", true),
                safeStarsEnabled = rulesObj.optBoolean("safeStarsEnabled", true),
                allowUndoInOffline = rulesObj.optBoolean("allowUndoInOffline", true),
                stackProtection = rulesObj.optBoolean("stackProtection", false)
            )

            val playersArr = root.getJSONArray("players")
            val players = (0 until playersArr.length()).map { i ->
                val pObj = playersArr.getJSONObject(i)
                val color = PlayerColor.valueOf(pObj.getString("color"))
                val cfg = PlayerConfig(
                    color = color,
                    name = pObj.getString("name"),
                    avatarId = pObj.optInt("avatarId", 0),
                    isAi = pObj.optBoolean("isAi", false),
                    aiDifficulty = AiDifficulty.valueOf(pObj.optString("aiDifficulty", "MEDIUM")),
                    teamId = pObj.optInt("teamId", 0)
                )
                val tArr = pObj.getJSONArray("tokens")
                val tokens = (0 until tArr.length()).map { j ->
                    val tObj = tArr.getJSONObject(j)
                    TokenState(
                        ownerColor = PlayerColor.valueOf(tObj.getString("ownerColor")),
                        tokenIndex = tObj.getInt("tokenIndex"),
                        progress = tObj.getInt("progress"),
                        shieldTurns = tObj.optInt("shieldTurns", 0)
                    )
                }
                val invArr = pObj.optJSONArray("inventory") ?: JSONArray()
                val inv = (0 until invArr.length()).map { idx -> PowerType.valueOf(invArr.getString(idx)) }
                val cdObj = pObj.optJSONObject("cooldowns") ?: JSONObject()
                val cds = mutableMapOf<PowerType, Int>()
                cdObj.keys().forEach { key ->
                    cds[PowerType.valueOf(key)] = cdObj.getInt(key)
                }

                PlayerMatchState(
                    config = cfg,
                    tokens = tokens,
                    score = pObj.optInt("score", 0),
                    cutsMade = pObj.optInt("cutsMade", 0),
                    frozenTurns = pObj.optInt("frozenTurns", 0),
                    finishRank = pObj.optInt("finishRank", 0),
                    activePowerInventory = inv,
                    powerCooldowns = cds,
                    pendingMultiplier = pObj.optInt("pendingMultiplier", 1),
                    pendingBonusSteps = pObj.optInt("pendingBonusSteps", 0)
                )
            }

            val powersObj = root.optJSONObject("boardPowers") ?: JSONObject()
            val boardPowers = mutableMapOf<Int, PowerType>()
            powersObj.keys().forEach { k ->
                boardPowers[k.toInt()] = PowerType.valueOf(powersObj.getString(k))
            }

            val winnersArr = root.optJSONArray("winnerColors") ?: JSONArray()
            val winners = (0 until winnersArr.length()).map { PlayerColor.valueOf(winnersArr.getString(it)) }

            val lastDiceRoll = if (root.isNull("lastDiceRoll")) null else root.getInt("lastDiceRoll")
            val phase = TurnPhase.valueOf(root.getString("phase"))

            val baseState = GameState(
                matchId = root.getString("matchId"),
                gameMode = gameMode,
                boardType = boardType,
                rules = rules,
                players = players,
                currentTurnIndex = root.getInt("currentTurnIndex"),
                phase = phase,
                lastDiceRoll = lastDiceRoll,
                consecutiveSixes = root.optInt("consecutiveSixes", 0),
                boardPowers = boardPowers,
                statusBanner = root.optString("statusBanner", ""),
                turnCounter = root.optInt("turnCounter", 1),
                startTimeMs = root.optLong("startTimeMs", System.currentTimeMillis()),
                winnerColors = winners,
                winningTeamId = if (root.isNull("winningTeamId")) null else root.getInt("winningTeamId"),
                tournamentRound = root.optInt("tournamentRound", 0)
            )
            if (phase == TurnPhase.SELECT_TOKEN && lastDiceRoll != null) {
                baseState.copy(legalMoves = LudoEngine.calculateLegalMoves(baseState, lastDiceRoll))
            } else {
                baseState
            }
        } catch (_: Exception) {
            null
        }
    }
}
