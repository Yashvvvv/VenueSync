package com.venuesync.app.core.repository

import com.venuesync.app.core.model.TicketDto
import kotlinx.serialization.Serializable

/**
 * A ticket saved on the phone for when the venue has no signal. The server's own DTO, so reading it back goes
 * through the same checks as live data.
 */
@Serializable
data class CachedTicket(
    val ticket: TicketDto,
    /** Base64 PNG; null until the code has been fetched once. */
    val qrPng: String? = null,
    /** Epoch millis of the live response this came from. */
    val savedAt: Long,
)

/** The whole set is read and written at once: it's a handful of ~1 KB entries. */
interface TicketCache {
    /** Empty unless signed in, so a signed-out phone never shows the previous account's tickets. */
    suspend fun read(): List<CachedTicket>
    /** Ignored unless signed in: a request that finishes after sign-out must not write tickets back. */
    suspend fun write(tickets: List<CachedTicket>)
    suspend fun clear()
}
