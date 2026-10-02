package com.venuesync.app.ui.events

import com.venuesync.app.core.model.Ticket
import com.venuesync.app.core.model.TicketType

/** The buy flow on the event detail screen. The idempotency key is not here: SavedStateHandle owns it. */
sealed interface PurchaseState {
    data object Idle : PurchaseState
    /** One-shot: the screen opens login, then calls onSignInHandled(). Nothing is bought after login. */
    data object SignInRequired : PurchaseState
    data class Confirming(val type: TicketType) : PurchaseState
    data class Purchasing(val type: TicketType) : PurchaseState
    /** Outcome unknown. Try again re-sends the SAME key, so it can never buy twice. */
    data class Retryable(val type: TicketType, val message: String) : PurchaseState
    data class Failed(val message: String) : PurchaseState
    /** One-shot: the screen opens the result, then calls onPurchaseShown(). */
    data class Purchased(val ticket: Ticket) : PurchaseState
}
