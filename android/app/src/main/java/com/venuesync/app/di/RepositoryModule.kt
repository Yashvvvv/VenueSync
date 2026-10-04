package com.venuesync.app.di

import com.venuesync.app.core.repository.EventsRepository
import com.venuesync.app.core.repository.EventsRepositoryImpl
import com.venuesync.app.core.repository.StaffRepository
import com.venuesync.app.core.repository.OrganizerRepository
import com.venuesync.app.core.repository.OrganizerRepositoryImpl
import com.venuesync.app.core.repository.StaffRepositoryImpl
import com.venuesync.app.core.repository.TicketsRepository
import com.venuesync.app.core.repository.TicketsRepositoryImpl
import com.venuesync.app.core.repository.ValidationRepository
import com.venuesync.app.core.repository.ValidationRepositoryImpl
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

    @Binds
    @Singleton
    abstract fun bindValidationRepository(impl: ValidationRepositoryImpl): ValidationRepository

    @Binds
    @Singleton
    abstract fun bindStaffRepository(impl: StaffRepositoryImpl): StaffRepository

    @Binds
    @Singleton
    abstract fun bindOrganizerRepository(impl: OrganizerRepositoryImpl): OrganizerRepository
}
