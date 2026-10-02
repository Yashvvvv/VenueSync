package com.venuesync.app.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
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
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
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
 * Tokens are AES-256-GCM encrypted with a key generated inside the Android Keystore. The key
 * never leaves secure hardware, so a copied or rooted data folder only yields ciphertext.
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
        val blob = withContext(Dispatchers.IO) { encrypt(Json.encodeToString(AuthTokens.serializer(), tokens).toByteArray()) }
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
        Json.decodeFromString(AuthTokens.serializer(), String(decrypt(blob), Charsets.UTF_8))
    }.onFailure { Log.w(TAG, "Stored session unreadable; treating as signed out (${it.javaClass.simpleName})") }
        .getOrNull()

    @Synchronized // two threads must never generate two different keys
    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()
    }

    /** Stored as base64(IV || ciphertext+tag); the Keystore picks a fresh random IV per encryption. */
    private fun encrypt(plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.getEncoder().encodeToString(cipher.iv + cipher.doFinal(plain))
    }

    private fun decrypt(blob: String): ByteArray {
        val bytes = Base64.getDecoder().decode(blob)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_SIZE))
        }
        return cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE)
    }

    private companion object {
        const val TAG = "KeystoreTokenStore"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "venuesync_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TAG_BITS = 128
        val TOKENS = stringPreferencesKey("tokens")
        val FORCE_LOGIN = booleanPreferencesKey("force_login_next")
    }
}
