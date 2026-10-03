package com.venuesync.app.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

/** Vectors computed with the web's own seedToIndex, which numbers photos from 1. */
class EventImageTest {

    @Test fun `same photo as the web for the same seed`() {
        mapOf(
            "0c8f9522-aa34-414d-9f7a-85e330229f70" to 1,
            "6dd8477a-c595-42ae-975a-940a56e2cf09" to 2,
            "f63cb229-89e9-4585-a715-e14adeb77e65" to 3,
            "272633e8-de0c-4e6b-a85a-7e890d50ec78" to 4,
            "venuesync" to 3,
            "gate-scan" to 2,
        ).forEach { (seed, web) -> assertEquals(seed, web, eventImageIndex(seed) + 1) }
    }

    @Test fun `an empty seed still picks a photo`() {
        assertEquals(0, eventImageIndex(""))
    }
}
