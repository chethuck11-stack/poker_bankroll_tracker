package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PokerDarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = OnPrimaryGreen,
    primaryContainer = EmeraldProfitContainer,
    onPrimaryContainer = EmeraldProfitOnContainer,
    secondary = SecondaryCyan,
    onSecondary = OnSecondaryCyan,
    secondaryContainer = CyanAccentContainer,
    onSecondaryContainer = Color(0xFFBAE6FD),
    tertiary = GoldAccent,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = GoldAccentContainer,
    onTertiaryContainer = GoldAccentOnContainer,
    error = RedLoss,
    onError = Color.White,
    errorContainer = RedLossContainer,
    onErrorContainer = RedLossOnContainer,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    outlineVariant = Color(0xFF334155)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force sleek poker dark theme by default as requested
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PokerDarkColorScheme,
        typography = Typography,
        content = content
    )
}
