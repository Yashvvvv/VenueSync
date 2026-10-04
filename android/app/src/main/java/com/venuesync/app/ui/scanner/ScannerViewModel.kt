package com.venuesync.app.ui.scanner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.Guest
import com.venuesync.app.core.model.ScanResult
import com.venuesync.app.core.model.toApiError
import com.venuesync.app.core.repository.EventsRepository
import com.venuesync.app.core.repository.StaffRepository
import com.venuesync.app.core.repository.ValidationRepository
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.events.message
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanState {
    data object Ready : ScanState
    data object Checking : ScanState
    /** [manual]: from a typed code or the guest list, so "invalid" can say why (see ScannerScreen). */
    data class Done(val result: ScanResult, val manual: Boolean = false) : ScanState
    /** The answer is unknown (network, 5xx). Try again re-sends the SAME key, so the server replays its answer. */
    data class Retryable(val message: String) : ScanState
    /** A definitive refusal that scanning again won't fix (signed out, not staff, unknown event). */
    data class Error(val message: String) : ScanState
}

/** The door guest list: what was typed, and what the server found. Empty query = nothing listed. */
data class GuestSearch(val query: String = "", val results: UiState<List<Guest>> = UiState.Empty)

/**
 * Door rules, the same as purchase: the key is created per scan and lives only in SavedStateHandle; an unknown
 * outcome keeps it, so Try again (even after process death) gets the server's FIRST answer. Without that, a lost
 * response followed by a rescan would say "Already used" about the ticket this very scan just admitted.
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val validation: ValidationRepository,
    private val events: EventsRepository,
    private val staff: StaffRepository,
) : ViewModel() {

    private val eventId: String? = savedStateHandle[EVENT_ID_ARG]

    private val _state = MutableStateFlow<ScanState>(ScanState.Ready)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    /** Only for the header. Optional: scanning works without it. */
    private val _eventName = MutableStateFlow<String?>(null)
    val eventName: StateFlow<String?> = _eventName.asStateFlow()

    private val _guests = MutableStateFlow(GuestSearch())
    val guests: StateFlow<GuestSearch> = _guests.asStateFlow()
    private var guestJob: Job? = null

    init {
        if (eventId == null) {
            _state.value = ScanState.Error("No event chosen. Go back and pick one.")
        } else {
            viewModelScope.launch { events.getPublishedEvent(eventId).onSuccess { _eventName.value = it.name } }
            if (savedStateHandle.get<String>(PENDING_KEY) != null) {
                _state.value = ScanState.Retryable("We couldn't confirm the last scan. Try again before letting them in.")
            }
        }
    }

    fun onScanned(raw: String) = start(raw, manual = false)

    /** Manual check-in when the QR won't scan: a typed ticket code, or a ticket id picked from the guest list. */
    fun onCodeEntered(entry: String) = start(entry, manual = true)

    fun checkIn(guest: Guest) = onCodeEntered(guest.ticketId)

    /** Searches as staff type; waits for a pause so each keystroke isn't a request. */
    fun searchGuests(query: String) {
        val id = eventId ?: return
        guestJob?.cancel()
        if (query.trim().length < MIN_GUEST_QUERY) {
            _guests.value = GuestSearch(query)
            return
        }
        _guests.value = GuestSearch(query, UiState.Loading)
        guestJob = viewModelScope.launch {
            delay(GUEST_SEARCH_DEBOUNCE_MS)
            val results = staff.searchGuests(id, query).fold(
                onSuccess = { if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                onFailure = { UiState.Error(it.toApiError()) },
            )
            _guests.value = GuestSearch(query, results)
        }
    }

    private fun start(entry: String, manual: Boolean) {
        if (_state.value != ScanState.Ready) return // a second scan while one is being checked is ignored
        val key = UUID.randomUUID().toString()
        savedStateHandle[PENDING_CODE] = entry
        savedStateHandle[PENDING_KEY] = key
        savedStateHandle[PENDING_MANUAL] = manual
        send(entry, key, manual)
    }

    fun retry() {
        if (_state.value !is ScanState.Retryable) return
        val code = savedStateHandle.get<String>(PENDING_CODE)
        val key = savedStateHandle.get<String>(PENDING_KEY)
        if (code == null || key == null) {
            _state.value = ScanState.Ready // nothing to replay: scan again
            return
        }
        send(code, key, manual = savedStateHandle.get<Boolean>(PENDING_MANUAL) == true)
    }

    /** Ready for the next attendee. Giving up on a Retryable scan forgets its key. */
    fun next() {
        if (_state.value == ScanState.Checking) return
        clearPending()
        _state.value = ScanState.Ready
    }

    private fun send(code: String, key: String, manual: Boolean) {
        val id = eventId ?: return
        _state.value = ScanState.Checking // synchronous: a double trigger can't send twice
        viewModelScope.launch {
            val result = if (manual) validation.checkIn(code, id, key) else validation.validate(code, id, key)
            result.fold(
                onSuccess = {
                    clearPending()
                    _state.value = ScanState.Done(it, manual)
                },
                onFailure = { onFailed(it.toApiError()) },
            )
        }
    }

    private fun onFailed(error: ApiError) {
        _state.value = when (error) {
            // Outcome unknown, the ticket may already be admitted: keep the key so Try again replays.
            ApiError.Network, ApiError.RateLimited, is ApiError.Server, ApiError.InvalidResponse, is ApiError.Unknown ->
                ScanState.Retryable(
                    if (error == ApiError.Network) "No connection. Try again; it won't use the ticket twice." else error.message(),
                )
            // Refused before anything changed: the key is spent.
            ApiError.Unauthorized -> refused("Your session expired. Sign in again.")
            ApiError.Forbidden -> refused("This account can't scan tickets. Ask an organizer for staff access.")
            ApiError.NotFound, ApiError.Conflict, ApiError.Gone, ApiError.SoldOut, ApiError.NotOnSale,
            is ApiError.Invalid, is ApiError.Refused ->
                refused("This event can't be scanned. Go back and pick it again.")
        }
    }

    private fun refused(message: String): ScanState {
        clearPending()
        return ScanState.Error(message)
    }

    private fun clearPending() {
        savedStateHandle.remove<String>(PENDING_CODE)
        savedStateHandle.remove<String>(PENDING_KEY)
        savedStateHandle.remove<Boolean>(PENDING_MANUAL)
    }

    companion object {
        const val EVENT_ID_ARG = "eventId"
        private const val PENDING_CODE = "scan.code"
        private const val PENDING_KEY = "scan.idempotencyKey"
        private const val PENDING_MANUAL = "scan.manual"
        private const val MIN_GUEST_QUERY = 2
        private const val GUEST_SEARCH_DEBOUNCE_MS = 300L
    }
}
