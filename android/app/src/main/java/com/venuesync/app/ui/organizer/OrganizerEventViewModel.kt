package com.venuesync.app.ui.organizer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.EventStatus
import com.venuesync.app.core.model.OrganizerEvent
import com.venuesync.app.core.model.toApiError
import com.venuesync.app.core.repository.OrganizerRepository
import com.venuesync.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What an organizer can do to the event from its overview. */
enum class EventAction { Publish, Unpublish, Cancel, Delete }

/** The last action's progress. [Done] after a delete means the event is gone: the screen leaves. */
sealed interface ActionState {
    data object Idle : ActionState
    data class Working(val action: EventAction) : ActionState
    data class Failed(val action: EventAction, val error: ApiError) : ActionState
    data object Deleted : ActionState
}

/** One of the organizer's events: its sales per ticket type and the status moves the server allows from here. */
@HiltViewModel
class OrganizerEventViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: OrganizerRepository,
) : ViewModel() {

    // Nullable on purpose: a malformed route shows an error, never a crash.
    private val eventId: String? = savedStateHandle[EVENT_ID_ARG]

    private val _state = MutableStateFlow<UiState<OrganizerEvent>>(UiState.Loading)
    val state: StateFlow<UiState<OrganizerEvent>> = _state.asStateFlow()

    private val _action = MutableStateFlow<ActionState>(ActionState.Idle)
    val action: StateFlow<ActionState> = _action.asStateFlow()

    private var job: Job? = null

    init {
        load(silent = false)
    }

    fun retry() = load(silent = false)

    /** Back from the form: show what was saved, behind what's on screen. */
    fun refresh() {
        if (_state.value is UiState.Success && _action.value !is ActionState.Working) load(silent = true)
    }

    fun dismissError() {
        if (_action.value is ActionState.Failed) _action.value = ActionState.Idle
    }

    fun perform(action: EventAction) {
        val id = eventId ?: return
        if (_action.value is ActionState.Working || _action.value == ActionState.Deleted) return
        _action.value = ActionState.Working(action) // synchronous: a double tap can't send twice
        job?.cancel() // a refresh landing after the change would show the old status
        viewModelScope.launch {
            val result = if (action == EventAction.Delete) {
                repository.delete(id).map { null }
            } else {
                // An update replaces the whole event, so it's built from the server's latest copy, not the one on
                // screen: an edit made on the website meanwhile survives a status change made here.
                // ponytail: still last-write-wins between the read and the write; the backend has no version or ETag.
                repository.event(id).mapCatching { fresh ->
                    repository.update(id, fresh.toDraft().copy(status = action.target())).getOrThrow()
                }
            }
            result.fold(
                onSuccess = { updated ->
                    if (updated == null) {
                        _action.value = ActionState.Deleted
                    } else {
                        _state.value = UiState.Success(updated)
                        _action.value = ActionState.Idle
                    }
                },
                onFailure = {
                    _action.value = ActionState.Failed(action, it.toApiError())
                    load(silent = true) // whatever the refusal was about, show the event as it is now
                },
            )
        }
    }

    private fun load(silent: Boolean) {
        val id = eventId ?: run {
            _state.value = UiState.Error(ApiError.NotFound)
            return
        }
        job?.cancel()
        if (!silent) _state.value = UiState.Loading
        job = viewModelScope.launch {
            repository.event(id).fold(
                onSuccess = { _state.value = UiState.Success(it) },
                onFailure = { if (!silent || _state.value !is UiState.Success) _state.value = UiState.Error(it.toApiError()) },
            )
        }
    }

    companion object {
        const val EVENT_ID_ARG = "eventId"
    }
}

private fun EventAction.target(): EventStatus = when (this) {
    EventAction.Publish -> EventStatus.Published
    EventAction.Unpublish -> EventStatus.Draft
    EventAction.Cancel -> EventStatus.Cancelled
    EventAction.Delete -> error("a delete has no status")
}

/** The moves the server allows (EventServiceImpl.checkStatusChange), so the screen never offers one it refuses. */
internal fun OrganizerEvent.actions(): List<EventAction> = when (status) {
    EventStatus.Draft -> listOf(EventAction.Publish, EventAction.Delete)
    EventStatus.Published -> if (sold == 0L) listOf(EventAction.Unpublish, EventAction.Cancel) else listOf(EventAction.Cancel)
    EventStatus.Cancelled, EventStatus.Completed, EventStatus.Unknown -> emptyList()
}

/** Cancelled and ended events are a record; the form is for events that can still change. */
internal fun OrganizerEvent.editable(): Boolean = status == EventStatus.Draft || status == EventStatus.Published
