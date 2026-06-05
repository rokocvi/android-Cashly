package com.example.projektmobpravi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme = lightColorScheme(
    primary = DeepGreen,
    secondary = MintGreen,
    tertiary = AccentGold,
    background = SurfaceLight,
    surface = SurfaceCard,
    error = ErrorRed,
    onPrimary = TextOnDark,
    onSecondary = TextOnDark,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun PROJEKTMOBPRAVITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        content = content
    )
}