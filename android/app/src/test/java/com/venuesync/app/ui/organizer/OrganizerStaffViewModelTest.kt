package com.venuesync.app.ui.organizer

import androidx.lifecycle.SavedStateHandle
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.StaffInvite
import com.venuesync.app.core.model.StaffMember
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrganizerStaffViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val ana = StaffMember("a1", "Ana", "ana@example.com")
    private val ben = StaffMember("b2", "Ben", null)
    private val code = StaffInvite("K7Q2M-9XH4P", LocalDateTime.of(2026, 10, 11, 21, 30))

    private fun repo() = FakeOrganizerRepository().apply {
        event = Result.success(organizerEvent())
        staff = Result.success(listOf(ana, ben))
    }

    private fun vm(repo: FakeOrganizerRepository, handle: SavedStateHandle = SavedStateHandle(mapOf(OrganizerEventViewModel.EVENT_ID_ARG to EVENT_ID))) =
        OrganizerStaffViewModel(handle, repo)

    @Test
    fun `loads the team and the event's name`() = runTest {
        val vm = vm(repo())
        assertEquals(listOf(ana, ben), (vm.ui.value.roster as UiState.Success).data)
        assertEquals("Night Market", vm.ui.value.eventName)
    }

    @Test
    fun `nobody on the team is an empty state, not an error`() = runTest {
        val vm = vm(repo().apply { staff = Result.success(emptyList()) })
        assertEquals(UiState.Empty, vm.ui.value.roster)
    }

    @Test
    fun `a new code is kept through process death`() = runTest {
        val handle = SavedStateHandle(mapOf(OrganizerEventViewModel.EVENT_ID_ARG to EVENT_ID))
        val repo = repo().apply { invite = Result.success(code) }
        vm(repo, handle).createInvite()
        val restored = vm(repo, SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) }))
        assertEquals(code, restored.ui.value.invite)
    }

    @Test
    fun `a failed invite says why and can be tried again`() = runTest {
        val vm = vm(repo().apply { invite = Result.failure(ApiException(ApiError.Network)) })
        vm.createInvite()
        assertEquals(ApiError.Network, vm.ui.value.error)
        assertFalse(vm.ui.value.inviting)
        vm.dismissError()
        assertEquals(null, vm.ui.value.error)
    }

    @Test
    fun `removing someone takes them off the list`() = runTest {
        val repo = repo().apply { removeStaff = Result.success(Unit) }
        val vm = vm(repo)
        repo.staff = Result.success(listOf(ben)) // what the server says afterwards
        vm.remove(ana)
        assertEquals(listOf("a1"), repo.removed)
        assertEquals(listOf(ben), (vm.ui.value.roster as UiState.Success).data)
        assertEquals(null, vm.ui.value.removing)
    }

    @Test
    fun `the message carries the link, the code and the rules`() {
        val text = inviteMessage(code, "Night Market", "Sun 11 Oct 2026 · 21:30")
        assertTrue("https://venuesync.pages.dev/staff/join?code=K7Q2M-9XH4P" in text)
        assertTrue("type K7Q2M-9XH4P in the VenueSync app under Scan tickets" in text)
        assertTrue("the door team for Night Market" in text)
        assertTrue("expires Sun 11 Oct 2026 · 21:30." in text)
        assertTrue("—" !in text)
    }
}
