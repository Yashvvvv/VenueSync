package com.venuesync.app.ui.theme

import androidx.compose.ui.graphics.Color

// Tokens from the web's vibe blocks (frontend/src/index.css), converted from oklch. Each comment is the source, so a
// change starts from the web token. Contrast is checked by ThemeContrastTest. The web's base :root tokens are gone:
// the web itself never shows them (every visitor gets calm or acid).

// ── Classic dark: the web's calm vibe. Warm neutrals (hue 60), one accent.
internal val CalmBackground = Color(0xFF050403) // --background   oklch(0.105 0.005 60)
internal val CalmCard = Color(0xFF0B0907) //       --card         oklch(0.142 0.006 60)
internal val CalmPopover = Color(0xFF0D0B09) //    --popover      oklch(0.15 0.006 60)
internal val CalmSecondary = Color(0xFF13100E) //  --secondary    oklch(0.175 0.006 60)
internal val CalmAccent = Color(0xFF181513) //     --accent       oklch(0.2 0.006 60)
internal val CalmForeground = Color(0xFFF3F1F0) // --foreground   oklch(0.96 0.002 60)
internal val CalmMuted = Color(0xFF918D89) //      --muted-fg     oklch(0.645 0.008 60)
internal val CalmOutline = Color(0xFF67625F) //    field edges    oklch(0.5 0.008 60), 3:1 on every surface
internal val CalmBorder = Color(0xFF23201E) //     --border       oklch(0.245 0.006 60), decorative only

/** The classic accent. Ink on ember, never white: white measures 2.6:1. */
internal val CalmEmber = Color(0xFFEA874C) //      --primary      oklch(0.72 0.142 50)
internal val Ink = Color(0xFF170904) //            --primary-fg   oklch(0.16 0.028 50)

/** Error text on dark grounds: --destructive is 4.4:1 as text, too low. Fills use [Destructive]. */
internal val ErrorText = Color(0xFFF27166) //      oklch(0.7 0.16 27)

// ── Classic light: Android-only (the web is dark-only), same rules on paper.
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
 * Light primary. M3 uses primary as a fill AND as text (text buttons, tabs, field labels); calm ember is ~2.4:1 as
 * text on paper. Calm's chroma is 81% of the old base ember's, so this is step 6's deep ember (0.52 0.14 46) at that
 * ratio: 4.6:1 as text on every light surface, 5.8:1 under white text.
 */
internal val DeepEmber = Color(0xFF9C5223) //          oklch(0.52 0.115 50)

// ── Hype: the web's acid vibe. Black stock, two ink plates. Dark only: lime on paper fails.
internal val AcidBackground = Color(0xFF000000) // --background   oklch(0.045 0 0)
internal val AcidCard = Color(0xFF020202) //       --card         oklch(0.085 0 0)
internal val AcidPopover = Color(0xFF030303) //    --popover      oklch(0.1 0 0)
internal val AcidSecondary = Color(0xFF090909) //  --secondary    oklch(0.14 0 0)
internal val AcidAccent = Color(0xFF0F0F0F) //     --accent       oklch(0.17 0 0)
internal val AcidForeground = Color(0xFFF8F8F8) // --foreground   oklch(0.98 0 0)
internal val AcidMuted = Color(0xFFA4A4A4) //      --muted-fg     oklch(0.72 0 0)
internal val AcidBorder = Color(0xFF636363) //     --border       oklch(0.5 0 0): also the field edge, 3.2:1+

/** Plate 1. Ink on lime, never white (1.5:1). */
internal val Lime = Color(0xFFA9E932) //           --primary      oklch(0.86 0.21 128)
internal val LimeInk = Color(0xFF091003) //        --primary-fg   oklch(0.16 0.03 128)

/** Plate 2: eyebrows, selection, the chooser's "for who" line. Small type only; never white on it (2.75:1). */
internal val Magenta = Color(0xFFFF62B2) //        --plate-2      oklch(0.72 0.205 352)

// ── Semantic, both experiences.
internal val Success = Color(0xFF41AA66) //        --success      oklch(0.66 0.14 152)
internal val Warning = Color(0xFFD3B63B) //        --warning      oklch(0.78 0.14 95)
/** Fills only (status icons, error icon). Text uses the scheme's error colour. */
internal val Destructive = Color(0xFFD33B36) //    --destructive  oklch(0.58 0.19 27)

/**
 * Full-screen scan answers, from the web's semantic tokens. The same in every experience and mode: the colour is
 * the message. Ink text on all four (white would fail on Go and Caution); ThemeContrastTest checks each.
 */
object DoorColors {
    val Go = Success
    val Stop = Color(0xFFED4B43) //    oklch(0.64 0.2 27): --destructive (0.58) gives ink only 4.2:1
    val Caution = Warning
    val Neutral = Color(0xFFA39D98) // oklch(0.7 0.01 60)
    val OnDoor = Ink
}
