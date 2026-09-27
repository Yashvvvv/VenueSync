package com.venuesync.app.ui.account

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.auth.Session
import com.venuesync.app.core.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionManager: SessionManager,
) : ViewModel() {

    val session: StateFlow<Session> = sessionManager.session

    fun signOut() {
        viewModelScope.launch {
            try {
                sessionManager.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Storage write failed: session stays SignedIn, so the UI keeps showing the account menu.
                Log.w("SessionViewModel", "Sign-out failed (${e.javaClass.simpleName})")
            }
        }
    }
}
