package com.venuesync.app.core.repository

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiCallCancellationTest {

    @Test
    fun `someone else's cancellation is a failure the screen can show, not silence`() = runTest {
        val result = apiCall<Unit> { throw CancellationException("handed over by a shared step") }
        assertEquals(ApiError.Network, (result.exceptionOrNull() as ApiException).error)
    }

    @Test
    fun `our own cancellation still stops the call`() = runTest {
        var result: Result<Unit>? = null
        val job = launch { result = apiCall { awaitCancellation() } }
        yield()
        job.cancel()
        job.join()
        assertTrue(job.isCancelled)
        assertNull(result) // nothing was reported: the caller is gone
    }
}
