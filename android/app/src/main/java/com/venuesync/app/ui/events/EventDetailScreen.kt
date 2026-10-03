package com.venuesync.app.ui.events

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.core.model.Availability
import com.venuesync.app.core.model.EventDetail
import com.venuesync.app.core.model.TicketType
import com.venuesync.app.core.model.availabilityOf
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.theme.Mono
import com.venuesync.app.ui.theme.StubCard
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    onBack: () -> Unit,
    onSignInRequired: () -> Unit,
    onPurchased: (ticketId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val purchase by viewModel.purchase.collectAsStateWithLifecycle()

    // One-shot states: act, then tell the VM so it leaves the state. Recomposition or rotation can't
    // fire them twice, because by then the VM has already moved on.
    LaunchedEffect(purchase) {
        when (val p = purchase) {
            PurchaseState.SignInRequired -> {
                onSignInRequired()
                viewModel.onSignInHandled()
            }
            is PurchaseState.Purchased -> {
                onPurchased(p.ticket.id)
                viewModel.onPurchaseShown()
            }
            else -> Unit
        }
    }
    PurchaseDialog(
        state = purchase,
        onConfirm = viewModel::confirmPurchase,
        onRetry = viewModel::retryPurchase,
        onDismiss = viewModel::dismissPurchase,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = state) {
                UiState.Loading -> Centered { CircularProgressIndicator() }
                // A single event has no "empty" case — a missing one is Error(NotFound).
                UiState.Empty -> Centered { Text("Not found.") }
                is UiState.Error -> Centered {
                    Text(s.error.message(), style = MaterialTheme.typography.bodyLarge)
                    Button(shape = MaterialTheme.shapes.small, onClick = viewModel::retry, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
                }
                is UiState.Success -> EventDetailContent(s.data, onBuy = viewModel::buy)
            }
        }
    }
}

@Composable
private fun EventDetailContent(event: EventDetail, onBuy: (TicketType) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    event.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.semantics { heading() },
                )
                event.venue?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                event.start?.let { start ->
                    val range = event.end?.takeIf { it.isAfter(start) }
                        ?.let { "${start.format(DateFormat)} – ${it.format(DateFormat)}" }
                        ?: start.format(DateFormat)
                    Text(range, style = MaterialTheme.typography.bodyMedium, fontFamily = Mono, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Text("Tickets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        }
        if (event.ticketTypes.isEmpty()) {
            item { Text("Tickets aren't on sale yet.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(event.ticketTypes, key = TicketType::id) { TicketTypeRow(it, event.availabilityOf(it), onBuy = { onBuy(it) }) }
        }
    }
}

@Composable
private fun TicketTypeRow(ticketType: TicketType, availability: Availability, onBuy: () -> Unit) {
    StubCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    ticketType.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                Text(
                    priceFormat().format(ticketType.price),
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = Mono,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            ticketType.description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (availability == Availability.Buyable) {
                Button(shape = MaterialTheme.shapes.small, onClick = onBuy, modifier = Modifier.padding(top = 8.dp)) { Text("Get ticket") }
            } else {
                availabilityLabel(availability)?.let {
                    Text(
                        it.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = Mono,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

private fun availabilityLabel(availability: Availability): String? = when (availability) {
    Availability.Buyable -> null
    Availability.SoldOut -> "Sold out"
    is Availability.OnSaleFrom -> availability.start?.let { "On sale ${it.format(DateFormat)}" } ?: "Not on sale yet"
    Availability.SalesEnded -> "Sales ended"
}

/**
 * One AlertDialog whose content changes: separate dialogs per state would close and reopen the
 * window on Confirming → Purchasing, a visible flicker.
 */
@Composable
private fun PurchaseDialog(
    state: PurchaseState,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val busy = state is PurchaseState.Purchasing
    val (title, body) = when (state) {
        is PurchaseState.Confirming -> "Get ticket" to "Get 1 × ${state.type.name} for ${priceLabel(state.type.price)}?"
        is PurchaseState.Purchasing -> "Getting your ticket…" to "This can take up to a minute."
        is PurchaseState.Retryable -> "Couldn't finish" to state.message
        is PurchaseState.Failed -> "Couldn't get a ticket" to state.message
        PurchaseState.Idle, PurchaseState.SignInRequired, is PurchaseState.Purchased -> return
    }
    AlertDialog(
        // The guard that matters: while the request is in flight nothing may close the dialog.
        onDismissRequest = { if (!busy) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy),
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(body)
                if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            when (state) {
                is PurchaseState.Confirming -> TextButton(onClick = onConfirm) { Text("Confirm") }
                is PurchaseState.Retryable -> TextButton(onClick = onRetry) { Text("Try again") }
                is PurchaseState.Failed -> TextButton(onClick = onDismiss) { Text("OK") }
                else -> Unit
            }
        },
        dismissButton = {
            if (state is PurchaseState.Confirming || state is PurchaseState.Retryable) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

/** "$25.00", or "free" for zero: reads as "Get 1 × GA for free?". */
internal fun priceLabel(price: BigDecimal): String =
    if (price.signum() == 0) "free" else priceFormat().format(price)

// ponytail: the API contract has no currency field and the web app shows "$", so USD is assumed.
// Add `currency` to the DTO once the backend sends it. NumberFormat isn't thread-safe, so one per call.
private fun priceFormat(): NumberFormat =
    NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("USD") }
