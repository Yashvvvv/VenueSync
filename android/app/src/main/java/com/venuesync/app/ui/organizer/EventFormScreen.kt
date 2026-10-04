package com.venuesync.app.ui.organizer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.R
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.ui.components.DisplayText
import com.venuesync.app.ui.components.ErrorState
import com.venuesync.app.ui.components.EyebrowText
import com.venuesync.app.ui.components.MetaText
import com.venuesync.app.ui.components.StubButton
import com.venuesync.app.ui.components.TicketWhen
import com.venuesync.app.ui.events.message
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.StubCard
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Create or edit an event: basics, when, the sales window, ticket types. Saving a new event makes a draft; publishing
 * happens on the event's overview, after the organizer has seen it laid out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventFormScreen(
    onClose: () -> Unit,
    onSaved: (eventId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventFormViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var askLeave by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(ui.savedId) { ui.savedId?.let(onSaved) }

    // Typed work is never dropped by a stray back: changes (or a save whose outcome is unknown) ask first.
    val guard = ui.savedId == null && !ui.saving && (ui.dirty || ui.unconfirmed)
    val leave = { if (guard) askLeave = true else onClose() }
    BackHandler(enabled = guard || ui.saving) { if (!ui.saving) askLeave = true }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { DisplayText(if (viewModel.creating) "New event" else "Edit event", MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = leave, enabled = !ui.saving) {
                        Icon(painterResource(R.drawable.ph_x), contentDescription = "Close")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val form = ui.form
            when {
                form != null -> FormContent(form, ui, viewModel.creating, viewModel::edit, viewModel::save)
                ui.loadError != null -> ErrorState(ui.loadError!!, viewModel::retryLoad)
                else -> LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }

    if (askLeave) {
        AlertDialog(
            onDismissRequest = { askLeave = false },
            title = { Text("Leave without saving?") },
            text = {
                Text(
                    if (ui.unconfirmed) {
                        "Your last save wasn't confirmed. If it went through, the event is in My events as a draft."
                    } else {
                        "Your changes to this event will be lost."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    askLeave = false
                    onClose()
                }) { Text("Leave") }
            },
            dismissButton = { TextButton(onClick = { askLeave = false }) { Text("Keep editing") } },
        )
    }
}

@Composable
private fun FormContent(
    form: EventForm,
    ui: FormUi,
    creating: Boolean,
    onEdit: ((EventForm) -> EventForm) -> Unit,
    onSave: () -> Unit,
) {
    val style = LocalExperience.current
    val badField = (ui.error as? ApiError.Invalid)?.field
    val enabled = !ui.locked
    fun error(field: String) = if (badField == field) fieldMessage(field, form) else null

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
        Section("Basics", top = 8.dp)
        FormField("Event name", form.name, { v -> onEdit { it.copy(name = v) } }, error("name"), enabled, capitalize = true)
        FormField("Venue", form.venue, { v -> onEdit { it.copy(venue = v) } }, error("venue"), enabled, capitalize = true)

        Section("When")
        DateTimeField("Starts", form.start.toWallClock(), { v -> onEdit { it.copy(start = v?.toString()) } }, error("start"), enabled)
        DateTimeField(
            "Ends", form.end.toWallClock(), { v -> onEdit { it.copy(end = v?.toString()) } }, error("end"), enabled,
            suggest = form.start.toWallClock()?.plusHours(3),
        )

        Section("Sales window")
        Text(
            "Leave these empty to sell from the moment you publish until the event ends.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        DateTimeField("Sales open", form.salesStart.toWallClock(), { v -> onEdit { it.copy(salesStart = v?.toString()) } }, error("salesStart"), enabled)
        DateTimeField(
            "Sales close", form.salesEnd.toWallClock(), { v -> onEdit { it.copy(salesEnd = v?.toString()) } }, error("salesEnd"), enabled,
            suggest = form.start.toWallClock(),
        )

        Section("Tickets")
        error("ticketTypes")?.let { FieldError(it) }
        Column(verticalArrangement = Arrangement.spacedBy(if (style.hardShadow) 20.dp else 12.dp)) {
            form.ticketTypes.forEachIndexed { i, type ->
                TicketTypeEditor(
                    index = i,
                    type = type,
                    form = form,
                    badField = badField,
                    enabled = enabled,
                    onChange = { changed -> onEdit { f -> f.copy(ticketTypes = f.ticketTypes.map { if (it.key == type.key) changed else it }) } },
                    onRemove = { onEdit { f -> f.copy(ticketTypes = f.ticketTypes.filterNot { it.key == type.key }) } },
                )
            }
        }
        StubButton(
            "Add ticket type",
            { onEdit { it.copy(ticketTypes = it.ticketTypes + TicketTypeForm()) } },
            Modifier.padding(top = 12.dp).fillMaxWidth(),
            enabled = enabled,
            outlined = true,
            icon = R.drawable.ph_plus_bold,
        )

        // The save button sits at the bottom, often far from the field: the reason is repeated here.
        when {
            ui.unconfirmed -> Notice(
                "Your last save wasn't confirmed. Saving again finishes that attempt, so you won't get two events.",
                isError = false,
            )
            ui.error is ApiError.Invalid -> Notice(fieldMessage(badField, form), isError = true)
            ui.error != null -> Notice(ui.error.message(), isError = true)
        }
        StubButton(
            when {
                ui.saving -> "Saving"
                ui.unconfirmed -> "Try again"
                creating -> "Save draft"
                else -> "Save changes"
            },
            onSave,
            Modifier.padding(top = 24.dp).fillMaxWidth().height(52.dp),
            enabled = !ui.saving && ui.savedId == null,
        )
        if (ui.saving) LinearProgressIndicator(Modifier.padding(top = 8.dp).fillMaxWidth())
        if (creating) {
            Text(
                "It stays a draft, visible only to you, until you publish it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Section(title: String, top: androidx.compose.ui.unit.Dp = 28.dp) {
    EyebrowText(title, Modifier.padding(top = top, bottom = 12.dp).semantics { heading() })
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    error: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    capitalize: Boolean = false,
    keyboard: KeyboardType = KeyboardType.Text,
    placeholder: String? = null,
    prefix: String? = null,
    singleLine: Boolean = true,
    hint: String? = null,
    readOnly: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        prefix = prefix?.let { { Text(it) } },
        isError = error != null,
        supportingText = (error ?: hint)?.let { { Text(it) } },
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 4,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(
            capitalization = if (capitalize) KeyboardCapitalization.Words else KeyboardCapitalization.Sentences,
            keyboardType = keyboard,
        ),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = colors.primary, unfocusedBorderColor = colors.outline),
        modifier = modifier.fillMaxWidth().padding(bottom = 4.dp),
    )
}

@Composable
private fun FieldError(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(bottom = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
    )
}

/**
 * A wall-clock date and time (ADR-003): a date picker, then a time picker. The picker's millis are UTC midnight of the
 * chosen day by contract, so they're read as a UTC day, never through the phone's zone. [suggest] opens an empty field
 * somewhere useful (the end near the start).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeField(
    label: String,
    value: LocalDateTime?,
    onChange: (LocalDateTime?) -> Unit,
    error: String?,
    enabled: Boolean,
    suggest: LocalDateTime? = null,
) {
    // 0 closed, 1 choosing the day, 2 choosing the time. Saved, with the day, so a rotation keeps the dialog.
    var step by rememberSaveable { mutableStateOf(0) }
    var day by rememberSaveable { mutableStateOf<Long?>(null) }
    val initial = value ?: suggest

    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.weight(1f)) {
            FormField(
                label,
                value?.format(TicketWhen) ?: "",
                onChange = {},
                error = error,
                enabled = enabled,
                placeholder = "Not set",
                readOnly = true,
            )
            // The field is a display; the whole of it opens the picker.
            Box(
                Modifier.matchParentSize().clickable(enabled = enabled, role = Role.Button, onClickLabel = "Choose ${label.lowercase()}") {
                    step = 1
                },
            )
        }
        if (value != null) {
            IconButton(onClick = { onChange(null) }, enabled = enabled, modifier = Modifier.padding(top = 8.dp)) {
                Icon(painterResource(R.drawable.ph_x), contentDescription = "Clear ${label.lowercase()}", Modifier.size(18.dp))
            }
        }
    }

    if (step == 1) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = initial?.toLocalDate()?.toEpochDay()?.times(DayMillis))
        DatePickerDialog(
            onDismissRequest = { step = 0 },
            confirmButton = {
                TextButton(
                    onClick = {
                        day = picker.selectedDateMillis?.let { Math.floorDiv(it, DayMillis) }
                        step = if (day != null) 2 else 0
                    },
                    enabled = picker.selectedDateMillis != null,
                ) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Cancel") } },
        ) { DatePicker(picker) }
    }
    if (step == 2) {
        val clock = rememberTimePickerState(initialHour = initial?.hour ?: 19, initialMinute = initial?.minute ?: 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { step = 0 },
            title = { Text(label) },
            text = { TimePicker(clock) },
            confirmButton = {
                TextButton(onClick = {
                    day?.let { onChange(LocalDateTime.of(LocalDate.ofEpochDay(it), LocalTime.of(clock.hour, clock.minute))) }
                    step = 0
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { step = 0 }) { Text("Cancel") } },
        )
    }
}

private const val DayMillis = 86_400_000L

@Composable
private fun TicketTypeEditor(
    index: Int,
    type: TicketTypeForm,
    form: EventForm,
    badField: String?,
    enabled: Boolean,
    onChange: (TicketTypeForm) -> Unit,
    onRemove: () -> Unit,
) {
    fun error(name: String) = "ticketTypes[$index].$name".takeIf { it == badField }?.let { fieldMessage(it, form) }
    StubCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MetaText("Ticket type ${index + 1}", Modifier.weight(1f))
                // Removing one with tickets would delete what people bought: the server refuses it, so it isn't offered.
                if (type.sold == 0L) {
                    IconButton(onClick = onRemove, enabled = enabled) {
                        Icon(
                            painterResource(R.drawable.ph_trash),
                            contentDescription = "Remove ticket type ${type.name.ifBlank { (index + 1).toString() }}",
                            Modifier.size(18.dp),
                        )
                    }
                } else {
                    MetaText("${type.sold} issued", Modifier.padding(vertical = 14.dp, horizontal = 8.dp))
                }
            }
            Column(Modifier.padding(end = 8.dp)) {
                FormField("Name", type.name, { onChange(type.copy(name = it)) }, error("name"), enabled, capitalize = true, placeholder = "General admission")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FormField(
                        "Price", type.price, { onChange(type.copy(price = it)) }, error("price"), enabled,
                        Modifier.weight(1f), keyboard = KeyboardType.Decimal, prefix = "$", placeholder = "0",
                    )
                    FormField(
                        "How many", type.capacity, { onChange(type.copy(capacity = it)) }, error("capacity"), enabled,
                        Modifier.weight(1f), keyboard = KeyboardType.Number, placeholder = "No limit",
                        hint = if (type.sold > 0) "At least ${type.sold}" else null,
                    )
                }
                FormField(
                    "Description (optional)", type.description, { onChange(type.copy(description = it)) }, error("description"), enabled,
                    singleLine = false,
                )
            }
        }
    }
}

/** The reason a save stopped, or that the last one is unconfirmed; announced as it appears. */
@Composable
private fun Notice(message: String, isError: Boolean) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) colors.error else colors.onSurface,
        modifier = Modifier.padding(top = 24.dp).fillMaxWidth()
            .background(if (isError) colors.error.copy(alpha = 0.05f) else colors.surfaceContainerHigh, shape)
            .border(LocalExperience.current.border, if (isError) colors.error.copy(alpha = 0.4f) else colors.outlineVariant, shape)
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
    )
}
