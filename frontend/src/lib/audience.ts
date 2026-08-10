import type { Vibe } from "./vibe"

/**
 * Audience: the fork that decides which *website* you get.
 *
 * This sits above the vibe tokens on purpose. Swapping colours, radius and
 * type scale was never going to read as "a different site", because the
 * structure underneath stayed identical: same grid, same top nav, same
 * section order, same motion. Audience changes the structure.
 *
 * Grounded in published 2026 research rather than taste:
 *
 *   hype     Gen Z leaning. Mobile-first and app-shaped. Vertical
 *            full-screen panels driven by scroll-snap, because this
 *            cohort uses Stories and Reels more than traditional feeds
 *            and has internalised gesture vocabulary. Bottom tab bar in
 *            the thumb zone. One thing per screen: 60% of Gen Z report
 *            preferring *simple* layouts, so this is bold visually and
 *            plain structurally, not maximalist clutter. Discovery over
 *            guided browsing, no onboarding, content on screen one.
 *
 *   classic  Millennial and organizer leaning. Balanced desktop and
 *            mobile, comfortable with denser menus and longer copy.
 *            Editorial grid, top navigation, metadata visible without
 *            tapping, value proposition and trust signals before the
 *            listings. Restrained motion.
 *
 * Each audience selects a vibe token set, so the existing CSS layer does
 * the painting and this layer only decides composition.
 */
export type Audience = "hype" | "classic"

export const AUDIENCES: readonly Audience[] = ["hype", "classic"] as const

/** Nothing is assumed until the visitor picks or a deep link forces it. */
export const DEFAULT_AUDIENCE: Audience = "classic"

export const AUDIENCE_STORAGE_KEY = "venuesync:audience"

/** Composition decides structure; the vibe decides paint. */
export const AUDIENCE_VIBE: Record<Audience, Vibe> = {
  hype: "acid",
  classic: "calm",
}

export const AUDIENCE_COPY: Record<
  Audience,
  { name: string; forWho: string; blurb: string; bullets: string[] }
> = {
  hype: {
    name: "Hype",
    forWho: "Made for scrolling on a phone",
    blurb: "One event per screen. Swipe through, tap once, done.",
    bullets: ["Full screen feed", "Thumb reach controls", "Loud and fast"],
  },
  classic: {
    name: "Classic",
    forWho: "Made for deciding properly",
    blurb: "Everything on one page. Dates, venues and prices side by side.",
    bullets: ["Sortable grid", "Detail up front", "Quiet and dense"],
  },
}

export function isAudience(value: unknown): value is Audience {
  return value === "hype" || value === "classic"
}

/** Storage throws in Safari private mode, so every access is guarded. */
export function readStoredAudience(): Audience | null {
  try {
    const raw = window.localStorage.getItem(AUDIENCE_STORAGE_KEY)
    return isAudience(raw) ? raw : null
  } catch {
    return null
  }
}

export function storeAudience(audience: Audience): void {
  try {
    window.localStorage.setItem(AUDIENCE_STORAGE_KEY, audience)
  } catch {
    /* Choice applies to this page view only. */
  }
}

export function applyAudience(audience: Audience): void {
  document.documentElement.dataset.audience = audience
  document.documentElement.dataset.vibe = AUDIENCE_VIBE[audience]
}
