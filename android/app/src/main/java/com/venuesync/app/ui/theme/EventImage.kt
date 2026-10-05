package com.venuesync.app.ui.theme

import androidx.annotation.DrawableRes
import com.venuesync.app.BuildConfig
import com.venuesync.app.R
import kotlin.math.abs

// ponytail: stand-in photography, the same 4 files the web ships. The API has no image field; when it gains one,
// this becomes the fallback.
private val EventImages = intArrayOf(
    R.drawable.event_image_1, R.drawable.event_image_2, R.drawable.event_image_3, R.drawable.event_image_4,
)

/**
 * The same photo for the same event as the web (frontend/src/components/random-event-image.tsx), so an event keeps
 * one identity across the site and the app. Kotlin's Int wraps exactly like JS `h |= 0`; abs(Int.MIN_VALUE) stays
 * negative here, but MIN_VALUE % 4 == 0, which is also what JS gets.
 */
@DrawableRes
fun eventImageFor(id: String): Int = EventImages[eventImageIndex(id)]

/** The API's origin, which [eventImage]'s relative photo paths hang off. */
private val ApiOrigin = BuildConfig.API_BASE_URL.removeSuffix("/").removeSuffix("/api/v1")

/** What Coil loads for an event: its own photo (an absolute URL) when it has one, else its stand-in drawable. */
fun eventImage(id: String, imageUrl: String?): Any = imageUrl?.let { ApiOrigin + it } ?: eventImageFor(id)

internal fun eventImageIndex(seed: String): Int {
    var h = 0
    for (c in seed) h = (h shl 5) - h + c.code
    return abs(h) % EventImages.size
}
