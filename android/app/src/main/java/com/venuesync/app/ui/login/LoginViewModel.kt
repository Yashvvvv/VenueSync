package com.venuesync.app.ui.login

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.auth.AuthFlow
import com.venuesync.app.auth.LoginFailure
import com.venuesync.app.auth.LoginResult
import com.venuesync.app.core.auth.Session
import com.venuesync.app.core.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Working : LoginUiState
    data class Error(val message: String) : LoginUiState
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authFlow: AuthFlow,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    val signedIn: StateFlow<Boolean> = session.session
        .map { it is Session.SignedIn }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** Null when a login is already running or Auth0 can't be reached (state says which). */
    suspend fun createLoginIntent(): Intent? {
        if (_state.value is LoginUiState.Working) return null // a double tap must not open two browser tabs
        _state.value = LoginUiState.Working
        return try {
            authFlow.loginIntent(forceLogin = session.shouldForceLogin())
        } catch (e: CancellationException) {
            _state.value = LoginUiState.Idle
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Couldn't start login (${e.javaClass.simpleName})")
            _state.value = LoginUiState.Error(LoginFailure.Network.message())
            null
        }
    }

    fun onResult(data: Intent?) {
        viewModelScope.launch {
            _state.value = when (val result = authFlow.completeLogin(data)) {
                is LoginResult.Success -> try {
                    session.signIn(result.tokens) // the screen leaves once `signedIn` flips
                    LoginUiState.Idle
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Couldn't store session (${e.javaClass.simpleName})")
                    LoginUiState.Error("Couldn't finish signing in. Please try again.")
                }
                LoginResult.Cancelled -> LoginUiState.Idle
                is LoginResult.Failed -> LoginUiState.Error(result.reason.message())
            }
        }
    }

    private fun LoginFailure.message(): String = when (this) {
        LoginFailure.Network -> "No connection. Check your network and try again."
        LoginFailure.DeviceClock -> "Your phone's date and time look wrong. Correct them in Settings, then try again."
        LoginFailure.Rejected -> "Sign-in failed. Please try again."
    }

    private companion object {
        const val TAG = "LoginViewModel"
    }
}
