package com.venuesync.app.ui.organizer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.R
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.EventCounts
import com.venuesync.app.core.model.EventStatus
import com.venuesync.app.core.model.OrganizerEvent
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.components.DisplayText
import com.venuesync.app.ui.components.ErrorState
import com.venuesync.app.ui.components.EventStatusChip
import com.venuesync.app.ui.components.MetaText
import com.venuesync.app.ui.components.SkeletonList
import com.venuesync.app.ui.components.StubEmptyState
import com.venuesync.app.ui.components.TicketStubSkeleton
import com.venuesync.app.ui.events.LoadMoreOnEnd
import com.venuesync.app.ui.events.StubRow
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.enter
import com.venuesync.app.ui.theme.rememberEntrance

/** The organizer's home: every event they run, counted and filtered by status. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerEventsScreen(
    /** Null where this is a root (Hype's Events tab): no back arrow. */
    onBack: (() -> Unit)?,
    onEventClick: (String) -> Unit,
    onNewEvent: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrganizerEventsViewModel = hiltViewModel(),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Coming back from an event or the form: the list may have changed there.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { DisplayText("My events", MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    onBack?.let {
                        IconButton(onClick = it) {
                            Icon(painterResource(R.drawable.ph_arrow_left), contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    TextButton(onClick = onNewEvent) {
                        Icon(painterResource(R.drawable.ph_plus_bold), contentDescription = null, Modifier.size(16.dp))
                        val label = "New event"
                        Text(
                            if (LocalExperience.current.uppercaseCta) label.uppercase() else label,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            StatusFilters(filter, counts, viewModel::select)
            when (val s = state) {
                UiState.Loading -> SkeletonList(4, "Loading your events") { TicketStubSkeleton() }
                UiState.Empty -> EmptyEvents(filter, onNewEvent)
                is UiState.Error -> if (s.error == ApiError.Forbidden) {
                    // The token says organizer but the server doesn't (a role removed since sign-in, or the reverse).
                    StubEmptyState(
                        R.drawable.ph_calendar_dots,
                        "This account can't manage events",
                        "Organizer tools need an organizer account. If you just became one, sign out and in again.",
                    )
                } else {
                    ErrorState(s.error, viewModel::retry)
                }
                is UiState.Success -> EventList(s.data, onEventClick, onEndReached = viewModel::loadMore)
            }
        }
    }
}

/** All plus one chip per status, each with its count once counts arrive. Tapping the active one is a no-op. */
@Composable
private fun StatusFilters(selected: EventStatus?, counts: EventCounts?, onSelect: (EventStatus?) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val border = LocalExperience.current.border
    val options: List<Triple<EventStatus?, String, Long?>> = listOf(
        Triple(null, "All", counts?.let { it.draft + it.published + it.cancelled + it.completed }),
        Triple(EventStatus.Draft, "Drafts", counts?.draft),
        Triple(EventStatus.Published, "On sale", counts?.published),
        Triple(EventStatus.Completed, "Ended", counts?.completed),
        Triple(EventStatus.Cancelled, "Cancelled", counts?.cancelled),
    )
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (status, label, count) ->
            val active = status == selected
            FilterChip(
                selected = active,
                onClick = { onSelect(status) },
                label = { Text(if (count == null) label else "$label  $count") },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = colors.onSurface,
                    selectedContainerColor = colors.primary,
                    selectedLabelColor = colors.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = active,
                    borderColor = colors.outlineVariant,
                    selectedBorderColor = colors.primary,
                    borderWidth = border,
                    selectedBorderWidth = border,
                ),
            )
        }
    }
}

@Composable
private fun EmptyEvents(filter: EventStatus?, onNewEvent: () -> Unit) {
    when (filter) {
        null -> StubEmptyState(
            R.drawable.ph_calendar_dots,
            "No events yet",
            "Create your first event. It stays a draft, visible only to you, until you publish it.",
            actionLabel = "New event",
            onAction = onNewEvent,
        )
        EventStatus.Draft -> StubEmptyState(
            R.drawable.ph_pencil_simple,
            "No drafts",
            "Drafts are events you're still setting up. Nobody else sees them.",
            actionLabel = "New event",
            onAction = onNewEvent,
        )
        EventStatus.Published -> StubEmptyState(R.drawable.ph_ticket, "Nothing on sale", "Publish a draft to put its tickets on sale.")
        EventStatus.Completed -> StubEmptyState(R.drawable.ph_clock_fill, "No ended events", "Events move here once they're over.")
        EventStatus.Cancelled, EventStatus.Unknown ->
            StubEmptyState(R.drawable.ph_x_circle_fill, "No cancelled events", "Cancelled events stay here with their tickets.")
    }
}

@Composable
private fun EventList(events: List<OrganizerEvent>, onEventClick: (String) -> Unit, onEndReached: () -> Unit) {
    val listState = rememberLazyListState()
    val entrance = rememberEntrance()
    LoadMoreOnEnd(listState, events.size, onEndReached)
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(if (LocalExperience.current.hardShadow) 20.dp else 12.dp),
    ) {
        itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
            Column(Modifier.enter(entrance, index)) { OrganizerEventRow(event, onClick = { onEventClick(event.id) }) }
        }
    }
}

/** A stub like the public card (the date counterfoil), with what an organizer checks first: status and sales. */
@Composable
private fun OrganizerEventRow(event: OrganizerEvent, onClick: () -> Unit) {
    val over = event.status == EventStatus.Cancelled || event.status == EventStatus.Completed
    StubRow(start = event.start, onClick = onClick, spent = over) {
        EventStatusChip(event.status)
        Text(
            event.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp),
        )
        MetaText(salesLine(event), Modifier.padding(top = 2.dp))
        if (event.venue.isNotBlank()) {
            Text(
                event.venue,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** "40 of 100 sold", "40 sold" when any type is unlimited, or "No ticket types". */
internal fun salesLine(event: OrganizerEvent): String {
    if (event.ticketTypes.isEmpty()) return "No ticket types"
    val capacities = event.ticketTypes.map { it.capacity }
    return if (capacities.any { it == null }) {
        "${event.sold} sold"
    } else {
        "${event.sold} of ${capacities.sumOf { it!!.toLong() }} sold"
    }
}
