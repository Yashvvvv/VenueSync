package com.venuesync.app.di

import com.venuesync.app.BuildConfig
import com.venuesync.app.core.network.EventsApi
import com.venuesync.app.core.network.VenueSyncHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

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
    fun provideHttpClient(): HttpClient =
        VenueSyncHttpClient.create(BuildConfig.API_BASE_URL)

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
