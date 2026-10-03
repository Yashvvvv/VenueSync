package com.venuesync.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// The web's ticket motif (.perf + .notch in index.css): a dashed tear line with a punched hole at each end.

/**
 * A 6dp card with a half-circle cut out of both edges where the perforation runs: [at] from the start edge when
 * [vertical], from the top otherwise. A real cut, so the border follows the hole and the page shows through.
 */
data class TicketShape(val at: Dp, val vertical: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = with(density) { NotchRadius.toPx() }
        val offset = with(density) { at.toPx() }
        val card = Path().apply {
            addRoundRect(RoundRect(Rect(Offset.Zero, size), CornerRadius(with(density) { 6.dp.toPx() })))
        }
        val holes = Path().apply {
            if (vertical) {
                val x = if (layoutDirection == LayoutDirection.Ltr) offset else size.width - offset
                addOval(Rect(Offset(x, 0f), radius))
                addOval(Rect(Offset(x, size.height), radius))
            } else {
                addOval(Rect(Offset(0f, offset), radius))
                addOval(Rect(Offset(size.width, offset), radius))
            }
        }
        return Outline.Generic(Path.combine(PathOperation.Difference, card, holes))
    }
}

private val NotchRadius = 10.dp

/** The dashed tear line, centred in its bounds. Give it 1dp across and the full length of the card. */
@Composable
fun Perforation(vertical: Boolean, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
        val (start, end) = if (vertical) {
            Offset(size.width / 2, 0f) to Offset(size.width / 2, size.height)
        } else {
            Offset(0f, size.height / 2) to Offset(size.width, size.height / 2)
        }
        drawLine(color, start, end, strokeWidth = 1.dp.toPx(), pathEffect = dash)
    }
}

/** Card stock: the web's `bg-card border`. Clickable when [onClick] is given. */
@Composable
fun StubCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    if (onClick != null) {
        OutlinedCard(onClick, modifier, shape = shape, colors = colors, border = border, content = content)
    } else {
        OutlinedCard(modifier, shape = shape, colors = colors, border = border, content = content)
    }
}
