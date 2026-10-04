package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.ErrorDto
import com.venuesync.app.core.model.Refusal
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.serialization.ContentConvertException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.SerializationException

/**
 * Runs one API call. Success, or a failure carrying an [ApiException] with a typed [ApiError];
 * only coroutine cancellation escapes. Every repository goes through here.
 */
internal suspend fun <T> apiCall(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    // Ours (screen closed, a newer search): stop, report nothing. Someone else's, handed over by a shared step (the
    // auth token load): a failure the screen can show and retry, never a silent stop that leaves a spinner forever.
    currentCoroutineContext().ensureActive()
    Result.failure(ApiException(ApiError.Network))
} catch (e: ApiException) {
    Result.failure(e) // already classified inside the block
} catch (e: ResponseException) {
    Result.failure(ApiException(e.toApiError()))
} catch (e: IOException) {
    Result.failure(ApiException(ApiError.Network))
} catch (e: ContentConvertException) {
    Result.failure(ApiException(ApiError.InvalidResponse))
} catch (e: SerializationException) {
    Result.failure(ApiException(ApiError.InvalidResponse))
} catch (e: Exception) {
    Result.failure(ApiException(ApiError.Unknown(e.message)))
}

/**
 * `code` first: it's the contract. The status is the fallback for codes this app version doesn't know,
 * so a new backend code can never crash an old app.
 */
private suspend fun ResponseException.toApiError(): ApiError {
    // A non-JSON body (gateway HTML page, Spring Security's empty 403) fails to parse → status fallback.
    val body = runCatching { response.body<ErrorDto>() }.getOrNull()
    when (body?.code) {
        "TICKETS_SOLD_OUT" -> return ApiError.SoldOut
        "SALES_NOT_STARTED", "SALES_ENDED" -> return ApiError.NotOnSale
        "EVENT_INVALID", "VALIDATION_FAILED" -> return ApiError.Invalid(body?.field)
        "TICKET_TYPE_HAS_SALES" -> return ApiError.Refused(Refusal.TicketTypeHasSales)
        "EVENT_HAS_SALES" -> return ApiError.Refused(Refusal.EventHasSales)
        "CAPACITY_BELOW_SOLD" -> return ApiError.Refused(Refusal.CapacityBelowSold)
        "STATUS_CHANGE_INVALID" -> return ApiError.Refused(Refusal.StatusChange)
    }
    return when (response.status.value) {
        401 -> ApiError.Unauthorized
        403 -> ApiError.Forbidden
        404 -> ApiError.NotFound
        409 -> ApiError.Conflict
        410 -> ApiError.Gone
        429 -> ApiError.RateLimited
        in 500..599 -> ApiError.Server(body?.error)
        else -> ApiError.Unknown(body?.error)
    }
}

/** Strict on purpose: java.util.UUID.fromString is lenient and accepts "1-1-1-1-1". */
internal val UuidRegex = Regex("^[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}$")
