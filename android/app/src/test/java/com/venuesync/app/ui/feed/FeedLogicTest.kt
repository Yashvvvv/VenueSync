package com.venuesync.app.ui.feed

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FeedLogicTest {

    @Test fun `the feed's empty copy tells a failed search from an empty catalogue`() {
        val none = feedEmptyCopy(" ")
        assertEquals("Nothing on sale yet", none.title)
        assertNull("nothing to undo, so no action", none.action)

        val searched = feedEmptyCopy(" techno ")
        assertEquals("Nothing matched that", searched.title)
        assertEquals("No events came back for \"techno\".", searched.body)
        assertEquals("Show everything", searched.action)
    }
}
