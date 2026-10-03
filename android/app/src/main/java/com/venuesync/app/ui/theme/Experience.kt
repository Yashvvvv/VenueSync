package com.venuesync.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

/**
 * The web's audience fork (frontend/src/lib/audience.ts): two experiences, not two skins. Hype is one event per
 * screen behind a bottom tab bar; Classic is the editorial list behind a top bar. Inner screens are one tree, painted
 * by [ExperienceStyle].
 */
enum class Experience { Classic, Hype }

/** What M3 has no slot for: the web's per-vibe knobs (`--radius`, `--display-*`, `--plate-2`, `--motion-scale`). */
@Immutable
data class ExperienceStyle(
    val experience: Experience,
    val radius: Dp,
    /** Card and control edges. Hype's 2dp edges also get a hard offset shadow instead of elevation. */
    val border: Dp,
    val hardShadow: Boolean,
    val displayFamily: FontFamily,
    /** Anton has one weight: asking it for bold synthesises a smear. */
    val displayWeight: FontWeight,
    val displayScale: Float,
    val displayTracking: TextUnit,
    /** Multiplier on font size. Hype's 0.9 is only safe because caps have no descenders. */
    val displayLineHeight: Float,
    val uppercaseDisplay: Boolean,
    val uppercaseCta: Boolean,
    /** Mono labels over content: primary in Classic, the magenta plate in Hype. */
    val eyebrow: Color,
    /** Multiplies every entrance and state-change duration. */
    val motionScale: Float,
    val halftone: Boolean,
    val confetti: List<Color>,
)

/** Varies per experience, so it earns a CompositionLocal (DoorColors doesn't, and stays an object). */
val LocalExperience = staticCompositionLocalOf { experienceStyle(Experience.Classic, ClassicDark) }

/** Classic's eyebrow follows its scheme's primary (ember in dark, deep ember on paper). */
internal fun experienceStyle(experience: Experience, colors: ColorScheme) = when (experience) {
    Experience.Classic -> ExperienceStyle(
        experience = experience,
        radius = 10.dp, border = 1.dp, hardShadow = false,
        displayFamily = Archivo, displayWeight = FontWeight.Bold,
        displayScale = 0.95f, displayTracking = (-0.028).em, displayLineHeight = 1.06f,
        uppercaseDisplay = false, uppercaseCta = false,
        eyebrow = colors.primary,
        motionScale = 1.15f, halftone = false,
        confetti = listOf(Color(0xFFE8A06A), Color(0xFFDD8449), Color(0xFFF2C39C)),
    )
    Experience.Hype -> ExperienceStyle(
        experience = experience,
        radius = 0.dp, border = 2.dp, hardShadow = true,
        displayFamily = Archivo, displayWeight = FontWeight.Bold, // Anton arrives with the assets (8.2)
        displayScale = 1.22f, displayTracking = (-0.005).em, displayLineHeight = 0.9f,
        uppercaseDisplay = true, uppercaseCta = true,
        eyebrow = Magenta,
        motionScale = 0.7f, halftone = true,
        confetti = listOf(Color(0xFFB6F24A), Color(0xFFD8FF7D), Color(0xFFFF5FB0)),
    )
}
