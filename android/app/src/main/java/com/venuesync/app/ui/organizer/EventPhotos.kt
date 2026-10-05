package com.venuesync.app.ui.organizer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.venuesync.app.core.repository.MaxPhotoBytes
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MaxEdgePx = 1600

/**
 * Turns a picked photo into what the server takes: a JPEG at most 1600 px on its long edge and under 2 MB, upright
 * (Coil applies the camera's EXIF rotation, which a plain decode ignores and which leaves phone photos sideways).
 * Written to the app's cache so the form can keep just its path, across process death too. Null when the file
 * can't be read as an image.
 */
suspend fun preparePhoto(context: Context, uri: Uri): File? {
    val request = ImageRequest.Builder(context)
        .data(uri)
        .size(MaxEdgePx)
        .scale(Scale.FIT)
        .allowHardware(false) // a hardware bitmap can't be compressed
        .memoryCacheKey(null as String?)
        .build()
    val bitmap = ((context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
        ?: return null
    return withContext(Dispatchers.IO) {
        // Most photos fit at 85; a busy one is tried again smaller rather than refused.
        val jpeg = listOf(85, 70, 55).asSequence()
            .map { quality -> ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }.toByteArray() }
            .firstOrNull { it.size <= MaxPhotoBytes }
            ?: return@withContext null
        val dir = File(context.cacheDir, "event-photos").apply { mkdirs() }
        File(dir, "${UUID.randomUUID()}.jpg").apply { writeBytes(jpeg) }
    }
}
