package com.venuesync.app.ui.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.EventDetail
import com.venuesync.app.core.model.toApiError
import com.venuesync.app.core.repository.EventsRepository
import com.venuesync.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class EventDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: EventsRepository,
) : ViewModel() {

    // Filled by Navigation from the {eventId} route segment; survives process death.
    // Nullable on purpose: a malformed route must show an error, never crash.
    private val eventId: String? = savedStateHandle[EVENT_ID_ARG]

    private val _state = MutableStateFlow<UiState<EventDetail>>(UiState.Loading)
    val state: StateFlow<UiState<EventDetail>> = _state.asStateFlow()

    private var job: Job? = null

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        val id = eventId ?: run {
            _state.value = UiState.Error(ApiError.NotFound)
            return
        }
        job?.cancel() // a retry replaces the in-flight load, so results never race
        _state.value = UiState.Loading
        job = viewModelScope.launch {
            repository.getPublishedEvent(id).fold(
                onSuccess = { _state.value = UiState.Success(it) },
                onFailure = { _state.value = UiState.Error(it.toApiError()) },
            )
        }
    }

    companion object {
        const val EVENT_ID_ARG = "eventId"
    }
}
