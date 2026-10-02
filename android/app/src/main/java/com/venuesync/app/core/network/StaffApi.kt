package com.venuesync.app.core.network

import com.venuesync.app.core.model.AcceptStaffInviteResponseDto
import com.venuesync.app.core.model.GuestDto
import com.venuesync.app.core.model.ListPublishedEventResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post

/* Door staff endpoints. Thin wrapper: mechanical HTTP only, no error mapping (that's repository work). */
class StaffApi(private val client: HttpClient) {

    /** Events this user can scan: organized plus staffed, published. */
    suspend fun staffingEvents(): List<ListPublishedEventResponseDto> = client.get("users/me/staffing-events").body()

    suspend fun acceptInvite(code: String): AcceptStaffInviteResponseDto = client.post("staff-invites/$code/accept").body()

    suspend fun guests(eventId: String, query: String): List<GuestDto> =
        client.get("staff/events/$eventId/guests") { parameter("q", query) }.body()
}
