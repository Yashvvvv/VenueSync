package com.venuesync.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG AA for every pair the screens draw, in every experience. Change a token and this says whether it still passes. */
class ThemeContrastTest {

    private val schemes = mapOf(
        "classic dark" to (Experience.Classic to ClassicDark),
        "classic light" to (Experience.Classic to ClassicLight),
        "hype" to (Experience.Hype to HypeColors),
    )

    @Test fun textOnEverySurface() = each { s, _ ->
        s.surfaces().forEach { (name, bg) ->
            assertContrast("onSurface on $name", s.onSurface, bg, 4.5)
            assertContrast("onSurfaceVariant on $name", s.onSurfaceVariant, bg, 4.5)
            assertContrast("error on $name", s.error, bg, 4.5)
        }
    }

    // M3 draws primary as text: text buttons (dialogs sit on surfaceContainerHigh), tabs, focused field labels.
    @Test fun primaryAsText() = each { s, _ ->
        s.surfaces().forEach { (name, bg) -> assertContrast("primary on $name", s.primary, bg, 4.5) }
    }

    // Eyebrows are 11sp mono: small text, so 4.5 on every surface they can sit on.
    @Test fun eyebrowAsText() = each { s, style ->
        s.surfaces().forEach { (name, bg) -> assertContrast("eyebrow on $name", style.eyebrow, bg, 4.5) }
    }

    // Ink on the accent in every experience: white fails on ember, lime and magenta alike.
    @Test fun textOnFills() = each { s, _ ->
        assertContrast("onPrimary on primary", s.onPrimary, s.primary, 4.5)
        assertContrast("onError on error", s.onError, s.error, 4.5)
    }

    // WCAG 1.4.11: the outline is what identifies a text field, on any surface it sits on.
    @Test fun fieldEdges() = each { s, _ ->
        s.surfaces().forEach { (name, bg) -> assertContrast("outline on $name", s.outline, bg, 3.0) }
    }

    // Hype prints two plates: the eyebrow is the second one, never a repeat of the first.
    @Test fun hypeHasTwoPlates() {
        assertNotEquals(HypeColors.primary, experienceStyle(Experience.Hype, HypeColors).eyebrow)
    }

    // The detail line is titleLarge (22sp), not "large text", so 4.5 rather than 3. "Scan next" inverts the pair.
    @Test fun doorAnswers() = with(DoorColors) {
        mapOf("Go" to Go, "Stop" to Stop, "Caution" to Caution, "Neutral" to Neutral).forEach { (name, door) ->
            assertContrast("OnDoor on $name", OnDoor, door, 4.5)
        }
    }

    private fun each(check: (ColorScheme, ExperienceStyle) -> Unit) = schemes.forEach { (mode, pair) ->
        val (experience, scheme) = pair
        try {
            check(scheme, experienceStyle(experience, scheme))
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
