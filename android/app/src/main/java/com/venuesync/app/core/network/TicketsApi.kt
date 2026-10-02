package com.venuesync.app.core.network

import com.venuesync.app.core.model.ListTicketDto
import com.venuesync.app.core.model.PageResponse
import com.venuesync.app.core.model.TicketDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.ContentType

/* Thin endpoint wrapper — mechanical HTTP only, no error mapping (that's repository work). */
class TicketsApi(private val client: HttpClient) {

    /** Same key = same ticket. The caller owns the key's lifetime; this only sends it. */
    suspend fun purchase(eventId: String, ticketTypeId: String, idempotencyKey: String): TicketDto =
        client.post("events/$eventId/ticket-types/$ticketTypeId/tickets") {
            header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            // Render cold start (~30s) plus QR generation; the default 30s would cut a working purchase off.
            timeout { requestTimeoutMillis = 60_000 }
        }.body()

    suspend fun listTickets(filter: String, page: Int, size: Int = 20): PageResponse<ListTicketDto> =
        client.get("tickets") {
            parameter("filter", filter)
            parameter("page", page)
            parameter("size", size)
        }.body()

    suspend fun getTicket(id: String): TicketDto = client.get("tickets/$id").body()

    /**
     * PNG bytes. The endpoint sets Content-Type image/png explicitly, and Spring answers 406 when the Accept
     * header doesn't allow it. ContentNegotiation only adds application/json, so image/png is named here.
     */
    suspend fun getQrCode(ticketId: String): ByteArray =
        client.get("tickets/$ticketId/qr-codes") { accept(ContentType.Image.PNG) }.body()
}
