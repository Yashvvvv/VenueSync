package com.venuesync.app.ui.theme

import androidx.compose.ui.graphics.Color

// Tokens from the web's :root (frontend/src/index.css), converted from oklch. Each comment is the source, so a
// change starts from the web token. Contrast is checked by ThemeContrastTest.

// ── Dark: the web's palette. Warm neutrals (hue 60), one accent.
internal val DarkBackground = Color(0xFF030202) // --background   oklch(0.09 0.004 60)
internal val DarkCard = Color(0xFF080605) //       --card         oklch(0.125 0.005 60)
internal val DarkPopover = Color(0xFF0A0806) //    --popover      oklch(0.135 0.005 60)
internal val DarkSecondary = Color(0xFF100E0C) //  --secondary    oklch(0.165 0.005 60)
internal val DarkAccent = Color(0xFF161312) //     --accent       oklch(0.19 0.005 60)
internal val DarkForeground = Color(0xFFF3F1F0) // --foreground   oklch(0.96 0.002 60)
internal val DarkMuted = Color(0xFF8A8581) //      --muted-fg     oklch(0.62 0.008 60)
internal val DarkOutline = Color(0xFF67625F) //    field edges    oklch(0.5 0.008 60), 3:1 on the background
internal val DarkBorder = Color(0xFF201D1B) //     --border       oklch(0.235 0.006 60), decorative only
internal val DarkError = Color(0xFFF27166) //      oklch(0.7 0.16 27): --destructive is 4.4:1 as text, too low

/** The one accent. Ink on ember, never white: white measures 2.3:1. */
internal val Ember = Color(0xFFF27626) //          --primary      oklch(0.7 0.175 48)
internal val Ink = Color(0xFF150702) //            --primary-fg   oklch(0.15 0.03 48)

// ── Light: new (the web is dark-only), same rules on paper.
internal val LightBackground = Color(0xFFF9F6F3) //    oklch(0.975 0.005 60)
internal val LightCard = Color(0xFFFEFDFC) //          oklch(0.995 0.002 60)
internal val LightContainer = Color(0xFFF4F1EE) //     oklch(0.96 0.005 60)
internal val LightContainerHigh = Color(0xFFF0ECE9) // oklch(0.945 0.006 60)
internal val LightContainerHighest = Color(0xFFEAE5E2) // oklch(0.925 0.007 60)
internal val LightForeground = Color(0xFF1A1512) //    oklch(0.2 0.01 60)
internal val LightMuted = Color(0xFF625C58) //         oklch(0.48 0.01 60)
internal val LightOutline = Color(0xFF857F7A) //       oklch(0.6 0.01 60)
internal val LightBorder = Color(0xFFDDD8D4) //        oklch(0.885 0.008 60)
internal val LightError = Color(0xFFBE2323) //         oklch(0.52 0.19 27)

/**
 * Light primary. M3 uses primary as a fill AND as text (text buttons, tabs, field labels); ember is 2.4:1 as text on
 * paper. This is the darkest ember that stays in sRGB and passes both ways, with white text.
 */
internal val DeepEmber = Color(0xFFA7490D) //          oklch(0.52 0.14 46)

/**
 * Full-screen scan answers, from the web's semantic tokens. The same in light and dark: the colour is the message.
 * Ink text on all four (white would fail on Go and Caution); ThemeContrastTest checks each.
 */
object DoorColors {
    val Go = Color(0xFF41AA66) //      --success      oklch(0.66 0.14 152)
    val Stop = Color(0xFFED4B43) //    oklch(0.64 0.2 27): --destructive (0.58) gives ink only 4.2:1
    val Caution = Color(0xFFD3B63B) // --warning      oklch(0.78 0.14 95)
    val Neutral = Color(0xFFA39D98) // oklch(0.7 0.01 60)
    val OnDoor = Ink
}
