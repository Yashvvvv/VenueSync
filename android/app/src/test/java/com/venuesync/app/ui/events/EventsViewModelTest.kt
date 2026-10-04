package com.venuesync.app.ui.events

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.Event
import com.venuesync.app.core.repository.EventPage
import com.venuesync.app.core.repository.EventsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.venuesync.app.ui.common.UiState

@OptIn(ExperimentalCoroutinesApi::class)
class EventsViewModelTest {

    private class FakeRepo(private val pages: Map<Int, Result<EventPage>>) : EventsRepository {
        val queries = mutableListOf<String?>()
        val sizes = mutableListOf<Int>()
        override suspend fun getPublishedEvents(query: String?, page: Int, size: Int): Result<EventPage> {
            queries += query
            sizes += size
            return pages[page] ?: error("unexpected page $page")
        }

        override suspend fun getPublishedEvent(id: String) = error("not used")
    }

    private fun event(id: String) = Event(id, "Event $id", null, null, null)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `typing a word sends one search after the pause, not one per letter`() = runTest {
        val repo = FakeRepo(mapOf(0 to Result.success(EventPage(listOf(event("a")), isLast = true))))
        val vm = EventsViewModel(SavedStateHandle(), repo)
        "summer".indices.forEach { vm.onQueryChanged("summer".take(it + 1)) } // as fast as anyone types
        vm.onQueryChanged("summer ") // a trailing space is the same search
        advanceTimeBy(399)
        assertEquals(listOf<String?>(null), repo.queries) // only the initial load so far

        advanceUntilIdle()
        assertEquals(listOf(null, "summer"), repo.queries)
    }

    @Test
    fun `the feed's page size reaches the server, and the last page is reported`() = runTest {
        val repo = FakeRepo(
            mapOf(
                0 to Result.success(EventPage(listOf(event("a")), isLast = false)),
                1 to Result.success(EventPage(listOf(event("b")), isLast = true)),
            ),
        )
        val vm = EventsViewModel(SavedStateHandle(mapOf(EventsViewModel.PAGE_SIZE_ARG to 8)), repo)
        assertEquals(false, vm.endReached.value)
        vm.loadMore()
        advanceUntilIdle()
        assertEquals(listOf(8, 8), repo.sizes)
        assertTrue(vm.endReached.value)
    }

    @Test
    fun `lists without a route size use the default`() = runTest {
        val repo = FakeRepo(mapOf(0 to Result.success(EventPage(listOf(event("a")), isLast = true))))
        EventsViewModel(SavedStateHandle(), repo)
        assertEquals(listOf(20), repo.sizes)
    }

    @Test
    fun `a submitted search runs at once, trimmed`() = runTest {
        val repo = FakeRepo(mapOf(0 to Result.success(EventPage(listOf(event("a")), isLast = true))))
        val vm = EventsViewModel(SavedStateHandle(), repo)
        vm.submit("  jazz ")
        assertEquals(listOf(null, "jazz"), repo.queries) // no typing pause to wait out
        assertEquals("jazz", vm.query.value)
    }

    @Test
    fun `empty first page emits Empty`() = runTest {
        val vm = EventsViewModel(SavedStateHandle(), FakeRepo(mapOf(0 to Result.success(EventPage(emptyList(), isLast = true)))))
        assertEquals(UiState.Empty, vm.state.value)
    }

    @Test
    fun `failure maps to Error with ApiError`() = runTest {
        val vm = EventsViewModel(SavedStateHandle(), FakeRepo(mapOf(0 to Result.failure(ApiException(ApiError.Network)))))
        assertEquals(UiState.Error(ApiError.Network), vm.state.value)
    }

    @Test
    fun `loadMore appends next page and stops at last`() = runTest {
        val repo = FakeRepo(
            mapOf(
                0 to Result.success(EventPage(listOf(event("a")), isLast = false)),
                1 to Result.success(EventPage(listOf(event("b")), isLast = true)),
            ),
        )
        val vm = EventsViewModel(SavedStateHandle(), repo)
        vm.state.test {
            assertEquals(listOf("a"), (awaitItem() as UiState.Success).data.map { it.id })
            vm.loadMore()
            assertEquals(listOf("a", "b"), (awaitItem() as UiState.Success).data.map { it.id })
            vm.loadMore() // isLast → no request, no emission (FakeRepo would throw on page 2)
            expectNoEvents()
        }
        assertTrue(vm.state.value is UiState.Success)
    }
}
