package com.venuesync.app.ui.tickets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.Ticket
import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.core.model.toApiError
import com.venuesync.app.core.repository.TicketsRepository
import com.venuesync.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** The QR code loads apart from the ticket: the details can show while the code fails, and the reverse. */
sealed interface QrState {
    /** The ticket can't be used (used, expired, cancelled): there's no code to show. */
    data object Hidden : QrState
    data object Loading : QrState
    /** Not a data class: equality on a ByteArray would be by reference anyway. */
    class Ready(val png: ByteArray) : QrState
    data class Error(val error: ApiError) : QrState
}

@HiltViewModel
class TicketDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TicketsRepository,
) : ViewModel() {

    // Nullable on purpose: a malformed route must show an error, never crash.
    private val ticketId: String? = savedStateHandle[TICKET_ID_ARG]

    private val _ticket = MutableStateFlow<UiState<Ticket>>(UiState.Loading)
    val ticket: StateFlow<UiState<Ticket>> = _ticket.asStateFlow()

    private val _qr = MutableStateFlow<QrState>(QrState.Hidden)
    val qr: StateFlow<QrState> = _qr.asStateFlow()

    private var ticketJob: Job? = null
    private var qrJob: Job? = null

    init {
        loadTicket()
    }

    fun retryTicket() = loadTicket()

    fun retryQr() {
        if ((_ticket.value as? UiState.Success)?.data?.hasCode() == true) loadQr()
    }

    private fun loadTicket() {
        val id = ticketId ?: run {
            _ticket.value = UiState.Error(ApiError.NotFound)
            return
        }
        ticketJob?.cancel()
        _ticket.value = UiState.Loading
        ticketJob = viewModelScope.launch {
            repository.getTicket(id).fold(
                onSuccess = {
                    _ticket.value = UiState.Success(it)
                    if (it.hasCode()) loadQr(fromPhone = it.savedAt != null) else _qr.value = QrState.Hidden
                },
                onFailure = { _ticket.value = UiState.Error(it.toApiError()) },
            )
        }
    }

    /**
     * [fromPhone]: the ticket itself just came from the phone because the network failed, so asking the network for
     * the code would only add the same retries again (seconds, at a door). Retry on the screen goes live again.
     */
    private fun loadQr(fromPhone: Boolean = false) {
        val id = ticketId ?: return
        qrJob?.cancel()
        _qr.value = QrState.Loading
        qrJob = viewModelScope.launch {
            if (fromPhone) {
                repository.savedQrCode(id)?.let {
                    _qr.value = QrState.Ready(it)
                    return@launch
                }
            }
            repository.getQrCode(id).fold(
                onSuccess = { _qr.value = QrState.Ready(it) },
                onFailure = { _qr.value = QrState.Error(it.toApiError()) },
            )
        }
    }

    /** Unknown (a status this app version doesn't know) still shows the code: the scanner is the authority. */
    private fun Ticket.hasCode() = status == TicketStatus.Purchased || status == TicketStatus.Unknown

    companion object {
        const val TICKET_ID_ARG = "ticketId"
    }
}
