package com.venuesync.app.core.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthApiTest {

    private val requests = mutableListOf<HttpRequestData>()

    private fun api(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): AuthApi {
        val engine = MockEngine { request -> requests += request; handler(request) }
        return AuthApi(AuthApi.createHttpClient(engine), "https://tenant.example/", "client-1")
    }

    private fun MockRequestHandleScope.json(status: HttpStatusCode, body: String) =
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))

    @Test
    fun `refresh posts the refresh grant as a form and returns rotated tokens`() = runTest {
        val result = api { json(HttpStatusCode.OK, """{"access_token":"at2","refresh_token":"rt2","expires_in":86400}""") }
            .refresh("rt1")

        assertEquals(RefreshResult.Success(TokenResponseDto("at2", "rt2", null)), result)
        val request = requests.single()
        assertEquals("https://tenant.example/oauth/token", request.url.toString())
        val form = (request.body as FormDataContent).formData
        assertEquals("refresh_token", form["grant_type"])
        assertEquals("client-1", form["client_id"])
        assertEquals("rt1", form["refresh_token"])
    }

    @Test
    fun `invalid_grant statuses mean Rejected`() = runTest {
        for (status in listOf(HttpStatusCode.Forbidden, HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized)) {
            val result = api { json(status, """{"error":"invalid_grant"}""") }.refresh("rt1")
            assertEquals("status $status", RefreshResult.Rejected, result)
        }
    }

    @Test
    fun `transient failures mean Unavailable, never Rejected`() = runTest {
        assertEquals(RefreshResult.Unavailable, api { json(HttpStatusCode.ServiceUnavailable, "") }.refresh("rt"))
        assertEquals(RefreshResult.Unavailable, api { json(HttpStatusCode.TooManyRequests, "") }.refresh("rt"))
        assertEquals(RefreshResult.Unavailable, api { throw IOException("offline") }.refresh("rt"))
        assertEquals(RefreshResult.Unavailable, api { json(HttpStatusCode.OK, """{"access_token":""") }.refresh("rt"))
        assertEquals(RefreshResult.Unavailable, api { json(HttpStatusCode.OK, """{"token_type":"Bearer"}""") }.refresh("rt"))
    }

    @Test
    fun `revoke sends client id and token as JSON`() = runTest {
        api { json(HttpStatusCode.OK, "") }.revoke("rt1")
        val request = requests.single()
        assertEquals("https://tenant.example/oauth/revoke", request.url.toString())
        val body = (request.body as TextContent).text
        assertTrue(body, body.contains("\"client_id\":\"client-1\"") && body.contains("\"token\":\"rt1\""))
    }

    @Test
    fun `revoke failures are swallowed`() = runTest {
        api { json(HttpStatusCode.InternalServerError, "") }.revoke("rt1")
        api { throw IOException("offline") }.revoke("rt1")
    }
}
