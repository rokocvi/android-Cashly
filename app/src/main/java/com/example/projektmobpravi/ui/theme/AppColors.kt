package com.example.projektmobpravi.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class AppColors(
    val surfaceLight: Color,
    val surfaceCard: Color,
    val textDark: Color,
    val textMuted: Color
)

internal val LightAppColors = AppColors(
    surfaceLight = Color(0xFFF9FAFB),
    surfaceCard  = Color(0xFFFFFFFF),
    textDark     = Color(0xFF101828),
    textMuted    = Color(0xFF667085)
)

internal val DarkAppColors = AppColors(
    surfaceLight = Color(0xFF0B0B14),
    surfaceCard  = Color(0xFF1A1A2E),
    textDark     = Color(0xFFF0F0FF),
    textMuted    = Color(0xFFBBBBD0)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

data class AppThemeState(
    val isDark: Boolean,
    val toggle: () -> Unit
)

val LocalTheme = staticCompositionLocalOf { AppThemeState(isDark = false, toggle = {}) }
