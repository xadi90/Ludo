package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.delay

/**
 * 3D-styled animated Ludo Die with tumble rotation, bounce physics,
 * glowing active-turn border, golden 6-celebration aura, and pip rendering.
 */
@Composable
fun Dice3D(
    value: Int?,
    isRolling: Boolean,
    canRoll: Boolean,
    accentColor: Color,
    onRollClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp
) {
    val spin = remember { Animatable(0f) }
    val bounce = remember { Animatable(1f) }
    var displayFace by remember(value) { mutableIntStateOf(value ?: 6) }

    val pulseTransition = rememberInfiniteTransition(label = "dice_pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(isRolling) {
        if (isRolling) {
            spin.snapTo(0f)
            bounce.snapTo(1f)
            bounce.animateTo(1.24f, tween(160))
            bounce.animateTo(0.94f, tween(110))
            bounce.animateTo(1f, tween(90))
        }
    }

    LaunchedEffect(isRolling) {
        if (isRolling) {
            spin.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 340, easing = LinearEasing)
            )
        } else {
            spin.snapTo(0f)
        }
    }

    LaunchedEffect(isRolling, value) {
        if (isRolling) {
            while (true) {
                displayFace = ((displayFace % 6) + 1)
                delay(55L)
            }
        } else if (value != null) {
            displayFace = value
        }
    }

    val isSixCelebration = !isRolling && value == 6
    val glowBorderColor = when {
        isSixCelebration -> GoldAccent
        canRoll -> accentColor
        else -> Color.White.copy(alpha = 0.35f)
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("dice_roll_button")
            .graphicsLayer {
                rotationZ = spin.value
                rotationX = if (isRolling) spin.value * 0.5f else 0f
                scaleX = bounce.value * (if (canRoll && !isRolling) 0.96f + (pulse - 0.85f) * 0.25f else 1f)
                scaleY = bounce.value * (if (canRoll && !isRolling) 0.96f + (pulse - 0.85f) * 0.25f else 1f)
            }
            .shadow(
                elevation = if (canRoll || isSixCelebration) 14.dp else 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = glowBorderColor,
                spotColor = glowBorderColor
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFFFFFFFF), Color(0xFFE8EEF9), Color(0xFFD2DDF2))
                )
            )
            .border(
                width = if (canRoll || isSixCelebration) 3.dp else 1.5.dp,
                color = glowBorderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(enabled = canRoll && !isRolling, onClick = onRollClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.78f)) {
            val w = this.size.width
            val h = this.size.height
            // Inner beveled face
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(Color.White, Color(0xFFF3F6FD))
                ),
                topLeft = Offset(0f, 0f),
                size = Size(w, h),
                cornerRadius = CornerRadius(w * 0.18f, w * 0.18f)
            )

            val pipColor = when {
                displayFace == 6 -> Color(0xFFD99000)
                displayFace == 1 -> Color(0xFFE5222A)
                else -> Color(0xFF142754)
            }
            val r = w * 0.105f

            fun pip(nx: Float, ny: Float) {
                val cx = w * nx
                val cy = h * ny
                drawCircle(
                    color = Color.Black.copy(alpha = 0.2f),
                    radius = r,
                    center = Offset(cx, cy + 1.5f)
                )
                drawCircle(
                    color = pipColor,
                    radius = r,
                    center = Offset(cx, cy)
                )
            }

            when (displayFace) {
                1 -> pip(0.5f, 0.5f)
                2 -> {
                    pip(0.26f, 0.26f); pip(0.74f, 0.74f)
                }
                3 -> {
                    pip(0.26f, 0.26f); pip(0.5f, 0.5f); pip(0.74f, 0.74f)
                }
                4 -> {
                    pip(0.26f, 0.26f); pip(0.74f, 0.26f)
                    pip(0.26f, 0.74f); pip(0.74f, 0.74f)
                }
                5 -> {
                    pip(0.26f, 0.26f); pip(0.74f, 0.26f)
                    pip(0.5f, 0.5f)
                    pip(0.26f, 0.74f); pip(0.74f, 0.74f)
                }
                else -> {
                    pip(0.26f, 0.22f); pip(0.74f, 0.22f)
                    pip(0.26f, 0.50f); pip(0.74f, 0.50f)
                    pip(0.26f, 0.78f); pip(0.74f, 0.78f)
                }
            }
        }
        if (isSixCelebration) {
            Text(
                text = "6!",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFD99000),
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
