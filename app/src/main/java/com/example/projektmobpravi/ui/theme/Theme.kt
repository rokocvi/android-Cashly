package com.example.projektmobpravi.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme = lightColorScheme(
    primary            = DeepGreen,
    onPrimary          = TextOnDark,
    primaryContainer   = PrimaryContainer,
    onPrimaryContainer = DeepGreen,
    secondary          = MintGreen,
    onSecondary        = TextOnDark,
    tertiary           = AccentGold,
    onTertiary         = TextOnDark,
    background         = SurfaceLight,
    onBackground       = TextDark,
    surface            = SurfaceCard,
    onSurface          = TextDark,
    onSurfaceVariant   = TextMuted,
    error              = ErrorRed,
    onError            = TextOnDark,
)

@Composable
fun PROJEKTMOBPRAVITheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography  = Typography,
        content     = content
    )
}
