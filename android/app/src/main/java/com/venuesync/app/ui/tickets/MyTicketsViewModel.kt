package com.venuesync.app.ui.tickets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.model.TicketFilter
import com.venuesync.app.core.model.TicketSummary
import com.venuesync.app.core.model.toApiError
import com.venuesync.app.core.repository.TicketsRepository
import com.venuesync.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MyTicketsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: TicketsRepository,
) : ViewModel() {

    // Saved as the enum's name (a String is always bundle-safe); anything unreadable falls back to Active.
    private val _filter = MutableStateFlow(
        TicketFilter.entries.firstOrNull { it.name == savedStateHandle.get<String>(FILTER) } ?: TicketFilter.Active,
    )
    val filter: StateFlow<TicketFilter> = _filter.asStateFlow()

    private val _state = MutableStateFlow<UiState<List<TicketSummary>>>(UiState.Loading)
    val state: StateFlow<UiState<List<TicketSummary>>> = _state.asStateFlow()

    private var page = 0
    private var isLast = false
    private var job: Job? = null

    init {
        load()
    }

    fun select(filter: TicketFilter) {
        if (filter == _filter.value) return
        _filter.value = filter
        savedStateHandle[FILTER] = filter.name
        load()
    }

    fun retry() = load()

    /** Appends the next server page. No-op while loading or once the server says last. */
    fun loadMore() {
        val current = _state.value as? UiState.Success ?: return
        if (isLast || job?.isActive == true) return
        val filter = _filter.value
        job = viewModelScope.launch {
            repository.listTickets(filter, page + 1).onSuccess { next ->
                page += 1
                isLast = next.isLast
                // Offset paging can repeat a row when tickets change between loads; duplicate keys crash LazyColumn.
                _state.update { UiState.Success((current.data + next.tickets).distinctBy { it.id }) }
            }
            // ponytail: a failed load-more keeps the list as-is, same as the event list
        }
    }

    private fun load() {
        job?.cancel() // switching tabs replaces the in-flight load, so one tab's rows never land in the other
        page = 0
        isLast = false
        _state.value = UiState.Loading
        val filter = _filter.value
        job = viewModelScope.launch {
            repository.listTickets(filter, page = 0).fold(
                onSuccess = { first ->
                    isLast = first.isLast
                    _state.value = if (first.tickets.isEmpty()) UiState.Empty else UiState.Success(first.tickets)
                },
                onFailure = { _state.value = UiState.Error(it.toApiError()) },
            )
        }
    }

    private companion object {
        const val FILTER = "tickets.filter"
    }
}
