package com.venuesync.app.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.venuesync.app.R
import com.venuesync.app.ui.theme.LocalExperience

enum class HypeTab { Feed, Explore, Tickets, Events }

/**
 * Hype's whole navigation, in the thumb zone. Feed, Explore (every event, searchable) and Tickets for everyone, and
 * Events for organizers: their main destination, too important for a menu. Four at most, never five.
 */
@Composable
fun HypeTabBar(active: HypeTab?, showEvents: Boolean, onSelect: (HypeTab) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val rule = LocalExperience.current.border
    Row(
        modifier.fillMaxWidth()
            .background(colors.background)
            .drawBehind { drawLine(colors.outlineVariant, Offset(0f, 0f), Offset(size.width, 0f), rule.toPx()) }
            .navigationBarsPadding()
            .selectableGroup(),
    ) {
        Tab(HypeTab.Feed, "Feed", R.drawable.ph_house_fill, R.drawable.ph_house, active, onSelect)
        Tab(HypeTab.Explore, "Explore", R.drawable.ph_magnifying_glass_bold, R.drawable.ph_magnifying_glass, active, onSelect)
        Tab(HypeTab.Tickets, "Tickets", R.drawable.ph_ticket_fill, R.drawable.ph_ticket, active, onSelect)
        if (showEvents) Tab(HypeTab.Events, "Events", R.drawable.ph_calendar_blank_fill, R.drawable.ph_calendar_blank_bold, active, onSelect)
    }
}

@Composable
private fun RowScope.Tab(tab: HypeTab, label: String, activeIcon: Int, icon: Int, active: HypeTab?, onSelect: (HypeTab) -> Unit) {
    val on = tab == active
    val color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        Modifier.weight(1f).heightIn(min = 56.dp)
            .clickable(role = Role.Tab) { onSelect(tab) }
            .semantics {
                role = Role.Tab
                selected = on
            }
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(painterResource(if (on) activeIcon else icon), contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(
            label.uppercase(),
            style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.1.em),
            color = color,
        )
    }
}
