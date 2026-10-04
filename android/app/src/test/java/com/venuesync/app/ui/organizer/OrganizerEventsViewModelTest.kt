package com.venuesync.app.ui.organizer

import androidx.lifecycle.SavedStateHandle
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.EventCounts
import com.venuesync.app.core.model.EventDraft
import com.venuesync.app.core.model.EventStatus
import com.venuesync.app.core.model.OrganizerEvent
import com.venuesync.app.core.model.OrganizerTicketType
import com.venuesync.app.core.model.StaffInvite
import com.venuesync.app.core.model.StaffMember
import com.venuesync.app.core.repository.OrganizerEventPage
import com.venuesync.app.core.repository.OrganizerRepository
import com.venuesync.app.ui.common.UiState
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizerEventsViewModelTest {

    /** Answers [pages] by page number; records every status asked for. */
    private class FakeRepo(
        var pages: Map<Int, Result<OrganizerEventPage>>,
        var counts: Result<EventCounts> = Result.success(EventCounts(1, 2, 0, 0)),
    ) : OrganizerRepository {
        val statuses = mutableListOf<EventStatus?>()
        override suspend fun events(status: EventStatus?, page: Int, size: Int): Result<OrganizerEventPage> {
            statuses += status
            return pages[page] ?: error("unexpected page $page")
        }
        override suspend fun counts() = counts
        override suspend fun event(id: String): Result<OrganizerEvent> = error("not used")
        override suspend fun create(draft: EventDraft, idempotencyKey: String): Result<OrganizerEvent> = error("not used")
        override suspend fun update(id: String, draft: EventDraft): Result<OrganizerEvent> = error("not used")
        override suspend fun delete(id: String): Result<Unit> = error("not used")
        override suspend fun staff(eventId: String): Result<List<StaffMember>> = error("not used")
        override suspend fun createInvite(eventId: String): Result<StaffInvite> = error("not used")
        override suspend fun removeStaff(eventId: String, userId: String): Result<Unit> = error("not used")
        override suspend fun becomeOrganizer(): Result<Unit> = error("not used")
    }

    private fun event(id: String, sold: Long = 0, capacity: Int? = 100) = OrganizerEvent(
        id, "Event $id", null, null, "Hall", null, null, EventStatus.Published,
        listOf(OrganizerTicketType("t$id", "GA", BigDecimal.TEN, null, capacity, sold)),
    )

    private fun page(vararg ids: String, last: Boolean = true) = Result.success(OrganizerEventPage(ids.map { event(it) }, last))

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads every status with counts`() = runTest {
        val repo = FakeRepo(mapOf(0 to page("a", "b")))
        val vm = OrganizerEventsViewModel(SavedStateHandle(), repo)
        assertEquals(listOf("a", "b"), (vm.state.value as UiState.Success).data.map { it.id })
        assertEquals(listOf<EventStatus?>(null), repo.statuses)
        assertEquals(2L, vm.counts.value!!.published)
    }

    @Test
    fun `the filter survives process death, and junk means all`() = runTest {
        val repo = FakeRepo(mapOf(0 to page("a")))
        OrganizerEventsViewModel(SavedStateHandle(mapOf("organizer.events.filter" to "DRAFT")), repo)
        OrganizerEventsViewModel(SavedStateHandle(mapOf("organizer.events.filter" to "WHATEVER")), repo)
        assertEquals(listOf(EventStatus.Draft, null), repo.statuses)
    }

    @Test
    fun `choosing a filter reloads with it and remembers it`() = runTest {
        val repo = FakeRepo(mapOf(0 to page("a")))
        val handle = SavedStateHandle()
        val vm = OrganizerEventsViewModel(handle, repo)
        vm.select(EventStatus.Cancelled)
        vm.select(EventStatus.Cancelled) // the active one again: nothing
        assertEquals(listOf(null, EventStatus.Cancelled), repo.statuses)
        assertEquals("CANCELLED", handle.get<String>("organizer.events.filter"))
    }

    @Test
    fun `an empty page is Empty and failed counts stay unknown`() = runTest {
        val repo = FakeRepo(mapOf(0 to page()), counts = Result.failure(ApiException(ApiError.Network)))
        val vm = OrganizerEventsViewModel(SavedStateHandle(), repo)
        assertEquals(UiState.Empty, vm.state.value)
        assertNull(vm.counts.value)
    }

    @Test
    fun `a silent refresh that fails keeps the list on screen`() = runTest {
        val repo = FakeRepo(mapOf(0 to page("a")))
        val vm = OrganizerEventsViewModel(SavedStateHandle(), repo)
        repo.pages = mapOf(0 to Result.failure(ApiException(ApiError.Network)))
        vm.refresh()
        assertEquals(listOf("a"), (vm.state.value as UiState.Success).data.map { it.id })
    }

    @Test
    fun `load more appends without duplicates`() = runTest {
        val repo = FakeRepo(mapOf(0 to page("a", "b", last = false), 1 to page("b", "c")))
        val vm = OrganizerEventsViewModel(SavedStateHandle(), repo)
        vm.loadMore()
        assertEquals(listOf("a", "b", "c"), (vm.state.value as UiState.Success).data.map { it.id })
    }

    @Test
    fun `the sales line counts capacity only when every type has one`() {
        assertEquals("40 of 100 sold", salesLine(event("a", sold = 40)))
        assertEquals("40 sold", salesLine(event("a", sold = 40, capacity = null)))
        assertEquals("No ticket types", salesLine(event("a").copy(ticketTypes = emptyList())))
    }
}
