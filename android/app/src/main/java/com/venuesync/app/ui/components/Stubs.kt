package com.venuesync.app.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.venuesync.app.R
import com.venuesync.app.core.model.ApiError
import com.venuesync.app.core.model.Event
import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.core.model.TicketSummary
import com.venuesync.app.core.model.ticketCodeOf
import com.venuesync.app.ui.events.message
import com.venuesync.app.ui.theme.Destructive
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.Meta
import com.venuesync.app.ui.theme.Num
import com.venuesync.app.ui.theme.Perforation
import com.venuesync.app.ui.theme.StubCard
import com.venuesync.app.ui.theme.TicketShape
import com.venuesync.app.ui.theme.animationsOn
import com.venuesync.app.ui.theme.eventImageFor
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Currency

// ── Formats. Wall clock as the server sent it (ADR-003): formatted, never converted.
internal val CardDay: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
internal val ShortDay: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
internal val Clock: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
internal val HeroDay: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy")
internal val TicketWhen: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy · HH:mm")

// ponytail: the API contract has no currency field and the web shows "$", so USD is assumed. Add `currency` to the
// DTOs once the backend sends it. NumberFormat isn't thread-safe, so one per call.
internal fun money(price: BigDecimal): String =
    NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance("USD") }.format(price)

/** "Fri 14 Mar", "Fri 14 Mar to 16 Mar" when it runs over several days. */
internal fun cardDay(start: LocalDateTime, end: LocalDateTime?): String {
    val multiDay = end != null && end.toLocalDate().isAfter(start.toLocalDate())
    return if (multiDay) "${start.format(CardDay)} to ${end!!.format(ShortDay)}" else start.format(CardDay)
}

/**
 * The web's event card: a photo counterfoil on top, a perforation punched at both edges, then the printed detail
 * (name, date in the accent with the time muted, venue). The photo is the web's stand-in for this event id.
 */
@Composable
fun EventStubCard(event: Event, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val photo = maxWidth * 0.6f // 5:3
        StubCard(shape = TicketShape(photo, vertical = false, LocalExperience.current.radius), onClick = onClick) {
            Box(Modifier.fillMaxWidth().height(photo).background(colors.surfaceContainerHigh)) {
                AsyncImage(eventImageFor(event.id), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                // Weighs the photo down so the stub below reads as the same object.
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(96.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, colors.surfaceContainerLow))),
                )
            }
            Perforation(vertical = false, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(LocalExperience.current.border))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    event.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.01).em),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    event.start?.let { start ->
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = colors.primary)) { append(cardDay(start, event.end).uppercase()) }
                                withStyle(SpanStyle(color = colors.onSurfaceVariant)) { append(" · ${start.format(Clock)}") }
                            },
                            style = Meta,
                        )
                    }
                    event.venue?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

private val TicketCounterfoil = 104.dp

/**
 * The web's ticket row, printed like a ticket: a counterfoil with the price and serial, a vertical perforation punched
 * top and bottom, then the event, a status chip, the type and the date. Spent tickets fade to 65%.
 */
@Composable
fun TicketStubRow(ticket: TicketSummary, now: LocalDateTime, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val status = ticket.displayStatus(now)
    val spent = status != TicketStatus.Purchased && status != TicketStatus.Unknown
    StubCard(
        shape = TicketShape(TicketCounterfoil, vertical = true, LocalExperience.current.radius),
        onClick = onClick,
        modifier = modifier.fillMaxWidth().alpha(if (spent) 0.65f else 1f),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Column(
                Modifier.width(TicketCounterfoil).fillMaxHeight().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                ticket.price?.let {
                    Text(money(it), style = Num.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = if (spent) colors.onSurfaceVariant else colors.primary)
                }
                Text(ticketCodeOf(ticket.id), style = Meta.copy(fontSize = 10.sp, letterSpacing = 0.1.em), color = colors.onSurfaceVariant)
            }
            Perforation(vertical = true, modifier = Modifier.fillMaxHeight().width(LocalExperience.current.border))
            Row(Modifier.weight(1f).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            ticket.eventName,
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        StatusChip(status)
                    }
                    Text(ticket.ticketTypeName, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    ticket.eventStart?.let { MetaText(it.format(TicketWhen), Modifier.padding(top = 2.dp)) }
                }
                Icon(painterResource(R.drawable.ph_caret_right), null, Modifier.padding(start = 12.dp).size(14.dp), tint = colors.onSurfaceVariant)
            }
        }
    }
}

