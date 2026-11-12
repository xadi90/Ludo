package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.BoardGeometry
import com.example.game.BoardType
import com.example.game.CutEvent
import com.example.game.GameState
import com.example.game.PlayerColor
import com.example.game.PowerActivationEvent
import com.example.game.PowerType
import com.example.game.TokenState
import com.example.game.TurnPhase
import com.example.ui.theme.BoardCellBorder
import com.example.ui.theme.BoardCream
import com.example.ui.theme.BoardWoodFrame
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoGreen
import com.example.ui.theme.LudoOrange
import com.example.ui.theme.LudoPurple
import com.example.ui.theme.LudoRed
import com.example.ui.theme.LudoYellow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun LudoBoardCanvas(
    gameState: GameState,
    activeCut: CutEvent?,
    activePower: PowerActivationEvent?,
    onTokenTapped: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val infinite = rememberInfiniteTransition(label = "board_anim")
    val pulse by infinite.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(680, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "token_pulse"
    )
    val confettiPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti"
    )

    val slashProgress = remember { Animatable(0f) }
    LaunchedEffect(activeCut) {
        if (activeCut != null) {
            slashProgress.snapTo(0f)
            slashProgress.animateTo(1f, tween(520))
        }
    }

    val powerBurst = remember { Animatable(0f) }
    LaunchedEffect(activePower) {
        if (activePower != null) {
            powerBurst.snapTo(0f)
            powerBurst.animateTo(1f, tween(600))
        }
    }

    // Group tokens by normalized cell position so stacked tokens fan out cleanly
    val allTokens = remember(gameState.players) {
        gameState.players.flatMap { it.tokens }
    }
    val cellOccupancy = remember(allTokens, gameState.boardType) {
        allTokens.groupBy { token ->
            val pos = BoardGeometry.getTokenNormalizedPosition(gameState.boardType, token)
            "${(pos.x * 100).toInt()}_${(pos.y * 100).toInt()}"
        }
    }

    // Animate every token's normalized position smoothly
    val animatedPositions = mutableMapOf<String, Offset>()
    for (token in allTokens) {
        val basePos = BoardGeometry.getTokenNormalizedPosition(gameState.boardType, token)
        val key = "${(basePos.x * 100).toInt()}_${(basePos.y * 100).toInt()}"
        val group = cellOccupancy[key] ?: listOf(token)
        val idxInGroup = group.indexOfFirst { it.id == token.id }.coerceAtLeast(0)
        val fanOffset = if (group.size <= 1 || token.isInBase()) {
            Offset.Zero
        } else {
            val angle = (2.0 * PI * idxInGroup / group.size)
            val spread = if (gameState.boardType == BoardType.SIX_PLAYER) 0.011f else 0.013f
            Offset(cos(angle).toFloat() * spread, sin(angle).toFloat() * spread)
        }
        val animPos by animateOffsetAsState(
            targetValue = basePos + fanOffset,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
            label = "tok_${token.id}"
        )
        animatedPositions[token.id] = animPos
    }

    val legalTokenIndices = if (gameState.phase == TurnPhase.SELECT_TOKEN && !gameState.currentPlayer.config.isAi) {
        gameState.legalMoves.map { it.tokenIndex }.toSet()
    } else {
        emptySet()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(22.dp))
            .background(BoardWoodFrame)
            .border(3.dp, GoldAccent.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
            .padding(6.dp)
            .testTag("ludo_board_canvas"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(gameState.currentTurnIndex, gameState.phase, legalTokenIndices) {
                    detectTapGestures { tapOffset ->
                        if (legalTokenIndices.isEmpty()) return@detectTapGestures
                        val nx = tapOffset.x / size.width
                        val ny = tapOffset.y / size.height
                        val currentPlayer = gameState.currentPlayer
                        var bestIndex: Int? = null
                        var bestDist = 0.095f
                        for (tok in currentPlayer.tokens) {
                            if (tok.tokenIndex !in legalTokenIndices) continue
                            val pos = animatedPositions[tok.id]
                                ?: BoardGeometry.getTokenNormalizedPosition(gameState.boardType, tok)
                            val dist = hypot(nx - pos.x, ny - pos.y)
                            if (dist < bestDist) {
                                bestDist = dist
                                bestIndex = tok.tokenIndex
                            }
                        }
                        if (bestIndex != null) {
                            onTokenTapped(bestIndex)
                        }
                    }
                }
        ) {
            when (gameState.boardType) {
                BoardType.FOUR_PLAYER -> drawFourPlayerBoard(gameState, textMeasurer)
                BoardType.SIX_PLAYER -> drawSixPlayerBoard(gameState, textMeasurer)
            }

            // Draw Power Tiles on board
            gameState.boardPowers.forEach { (tileIdx, power) ->
                val c = BoardGeometry.getTrackNormalizedCenter(gameState.boardType, tileIdx)
                val px = Offset(c.x * size.width, c.y * size.height)
                val r = size.minDimension * (if (gameState.boardType == BoardType.SIX_PLAYER) 0.022f else 0.027f)
                drawPowerTileBadge(px, r, power, textMeasurer)
            }

            // Draw all player tokens (draw non-legal first, legal active player's on top!)
            val activeColor = gameState.currentPlayer.config.color
            val sortedTokens = allTokens.sortedBy {
                val isLegal = it.ownerColor == activeColor && it.tokenIndex in legalTokenIndices
                if (isLegal) 2 else if (it.ownerColor == activeColor) 1 else 0
            }

            val tokenRadius = size.minDimension * if (gameState.boardType == BoardType.SIX_PLAYER) 0.022f else 0.026f
            for (token in sortedTokens) {
                val norm = animatedPositions[token.id]
                    ?: BoardGeometry.getTokenNormalizedPosition(gameState.boardType, token)
                val center = Offset(norm.x * size.width, norm.y * size.height)
                val isLegal = token.ownerColor == activeColor && token.tokenIndex in legalTokenIndices
                drawGlossyPawn(
                    center = center,
                    radius = tokenRadius,
                    color = token.ownerColor,
                    isLegal = isLegal,
                    pulseScale = pulse,
                    hasShield = token.shieldTurns > 0,
                    isFrozen = gameState.players.firstOrNull { it.config.color == token.ownerColor }?.frozenTurns?.let { it > 0 } == true
                )
            }

            // Sword Slash / Cut Animation
            if (activeCut != null) {
                val cNorm = BoardGeometry.getTrackNormalizedCenter(gameState.boardType, activeCut.globalTileIndex)
                val cPx = Offset(cNorm.x * size.width, cNorm.y * size.height)
                val p = slashProgress.value
                val r = size.minDimension * 0.08f * (0.6f + p * 0.8f)
                drawCircle(
                    color = LudoRed.copy(alpha = (1f - p) * 0.7f),
                    radius = r,
                    center = cPx,
                    style = Stroke(width = 6f)
                )
                // Crossed sword slash strokes
                val d = size.minDimension * 0.065f
                drawLine(
                    color = Color.White.copy(alpha = 1f - p * 0.5f),
                    start = cPx + Offset(-d, -d),
                    end = cPx + Offset(-d + 2 * d * p, -d + 2 * d * p),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = GoldAccent.copy(alpha = 1f - p * 0.5f),
                    start = cPx + Offset(d, -d),
                    end = cPx + Offset(d - 2 * d * p, -d + 2 * d * p),
                    strokeWidth = 7f,
                    cap = StrokeCap.Round
                )
            }

            // Power Activation Ring Burst
            if (activePower != null && activePower.targetTileIndex != null) {
                val cNorm = BoardGeometry.getTrackNormalizedCenter(gameState.boardType, activePower.targetTileIndex)
                val cPx = Offset(cNorm.x * size.width, cNorm.y * size.height)
                val p = powerBurst.value
                drawCircle(
                    color = CyanGlow.copy(alpha = 1f - p),
                    radius = size.minDimension * 0.03f + size.minDimension * 0.11f * p,
                    center = cPx,
                    style = Stroke(width = 7f)
                )
            }

            // Confetti overlay on Match Over
            if (gameState.phase == TurnPhase.MATCH_OVER) {
                val colors = listOf(LudoRed, LudoGreen, LudoBlue, LudoYellow, LudoPurple, LudoOrange)
                for (i in 0 until 36) {
                    val x = ((i * 37) % 100) / 100f * size.width
                    val y = (((i * 23) / 100f + confettiPhase) % 1f) * size.height
                    drawRect(
                        color = colors[i % colors.size].copy(alpha = 0.88f),
                        topLeft = Offset(x, y),
                        size = Size(10f, 16f)
                    )
                }
            }
        }

        // Floating TOKEN CUT banner
        AnimatedVisibility(
            visible = activeCut != null,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.horizontalGradient(listOf(LudoRed, Color(0xFFB3001B))))
                    .border(2.dp, GoldAccent, RoundedCornerShape(14.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "⚔ TOKEN CUT!",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        }
    }
}

