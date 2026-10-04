package com.venuesync.app.ui.events

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.auth.ROLE_ATTENDEE
import com.venuesync.app.core.auth.Session
import com.venuesync.app.core.auth.SessionManager
import com.venuesync.app.core.auth.roles
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.EventDetail
import com.venuesync.app.core.model.TicketType
import com.venuesync.app.core.model.toApiError
import com.venuesync.app.core.repository.EventsRepository
import com.venuesync.app.core.repository.TicketsRepository
import com.venuesync.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Signed in without the attendee role (an organizer): the selector says so instead of offering a button. */
enum class Buyer { Unknown, SignedOut, Attendee, NotAttendee }

/**
 * Purchase rules:
 * - The idempotency key is created on Confirm and lives only in [SavedStateHandle], so it survives process death.
 * - The key is cleared only when the server's answer proves no ticket was made, or once the result was shown.
 *   Any unclear outcome keeps it, so the next attempt for that type replays instead of buying a second ticket.
 * - Signed out → login; nothing is bought automatically after login.
 */
@HiltViewModel
class EventDetailViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: EventsRepository,
    private val tickets: TicketsRepository,
    private val session: SessionManager,
) : ViewModel() {

    // Filled by Navigation from the {eventId} route segment; survives process death.
    // Nullable on purpose: a malformed route must show an error, never crash.
    private val eventId: String? = savedStateHandle[EVENT_ID_ARG]

    private val _state = MutableStateFlow<UiState<EventDetail>>(UiState.Loading)
    val state: StateFlow<UiState<EventDetail>> = _state.asStateFlow()

    private val _purchase = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchase: StateFlow<PurchaseState> = _purchase.asStateFlow()

    /** Who's looking: picks the selector's button label, or the note for an account that can't buy. */
    val buyer: StateFlow<Buyer> = session.session
        .map {
            when (it) {
                Session.Unknown -> Buyer.Unknown
                Session.SignedOut -> Buyer.SignedOut
                is Session.SignedIn -> if (ROLE_ATTENDEE in it.roles) Buyer.Attendee else Buyer.NotAttendee
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Buyer.Unknown)

    private var job: Job? = null

    /** Only the first successful load may bring back a purchase interrupted by process death. */
    private var restoreInterrupted = true

    init {
        load()
    }

    fun retry() = load()

    /** [silent]: refresh behind what's on screen (no spinner; a failure keeps the old data). */
    private fun load(silent: Boolean = false) {
        val id = eventId ?: run {
            _state.value = UiState.Error(ApiError.NotFound)
            return
        }
        job?.cancel() // a retry replaces the in-flight load, so results never race
        if (!silent) _state.value = UiState.Loading
        job = viewModelScope.launch {
            repository.getPublishedEvent(id).fold(
                onSuccess = {
                    _state.value = UiState.Success(it)
                    restoreInterruptedPurchase(it)
                },
                onFailure = { if (!silent) _state.value = UiState.Error(it.toApiError()) },
            )
        }
    }

    fun buy(type: TicketType) {
        if (_purchase.value != PurchaseState.Idle) return
        viewModelScope.launch {
            val tokens = session.currentTokens() // reads storage, so never "unknown yet"
            if (_purchase.value != PurchaseState.Idle) return@launch // a second tap got here first
            _purchase.value = when {
                tokens == null -> PurchaseState.SignInRequired
                ROLE_ATTENDEE !in tokens.roles() -> PurchaseState.Failed(NO_ATTENDEE_ROLE)
                else -> PurchaseState.Confirming(type)
            }
        }
    }

    fun confirmPurchase() {
        val type = (_purchase.value as? PurchaseState.Confirming)?.type ?: return // a 2nd tap sees Purchasing
        val key = pendingKeyFor(type.id) ?: UUID.randomUUID().toString().also { savePending(type.id, it) }
        send(type, key)
    }

    fun retryPurchase() {
        val type = (_purchase.value as? PurchaseState.Retryable)?.type ?: return
        // Retryable always has a saved key. If it's gone, ask again rather than invent one behind the user's back.
        val key = pendingKeyFor(type.id) ?: run {
            _purchase.value = PurchaseState.Confirming(type)
            return
        }
        send(type, key)
    }

    fun dismissPurchase() {
        if (_purchase.value !is PurchaseState.Purchasing) _purchase.value = PurchaseState.Idle
    }

    fun onSignInHandled() {
        if (_purchase.value == PurchaseState.SignInRequired) _purchase.value = PurchaseState.Idle
    }

    fun onPurchaseShown() {
        if (_purchase.value !is PurchaseState.Purchased) return
        clearPending()
        _purchase.value = PurchaseState.Idle
    }

    private fun send(type: TicketType, key: String) {
        val id = eventId ?: return
        _purchase.value = PurchaseState.Purchasing(type) // synchronous: a double tap can't send twice
        viewModelScope.launch {
            tickets.purchase(id, type.id, key).fold(
                onSuccess = {
                    _purchase.value = PurchaseState.Purchased(it)
                    load(silent = true) // we may have bought the last one
                },
                onFailure = { onPurchaseFailed(type, it.toApiError()) },
            )
        }
    }

    private fun onPurchaseFailed(type: TicketType, error: ApiError) {
        _purchase.value = when (error) {
            // Outcome unknown: keep the key, Try again replays it.
            ApiError.Network -> PurchaseState.Retryable(type, "No connection. Try again: you won't get a second ticket.")
            ApiError.RateLimited, is ApiError.Server -> PurchaseState.Retryable(type, error.message())
            // The ticket may exist: keep the key so another attempt for this type replays instead of buying again.
            ApiError.InvalidResponse, is ApiError.Unknown ->
                PurchaseState.Failed("We couldn't confirm your ticket. Check My Tickets before trying again.")
            // Rejected before anything was created: the key is spent.
            // (Invalid and Refused are organizer answers; a purchase never gets them, but if one did, nothing was made.)
            ApiError.SoldOut, ApiError.NotOnSale, ApiError.NotFound, ApiError.Conflict, ApiError.Gone,
            is ApiError.Invalid, is ApiError.Refused -> {
                clearPending()
                load(silent = true) // show the new availability under the message
                PurchaseState.Failed(
                    when (error) {
                        ApiError.SoldOut -> "Sold out. Someone got the last one."
                        ApiError.NotOnSale -> error.message()
                        else -> "This ticket changed. We've refreshed the event."
                    },
                )
            }
            ApiError.Unauthorized -> {
                clearPending()
                PurchaseState.Failed("Your session expired. Sign in and try again.")
            }
            ApiError.Forbidden -> {
                clearPending()
                PurchaseState.Failed(NO_ATTENDEE_ROLE)
            }
        }
    }

    private fun restoreInterruptedPurchase(detail: EventDetail) {
        if (!restoreInterrupted) return
        restoreInterrupted = false
        val typeId = savedStateHandle.get<String>(PENDING_TYPE_ID) ?: return
        val type = detail.ticketTypes.firstOrNull { it.id == typeId } ?: return clearPending()
        if (_purchase.value == PurchaseState.Idle) {
            _purchase.value = PurchaseState.Retryable(
                type,
                "Your last attempt wasn't confirmed. Getting the ticket now finishes that attempt, so you won't get two.",
            )
        }
    }

    // ponytail: one pending key per screen. Confirming type B while A's outcome is unknown forgets A's key,
    // so a later retry of A could buy a second A. Per-type map if that ever matters.
    private fun pendingKeyFor(typeId: String): String? =
        savedStateHandle.get<String>(PENDING_KEY)?.takeIf { savedStateHandle.get<String>(PENDING_TYPE_ID) == typeId }

    private fun savePending(typeId: String, key: String) {
        savedStateHandle[PENDING_TYPE_ID] = typeId
        savedStateHandle[PENDING_KEY] = key
    }

    private fun clearPending() {
        savedStateHandle.remove<String>(PENDING_TYPE_ID)
        savedStateHandle.remove<String>(PENDING_KEY)
    }

    companion object {
        const val EVENT_ID_ARG = "eventId"
        private const val PENDING_TYPE_ID = "purchase.ticketTypeId"
        private const val PENDING_KEY = "purchase.idempotencyKey"
        private const val NO_ATTENDEE_ROLE = "Your account can't buy tickets yet. Sign out and sign in again."
    }
}
