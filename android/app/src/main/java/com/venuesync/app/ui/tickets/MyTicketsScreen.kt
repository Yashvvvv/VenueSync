package com.venuesync.app.ui.tickets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.TicketFilter
import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.core.model.TicketSummary
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.events.Centered
import com.venuesync.app.ui.events.LoadMoreOnEnd
import com.venuesync.app.ui.events.StubRow
import com.venuesync.app.ui.events.message
import com.venuesync.app.ui.theme.Mono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTicketsScreen(
    onBack: () -> Unit,
    onTicketClick: (String) -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyTicketsViewModel = hiltViewModel(),
) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("My tickets") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PrimaryTabRow(selectedTabIndex = filter.ordinal) {
                TicketFilter.entries.forEach { tab ->
                    Tab(
                        selected = tab == filter,
                        onClick = { viewModel.select(tab) },
                        text = { Text(tab.label()) },
                        // M3 paints the unselected tab in the selected colour; muted makes the choice readable.
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when (val s = state) {
                UiState.Loading -> Centered { CircularProgressIndicator() }
                UiState.Empty -> Centered {
                    Text(
                        if (filter == TicketFilter.Active) "No upcoming tickets. Tickets you buy show up here." else "No past tickets.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
                is UiState.Error -> Centered {
                    Text(s.error.message(), style = MaterialTheme.typography.bodyLarge)
                    if (s.error == ApiError.Unauthorized) {
                        Button(shape = MaterialTheme.shapes.small, onClick = onSignInClick, modifier = Modifier.padding(top = 12.dp)) { Text("Sign in") }
                    } else {
                        Button(shape = MaterialTheme.shapes.small, onClick = viewModel::retry, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
                    }
                }
                is UiState.Success -> TicketList(s.data, onTicketClick, onEndReached = viewModel::loadMore)
            }
        }
    }
}

@Composable
private fun TicketList(tickets: List<TicketSummary>, onTicketClick: (String) -> Unit, onEndReached: () -> Unit) {
    val listState = rememberLazyListState()
    LoadMoreOnEnd(listState, tickets.size, onEndReached)
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(tickets, key = TicketSummary::id) { ticket -> TicketRow(ticket, onClick = { onTicketClick(ticket.id) }) }
    }
}

@Composable
private fun TicketRow(ticket: TicketSummary, onClick: () -> Unit) {
    val spent = ticket.status != TicketStatus.Purchased && ticket.status != TicketStatus.Unknown
    StubRow(start = ticket.eventStart, onClick = onClick, spent = spent) {
        Text(ticket.eventName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(
            "1 × ${ticket.ticketTypeName}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        statusLabel(ticket.status)?.let {
            Text(it.uppercase(), style = MaterialTheme.typography.labelSmall, fontFamily = Mono, modifier = Modifier.padding(top = 4.dp))
        }
    }
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
