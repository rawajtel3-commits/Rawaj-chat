package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PlumPrimaryLight,
    onPrimary = PlumOnPrimaryLight,
    primaryContainer = PlumPrimary,
    onPrimaryContainer = PlumPrimaryContainerLight,
    secondary = MauveSecondaryLight,
    onSecondary = MauveOnSecondaryLight,
    secondaryContainer = MauveSecondaryContainerLight,
    onSecondaryContainer = MauveOnSecondaryContainerLight,
    tertiary = BerryTertiaryLight,
    onTertiary = BerryOnTertiaryLight,
    tertiaryContainer = BerryTertiaryContainerLight,
    onTertiaryContainer = BerryOnTertiaryContainerLight,
    background = PurpleDarkBackground,
    onBackground = TextPrimaryDark,
    surface = PurpleDarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = PurpleDarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = BorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = PlumPrimary,
    onPrimary = Color.White,
    primaryContainer = PlumPrimaryContainerLight,
    onPrimaryContainer = PlumOnPrimaryContainerLight,
    secondary = MauveSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3DAF8),
    onSecondaryContainer = Color(0xFF25112B),
    tertiary = BerryTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF3A071C),
    background = PurpleLightBackground,
    onBackground = TextPrimaryLight,
    surface = PurpleLightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = PurpleLightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight
)

@Composable
fun PrivateCabinsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
