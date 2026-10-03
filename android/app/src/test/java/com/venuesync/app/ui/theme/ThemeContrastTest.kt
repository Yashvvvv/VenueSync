package com.venuesync.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG AA for every pair the screens draw. Change a token and this says whether it still passes. */
class ThemeContrastTest {

    private val schemes = mapOf("dark" to DarkColors, "light" to LightColors)

    @Test fun textOnEverySurface() = each { s ->
        s.surfaces().forEach { (name, bg) ->
            assertContrast("onSurface on $name", s.onSurface, bg, 4.5)
            assertContrast("onSurfaceVariant on $name", s.onSurfaceVariant, bg, 4.5)
            assertContrast("error on $name", s.error, bg, 4.5)
        }
    }

    // M3 draws primary as text: text buttons (dialogs sit on surfaceContainerHigh), tabs, focused field labels.
    @Test fun primaryAsText() = each { s ->
        s.surfaces().forEach { (name, bg) -> assertContrast("primary on $name", s.primary, bg, 4.5) }
    }

    @Test fun textOnFills() = each { s ->
        assertContrast("onPrimary on primary", s.onPrimary, s.primary, 4.5)
        assertContrast("onError on error", s.onError, s.error, 4.5)
    }

    // WCAG 1.4.11: the outline is what identifies a text field.
    @Test fun fieldEdges() = each { s ->
        assertContrast("outline on surface", s.outline, s.surface, 3.0)
        assertContrast("outline on surfaceContainerLow", s.outline, s.surfaceContainerLow, 3.0)
    }

    private fun each(check: (ColorScheme) -> Unit) = schemes.forEach { (mode, scheme) ->
        try {
            check(scheme)
        } catch (e: AssertionError) {
            throw AssertionError("$mode: ${e.message}")
        }
    }

    private fun ColorScheme.surfaces() = mapOf(
        "surface" to surface,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
    )
}

internal fun assertContrast(what: String, fg: Color, bg: Color, min: Double) {
    val (hi, lo) = listOf(fg.luminance(), bg.luminance()).sortedDescending()
    val ratio = (hi + 0.05) / (lo + 0.05)
    assertTrue("$what is ${"%.2f".format(ratio)}:1, needs $min:1", ratio >= min)
}
