package com.venuesync.app.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.model.Event
import com.venuesync.app.core.model.toApiError
import com.venuesync.app.core.repository.EventsRepository
import com.venuesync.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val repository: EventsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState<List<Event>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Event>>> = _state.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private var page = 0
    private var isLast = false
    private var job: Job? = null

    init {
        load()
    }

    /** Searches as people type; waits for a pause so each keystroke isn't a request ("summer" was 6). */
    fun onQueryChanged(value: String) {
        val sameSearch = value.trim() == _query.value.trim() // a trailing space changes nothing the server sees
        _query.value = value
        if (!sameSearch) load(debounceMs = SEARCH_DEBOUNCE_MS)
    }

    fun retry() = load()

    /** Appends the next server page. No-op while loading or once the server says last. */
    fun loadMore() {
        val current = _state.value as? UiState.Success ?: return
        if (isLast || job?.isActive == true) return
        job = viewModelScope.launch {
            repository.getPublishedEvents(_query.value.trim().ifBlank { null }, page + 1).onSuccess { next ->
                page += 1
                isLast = next.isLast
                // Offset paging shifts when events are published between loads, so a page can repeat
                // items; duplicate LazyColumn keys crash, hence distinctBy.
                _state.update { UiState.Success((current.data + next.events).distinctBy { it.id }) }
            }
            // ponytail: a failed load-more keeps the list as-is; surface a toast when UX asks for it
        }
    }

    private fun load(debounceMs: Long = 0) {
        job?.cancel()
        page = 0
        isLast = false
        _state.value = UiState.Loading
        job = viewModelScope.launch {
            delay(debounceMs) // cancelled by the next keystroke before anything is sent
            repository.getPublishedEvents(_query.value.trim().ifBlank { null }, page = 0).fold(
                onSuccess = { first ->
                    isLast = first.isLast
                    _state.value = if (first.events.isEmpty()) UiState.Empty else UiState.Success(first.events.distinctBy { it.id })
                },
                onFailure = { _state.value = UiState.Error(it.toApiError()) },
            )
        }
    }

    private companion object {
        // Phone typing averages ~330 ms a letter; 300 ms would still search mid-word for many people.
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
