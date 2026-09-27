package com.venuesync.app.di

import com.venuesync.app.auth.KeystoreTokenStore
import com.venuesync.app.core.auth.TokenStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {
    @Binds
    abstract fun bindTokenStore(impl: KeystoreTokenStore): TokenStore
}
