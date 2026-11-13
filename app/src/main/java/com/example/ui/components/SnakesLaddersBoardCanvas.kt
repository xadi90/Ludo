package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.game.BoardJump
import com.example.game.SnakesLaddersEngine
import com.example.game.SnakesLaddersState
import com.example.game.SnakesPlayer
import com.example.ui.theme.GoldAccent
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@Composable
fun SnakesLaddersBoardCanvas(
    state: SnakesLaddersState,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val boardSize = min(maxWidth, maxHeight)
        val boardShape = RoundedCornerShape(16.dp)

        Canvas(
            modifier = Modifier
                .size(boardSize)
                .aspectRatio(1f)
                .shadow(16.dp, boardShape, ambientColor = GoldAccent)
                .clip(boardShape)
                .background(Color(0xFFFFF5E8))
                .border(3.5.dp, GoldAccent, boardShape)
                .padding(3.dp)
                .testTag("snakes_ladders_board")
        ) {
            val w = size.width
            val cell = w / 10f

            // 1. Draw 10x10 checkerboard cells + numbers 1..100
            drawSnakesGridCells(cell, textMeasurer)

            // 2. Draw Victory Flag on 100
            drawGoalFlag(cell)

            // 3. Draw Ladders underneath snakes
            SnakesLaddersEngine.ladders.forEach { ladder ->
                drawLadder(ladder, w, cell)
            }

            // 4. Draw Illustrated S-curve Snakes
            SnakesLaddersEngine.snakes.forEachIndexed { idx, snake ->
                drawSnake(snake, idx, w, cell)
            }

            // 5. Draw Players' 3D Glossy Tokens (with multi-token offset on shared tiles)
            val grouped = state.players.mapIndexed { idx, p -> idx to p }.groupBy { it.second.square }
            grouped.forEach { (sq, group) ->
                val centerNorm = SnakesLaddersEngine.squareNormalizedCenter(sq.coerceAtLeast(1))
                val baseCenter = Offset(centerNorm.x * w, centerNorm.y * w)
                val radius = cell * 0.27f

                group.forEachIndexed { slotIdx, (playerIdx, player) ->
                    val offset = if (group.size == 1) {
                        Offset.Zero
                    } else {
                        val angle = (2.0 * PI * slotIdx / group.size) - PI / 4
                        Offset(
                            x = (cos(angle) * cell * 0.21f).toFloat(),
                            y = (sin(angle) * cell * 0.21f).toFloat()
                        )
                    }
                    drawSnakesToken(
                        player = player,
                        center = baseCenter + offset,
                        radius = if (group.size > 1) radius * 0.85f else radius,
                        isActive = playerIdx == state.currentTurnIndex && !state.isMatchOver
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSnakesGridCells(cell: Float, textMeasurer: TextMeasurer) {
    val lightTile = Color(0xFFFFF6EA)
    val warmTile = Color(0xFFF6D6D3)
    val accentTile = Color(0xFFFEE5C8)

    for (sq in 1..100) {
        val (r, c) = SnakesLaddersEngine.squareToRowCol(sq)
        val topLeft = Offset(c * cell, r * cell)
        val fill = when {
            sq == 100 -> Color(0xFFFFE57F)
            sq == 1 -> Color(0xFFD5F5E3)
            (r + c) % 2 == 0 -> lightTile
            sq % 5 == 0 -> accentTile
            else -> warmTile
        }
        drawRect(color = fill, topLeft = topLeft, size = Size(cell, cell))
        drawRect(
            color = Color(0xFFD7A89E).copy(alpha = 0.55f),
            topLeft = topLeft,
            size = Size(cell, cell),
            style = Stroke(width = 1.2f)
        )

        val numberStr = sq.toString()
        val fontSizeSp = (cell * 0.28f / density).coerceIn(8f, 13f).sp
        val measured = textMeasurer.measure(
            text = numberStr,
            style = TextStyle(
                fontSize = fontSizeSp,
                fontWeight = FontWeight.ExtraBold,
                color = if (sq == 100) Color(0xFFB31B1B) else Color(0xFF2B2129)
            )
        )
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                x = topLeft.x + (cell - measured.size.width) * 0.18f,
                y = topLeft.y + cell * 0.08f
            )
        )
    }
}

private fun DrawScope.drawGoalFlag(cell: Float) {
    val (r, c) = SnakesLaddersEngine.squareToRowCol(100)
    val cx = (c + 0.65f) * cell
    val cy = (r + 0.65f) * cell
    // Flag pole
    drawLine(
        color = Color(0xFF4A2511),
        start = Offset(cx - cell * 0.14f, cy + cell * 0.2f),
        end = Offset(cx - cell * 0.14f, cy - cell * 0.2f),
        strokeWidth = 3f,
        cap = StrokeCap.Round
    )
    val flagPath = Path().apply {
        moveTo(cx - cell * 0.14f, cy - cell * 0.2f)
        lineTo(cx + cell * 0.2f, cy - cell * 0.09f)
        lineTo(cx - cell * 0.14f, cy + cell * 0.02f)
        close()
    }
    drawPath(flagPath, color = Color(0xFFE5222A))
}

private fun DrawScope.drawLadder(jump: BoardJump, boardPx: Float, cell: Float) {
    val startN = SnakesLaddersEngine.squareNormalizedCenter(jump.from)
    val endN = SnakesLaddersEngine.squareNormalizedCenter(jump.to)
    val p1 = Offset(startN.x * boardPx, startN.y * boardPx)
    val p2 = Offset(endN.x * boardPx, endN.y * boardPx)

    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val nx = -dy / len
    val ny = dx / len
    val halfWidth = cell * 0.21f
    val perp = Offset(nx * halfWidth, ny * halfWidth)

    val railWidth = (cell * 0.10f).coerceIn(3f, 6.5f)
    val railColor = Color(0xFF38BDF8)
    val railBorder = Color(0xFF0369A1)
    val shadowOffset = Offset(3f, 4f)

    // Drop shadow
    drawLine(Color.Black.copy(alpha = 0.18f), p1 + perp + shadowOffset, p2 + perp + shadowOffset, strokeWidth = railWidth, cap = StrokeCap.Round)
    drawLine(Color.Black.copy(alpha = 0.18f), p1 - perp + shadowOffset, p2 - perp + shadowOffset, strokeWidth = railWidth, cap = StrokeCap.Round)

    // Rungs
    val rungs = (len / (cell * 0.42f)).toInt().coerceIn(4, 16)
    for (i in 1 until rungs) {
        val t = i.toFloat() / rungs.toFloat()
        val mid = Offset(p1.x + dx * t, p1.y + dy * t)
        drawLine(
            color = railBorder,
            start = mid - perp,
            end = mid + perp,
            strokeWidth = railWidth * 0.9f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFFBAE6FD),
            start = mid - perp,
            end = mid + perp,
            strokeWidth = railWidth * 0.55f,
            cap = StrokeCap.Round
        )
    }

    // Side rails
    listOf(perp, -perp).forEach { side ->
        drawLine(railBorder, p1 + side, p2 + side, strokeWidth = railWidth + 2f, cap = StrokeCap.Round)
        drawLine(railColor, p1 + side, p2 + side, strokeWidth = railWidth * 0.72f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawSnake(jump: BoardJump, idx: Int, boardPx: Float, cell: Float) {
    val headN = SnakesLaddersEngine.squareNormalizedCenter(jump.from)
    val tailN = SnakesLaddersEngine.squareNormalizedCenter(jump.to)
    val head = Offset(headN.x * boardPx, headN.y * boardPx)
    val tail = Offset(tailN.x * boardPx, tailN.y * boardPx)

    val dx = tail.x - head.x
    val dy = tail.y - head.y
    val len = hypot(dx, dy).coerceAtLeast(1f)
    val nx = -dy / len
    val ny = dx / len
    val amp = cell * 0.48f * if (idx % 2 == 0) 1f else -1f

    val c1 = Offset(head.x + dx * 0.30f + nx * amp, head.y + dy * 0.30f + ny * amp)
    val c2 = Offset(head.x + dx * 0.70f - nx * amp, head.y + dy * 0.70f - ny * amp)

    val bodyColor = when (idx % 4) {
        0 -> Color(0xFF16A34A)
        1 -> Color(0xFF10B981)
        2 -> Color(0xFF65A30D)
        else -> Color(0xFFEA580C)
    }
    val bellyColor = when (idx % 4) {
        0 -> Color(0xFF86EFAC)
        1 -> Color(0xFF6EE7B7)
        2 -> Color(0xFFBEF264)
        else -> Color(0xFFFDBA74)
    }

    val bodyPath = Path().apply {
        moveTo(head.x, head.y)
        cubicTo(c1.x, c1.y, c2.x, c2.y, tail.x, tail.y)
    }

    val bodyStroke = (cell * 0.24f).coerceIn(6f, 14f)

    // Drop shadow
    drawPath(
        path = bodyPath,
        color = Color.Black.copy(alpha = 0.22f),
        style = Stroke(width = bodyStroke + 3f, cap = StrokeCap.Round)
    )
    // Dark outline
    drawPath(
        path = bodyPath,
        color = Color(0xFF0B3B1F),
        style = Stroke(width = bodyStroke + 2.5f, cap = StrokeCap.Round)
    )
    // Main snake body
    drawPath(
        path = bodyPath,
        color = bodyColor,
        style = Stroke(width = bodyStroke, cap = StrokeCap.Round)
    )
    // Inner highlight stripe
    drawPath(
        path = bodyPath,
        color = bellyColor.copy(alpha = 0.7f),
        style = Stroke(width = bodyStroke * 0.42f, cap = StrokeCap.Round)
    )

    // Forked tongue pointing slightly upward/outward from head
    val angle = atan2(dy, dx)
    val tongueStart = Offset(head.x - cos(angle) * cell * 0.14f, head.y - sin(angle) * cell * 0.14f)
    val tongueTip = Offset(head.x - cos(angle) * cell * 0.30f, head.y - sin(angle) * cell * 0.30f)
    drawLine(Color(0xFFEF4444), tongueStart, tongueTip, strokeWidth = 2.4f, cap = StrokeCap.Round)

    // Snake head
    val headRadius = cell * 0.22f
    drawCircle(Color(0xFF0B3B1F), radius = headRadius + 1.8f, center = head)
    drawCircle(bodyColor, radius = headRadius, center = head)

    // Eyes
    val eyeOffsetPerp = Offset(nx * headRadius * 0.42f, ny * headRadius * 0.42f)
    listOf(head + eyeOffsetPerp, head - eyeOffsetPerp).forEach { eye ->
        drawCircle(Color.White, radius = headRadius * 0.28f, center = eye)
        drawCircle(Color.Black, radius = headRadius * 0.15f, center = eye)
    }
}

private fun DrawScope.drawSnakesToken(
    player: SnakesPlayer,
    center: Offset,
    radius: Float,
    isActive: Boolean
) {
    if (isActive) {
        drawCircle(
            color = GoldAccent.copy(alpha = 0.45f),
            radius = radius * 1.38f,
            center = center
        )
    }
    // Shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.35f),
        radius = radius,
        center = center + Offset(2f, 3f)
    )
    // White outer rim
    drawCircle(
        color = Color.White,
        radius = radius,
        center = center
    )
    // 3D Glossy sphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(player.color.light, player.color.primary, player.color.dark),
            center = Offset(center.x - radius * 0.3f, center.y - radius * 0.3f),
            radius = radius * 1.4f
        ),
        radius = radius * 0.82f,
        center = center
    )
    // Specular highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.6f),
        radius = radius * 0.26f,
        center = Offset(center.x - radius * 0.28f, center.y - radius * 0.28f)
    )
}
