package com.venuesync.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.venuesync.app.ui.navigation.VenueSyncNavHost
import com.venuesync.app.ui.theme.VenueSyncTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VenueSyncTheme {
                VenueSyncNavHost()
            }
        }
    }
}
