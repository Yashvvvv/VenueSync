package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.ScanResult
import com.venuesync.app.core.model.ScanStatus
import com.venuesync.app.core.model.ValidationRequestDto
import com.venuesync.app.core.model.normalizeCheckInEntry
import com.venuesync.app.core.model.toDomainOrNull
import com.venuesync.app.core.network.TicketsApi
import javax.inject.Inject

interface ValidationRepository {
    /**
     * Checks a scanned code in at [eventId]'s door, admitting the ticket if valid.
     * [qrValue] is the raw scanned text. The same [idempotencyKey] always gets the same answer.
     */
    suspend fun validate(qrValue: String, eventId: String, idempotencyKey: String): Result<ScanResult>

    /**
     * Manual check-in, for when the QR code won't scan: [entry] is a ticket code (F5A3-038B) or a full ticket id
     * (from the guest list). Same key rules as [validate].
     */
    suspend fun checkIn(entry: String, eventId: String, idempotencyKey: String): Result<ScanResult>
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
        return send(ValidationRequestDto(id = code, method = "QR_SCAN", eventId = eventId), idempotencyKey)
    }

    override suspend fun checkIn(entry: String, eventId: String, idempotencyKey: String): Result<ScanResult> {
        if (!UuidRegex.matches(eventId)) return Result.failure(ApiException(ApiError.NotFound))
        if (!UuidRegex.matches(idempotencyKey)) {
            return Result.failure(ApiException(ApiError.Unknown("invalid idempotency key")))
        }
        // Neither a ticket code nor a ticket id: it can't match any ticket, so it's an answer, not a request.
        val id = normalizeCheckInEntry(entry) ?: return Result.success(ScanResult(ScanStatus.Invalid))
        return send(ValidationRequestDto(id = id, method = "MANUAL", eventId = eventId), idempotencyKey)
    }

    private suspend fun send(request: ValidationRequestDto, idempotencyKey: String): Result<ScanResult> = apiCall {
        api.validate(request, idempotencyKey).toDomainOrNull() ?: throw ApiException(ApiError.InvalidResponse)
    }
}
