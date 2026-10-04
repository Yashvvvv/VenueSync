package com.venuesync.app.ui.organizer

import com.venuesync.app.core.auth.AuthApi
import com.venuesync.app.core.auth.AuthTokens
import com.venuesync.app.core.auth.FakeTokenStore
import com.venuesync.app.core.auth.ROLES_CLAIM
import com.venuesync.app.core.auth.SessionManager
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.ApiException
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BecomeOrganizerViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private fun b64(json: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
    private fun jwt(vararg roles: String) =
        "${b64("""{"alg":"RS256"}""")}.${b64("""{"$ROLES_CLAIM":[${roles.joinToString { "\"$it\"" }}]}""")}.sig"

    /** Auth0 answers a refresh with [newAccessToken], or can't be reached when it's null. */
    private fun auth(newAccessToken: String?) = AuthApi(
        AuthApi.createHttpClient(
            MockEngine {
                newAccessToken ?: throw IOException("offline")
                respond("""{"access_token":"$newAccessToken","refresh_token":"rt2"}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
        "https://tenant.example",
        "client-1",
    )

    private val attendee = AuthTokens(jwt("ROLE_ATTENDEE"), "rt1", null)

    /** The token renewal runs on Ktor's own threads, outside the test's virtual time: wait for it to land. */
    private suspend fun BecomeOrganizerViewModel.settled(): UpgradeState = withContext(Dispatchers.Default) {
        withTimeout(5_000) { state.first { it != UpgradeState.Working } }
    }

    @Test
    fun `upgrades, renews the token at once, and opens the organizer tools`() = runTest {
        val store = FakeTokenStore(attendee)
        val repo = FakeOrganizerRepository().apply { becomeOrganizer = Result.success(Unit) }
        val vm = BecomeOrganizerViewModel(repo, SessionManager(store, auth(jwt("ROLE_ATTENDEE", "ROLE_ORGANIZER")), backgroundScope))
        vm.become()
        assertEquals(UpgradeState.Done, vm.settled())
        assertEquals(jwt("ROLE_ATTENDEE", "ROLE_ORGANIZER"), store.tokens.value?.accessToken)
    }

    @Test
    fun `upgraded without signal for a new token says to sign in again`() = runTest {
        val repo = FakeOrganizerRepository().apply { becomeOrganizer = Result.success(Unit) }
        val vm = BecomeOrganizerViewModel(repo, SessionManager(FakeTokenStore(attendee), auth(null), backgroundScope))
        vm.become()
        assertEquals(UpgradeState.SignInAgain, vm.settled())
        vm.become() // already done on the server: nothing is sent again
        assertEquals(UpgradeState.SignInAgain, vm.state.value)
    }

    @Test
    fun `a refusal is kept and can be retried`() = runTest {
        val repo = FakeOrganizerRepository().apply { becomeOrganizer = Result.failure(ApiException(ApiError.Forbidden)) }
        val vm = BecomeOrganizerViewModel(repo, SessionManager(FakeTokenStore(attendee), auth(null), backgroundScope))
        vm.become()
        assertEquals(UpgradeState.Failed(ApiError.Forbidden), vm.settled())
        repo.becomeOrganizer = Result.success(Unit)
        vm.become()
        assertEquals(UpgradeState.SignInAgain, vm.settled())
    }
}
