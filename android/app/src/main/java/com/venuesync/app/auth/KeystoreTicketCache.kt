package com.venuesync.app.auth

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.venuesync.app.core.auth.Session
import com.venuesync.app.core.auth.SessionManager
import com.venuesync.app.core.auth.subject
import com.venuesync.app.core.repository.CachedTicket
import com.venuesync.app.core.repository.TicketCache
import com.venuesync.app.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.offlineTicketsDataStore: DataStore<Preferences> by preferencesDataStore(name = "offline_tickets")

/** A saved QR code is a bearer credential, so it's encrypted like the session tokens. Backup is off for all data. */
@Singleton
class KeystoreTicketCache @Inject constructor(
    @ApplicationContext context: Context,
    private val sessionManager: SessionManager,
    @ApplicationScope scope: CoroutineScope,
) : TicketCache {

    private val dataStore = context.offlineTicketsDataStore

    init {
        // Every way a session ends (sign-out, refresh rejected, no refresh token) lands here.
        scope.launch { sessionManager.session.filterIsInstance<Session.SignedOut>().collect { clear() } }
    }

    override suspend fun read(): List<CachedTicket> {
        if (sessionManager.session.first { it != Session.Unknown } !is Session.SignedIn) return emptyList()
        val saved = dataStore.data.catch { if (it is IOException) emit(emptyPreferences()) else throw it }.first()
        // The sign-out clear above runs asynchronously; a phone that died in between must still never show one
        // account's tickets to the next. Saved by someone else (or by a version that didn't record it) = nothing saved.
        val owner = owner() ?: return emptyList()
        if (saved[OWNER] != owner) return emptyList()
        val blob = saved[TICKETS] ?: return emptyList()
        return withContext(Dispatchers.IO) {
            runCatching { json.decodeFromString(Serializer, String(KeystoreCipher.decrypt(blob), Charsets.UTF_8)) }
                .onFailure { Log.w(TAG, "Saved tickets unreadable; treating as none (${it.javaClass.simpleName})") }
                .getOrDefault(emptyList())
        }
    }

    override suspend fun write(tickets: List<CachedTicket>) {
        val owner = owner() ?: return
        val blob = withContext(Dispatchers.IO) { KeystoreCipher.encrypt(json.encodeToString(Serializer, tickets).toByteArray()) }
        // Checked inside the edit: edits are serialized, so this either runs before the sign-out clear or sees SignedOut.
        dataStore.edit {
            if (sessionManager.session.value is Session.SignedIn) {
                it[TICKETS] = blob
                it[OWNER] = owner
            }
        }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.remove(TICKETS)
            it.remove(OWNER)
        }
    }

    /** The signed-in account, as its token names it. */
    private suspend fun owner(): String? = sessionManager.currentTokens()?.subject()

    private companion object {
        const val TAG = "KeystoreTicketCache"
        val TICKETS = stringPreferencesKey("tickets")
        /** Whose tickets these are (the token's `sub`). */
        val OWNER = stringPreferencesKey("owner")
        val Serializer = ListSerializer(CachedTicket.serializer())
        val json = Json { ignoreUnknownKeys = true }
    }
}
