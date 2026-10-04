package com.venuesync.app.core.model

/** The app's error vocabulary. UI code switches on this, never on HTTP codes or exceptions. */
sealed interface ApiError {
    data object Network : ApiError
    data object Unauthorized : ApiError
    /** Signed in, but the account lacks the role (e.g. no ROLE_ATTENDEE). */
    data object Forbidden : ApiError
    data object NotFound : ApiError
    /** 409 with a code this app version doesn't know: the resource changed under us. */
    data object Conflict : ApiError
    /** 410: the thing existed but is no longer usable (an expired invite code). */
    data object Gone : ApiError
    /** The app owns the wording; the server's text is never shown. */
    data object SoldOut : ApiError
    /** SALES_NOT_STARTED or SALES_ENDED. */
    data object NotOnSale : ApiError
    data object RateLimited : ApiError
    /**
     * The request was refused as invalid (400 EVENT_INVALID or VALIDATION_FAILED, or caught on the device first).
     * [field] names the input to fix (e.g. "end", "ticketTypes[1].price"); the app owns the wording.
     */
    data class Invalid(val field: String?) : ApiError
    /** 409 with a reason this app knows: the server refused to destroy or contradict something that already happened. */
    data class Refused(val reason: Refusal) : ApiError
    /** The server broke the contract: unparseable JSON, missing required fields, wrong entity. */
    data object InvalidResponse : ApiError
    data class Server(val message: String?) : ApiError
    data class Unknown(val message: String?) : ApiError
}

/** Why an organizer change was refused. Each one keeps sold tickets or the catalogue honest. */
enum class Refusal {
    /** TICKET_TYPE_HAS_SALES: removing it would delete tickets people bought. */
    TicketTypeHasSales,
    /** EVENT_HAS_SALES: deleting it would delete tickets people bought; cancel instead. */
    EventHasSales,
    /** CAPACITY_BELOW_SOLD */
    CapacityBelowSold,
    /** STATUS_CHANGE_INVALID: e.g. a cancelled event is final. */
    StatusChange,
    /** EVENT_CHANGED: the edit was made from an older copy; someone changed the event since. */
    EventChanged,
}

/** Carrier so repository results fit the stdlib [Result] without losing the typed error. */
class ApiException(val error: ApiError) : Exception(error.toString())

fun Throwable.toApiError(): ApiError =
    (this as? ApiException)?.error ?: ApiError.Unknown(message)
