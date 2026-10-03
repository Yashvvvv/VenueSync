package com.venuesync.app.ui.tickets

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.BitmapFactory
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.venuesync.app.core.model.Ticket
import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.events.Centered
import com.venuesync.app.ui.events.DateFormat
import com.venuesync.app.ui.events.message
import com.venuesync.app.ui.events.priceLabel
import com.venuesync.app.ui.theme.Mono
import com.venuesync.app.ui.theme.Perforation
import com.venuesync.app.ui.theme.StubCard
import com.venuesync.app.ui.theme.TicketShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TicketDetailViewModel = hiltViewModel(),
) {
    val ticket by viewModel.ticket.collectAsStateWithLifecycle()
    val qr by viewModel.qr.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Ticket") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val s = ticket) {
                UiState.Loading -> Centered { CircularProgressIndicator() }
                UiState.Empty -> Centered { Text("Not found.") }
                is UiState.Error -> Centered {
                    Text(s.error.message(), style = MaterialTheme.typography.bodyLarge)
                    Button(shape = MaterialTheme.shapes.small, onClick = viewModel::retryTicket, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
                }
                is UiState.Success -> Column {
                    s.data.savedAt?.let { OfflineBanner(it, onRetry = viewModel::retryTicket) }
                    TicketContent(s.data, qr, onRetryQr = viewModel::retryQr)
                }
            }
        }
    }
}

/** One ticket stub: the event above the perforation, the code below it. */
@Composable
private fun TicketContent(ticket: Ticket, qr: QrState, onRetryQr: () -> Unit) {
    // The holes sit on the perforation, whose height depends on the event block; plain corners until it's placed.
    var perforationAt by remember { mutableStateOf<Dp?>(null) }
    val density = LocalDensity.current
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        StubCard(
            shape = perforationAt?.let { TicketShape(it, vertical = false) } ?: MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
        ) {
            EventBlock(ticket)
            Perforation(
                vertical = false,
                modifier = Modifier.fillMaxWidth().height(1.dp).onPlaced {
                    perforationAt = with(density) { it.positionInParent().y.toDp() } + 0.5.dp
                },
            )
            CodeBlock(ticket, qr, onRetryQr)
        }
    }
}

@Composable
private fun EventBlock(ticket: Ticket) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            ticket.eventName,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        ticket.venue?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = muted, textAlign = TextAlign.Center) }
        ticket.eventStart?.let { start ->
            val range = ticket.eventEnd?.takeIf { it.isAfter(start) }
                ?.let { "${start.format(DateFormat)} – ${it.format(DateFormat)}" }
                ?: start.format(DateFormat)
            Text(range, style = MaterialTheme.typography.bodyMedium, fontFamily = Mono, color = muted, textAlign = TextAlign.Center)
        }
        Text(
            buildAnnotatedString {
                append("1 × ${ticket.ticketTypeName}")
                ticket.price?.let {
                    append(" · ")
                    withStyle(SpanStyle(fontFamily = Mono)) { append(priceLabel(it)) }
                }
            },
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CodeBlock(ticket: Ticket, qr: QrState, onRetryQr: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (qr) {
            QrState.Hidden -> Text(
                unusableMessage(ticket.status),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            QrState.Loading -> Box(Modifier.size(QrSize), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is QrState.Error -> QrError(qr.error.message(), onRetryQr)
            is QrState.Ready -> {
                // A null decode (bytes passed the PNG check but aren't a readable image) shows an error, never crashes.
                val image = remember(qr) { BitmapFactory.decodeByteArray(qr.png, 0, qr.png.size)?.asImageBitmap() }
                if (image == null) {
                    QrError("We couldn't show the code.", onRetryQr)
                } else {
                    FullBrightness()
                    // White with padding in every theme: scanners need the contrast and the quiet zone.
                    Box(
                        modifier = Modifier.size(QrSize).clip(MaterialTheme.shapes.medium).background(Color.White).padding(16.dp),
                    ) {
                        Image(
                            image,
                            contentDescription = "Ticket QR code",
                            filterQuality = FilterQuality.None, // keep the modules' edges sharp when scaled
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Text(
                        "Show this code at the entrance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        // The fallback when the QR code won't scan (cracked screen, glare) or won't even load: staff type this.
        if (qr != QrState.Hidden) {
            Text(
                "TICKET CODE",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = Mono,
                letterSpacing = 0.1.em,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp).clearAndSetSemantics {},
            )
            Text(
                ticket.code,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = Mono,
                letterSpacing = 3.sp,
                modifier = Modifier.semantics { contentDescription = "Ticket code ${ticket.code.toList().joinToString(" ")}" },
            )
        }
    }
}

@Composable
private fun QrError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Button(shape = MaterialTheme.shapes.small, onClick = onRetry, modifier = Modifier.padding(top = 12.dp)) { Text("Retry") }
    }
}

/** Full screen brightness while the code is on screen, so scanners read it; the previous value comes back on leave. */
@Composable
private fun FullBrightness() {
    val activity = LocalContext.current.findActivity() ?: return
    DisposableEffect(activity) {
        val window = activity.window
        val previous = window.attributes.screenBrightness
        window.attributes = window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL }
        onDispose { window.attributes = window.attributes.apply { screenBrightness = previous } }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun unusableMessage(status: TicketStatus) = when (status) {
    TicketStatus.Used -> "This ticket was used."
    TicketStatus.Expired -> "This ticket has expired."
    TicketStatus.Cancelled -> "This ticket was cancelled."
    TicketStatus.Purchased, TicketStatus.Unknown -> "" // never Hidden: these always load the code
}

private val QrSize = 264.dp
