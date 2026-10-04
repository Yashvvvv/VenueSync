package com.venuesync.app.ui.organizer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.auth.ROLE_ORGANIZER
import com.venuesync.app.core.auth.SessionManager
import com.venuesync.app.core.auth.roles
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.toApiError
import com.venuesync.app.core.repository.OrganizerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpgradeState {
    data object Idle : UpgradeState
    data object Working : UpgradeState
    /** Organizer, and this phone's token says so: on to the organizer tools. */
    data object Done : UpgradeState
    /** Organizer on the server, but a new token couldn't be fetched (no signal): signing in again shows the tools. */
    data object SignInAgain : UpgradeState
    data class Failed(val error: ApiError) : UpgradeState
}

/**
 * The self-service upgrade. The server adds the organizer role (and keeps attendee), then the app fetches a new token
 * at once, because the roles live in the token and the old one would hide the organizer tools until it expired.
 */
@HiltViewModel
class BecomeOrganizerViewModel @Inject constructor(
    private val repository: OrganizerRepository,
    private val session: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<UpgradeState>(UpgradeState.Idle)
    val state: StateFlow<UpgradeState> = _state.asStateFlow()

    fun become() {
        val current = _state.value
        if (current == UpgradeState.Working || current == UpgradeState.Done || current == UpgradeState.SignInAgain) return
        _state.value = UpgradeState.Working // synchronous: a double tap can't send twice
        viewModelScope.launch {
            repository.becomeOrganizer().fold(
                onSuccess = {
                    val tokens = runCatching { session.renew() }.getOrNull()
                    _state.value = if (tokens != null && ROLE_ORGANIZER in tokens.roles()) UpgradeState.Done else UpgradeState.SignInAgain
                },
                onFailure = { _state.value = UpgradeState.Failed(it.toApiError()) },
            )
        }
    }
}
