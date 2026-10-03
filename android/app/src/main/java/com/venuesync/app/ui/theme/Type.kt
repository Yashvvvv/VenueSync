package com.venuesync.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import com.venuesync.app.R

// Bundled, not Downloadable Fonts: a door with no signal still renders codes in the right face.
// Both files are variable fonts; Font() sets the wght axis from the weight it's given (API 26+, our minSdk).
private val Weights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

/** Text: Archivo, as on the web. */
val Archivo = FontFamily(Weights.map { Font(R.font.archivo, it) })

/** Data: dates, prices, codes, anything a person compares character by character. JetBrains Mono, as on the web. */
val Mono = FontFamily(Weights.map { Font(R.font.jetbrains_mono, it) })

private fun TextStyle.display() = copy(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.02).em)
private fun TextStyle.text(weight: FontWeight) = copy(fontFamily = Archivo, fontWeight = weight)

// The web's display tracking is -0.035em at hero sizes; -0.02em keeps phone-size headlines from crowding.
val Typography = Typography().run {
    copy(
        displayLarge = displayLarge.display(),
        displayMedium = displayMedium.display(),
        displaySmall = displaySmall.display(),
        headlineLarge = headlineLarge.display(),
        headlineMedium = headlineMedium.display(),
        headlineSmall = headlineSmall.display(),
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
