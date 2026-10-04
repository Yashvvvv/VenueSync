package com.venuesync.app.ui.events

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.venuesync.app.R
import com.venuesync.app.core.model.Availability
import com.venuesync.app.core.model.EventDetail
import com.venuesync.app.core.model.TicketType
import com.venuesync.app.core.model.availabilityOf
import com.venuesync.app.ui.common.UiState
import com.venuesync.app.ui.components.Clock
import com.venuesync.app.ui.components.DisplayText
import com.venuesync.app.ui.components.ErrorState
import com.venuesync.app.ui.components.EyebrowText
import com.venuesync.app.ui.components.HeroDay
import com.venuesync.app.ui.components.MetaText
import com.venuesync.app.ui.components.StubButton
import com.venuesync.app.ui.components.TicketWhen
import com.venuesync.app.ui.components.addToCalendar
import com.venuesync.app.ui.components.money
import com.venuesync.app.ui.components.shareEvent
import com.venuesync.app.ui.theme.Eyebrow
import com.venuesync.app.ui.theme.Experience
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.Num
import com.venuesync.app.ui.theme.Perforation
import com.venuesync.app.ui.theme.StubCard
import com.venuesync.app.ui.theme.Total
import com.venuesync.app.ui.theme.displayHero
import com.venuesync.app.ui.theme.displaySection
import com.venuesync.app.ui.theme.eventImageFor
import java.math.BigDecimal

@Composable
fun EventDetailScreen(
    onBack: () -> Unit,
    onSignInRequired: () -> Unit,
    onPurchased: (ticketId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EventDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val purchase by viewModel.purchase.collectAsStateWithLifecycle()
    val buyer by viewModel.buyer.collectAsStateWithLifecycle()

    // One-shot states: act, then tell the VM so it leaves the state. Recomposition or rotation can't
    // fire them twice, because by then the VM has already moved on.
    LaunchedEffect(purchase) {
        when (val p = purchase) {
            PurchaseState.SignInRequired -> {
                onSignInRequired()
                viewModel.onSignInHandled()
            }
            is PurchaseState.Purchased -> {
                onPurchased(p.ticket.id)
                viewModel.onPurchaseShown()
            }
            else -> Unit
        }
    }

    // The confirm screen is a state of this route, not a route of its own: the purchase (and its idempotency key in
    // SavedStateHandle) stays with the one ViewModel that owns it, through process death too.
    val confirming = purchase is PurchaseState.Confirming || purchase is PurchaseState.Purchasing ||
        purchase is PurchaseState.Retryable || purchase is PurchaseState.Failed
    // While the request is in flight nothing may leave the screen: dismissPurchase ignores Purchasing.
    BackHandler(enabled = confirming) { viewModel.dismissPurchase() }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (confirming) {
            ConfirmContent(
                purchase = purchase,
                event = (state as? UiState.Success)?.data,
                onBack = viewModel::dismissPurchase,
                onConfirm = viewModel::confirmPurchase,
                onRetry = viewModel::retryPurchase,
            )
        } else {
            when (val s = state) {
                UiState.Loading -> HeroSkeleton()
                // A single event has no "empty" case: a missing one is Error(NotFound).
                UiState.Empty -> Unit
                is UiState.Error -> ErrorState(s.error, viewModel::retry, Modifier.align(Alignment.Center))
                is UiState.Success -> EventDetailContent(s.data, buyer, onBuy = viewModel::buy)
            }
            BackButton(onBack, Modifier.statusBarsPadding().padding(8.dp))
        }
    }
}

/** Floats over the photo, so it carries its own ground. */
@Composable
private fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    IconButton(
        onClick,
        modifier.clip(if (LocalExperience.current.experience == Experience.Hype) RectangleShape else CircleShape)
            .background(colors.background.copy(alpha = 0.7f)),
    ) {
        Icon(painterResource(R.drawable.ph_arrow_left), contentDescription = "Back", tint = colors.onSurface)
    }
}

@Composable
private fun heroHeight(): Dp = (LocalConfiguration.current.screenHeightDp * 0.46f).dp.coerceAtLeast(320.dp)

