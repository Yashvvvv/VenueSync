package com.venuesync.app.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.venuesync.app.ui.theme.Mono
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.venuesync.app.core.model.Guest
import com.venuesync.app.core.model.normalizeCheckInEntry
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.events.message
import com.venuesync.app.ui.tickets.statusLabel
import com.venuesync.app.core.model.ScanResult
import com.venuesync.app.core.model.ScanStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScannerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val eventName by viewModel.eventName.collectAsStateWithLifecycle()
    val guests by viewModel.guests.collectAsStateWithLifecycle()
    var showCodeEntry by rememberSaveable { mutableStateOf(false) }
    var showGuestList by rememberSaveable { mutableStateOf(false) }

    // Google's code scanner: Play services shows the camera, so the app holds no camera permission.
    val context = LocalContext.current
    val scanner = remember(context) {
        GmsBarcodeScanning.getClient(
            context,
            GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build(),
        )
    }
    var scannerProblem by rememberSaveable { mutableStateOf<String?>(null) }
    val scan: () -> Unit = {
        scannerProblem = null
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                barcode.rawValue?.let(viewModel::onScanned) ?: run { scannerProblem = "That code has no text in it." }
            }
            // Cancelled (back pressed in the scanner): nothing to do, the screen stays as it was.
            .addOnFailureListener {
                scannerProblem = "Couldn't open the scanner. Update Google Play services, then try again."
            }
    }

    KeepScreenOn()
    ResultHaptics(state)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(eventName ?: "Scan tickets", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = state) {
                ScanState.Ready -> Prompt(
                    text = "Scan the QR code on the attendee's ticket.",
                    problem = scannerProblem,
                    primary = "Scan ticket" to scan,
                    // When the QR won't scan (cracked screen, glare, no data): type the code, or find them by name.
                    extra = listOf(
                        "Type ticket code" to { showCodeEntry = true },
                        "Guest list" to { showGuestList = true },
                    ),
                )
                ScanState.Checking -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text("Checking…", modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.titleMedium)
                }
                is ScanState.Done -> ResultPanel(s.result, manual = s.manual, onNext = {
                    viewModel.next()
                    scan()
                })
                is ScanState.Retryable -> Prompt(
                    text = s.message,
                    problem = null,
                    primary = "Try again" to viewModel::retry,
                    secondary = "Skip" to viewModel::next,
                )
                is ScanState.Error -> Prompt(text = s.message, problem = null, primary = "Back" to onBack)
            }
        }
    }

    if (showCodeEntry) {
        TicketCodeDialog(
            onCheckIn = {
                showCodeEntry = false
                viewModel.onCodeEntered(it)
            },
            onDismiss = { showCodeEntry = false },
        )
    }
    if (showGuestList) {
        GuestListSheet(
            search = guests,
            onQueryChange = viewModel::searchGuests,
            onCheckIn = {
                showGuestList = false
                viewModel.checkIn(it)
            },
            onDismiss = { showGuestList = false },
        )
    }
}

@Composable
private fun TicketCodeDialog(onCheckIn: (String) -> Unit, onDismiss: () -> Unit) {
    var entry by rememberSaveable { mutableStateOf("") }
    var showFormatHint by rememberSaveable { mutableStateOf(false) }
    val submit = {
        if (normalizeCheckInEntry(entry) != null) onCheckIn(entry) else showFormatHint = true
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Type ticket code") },
        text = {
            OutlinedTextField(
                value = entry,
                onValueChange = {
                    entry = it.take(40)
                    showFormatHint = false
                },
                placeholder = { Text("F5A3-038B", fontFamily = Mono) },
                singleLine = true,
                isError = showFormatHint,
                supportingText = {
                    Text(if (showFormatHint) "Ticket codes look like F5A3-038B." else "Under the QR code on their ticket.")
                },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = Mono),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = submit, enabled = entry.isNotBlank()) { Text("Check in") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Find a guest by name, email or ticket code. Check-in is an explicit button, so a stray tap admits nobody. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuestListSheet(
    search: GuestSearch,
    onQueryChange: (String) -> Unit,
    onCheckIn: (Guest) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            Text("Guest list", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = search.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Name, email or ticket code") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
            when (val results = search.results) {
                UiState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                UiState.Empty -> Text(
                    if (search.query.trim().length < 2) "Type at least 2 characters." else "No guest matches.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                is UiState.Error -> Text(results.error.message(), color = MaterialTheme.colorScheme.error)
                is UiState.Success -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(results.data, key = Guest::ticketId) { guest -> GuestRow(guest, onCheckIn = { onCheckIn(guest) }) }
                }
            }
        }
    }
}

@Composable
private fun GuestRow(guest: Guest, onCheckIn: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(guest.name ?: "No name", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(guest.ticketTypeName, guest.ticketCode).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = Mono,
                )
                guest.email?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                statusLabel(guest.status)?.let { Text(it, style = MaterialTheme.typography.labelLarge) }
            }
            // A used/expired/cancelled ticket can still be "checked in": the server answers, and the door sees why not.
            Button(onClick = onCheckIn) { Text("Check in") }
        }
    }
}

