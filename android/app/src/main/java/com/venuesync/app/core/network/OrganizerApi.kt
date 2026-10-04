package com.venuesync.app.core.network

import com.venuesync.app.core.model.EventWriteDto
import com.venuesync.app.core.model.OrganizerEventDto
import com.venuesync.app.core.model.PageResponse
import com.venuesync.app.core.model.StaffInviteDto
import com.venuesync.app.core.model.StaffMemberDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

/* Organizer endpoints. Thin wrapper: mechanical HTTP only, no error mapping (that's repository work). */
class OrganizerApi(private val client: HttpClient) {

    suspend fun listEvents(status: String?, page: Int, size: Int): PageResponse<OrganizerEventDto> =
        client.get("events") {
            if (status != null) parameter("status", status)
            parameter("page", page)
            parameter("size", size)
            parameter("sort", "createdAt,desc") // newest first, like the web dashboard
        }.body()

    suspend fun counts(): Map<String, Long> = client.get("events/counts").body()

    suspend fun getEvent(id: String): OrganizerEventDto = client.get("events/$id").body()

    /** Same key = same event: a retry after a timeout returns the first one. The caller owns the key's lifetime. */
    suspend fun createEvent(body: EventWriteDto, idempotencyKey: String): OrganizerEventDto =
        client.post("events") {
            header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
            contentType(ContentType.Application.Json)
            setBody(body)
            timeout { requestTimeoutMillis = 60_000 } // Render cold start
        }.body()

    /** A full replace: ticket types left out are removed (the server refuses if any have tickets). */
    suspend fun updateEvent(id: String, body: EventWriteDto): OrganizerEventDto =
        client.put("events/$id") {
            contentType(ContentType.Application.Json)
            setBody(body)
            timeout { requestTimeoutMillis = 60_000 }
        }.body()

    suspend fun deleteEvent(id: String) {
        client.delete("events/$id")
    }

    suspend fun staff(eventId: String): List<StaffMemberDto> = client.get("events/$eventId/staff").body()

    suspend fun createInvite(eventId: String): StaffInviteDto = client.post("events/$eventId/staff-invites").body()

    suspend fun removeStaff(eventId: String, userId: String) {
        client.delete("events/$eventId/staff/$userId")
    }

    /** Self-service upgrade to organizer (the account stays an attendee too). */
    suspend fun becomeOrganizer() {
        client.post("users/me/roles/organizer")
    }
}
