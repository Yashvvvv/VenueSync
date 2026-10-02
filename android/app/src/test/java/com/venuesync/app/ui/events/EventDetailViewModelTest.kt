package com.venuesync.app.ui.events

import androidx.lifecycle.SavedStateHandle
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.EventDetail
import com.venuesync.app.core.repository.EventsRepository
import com.venuesync.app.ui.common.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModelTest {

    private class FakeRepo(private val results: ArrayDeque<Result<EventDetail>>) : EventsRepository {
        val requestedIds = mutableListOf<String>()
        override suspend fun getPublishedEvents(query: String?, page: Int) = error("not used")
        override suspend fun getPublishedEvent(id: String): Result<EventDetail> {
            requestedIds += id
            return results.removeFirst()
        }
    }

    private val detail = EventDetail("e1", "Show", null, null, null, emptyList())

    private fun vm(repo: FakeRepo, args: Map<String, Any?> = mapOf(EventDetailViewModel.EVENT_ID_ARG to "e1")) =
        EventDetailViewModel(SavedStateHandle(args), repo)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads the event whose id is in the route`() = runTest {
        val repo = FakeRepo(ArrayDeque(listOf(Result.success(detail))))
        val vm = vm(repo)
        assertEquals(UiState.Success(detail), vm.state.value)
        assertEquals(listOf("e1"), repo.requestedIds)
    }

    @Test
    fun `failure maps to Error`() = runTest {
        val repo = FakeRepo(ArrayDeque(listOf(Result.failure(ApiException(ApiError.NotFound)))))
        assertEquals(UiState.Error(ApiError.NotFound), vm(repo).state.value)
    }

    @Test
    fun `missing route arg shows NotFound without calling the network`() = runTest {
        val repo = FakeRepo(ArrayDeque())
        assertEquals(UiState.Error(ApiError.NotFound), vm(repo, args = emptyMap()).state.value)
        assertEquals(emptyList<String>(), repo.requestedIds)
    }

    @Test
    fun `retry after failure loads again`() = runTest {
        val repo = FakeRepo(
            ArrayDeque(listOf(Result.failure(ApiException(ApiError.Network)), Result.success(detail))),
        )
        val vm = vm(repo)
        assertEquals(UiState.Error(ApiError.Network), vm.state.value)
        vm.retry()
        assertEquals(UiState.Success(detail), vm.state.value)
    }
}
