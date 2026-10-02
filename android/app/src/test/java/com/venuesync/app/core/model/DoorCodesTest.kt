package com.venuesync.app.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DoorCodesTest {

    @Test
    fun `invite codes normalize like the server reads them`() {
        assertEquals("K7Q2M9XH4P", normalizeInviteCode(" k7q2m-9xh4p "))
        assertEquals("10Q2M9XH41", normalizeInviteCode("IOQ2M 9XH4L")) // I/L -> 1, O -> 0
        assertNull(normalizeInviteCode("K7Q2M-9XH4"))  // too short
        assertNull(normalizeInviteCode("K7Q2M-9XH4U")) // U isn't in the alphabet
    }

    @Test
    fun `check-in entries are a ticket code or a full ticket id`() {
        assertEquals("f5a3038b", normalizeCheckInEntry(" f5a3-038b "))
        assertEquals("F5A3038B", normalizeCheckInEntry("F5A3 038B"))
        val id = "f5a3038b-3412-46f9-8f10-33fc236d6b17"
        assertEquals(id, normalizeCheckInEntry(id))
        assertNull(normalizeCheckInEntry("G5A3-038B")) // not hex
        assertNull(normalizeCheckInEntry("F5A3-03"))   // too short
    }

    @Test
    fun `ticket code falls back to the server's rule`() {
        assertEquals("F5A3-038B", ticketCodeOf("f5a3038b-3412-46f9-8f10-33fc236d6b17"))
        val ticket = TicketDto(id = "f5a3038b-3412-46f9-8f10-33fc236d6b17", ticketTypeName = "GA", eventId = "e", eventName = "Show")
        assertEquals("F5A3-038B", ticket.toDomainOrNull()!!.code)
        assertEquals("AB12-CD34", ticket.copy(ticketCode = "AB12-CD34").toDomainOrNull()!!.code) // the server's value wins
    }
}
