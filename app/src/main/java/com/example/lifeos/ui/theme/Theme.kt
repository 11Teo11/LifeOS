package com.example.lifeos.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LifeOSColorScheme = lightColorScheme(
    primary = Lavender,
    onPrimary = Color.White,
    primaryContainer = LavenderLight,
    onPrimaryContainer = LavenderDark,

    secondary = BlushPink,
    onSecondary = Color.White,
    secondaryContainer = BlushPinkLight,
    onSecondaryContainer = BlushPinkDark,

    tertiary = MintGreen,
    onTertiary = Color.White,
    tertiaryContainer = MintGreenLight,
    onTertiaryContainer = MintGreenDark,

    background = WarmWhite,
    onBackground = TextDark,
    surface = WarmWhite,
    onSurface = TextDark,
    surfaceVariant = SoftGray,
    onSurfaceVariant = TextMuted
)

@Composable
fun LifeOSTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LifeOSColorScheme,
        typography = Typography,
        content = content
    )
}