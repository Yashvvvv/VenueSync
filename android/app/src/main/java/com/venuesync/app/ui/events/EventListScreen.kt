package com.venuesync.app.ui.events

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.R
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.Event
import com.venuesync.app.core.model.Refusal
import com.venuesync.app.ui.account.AccountAction
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.components.ErrorState
import com.venuesync.app.ui.components.EventStubCard
import com.venuesync.app.ui.components.EventStubSkeleton
import com.venuesync.app.ui.components.SkeletonList
import com.venuesync.app.ui.components.StubEmptyState
import com.venuesync.app.ui.components.eventsEmptyCopy
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.Lockup
import com.venuesync.app.ui.theme.Mono
import com.venuesync.app.ui.theme.Perforation
import com.venuesync.app.ui.theme.StubCard
import com.venuesync.app.ui.theme.TicketShape
import com.venuesync.app.ui.theme.enter
import com.venuesync.app.ui.theme.rememberEntrance
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Classic's home (the web's attendee landing, minus the marketing): search, categories, the photo stub list. */
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
                title = { Lockup() },
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

/** The web's category row. No category field exists in the API, so each one is a search term, as on the web. */
private val Categories = listOf(
    "Music" to R.drawable.ph_music_notes_fill,
    "Sports" to R.drawable.ph_barbell_fill,
    "Arts" to R.drawable.ph_paint_brush_fill,
    "Tech" to R.drawable.ph_terminal_fill,
    "Food" to R.drawable.ph_fork_knife_fill,
    "Comedy" to R.drawable.ph_confetti_fill,
)

/** Search + categories + paged list of published events. */
@Composable
internal fun EventBrowser(
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        SearchField(query, viewModel::onQueryChanged, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        CategoryChips(query, viewModel::onQueryChanged)
        when (val s = state) {
            UiState.Loading -> SkeletonList(3, "Loading events") { EventStubSkeleton() }
            UiState.Empty -> {
                val copy = eventsEmptyCopy(query)
                StubEmptyState(
                    icon = if (query.isBlank()) R.drawable.ph_calendar_dots else R.drawable.ph_magnifying_glass,
                    title = copy.title,
                    body = copy.body,
                    actionLabel = copy.action,
                    onAction = { viewModel.onQueryChanged("") },
                )
            }
            is UiState.Error -> ErrorState(s.error, viewModel::retry)
            is UiState.Success -> EventList(
                s.data,
                header = if (query.isBlank()) "On sale now" else "Search results",
                onEventClick = onEventClick,
                onEndReached = viewModel::loadMore,
            )
        }
    }
}

/**
 * One bordered field. The magnifier and the border take the accent on focus (no glow); the clear button scales in.
 */
@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val focus = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Search events, venues or artists") },
        leadingIcon = { Icon(painterResource(R.drawable.ph_magnifying_glass), contentDescription = null, Modifier.size(20.dp)) },
        trailingIcon = {
            AnimatedVisibility(query.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(painterResource(R.drawable.ph_x), contentDescription = "Clear search", Modifier.size(18.dp))
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.outline,
            focusedLeadingIconColor = colors.primary,
            unfocusedLeadingIconColor = colors.onSurfaceVariant,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Active chip = accent fill with ink; tapping it again clears the search. */
@Composable
private fun CategoryChips(query: String, onChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val border = LocalExperience.current.border
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Categories.forEach { (name, icon) ->
            val active = query.trim().equals(name, ignoreCase = true)
            FilterChip(
                selected = active,
                onClick = { onChange(if (active) "" else name) },
                label = { Text(name) },
                leadingIcon = { Icon(painterResource(icon), contentDescription = null, Modifier.size(16.dp)) },
                shape = MaterialTheme.shapes.small,
                colors = FilterChipDefaults.filterChipColors(
                    labelColor = colors.onSurface,
                    iconColor = colors.onSurfaceVariant,
                    selectedContainerColor = colors.primary,
                    selectedLabelColor = colors.onPrimary,
                    selectedLeadingIconColor = colors.onPrimary,
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
private fun EventList(events: List<Event>, header: String, onEventClick: (String) -> Unit, onEndReached: () -> Unit) {
    val listState = rememberLazyListState()
    val entrance = rememberEntrance()
    LoadMoreOnEnd(listState, events.size + 1, onEndReached) // + the header item
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header") {
            Text(header, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        }
        itemsIndexed(events, key = { _, event -> event.id }) { index, event ->
            EventStubCard(event, onClick = { onEventClick(event.id) }, modifier = Modifier.enter(entrance, index))
        }
    }
}

@Composable
internal fun EventCard(event: Event, onClick: () -> Unit) {
    StubRow(start = event.start, onClick = onClick) {
        Text(event.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        event.venue?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A ticket stub: the counterfoil holds the date, a perforation, then [body]. [spent] (a used or past ticket) drops
 * the ember, like the web's faded stub.
 */
@Composable
internal fun StubRow(
    start: LocalDateTime?,
    onClick: () -> Unit,
    spent: Boolean = false,
    body: @Composable ColumnScope.() -> Unit,
) {
    StubCard(shape = TicketShape(Counterfoil, vertical = true, LocalExperience.current.radius), onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            DateStub(start, spent, Modifier.width(Counterfoil).padding(vertical = 16.dp))
            Perforation(vertical = true, modifier = Modifier.fillMaxHeight().width(1.dp))
            Column(
                modifier = Modifier.weight(1f).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
                content = body,
            )
        }
    }
}

/** OCT / 12 / 7:30 PM in mono, the way a printed stub carries its date. */
@Composable
private fun DateStub(start: LocalDateTime?, spent: Boolean, modifier: Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (start == null) {
            Text("TBA", style = MaterialTheme.typography.labelMedium, fontFamily = Mono, color = muted)
        } else {
            Text(start.format(MonthFormat).uppercase(), style = MaterialTheme.typography.labelSmall, fontFamily = Mono, color = muted)
            Text(
                start.dayOfMonth.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = Mono,
                color = if (spent) muted else MaterialTheme.colorScheme.primary,
            )
            Text(start.format(TimeFormat), style = MaterialTheme.typography.labelSmall, fontFamily = Mono, color = muted)
        }
    }
}

private val Counterfoil = 80.dp
private val MonthFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
private val TimeFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

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
    ApiError.Gone -> "This has expired."
    ApiError.SoldOut -> "Sold out."
    ApiError.NotOnSale -> "Tickets aren't on sale right now."
    ApiError.RateLimited -> "Too many requests. Wait a moment and try again."
    // Forms mark the field themselves; this is the line for anywhere without one.
    is ApiError.Invalid -> "Some details aren't right. Check the highlighted field."
    is ApiError.Refused -> when (reason) {
        Refusal.TicketTypeHasSales -> "This ticket type has tickets sold, so it can't be removed. Lower its capacity instead."
        Refusal.EventHasSales -> "This event has tickets sold, so it can't be deleted. Cancel it instead."
        Refusal.CapacityBelowSold -> "Capacity can't be lower than the tickets already sold."
        Refusal.StatusChange -> "This event can't change to that status."
    }
    ApiError.InvalidResponse -> "We couldn't load this. Try again later."
    is ApiError.Server -> "Server error. Try again later."
    is ApiError.Unknown -> "Something went wrong."
}
