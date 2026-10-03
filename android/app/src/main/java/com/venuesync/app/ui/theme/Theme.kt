package com.venuesync.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Flat fills, one accent: secondary/tertiary are neutral, containers equal their base, and surfaceTint is the surface
// itself so elevated components get no ember wash.
internal val DarkColors = darkColorScheme(
    primary = Ember, onPrimary = Ink, primaryContainer = Ember, onPrimaryContainer = Ink,
    inversePrimary = DeepEmber,
    secondary = DarkForeground, onSecondary = DarkBackground,
    secondaryContainer = DarkAccent, onSecondaryContainer = DarkForeground,
    tertiary = DarkForeground, onTertiary = DarkBackground,
    tertiaryContainer = DarkAccent, onTertiaryContainer = DarkForeground,
    background = DarkBackground, onBackground = DarkForeground,
    surface = DarkBackground, onSurface = DarkForeground,
    surfaceVariant = DarkSecondary, onSurfaceVariant = DarkMuted,
    surfaceTint = DarkBackground,
    inverseSurface = DarkForeground, inverseOnSurface = DarkBackground,
    error = DarkError, onError = Ink,
    outline = DarkOutline, outlineVariant = DarkBorder,
    surfaceBright = DarkAccent, surfaceDim = DarkBackground,
    surfaceContainerLowest = DarkBackground, surfaceContainerLow = DarkCard, surfaceContainer = DarkPopover,
    surfaceContainerHigh = DarkSecondary, surfaceContainerHighest = DarkAccent,
)

internal val LightColors = lightColorScheme(
    primary = DeepEmber, onPrimary = Color.White, primaryContainer = DeepEmber, onPrimaryContainer = Color.White,
    inversePrimary = Ember,
    secondary = LightForeground, onSecondary = LightBackground,
    secondaryContainer = LightContainerHighest, onSecondaryContainer = LightForeground,
    tertiary = LightForeground, onTertiary = LightBackground,
    tertiaryContainer = LightContainerHighest, onTertiaryContainer = LightForeground,
    background = LightBackground, onBackground = LightForeground,
    surface = LightBackground, onSurface = LightForeground,
    surfaceVariant = LightContainerHigh, onSurfaceVariant = LightMuted,
    surfaceTint = LightBackground,
    inverseSurface = LightForeground, inverseOnSurface = LightBackground,
    error = LightError, onError = Color.White,
    outline = LightOutline, outlineVariant = LightBorder,
    surfaceBright = LightCard, surfaceDim = LightContainerHighest,
    surfaceContainerLowest = LightCard, surfaceContainerLow = LightCard, surfaceContainer = LightContainer,
    surfaceContainerHigh = LightContainerHigh, surfaceContainerHighest = LightContainerHighest,
)

/** 6dp everywhere (web --radius: 0.375rem): cards, fields, menus, dialogs, sheets. Buttons pass shapes.small. */
private val Radius = RoundedCornerShape(6.dp)
internal val VenueSyncShapes = Shapes(
    extraSmall = Radius, small = Radius, medium = Radius, large = Radius, extraLarge = Radius,
)

/** Brand colours in both modes. No dynamic colour: on Android 12+ it would paint the app from the wallpaper. */
@Composable
fun VenueSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = VenueSyncShapes,
        content = content,
    )
}
