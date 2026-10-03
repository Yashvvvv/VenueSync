package com.venuesync.app.ui.experience

import com.venuesync.app.ui.theme.Experience
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExperienceStoreTest {

    @get:Rule val tmp = TemporaryFolder()

    @Test fun `parse covers absent, both values and junk`() {
        assertEquals(ExperienceChoice(Experience.Classic, chosen = false), parseExperience(null))
        assertEquals(ExperienceChoice(Experience.Hype, chosen = true), parseExperience("hype"))
        assertEquals(ExperienceChoice(Experience.Classic, chosen = true), parseExperience("classic"))
        assertEquals(ExperienceChoice(Experience.Classic, chosen = true), parseExperience("bold"))
    }

    @Test fun `a choice survives a new store, as after process death`() = runBlocking {
        val file = File(tmp.root, "experience.preferences_pb")
        val first = open(file)
        assertEquals(ExperienceChoice(Experience.Classic, chosen = false), first.store.choice.filterNotNull().first())
        first.store.choose(Experience.Hype).join()
        assertEquals(Experience.Hype, first.store.choice.value?.experience) // applied at once too
        first.close() // one DataStore per file at a time

        val second = open(file)
        assertEquals(ExperienceChoice(Experience.Hype, chosen = true), second.store.choice.filterNotNull().first())
        second.close()
    }

    @Test fun `a corrupt file shows the chooser again instead of crashing`() = runBlocking {
        val file = File(tmp.root, "experience.preferences_pb").apply { writeBytes(byteArrayOf(0x7f, 0x01, 0x02, 0x03)) }
        val opened = open(file)
        assertEquals(ExperienceChoice(Experience.Classic, chosen = false), opened.store.choice.filterNotNull().first())
        opened.close()
    }

    private class Opened(val store: ExperienceStore, private val scope: CoroutineScope) {
        suspend fun close() = scope.coroutineContext[Job]!!.cancelAndJoin()
    }

    private fun open(file: File): Opened {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return Opened(ExperienceStore(experienceDataStore(scope) { file }, scope), scope)
    }
}