private fun DrawScope.drawFourPlayerBoard(
    gameState: GameState,
    textMeasurer: TextMeasurer
) {
    val cell = size.width / 15f
    drawRect(color = BoardCream, size = size)

    val activeColor = gameState.currentPlayer.config.color
    val presentColors = gameState.players.map { it.config.color }.toSet()

    // 4 Corner Bases (6x6 cells)
    val baseAreas = listOf(
        Triple(PlayerColor.RED, 0, 0),
        Triple(PlayerColor.GREEN, 0, 9),
        Triple(PlayerColor.YELLOW, 9, 9),
        Triple(PlayerColor.BLUE, 9, 0)
    )
    for ((color, r0, c0) in baseAreas) {
        val topLeft = Offset(c0 * cell, r0 * cell)
        val baseSize = Size(6 * cell, 6 * cell)
        val alpha = if (color in presentColors) 1f else 0.4f
        drawRect(
            brush = Brush.linearGradient(
                listOf(color.primary.copy(alpha = alpha), color.dark.copy(alpha = alpha))
            ),
            topLeft = topLeft,
            size = baseSize
        )
        if (color == activeColor && gameState.phase != TurnPhase.MATCH_OVER) {
            drawRect(
                color = GoldAccent,
                topLeft = topLeft + Offset(2f, 2f),
                size = Size(baseSize.width - 4f, baseSize.height - 4f),
                style = Stroke(width = 5f)
            )
        }
        // White inner yard
        val yardTopLeft = Offset((c0 + 0.85f) * cell, (r0 + 0.85f) * cell)
        val yardSize = Size(4.3f * cell, 4.3f * cell)
        drawRoundRect(
            color = Color.White,
            topLeft = yardTopLeft,
            size = yardSize,
            cornerRadius = CornerRadius(cell * 0.55f, cell * 0.55f)
        )
        // 4 circular base token sockets
        for (idx in 0 until 4) {
            val slotNorm = BoardGeometry.getBaseSlotCenter(BoardType.FOUR_PLAYER, color, idx)
            val slotPx = Offset(slotNorm.x * size.width, slotNorm.y * size.height)
            drawCircle(
                color = color.primary.copy(alpha = 0.25f),
                radius = cell * 0.52f,
                center = slotPx
            )
            drawCircle(
                color = color.primary,
                radius = cell * 0.44f,
                center = slotPx,
                style = Stroke(width = 2.5f)
            )
        }
    }

    // 52 Main Track Cells
    val trackGrid = BoardGeometry.getFourPlayerGridTrack()
    val startIndices = BoardGeometry.getAllStartIndices(BoardType.FOUR_PLAYER)
    val startColorByIdx = startIndices.entries.associate { (c, i) -> i to c }

    trackGrid.forEachIndexed { globalIdx, (r, c) ->
        val tl = Offset(c * cell, r * cell)
        val startColor = startColorByIdx[globalIdx]
        val isStar = BoardGeometry.isStarGlobalIndex(BoardType.FOUR_PLAYER, globalIdx)
        val fill = startColor?.primary ?: if (isStar) Color(0xFFFFF4D1) else Color.White

        drawRect(color = fill, topLeft = tl, size = Size(cell, cell))
        drawRect(color = BoardCellBorder, topLeft = tl, size = Size(cell, cell), style = Stroke(width = 1.4f))

        if (isStar && gameState.rules.safeStarsEnabled) {
            drawStarIcon(
                center = Offset(tl.x + cell / 2f, tl.y + cell / 2f),
                outerRadius = cell * 0.34f,
                color = GoldAccent
            )
        } else if (startColor != null) {
            drawStarIcon(
                center = Offset(tl.x + cell / 2f, tl.y + cell / 2f),
                outerRadius = cell * 0.32f,
                color = Color.White
            )
        }
    }

    // 4 Home Columns
    BoardGeometry.getFourPlayerGridHomeColumns().forEach { (color, cells) ->
        cells.forEach { (r, c) ->
            val tl = Offset(c * cell, r * cell)
            drawRect(color = color.primary.copy(alpha = 0.88f), topLeft = tl, size = Size(cell, cell))
            drawRect(color = Color.White.copy(alpha = 0.65f), topLeft = tl, size = Size(cell, cell), style = Stroke(width = 1.4f))
        }
    }

    // Center 3x3 Home Triangles
    val cx = 7.5f * cell
    val cy = 7.5f * cell
    val l = 6f * cell
    val r = 9f * cell
    val t = 6f * cell
    val b = 9f * cell

    fun drawTriangle(p1: Offset, p2: Offset, color: Color) {
        val path = Path().apply {
            moveTo(cx, cy)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }
        drawPath(path, color = color)
        drawPath(path, color = Color.White.copy(alpha = 0.8f), style = Stroke(width = 2f))
    }

    drawTriangle(Offset(l, t), Offset(l, b), LudoRed)
    drawTriangle(Offset(l, t), Offset(r, t), LudoGreen)
    drawTriangle(Offset(r, t), Offset(r, b), LudoYellow)
    drawTriangle(Offset(l, b), Offset(r, b), LudoBlue)

    // Center Trophy Crown Emblem
    drawCircle(color = Color.White, radius = cell * 0.56f, center = Offset(cx, cy))
    drawCircle(color = GoldAccent, radius = cell * 0.52f, center = Offset(cx, cy), style = Stroke(width = 3f))
    val crownLayout = textMeasurer.measure(
        "★",
        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFFD99000))
    )
    drawText(
        crownLayout,
        topLeft = Offset(cx - crownLayout.size.width / 2f, cy - crownLayout.size.height / 2f)
    )
}