/** Moves content up over what's above it and gives the space back, like the web's `-mt-24`. */
private fun Modifier.pullUp(by: Dp) = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = by.roundToPx()
    layout(placeable.width, (placeable.height - shift).coerceAtLeast(0)) { placeable.place(0, -shift) }
}

@Composable
private fun HeroPhoto(eventId: String?) {
    val bg = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxWidth().height(heroHeight()).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        eventId?.let { AsyncImage(eventImageFor(it), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to bg.copy(alpha = 0.1f), 0.5f to bg.copy(alpha = 0.55f), 1f to bg)))
    }
}

@Composable
private fun HeroSkeleton() {
    Column(Modifier.semantics { liveRegion = LiveRegionMode.Polite }) { HeroPhoto(null) }
}

@Composable
private fun EventDetailContent(event: EventDetail, buyer: Buyer, onBuy: (TicketType) -> Unit) {
    val context = LocalContext.current
    val style = LocalExperience.current
    val colors = MaterialTheme.colorScheme
    LazyColumn {
        item {
            HeroPhoto(event.id)
            Column(Modifier.pullUp(96.dp).padding(horizontal = 20.dp)) {
                event.start?.let { EyebrowText(it.format(HeroDay), color = colors.primary) }
                DisplayText(event.name, style.displayHero(), Modifier.padding(top = 16.dp).semantics { heading() })
                Column(Modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    event.venue?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    event.start?.let { start ->
                        val time = event.end?.takeIf { it.isAfter(start) }?.let { "${start.format(Clock)} to ${it.format(Clock)}" }
                            ?: start.format(Clock)
                        Text(time, style = Num.copy(fontSize = 14.sp), color = colors.onSurfaceVariant)
                    }
                }
                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StubButton("Share", { shareEvent(context, event.id, event.name) }, outlined = true, icon = R.drawable.ph_share_network_bold)
                    event.start?.let { start ->
                        StubButton(
                            "Add to calendar",
                            { addToCalendar(context, event.name, event.venue, start, event.end) },
                            outlined = true,
                            icon = R.drawable.ph_calendar_blank_bold,
                        )
                    }
                }
            }
        }
        item { Details(event, Modifier.padding(horizontal = 20.dp, vertical = 28.dp)) }
        item { TicketSelector(event, buyer, onBuy, Modifier.padding(horizontal = 20.dp)) }
        item {
            Spacer(Modifier.height(32.dp))
            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

/** The details list: what you'd want written on the ticket, each with its glyph. */
@Composable
private fun Details(event: EventDetail, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HorizontalDivider(color = colors.outlineVariant, thickness = LocalExperience.current.border)
        event.start?.let { start ->
            val end = event.end?.takeIf { it.isAfter(start) }
            DetailRow(
                R.drawable.ph_calendar_blank_fill,
                "When",
                if (end != null && end.toLocalDate() != start.toLocalDate()) {
                    "${start.format(TicketWhen)} to ${end.format(TicketWhen)}"
                } else {
                    start.format(TicketWhen) + (end?.let { " to ${it.format(Clock)}" } ?: "")
                },
            )
        }
        event.venue?.let { DetailRow(R.drawable.ph_map_pin_fill, "Where", it) }
        HorizontalDivider(color = colors.outlineVariant, thickness = LocalExperience.current.border)
    }
}

@Composable
private fun DetailRow(icon: Int, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(painterResource(icon), null, Modifier.padding(top = 2.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.padding(start = 12.dp)) {
            MetaText(label)
            Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/**
 * The web's ticket selector: a real radio group of tiers, then a perforation, the total and one button. There is no
 * fee anywhere in the ticket model, so "nothing gets added" is a fact; if a fee is ever added, this copy must change.
 */
@Composable
private fun TicketSelector(event: EventDetail, buyer: Buyer, onBuy: (TicketType) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val style = LocalExperience.current
    val shape = MaterialTheme.shapes.medium
    var selectedId by rememberSaveable(event.id) {
        mutableStateOf(event.ticketTypes.firstOrNull { event.availabilityOf(it) == Availability.Buyable }?.id)
    }
    // A refresh can make the chosen tier unavailable (sold out): then nothing is selected, and there's no button.
    val selected = event.ticketTypes.firstOrNull { it.id == selectedId && event.availabilityOf(it) == Availability.Buyable }

    Column(modifier) {
        Text("CHOOSE A TICKET", style = Eyebrow, color = colors.onSurfaceVariant, modifier = Modifier.semantics { heading() })
        if (event.ticketTypes.isEmpty()) {
            Text("Tickets aren't on sale yet.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 16.dp))
            return@Column
        }
        Column(Modifier.padding(top = 16.dp).selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            event.ticketTypes.forEach { type ->
                val reason = availabilityLabel(event.availabilityOf(type))
                val isSelected = type.id == selected?.id
                Row(
                    Modifier.fillMaxWidth()
                        .alpha(if (reason != null) 0.6f else 1f)
                        .clip(shape)
                        .background(if (isSelected) colors.primary.copy(alpha = 0.07f) else Color.Transparent)
                        .border(style.border, if (isSelected) colors.primary else colors.outlineVariant, shape)
                        .selectable(isSelected, enabled = reason == null, role = Role.RadioButton) { selectedId = type.id }
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    RadioDot(isSelected, Modifier.padding(top = 2.dp))
                    Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(type.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                        type.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant) }
                        reason?.let { MetaText(it, color = colors.onSurface) }
                    }
                    Text(
                        money(type.price),
                        style = Num.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                        color = if (isSelected) colors.primary else colors.onSurface,
                    )
                }
            }
        }
        selected?.let { type ->
            Perforation(vertical = false, modifier = Modifier.padding(top = 20.dp).fillMaxWidth().height(style.border))
            Row(Modifier.padding(top = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Text("Total", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(money(type.price), style = Total)
            }
            Text(
                "That is the total. Nothing gets added at checkout.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (buyer == Buyer.NotAttendee) {
                InfoBox(
                    R.drawable.ph_info_fill,
                    title = null,
                    body = "You are signed in as an organizer. Buying needs an attendee account.",
                    modifier = Modifier.padding(top = 20.dp),
                )
            } else {
                StubButton(
                    if (buyer == Buyer.SignedOut) "Sign in to get tickets" else "Get tickets",
                    { onBuy(type) },
                    Modifier.padding(top = 20.dp).fillMaxWidth().height(52.dp),
                    icon = R.drawable.ph_arrow_right_bold,
                )
            }
        }
    }
}

@Composable
private fun RadioDot(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = if (LocalExperience.current.experience == Experience.Hype) RectangleShape else CircleShape
    Box(
        modifier.size(16.dp).clip(shape)
            .background(if (selected) colors.primary else Color.Transparent)
            .border(1.dp, if (selected) colors.primary else colors.onSurfaceVariant, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(painterResource(R.drawable.ph_check_bold), null, Modifier.size(10.dp), tint = colors.onPrimary)
    }
}

/** A note set in the accent's tint (the web's `bg-primary/6 border-primary/25`), or neutral when [tinted] is false. */
@Composable
private fun InfoBox(icon: Int, title: String?, body: String, modifier: Modifier = Modifier, tinted: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier.fillMaxWidth()
            .background(if (tinted) colors.primary.copy(alpha = 0.06f) else colors.surfaceContainerHigh, shape)
            .border(LocalExperience.current.border, if (tinted) colors.primary.copy(alpha = 0.25f) else colors.outlineVariant, shape)
            .padding(16.dp),
    ) {
        Icon(painterResource(icon), null, Modifier.padding(top = 2.dp).size(17.dp), tint = if (tinted) colors.primary else colors.onSurfaceVariant)
        Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            title?.let { Text(it, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)) }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

/**
 * The web's confirm page. "No payment is taken" sits above the action, never in small print under it: this build
 * takes no payment and must never look like it collects card details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmContent(
    purchase: PurchaseState,
    event: EventDetail?,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val style = LocalExperience.current
    val busy = purchase is PurchaseState.Purchasing
    val type = when (purchase) {
        is PurchaseState.Confirming -> purchase.type
        is PurchaseState.Purchasing -> purchase.type
        is PurchaseState.Retryable -> purchase.type
        else -> null
    }
    Scaffold(
        containerColor = colors.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !busy) {
                        Icon(painterResource(R.drawable.ph_arrow_left), contentDescription = "Back to event")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            DisplayText("Confirm your ticket", style.displaySection(), Modifier.padding(top = 8.dp).semantics { heading() })
            InfoBox(
                R.drawable.ph_info_fill,
                title = "No payment is taken",
                body = "Card processing is not connected yet, so this issues the ticket directly. Do not enter card " +
                    "details anywhere in this app.",
                modifier = Modifier.padding(top = 24.dp),
                tinted = true,
            )
            StubCard(Modifier.padding(top = 24.dp).fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text("YOU ARE GETTING", style = Eyebrow, color = colors.onSurfaceVariant)
                    event?.let {
                        Text(
                            it.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(top = 16.dp),
                        )
                        it.start?.let { start -> MetaText(start.format(TicketWhen), Modifier.padding(top = 6.dp), color = colors.primary) }
                        it.venue?.let { venue ->
                            Text(venue, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    Perforation(vertical = false, modifier = Modifier.padding(vertical = 20.dp).fillMaxWidth().height(style.border))
                    type?.let { SummaryLines(it.name, it.price) }
                }
            }
            when (purchase) {
                is PurchaseState.Retryable -> Notice(purchase.message, isError = false)
                is PurchaseState.Failed -> Notice(purchase.message, isError = true)
                else -> Unit
            }
            when (purchase) {
                is PurchaseState.Confirming -> StubButton("Get my ticket", onConfirm, ConfirmButton)
                is PurchaseState.Purchasing -> {
                    StubButton("Issuing your ticket", {}, ConfirmButton, enabled = false)
                    LinearProgressIndicator(Modifier.padding(top = 8.dp).fillMaxWidth())
                }
                is PurchaseState.Retryable -> StubButton("Try again", onRetry, ConfirmButton)
                else -> StubButton("Back to event", onBack, ConfirmButton, outlined = true)
            }
            Spacer(Modifier.height(24.dp)) // Scaffold already pads for the navigation bar
        }
    }
}

private val ConfirmButton = Modifier.padding(top = 24.dp).fillMaxWidth().height(52.dp)

@Composable
private fun SummaryLines(tier: String, price: BigDecimal) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(tier, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(money(price), style = Num.copy(fontSize = 14.sp), color = colors.onSurfaceVariant)
    }
    HorizontalDivider(Modifier.padding(top = 16.dp), color = colors.outlineVariant)
    Row(Modifier.padding(top = 16.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text("Total", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), modifier = Modifier.weight(1f))
        Text(money(price), style = Total)
    }
    Text("Nothing is added on top of that.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
}

/** Announced as it appears: the reason a purchase stopped, or that the last attempt is unconfirmed. */
@Composable
private fun Notice(message: String, isError: Boolean) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Text(
        message,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) colors.error else colors.onSurface,
        modifier = Modifier.padding(top = 20.dp).fillMaxWidth()
            .background(if (isError) colors.error.copy(alpha = 0.05f) else colors.surfaceContainerHigh, shape)
            .border(LocalExperience.current.border, if (isError) colors.error.copy(alpha = 0.4f) else colors.outlineVariant, shape)
            .padding(16.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
    )
}

private fun availabilityLabel(availability: Availability): String? = when (availability) {
    Availability.Buyable -> null
    Availability.SoldOut -> "Sold out"
    is Availability.OnSaleFrom -> availability.start?.let { "On sale ${it.format(TicketWhen)}" } ?: "Not on sale yet"
    Availability.SalesEnded -> "Sales ended"
}

/** "$25.00", or "free" for zero: reads as "1 × GA · free". */
internal fun priceLabel(price: BigDecimal): String = if (price.signum() == 0) "free" else money(price)
