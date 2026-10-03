package com.venuesync.app.ui.tickets

import androidx.lifecycle.SavedStateHandle
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.Ticket
import com.venuesync.app.core.model.TicketFilter
import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.core.model.TicketSummary
import com.venuesync.app.core.repository.TicketPage
import com.venuesync.app.core.repository.TicketsRepository
import com.venuesync.app.ui.common.UiState
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TicketsViewModelsTest {

    private class FakeTickets : TicketsRepository {
        var savedQr: ByteArray? = null
        override suspend fun savedQrCode(ticketId: String) = savedQr
        var syncs = 0
        override fun syncOffline(force: Boolean) {
            syncs++
        }
        val listCalls = mutableListOf<Pair<TicketFilter, Int>>()
        var pages: (TicketFilter, Int) -> Result<TicketPage> = { _, _ -> Result.success(TicketPage(emptyList(), true)) }
        var ticket: Result<Ticket> = Result.failure(ApiException(ApiError.NotFound))
        val qrResults = ArrayDeque<Result<ByteArray>>()
        var qrCalls = 0

        override suspend fun purchase(eventId: String, ticketTypeId: String, idempotencyKey: String) = error("not used")
        override suspend fun listTickets(filter: TicketFilter, page: Int): Result<TicketPage> {
            listCalls += filter to page
            return pages(filter, page)
        }
        override suspend fun getTicket(id: String) = ticket
        override suspend fun getQrCode(ticketId: String): Result<ByteArray> {
            qrCalls++
            return qrResults.removeFirst()
        }
    }

    private val repo = FakeTickets()
    private fun summary(id: String) = TicketSummary(id, TicketStatus.Purchased, "GA", "Show", null)
    private fun ticket(status: TicketStatus) = Ticket("t1", "T100-0000", status, "GA", null, "e1", "Show", null, null, null, null)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    // ── My tickets ──

    @Test
    fun `starts on Active, switching tab loads Past`() = runTest {
        repo.pages = { filter, _ -> Result.success(TicketPage(listOf(summary(filter.name)), isLast = true)) }
        val vm = MyTicketsViewModel(SavedStateHandle(), repo)
        assertEquals(UiState.Success(listOf(summary("Active"))), vm.state.value)

        vm.select(TicketFilter.Past)
        vm.select(TicketFilter.Past) // re-selecting the open tab does nothing
        assertEquals(UiState.Success(listOf(summary("Past"))), vm.state.value)
        assertEquals(listOf(TicketFilter.Active to 0, TicketFilter.Past to 0), repo.listCalls)
    }

    @Test
    fun `the selected tab survives a new ViewModel on the same SavedStateHandle`() = runTest {
        val handle = SavedStateHandle()
        MyTicketsViewModel(handle, repo).select(TicketFilter.Past)
        assertEquals(TicketFilter.Past, MyTicketsViewModel(handle, repo).filter.value)
    }

    @Test
    fun `no tickets is Empty`() = runTest {
        assertEquals(UiState.Empty, MyTicketsViewModel(SavedStateHandle(), repo).state.value)
    }

    @Test
    fun `loadMore appends the next page and stops at the last`() = runTest {
        repo.pages = { _, page -> Result.success(TicketPage(listOf(summary("p$page")), isLast = page == 1)) }
        val vm = MyTicketsViewModel(SavedStateHandle(), repo)
        vm.loadMore()
        vm.loadMore() // server said last: no request
        assertEquals(UiState.Success(listOf(summary("p0"), summary("p1"))), vm.state.value)
        assertEquals(2, repo.listCalls.size)
    }

    @Test
    fun `a live Active list refreshes the phone's copy, an offline one or Past doesn't`() = runTest {
        repo.pages = { _, _ -> Result.success(TicketPage(listOf(summary("a")), isLast = true)) }
        val vm = MyTicketsViewModel(SavedStateHandle(), repo)
        assertEquals(1, repo.syncs)

        vm.select(TicketFilter.Past)
        assertEquals(1, repo.syncs)

        repo.pages = { _, _ -> Result.success(TicketPage(listOf(summary("a")), isLast = true, savedAt = Instant.EPOCH)) }
        vm.select(TicketFilter.Active)
        assertEquals(1, repo.syncs) // syncing from the copy would only fail again
    }

    // ── Ticket detail ──

    private fun detailVm() = TicketDetailViewModel(SavedStateHandle(mapOf(TicketDetailViewModel.TICKET_ID_ARG to "t1")), repo)

    @Test
    fun `purchased ticket loads its QR code`() = runTest {
        repo.ticket = Result.success(ticket(TicketStatus.Purchased))
        repo.qrResults += Result.success(byteArrayOf(1))
        val vm = detailVm()
        assertTrue(vm.ticket.value is UiState.Success)
        assertTrue(vm.qr.value is QrState.Ready)
    }

    @Test
    fun `a ticket from the phone shows the phone's code without trying the network again`() = runTest {
        repo.ticket = Result.success(ticket(TicketStatus.Purchased).copy(savedAt = Instant.EPOCH))
        repo.savedQr = byteArrayOf(9)
        val vm = detailVm()
        assertTrue(vm.qr.value is QrState.Ready)
        assertEquals(0, repo.qrCalls)
    }

    @Test
    fun `a live ticket whose code is already on the phone doesn't download it again`() = runTest {
        repo.ticket = Result.success(ticket(TicketStatus.Purchased)) // live: savedAt is null
        repo.savedQr = byteArrayOf(9)
        val vm = detailVm()
        assertTrue(vm.qr.value is QrState.Ready)
        assertEquals(0, repo.qrCalls)
    }

    @Test
    fun `used ticket shows no code and never requests one`() = runTest {
        repo.ticket = Result.success(ticket(TicketStatus.Used))
        val vm = detailVm()
        assertEquals(QrState.Hidden, vm.qr.value)
        assertEquals(0, repo.qrCalls)
    }

    @Test
    fun `QR failure keeps the ticket, and retry reloads only the code`() = runTest {
        repo.ticket = Result.success(ticket(TicketStatus.Purchased))
        repo.qrResults += Result.failure(ApiException(ApiError.Network))
        repo.qrResults += Result.success(byteArrayOf(1))
        val vm = detailVm()
        assertEquals(QrState.Error(ApiError.Network), vm.qr.value)
        assertTrue(vm.ticket.value is UiState.Success)

        repo.ticket = Result.failure(IllegalStateException("must not be reloaded"))
        vm.retryQr()
        assertTrue(vm.qr.value is QrState.Ready)
        assertTrue(vm.ticket.value is UiState.Success)
    }

    @Test
    fun `missing route arg shows NotFound`() = runTest {
        val vm = TicketDetailViewModel(SavedStateHandle(), repo)
        assertEquals(UiState.Error(ApiError.NotFound), vm.ticket.value)
    }
}
