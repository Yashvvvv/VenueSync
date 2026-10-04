package com.venuesync.app.ui.organizer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.model.EventCounts
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** An organizer's events: counts per status, a list filtered by one status (or all), paged. */
@HiltViewModel
class OrganizerEventsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: OrganizerRepository,
) : ViewModel() {

    // The chosen filter survives process death; a junk or unknown value means "all".
    private val _filter = MutableStateFlow(
        savedStateHandle.get<String>(FILTER)?.let(EventStatus::of)?.takeIf { it != EventStatus.Unknown },
    )
    /** Null = every status. */
    val filter: StateFlow<EventStatus?> = _filter.asStateFlow()

    private val _counts = MutableStateFlow<EventCounts?>(null)
    /** Null until loaded (or if they failed): the filter row then shows labels without numbers. */
    val counts: StateFlow<EventCounts?> = _counts.asStateFlow()

    private val _state = MutableStateFlow<UiState<List<OrganizerEvent>>>(UiState.Loading)
    val state: StateFlow<UiState<List<OrganizerEvent>>> = _state.asStateFlow()

    private var page = 0
    private var isLast = false
    private var job: Job? = null
    private var loadedOnce = false

    init {
        load(silent = false)
    }

    fun select(status: EventStatus?) {
        if (status == _filter.value || status == EventStatus.Unknown) return
        _filter.value = status
        savedStateHandle[FILTER] = status?.wire
        load(silent = false)
    }

    fun retry() = load(silent = false)

    /** Back from an event or the form: refresh behind what's on screen (a failure keeps it). */
    fun refresh() {
        if (loadedOnce) load(silent = true)
    }

    fun loadMore() {
        val current = _state.value as? UiState.Success ?: return
        if (isLast || job?.isActive == true) return
        job = viewModelScope.launch {
            repository.events(_filter.value, page + 1).onSuccess { next ->
                page += 1
                isLast = next.isLast
                // Offset paging can repeat a row when an event is created between loads; keys must stay unique.
                _state.update { UiState.Success((current.data + next.events).distinctBy { it.id }) }
            }
            // ponytail: a failed load-more keeps the list as-is, like the event list; surface it if it ever matters.
        }
    }

    private fun load(silent: Boolean) {
        job?.cancel()
        if (!silent) _state.value = UiState.Loading
        viewModelScope.launch { repository.counts().onSuccess { _counts.value = it } }
        job = viewModelScope.launch {
            repository.events(_filter.value, page = 0).fold(
                onSuccess = { first ->
                    page = 0
                    isLast = first.isLast
                    loadedOnce = true
                    _state.value = if (first.events.isEmpty()) UiState.Empty else UiState.Success(first.events)
                },
                onFailure = { if (!silent || _state.value !is UiState.Success) _state.value = UiState.Error(it.toApiError()) },
            )
        }
    }

    private companion object {
        const val FILTER = "organizer.events.filter"
    }
}
