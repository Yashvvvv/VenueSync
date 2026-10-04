package com.venuesync.app.ui.organizer

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.R
import com.venuesync.app.core.model.StaffInvite
import com.venuesync.app.core.model.StaffMember
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.components.DisplayText
import com.venuesync.app.ui.components.ErrorState
import com.venuesync.app.ui.components.EyebrowText
import com.venuesync.app.ui.components.MetaText
import com.venuesync.app.ui.components.StubButton
import com.venuesync.app.ui.components.StubEmptyState
import com.venuesync.app.ui.components.TicketWhen
import com.venuesync.app.ui.events.message
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.Num
import com.venuesync.app.ui.theme.StubCard

/** An event's door team: an invite code to hand to one person, and who can scan now. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizerStaffScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrganizerStaffViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var confirmRemove by rememberSaveable { mutableStateOf<String?>(null) } // a userId

    // Back from the share sheet or another app: the person may have used the code already.
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { DisplayText("Door staff", MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ph_arrow_left), contentDescription = "Back to the event")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            InviteSection(ui, onCreate = viewModel::createInvite)
            ui.error?.let { error ->
                Row(
                    Modifier.padding(top = 16.dp).fillMaxWidth()
                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.05f), MaterialTheme.shapes.medium)
                        .padding(start = 16.dp)
                        .semantics { liveRegion = LiveRegionMode.Assertive },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(error.message(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    IconButton(onClick = viewModel::dismissError) {
                        Icon(painterResource(R.drawable.ph_x_bold), contentDescription = "Dismiss")
                    }
                }
            }

            EyebrowText("Can scan now", Modifier.padding(top = 32.dp, bottom = 12.dp).semantics { heading() })
            when (val roster = ui.roster) {
                UiState.Loading -> CircularProgressIndicator(Modifier.padding(16.dp).size(24.dp))
                UiState.Empty -> StubEmptyState(
                    R.drawable.ph_users_three,
                    "No door staff yet",
                    "Create a code above and send it to someone you trust with the door. You can always scan this event yourself.",
                )
                is UiState.Error -> ErrorState(roster.error, viewModel::retry)
                is UiState.Success -> Column(verticalArrangement = Arrangement.spacedBy(if (LocalExperience.current.hardShadow) 16.dp else 10.dp)) {
                    roster.data.forEach { member ->
                        StaffRow(member, removing = ui.removing == member.userId, enabled = ui.removing == null) {
                            confirmRemove = member.userId
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    val member = (ui.roster as? UiState.Success)?.data?.firstOrNull { it.userId == confirmRemove }
    if (member != null) {
        AlertDialog(
            onDismissRequest = { confirmRemove = null },
            title = { Text("Remove ${member.name}?") },
            text = { Text("They can't scan tickets for this event any more. Guests they already let in stay checked in.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = null
                    viewModel.remove(member)
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = null }) { Text("Keep") } },
        )
    }
}

@Composable
private fun InviteSection(ui: StaffUi, onCreate: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    EyebrowText("Invite someone", Modifier.padding(top = 8.dp).semantics { heading() })
    Text(
        "Each code works once, for this event only, and expires after 7 days. Send it to one person: they open the " +
            "link, or type the code in the VenueSync app under Scan tickets.",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
    ui.invite?.let { invite ->
        val expires = invite.expiresAt?.format(TicketWhen)
        StubCard(Modifier.padding(top = 16.dp).fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Spelled out for TalkBack, which reads "K7Q2M" as a word otherwise.
                Text(
                    invite.code,
                    style = Num.copy(fontSize = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.12.em),
                    color = colors.primary,
                    modifier = Modifier.semantics { contentDescription = "Invite code ${invite.code.toList().joinToString(" ")}" },
                )
                expires?.let { MetaText("Expires $it", Modifier.padding(top = 8.dp)) }
                Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StubButton("Share", { shareInvite(context, invite, ui.eventName, expires) }, icon = R.drawable.ph_share_network_bold)
                    StubButton("Copy code", { clipboard.setText(AnnotatedString(invite.code)) }, outlined = true, icon = R.drawable.ph_copy)
                }
            }
        }
    }
    StubButton(
        when {
            ui.inviting -> "Creating a code"
            ui.invite != null -> "Create another code"
            else -> "Create invite code"
        },
        onCreate,
        Modifier.padding(top = 16.dp).fillMaxWidth().height(48.dp),
        enabled = !ui.inviting,
        outlined = ui.invite != null,
        icon = R.drawable.ph_plus_bold,
    )
}

private fun shareInvite(context: android.content.Context, invite: StaffInvite, eventName: String?, expires: String?) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, inviteMessage(invite, eventName, expires))
    }
    context.startActivity(Intent.createChooser(send, "Send the invite"))
}

@Composable
private fun StaffRow(member: StaffMember, removing: Boolean, enabled: Boolean, onRemove: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth()
            .border(LocalExperience.current.border, colors.outlineVariant, MaterialTheme.shapes.medium)
            .padding(start = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
            Text(member.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            member.email?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (removing) {
            CircularProgressIndicator(Modifier.padding(14.dp).size(20.dp), strokeWidth = 2.dp)
        } else {
            IconButton(onClick = onRemove, enabled = enabled) {
                Icon(painterResource(R.drawable.ph_trash), contentDescription = "Remove ${member.name}", Modifier.size(18.dp))
            }
        }
    }
}
