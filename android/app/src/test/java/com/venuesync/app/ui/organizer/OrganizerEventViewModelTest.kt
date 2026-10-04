package com.venuesync.app.ui.organizer

import androidx.lifecycle.SavedStateHandle
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.EventStatus
import com.venuesync.app.core.model.Refusal
import com.venuesync.app.ui.common.UiState
import java.time.LocalDateTime
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
class OrganizerEventViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(repo: FakeOrganizerRepository, id: String? = EVENT_ID) =
        OrganizerEventViewModel(SavedStateHandle(mapOf(OrganizerEventViewModel.EVENT_ID_ARG to id)), repo)

    @Test
    fun `a malformed route is not found, never a crash`() = runTest {
        val vm = vm(FakeOrganizerRepository(), id = null)
        assertEquals(UiState.Error(ApiError.NotFound), vm.state.value)
    }

    @Test
    fun `publish resends the server's latest copy with the new status`() = runTest {
        val repo = FakeOrganizerRepository().apply {
            event = Result.success(organizerEvent())
            update = { _, draft -> Result.success(organizerEvent(status = draft.status)) }
        }
        val vm = vm(repo)
        vm.perform(EventAction.Publish)
        assertEquals(2, repo.eventCalls) // the load, then a fresh read before the write
        assertEquals(EventStatus.Published, repo.updates.single().second.status)
        assertEquals("General", repo.updates.single().second.ticketTypes.single().name)
        assertEquals(EventStatus.Published, (vm.state.value as UiState.Success).data.status)
        assertEquals(ActionState.Idle, vm.action.value)
    }

    @Test
    fun `a refusal is reported and the event reloaded`() = runTest {
        val repo = FakeOrganizerRepository().apply {
            event = Result.success(organizerEvent(EventStatus.Published, sold = 3))
            update = { _, _ -> Result.failure(ApiException(ApiError.Refused(Refusal.StatusChange))) }
        }
        val vm = vm(repo)
        vm.perform(EventAction.Unpublish)
        assertEquals(ActionState.Failed(EventAction.Unpublish, ApiError.Refused(Refusal.StatusChange)), vm.action.value)
        assertEquals(3, repo.eventCalls)
        vm.dismissError()
        assertEquals(ActionState.Idle, vm.action.value)
    }

    @Test
    fun `delete ends on Deleted so the screen leaves`() = runTest {
        val repo = FakeOrganizerRepository().apply {
            event = Result.success(organizerEvent())
            delete = Result.success(Unit)
        }
        val vm = vm(repo)
        vm.perform(EventAction.Delete)
        assertEquals(ActionState.Deleted, vm.action.value)
        vm.perform(EventAction.Publish) // nothing left to act on
        assertTrue(repo.updates.isEmpty())
    }

    @Test
    fun `only the moves the server allows are offered`() {
        assertEquals(listOf(EventAction.Publish, EventAction.Delete), organizerEvent(EventStatus.Draft).actions())
        assertEquals(listOf(EventAction.Unpublish, EventAction.Cancel), organizerEvent(EventStatus.Published).actions())
        assertEquals(listOf(EventAction.Cancel), organizerEvent(EventStatus.Published, sold = 1).actions())
        assertEquals(emptyList<EventAction>(), organizerEvent(EventStatus.Cancelled).actions())
        assertEquals(false, organizerEvent(EventStatus.Completed).editable())
    }

    @Test
    fun `cancel says what happens to issued tickets`() {
        assertTrue("12 tickets" in confirmCopy(EventAction.Cancel, organizerEvent(EventStatus.Published, sold = 12)).body)
        assertTrue("tickets" !in confirmCopy(EventAction.Cancel, organizerEvent(EventStatus.Published)).body)
    }

    @Test
    fun `schedule lines read like the server's rules`() {
        val start = LocalDateTime.of(2026, 11, 14, 19, 0)
        assertEquals("Sat 14 Nov 2026 · 19:00 to 23:00", whenLine(start, start.plusHours(4)))
        assertEquals("Sat 14 Nov 2026 · 19:00 to Sun 15 Nov 2026 · 02:00", whenLine(start, start.plusHours(7)))
        assertEquals(null, whenLine(null, start))
        assertEquals("From publishing until the event ends", salesWindowLine(null, null))
        assertEquals("From publishing until Sat 14 Nov 2026 · 19:00", salesWindowLine(null, start))
    }
}
