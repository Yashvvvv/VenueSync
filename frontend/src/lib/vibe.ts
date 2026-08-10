/**
 * Visual vibe preference.
 *
 * Two named looks over one component tree. Everything that differs between
 * them is a CSS custom property, so there is exactly one set of components
 * to build, test and keep accessible. Shipping two parallel UIs would have
 * doubled the surface area for the rest of the project's life.
 *
 *   calm - softer accent, rounder corners, quieter grain, lifted surfaces
 *   bold - hotter accent, near-square corners, heavier grain, deeper ground
 *
 * Deliberately not labelled by generation. People cannot answer "are you
 * Gen Z or a millennial" about an interface they have not seen yet, and
 * sorting visitors by age reads badly no matter how it is worded.
 */

export type Vibe = "calm" | "bold" | "acid"

export const VIBES: readonly Vibe[] = ["calm", "bold", "acid"] as const

/**
 * The quieter look is the default: it is the safer first impression, and a
 * shared event link should not open on the loudest thing we own.
 * Change this one constant to make another look the default.
 */
export const DEFAULT_VIBE: Vibe = "calm"

export const VIBE_STORAGE_KEY = "venuesync:vibe"

/** Accent swatch per vibe. Also drives the preview dots on the switch. */
export const VIBE_SWATCH: Record<Vibe, { fill: string; radius: string }> = {
  calm: { fill: "oklch(0.72 0.142 50)", radius: "999px" },
  bold: { fill: "oklch(0.72 0.205 45)", radius: "1px" },
  acid: { fill: "oklch(0.86 0.21 128)", radius: "0px" },
}

/**
 * canvas-confetti paints to a canvas and needs concrete colours, so it
 * cannot read the oklch custom properties the rest of the theme uses.
 * These are hex equivalents of each vibe's accents, kept here next to the
 * swatches so the two do not drift apart.
 */
export const VIBE_CONFETTI: Record<Vibe, string[]> = {
  calm: ["#e8a06a", "#dd8449", "#f2c39c"],
  bold: ["#f0913c", "#e8752b", "#f5b169"],
  acid: ["#b6f24a", "#d8ff7d", "#ff5fb0"],
}

export const VIBE_LABELS: Record<Vibe, { label: string; hint: string }> = {
  calm: { label: "Calm", hint: "Softer contrast, rounder edges" },
  bold: { label: "Bold", hint: "Hotter accent, harder edges" },
  acid: { label: "Acid", hint: "Xeroxed gig flyer. Loud on purpose" },
}

export function isVibe(value: unknown): value is Vibe {
  return value === "calm" || value === "bold" || value === "acid"
}

/**
 * localStorage throws rather than returning null in Safari private mode and
 * when a site is blocked from storing data, so every access is guarded.
 * A visitor who blocks storage still gets a working site on the default.
 */
export function readStoredVibe(): Vibe | null {
  try {
    const raw = window.localStorage.getItem(VIBE_STORAGE_KEY)
    return isVibe(raw) ? raw : null
  } catch {
    return null
  }
}

export function storeVibe(vibe: Vibe): void {
  try {
    window.localStorage.setItem(VIBE_STORAGE_KEY, vibe)
  } catch {
    /* Storage unavailable. The choice applies for this page view only. */
  }
}

export function applyVibe(vibe: Vibe): void {
  document.documentElement.dataset.vibe = vibe
}
