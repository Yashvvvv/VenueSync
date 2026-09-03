package com.venuesync.app.core.model

/*
 * Sealed error model per architecture.md. SHELL ONLY — the mapping logic
 * (HTTP status + ErrorDto.error message -> ApiError) is Yash's to write in the
 * repository layer (M1). Backend quirk to handle there: almost everything is 400,
 * sold-out is 400 with message "Tickets are sold out for this ticket type".
 */
sealed interface ApiError {
    data object Network : ApiError
    data object Unauthorized : ApiError
    data object NotFound : ApiError
    data class SoldOut(val message: String) : ApiError
    data class Server(val message: String?) : ApiError
    data class Unknown(val message: String?) : ApiError
}
