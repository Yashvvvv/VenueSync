package com.venuesync.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

/**
 * Flat fills, one accent: secondary/tertiary are neutral, containers equal their base, and surfaceTint is the surface
 * itself so elevated components get no accent wash. [low] is the card, [container] the menu, [high] the dialog.
 */
@Suppress("LongParameterList")
private fun scheme(
    dark: Boolean,
    background: Color, low: Color, container: Color, high: Color, highest: Color,
    foreground: Color, muted: Color, outline: Color, border: Color,
    primary: Color, onPrimary: Color, error: Color, onError: Color,
    // Text on inverseSurface (snackbar actions): the other mode's accent.
    inversePrimary: Color,
): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = primary, onPrimary = onPrimary, primaryContainer = primary, onPrimaryContainer = onPrimary,
        inversePrimary = inversePrimary,
        secondary = foreground, onSecondary = background,
        secondaryContainer = highest, onSecondaryContainer = foreground,
        tertiary = foreground, onTertiary = background,
        tertiaryContainer = highest, onTertiaryContainer = foreground,
        background = background, onBackground = foreground,
        surface = background, onSurface = foreground,
        surfaceVariant = high, onSurfaceVariant = muted,
        surfaceTint = background,
        inverseSurface = foreground, inverseOnSurface = background,
        error = error, onError = onError,
        outline = outline, outlineVariant = border,
        // Dark grounds lift toward the light; on paper the card is the brightest surface.
        surfaceBright = if (dark) highest else low, surfaceDim = if (dark) background else highest,
        surfaceContainerLowest = if (dark) background else low, surfaceContainerLow = low,
        surfaceContainer = container, surfaceContainerHigh = high, surfaceContainerHighest = highest,
    )
}

internal val ClassicDark = scheme(
    dark = true,
    background = CalmBackground, low = CalmCard, container = CalmPopover, high = CalmSecondary, highest = CalmAccent,
    foreground = CalmForeground, muted = CalmMuted, outline = CalmOutline, border = CalmBorder,
    primary = CalmEmber, onPrimary = Ink, error = ErrorText, onError = Ink, inversePrimary = DeepEmber,
)

internal val ClassicLight = scheme(
    dark = false,
    background = LightBackground, low = LightCard, container = LightContainer, high = LightContainerHigh,
    highest = LightContainerHighest,
    foreground = LightForeground, muted = LightMuted, outline = LightOutline, border = LightBorder,
    primary = DeepEmber, onPrimary = Color.White, error = LightError, onError = Color.White,
    inversePrimary = CalmEmber,
)

/** Black stock. Border and field edge are the same 0.5 grey: in Hype every edge is a printed 2dp rule. */
internal val HypeColors = scheme(
    dark = true,
    background = AcidBackground, low = AcidCard, container = AcidPopover, high = AcidSecondary, highest = AcidAccent,
    foreground = AcidForeground, muted = AcidMuted, outline = AcidBorder, border = AcidBorder,
    primary = Lime, onPrimary = LimeInk, error = ErrorText, onError = Ink, inversePrimary = LimeInk,
)

/** Hype is dark only (a flyer on black stock); Classic follows the system. */
internal fun colorsFor(experience: Experience, darkTheme: Boolean): ColorScheme = when {
    experience == Experience.Hype -> HypeColors
    darkTheme -> ClassicDark
    else -> ClassicLight
}

/** Brand colours in every mode. No dynamic colour: on Android 12+ it would paint the app from the wallpaper. */
@Composable
fun VenueSyncTheme(
    experience: Experience = Experience.Classic,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = colorsFor(experience, darkTheme)
    val style = remember(experience, colors) { experienceStyle(experience, colors) }
    // One radius everywhere (web --radius): cards, fields, menus, dialogs, sheets. Buttons pass shapes.small.
    val shapes = remember(style.radius) { RoundedCornerShape(style.radius).let { Shapes(it, it, it, it, it) } }
    CompositionLocalProvider(LocalExperience provides style) {
        MaterialTheme(
            colorScheme = colors,
            typography = remember(style) { typographyFor(style) },
            shapes = shapes,
            content = content,
        )
    }
}
