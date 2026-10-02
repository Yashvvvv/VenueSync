package com.venuesync.app.ui.purchase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Shown once a purchase succeeded. [onViewTicket] is null when the route carries no usable ticket id. */
@Composable
fun PurchaseResultScreen(onDone: () -> Unit, onViewTicket: (() -> Unit)?, modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "You're in!",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )
            Text("Your ticket is booked.", style = MaterialTheme.typography.bodyLarge)
            if (onViewTicket != null) {
                Button(onClick = onViewTicket, modifier = Modifier.padding(top = 12.dp)) { Text("View ticket") }
                TextButton(onClick = onDone) { Text("Done") }
            } else {
                Button(onClick = onDone, modifier = Modifier.padding(top = 12.dp)) { Text("Done") }
            }
        }
    }
}
