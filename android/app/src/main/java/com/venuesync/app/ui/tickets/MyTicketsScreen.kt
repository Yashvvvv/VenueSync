package com.venuesync.app.ui.tickets

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TabRowDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.Dp
import com.venuesync.app.ui.theme.LocalExperience
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.R
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.TicketFilter
import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.core.model.TicketSummary
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.components.DisplayText
import com.venuesync.app.ui.components.ErrorState
import com.venuesync.app.ui.components.SkeletonList
import com.venuesync.app.ui.components.StubEmptyState
import com.venuesync.app.ui.components.TicketStubRow
import com.venuesync.app.ui.components.TicketStubSkeleton
import com.venuesync.app.ui.events.LoadMoreOnEnd
import com.venuesync.app.ui.theme.enter
import com.venuesync.app.ui.theme.rememberEntrance
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The attendee's tickets, as printed stubs. [onBack] is null where this is a root (Hype's Tickets tab); [actions]
 * carries the account menu there, since Hype has no other top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTicketsScreen(
    onBack: (() -> Unit)?,
    onTicketClick: (String) -> Unit,
    onSignInClick: () -> Unit,
    onBrowse: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    viewModel: MyTicketsViewModel = hiltViewModel(),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val savedAt by viewModel.savedAt.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                // headlineSmall is the experience's display face: Anton caps in Hype, Archivo in Classic.
                title = { DisplayText("My tickets", MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(painterResource(R.drawable.ph_arrow_left), contentDescription = "Back")
                        }
                    }
                },
                actions = actions,
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val style = LocalExperience.current
            PrimaryTabRow(
                selectedTabIndex = filter.ordinal,
                indicator = {
                    // Hype prints a square rule under the tab; Classic keeps M3's rounded one.
                    TabRowDefaults.PrimaryIndicator(
                        Modifier.tabIndicatorOffset(filter.ordinal, matchContentSize = true),
                        width = Dp.Unspecified,
                        shape = if (style.hardShadow) RectangleShape else RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                    )
                },
            ) {
                TicketFilter.entries.forEach { tab ->
                    Tab(
                        selected = tab == filter,
                        onClick = { viewModel.select(tab) },
                        text = { Text(if (style.uppercaseCta) tab.label().uppercase() else tab.label()) },
                        // M3 paints the unselected tab in the selected colour; muted makes the choice readable.
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when (val s = state) {
                UiState.Loading -> SkeletonList(4, "Loading tickets") { TicketStubSkeleton() }
                UiState.Empty -> if (filter == TicketFilter.Active) {
                    StubEmptyState(
                        R.drawable.ph_ticket,
                        "No tickets yet",
                        "Tickets you buy show up here, with the code you scan at the door.",
                        actionLabel = "Browse events",
                        onAction = onBrowse,
                    )
                } else {
                    StubEmptyState(R.drawable.ph_ticket, "No past tickets", "Tickets for events that have ended show up here.")
                }
                is UiState.Error -> if (s.error == ApiError.Unauthorized) {
                    ErrorState(s.error, onSignInClick, actionLabel = "Sign in")
                } else {
                    ErrorState(s.error, viewModel::retry)
                }
                is UiState.Success -> {
                    savedAt?.let { OfflineBanner(it, onRetry = viewModel::retry) }
                    TicketList(s.data, onTicketClick, onEndReached = viewModel::loadMore)
                }
            }
        }
    }
}

/** Events are wall-clock India time (ADR-003), so "has it ended" is judged on that clock, not the phone's zone. */
private val EventZone = ZoneId.of("Asia/Kolkata")

@Composable
private fun TicketList(tickets: List<TicketSummary>, onTicketClick: (String) -> Unit, onEndReached: () -> Unit) {
    val listState = rememberLazyListState()
    val entrance = rememberEntrance()
    val now = remember(tickets) { LocalDateTime.now(EventZone) }
    LoadMoreOnEnd(listState, tickets.size, onEndReached)
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(if (LocalExperience.current.hardShadow) 20.dp else 12.dp),
    ) {
        itemsIndexed(tickets, key = { _, ticket -> ticket.id }) { index, ticket ->
            TicketStubRow(ticket, now, onClick = { onTicketClick(ticket.id) }, modifier = Modifier.enter(entrance, index))
        }
    }
}

/** No signal: say plainly that this is the copy on the phone and how old it is. */
@Composable
internal fun OfflineBanner(savedAt: Instant, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 12.dp)) {
            Text(
                "Offline. Saved on this phone ${savedAgo(savedAt)}.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            )
            TextButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

private fun savedAgo(savedAt: Instant): String {
    val now = System.currentTimeMillis()
    if (now - savedAt.toEpochMilli() < DateUtils.MINUTE_IN_MILLIS) return "just now"
    return DateUtils.getRelativeTimeSpanString(savedAt.toEpochMilli(), now, DateUtils.MINUTE_IN_MILLIS).toString()
}

private fun TicketFilter.label() = when (this) {
    TicketFilter.Active -> "Upcoming"
    TicketFilter.Past -> "Past"
}

/** Only states worth calling out; a normal purchased ticket shows no label. */
internal fun statusLabel(status: TicketStatus): String? = when (status) {
    TicketStatus.Purchased, TicketStatus.Unknown -> null
    TicketStatus.Used -> "Used"
    TicketStatus.Expired -> "Expired"
    TicketStatus.Cancelled -> "Cancelled"
}
