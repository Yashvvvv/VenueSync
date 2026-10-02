package com.venuesync.app.core.model

/** The app's error vocabulary. UI code switches on this, never on HTTP codes or exceptions. */
sealed interface ApiError {
    data object Network : ApiError
    data object Unauthorized : ApiError
    data object NotFound : ApiError
    data object RateLimited : ApiError
    /** The server broke the contract: unparseable JSON, missing required fields, wrong entity. */
    data object InvalidResponse : ApiError
    data class SoldOut(val message: String) : ApiError
    data class Server(val message: String?) : ApiError
    data class Unknown(val message: String?) : ApiError
}

/** Carrier so repository results fit the stdlib [Result] without losing the typed error. */
class ApiException(val error: ApiError) : Exception(error.toString())

fun Throwable.toApiError(): ApiError =
    (this as? ApiException)?.error ?: ApiError.Unknown(message)
