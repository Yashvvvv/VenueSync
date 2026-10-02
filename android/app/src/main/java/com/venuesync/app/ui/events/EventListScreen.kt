package com.venuesync.app.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.Event
import com.venuesync.app.ui.account.AccountAction
import com.venuesync.app.ui.common.UiState
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onEventClick: (String) -> Unit,
    onSignInClick: () -> Unit,
    onMyTicketsClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("VenueSync") },
                actions = {
                    AccountAction(
                        onSignInClick = onSignInClick,
                        onMyTicketsClick = onMyTicketsClick,
                        onScanClick = onScanClick,
                    )
                },
            )
        },
    ) { innerPadding ->
        EventBrowser(onEventClick, modifier = Modifier.padding(innerPadding))
    }
}

/** Search + paged event list. Shared by the public event list and the staff event picker. */
@Composable
internal fun EventBrowser(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChanged,
            placeholder = { Text("Search events") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        when (val s = state) {
            UiState.Loading -> Centered { CircularProgressIndicator() }
            UiState.Empty -> Centered { Text("No events found") }
            is UiState.Error -> Centered {
                Text(s.error.message(), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = viewModel::retry, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
            }
            is UiState.Success -> EventList(s.data, onEventClick, onEndReached = viewModel::loadMore)
        }
    }
}

@Composable
private fun EventList(events: List<Event>, onEventClick: (String) -> Unit, onEndReached: () -> Unit) {
    val listState = rememberLazyListState()
    LoadMoreOnEnd(listState, events.size, onEndReached)
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(events, key = Event::id) { event -> EventCard(event, onClick = { onEventClick(event.id) }) }
    }
}

@Composable
private fun EventCard(event: Event, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(event.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            event.venue?.let { Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            event.start?.let {
                Text(it.format(DateFormat), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Fires [onEndReached] when the last item scrolls into view. */
@Composable
internal fun LoadMoreOnEnd(listState: LazyListState, count: Int, onEndReached: () -> Unit) {
    LaunchedEffect(listState, count) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { it != null && it >= count - 1 }
            .collect { onEndReached() }
    }
}

@Composable
internal fun Centered(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

internal val DateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

/** User-facing copy only. Raw server/exception text never reaches the screen. */
internal fun ApiError.message(): String = when (this) {
    ApiError.Network -> "No connection. Check your network and try again."
    ApiError.Unauthorized -> "Please sign in to continue."
    ApiError.Forbidden -> "Your account can't do this."
    ApiError.NotFound -> "Not found."
    ApiError.Conflict -> "This changed while you were looking. Refresh and try again."
    ApiError.SoldOut -> "Sold out."
    ApiError.NotOnSale -> "Tickets aren't on sale right now."
    ApiError.RateLimited -> "Too many requests. Wait a moment and try again."
    ApiError.InvalidResponse -> "We couldn't load this. Try again later."
    is ApiError.Server -> "Server error. Try again later."
    is ApiError.Unknown -> "Something went wrong."
}
