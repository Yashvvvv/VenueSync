package com.venuesync.app

import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.ui.experience.ChooserScreen
import com.venuesync.app.ui.experience.ExperienceChoice
import com.venuesync.app.ui.experience.ExperienceStore
import com.venuesync.app.ui.experience.LocalChooseExperience
import com.venuesync.app.ui.navigation.VenueSyncNavHost
import com.venuesync.app.ui.theme.Experience
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.halftone
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import com.venuesync.app.ui.theme.VenueSyncTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var experienceStore: ExperienceStore

    /** Set once a composition using the stored choice has been applied; until then the first frame is held. */
    @Volatile private var choiceComposed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        holdFirstFrameUntilChoiceIsComposed()
        val choose: (Experience) -> Unit = { experienceStore.choose(it) }
        setContent {
            val stored by experienceStore.choice.collectAsStateWithLifecycle()
            // Null only if storage took over a second: Classic, and no chooser over content already on screen.
            val choice = stored ?: ExperienceChoice(Experience.Classic, chosen = true)
            val dark = isSystemInDarkTheme()
            SideEffect { if (stored != null) choiceComposed = true }
            LaunchedEffect(choice.experience, dark) { applyWindowFor(choice.experience, dark) }

            CompositionLocalProvider(LocalChooseExperience provides choose) {
                VenueSyncTheme(experience = choice.experience, darkTheme = dark) {
                    if (!choice.chosen) {
                        ChooserScreen(onChoose = choose)
                    } else {
                        // A switch starts the other experience at its home, like the web going to "/". Same key
                        // after process death, so the back stack is restored.
                        key(choice.experience) {
                            // Hype's halftone screen sits over the whole tree, like the web's fixed overlay.
                            val texture = if (LocalExperience.current.halftone) Modifier.halftone() else Modifier
                            Box(texture) { VenueSyncNavHost(choice.experience) }
                        }
                    }
                }
            }
        }
    }

    /**
     * No flash of the wrong experience: the launch window stays up until the stored choice is on screen. A plain
     * pre-draw hold (what the splash screen library does), capped so a stuck read can never block the app.
     */
    private fun holdFirstFrameUntilChoiceIsComposed() {
        val content = findViewById<View>(android.R.id.content)
        val giveUpAt = SystemClock.uptimeMillis() + 1_000
        content.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                val ready = choiceComposed || SystemClock.uptimeMillis() > giveUpAt
                if (ready) content.viewTreeObserver.removeOnPreDrawListener(this)
                return ready
            }
        })
    }

    private fun applyWindowFor(experience: Experience, dark: Boolean) {
        // Hype is black stock whatever the system mode, so its bar icons are always light.
        val bars = if (experience == Experience.Hype || dark) {
            SystemBarStyle.dark(Color.TRANSPARENT)
        } else {
            SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        }
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        // The system draws the next cold start's launch window before any app code runs; on API 33+ it can be told
        // ahead of time. ponytail: below 33, a light-mode Hype cold start shows paper for the launch window only.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            splashScreen.setSplashScreenTheme(
                if (experience == Experience.Hype) R.style.Theme_VenueSync_Hype else Resources.ID_NULL,
            )
        }
    }
}
