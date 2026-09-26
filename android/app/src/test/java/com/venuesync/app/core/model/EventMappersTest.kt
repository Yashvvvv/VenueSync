package com.venuesync.app.core.model

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventMappersTest {

    private fun ticket(id: String? = "t1", name: String? = "GA", price: Double? = 10.0) =
        PublishedTicketTypeDto(id = id, name = name, price = price)

    private fun detail(
        id: String? = "e1",
        name: String? = "Show",
        ticketTypes: List<PublishedTicketTypeDto>? = null,
    ) = GetPublishedEventDetailsResponseDto(id = id, name = name, ticketTypes = ticketTypes)

    @Test
    fun `missing or blank id or name breaks the contract`() {
        assertNull(detail(id = null).toDomainOrNull())
        assertNull(detail(id = " ").toDomainOrNull())
        assertNull(detail(name = null).toDomainOrNull())
        assertNull(detail(name = "").toDomainOrNull())
    }

    @Test
    fun `null ticketTypes becomes an empty list`() {
        assertEquals(emptyList<TicketType>(), detail(ticketTypes = null).toDomainOrNull()!!.ticketTypes)
    }

    @Test
    fun `invalid ticket types are dropped and valid ones kept`() {
        val types = detail(
            ticketTypes = listOf(
                ticket(id = "ok", price = 19.99),
                ticket(id = "negative", price = -5.0),
                ticket(id = "nan", price = Double.NaN),
                ticket(id = "inf", price = Double.POSITIVE_INFINITY),
                ticket(id = "noprice", price = null),
                ticket(id = null),
                ticket(id = "noname", name = " "),
                ticket(id = "free", price = 0.0),
            ),
        ).toDomainOrNull()!!.ticketTypes
        assertEquals(listOf("ok", "free"), types.map { it.id })
    }

    @Test
    fun `price keeps its decimal value exactly`() {
        val type = detail(ticketTypes = listOf(ticket(price = 19.99))).toDomainOrNull()!!.ticketTypes.single()
        assertEquals(BigDecimal("19.99"), type.price)
    }

    @Test
    fun `duplicate ticket ids are collapsed so list keys stay unique`() {
        val types = detail(ticketTypes = listOf(ticket(id = "a"), ticket(id = "a"))).toDomainOrNull()!!.ticketTypes
        assertEquals(1, types.size)
    }

    @Test
    fun `unparseable dates become null instead of failing`() {
        val event = GetPublishedEventDetailsResponseDto(id = "e1", name = "Show", start = "tomorrow")
            .toDomainOrNull()!!
        assertNull(event.start)
    }
}
