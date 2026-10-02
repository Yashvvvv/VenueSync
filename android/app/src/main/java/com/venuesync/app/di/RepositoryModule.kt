package com.venuesync.app.di

import com.venuesync.app.core.repository.EventsRepository
import com.venuesync.app.core.repository.EventsRepositoryImpl
import com.venuesync.app.core.repository.TicketsRepository
import com.venuesync.app.core.repository.TicketsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindEventsRepository(impl: EventsRepositoryImpl): EventsRepository

    @Binds
    @Singleton
    abstract fun bindTicketsRepository(impl: TicketsRepositoryImpl): TicketsRepository
}