private fun DrawScope.drawSixPlayerBoard(
    gameState: GameState,
    textMeasurer: TextMeasurer
) {
    // Rich dark royal board surface with crisp cream track circles
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF143C9A), Color(0xFF0A2059)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * 0.65f
        ),
        size = size
    )

    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(
        color = Color.White.copy(alpha = 0.08f),
        radius = size.minDimension * 0.47f,
        center = center
    )

    val activeColor = gameState.currentPlayer.config.color
    val presentColors = gameState.players.map { it.config.color }.toSet()

    // Draw 6 colored base wedges around the outer ring
    val baseRadius = size.minDimension * 0.082f
    BoardGeometry.sixPlayerBaseCenters.forEach { (color, normCenter) ->
        val cPx = Offset(normCenter.x * size.width, normCenter.y * size.height)
        val alpha = if (color in presentColors) 1f else 0.42f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(color.primary.copy(alpha = alpha), color.dark.copy(alpha = alpha)),
                center = cPx,
                radius = baseRadius
            ),
            radius = baseRadius,
            center = cPx
        )
        drawCircle(
            color = if (color == activeColor && gameState.phase != TurnPhase.MATCH_OVER) GoldAccent else Color.White.copy(alpha = 0.8f),
            radius = baseRadius,
            center = cPx,
            style = Stroke(width = if (color == activeColor) 5f else 2.5f)
        )
        // White inner circle
        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = baseRadius * 0.76f,
            center = cPx
        )
        for (idx in 0 until 4) {
            val slot = BoardGeometry.getBaseSlotCenter(BoardType.SIX_PLAYER, color, idx)
            val slotPx = Offset(slot.x * size.width, slot.y * size.height)
            drawCircle(
                color = color.primary.copy(alpha = 0.25f),
                radius = size.minDimension * 0.019f,
                center = slotPx
            )
            drawCircle(
                color = color.primary,
                radius = size.minDimension * 0.017f,
                center = slotPx,
                style = Stroke(width = 2f)
            )
        }
    }

    // Draw connecting ring guide beneath 72 cells
    val trackPoints = BoardGeometry.sixPlayerTrackNormalized.map {
        Offset(it.x * size.width, it.y * size.height)
    }
    for (i in trackPoints.indices) {
        val next = trackPoints[(i + 1) % trackPoints.size]
        drawLine(
            color = Color.White.copy(alpha = 0.28f),
            start = trackPoints[i],
            end = next,
            strokeWidth = 3f
        )
    }

    // 72 Main Track Cells
    val cellRadius = size.minDimension * 0.023f
    val startByIdx = BoardGeometry.getAllStartIndices(BoardType.SIX_PLAYER).entries.associate { (c, i) -> i to c }
    trackPoints.forEachIndexed { idx, pt ->
        val startColor = startByIdx[idx]
        val isStar = BoardGeometry.isStarGlobalIndex(BoardType.SIX_PLAYER, idx)
        val fill = startColor?.primary ?: if (isStar) Color(0xFFFFF3C7) else BoardCream
        drawCircle(color = Color.Black.copy(alpha = 0.22f), radius = cellRadius, center = pt + Offset(0f, 2f))
        drawCircle(color = fill, radius = cellRadius, center = pt)
        drawCircle(color = BoardCellBorder, radius = cellRadius, center = pt, style = Stroke(width = 1.6f))
        if (isStar && gameState.rules.safeStarsEnabled) {
            drawStarIcon(pt, cellRadius * 0.65f, GoldAccent)
        } else if (startColor != null) {
            drawStarIcon(pt, cellRadius * 0.6f, Color.White)
        }
    }

    // 6 Home Columns
    PlayerColor.sixPlayerOrder.forEach { color ->
        val cols = BoardGeometry.getHomeColumnCenters(BoardType.SIX_PLAYER, color)
        cols.forEach { norm ->
            val pt = Offset(norm.x * size.width, norm.y * size.height)
            drawCircle(color = color.primary, radius = cellRadius * 0.92f, center = pt)
            drawCircle(color = Color.White.copy(alpha = 0.85f), radius = cellRadius * 0.92f, center = pt, style = Stroke(width = 1.8f))
        }
    }

    // Central 6-slice hexagonal HOME hub
    val hubRadius = size.minDimension * 0.105f
    val angles = listOf(-120.0, -60.0, 0.0, 60.0, 120.0, 180.0)
    PlayerColor.sixPlayerOrder.forEachIndexed { idx, color ->
        val a1 = angles[idx] * PI / 180.0
        val a2 = (angles[idx] + 60.0) * PI / 180.0
        val p1 = Offset(center.x + cos(a1).toFloat() * hubRadius, center.y + sin(a1).toFloat() * hubRadius)
        val p2 = Offset(center.x + cos(a2).toFloat() * hubRadius, center.y + sin(a2).toFloat() * hubRadius)
        val slice = Path().apply {
            moveTo(center.x, center.y)
            lineTo(p1.x, p1.y)
            lineTo(p2.x, p2.y)
            close()
        }
        drawPath(slice, color = color.primary)
        drawPath(slice, color = Color.White, style = Stroke(width = 2f))
    }
    drawCircle(color = Color.White, radius = hubRadius * 0.34f, center = center)
    drawCircle(color = GoldAccent, radius = hubRadius * 0.32f, center = center, style = Stroke(width = 2.5f))
    val starLayout = textMeasurer.measure("★", style = TextStyle(fontSize = 13.sp, color = Color(0xFFD99000)))
    drawText(starLayout, topLeft = Offset(center.x - starLayout.size.width / 2f, center.y - starLayout.size.height / 2f))
}

