package com.venuesync.app.ui.organizer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.R
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.ui.components.DisplayText
import com.venuesync.app.ui.components.EyebrowText
import com.venuesync.app.ui.components.StubButton
import com.venuesync.app.ui.events.message
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.displaySection

/** What becoming an organizer gives and costs, then one button. On success the organizer tools open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BecomeOrganizerScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BecomeOrganizerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val working = state == UpgradeState.Working
    LaunchedEffect(state) { if (state == UpgradeState.Done) onDone() }
    // Mid-request the answer must land here: leaving would hide whether it worked.
    BackHandler(enabled = working) {}

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !working) {
                        Icon(painterResource(R.drawable.ph_arrow_left), contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            EyebrowText("For organizers")
            DisplayText("Run your own events", LocalExperience.current.displaySection(), Modifier.padding(top = 12.dp).semantics { heading() })
            Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Point(R.drawable.ph_calendar_blank_fill, "Create events and put tickets on sale, from this phone or the website.")
                Point(R.drawable.ph_ticket_fill, "See what sold, per ticket type, as it happens.")
                Point(R.drawable.ph_users_three, "Invite door staff and scan tickets at your events.")
                Point(R.drawable.ph_user_circle, "Your account stays yours: your tickets stay, and you can still buy more.")
            }
            Text(
                "There's no fee and nothing to fill in. The organizer tools then live under Manage events in the account menu.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )

            when (val s = state) {
                is UpgradeState.Failed -> Notice(failureMessage(s.error))
                UpgradeState.SignInAgain -> Notice(
                    "You're an organizer now. This phone couldn't fetch your new access, so sign out and in again to see " +
                        "Manage events.",
                )
                else -> Unit
            }
            StubButton(
                if (working) "Setting up" else "Become an organizer",
                viewModel::become,
                Modifier.padding(top = 24.dp).fillMaxWidth().height(52.dp),
                enabled = !working && state != UpgradeState.SignInAgain,
            )
            if (working) LinearProgressIndicator(Modifier.padding(top = 8.dp).fillMaxWidth())
        }
    }
}

@Composable
private fun Point(icon: Int, text: String) {
    Row {
        Icon(painterResource(icon), null, Modifier.padding(top = 2.dp).size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun Notice(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(top = 24.dp).semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

/** An account the server won't upgrade (no attendee role) gets told why, not "Your account can't do this". */
internal fun failureMessage(error: ApiError): String = when (error) {
    ApiError.Forbidden -> "This account can't become an organizer from the app. Sign out and in again, or contact support."
    is ApiError.Server -> "We couldn't set that up just now. Nothing changed on your account. Try again in a minute."
    else -> error.message()
}
