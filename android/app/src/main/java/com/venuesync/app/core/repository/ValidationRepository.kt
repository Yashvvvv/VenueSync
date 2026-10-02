package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.ScanResult
import com.venuesync.app.core.model.ScanStatus
import com.venuesync.app.core.model.ValidationRequestDto
import com.venuesync.app.core.model.toDomainOrNull
import com.venuesync.app.core.network.TicketsApi
import javax.inject.Inject

interface ValidationRepository {
    /**
     * Checks a scanned code in at [eventId]'s door, admitting the ticket if valid.
     * [qrValue] is the raw scanned text. The same [idempotencyKey] always gets the same answer.
     */
    suspend fun validate(qrValue: String, eventId: String, idempotencyKey: String): Result<ScanResult>
}

class ValidationRepositoryImpl @Inject constructor(
    private val api: TicketsApi,
) : ValidationRepository {

    override suspend fun validate(qrValue: String, eventId: String, idempotencyKey: String): Result<ScanResult> {
        if (!UuidRegex.matches(eventId)) return Result.failure(ApiException(ApiError.NotFound))
        if (!UuidRegex.matches(idempotencyKey)) {
            return Result.failure(ApiException(ApiError.Unknown("invalid idempotency key")))
        }
        // Our codes hold a bare UUID. Anything else (a URL, a Wi-Fi code, another app's ticket) is simply
        // not our ticket: an answer, not an error, and no reason to ask the server.
        val code = qrValue.trim()
        if (!UuidRegex.matches(code)) return Result.success(ScanResult(ScanStatus.Invalid))
        return apiCall {
            api.validate(ValidationRequestDto(id = code, method = "QR_SCAN", eventId = eventId), idempotencyKey).toDomainOrNull()
                ?: throw ApiException(ApiError.InvalidResponse)
        }
    }
}