private fun DrawScope.drawGlossyPawn(
    center: Offset,
    radius: Float,
    color: PlayerColor,
    isLegal: Boolean,
    pulseScale: Float,
    hasShield: Boolean,
    isFrozen: Boolean
) {
    if (isLegal) {
        drawCircle(
            color = GoldAccent.copy(alpha = 0.45f),
            radius = radius * 1.55f * pulseScale,
            center = center
        )
        drawCircle(
            color = Color.White,
            radius = radius * 1.32f,
            center = center,
            style = Stroke(width = 2.8f)
        )
    }

    if (hasShield) {
        drawCircle(
            color = CyanGlow.copy(alpha = 0.85f),
            radius = radius * 1.35f,
            center = center,
            style = Stroke(width = 3.2f)
        )
    }

    // Soft drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.38f),
        radius = radius * 1.05f,
        center = center + Offset(0f, radius * 0.25f)
    )

    // Outer white base ring
    drawCircle(
        color = Color.White,
        radius = radius * 1.02f,
        center = center
    )

    // 3D radial gradient pawn body
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.light, color.primary, color.dark),
            center = center - Offset(radius * 0.28f, radius * 0.28f),
            radius = radius * 1.25f
        ),
        radius = radius * 0.88f,
        center = center
    )

    // Inner glossy crown pin
    drawCircle(
        color = Color.White.copy(alpha = 0.72f),
        radius = radius * 0.28f,
        center = center - Offset(radius * 0.22f, radius * 0.24f)
    )

    if (isFrozen) {
        drawCircle(
            color = Color(0xFF9DF0FF).copy(alpha = 0.55f),
            radius = radius * 0.9f,
            center = center
        )
    }
}

private fun DrawScope.drawPowerTileBadge(
    center: Offset,
    radius: Float,
    power: PowerType,
    textMeasurer: TextMeasurer
) {
    val bgColor = when (power) {
        PowerType.FIRE, PowerType.TNT -> LudoRed
        PowerType.ICE_TILE, PowerType.SHIELD -> LudoBlue
        PowerType.ROCKET, PowerType.SPEED_BOOST -> LudoOrange
        PowerType.TELEPORT -> LudoPurple
        else -> LudoGreen
    }
    drawCircle(color = bgColor, radius = radius * 0.95f, center = center)
    drawCircle(color = Color.White, radius = radius * 0.95f, center = center, style = Stroke(width = 2f))
    val layout = textMeasurer.measure(
        power.shortCode,
        style = TextStyle(fontSize = 7.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
    )
    drawText(
        layout,
        topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f)
    )
}

private fun DrawScope.drawStarIcon(
    center: Offset,
    outerRadius: Float,
    color: Color
) {
    val innerRadius = outerRadius * 0.44f
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = (-90.0 + i * 36.0) * PI / 180.0
        val x = center.x + cos(angle).toFloat() * r
        val y = center.y + sin(angle).toFloat() * r
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color)
}
