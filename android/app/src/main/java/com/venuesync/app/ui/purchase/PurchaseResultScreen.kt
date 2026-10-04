package com.venuesync.app.ui.purchase

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.venuesync.app.R
import com.venuesync.app.ui.components.ConfettiOnce
import com.venuesync.app.ui.components.DisplayText
import com.venuesync.app.ui.components.StubButton
import com.venuesync.app.ui.theme.StubCard
import com.venuesync.app.ui.theme.Success

/**
 * Shown once a purchase succeeded. No auto-redirect: the web's old page bounced away mid-read. [onViewTicket] is null
 * when the route carries no usable ticket id; [onDone] goes back to the event.
 */
@Composable
fun PurchaseResultScreen(onDone: () -> Unit, onViewTicket: (() -> Unit)?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        StubCard(Modifier.align(Alignment.Center).safeDrawingPadding().padding(20.dp).fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(painterResource(R.drawable.ph_check_circle_fill), null, Modifier.size(30.dp), tint = Success)
                DisplayText(
                    "That is yours",
                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    Modifier.padding(top = 12.dp).semantics { heading() },
                )
                Text(
                    "The ticket is on your account with the code you scan at the door.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Column(Modifier.padding(top = 20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (onViewTicket != null) StubButton("View my ticket", onViewTicket, Modifier.fillMaxWidth())
                    StubButton("Back to event", onDone, Modifier.fillMaxWidth(), outlined = onViewTicket != null)
                }
            }
        }
        ConfettiOnce(Modifier.fillMaxSize())
    }
}
