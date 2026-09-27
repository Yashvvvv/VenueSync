package com.venuesync.app.core.auth

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthTokensTest {

    private fun b64(json: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())
    private fun jwt(payload: String) = AuthTokens("${b64("""{"alg":"RS256"}""")}.${b64(payload)}.sig", null, null)

    @Test
    fun `reads roles from the namespaced claim`() {
        val token = jwt("""{"sub":"u1","$ROLES_CLAIM":["ROLE_ATTENDEE","ROLE_STAFF"]}""")
        assertEquals(setOf("ROLE_ATTENDEE", "ROLE_STAFF"), token.roles())
    }

    @Test
    fun `opaque token (audience missing) has no roles`() {
        assertEquals(emptySet<String>(), AuthTokens("opaque-token-without-dots", null, null).roles())
    }

    @Test
    fun `broken payload has no roles`() {
        assertEquals(emptySet<String>(), AuthTokens("a.%%%not-base64%%%.c", null, null).roles())
        assertEquals(emptySet<String>(), jwt("not json").roles())
    }

    @Test
    fun `claim of the wrong shape has no roles`() {
        assertEquals(emptySet<String>(), jwt("""{"$ROLES_CLAIM":"ROLE_ATTENDEE"}""").roles())
        assertEquals(setOf("ROLE_ATTENDEE"), jwt("""{"$ROLES_CLAIM":["ROLE_ATTENDEE",42,null]}""").roles())
    }

    @Test
    fun `missing claim has no roles`() {
        assertEquals(emptySet<String>(), jwt("""{"sub":"u1"}""").roles())
    }
}
