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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.core.model.Availability
import com.venuesync.app.core.model.EventDetail
import com.venuesync.app.core.model.TicketType
import com.venuesync.app.core.model.availabilityOf
import com.venuesync.app.ui.common.UiState
import java.text.NumberFormat
import java.util.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                    Button(onClick = viewModel::retry, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
                }
                is UiState.Success -> EventDetailContent(s.data)
            }
        }
    }
}

@Composable
private fun EventDetailContent(event: EventDetail) {
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
                event.venue?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                event.start?.let { start ->
                    val range = event.end?.takeIf { it.isAfter(start) }
                        ?.let { "${start.format(DateFormat)} – ${it.format(DateFormat)}" }
                        ?: start.format(DateFormat)
                    Text(range, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Text("Tickets", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        }
        if (event.ticketTypes.isEmpty()) {
            item { Text("Tickets aren't on sale yet.", style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(event.ticketTypes, key = TicketType::id) { TicketTypeRow(it, event.availabilityOf(it)) }
        }
    }
}

@Composable
private fun TicketTypeRow(ticketType: TicketType, availability: Availability) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    ticketType.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                )
                Text(priceFormat().format(ticketType.price), style = MaterialTheme.typography.titleSmall)
            }
            ticketType.description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
            }
            availabilityLabel(availability)?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
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

// ponytail: the API contract has no currency field and the web app shows "$", so USD is assumed.
// Add `currency` to the DTO once the backend sends it. NumberFormat isn't thread-safe, so one per call.
private fun priceFormat(): NumberFormat =
    NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("USD") }
