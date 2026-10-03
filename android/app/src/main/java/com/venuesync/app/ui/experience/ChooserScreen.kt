package com.venuesync.app.ui.experience

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil.compose.AsyncImage
import com.venuesync.app.R
import com.venuesync.app.ui.theme.Experience
import com.venuesync.app.ui.theme.Lockup
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.Mono
import com.venuesync.app.ui.theme.VenueSyncTheme

/**
 * First-run fork (the web's AudienceChooser). Shown once, before any choice exists; the app has no deep links, so it
 * never stands between someone and an event they followed. Both options are previewed, not described: nobody can
 * pick an interface they haven't seen.
 */
@Composable
fun ChooserScreen(onChoose: (Experience) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Lockup()
        Text(
            "Two ways to use this. Pick one.",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 40.dp).semantics { heading() },
        )
        Text(
            "Same events, same tickets. Completely different app around them. You can switch back whenever you like.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 12.dp, bottom = 28.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            // Each card wears its own experience, so the choice is between two real looks, side by side.
            Experience.entries.sortedByDescending { it == Experience.Hype }.forEach { option ->
                VenueSyncTheme(experience = option) { ChoiceCard(option, onClick = { onChoose(option) }) }
            }
        }
        TextButton(
            onClick = { onChoose(Experience.Classic) },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp),
        ) {
            Text("Skip, just show me the events", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ChoiceCard(option: Experience, onClick: () -> Unit) {
    val style = LocalExperience.current
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val copy = option.copy
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.background, shape)
            .border(2.dp, colors.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick) // merges the card's text into one TalkBack node
            .padding(24.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                if (style.uppercaseDisplay) copy.name.uppercase() else copy.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = style.displayFamily,
                    fontWeight = if (option == Experience.Hype) style.displayWeight else FontWeight.SemiBold,
                    letterSpacing = if (option == Experience.Hype) 0.em else (-0.03).em,
                ),
                color = colors.onSurface,
            )
            Text(
                copy.forWho.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = Mono,
                letterSpacing = 0.12.em,
                color = style.eyebrow,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Text(copy.blurb, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        Box(
            Modifier.fillMaxWidth().padding(vertical = 24.dp).clearAndSetSemantics {},
            contentAlignment = Alignment.Center,
        ) {
            if (option == Experience.Hype) HypePreview() else ClassicPreview()
        }
        copy.bullets.forEach { bullet ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                Icon(painterResource(R.drawable.ph_check_bold), null, Modifier.size(12.dp), tint = colors.primary)
                Text(bullet, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp))
            }
        }
        Row(
            Modifier.padding(top = 24.dp).fillMaxWidth().height(44.dp).background(colors.primary, shape),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val cta = "Use ${copy.name}"
            Text(
                if (style.uppercaseCta) cta.uppercase() else cta,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.onPrimary,
            )
            Icon(painterResource(R.drawable.ph_arrow_right_bold), null, Modifier.padding(start = 8.dp).size(15.dp), tint = colors.onPrimary)
        }
    }
}

/** Sizes in dp, not sp: a miniature is a picture of a screen, and font scaling would break the picture. */
@Composable
private fun Dp.fixed(): TextUnit = with(LocalDensity.current) { this@fixed.toSp() }

/** One full-bleed feed panel and the 3-tab bar: what Hype actually looks like. */
@Composable
private fun HypePreview() {
    val colors = MaterialTheme.colorScheme
    val style = LocalExperience.current
    Column(Modifier.width(186.dp).background(colors.surfaceContainerLow).border(2.dp, colors.outlineVariant)) {
        Box(Modifier.fillMaxWidth().height(228.dp)) {
            AsyncImage(R.drawable.event_image_3, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.3f to Color.Transparent, 0.88f to colors.background)))
            Column(Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(
                    "NOCTURNE\nLATE SET",
                    style = TextStyle(fontFamily = style.displayFamily, fontSize = 19.dp.fixed(), lineHeight = 16.dp.fixed()),
                    color = colors.onSurface,
                )
                Text(
                    "FRI 14 MAR",
                    style = TextStyle(fontFamily = Mono, fontSize = 8.dp.fixed(), letterSpacing = 0.14.em),
                    color = style.eyebrow,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Box(Modifier.padding(top = 10.dp).fillMaxWidth().height(24.dp).background(colors.primary), contentAlignment = Alignment.Center) {
                    Text(
                        "GET TICKET",
                        style = TextStyle(fontSize = 9.dp.fixed(), fontWeight = FontWeight.SemiBold),
                        color = colors.onPrimary,
                    )
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().border(2.dp, colors.outlineVariant).padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            listOf(colors.primary, colors.onSurfaceVariant, colors.onSurfaceVariant).forEach {
                Box(Modifier.size(width = 24.dp, height = 6.dp).background(it))
            }
        }
    }
}

/** A dense 2×2 grid with a header: what Classic actually looks like. */
@Composable
private fun ClassicPreview() {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.small
    Column(Modifier.width(210.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.Bottom) {
            Text("On sale now", style = TextStyle(fontSize = 10.dp.fixed(), fontWeight = FontWeight.SemiBold), color = colors.onSurface)
            Spacer(Modifier.weight(1f))
            Text("View all", style = TextStyle(fontSize = 8.dp.fixed()), color = colors.onSurfaceVariant)
        }
        listOf(listOf(R.drawable.event_image_1, R.drawable.event_image_2), listOf(R.drawable.event_image_4, R.drawable.event_image_3))
            .forEachIndexed { row, images ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = if (row == 0) 0.dp else 8.dp)) {
                    images.forEachIndexed { col, image ->
                        val first = row == 0 && col == 0
                        Column(
                            Modifier.weight(1f).clip(shape).background(colors.surfaceContainerLow).border(1.dp, colors.outlineVariant, shape),
                        ) {
                            AsyncImage(image, null, Modifier.fillMaxWidth().height(46.dp), contentScale = ContentScale.Crop)
                            Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.fillMaxWidth(0.8f).height(4.dp).background(colors.onSurfaceVariant.copy(alpha = 0.5f)))
                                Box(
                                    Modifier.fillMaxWidth(0.4f).height(4.dp)
                                        .background(if (first) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.35f)),
                                )
                            }
                        }
                    }
                }
            }
    }
}