@Composable
private fun Prompt(
    text: String,
    problem: String?,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>? = null,
    extra: List<Pair<String, () -> Unit>> = emptyList(),
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        problem?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
        Button(onClick = primary.second, modifier = Modifier.fillMaxWidth().height(64.dp)) {
            Text(primary.first, style = MaterialTheme.typography.titleMedium)
        }
        secondary?.let { (label, action) ->
            OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(label) }
        }
        extra.forEach { (label, action) ->
            OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(label) }
        }
    }
}

/** The whole screen is the answer, readable at arm's length. Colour is backed by words for colour-blind staff. */
@Composable
private fun ResultPanel(result: ScanResult, manual: Boolean, onNext: () -> Unit) {
    val (color, title, detail) = when (result.status) {
        ScanStatus.Valid -> Triple(Go, "Let in", result.ticketTypeName?.let { "1 × $it" } ?: "Valid ticket")
        ScanStatus.AlreadyUsed -> Triple(Stop, "Already used", "This ticket was scanned before. Don't let in.")
        ScanStatus.Expired -> Triple(Stop, "Expired", "This ticket's event is over.")
        // A typed code is only looked up within this event (short codes are guessable, so the server won't search
        // other organizers' events). So "invalid" here usually means a typo or the wrong event, not a fake ticket.
        ScanStatus.Invalid -> Triple(
            Stop,
            "Not a valid ticket",
            if (manual) "No ticket with this code for this event, or it was cancelled. Check the code and the event."
            else "Not a VenueSync ticket, or it was cancelled.",
        )
        ScanStatus.WrongEvent -> Triple(Caution, "Wrong event", result.eventName?.let { "This ticket is for $it." } ?: "This ticket is for another event.")
        ScanStatus.Unknown -> Triple(Neutral, "Couldn't verify", "Check the ticket by hand.")
    }
    Column(
        modifier = Modifier.fillMaxSize().background(color).padding(24.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive }, // TalkBack announces the answer
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.displayMedium, color = Color.White, textAlign = TextAlign.Center)
        Text(detail, style = MaterialTheme.typography.titleLarge, color = Color.White, textAlign = TextAlign.Center)
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = color),
            modifier = Modifier.padding(top = 24.dp).fillMaxWidth().height(64.dp),
        ) { Text("Scan next", style = MaterialTheme.typography.titleMedium) }
    }
}

/** One short tick for "let in", a long buzz for anything else: staff feel the answer without looking. */
@Composable
private fun ResultHaptics(state: ScanState) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state) {
        val status = (state as? ScanState.Done)?.result?.status ?: return@LaunchedEffect
        haptics.performHapticFeedback(
            if (status == ScanStatus.Valid) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress,
        )
    }
}

/** A door shift is long; the screen must not lock between attendees. */
@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

// ponytail: fixed door colours (white text passes contrast on all four); move into the theme in the step 6 pass.
private val Go = Color(0xFF1B7F3B)
private val Stop = Color(0xFFB3261E)
private val Caution = Color(0xFF8A5A00)
private val Neutral = Color(0xFF5F6368)
