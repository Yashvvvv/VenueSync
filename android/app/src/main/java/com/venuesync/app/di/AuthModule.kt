package com.venuesync.app.di

import com.venuesync.app.BuildConfig
import com.venuesync.app.auth.KeystoreTokenStore
import com.venuesync.app.core.auth.AuthApi
import com.venuesync.app.core.auth.TokenStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    abstract fun bindTokenStore(impl: KeystoreTokenStore): TokenStore

    companion object {
        @Provides
        @Singleton
        fun provideAuthApi(): AuthApi =
            AuthApi(AuthApi.createHttpClient(), BuildConfig.OIDC_AUTHORITY, BuildConfig.OIDC_CLIENT_ID)
    }
}
