package com.venuesync.app.ui.account

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.core.auth.Session

/** Top-bar account entry: nothing while unknown, "Sign in" when signed out, a menu when signed in. */
@Composable
fun AccountAction(
    onSignInClick: () -> Unit,
    onMyTicketsClick: () -> Unit,
    viewModel: SessionViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    when (session) {
        Session.Unknown -> Unit // don't flash "Sign in" before storage has been read
        Session.SignedOut -> TextButton(onClick = onSignInClick) { Text("Sign in") }
        is Session.SignedIn -> {
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Account")
                }
                // A menu, not a bare button: one stray tap must not sign someone out.
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("My tickets") },
                        onClick = {
                            menuOpen = false
                            onMyTicketsClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Sign out") },
                        onClick = {
                            menuOpen = false
                            viewModel.signOut()
                        },
                    )
                }
            }
        }
    }
}
