package com.venuesync.app.core.auth

import java.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/** Namespaced claim written by the Auth0 Post-Login Action; must match the backend's Auth0Claims.ROLES. */
const val ROLES_CLAIM = "https://venuesync.app/roles"

/** Required by the backend to buy tickets and see your own. */
const val ROLE_ATTENDEE = "ROLE_ATTENDEE"

/** Required by the backend to validate tickets at the door. */
const val ROLE_STAFF = "ROLE_STAFF"

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
fun AuthTokens.roles(): Set<String> = runCatching {
    val parts = accessToken.split('.')
    if (parts.size != 3) return emptySet()
    val payload = String(Base64.getUrlDecoder().decode(parts[1]), Charsets.UTF_8)
    val roles = Json.parseToJsonElement(payload).jsonObject[ROLES_CLAIM] as? JsonArray ?: return emptySet()
    roles.mapNotNull { (it as? JsonPrimitive)?.takeIf { p -> p.isString }?.content }.toSet()
}.getOrDefault(emptySet())
