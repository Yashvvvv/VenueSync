package com.venuesync.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.animationsOn
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * The web's success burst (canvas-confetti: 90 pieces, 68° spread, from 60% height, the experience's colours), once.
 * Survives rotation without replaying; skipped entirely when animations are off. Decorative: hidden from TalkBack.
 */
@Composable
fun ConfettiOnce(modifier: Modifier = Modifier) {
    var pending by rememberSaveable { mutableStateOf(true) }
    if (!pending || !animationsOn()) return
    val palette = LocalExperience.current.confetti
    val pieces = remember { List(Count) { Piece.random(palette) } }
    val tick = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        tick.animateTo(Ticks.toFloat(), tween(Ticks * 1000 / 60, easing = LinearEasing)) // canvas-confetti runs at 60 ticks/s
        pending = false
    }
    Canvas(modifier.clearAndSetSemantics {}) {
        val t = tick.value
        val px = 1.dp.toPx() // the web's CSS pixels
        val origin = Offset(size.width / 2, size.height * 0.6f)
        // Velocity decays geometrically per tick, so position is a closed-form sum; gravity adds 3px per tick.
        val travelled = (1 - Decay.pow(t)) / (1 - Decay)
        val alpha = (1f - t / Ticks).coerceIn(0f, 1f)
        pieces.forEach { p ->
            val x = origin.x + cos(p.angle) * p.velocity * travelled * px
            val y = origin.y - sin(p.angle) * p.velocity * travelled * px + Gravity * t * px
            rotate(p.spin * t, Offset(x, y)) {
                drawRect(p.color, Offset(x - 4 * px, y - 2.5f * px), Size(8 * px, 5 * px), alpha = alpha)
            }
        }
    }
}

private class Piece(val angle: Float, val velocity: Float, val spin: Float, val color: Color) {
    companion object {
        fun random(palette: List<Color>) = Piece(
            // 90° (straight up) ± half the spread, in radians.
            angle = Math.toRadians(90.0 + (Random.nextDouble() - 0.5) * Spread).toFloat(),
            velocity = StartVelocity * 0.5f + Random.nextFloat() * StartVelocity,
            spin = (Random.nextFloat() - 0.5f) * 20f,
            color = palette[Random.nextInt(palette.size)],
        )
    }
}

private const val Count = 90
private const val Spread = 68.0
private const val StartVelocity = 45f
private const val Decay = 0.9f
private const val Gravity = 3f
private const val Ticks = 200
