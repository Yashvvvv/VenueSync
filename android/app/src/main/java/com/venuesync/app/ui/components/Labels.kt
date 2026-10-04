package com.venuesync.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.venuesync.app.R
import com.venuesync.app.core.model.TicketStatus
import com.venuesync.app.ui.theme.Destructive
import com.venuesync.app.ui.theme.Eyebrow
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.Meta
import com.venuesync.app.ui.theme.Mono
import com.venuesync.app.ui.theme.Success
import com.venuesync.app.ui.theme.Warning
import com.venuesync.app.ui.theme.pressNudge

/** `.eyebrow`: a quiet mono label in the experience's eyebrow plate (ember in Classic, magenta in Hype). */
@Composable
fun EyebrowText(text: String, modifier: Modifier = Modifier, color: Color = LocalExperience.current.eyebrow) {
    Text(text.uppercase(), style = Eyebrow, color = color, modifier = modifier)
}

/** `.meta`: dates, venues, serials. */
@Composable
fun MetaText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(text.uppercase(), style = Meta, color = color, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

/** Display type, uppercased where the experience's face is caps-only (Anton). */
@Composable
fun DisplayText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        if (LocalExperience.current.uppercaseDisplay) text.uppercase() else text,
        style = style,
        color = color,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * Ticket status as icon + label (never colour alone). The icon carries the semantic colour; the label stays muted so
 * it passes AA on every surface in every scheme (success and destructive don't as small text).
 */
@Composable
fun StatusChip(status: TicketStatus, modifier: Modifier = Modifier) {
    val (icon, tint, label) = when (status) {
        TicketStatus.Purchased, TicketStatus.Unknown -> Triple(R.drawable.ph_ticket_fill, Success, "Valid")
        TicketStatus.Used -> Triple(R.drawable.ph_check_circle_fill, MaterialTheme.colorScheme.onSurfaceVariant, "Used")
        TicketStatus.Expired -> Triple(R.drawable.ph_clock_fill, Warning, "Expired")
        TicketStatus.Cancelled -> Triple(R.drawable.ph_x_circle_fill, Destructive, "Cancelled")
    }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(11.dp))
        Text(
            label.uppercase(),
            style = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 0.08.em),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The app's button: the theme's corner (M3 buttons ignore Shapes), Hype's caps and 2dp rule, and the 1dp press
 * nudge. [outlined] is the secondary action.
 */
@Composable
fun StubButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    outlined: Boolean = false,
    icon: Int? = null,
) {
    val style = LocalExperience.current
    val interaction = remember { MutableInteractionSource() }
    val label: @Composable () -> Unit = {
        Text(if (style.uppercaseCta) text.uppercase() else text, fontWeight = FontWeight.SemiBold)
        icon?.let { Icon(painterResource(it), null, Modifier.padding(start = 8.dp).size(16.dp)) }
    }
    val m = modifier.pressNudge(interaction)
    if (outlined) {
        OutlinedButton(
            onClick, m, enabled, shape = MaterialTheme.shapes.small, interactionSource = interaction,
            border = BorderStroke(style.border, MaterialTheme.colorScheme.outline),
        ) { label() }
    } else {
        Button(onClick, m, enabled, shape = MaterialTheme.shapes.small, interactionSource = interaction) { label() }
    }
}
