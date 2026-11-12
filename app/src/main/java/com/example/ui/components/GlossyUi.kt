package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ArenaBrightBlue
import com.example.ui.theme.ArenaCardSurface
import com.example.ui.theme.ArenaDeepNavy
import com.example.ui.theme.ArenaRoyalBlue
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.LudoBlue
import com.example.ui.theme.LudoBlueDark
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RoyalArenaBackground(
    darkTheme: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arena_particles")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "drift"
    )

    val topColor = if (darkTheme) ArenaRoyalBlue else Color(0xFFD8E6FF)
    val midColor = if (darkTheme) ArenaDeepNavy else Color(0xFFEEF4FF)
    val glowColor = if (darkTheme) ArenaBrightBlue.copy(alpha = 0.55f) else LudoBlue.copy(alpha = 0.16f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(topColor, midColor, if (darkTheme) Color(0xFF030B22) else Color(0xFFE0ECFF))
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.18f),
                    radius = size.maxDimension * 0.55f
                ),
                radius = size.maxDimension * 0.55f,
                center = Offset(size.width * 0.5f, size.height * 0.18f)
            )
            // Floating subtle starlight bokeh particles
            for (i in 0 until 14) {
                val baseX = ((i * 73) % 100) / 100f
                val baseY = ((i * 41) % 100) / 100f
                val px = size.width * baseX + sin(drift + i) * 14f
                val py = size.height * baseY + cos(drift * 0.8f + i) * 12f
                val r = (3f + (i % 4) * 2.5f)
                drawCircle(
                    color = (if (i % 3 == 0) GoldAccent else Color.White).copy(alpha = if (darkTheme) 0.14f else 0.25f),
                    radius = r,
                    center = Offset(px, py)
                )
            }
        }
        content()
    }
}

@Composable
fun GlossyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    topColor: Color = LudoBlue,
    bottomColor: Color = LudoBlueDark,
    borderColor: Color = Color.White.copy(alpha = 0.45f),
    textColor: Color = Color.White,
    icon: ImageVector? = null,
    height: Dp = 54.dp,
    enabled: Boolean = true,
    testTag: String = ""
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.5f),
        label = "btn_scale"
    )

    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .scale(scale)
            .height(height)
            .shadow(
                elevation = if (enabled) 8.dp else 2.dp,
                shape = shape,
                ambientColor = topColor,
                spotColor = bottomColor
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = if (enabled) listOf(topColor, bottomColor)
                    else listOf(Color.Gray.copy(alpha = 0.6f), Color.DarkGray.copy(alpha = 0.6f))
                )
            )
            .border(1.5.dp, borderColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Glossy top specular strip
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(height * 0.42f)
                .padding(horizontal = 6.dp, vertical = 3.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.34f), Color.Transparent)
                    )
                )
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.4.sp
                ),
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlowingGameCard(
    modifier: Modifier = Modifier,
    accentColor: Color = LudoBlue,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier
            .shadow(8.dp, shape, ambientColor = accentColor.copy(alpha = 0.3f))
            .clip(shape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.5.dp, accentColor.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun LudoTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = ArenaCardSurface.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier.size(48.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("toolbar_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing()
    }
}

fun avatarTitle(avatarId: Int): String = when (avatarId % 8) {
    0 -> "Royal Knight"
    1 -> "Neon Fox"
    2 -> "Golden Eagle"
    3 -> "Star Wizard"
    4 -> "Cyber Panda"
    5 -> "Crimson Lion"
    6 -> "Emerald Dragon"
    else -> "Grand Monarch"
}
