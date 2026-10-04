package com.venuesync.app.ui.theme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.venuesync.app.R

/**
 * The mark: a ticket stub with punched sides, a tear line and a spark. Painted from the theme (primary stock,
 * onPrimary detail) so it follows the experience, like the web's VenueSyncMark. Decorative: pair it with text.
 */
@Composable
fun BrandMark(width: Dp = 28.dp, modifier: Modifier = Modifier) {
    Box(modifier.width(width).aspectRatio(40f / 29f)) {
        Icon(painterResource(R.drawable.ic_mark_body), null, Modifier.fillMaxSize(), MaterialTheme.colorScheme.primary)
        Icon(painterResource(R.drawable.ic_mark_detail), null, Modifier.fillMaxSize(), MaterialTheme.colorScheme.onPrimary)
    }
}

/** "VenueSync" set like the web's logotype: Archivo SemiBold, tight, "Sync" in the accent. */
@Composable
fun Wordmark(modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.titleLarge) {
    Text(
        buildAnnotatedString {
            append("Venue")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Sync") }
        },
        style = style.copy(fontFamily = Archivo, letterSpacing = (-0.02).em),
        modifier = modifier,
    )
}

/** Mark + wordmark, read as one word by TalkBack. */
@Composable
fun Lockup(modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.titleLarge, markWidth: Dp = 28.dp) {
    Row(
        modifier.clearAndSetSemantics { contentDescription = "VenueSync" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(markWidth)
        Wordmark(style = style)
    }
}
