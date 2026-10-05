package com.venuesync.app.ui.organizer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.EventDraft
import com.venuesync.app.core.model.EventStatus
import com.venuesync.app.core.model.OrganizerEvent
import com.venuesync.app.core.model.TicketTypeDraft
import com.venuesync.app.core.model.firstInvalidField
import com.venuesync.app.core.model.toApiError
import com.venuesync.app.core.repository.OrganizerRepository
import com.venuesync.app.core.model.ApiException
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * What the form edits, as typed: a half-typed price ("12.") is text, not a number. Dates are ISO wall-clock strings
 * (ADR-003), so the whole form serializes into [SavedStateHandle] as it is.
 */
@Serializable
data class EventForm(
    val name: String = "",
    val venue: String = "",
    val start: String? = null,
    val end: String? = null,
    val salesStart: String? = null,
    val salesEnd: String? = null,
    val status: String = EventStatus.Draft.wire,
    val ticketTypes: List<TicketTypeForm> = listOf(TicketTypeForm()),
    /** The server's version of the event being edited; sent back so a stale save is refused, not applied. */
    val version: Long? = null,
    /** The photo the event has on the server (its imageUrl), if any. */
    val imageUrl: String? = null,
    /** A photo picked on the form, prepared to upload (a JPEG in the app's cache); sent once the event saves. */
    val photoPath: String? = null,
    /** The server's photo is to be removed on save. */
    val photoRemoved: Boolean = false,
)

@Serializable
data class TicketTypeForm(
    /** The list's identity on the phone; never sent. */
    val key: String = UUID.randomUUID().toString(),
    /** Null for a type the server hasn't seen yet. */
    val id: String? = null,
    val name: String = "",
    val price: String = "",
    val description: String = "",
    /** Empty = no limit. */
    val capacity: String = "",
    /** Already issued: the floor for [capacity], and a type with any can't be removed. */
    val sold: Long = 0,
)

data class FormUi(
    /** Null while the event to edit loads. */
    val form: EventForm? = null,
    /** What "unchanged" means: leaving with changes asks first. */
    val original: EventForm? = null,
    val loadError: ApiError? = null,
    val saving: Boolean = false,
    /** The last save's failure; [ApiError.Invalid] names the field. */
    val error: ApiError? = null,
    /** A create whose answer never came. The form is locked until it's retried with the same key. */
    val unconfirmed: Boolean = false,
    /** Saved: the screen moves on to this event. */
    val savedId: String? = null,
    /**
     * The event saved but its photo didn't (the reason is in [error]). The form is locked: the choice is to try the
     * photo again or go on without it.
     */
    val photoPendingFor: String? = null,
    /** The picked photo couldn't be read or made small enough. */
    val photoUnreadable: Boolean = false,
) {
    val dirty: Boolean get() = form != original
    val locked: Boolean get() = saving || unconfirmed || savedId != null || photoPendingFor != null
}

/**
 * Create (no id in the route) or edit one event.
 * - The form lives in [SavedStateHandle]: process death mid-form loses nothing.
 * - A create carries one idempotency key from the first attempt until the server's answer is known, so a retry after a
 *   timeout (or after process death) returns the event the first attempt made instead of making a second. While the
 *   outcome is unknown the form is locked: the server replays the first attempt's body, so edits made after it would
 *   be silently lost.
 */
