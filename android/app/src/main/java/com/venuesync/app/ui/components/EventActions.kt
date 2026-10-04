package com.venuesync.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import java.time.LocalDateTime
import java.time.ZoneId

/** The web's public event page: what a shared link opens for someone without the app. */
internal fun eventUrl(eventId: String) = "https://venuesync.pages.dev/events/$eventId"

/** The system share sheet, with the web's wording. No permission, no dependency. */
internal fun shareEvent(context: Context, eventId: String, name: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, name)
        putExtra(Intent.EXTRA_TEXT, "Have a look at $name\n${eventUrl(eventId)}")
    }
    context.startActivity(Intent.createChooser(send, null))
}

/** Events are wall-clock India time (ADR-003): the calendar gets that zone, so the entry shows the printed time. */
private val EventZone = ZoneId.of("Asia/Kolkata")

/**
 * Hands the event to the calendar app's own "new event" screen (CalendarContract insert intent): the person reviews
 * and saves it there, so the app needs no calendar permission.
 */
internal fun addToCalendar(context: Context, name: String, venue: String?, start: LocalDateTime, end: LocalDateTime?) {
    val insert = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
        putExtra(CalendarContract.Events.TITLE, name)
        venue?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
        putExtra(CalendarContract.Events.EVENT_TIMEZONE, EventZone.id)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start.atZone(EventZone).toInstant().toEpochMilli())
        end?.takeIf { it.isAfter(start) }?.let {
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it.atZone(EventZone).toInstant().toEpochMilli())
        }
    }
    try {
        context.startActivity(insert)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No calendar app on this phone.", Toast.LENGTH_SHORT).show()
    }
}
