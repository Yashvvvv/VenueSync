package com.venuesync.app.auth

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.venuesync.app.core.auth.AuthTokens
import com.venuesync.app.core.auth.TokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/**
 * Tokens are encrypted with [KeystoreCipher] before they reach disk.
 * (EncryptedSharedPreferences is deprecated, hence DataStore + Keystore directly.)
 */
@Singleton
class KeystoreTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) : TokenStore {

    private val dataStore = context.sessionDataStore

    private val prefs: Flow<Preferences> = dataStore.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e // unreadable file → signed out
    }

    override val tokens: Flow<AuthTokens?> = prefs
        .map { it[TOKENS]?.let(::decodeOrNull) }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    override val forceLoginNext: Flow<Boolean> = prefs.map { it[FORCE_LOGIN] ?: false }.distinctUntilChanged()

    override suspend fun save(tokens: AuthTokens) {
        val blob = withContext(Dispatchers.IO) { KeystoreCipher.encrypt(Json.encodeToString(AuthTokens.serializer(), tokens).toByteArray()) }
        dataStore.edit { it[TOKENS] = blob }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(TOKENS) }
    }

    override suspend fun setForceLoginNext(value: Boolean) {
        dataStore.edit { it[FORCE_LOGIN] = value }
    }

    /**
     * Decryption fails when the Keystore key was wiped (lock-screen reset, OS restore) or the blob is
     * corrupt. The session is then simply gone: the user signs in again, the app never crashes.
     * The unreadable blob is overwritten by the next save() or removed by clear().
     */
    private fun decodeOrNull(blob: String): AuthTokens? = runCatching {
        Json.decodeFromString(AuthTokens.serializer(), String(KeystoreCipher.decrypt(blob), Charsets.UTF_8))
    }.onFailure { Log.w(TAG, "Stored session unreadable; treating as signed out (${it.javaClass.simpleName})") }
        .getOrNull()

    private companion object {
        const val TAG = "KeystoreTokenStore"
        val TOKENS = stringPreferencesKey("tokens")
        val FORCE_LOGIN = booleanPreferencesKey("force_login_next")
    }
}
