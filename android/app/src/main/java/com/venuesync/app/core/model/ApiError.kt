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
    /** The server broke the contract: unparseable JSON, missing required fields, wrong entity. */
    data object InvalidResponse : ApiError
    data class Server(val message: String?) : ApiError
    data class Unknown(val message: String?) : ApiError
}

/** Carrier so repository results fit the stdlib [Result] without losing the typed error. */
class ApiException(val error: ApiError) : Exception(error.toString())

fun Throwable.toApiError(): ApiError =
    (this as? ApiException)?.error ?: ApiError.Unknown(message)
