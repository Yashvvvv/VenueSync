package com.venuesync.app.ui.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.venuesync.app.R
import com.venuesync.app.core.model.Event
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.components.CardDay
import com.venuesync.app.ui.components.Clock
import com.venuesync.app.ui.components.DisplayText
import com.venuesync.app.ui.components.ErrorState
import com.venuesync.app.ui.components.StubButton
import com.venuesync.app.ui.components.addToCalendar
import com.venuesync.app.ui.components.shareEvent
import com.venuesync.app.ui.components.shimmer
import com.venuesync.app.ui.events.EventsViewModel
import com.venuesync.app.ui.experience.LocalChooseExperience
import com.venuesync.app.ui.theme.BrandMark
import com.venuesync.app.ui.theme.Easings
import com.venuesync.app.ui.theme.Experience
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.Mono
import com.venuesync.app.ui.theme.animationsOn
import com.venuesync.app.ui.theme.displayHero
import com.venuesync.app.ui.theme.displaySection
import com.venuesync.app.ui.theme.eventImage
import com.venuesync.app.ui.theme.eventImageFor
import com.venuesync.app.ui.theme.scaled
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/**
 * Hype's home (the web's hype landing): one event per screen, snapped, with the next page pulled in by a closing
 * screen. No top nav: a floating mark and a way back to Classic; the tab bar (in the shell) is the navigation.
 * Searching and browsing everything live in the Explore tab, so the feed is only ever the feed.
 */
@Composable
fun FeedScreen(
    onEventClick: (String) -> Unit,
    onBrowseAll: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val endReached by viewModel.endReached.collectAsStateWithLifecycle()
    val choose = LocalChooseExperience.current

    val events = (state as? UiState.Success)?.data.orEmpty()
    val pager = rememberPagerState { events.size + 1 } // + the closing screen
    // Reaching the closing screen pulls the next page, so the feed keeps going instead of stopping dead.
    LaunchedEffect(pager, events.size) {
        snapshotFlow { pager.currentPage }.distinctUntilChanged().filter { it == events.size && events.isNotEmpty() }
            .collect { viewModel.loadMore() }
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        when (val s = state) {
            UiState.Loading -> PanelSkeleton()
            is UiState.Error -> ErrorState(s.error, viewModel::retry, Modifier.align(Alignment.Center))
            UiState.Empty -> EmptyFeed(onBrowseAll, Modifier.align(Alignment.Center))
            is UiState.Success -> {
                val seen = remember { mutableStateListOf<String>() }
                VerticalPager(pager, Modifier.fillMaxSize(), key = { if (it < events.size) events[it].id else "end" }) { page ->
                    if (page < events.size) {
                        val event = events[page]
                        EventPanel(event, onGet = { onEventClick(event.id) }, animate = event.id !in seen)
                        LaunchedEffect(event.id) { if (event.id !in seen) seen += event.id }
                    } else {
                        ClosingPanel(endReached, onBrowseAll = onBrowseAll)
                    }
                }
                ScrollHint(visible = pager.currentPage == 0 && events.size > 0, Modifier.align(Alignment.BottomCenter))
            }
        }
        // Floating brand and the way back. No top nav: the tab bar is the navigation.
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandMark(28.dp, Modifier.clearAndSetSemantics { contentDescription = "VenueSync" })
            Box(Modifier.weight(1f))
            ClassicViewChip { choose(Experience.Classic) }
        }
    }
}

