package com.venuesync.app.ui.theme

import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The web's easings (index.css). Motion is for entrances and state changes only: nothing here loops on its own.
object Easings {
    /** Entrances. */
    val OutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    /** Colour and small state changes. */
    val Soft = CubicBezierEasing(0.25f, 1f, 0.5f, 1f)
    /** Sheets. */
    val Drawer = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
}

/**
 * False when the system's animator duration scale is 0 (Android's "remove animations"; the web checks
 * prefers-reduced-motion). Compose already scales its own animations by this setting; this exists to *skip* effects
 * that would otherwise sit frozen mid-way or burst at once: confetti, shimmer, the feed's scroll hint, entrances.
 */
@Composable
fun animationsOn(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f }
}

/** A duration in ms × the experience's motion scale (Classic softer and longer, Hype snappier). */
@Composable
fun scaled(ms: Int): Int = (ms * LocalExperience.current.motionScale).toInt()

/**
 * List entrances: fade + rise 14dp, staggered 48ms, capped at the 8th item, like the web's cards. Only items composed
 * within [Window] of the list appearing animate, so scrolling back up never replays them.
 */
class Entrance internal constructor(private val startedAt: Long, internal val enabled: Boolean) {
    internal fun animates() = enabled && SystemClock.uptimeMillis() - startedAt < Window
    private companion object { const val Window = 900L }
}

@Composable
fun rememberEntrance(): Entrance {
    val on = animationsOn()
    return remember { Entrance(SystemClock.uptimeMillis(), on) }
}

@Composable
fun Modifier.enter(entrance: Entrance, index: Int): Modifier {
    val animate = remember { entrance.animates() }
    if (!animate) return this
    val duration = scaled(480)
    val progress = remember { Animatable(0f) }
    val rise = with(LocalDensity.current) { 14.dp.toPx() }
    LaunchedEffect(Unit) {
        launch {
            delay(minOf(index, 7) * 48L)
            progress.animateTo(1f, tween(duration, easing = Easings.OutExpo))
        }
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * rise
    }
}

/** Press feedback: a 1dp downward nudge, never a scale, so text never resamples. */
@Composable
fun Modifier.pressNudge(interaction: InteractionSource): Modifier {
    val pressed by interaction.collectIsPressedAsState()
    val y by animateDpAsState(if (pressed) 1.dp else 0.dp, tween(scaled(120), easing = Easings.Soft), label = "nudge")
    return offset { IntOffset(0, y.roundToPx()) }
}
