package com.venuesync.app.core.model

import java.math.BigDecimal
import java.time.LocalDateTime
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
    fun `a list row without id or name is dropped, not fatal`() {
        assertNull(ListPublishedEventResponseDto(id = null, name = "Show").toDomainOrNull())
        assertNull(ListPublishedEventResponseDto(id = "e1", name = " ").toDomainOrNull())
        assertEquals("Show", ListPublishedEventResponseDto(id = "e1", name = "Show").toDomainOrNull()!!.name)
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
    fun `server times keep the event's wall clock, with or without an offset`() {
        val wallClock = LocalDateTime.of(2026, 10, 5, 19, 0)
        assertEquals(wallClock, "2026-10-05T19:00:00+05:30".toLocalDateTimeOrNull()) // never converted to device zone
        assertEquals(wallClock, "2026-10-05T19:00:00Z".toLocalDateTimeOrNull())
        assertEquals(wallClock, "2026-10-05T19:00:00".toLocalDateTimeOrNull()) // servers before the offset change
        assertEquals(
            LocalDateTime.of(2026, 9, 30, 9, 0, 26, 996_534_000),
            "2026-09-30T09:00:26.996534+05:30".toLocalDateTimeOrNull(), // fractional seconds, as seed data sends
        )
    }

    @Test
    fun `unparseable dates become null instead of failing`() {
        val event = GetPublishedEventDetailsResponseDto(id = "e1", name = "Show", start = "tomorrow")
            .toDomainOrNull()!!
        assertNull(event.start)
    }

    @Test
    fun `sales fields and soldOut are mapped, unknown values are safe`() {
        val event = GetPublishedEventDetailsResponseDto(
            id = "e1",
            name = "Show",
            salesStatus = "UPCOMING",
            salesStart = "2026-10-05T10:00:00+05:30",
            ticketTypes = listOf(ticket(id = "a").copy(soldOut = true), ticket(id = "b")),
        ).toDomainOrNull()!!
        assertEquals(SalesStatus.Upcoming, event.salesStatus)
        assertEquals(LocalDateTime.of(2026, 10, 5, 10, 0), event.salesStart)
        assertEquals(listOf(true, false), event.ticketTypes.map { it.soldOut }) // missing → not sold out
        assertEquals(SalesStatus.OnSale, "ON_SALE".toSalesStatus())
        assertEquals(SalesStatus.Ended, "ENDED".toSalesStatus())
        assertEquals(SalesStatus.Unknown, "SOMETHING_NEW".toSalesStatus())
        assertEquals(SalesStatus.Unknown, null.toSalesStatus())
    }

    @Test
    fun `availability covers every branch`() {
        val open = TicketType("a", "GA", BigDecimal.TEN, null, soldOut = false)
        val gone = open.copy(soldOut = true)
        val start = LocalDateTime.of(2026, 10, 5, 10, 0)
        fun event(status: SalesStatus) = EventDetail("e1", "Show", null, null, null, listOf(open, gone), status, start, null)

        assertEquals(Availability.Buyable, event(SalesStatus.OnSale).availabilityOf(open))
        assertEquals(Availability.Buyable, event(SalesStatus.Unknown).availabilityOf(open)) // server decides
        assertEquals(Availability.SoldOut, event(SalesStatus.OnSale).availabilityOf(gone))
        assertEquals(Availability.OnSaleFrom(start), event(SalesStatus.Upcoming).availabilityOf(open))
        assertEquals(Availability.SalesEnded, event(SalesStatus.Ended).availabilityOf(gone))
    }
}