@HiltViewModel
class EventFormViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val repository: OrganizerRepository,
) : ViewModel() {

    private val eventId: String? = handle[OrganizerEventViewModel.EVENT_ID_ARG]
    val creating: Boolean = eventId == null

    /** Where the picked photo's file is read and deleted. Tests run it on their own dispatcher. */
    internal var io: CoroutineDispatcher = Dispatchers.IO

    private val _ui = MutableStateFlow(restore())
    val ui: StateFlow<FormUi> = _ui.asStateFlow()

    init {
        if (_ui.value.form == null) load()
    }

    fun retryLoad() = load()

    /** A photo was picked and prepared ([path] in the app's cache); null when it couldn't be read. */
    fun photoPicked(path: String?) {
        if (path == null) {
            _ui.update { it.copy(photoUnreadable = true) }
            return
        }
        _ui.update { it.copy(photoUnreadable = false) }
        edit { it.copy(photoPath = path, photoRemoved = false) }
    }

    fun removePhoto() = edit { it.copy(photoPath = null, photoRemoved = true) }

    /** After "the event saved but its photo didn't": try the photo again, or go on to the event without it. */
    fun retryPhoto() {
        val id = _ui.value.photoPendingFor ?: return
        if (_ui.value.saving) return
        _ui.update { it.copy(saving = true, error = null) }
        viewModelScope.launch { finish(id) }
    }

    fun skipPhoto() {
        val id = _ui.value.photoPendingFor ?: return
        _ui.update { it.copy(photoPendingFor = null, error = null, savedId = id) }
        persist()
    }

    fun edit(transform: (EventForm) -> EventForm) {
        val current = _ui.value
        val form = current.form ?: return
        if (current.locked) return
        _ui.value = current.copy(form = transform(form), error = null)
        persist()
    }

    fun save() {
        val current = _ui.value
        val form = current.form ?: return
        if (current.saving || current.savedId != null || current.photoPendingFor != null) return
        val draft = form.toDraft()
        draft.firstInvalidField(creating)?.let { field ->
            _ui.value = current.copy(error = ApiError.Invalid(field))
            return
        }
        // The key is written before the request leaves, so process death mid-request still finds it.
        val key = if (eventId == null) handle[KEY] ?: UUID.randomUUID().toString().also { handle[KEY] = it } else null
        _ui.value = current.copy(saving = true, error = null)
        viewModelScope.launch {
            val result = if (eventId == null) repository.create(draft, key!!) else repository.update(eventId, draft)
            result.fold(
                onSuccess = { saved ->
                    _ui.update { it.copy(unconfirmed = false) }
                    finish(saved.id)
                },
                onFailure = {
                    val error = it.toApiError().normalized()
                    val unclear = eventId == null && !error.nothingWasMade()
                    if (!unclear) handle.remove<String>(KEY)
                    _ui.update { ui -> ui.copy(saving = false, error = error, unconfirmed = unclear) }
                },
            )
            persist()
        }
    }

    /**
     * The photo goes after the event: a new event needs its id first. If only the photo fails, the event is still
     * saved; the form says so and offers to try the photo again ([retryPhoto]) or leave it ([skipPhoto]).
     */
    private suspend fun finish(savedId: String) {
        val form = _ui.value.form
        val photo = form?.photoPath?.let { path -> withContext(io) { runCatching { File(path).readBytes() }.getOrNull() } }
        val result = when {
            form?.photoPath != null && photo == null -> Result.failure(ApiException(ApiError.Invalid("photo"))) // cache cleared
            photo != null -> repository.setPhoto(savedId, photo).map { }
            form?.photoRemoved == true && form.imageUrl != null -> repository.removePhoto(savedId)
            else -> Result.success(Unit)
        }
        result.fold(
            onSuccess = {
                form?.photoPath?.let { path -> withContext(io) { File(path).delete() } }
                _ui.update { it.copy(saving = false, savedId = savedId, photoPendingFor = null) }
            },
            onFailure = { e -> _ui.update { it.copy(saving = false, photoPendingFor = savedId, error = e.toApiError()) } },
        )
        persist()
    }

    private fun load() {
        val id = eventId ?: return
        _ui.update { it.copy(loadError = null) }
        viewModelScope.launch {
            repository.event(id).fold(
                onSuccess = { event ->
                    val form = event.toForm()
                    _ui.update { it.copy(form = form, original = form) }
                    persist()
                },
                onFailure = { e -> _ui.update { it.copy(loadError = e.toApiError()) } },
            )
        }
    }

    private fun restore(): FormUi {
        val saved = handle.get<String>(STATE)?.let { runCatching { FormJson.decodeFromString<Saved>(it) }.getOrNull() }
        if (saved != null) {
            return FormUi(
                form = saved.form,
                original = saved.original,
                // Killed with a create in flight or unconfirmed: whether it landed is unknown until it's retried.
                unconfirmed = eventId == null && saved.savedId == null && handle.get<String>(KEY) != null,
                savedId = saved.savedId,
                photoPendingFor = saved.photoPendingFor,
            )
        }
        if (eventId != null) return FormUi()
        val blank = EventForm()
        return FormUi(form = blank, original = blank)
    }

    private fun persist() {
        val ui = _ui.value
        val form = ui.form ?: return
        handle[STATE] = FormJson.encodeToString(Saved.serializer(), Saved(form, ui.original ?: form, ui.savedId, ui.photoPendingFor))
    }

    @Serializable
    private data class Saved(
        val form: EventForm,
        val original: EventForm,
        val savedId: String? = null,
        val photoPendingFor: String? = null,
    )

    private companion object {
        const val STATE = "eventForm.state"
        const val KEY = "eventForm.idempotencyKey"

        // encodeDefaults: a ticket type's key has a random default, and a restored form must keep the same keys.
        val FormJson = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    }
}

