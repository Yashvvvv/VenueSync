package com.venuesync.app.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.R
import com.venuesync.app.core.auth.Session
import com.venuesync.app.ui.experience.ExperienceMenuItems

/**
 * Top-bar account entry. "Sign in" shows once storage says signed out (never flashed before it's read). The menu is
 * always there, signed in or not, because it also holds the view switch.
 */
@Composable
fun AccountAction(
    onSignInClick: () -> Unit,
    onMyTicketsClick: () -> Unit,
    onScanClick: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (session == Session.SignedOut) TextButton(onClick = onSignInClick) { Text("Sign in") }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(painterResource(R.drawable.ph_user_circle), contentDescription = "Account and view")
            }
            // A menu, not a bare button: one stray tap must not sign someone out.
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                val close = { menuOpen = false }
                if (session is Session.SignedIn) {
                    DropdownMenuItem(text = { Text("My tickets") }, onClick = { close(); onMyTicketsClick() })
                    // Anyone signed in: the next screen lists only events they can scan (organized or staffed via an
                    // invite), and the server checks every scan. No Auth0 role decides this.
                    DropdownMenuItem(text = { Text("Scan tickets") }, onClick = { close(); onScanClick() })
                    HorizontalDivider()
                }
                ExperienceMenuItems(onPicked = close)
                if (session is Session.SignedIn) {
                    HorizontalDivider()
                    DropdownMenuItem(text = { Text("Sign out") }, onClick = { close(); viewModel.signOut() })
                }
            }
        }
    }
}
