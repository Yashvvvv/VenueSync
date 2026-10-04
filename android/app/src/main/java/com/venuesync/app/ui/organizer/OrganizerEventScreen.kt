package com.venuesync.app.ui.organizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.R
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.OrganizerEvent
import com.venuesync.app.core.model.OrganizerTicketType
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.components.Clock
import com.venuesync.app.ui.components.DisplayText
import com.venuesync.app.ui.components.ErrorState
import com.venuesync.app.ui.components.EventStatusChip
import com.venuesync.app.ui.components.EyebrowText
import com.venuesync.app.ui.components.MetaText
import com.venuesync.app.ui.components.SkeletonList
import com.venuesync.app.ui.components.StubButton
import com.venuesync.app.ui.components.TicketStubSkeleton
import com.venuesync.app.ui.components.TicketWhen
import com.venuesync.app.ui.components.money
import com.venuesync.app.ui.events.message
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.Num
import com.venuesync.app.ui.theme.StubCard
import com.venuesync.app.ui.theme.Total
import com.venuesync.app.ui.theme.displaySection
import java.math.BigDecimal
import java.time.LocalDateTime

/** An organizer's event: status, schedule, sales per ticket type, and what can be done to it from here. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerEventScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrganizerEventViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val action by viewModel.action.collectAsStateWithLifecycle()

    // Back from the form: show what was saved.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    // Deleted: there's nothing left to look at.
    LaunchedEffect(action) { if (action == ActionState.Deleted) onBack() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ph_arrow_left), contentDescription = "Back to my events")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                UiState.Loading, UiState.Empty -> SkeletonList(3, "Loading the event") { TicketStubSkeleton() }
                is UiState.Error -> ErrorState(s.error, viewModel::retry)
                is UiState.Success -> EventOverview(
                    event = s.data,
                    action = action,
                    onEdit = { onEdit(s.data.id) },
                    onAction = viewModel::perform,
                    onDismissError = viewModel::dismissError,
                )
            }
        }
    }
}

@Composable
private fun EventOverview(
    event: OrganizerEvent,
    action: ActionState,
    onEdit: () -> Unit,
    onAction: (EventAction) -> Unit,
    onDismissError: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val style = LocalExperience.current
    // The action waiting for a yes; saved, so a rotation doesn't drop the question.
    var confirming by rememberSaveable { mutableStateOf<EventAction?>(null) }
    val busy = action is ActionState.Working

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        EventStatusChip(event.status)
        DisplayText(event.name, style.displaySection(), Modifier.padding(top = 8.dp).semantics { heading() })
        Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            HorizontalDivider(color = colors.outlineVariant, thickness = style.border)
            InfoRow(R.drawable.ph_calendar_blank_fill, "When", whenLine(event.start, event.end) ?: "No date yet")
            InfoRow(R.drawable.ph_map_pin_fill, "Where", event.venue.ifBlank { "No venue yet" })
            InfoRow(R.drawable.ph_ticket_fill, "Sales", salesWindowLine(event.salesStart, event.salesEnd))
            HorizontalDivider(color = colors.outlineVariant, thickness = style.border)
        }

        EyebrowText("Sales", Modifier.padding(top = 28.dp).semantics { heading() })
        Row(Modifier.padding(top = 12.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(salesLine(event), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(money(event.grossAtCurrentPrices), style = Total)
        }
        Text(
            "Gross at today's prices. Each ticket keeps the price it was bought at.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            event.ticketTypes.forEach { TicketTypeSales(it) }
        }

        (action as? ActionState.Failed)?.let { ActionNotice(it.error, onDismissError) }

        Column(Modifier.padding(top = 28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val actions = event.actions()
            if (EventAction.Publish in actions) {
                if (event.start == null) {
                    // The server would take it, but an event with no date can't be planned around or sold honestly.
                    Text(
                        "Add the date and time before publishing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
                StubButton(
                    if (action == ActionState.Working(EventAction.Publish)) "Publishing" else "Publish",
                    { confirming = EventAction.Publish },
                    FullWidth,
                    enabled = !busy && event.start != null,
                )
            }
            if (event.editable()) StubButton("Edit", onEdit, FullWidth, enabled = !busy, outlined = true, icon = R.drawable.ph_pencil_simple)
            if (EventAction.Unpublish in actions) {
                StubButton("Take off sale", { confirming = EventAction.Unpublish }, FullWidth, enabled = !busy, outlined = true)
            }
            if (EventAction.Cancel in actions) {
                StubButton(
                    if (action == ActionState.Working(EventAction.Cancel)) "Cancelling" else "Cancel event",
                    { confirming = EventAction.Cancel },
                    FullWidth,
                    enabled = !busy,
                    outlined = true,
                )
            }
            if (EventAction.Delete in actions) {
                StubButton("Delete draft", { confirming = EventAction.Delete }, FullWidth, enabled = !busy, outlined = true, icon = R.drawable.ph_trash)
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(32.dp))
    }

    confirming?.let { pending ->
        val copy = confirmCopy(pending, event)
        AlertDialog(
            onDismissRequest = { confirming = null },
            title = { Text(copy.title) },
            text = { Text(copy.body) },
            confirmButton = {
                TextButton(onClick = {
                    confirming = null
                    onAction(pending)
                }) { Text(copy.confirm) }
            },
            dismissButton = { TextButton(onClick = { confirming = null }) { Text(copy.keep) } },
        )
    }
}

private val FullWidth = Modifier.fillMaxWidth().height(48.dp)

@Composable
private fun InfoRow(icon: Int, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(painterResource(icon), null, Modifier.padding(top = 2.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.padding(start = 12.dp)) {
            MetaText(label)
            Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** One ticket type's sales: sold of capacity with a bar (none for unlimited), and what it took at today's price. */
