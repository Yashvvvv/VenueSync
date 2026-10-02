package com.venuesync.app.ui.scanner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ScanResult
import com.venuesync.app.core.model.toApiError
import com.venuesync.app.core.repository.EventsRepository
import com.venuesync.app.core.repository.ValidationRepository
import com.venuesync.app.ui.events.message
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanState {
    data object Ready : ScanState
    data object Checking : ScanState
    data class Done(val result: ScanResult) : ScanState
    /** The answer is unknown (network, 5xx). Try again re-sends the SAME key, so the server replays its answer. */
    data class Retryable(val message: String) : ScanState
    /** A definitive refusal that scanning again won't fix (signed out, not staff, unknown event). */
    data class Error(val message: String) : ScanState
}

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
) : ViewModel() {

    private val eventId: String? = savedStateHandle[EVENT_ID_ARG]

    private val _state = MutableStateFlow<ScanState>(ScanState.Ready)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    /** Only for the header. Optional: scanning works without it. */
    private val _eventName = MutableStateFlow<String?>(null)
    val eventName: StateFlow<String?> = _eventName.asStateFlow()

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

    fun onScanned(raw: String) {
        if (_state.value != ScanState.Ready) return // a second scan while one is being checked is ignored
        val key = UUID.randomUUID().toString()
        savedStateHandle[PENDING_CODE] = raw
        savedStateHandle[PENDING_KEY] = key
        send(raw, key)
    }

    fun retry() {
        if (_state.value !is ScanState.Retryable) return
        val code = savedStateHandle.get<String>(PENDING_CODE)
        val key = savedStateHandle.get<String>(PENDING_KEY)
        if (code == null || key == null) {
            _state.value = ScanState.Ready // nothing to replay: scan again
            return
        }
        send(code, key)
    }

    /** Ready for the next attendee. Giving up on a Retryable scan forgets its key. */
    fun next() {
        if (_state.value == ScanState.Checking) return
        clearPending()
        _state.value = ScanState.Ready
    }

    private fun send(code: String, key: String) {
        val id = eventId ?: return
        _state.value = ScanState.Checking // synchronous: a double trigger can't send twice
        viewModelScope.launch {
            validation.validate(code, id, key).fold(
                onSuccess = {
                    clearPending()
                    _state.value = ScanState.Done(it)
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
            ApiError.NotFound, ApiError.Conflict, ApiError.SoldOut, ApiError.NotOnSale ->
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
    }

    companion object {
        const val EVENT_ID_ARG = "eventId"
        private const val PENDING_CODE = "scan.code"
        private const val PENDING_KEY = "scan.idempotencyKey"
    }
}
