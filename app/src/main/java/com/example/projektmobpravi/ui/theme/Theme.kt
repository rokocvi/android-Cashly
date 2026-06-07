package com.example.projektmobpravi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary            = DeepGreen,
    onPrimary          = TextOnDark,
    primaryContainer   = PrimaryContainer,
    onPrimaryContainer = DeepGreen,
    secondary          = MintGreen,
    onSecondary        = TextOnDark,
    tertiary           = AccentGold,
    onTertiary         = TextOnDark,
    background         = Color(0xFFF9FAFB),
    onBackground       = Color(0xFF101828),
    surface            = Color(0xFFFFFFFF),
    onSurface          = Color(0xFF101828),
    onSurfaceVariant   = Color(0xFF667085),
    error              = ErrorRed,
    onError            = TextOnDark,
)

private val DarkColorScheme = darkColorScheme(
    primary            = MintGreen,
    onPrimary          = TextOnDark,
    primaryContainer   = Color(0xFF3B2880),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary          = AccentGold,
    onSecondary        = Color(0xFF101828),
    tertiary           = AccentGold,
    onTertiary         = Color(0xFF101828),
    background         = Color(0xFF0B0B14),
    onBackground       = Color(0xFFF0F0FF),
    surface            = Color(0xFF1A1A2E),
    onSurface          = Color(0xFFF0F0FF),
    onSurfaceVariant   = Color(0xFFBBBBD0),
    error              = ErrorRed,
    onError            = TextOnDark,
)

@Composable
fun PROJEKTMOBPRAVITheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val appColors   = if (darkTheme) DarkAppColors   else LightAppColors

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = Typography,
            content     = content
        )
    }
}
