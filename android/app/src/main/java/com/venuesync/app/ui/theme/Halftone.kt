package com.venuesync.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt

/**
 * Hype's halftone screen (the web's acid `body::before`): a 4dp grid of white dots at 7% over everything, like a
 * xeroxed flyer. One tiny tile repeated by the GPU as a shader, drawn once over the whole tree: not a layer per
 * screen, and nothing that relayouts.
 */
@Composable
fun Modifier.halftone(): Modifier {
    val density = LocalDensity.current.density
    val brush = remember(density) {
        val cell = (4 * density).roundToInt().coerceAtLeast(2)
        val tile = ImageBitmap(cell, cell)
        val paint = Paint().apply {
            color = Color.White
            isAntiAlias = true
        }
        // The web's dot is solid to 0.5px and fades out by 1.2px; a 0.85dp antialiased dot is that, rasterised.
        Canvas(tile).drawCircle(Offset(cell / 2f, cell / 2f), 0.85f * density, paint)
        ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    }
    return drawWithContent {
        drawContent()
        drawRect(brush, alpha = 0.07f)
    }
}