@Composable
private fun TicketTypeSales(type: OrganizerTicketType) {
    val colors = MaterialTheme.colorScheme
    StubCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    type.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    modifier = Modifier.weight(1f),
                )
                Text(money(type.price), style = Num.copy(fontSize = 14.sp), color = colors.onSurfaceVariant)
            }
            type.capacity?.takeIf { it > 0 }?.let { capacity ->
                LinearProgressIndicator(
                    progress = { (type.sold.toFloat() / capacity).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    drawStopIndicator = {},
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    type.capacity?.let { "${type.sold} of $it sold" } ?: "${type.sold} sold, no limit",
                    style = Num.copy(fontSize = 13.sp),
                    modifier = Modifier.weight(1f),
                )
                Text(money(type.price * BigDecimal.valueOf(type.sold)), style = Num.copy(fontSize = 13.sp), color = colors.primary)
            }
        }
    }
}

/** The reason an action was refused or failed, with a way to put it away. */
@Composable
private fun ActionNotice(error: ApiError, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Row(
        Modifier.padding(top = 20.dp).fillMaxWidth()
            .background(colors.error.copy(alpha = 0.05f), shape)
            .border(LocalExperience.current.border, colors.error.copy(alpha = 0.4f), shape)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(actionErrorMessage(error), style = MaterialTheme.typography.bodyMedium, color = colors.error, modifier = Modifier.weight(1f))
        IconButton(onClick = onDismiss) {
            Icon(painterResource(R.drawable.ph_x_bold), contentDescription = "Dismiss", tint = colors.onSurfaceVariant)
        }
    }
}

/** A status change resends the event: if the server refuses its details, the fix is in the form. */
internal fun actionErrorMessage(error: ApiError): String =
    if (error is ApiError.Invalid) "Some of this event's details need fixing first. Tap Edit." else error.message()

internal data class ConfirmCopy(val title: String, val body: String, val confirm: String, val keep: String = "Not now")

/** Every destructive or public step says what happens, including to tickets already issued. */
internal fun confirmCopy(action: EventAction, event: OrganizerEvent): ConfirmCopy = when (action) {
    EventAction.Publish -> ConfirmCopy(
        "Publish this event?",
        "It goes on sale in the app and on the website, and anyone can find it.",
        "Publish",
    )
    EventAction.Unpublish -> ConfirmCopy(
        "Take it off sale?",
        "It leaves the catalogue and goes back to your drafts. Nobody has a ticket yet.",
        "Take off sale",
    )
    EventAction.Cancel -> ConfirmCopy(
        "Cancel this event?",
        "It leaves the catalogue and can't be sold or published again. " +
            (if (event.sold > 0) "The ${event.sold} tickets already issued stay on their holders' accounts. " else "") +
            "This can't be undone.",
        "Cancel event",
        keep = "Keep it",
    )
    EventAction.Delete -> ConfirmCopy(
        "Delete this draft?",
        "It's removed for good. Nobody else has seen it.",
        "Delete",
    )
}

/** "Fri 14 Mar 2026 · 19:00 to 23:00", across days in full; null without a start. */
internal fun whenLine(start: LocalDateTime?, end: LocalDateTime?): String? {
    start ?: return null
    val until = end?.takeIf { it.isAfter(start) } ?: return start.format(TicketWhen)
    return if (until.toLocalDate() == start.toLocalDate()) {
        "${start.format(TicketWhen)} to ${until.format(Clock)}"
    } else {
        "${start.format(TicketWhen)} to ${until.format(TicketWhen)}"
    }
}

/** The server's rule (Event.salesStatusAt): no start = on sale once published, no end = until the event ends. */
internal fun salesWindowLine(salesStart: LocalDateTime?, salesEnd: LocalDateTime?): String {
    val from = salesStart?.let { "From ${it.format(TicketWhen)}" } ?: "From publishing"
    val until = salesEnd?.let { "until ${it.format(TicketWhen)}" } ?: "until the event ends"
    return "$from $until"
}
