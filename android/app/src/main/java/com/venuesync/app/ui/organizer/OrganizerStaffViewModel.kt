package com.venuesync.app.ui.organizer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.StaffInvite
import com.venuesync.app.core.model.StaffMember
import com.venuesync.app.core.model.toApiError
import com.venuesync.app.core.repository.OrganizerRepository
import com.venuesync.app.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StaffUi(
    /** For the share text; null until the event loads (or if it can't), and then the text does without it. */
    val eventName: String? = null,
    val roster: UiState<List<StaffMember>> = UiState.Loading,
    /** The code just created, kept until the organizer creates another or leaves. */
    val invite: StaffInvite? = null,
    val inviting: Boolean = false,
    /** The member being removed, so only their row shows progress. */
    val removing: String? = null,
    /** The last invite or removal that failed. */
    val error: ApiError? = null,
)

/**
 * One event's door team: who can scan, one-time invite codes to add someone, and removal. Codes are made by the
 * server (one person, this event only, 7 days); the app only shows and shares them.
 */
@HiltViewModel
class OrganizerStaffViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val repository: OrganizerRepository,
) : ViewModel() {

    private val eventId: String? = handle[OrganizerEventViewModel.EVENT_ID_ARG]

    // The code survives rotation and process death: it's shown once, and making another just to see it is waste.
    private val _ui = MutableStateFlow(
        StaffUi(
            invite = handle.get<String>(INVITE_CODE)?.let {
                StaffInvite(it, handle.get<String>(INVITE_EXPIRES)?.let { at -> runCatching { LocalDateTime.parse(at) }.getOrNull() })
            },
        ),
    )
    val ui: StateFlow<StaffUi> = _ui.asStateFlow()

    init {
        load(silent = false)
        eventId?.let { id ->
            viewModelScope.launch { repository.event(id).onSuccess { e -> _ui.update { it.copy(eventName = e.name) } } }
        }
    }

    fun retry() = load(silent = false)

    fun dismissError() = _ui.update { it.copy(error = null) }

    fun createInvite() {
        val id = eventId ?: return
        if (_ui.value.inviting) return
        _ui.update { it.copy(inviting = true, error = null) }
        viewModelScope.launch {
            repository.createInvite(id).fold(
                onSuccess = { invite ->
                    handle[INVITE_CODE] = invite.code
                    handle[INVITE_EXPIRES] = invite.expiresAt?.toString()
                    _ui.update { it.copy(inviting = false, invite = invite) }
                },
                onFailure = { e -> _ui.update { it.copy(inviting = false, error = e.toApiError()) } },
            )
        }
    }

    fun remove(member: StaffMember) {
        val id = eventId ?: return
        if (_ui.value.removing != null) return
        _ui.update { it.copy(removing = member.userId, error = null) }
        viewModelScope.launch {
            repository.removeStaff(id, member.userId).fold(
                onSuccess = {
                    _ui.update { ui ->
                        val left = (ui.roster as? UiState.Success)?.data.orEmpty().filterNot { it.userId == member.userId }
                        ui.copy(removing = null, roster = if (left.isEmpty()) UiState.Empty else UiState.Success(left))
                    }
                },
                onFailure = { e -> _ui.update { it.copy(removing = null, error = e.toApiError()) } },
            )
            load(silent = true) // the server's list is the truth (someone may have joined meanwhile)
        }
    }

    /** Back from sharing a code: whoever used it may be on the team now. */
    fun refresh() {
        if (_ui.value.roster !is UiState.Loading) load(silent = true)
    }

    private fun load(silent: Boolean) {
        val id = eventId ?: run {
            _ui.update { it.copy(roster = UiState.Error(ApiError.NotFound)) }
            return
        }
        if (!silent) _ui.update { it.copy(roster = UiState.Loading) }
        viewModelScope.launch {
            repository.staff(id).fold(
                onSuccess = { list -> _ui.update { it.copy(roster = if (list.isEmpty()) UiState.Empty else UiState.Success(list)) } },
                onFailure = { e ->
                    _ui.update { if (silent && it.roster is UiState.Success) it else it.copy(roster = UiState.Error(e.toApiError())) }
                },
            )
        }
    }

    private companion object {
        const val INVITE_CODE = "staff.invite.code"
        const val INVITE_EXPIRES = "staff.invite.expires"
    }
}

/** What the organizer sends: the link the website opens, the code for the app, and the rules that come with it. */
internal fun inviteMessage(invite: StaffInvite, eventName: String?, expires: String?): String {
    val what = eventName?.let { "the door team for $it" } ?: "an event's door team"
    return "You're invited to $what on VenueSync. Open ${inviteLink(invite.code)} or type ${invite.code} in the " +
        "VenueSync app under Scan tickets. The code works once" + (expires?.let { " and expires $it." } ?: ".")
}

internal fun inviteLink(code: String) = "https://venuesync.pages.dev/staff/join?code=${java.net.URLEncoder.encode(code, "UTF-8")}"