/** Copy for an empty event list: a search that found nothing is the reader's to fix; an empty catalogue isn't. */
data class EmptyCopy(val title: String, val body: String, val action: String?)

fun eventsEmptyCopy(query: String): EmptyCopy = query.trim().let { q ->
    if (q.isEmpty()) {
        EmptyCopy(
            "Nothing on sale right now",
            "No events are published yet. New ones appear here as soon as an organizer puts them on sale.",
            action = null,
        )
    } else {
        EmptyCopy("Nothing matched that", "No events came back for \"$q\". Try a shorter term, or a city name on its own.", "Show all events")
    }
}

/** The web's EmptyState: an unpunched stub (the shape of the thing that is missing), a title, a line, one action. */
@Composable
fun StubEmptyState(
    icon: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val style = LocalExperience.current
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier.width(160.dp)
                .background(colors.surfaceContainerLow.copy(alpha = 0.4f), MaterialTheme.shapes.medium)
                .drawBehind {
                    val w = style.border.toPx()
                    drawRoundRect(
                        colors.outlineVariant,
                        topLeft = Offset(w / 2, w / 2),
                        size = Size(size.width - w, size.height - w),
                        cornerRadius = CornerRadius(style.radius.toPx()),
                        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))),
                    )
                }
                .clearAndSetSemantics {},
        ) {
            Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.Center) {
                Icon(painterResource(icon), null, Modifier.size(22.dp), tint = colors.onSurfaceVariant.copy(alpha = 0.5f))
            }
            Perforation(vertical = false, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(style.border))
            Spacer(Modifier.height(32.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 28.dp, bottom = 8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
        if (actionLabel != null && onAction != null) {
            StubButton(actionLabel, onAction, Modifier.padding(top = 28.dp))
        }
    }
}

/** A failure in plain words: a warning icon, the reason (never raw exception text), one action. */
@Composable
fun ErrorState(
    error: ApiError,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    actionLabel: String = "Try again",
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp).semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(painterResource(R.drawable.ph_warning_circle_fill), null, Modifier.size(30.dp), tint = Destructive)
        Text(
            error.message(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
        )
        StubButton(actionLabel, onAction)
    }
}

// ── Skeletons: the real card's exact shape (notches included), so nothing jumps when data lands.

/** 1.6s breathing between the two highest surfaces; still when animations are off. */
@Composable
private fun shimmer(): Color {
    val colors = MaterialTheme.colorScheme
    if (!animationsOn()) return colors.surfaceContainerHigh
    val color by rememberInfiniteTransition(label = "shimmer").animateColor(
        colors.surfaceContainerHigh,
        colors.surfaceContainerHighest,
        infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "shimmer",
    )
    return color
}

@Composable
private fun Bar(width: Float, height: Dp, color: Color) {
    Box(Modifier.fillMaxWidth(width).height(height).background(color, MaterialTheme.shapes.extraSmall))
}

/** [count] placeholders read as one "Loading" to TalkBack, not as a pile of empty boxes. */
@Composable
fun SkeletonList(count: Int, label: String, modifier: Modifier = Modifier, item: @Composable () -> Unit) {
    Column(
        modifier.padding(16.dp).clearAndSetSemantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { repeat(count) { item() } }
}

@Composable
fun EventStubSkeleton() {
    val tone = shimmer()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val photo = maxWidth * 0.6f
        StubCard(shape = TicketShape(photo, vertical = false, LocalExperience.current.radius)) {
            Box(Modifier.fillMaxWidth().height(photo).background(tone))
            Perforation(vertical = false, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(LocalExperience.current.border))
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Bar(0.7f, 14.dp, tone)
                Bar(0.35f, 10.dp, tone)
                Bar(0.5f, 10.dp, tone)
            }
        }
    }
}

@Composable
fun TicketStubSkeleton() {
    val tone = shimmer()
    StubCard(shape = TicketShape(TicketCounterfoil, vertical = true, LocalExperience.current.radius), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Column(Modifier.width(TicketCounterfoil).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Bar(0.8f, 14.dp, tone)
                Bar(0.6f, 8.dp, tone)
            }
            Perforation(vertical = true, modifier = Modifier.fillMaxHeight().width(LocalExperience.current.border))
            Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Bar(0.7f, 12.dp, tone)
                Bar(0.4f, 10.dp, tone)
                Bar(0.55f, 8.dp, tone)
            }
        }
    }
}
