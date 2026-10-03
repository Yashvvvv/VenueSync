package com.venuesync.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.venuesync.app.R

// Bundled, not Downloadable Fonts: a door with no signal still renders codes in the right face.
// Both files are variable fonts; Font() sets the wght axis from the weight it's given (API 26+, our minSdk).
private val Weights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

/** Text: Archivo, as on the web. */
val Archivo = FontFamily(Weights.map { Font(R.font.archivo, it) })

/** Data: dates, prices, codes, anything a person compares character by character. JetBrains Mono, as on the web. */
val Mono = FontFamily(Weights.map { Font(R.font.jetbrains_mono, it) })

/** Hype's poster face, display sizes only. One weight (400): asking it for bold would synthesise a smear. */
val Anton = FontFamily(Font(R.font.anton, FontWeight.Normal))

// ── The web's utility styles (index.css). Callers uppercase meta and eyebrow text: TextStyle has no text-transform.

/** `.meta`: dates, venues, serials. Mono caps, 0.08em, muted at the call site. */
val Meta = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.08.em)

/** `.eyebrow`: a quiet label over content, in [ExperienceStyle.eyebrow]. Never a pill. */
val Eyebrow = Meta.copy(letterSpacing = 0.14.em)

/** `.num`: prices and totals. Tabular, so columns of figures line up. */
val Num = TextStyle(fontFamily = Mono, fontFeatureSettings = "tnum", letterSpacing = (-0.01).em)

/** A total: 20sp SemiBold mono. */
val Total = Num.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

/** `.display-hero` at its phone floor (the web's clamp starts at 2.75rem), × the experience's display scale. */
fun ExperienceStyle.displayHero(): TextStyle = display(44f, if (experience == Experience.Hype) displayWeight else FontWeight.Bold)

/** `.display-section`, floor 30sp, weight 650. */
fun ExperienceStyle.displaySection(): TextStyle =
    display(30f, if (experience == Experience.Hype) displayWeight else FontWeight(650), lineHeight = minOf(displayLineHeight, 1.1f))

private fun ExperienceStyle.display(size: Float, weight: FontWeight, lineHeight: Float = displayLineHeight) =
    TextStyle(
        fontFamily = displayFamily,
        fontWeight = weight,
        fontSize = (size * displayScale).sp,
        lineHeight = (size * displayScale * lineHeight).sp,
        letterSpacing = displayTracking,
    )

// Classic headlines stay SemiBold with -0.02em (the web's hero tracking crowds at phone sizes); Hype sets every headline
// in its poster face. The hero sizes (displayHero, 8.4) also take the scale and line height.
private fun TextStyle.display(style: ExperienceStyle) = when (style.experience) {
    Experience.Classic -> copy(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em)
    Experience.Hype -> copy(fontFamily = style.displayFamily, fontWeight = style.displayWeight, letterSpacing = style.displayTracking)
}
private fun TextStyle.text(weight: FontWeight) = copy(fontFamily = Archivo, fontWeight = weight)

internal fun typographyFor(style: ExperienceStyle) = Typography().run {
    copy(
        displayLarge = displayLarge.display(style),
        displayMedium = displayMedium.display(style),
        displaySmall = displaySmall.display(style),
        headlineLarge = headlineLarge.display(style),
        headlineMedium = headlineMedium.display(style),
        headlineSmall = headlineSmall.display(style),
        titleLarge = titleLarge.text(FontWeight.SemiBold),
        titleMedium = titleMedium.text(FontWeight.Medium),
        titleSmall = titleSmall.text(FontWeight.Medium),
        bodyLarge = bodyLarge.text(FontWeight.Normal),
        bodyMedium = bodyMedium.text(FontWeight.Normal),
        bodySmall = bodySmall.text(FontWeight.Normal),
        labelLarge = labelLarge.text(FontWeight.Medium),
        labelMedium = labelMedium.text(FontWeight.Medium),
        labelSmall = labelSmall.text(FontWeight.Medium),
    )
}
