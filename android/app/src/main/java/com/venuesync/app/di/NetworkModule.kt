package com.venuesync.app.di

import android.util.Log
import com.venuesync.app.BuildConfig
import com.venuesync.app.core.auth.SessionManager
import com.venuesync.app.core.network.EventsApi
import com.venuesync.app.core.network.VenueSyncHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Module
@InstallIn(SingletonComponent::class)
/**
 * Hilt module that provides the shared network dependencies used by the app.
 *
 * Keeping the `HttpClient` and API wrapper as singletons ensures the networking
 * stack is reused across the application lifecycle.
 */
object NetworkModule {

    /**
     * Creates the singleton Ktor [HttpClient] configured with the app's API base URL.
     *
     * @return a shared HTTP client instance for all network requests.
     */
    @Provides
    @Singleton
    fun provideHttpClient(
        session: SessionManager,
        @ApplicationScope scope: CoroutineScope,
    ): HttpClient {
        val client = VenueSyncHttpClient.create(
            BuildConfig.API_BASE_URL,
            enableLogging = BuildConfig.DEBUG,
            session = session,
        )
        // Ktor 3.0.3 caches the first loadTokens() result and has no cacheTokens switch. Drop that
        // cache on every sign-in/sign-out, or the previous user's token keeps being sent.
        scope.launch {
            session.session.drop(1).collect {
                if (BuildConfig.DEBUG) Log.d("Session", "Session is now $it") // e.g. SignedIn(roles=[ROLE_ATTENDEE])
                client.authProvider<BearerAuthProvider>()?.clearToken()
            }
        }
        return client
    }

    /**
     * Creates the singleton [EventsApi] wrapper backed by the shared [HttpClient].
     *
     * @param client the injected shared HTTP client.
     * @return an API client for event-related endpoints.
     */
    @Provides
    @Singleton
    fun provideEventsApi(client: HttpClient): EventsApi = EventsApi(client)
}
