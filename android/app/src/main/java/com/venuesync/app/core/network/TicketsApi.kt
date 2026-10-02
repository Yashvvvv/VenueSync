package com.venuesync.app.core.network

import com.venuesync.app.core.model.TicketDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.header
import io.ktor.client.request.post

/* Thin endpoint wrapper — mechanical HTTP only, no error mapping (that's repository work). */
class TicketsApi(private val client: HttpClient) {

    /** Same key = same ticket. The caller owns the key's lifetime; this only sends it. */
    suspend fun purchase(eventId: String, ticketTypeId: String, idempotencyKey: String): TicketDto =
        client.post("events/$eventId/ticket-types/$ticketTypeId/tickets") {
            header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            // Render cold start (~30s) plus QR generation; the default 30s would cut a working purchase off.
            timeout { requestTimeoutMillis = 60_000 }
        }.body()
}
