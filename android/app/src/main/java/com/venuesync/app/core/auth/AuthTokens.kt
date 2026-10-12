package com.venuesync.app.core.auth

import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/** Namespaced claim written by the Auth0 Post-Login Action; must match the backend's Auth0Claims.ROLES. */
const val ROLES_CLAIM = "https://venuesync.app/roles"

/** Required by the backend to buy tickets and see your own. */
const val ROLE_ATTENDEE = "ROLE_ATTENDEE"

/** Creates and runs events (granted by the self-upgrade, on top of ATTENDEE). */
const val ROLE_ORGANIZER = "ROLE_ORGANIZER"

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String?,
    val idToken: String?,
)

/**
 * Roles for UI decisions only (show/hide the staff scanner). The signature is NOT verified here —
 * the server verifies every token, so this must never gate anything security-relevant.
 *
 * An opaque (non-JWT) access token yields no roles. That is also the symptom of a login that
 * forgot the `audience` parameter.
 */
fun AuthTokens.roles(): Set<String> {
    val roles = claims()?.get(ROLES_CLAIM) as? JsonArray ?: return emptySet()
    return roles.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }.toSet()
}

/**
 * Whose token this is (`sub`), to keep one account's saved data from another's on this phone. Unverified, like
 * [roles]: never an authorization decision. Null for an opaque token.
 */
fun AuthTokens.subject(): String? = (claims()?.get("sub") as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun AuthTokens.claims(): JsonObject? = runCatching {
    val parts = accessToken.split('.')
    if (parts.size != 3) return null
    Json.parseToJsonElement(String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)).jsonObject
}.getOrNull()
