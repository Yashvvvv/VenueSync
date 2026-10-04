package com.venuesync.app.ui.experience

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.venuesync.app.ui.theme.Experience
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** [chosen] is false until the person picks (or skips): only then does the first-run chooser go away. */
data class ExperienceChoice(val experience: Experience, val chosen: Boolean)

/** Same values as the web's `venuesync:audience`. Unknown text (a future value, a bad write) is Classic, chosen. */
internal fun parseExperience(raw: String?): ExperienceChoice = when (raw) {
    null -> ExperienceChoice(Experience.Classic, chosen = false)
    HYPE -> ExperienceChoice(Experience.Hype, chosen = true)
    else -> ExperienceChoice(Experience.Classic, chosen = true)
}

private fun Experience.wire() = if (this == Experience.Hype) HYPE else CLASSIC
private const val HYPE = "hype"
private const val CLASSIC = "classic"

/** A corrupt file reads as empty (the chooser shows again) instead of crashing every launch. */
internal fun experienceDataStore(scope: CoroutineScope, file: () -> File): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        scope = scope,
        produceFile = file,
    )

/** Which experience this phone shows. Not a secret, so plain DataStore; backup is off for all data anyway. */
class ExperienceStore(private val dataStore: DataStore<Preferences>, private val scope: CoroutineScope) {

    /** The last choice made in this process, so a failed write still switches the app for this session. */
    private val override = MutableStateFlow<ExperienceChoice?>(null)

    /** Null until the first read finishes: MainActivity holds the first frame until then, so nothing flashes. */
    val choice: StateFlow<ExperienceChoice?> = combine(
        dataStore.data
            .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
            .map { parseExperience(it[KEY]) },
        override,
    ) { stored, latest -> latest ?: stored }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** Applies at once, then persists in the app scope: the screen that asked is torn down by the switch itself. */
    fun choose(experience: Experience): Job {
        override.value = ExperienceChoice(experience, chosen = true)
        return scope.launch {
            try {
                dataStore.edit { it[KEY] = experience.wire() }
            } catch (e: IOException) {
                Log.w("ExperienceStore", "Choice not saved; applies until the app closes (${e.javaClass.simpleName})")
            }
        }
    }

    private companion object {
        val KEY = stringPreferencesKey("experience")
    }
}
