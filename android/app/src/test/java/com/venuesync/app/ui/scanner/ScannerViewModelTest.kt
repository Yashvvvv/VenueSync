package com.venuesync.app.ui.scanner

import androidx.lifecycle.SavedStateHandle
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.EventDetail
import com.venuesync.app.core.model.SalesStatus
import com.venuesync.app.core.model.ScanResult
import com.venuesync.app.core.model.ScanStatus
import com.venuesync.app.core.model.Guest
import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.core.repository.EventsRepository
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.core.repository.StaffRepository
import com.venuesync.app.core.repository.ValidationRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    /** Each validation waits on a deferred the test completes, so "checking" is a state the test can see. */
    private class FakeValidation : ValidationRepository {
        val calls = mutableListOf<Pair<String, String>>() // code to key
        val manual = mutableListOf<Boolean>()
        var pending = CompletableDeferred<Result<ScanResult>>()
        override suspend fun validate(qrValue: String, eventId: String, idempotencyKey: String): Result<ScanResult> {
            calls += qrValue to idempotencyKey
            manual += false
            return pending.await().also { pending = CompletableDeferred() }
        }
        override suspend fun checkIn(entry: String, eventId: String, idempotencyKey: String): Result<ScanResult> {
            calls += entry to idempotencyKey
            manual += true
            return pending.await().also { pending = CompletableDeferred() }
        }
    }

    private class FakeEvents : EventsRepository {
        override suspend fun getPublishedEvents(query: String?, page: Int) = error("not used")
        override suspend fun getPublishedEvent(id: String) =
            Result.success(EventDetail(id, "Summer Vibes", null, null, null, emptyList(), SalesStatus.OnSale, null, null))
    }

    private class FakeStaff : StaffRepository {
        val queries = mutableListOf<String>()
        override suspend fun staffingEvents() = error("not used")
        override suspend fun acceptInvite(code: String) = error("not used")
        override suspend fun searchGuests(eventId: String, query: String): Result<List<Guest>> {
            queries += query
            return Result.success(listOf(Guest("t-1", "F5A3-038B", "Yash", "ya***@gmail.com", "VIP", TicketStatus.Purchased)))
        }
    }

    private val validation = FakeValidation()
    private val staff = FakeStaff()
    private val handle = SavedStateHandle(mapOf(ScannerViewModel.EVENT_ID_ARG to "e1"))
    private fun vm(savedState: SavedStateHandle = handle) = ScannerViewModel(savedState, validation, FakeEvents(), staff)
    private fun answer(status: ScanStatus) = validation.pending.complete(Result.success(ScanResult(status)))
    private fun fail(error: ApiError) = validation.pending.complete(Result.failure(ApiException(error)))

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `a scan is checked once and shows the answer`() = runTest {
        val vm = vm()
        assertEquals("Summer Vibes", vm.eventName.value)
        vm.onScanned("code-1")
        vm.onScanned("code-2") // a second scan while checking is ignored
        assertEquals(ScanState.Checking, vm.state.value)
        answer(ScanStatus.Valid)
        assertEquals(ScanState.Done(ScanResult(ScanStatus.Valid)), vm.state.value)
        assertEquals(listOf("code-1"), validation.calls.map { it.first })
    }

    @Test
    fun `network failure is retryable with the same key`() = runTest {
        val vm = vm()
        vm.onScanned("code-1")
        fail(ApiError.Network)
        assertTrue(vm.state.value is ScanState.Retryable)
        vm.retry()
        assertEquals(validation.calls[0], validation.calls[1]) // same code, same key
    }

    @Test
    fun `after process death the unconfirmed scan comes back with the same key`() = runTest {
        vm().onScanned("code-1") // in flight when the process dies
        val restored = vm(SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) }))
        assertTrue(restored.state.value is ScanState.Retryable)
        restored.retry()
        assertEquals(validation.calls[0], validation.calls[1])
    }

    @Test
    fun `next scan gets a new key`() = runTest {
        val vm = vm()
        vm.onScanned("code-1")
        answer(ScanStatus.AlreadyUsed)
        vm.next()
        assertEquals(ScanState.Ready, vm.state.value)
        vm.onScanned("code-1")
        assertNotEquals(validation.calls[0].second, validation.calls[1].second)
    }

    @Test
    fun `not staff is a definitive error that spends the key`() = runTest {
        val vm = vm()
        vm.onScanned("code-1")
        fail(ApiError.Forbidden)
        assertTrue(vm.state.value is ScanState.Error)
        assertTrue(vm(SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) })).state.value == ScanState.Ready)
    }

    @Test
    fun `missing event shows an error and never scans`() = runTest {
        val vm = vm(SavedStateHandle())
        assertTrue(vm.state.value is ScanState.Error)
        vm.onScanned("code-1")
        assertTrue(validation.calls.isEmpty())
    }

    @Test
    fun `a typed code checks in manually, and a restored one replays manually too`() = runTest {
        vm().onCodeEntered("F5A3-038B") // in flight when the process dies
        val restored = vm(SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) }))
        restored.retry()
        assertEquals(listOf(true, true), validation.manual)
        assertEquals(validation.calls[0], validation.calls[1])
    }

    @Test
    fun `checking in a guest sends their ticket id`() = runTest {
        val vm = vm()
        vm.checkIn(Guest("t-1", "F5A3-038B", "Yash", null, "VIP", TicketStatus.Purchased))
        assertEquals("t-1" to validation.calls.single().second, validation.calls.single())
        assertEquals(listOf(true), validation.manual)
    }

    @Test
    fun `guest search waits for 2 characters and a pause in typing`() = runTest {
        val vm = vm()
        vm.searchGuests("y")
        assertEquals(GuestSearch("y"), vm.guests.value)
        vm.searchGuests("ya")
        vm.searchGuests("yas")
        vm.searchGuests("yash")
        testScheduler.advanceUntilIdle()
        assertEquals(listOf("yash"), staff.queries) // only the last keystroke reached the server
        assertTrue(vm.guests.value.results is UiState.Success)
    }
}
