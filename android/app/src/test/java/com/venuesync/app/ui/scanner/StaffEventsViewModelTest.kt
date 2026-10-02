package com.venuesync.app.ui.scanner

import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.Event
import com.venuesync.app.core.model.Guest
import com.venuesync.app.core.model.JoinedEvent
import com.venuesync.app.core.repository.StaffRepository
import com.venuesync.app.ui.common.UiState
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
class StaffEventsViewModelTest {

    private class FakeStaff : StaffRepository {
        var events = listOf<Event>()
        var loads = 0
        val accepted = mutableListOf<String>()
        var acceptResult: Result<JoinedEvent> = Result.success(JoinedEvent("e1", "Summer Vibes"))
        override suspend fun staffingEvents(): Result<List<Event>> {
            loads++
            return Result.success(events)
        }
        override suspend fun acceptInvite(code: String): Result<JoinedEvent> {
            accepted += code
            return acceptResult
        }
        override suspend fun searchGuests(eventId: String, query: String): Result<List<Guest>> = error("not used")
    }

    private val repo = FakeStaff()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `no events yet is Empty`() = runTest {
        assertEquals(UiState.Empty, StaffEventsViewModel(repo).state.value)
    }

    @Test
    fun `a malformed code is refused without asking the server`() = runTest {
        val vm = StaffEventsViewModel(repo)
        vm.join("hello")
        assertTrue(vm.join.value is JoinState.Failed)
        assertTrue(repo.accepted.isEmpty())
    }

    @Test
    fun `joining opens that event once and reloads the list`() = runTest {
        val vm = StaffEventsViewModel(repo)
        vm.join("K7Q2M-9XH4P")
        assertEquals(JoinState.Joined(JoinedEvent("e1", "Summer Vibes")), vm.join.value)
        assertEquals(2, repo.loads)
        vm.onJoinedShown()
        assertEquals(JoinState.Idle, vm.join.value)
    }

    @Test
    fun `a code someone else used gets the app's own wording`() = runTest {
        repo.acceptResult = Result.failure(ApiException(ApiError.Conflict))
        val vm = StaffEventsViewModel(repo)
        vm.join("K7Q2M-9XH4P")
        assertEquals(
            JoinState.Failed("Someone else already used this code. Ask the organizer for a new one."),
            vm.join.value,
        )
    }
}
