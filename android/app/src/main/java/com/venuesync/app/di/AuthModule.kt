package com.venuesync.app.di

import com.venuesync.app.BuildConfig
import com.venuesync.app.auth.KeystoreTokenStore
import com.venuesync.app.core.auth.AuthApi
import com.venuesync.app.core.auth.SessionManager
import com.venuesync.app.core.auth.TokenStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Lives as long as the process; for work that must outlive any screen (session state, token cache sync). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    abstract fun bindTokenStore(impl: KeystoreTokenStore): TokenStore

    companion object {
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        @Singleton
        fun provideAuthApi(): AuthApi =
            AuthApi(AuthApi.createHttpClient(), BuildConfig.OIDC_AUTHORITY, BuildConfig.OIDC_CLIENT_ID)

        // Constructed here rather than via @Inject so core/auth stays free of Hilt qualifiers.
        @Provides
        @Singleton
        fun provideSessionManager(
            store: TokenStore,
            api: AuthApi,
            @ApplicationScope scope: CoroutineScope,
        ): SessionManager = SessionManager(store, api, scope)
    }
}
