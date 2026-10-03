package com.venuesync.app.ui.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.Event
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.events.Centered
import com.venuesync.app.ui.events.EventCard
import com.venuesync.app.ui.events.message
import com.venuesync.app.ui.theme.Mono

/**
 * Staff pick the event whose door they're working; every scan is then checked against it. Only events they can
 * actually scan are listed (organized or staffed), and an organizer's invite code adds one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanEventPickerScreen(
    onBack: () -> Unit,
    onEventClick: (String) -> Unit,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StaffEventsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val join by viewModel.join.collectAsStateWithLifecycle()
    var showCodeDialog by rememberSaveable { mutableStateOf(false) }

    // Joined is a one-shot: open that event's scanner straight away, then let the VM forget it.
    LaunchedEffect(join) {
        (join as? JoinState.Joined)?.let {
            showCodeDialog = false
            onEventClick(it.event.eventId)
            viewModel.onJoinedShown()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Scan tickets") },
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
                UiState.Empty -> Centered {
                    Text(
                        "You're not on the door team for any event yet.\nAsk the organizer for an invite code.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Button(shape = MaterialTheme.shapes.small, onClick = { showCodeDialog = true }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Enter invite code")
                    }
                }
                is UiState.Error -> Centered {
                    Text(s.error.message(), style = MaterialTheme.typography.bodyLarge)
                    if (s.error == ApiError.Unauthorized) {
                        Button(shape = MaterialTheme.shapes.small, onClick = onSignInClick, modifier = Modifier.padding(top = 12.dp)) { Text("Sign in") }
                    } else {
                        Button(shape = MaterialTheme.shapes.small, onClick = viewModel::retry, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
                    }
                }
                is UiState.Success -> EventsToScan(s.data, onEventClick, onEnterCode = { showCodeDialog = true })
            }
        }
    }

    if (showCodeDialog) {
        InviteCodeDialog(
            join = join,
            onJoin = viewModel::join,
            onDismiss = {
                showCodeDialog = false
                viewModel.dismissJoinError()
            },
            onEdit = viewModel::dismissJoinError,
        )
    }
}

@Composable
private fun EventsToScan(events: List<Event>, onEventClick: (String) -> Unit, onEnterCode: () -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                "Pick the event whose door you're working.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(events, key = Event::id) { event -> EventCard(event, onClick = { onEventClick(event.id) }) }
        item {
            OutlinedButton(shape = MaterialTheme.shapes.small, onClick = onEnterCode, modifier = Modifier.fillMaxWidth()) { Text("Enter invite code") }
        }
    }
}

@Composable
private fun InviteCodeDialog(join: JoinState, onJoin: (String) -> Unit, onDismiss: () -> Unit, onEdit: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val joining = join == JoinState.Joining
    AlertDialog(
        onDismissRequest = { if (!joining) onDismiss() },
        title = { Text("Join a door team") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Type the code the organizer sent you.")
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it.take(16)
                        onEdit() // typing again clears the last error
                    },
                    placeholder = { Text("K7Q2M-9XH4P", fontFamily = Mono) },
                    singleLine = true,
                    enabled = !joining,
                    textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = Mono),
                    isError = join is JoinState.Failed,
                    supportingText = (join as? JoinState.Failed)?.let { failed -> { Text(failed.message) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onJoin(code) }),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onJoin(code) }, enabled = code.isNotBlank() && !joining) {
                Text(if (joining) "Joining…" else "Join")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !joining) { Text("Cancel") } },
    )
}
