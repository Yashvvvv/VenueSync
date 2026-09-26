package com.venuesync.app.core.model

import java.time.LocalDateTime

/**
 * Domain model — what the UI and ViewModel see. Never a wire DTO: the backend's field
 * names, string timestamps and error taxonomy stop at the repository boundary.
 */
data class Event(
    val id: String,
    val name: String,
    val start: LocalDateTime?,
    val end: LocalDateTime?,
    val venue: String?,
)

internal fun ListPublishedEventResponseDto.toDomain() = Event(
    id = id,
    name = name,
    start = start?.toLocalDateTimeOrNull(),
    end = end?.toLocalDateTimeOrNull(),
    venue = venue,
)

private fun String.toLocalDateTimeOrNull(): LocalDateTime? =
    runCatching { LocalDateTime.parse(this) }.getOrNull()
