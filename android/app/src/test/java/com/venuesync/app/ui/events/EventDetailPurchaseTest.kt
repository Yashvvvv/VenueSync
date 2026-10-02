package com.venuesync.app.ui.events

import androidx.lifecycle.SavedStateHandle
import com.venuesync.app.core.auth.AuthApi
import com.venuesync.app.core.auth.AuthTokens
import com.venuesync.app.core.auth.FakeTokenStore
import com.venuesync.app.core.auth.ROLES_CLAIM
import com.venuesync.app.core.auth.SessionManager
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import com.venuesync.app.core.model.EventDetail
import com.venuesync.app.core.model.SalesStatus
import com.venuesync.app.core.model.Ticket
import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.core.model.TicketType
import com.venuesync.app.core.repository.EventsRepository
import com.venuesync.app.core.repository.TicketsRepository
import io.ktor.client.engine.mock.MockEngine
import java.math.BigDecimal
import java.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
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

/** The buy flow: key lifecycle, sign-in gating, double taps, process death. */
@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailPurchaseTest {

    private val type = TicketType("t1", "GA", BigDecimal("25.00"), null, soldOut = false)
    private val detail = EventDetail("e1", "Show", null, null, null, listOf(type), SalesStatus.OnSale, null, null)
    private val ticket = Ticket("tk1", TicketStatus.Purchased, "GA", BigDecimal("25.00"), "e1", "Show", null, null, null, null)

    private class Events(private val detail: EventDetail) : EventsRepository {
        var loads = 0
        override suspend fun getPublishedEvents(query: String?, page: Int) = error("not used")
        override suspend fun getPublishedEvent(id: String): Result<EventDetail> {
            loads++
            return Result.success(detail)
        }
    }

    /** Each purchase waits on a deferred the test completes, so "in flight" is a state the test can see. */
    private class Tickets : TicketsRepository {
        val keys = mutableListOf<String>()
        var pending = CompletableDeferred<Result<Ticket>>()
        override suspend fun purchase(eventId: String, ticketTypeId: String, idempotencyKey: String): Result<Ticket> {
            keys += idempotencyKey
            return pending.await().also { pending = CompletableDeferred() }
        }
    }

    private fun b64(json: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
    private fun tokens(vararg roles: String) = AuthTokens(
        "${b64("""{"alg":"RS256"}""")}.${b64("""{"$ROLES_CLAIM":[${roles.joinToString { "\"$it\"" }}]}""")}.sig",
        null,
        null,
    )
    private val attendee = tokens("ROLE_ATTENDEE")

    private val events = Events(detail)
    private val tickets = Tickets()
    private val handle = SavedStateHandle(mapOf(EventDetailViewModel.EVENT_ID_ARG to "e1"))

    private fun vm(signedIn: AuthTokens? = attendee, savedState: SavedStateHandle = handle): EventDetailViewModel {
        val auth = AuthApi(AuthApi.createHttpClient(MockEngine { error("not used") }), "https://tenant.example", "c")
        val session = SessionManager(FakeTokenStore(signedIn), auth, CoroutineScope(Dispatchers.Unconfined))
        return EventDetailViewModel(savedState, events, tickets, session)
    }

    private fun EventDetailViewModel.confirm() {
        buy(type)
        assertEquals(PurchaseState.Confirming(type), purchase.value)
        confirmPurchase()
    }

    private fun fail(error: ApiError) = tickets.pending.complete(Result.failure(ApiException(error)))

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `signed out - buy asks for sign-in and buys nothing`() = runTest {
        val vm = vm(signedIn = null)
        vm.buy(type)
        assertEquals(PurchaseState.SignInRequired, vm.purchase.value)
        vm.onSignInHandled()
        assertEquals(PurchaseState.Idle, vm.purchase.value) // back from login: the user must tap again
        assertTrue(tickets.keys.isEmpty())
    }

    @Test
    fun `no attendee role - buy shows the hint`() = runTest {
        val vm = vm(signedIn = tokens("ROLE_STAFF"))
        vm.buy(type)
        assertTrue(vm.purchase.value is PurchaseState.Failed)
        assertTrue(tickets.keys.isEmpty())
    }

    @Test
    fun `cancel at confirm sends nothing`() = runTest {
        val vm = vm()
        vm.buy(type)
        vm.dismissPurchase()
        assertEquals(PurchaseState.Idle, vm.purchase.value)
        assertTrue(tickets.keys.isEmpty())
    }

    @Test
    fun `double confirm sends one request and the dialog can't be dismissed meanwhile`() = runTest {
        val vm = vm()
        vm.confirm()
        vm.confirmPurchase()
        vm.dismissPurchase()
        assertEquals(PurchaseState.Purchasing(type), vm.purchase.value)
        assertEquals(1, tickets.keys.size)
    }

    @Test
    fun `success shows the result once, refreshes the event, then forgets the key`() = runTest {
        val vm = vm()
        vm.confirm()
        tickets.pending.complete(Result.success(ticket))
        assertEquals(PurchaseState.Purchased(ticket), vm.purchase.value)
        assertEquals(2, events.loads)

        vm.onPurchaseShown()
        assertEquals(PurchaseState.Idle, vm.purchase.value)
        vm.confirm() // the next purchase is a new one
        assertNotEquals(tickets.keys[0], tickets.keys[1])
    }

    @Test
    fun `retry after a network failure reuses the same key`() = runTest {
        val vm = vm()
        vm.confirm()
        fail(ApiError.Network)
        assertTrue(vm.purchase.value is PurchaseState.Retryable)
        vm.retryPurchase()
        assertEquals(2, tickets.keys.size)
        assertEquals(tickets.keys[0], tickets.keys[1])
    }

    @Test
    fun `cancelling a Retryable keeps the key for the next attempt`() = runTest {
        val vm = vm()
        vm.confirm()
        fail(ApiError.Server(null))
        vm.dismissPurchase()
        vm.confirm()
        assertEquals(tickets.keys[0], tickets.keys[1])
    }

    @Test
    fun `after process death the purchase comes back as Retryable with the same key`() = runTest {
        vm().confirm() // request in flight when the process dies; SavedStateHandle is what survives
        val restored = vm(savedState = SavedStateHandle(handle.keys().associateWith { handle.get<Any>(it) }))
        assertTrue(restored.purchase.value is PurchaseState.Retryable)
        restored.retryPurchase()
        assertEquals(tickets.keys[0], tickets.keys[1])
    }

    @Test
    fun `sold out shows a message, refreshes the event and spends the key`() = runTest {
        val vm = vm()
        vm.confirm()
        fail(ApiError.SoldOut)
        assertEquals(PurchaseState.Failed("Sold out. Someone got the last one."), vm.purchase.value)
        assertEquals(2, events.loads)
        vm.dismissPurchase()
        vm.confirm()
        assertNotEquals(tickets.keys[0], tickets.keys[1])
    }

    @Test
    fun `unclear outcome keeps the key so the next attempt can't buy twice`() = runTest {
        val vm = vm()
        vm.confirm()
        fail(ApiError.InvalidResponse)
        assertTrue(vm.purchase.value is PurchaseState.Failed)
        vm.dismissPurchase()
        vm.confirm()
        assertEquals(tickets.keys[0], tickets.keys[1])
    }
}
