package com.venuesync.app.ui.experience

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.venuesync.app.R
import com.venuesync.app.ui.theme.CalmEmber
import com.venuesync.app.ui.theme.Experience
import com.venuesync.app.ui.theme.Lime
import com.venuesync.app.ui.theme.LocalExperience
import com.venuesync.app.ui.theme.Mono

/** Switches the whole app. Provided once by MainActivity from [ExperienceStore.choose]. */
val LocalChooseExperience = staticCompositionLocalOf<(Experience) -> Unit> { error("No experience switch provided") }

/** The web's AUDIENCE_COPY, word for word (except "site" → "app"). */
internal data class ExperienceCopy(val name: String, val forWho: String, val blurb: String, val bullets: List<String>)

internal val Experience.copy: ExperienceCopy
    get() = when (this) {
        Experience.Hype -> ExperienceCopy(
            "Hype", "Made for scrolling on a phone", "One event per screen. Swipe through, tap once, done.",
            listOf("Full screen feed", "Thumb reach controls", "Loud and fast"),
        )
        Experience.Classic -> ExperienceCopy(
            "Classic", "Made for deciding properly", "Everything on one page. Dates, venues and prices side by side.",
            listOf("Sortable list", "Detail up front", "Quiet and dense"),
        )
    }

/** Each experience's own accent and corner, fixed: the swatch shows the other look, not the current one's paint. */
@Composable
internal fun ExperienceSwatch(experience: Experience, modifier: Modifier = Modifier) {
    val (color, shape) = when (experience) {
        Experience.Hype -> Lime to RoundedCornerShape(0.dp)
        Experience.Classic -> CalmEmber to CircleShape
    }
    Box(modifier.size(10.dp).background(color, shape))
}

/**
 * The web's AudienceSwitch as menu rows: a "View" label and two radio items. Lives in the account menu, which is
 * always present, so the first-run choice is never a one-way door.
 */
@Composable
internal fun ExperienceMenuItems(onPicked: () -> Unit) {
    val current = LocalExperience.current.experience
    val choose = LocalChooseExperience.current
    Text(
        "VIEW",
        style = MaterialTheme.typography.labelSmall,
        fontFamily = Mono,
        letterSpacing = 0.14.em,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).semantics { heading() },
    )
    Experience.entries.forEach { option ->
        val isCurrent = option == current
        DropdownMenuItem(
            text = { Text(option.copy.name) },
            leadingIcon = { ExperienceSwatch(option) },
            trailingIcon = if (isCurrent) {
                { Icon(painterResource(R.drawable.ph_check_bold), contentDescription = null, Modifier.size(16.dp)) }
            } else null,
            onClick = {
                onPicked()
                if (!isCurrent) choose(option)
            },
            modifier = Modifier.semantics {
                role = Role.RadioButton
                selected = isCurrent
            },
        )
    }
}
