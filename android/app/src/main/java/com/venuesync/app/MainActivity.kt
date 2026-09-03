package com.venuesync.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.venuesync.app.core.network.EventsApi
import com.venuesync.app.ui.navigation.VenueSyncNavHost
import com.venuesync.app.ui.theme.VenueSyncTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var eventsApi: EventsApi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // M0 smoke test ONLY — proves Ktor reaches the live backend. Delete this block
        // when EventsRepository + EventsViewModel take over (M1, Jul 26 session).
        lifecycleScope.launch {
            runCatching { eventsApi.publishedEventsRawJson() }
                .onSuccess { Log.d("VenueSyncM0", "published-events JSON:\n$it") }
                .onFailure { Log.e("VenueSyncM0", "published-events call FAILED", it) }
        }

        setContent {
            VenueSyncTheme {
                VenueSyncNavHost()
            }
        }
    }
}
