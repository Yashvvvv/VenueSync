package com.venuesync.app.core.model

import kotlinx.serialization.Serializable

/*
 * DTOs mirror the backend contract (Track-B-VenueSync/client-api.md + repo docs/06_API).
 * Field names verified against backend Java DTOs on 17 Jul 2026 — do not invent fields.
 * Timestamps arrive as ISO-8601 strings without zone (Java LocalDateTime); kept as String
 * here, formatting is a UI concern (M1).
 */

/** GET /published-events — Page<ListPublishedEventResponseDto> */
@Serializable
data class ListPublishedEventResponseDto(
    val id: String,
    val name: String,
    val start: String? = null,
    val end: String? = null,
    val venue: String? = null,
)

/**
 * GET /published-events/{id}. Every field is optional on the wire; the mapper
 * (toDomainOrNull) is the single place that decides what is required.
 */
@Serializable
data class GetPublishedEventDetailsResponseDto(
    val id: String? = null,
    val name: String? = null,
    val start: String? = null,
    val end: String? = null,
    val venue: String? = null,
    val ticketTypes: List<PublishedTicketTypeDto>? = null,
)

@Serializable
data class PublishedTicketTypeDto(
    val id: String? = null,
    val name: String? = null,
    val price: Double? = null,
    val description: String? = null,
)

/** Spring Page<T> envelope (same shape the web client types as SpringBootPagination<T>). */
@Serializable
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
    val size: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true,
)

/**
 * Backend error envelope. `code` is the stable contract clients branch on; `error` is display text that
 * may change. Both optional: Spring Security's 401/403 and gateway errors arrive without this body.
 */
@Serializable
data class ErrorDto(
    val code: String? = null,
    val error: String? = null,
)