/** One event, one screen: photo, a scrim that carries the type, the poster name, one action, and the rail. */
@Composable
private fun EventPanel(event: Event, onGet: () -> Unit, animate: Boolean) {
    val colors = MaterialTheme.colorScheme
    val style = LocalExperience.current
    val context = LocalContext.current
    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            eventImage(event.id, event.imageUrl), null, Modifier.fillMaxSize(),
            error = painterResource(eventImageFor(event.id)), contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to colors.background.copy(alpha = 0.2f),
                    0.5f to colors.background.copy(alpha = 0.75f),
                    1f to colors.background,
                ),
            ),
        )
        // Thumb-reachable on the right edge.
        Column(Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 200.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            RailButton(R.drawable.ph_share_network_bold, "Share ${event.name}") { shareEvent(context, event.id, event.name) }
            event.start?.let { start ->
                RailButton(R.drawable.ph_calendar_blank_bold, "Add ${event.name} to calendar") {
                    addToCalendar(context, event.name, event.venue, start, event.end)
                }
            }
        }
        Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 76.dp, bottom = 32.dp).riseIn(animate)) {
            // Name, date and venue read as one announcement; the button is its own stop.
            Column(Modifier.semantics(mergeDescendants = true) {}) {
                event.start?.let {
                    Text(
                        "${it.format(CardDay)} · ${it.format(Clock)}".uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = Mono, letterSpacing = 0.16.em),
                        color = colors.primary,
                    )
                }
                DisplayText(
                    event.name,
                    style.displayHero(),
                    Modifier.padding(top = 12.dp).widthIn(max = 320.dp).semantics { heading() },
                    maxLines = 4,
                )
                event.venue?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
                }
            }
            StubButton("Get ticket", onGet, Modifier.padding(top = 28.dp).height(52.dp), icon = R.drawable.ph_arrow_right_bold)
        }
    }
}

/** Fade + rise the first time a panel is seen (the web's whileInView once), × the experience's motion scale. */
@Composable
private fun Modifier.riseIn(animate: Boolean): Modifier {
    if (!animate || !animationsOn()) return this
    val progress = remember { Animatable(0f) }
    val rise = with(LocalDensity.current) { 26.dp.toPx() }
    val duration = scaled(500)
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(duration, easing = Easings.OutExpo)) }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * rise
    }
}

@Composable
private fun RailButton(icon: Int, label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(44.dp)
            .background(colors.background.copy(alpha = 0.7f))
            .border(2.dp, colors.outlineVariant)
            .clickable(role = Role.Button, onClickLabel = null, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), null, Modifier.size(18.dp), tint = colors.onSurface)
    }
}

@Composable
private fun ClassicViewChip(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Text(
        "CLASSIC VIEW",
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = Mono, fontSize = 10.sp, letterSpacing = 0.12.em),
        color = colors.onSurfaceVariant,
        modifier = Modifier
            .background(colors.background.copy(alpha = 0.7f))
            .border(2.dp, colors.outlineVariant)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Switch to the Classic view" }
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

/** The end of a page: pulls the next one (keep scrolling) or says plainly that this is everything, and points at Explore. */
@Composable
private fun ClosingPanel(endReached: Boolean, onBrowseAll: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        DisplayText(
            if (endReached) "That is everything on sale" else "Keep scrolling",
            LocalExperience.current.displaySection().copy(textAlign = TextAlign.Center),
            Modifier.semantics { heading() },
        )
        StubButton("Explore all events", onBrowseAll, Modifier.padding(top = 28.dp))
    }
}

@Composable
private fun EmptyFeed(onBrowseAll: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        DisplayText("Nothing on sale yet", LocalExperience.current.displaySection().copy(textAlign = TextAlign.Center), Modifier.semantics { heading() })
        Text(
            "No events are published right now. New ones land here the moment an organizer puts them on sale.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** The first screen still has to say the feed continues: a caret that fades in after 1.2s, gone after panel 1. */
@Composable
private fun ScrollHint(visible: Boolean, modifier: Modifier = Modifier) {
    val on = animationsOn()
    var armed by remember { mutableStateOf(!on) }
    LaunchedEffect(Unit) {
        delay(1_200)
        armed = true
    }
    val alpha by animateFloatAsState(if (visible && armed) 1f else 0f, tween(if (on) 500 else 0), label = "scrollHint")
    Icon(
        painterResource(R.drawable.ph_caret_down_bold),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(bottom = 8.dp).size(18.dp).graphicsLayer { this.alpha = alpha },
    )
}

/** A panel-shaped placeholder: the poster block at the bottom, where the real one lands. */
@Composable
private fun PanelSkeleton() {
    val tone = shimmer()
    Box(Modifier.fillMaxSize().semantics { contentDescription = "Loading events" }) {
        Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(width = 120.dp, height = 12.dp).background(tone))
            Box(Modifier.size(width = 260.dp, height = 48.dp).background(tone))
            Box(Modifier.size(width = 200.dp, height = 48.dp).background(tone))
            Box(Modifier.padding(top = 16.dp).size(width = 160.dp, height = 52.dp).background(tone))
        }
    }
}
