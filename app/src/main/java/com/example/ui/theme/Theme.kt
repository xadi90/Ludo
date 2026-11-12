package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val GoldYellowLightFallback = Color(0xFFFFEA8A)

private val RoyalDarkColorScheme = darkColorScheme(
    primary = LudoBlue,
    onPrimary = Color.White,
    primaryContainer = ArenaBrightBlue,
    onPrimaryContainer = Color.White,
    secondary = GoldAccent,
    onSecondary = Color(0xFF1A1200),
    secondaryContainer = Color(0xFF473700),
    onSecondaryContainer = GoldYellowLightFallback,
    tertiary = CyanGlow,
    onTertiary = Color(0xFF00242B),
    background = ArenaDeepNavy,
    onBackground = TextPrimaryDark,
    surface = ArenaCardSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF143A92),
    onSurfaceVariant = TextSecondaryDark,
    outline = ArenaCardBorder,
    error = LudoRed,
    onError = Color.White
)

private val VividLightColorScheme = lightColorScheme(
    primary = LudoBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E6FF),
    onPrimaryContainer = Color(0xFF0B2A6F),
    secondary = LudoOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE5CC),
    onSecondaryContainer = Color(0xFF592400),
    tertiary = LudoGreen,
    onTertiary = Color.White,
    background = LightSkyBackground,
    onBackground = LightTextPrimary,
    surface = LightCardSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = Color(0xFFE0EBFF),
    onSurfaceVariant = LightTextSecondary,
    outline = Color(0xFF9BB7F5),
    error = LudoRed,
    onError = Color.White
)

val LudoShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LudoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) RoyalDarkColorScheme else VividLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LudoTypography,
        shapes = LudoShapes,
        content = content
    )
}