/**
 * Rejected before anything was created, so the key is spent. Anything else (no answer, a 5xx, an unreadable answer,
 * the key's unique constraint tripping in a race) may have made the event.
 */
private fun ApiError.nothingWasMade(): Boolean = when (this) {
    is ApiError.Invalid, is ApiError.Refused, ApiError.Unauthorized, ApiError.Forbidden, ApiError.RateLimited -> true
    else -> false
}

/** The server names a ticket type's limit by its wire name; the form calls it capacity. */
private fun ApiError.normalized(): ApiError =
    if (this is ApiError.Invalid && field != null) ApiError.Invalid(field.replace(".totalAvailable", ".capacity")) else this

// ── Form ↔ draft.

/** Text the form can't read becomes a value [firstInvalidField] refuses, so the error lands on that field, in order. */
private val Unreadable: BigDecimal = BigDecimal.ONE.negate()

internal fun EventForm.toDraft() = EventDraft(
    name = name,
    venue = venue,
    start = start.toWallClock(),
    end = end.toWallClock(),
    salesStart = salesStart.toWallClock(),
    salesEnd = salesEnd.toWallClock(),
    status = EventStatus.of(status),
    ticketTypes = ticketTypes.map { type ->
        TicketTypeDraft(
            id = type.id,
            name = type.name,
            // Money: at most cents. "12.5" and "12.50" are fine; "12.505" or "12,50" isn't read as a price.
            price = type.price.trim().toBigDecimalOrNull()?.takeIf { it.scale() <= 2 } ?: Unreadable,
            description = type.description.ifBlank { null },
            capacity = type.capacity.trim().let { if (it.isEmpty()) null else it.toIntOrNull() ?: -1 },
            sold = type.sold,
        )
    },
    version = version,
)

internal fun OrganizerEvent.toForm() = EventForm(
    name = name,
    venue = venue,
    start = start?.toString(),
    end = end?.toString(),
    salesStart = salesStart?.toString(),
    salesEnd = salesEnd?.toString(),
    status = status.wire,
    ticketTypes = ticketTypes.map {
        TicketTypeForm(
            key = it.id,
            id = it.id,
            name = it.name,
            price = priceText(it.price),
            description = it.description.orEmpty(),
            capacity = it.capacity?.toString().orEmpty(),
            sold = it.sold,
        )
    },
    version = version,
    imageUrl = imageUrl,
)

/** "25" for whole amounts, "12.50" (never "12.5") for cents: how a price is written. */
internal fun priceText(price: BigDecimal): String = price.stripTrailingZeros().let {
    if (it.scale() <= 0) it.toPlainString() else price.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString()
}

internal fun String?.toWallClock(): LocalDateTime? = this?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

// ── Field errors, in the app's words.

private val TypeField = Regex("""ticketTypes\[(\d+)]\.(\w+)""")

/** What to say about [field] (as the server or [firstInvalidField] names it), given what's in the form. */
internal fun fieldMessage(field: String?, form: EventForm): String {
    TypeField.matchEntire(field.orEmpty())?.let { match ->
        val type = form.ticketTypes.getOrNull(match.groupValues[1].toInt())
        return when (match.groupValues[2]) {
            "name" -> "Name this ticket type."
            "price" -> "Enter a price of 0 or more, like 25 or 12.50."
            "capacity" -> if ((type?.sold ?: 0) > 0) {
                "Can't be lower than the ${type!!.sold} already issued."
            } else {
                "Enter a whole number, or leave it empty for no limit."
            }
            "description" -> "Keep the description under 500 characters."
            else -> "Check this ticket type."
        }
    }
    return when (field) {
        "name" -> "Give the event a name, 2 to 200 characters."
        "venue" -> "Add the venue, 2 to 500 characters."
        "start" -> "A published event needs a start time."
        "end" -> if (form.end == null) "A published event needs an end time." else "The event has to end after it starts."
        "salesEnd" -> {
            val opens = form.salesStart.toWallClock()
            val closes = form.salesEnd.toWallClock()
            if (opens != null && closes != null && !closes.isAfter(opens)) {
                "Sales have to close after they open."
            } else {
                "Sales can't close after the event ends."
            }
        }
        "ticketTypes" -> "Add at least one ticket type."
        "photo" -> "That photo wasn't accepted. Use a JPEG, PNG or WebP photo under 2 MB."
        else -> "Some details aren't right. Check the form and try again."
    }
}
