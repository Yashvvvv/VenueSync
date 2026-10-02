package com.venuesync.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.Event
import com.venuesync.app.core.model.JoinedEvent
import com.venuesync.app.core.model.normalizeInviteCode
import com.venuesync.app.core.model.toApiError
import com.venuesync.app.core.repository.StaffRepository
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.events.message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface JoinState {
    data object Idle : JoinState
    data object Joining : JoinState
    data class Failed(val message: String) : JoinState
    /** One-shot: the screen opens that event's scanner, then calls onJoinedShown(). */
    data class Joined(val event: JoinedEvent) : JoinState
}

/** The scanner's front door: the events I can scan, and redeeming an organizer's invite code to add one. */
@HiltViewModel
class StaffEventsViewModel @Inject constructor(
    private val repository: StaffRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Event>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Event>>> = _state.asStateFlow()

    private val _join = MutableStateFlow<JoinState>(JoinState.Idle)
    val join: StateFlow<JoinState> = _join.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun retry() = load()

    fun join(code: String) {
        if (_join.value == JoinState.Joining) return
        if (normalizeInviteCode(code) == null) {
            _join.value = JoinState.Failed("That isn't an invite code. It looks like K7Q2M-9XH4P.")
            return
        }
        _join.value = JoinState.Joining // synchronous: a double tap redeems once
        viewModelScope.launch {
            repository.acceptInvite(code).fold(
                onSuccess = {
                    _join.value = JoinState.Joined(it)
                    load() // the new event belongs in the list when they come back
                },
                onFailure = { _join.value = JoinState.Failed(joinMessage(it.toApiError())) },
            )
        }
    }

    fun onJoinedShown() {
        if (_join.value is JoinState.Joined) _join.value = JoinState.Idle
    }

    fun dismissJoinError() {
        if (_join.value is JoinState.Failed) _join.value = JoinState.Idle
    }

    private fun load() {
        loadJob?.cancel()
        _state.value = UiState.Loading
        loadJob = viewModelScope.launch {
            repository.staffingEvents().fold(
                onSuccess = { _state.value = if (it.isEmpty()) UiState.Empty else UiState.Success(it) },
                onFailure = { _state.value = UiState.Error(it.toApiError()) },
            )
        }
    }

    private fun joinMessage(error: ApiError) = when (error) {
        ApiError.NotFound -> "No invite has that code. Check it with the organizer."
        ApiError.Conflict -> "Someone else already used this code. Ask the organizer for a new one."
        ApiError.Gone -> "This code has expired. Ask the organizer for a new one."
        else -> error.message()
    }
}
