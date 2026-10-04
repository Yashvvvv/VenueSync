package com.venuesync.app.ui.organizer

import androidx.lifecycle.SavedStateHandle
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.EventStatus
import com.venuesync.app.core.model.firstInvalidField
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventFormViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    /** What the system hands a new process: the same keys and values. */
    private fun SavedStateHandle.afterProcessDeath() = SavedStateHandle(keys().associateWith { get<Any>(it) })

    private fun EventFormViewModel.fill() = edit {
        it.copy(
            name = "Night Market",
            venue = "Phoenix Hall, Pune",
            start = "2026-11-14T19:00",
            end = "2026-11-14T23:00",
            ticketTypes = listOf(it.ticketTypes.single().copy(name = "General", price = "12.50", capacity = "200")),
        )
    }

    @Test
    fun `a blank form is marked on the phone, nothing is sent`() = runTest {
        val repo = FakeOrganizerRepository()
        val vm = EventFormViewModel(SavedStateHandle(), repo)
        assertTrue(vm.creating)
        assertFalse(vm.ui.value.dirty)
        vm.save()
        assertEquals(ApiError.Invalid("name"), vm.ui.value.error)
        assertTrue(repo.creates.isEmpty())
    }

    @Test
    fun `create sends the typed values and ends on the new event`() = runTest {
        val repo = FakeOrganizerRepository().apply { create = { _, _ -> Result.success(organizerEvent()) } }
        val vm = EventFormViewModel(SavedStateHandle(), repo)
        vm.fill()
        assertTrue(vm.ui.value.dirty)
        vm.save()
        val draft = repo.creates.single().first
        assertEquals(BigDecimal("12.50"), draft.ticketTypes.single().price)
        assertEquals(200, draft.ticketTypes.single().capacity)
        assertEquals(EventStatus.Draft, draft.status)
        assertEquals(14, draft.start!!.dayOfMonth)
        assertEquals(EVENT_ID, vm.ui.value.savedId)
    }

    @Test
    fun `an unanswered create locks the form and retries with the same key`() = runTest {
        val repo = FakeOrganizerRepository().apply { create = { _, _ -> Result.failure(ApiException(ApiError.Network)) } }
        val vm = EventFormViewModel(SavedStateHandle(), repo)
        vm.fill()
        vm.save()
        assertTrue(vm.ui.value.unconfirmed)
        vm.edit { it.copy(name = "Changed after the attempt") } // would be lost to the replay: refused
        assertEquals("Night Market", vm.ui.value.form!!.name)
        vm.save()
        assertEquals(2, repo.creates.size)
        assertEquals(repo.creates[0].second, repo.creates[1].second)
    }

    @Test
    fun `a refused create spends the key and marks the server's field`() = runTest {
        val repo = FakeOrganizerRepository().apply {
            create = { _, _ -> Result.failure(ApiException(ApiError.Invalid("ticketTypes[0].totalAvailable"))) }
        }
        val vm = EventFormViewModel(SavedStateHandle(), repo)
        vm.fill()
        vm.save()
        assertEquals(ApiError.Invalid("ticketTypes[0].capacity"), vm.ui.value.error)
        assertFalse(vm.ui.value.unconfirmed)
        vm.save()
        assertNotEquals(repo.creates[0].second, repo.creates[1].second)
    }

    @Test
    fun `the form and an unconfirmed save survive process death`() = runTest {
        val handle = SavedStateHandle()
        val repo = FakeOrganizerRepository().apply { create = { _, _ -> Result.failure(ApiException(ApiError.Network)) } }
        EventFormViewModel(handle, repo).apply { fill(); save() }

        val restored = EventFormViewModel(handle.afterProcessDeath(), repo)
        assertEquals("Night Market", restored.ui.value.form!!.name)
        assertTrue(restored.ui.value.dirty)
        assertTrue(restored.ui.value.unconfirmed)
        restored.save()
        assertEquals(repo.creates[0].second, repo.creates[1].second)
    }

    @Test
    fun `a typed form without a save survives process death unchanged`() = runTest {
        val handle = SavedStateHandle()
        val vm = EventFormViewModel(handle, FakeOrganizerRepository())
        vm.fill()
        val restored = EventFormViewModel(handle.afterProcessDeath(), FakeOrganizerRepository())
        assertEquals(vm.ui.value.form, restored.ui.value.form) // ticket type keys included
        assertFalse(restored.ui.value.unconfirmed)
    }

    @Test
    fun `edit loads the event and sends it back with its ids`() = runTest {
        val repo = FakeOrganizerRepository().apply {
            event = Result.success(organizerEvent(EventStatus.Published, sold = 5))
            update = { _, _ -> Result.success(organizerEvent(EventStatus.Published, sold = 5)) }
        }
        val vm = EventFormViewModel(SavedStateHandle(mapOf(OrganizerEventViewModel.EVENT_ID_ARG to EVENT_ID)), repo)
        assertFalse(vm.creating)
        assertEquals("25", vm.ui.value.form!!.ticketTypes.single().price)
        assertFalse(vm.ui.value.dirty)
        vm.edit { it.copy(name = "Night Market II") }
        vm.save()
        val (id, draft) = repo.updates.single()
        assertEquals(EVENT_ID, id)
        assertEquals(TYPE_ID, draft.ticketTypes.single().id)
        assertEquals(3L, draft.version) // the copy the form loaded, so a stale save is refused
        assertEquals(EventStatus.Published, draft.status)
        assertEquals(EVENT_ID, vm.ui.value.savedId)
    }

    @Test
    fun `unreadable numbers land on their field`() {
        val type = TicketTypeForm(name = "GA", price = "12,50")
        val form = EventForm(name = "Night Market", venue = "Hall", ticketTypes = listOf(type))
        assertEquals("ticketTypes[0].price", form.toDraft().firstInvalidField(creating = true))
        val capped = form.copy(ticketTypes = listOf(type.copy(price = "12.5", capacity = "3", sold = 5)))
        assertEquals("ticketTypes[0].capacity", capped.toDraft().firstInvalidField(creating = true))
        assertEquals("Can't be lower than the 5 already issued.", fieldMessage("ticketTypes[0].capacity", capped))
        assertEquals(null, capped.copy(ticketTypes = listOf(type.copy(price = "0", capacity = ""))).toDraft().firstInvalidField(creating = true))
    }

    @Test
    fun `the sales window message names the rule that broke`() {
        val form = EventForm(salesStart = "2026-11-10T10:00", salesEnd = "2026-11-09T10:00")
        assertEquals("Sales have to close after they open.", fieldMessage("salesEnd", form))
        assertEquals("Sales can't close after the event ends.", fieldMessage("salesEnd", form.copy(salesStart = null)))
    }
}
