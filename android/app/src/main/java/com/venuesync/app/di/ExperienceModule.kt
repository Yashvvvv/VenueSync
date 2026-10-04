package com.venuesync.app.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import com.venuesync.app.ui.experience.ExperienceStore
import com.venuesync.app.ui.experience.experienceDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
object ExperienceModule {
    @Provides
    @Singleton
    fun provideExperienceStore(@ApplicationContext context: Context, @ApplicationScope scope: CoroutineScope) =
        ExperienceStore(experienceDataStore(scope) { context.preferencesDataStoreFile("experience") }, scope)
}
