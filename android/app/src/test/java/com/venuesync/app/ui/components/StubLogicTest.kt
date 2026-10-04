package com.venuesync.app.ui.components

import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.core.model.TicketSummary
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StubLogicTest {

    @Test fun `empty copy says which situation it is`() {
        assertEquals("Nothing on sale right now", eventsEmptyCopy("").title)
        assertNull("nothing to search again, so no action", eventsEmptyCopy("   ").action)
        val searched = eventsEmptyCopy("  jazz ")
        assertEquals("Nothing matched that", searched.title)
        assertTrue(searched.body.contains("\"jazz\""))
        assertEquals("Show all events", searched.action)
    }

    @Test fun `a purchased ticket for an ended event reads as expired`() {
        val now = LocalDateTime.of(2026, 10, 4, 12, 0)
        fun ticket(status: TicketStatus, end: LocalDateTime?) = TicketSummary("t", status, "GA", "Show", null, eventEnd = end)
        assertEquals(TicketStatus.Expired, ticket(TicketStatus.Purchased, now.minusMinutes(1)).displayStatus(now))
        assertEquals(TicketStatus.Purchased, ticket(TicketStatus.Purchased, now.plusMinutes(1)).displayStatus(now))
        assertEquals(TicketStatus.Purchased, ticket(TicketStatus.Purchased, null).displayStatus(now))
        assertEquals(TicketStatus.Used, ticket(TicketStatus.Used, now.minusDays(1)).displayStatus(now))
    }

    @Test fun `card date names the last day only when it runs over several`() {
        val start = LocalDateTime.of(2026, 3, 13, 19, 0)
        assertEquals(start.format(CardDay), cardDay(start, start.plusHours(4)))
        assertEquals("${start.format(CardDay)} to ${start.plusDays(2).format(ShortDay)}", cardDay(start, start.plusDays(2)))
        assertEquals(start.format(CardDay), cardDay(start, null))
    }
}
