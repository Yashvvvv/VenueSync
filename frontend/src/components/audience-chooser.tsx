"use client"

import type React from "react"
import { motion, useReducedMotion } from "framer-motion"
import { useAudience } from "@/hooks/use-audience"
import { AUDIENCE_COPY, type Audience } from "@/lib/audience"
import { VenueSyncMark, ArrowRight, Check } from "@/components/icons"

/*
 * Palettes are written out literally here instead of read from tokens.
 * This is the one surface that has to show both looks at the same moment,
 * and the token layer paints exactly one at a time by design.
 * Values mirror the calm and acid blocks in index.css; keep them in step.
 */
const PALETTE = {
  hype: {
    bg: "oklch(0.045 0 0)",
    panel: "oklch(0.085 0 0)",
    line: "oklch(0.45 0 0)",
    ink: "oklch(0.98 0 0)",
    muted: "oklch(0.72 0 0)",
    accent: "oklch(0.86 0.21 128)",
    onAccent: "oklch(0.16 0.03 128)",
    second: "oklch(0.72 0.205 352)",
    display: '"Anton", system-ui, sans-serif',
    radius: "0px",
  },
  classic: {
    bg: "oklch(0.105 0.005 60)",
    panel: "oklch(0.142 0.006 60)",
    line: "oklch(0.245 0.006 60)",
    ink: "oklch(0.96 0.002 60)",
    muted: "oklch(0.645 0.008 60)",
    accent: "oklch(0.72 0.142 50)",
    onAccent: "oklch(0.16 0.028 50)",
    second: "oklch(0.72 0.142 50)",
    display: '"Archivo Variable", system-ui, sans-serif',
    radius: "10px",
  },
} as const

const EASE = [0.16, 1, 0.3, 1] as const

/** Full-bleed single panel: what the hype feed actually looks like. */
const HypePreview: React.FC = () => {
  const p = PALETTE.hype
  return (
    <div
      className="relative mx-auto w-[186px] overflow-hidden"
      style={{ background: p.panel, border: `2px solid ${p.line}`, borderRadius: p.radius }}
    >
      <div className="relative h-[228px]">
        <img src="/event-image-3.webp" alt="" className="h-full w-full object-cover" />
        <div
          className="absolute inset-0"
          style={{ background: `linear-gradient(to top, ${p.bg} 12%, transparent 70%)` }}
        />
        <div className="absolute inset-x-0 bottom-0 p-3">
          <p
            className="text-[19px] uppercase leading-[0.86]"
            style={{ fontFamily: p.display, color: p.ink }}
          >
            Nocturne
            <br />
            late set
          </p>
          <p
            className="mt-1.5 font-mono text-[8px] uppercase tracking-[0.14em]"
            style={{ color: p.second }}
          >
            Fri 14 Mar
          </p>
          <div
            className="mt-2.5 flex h-6 items-center justify-center text-[9px] font-semibold uppercase tracking-wide"
            style={{ background: p.accent, color: p.onAccent, borderRadius: p.radius }}
          >
            Get ticket
          </div>
        </div>
      </div>
      {/* Thumb-zone tab bar */}
      <div
        className="flex items-center justify-around py-2"
        style={{ borderTop: `2px solid ${p.line}` }}
      >
        {[p.accent, p.muted, p.muted].map((c, i) => (
          <span key={i} className="h-1.5 w-6" style={{ background: c }} />
        ))}
      </div>
    </div>
  )
}

/** Dense grid: what the classic listing actually looks like. */
const ClassicPreview: React.FC = () => {
  const p = PALETTE.classic
  return (
    <div className="mx-auto w-[210px]">
      <div className="flex items-center justify-between pb-2">
        <span className="text-[10px] font-semibold" style={{ color: p.ink }}>
          On sale now
        </span>
        <span className="text-[8px]" style={{ color: p.muted }}>
          View all
        </span>
      </div>
      <div className="grid grid-cols-2 gap-2">
        {[1, 2, 4, 3].map((n, i) => (
          <div
            key={n}
            style={{
              background: p.panel,
              border: `1px solid ${p.line}`,
              borderRadius: p.radius,
              overflow: "hidden",
            }}
          >
            <img src={`/event-image-${n}.webp`} alt="" className="h-[46px] w-full object-cover" />
            <div className="space-y-1 p-1.5">
              <div className="h-1 w-4/5 rounded-sm" style={{ background: p.muted, opacity: 0.5 }} />
              <div
                className="h-1 w-2/5 rounded-sm"
                style={{ background: i === 0 ? p.accent : p.muted, opacity: i === 0 ? 1 : 0.35 }}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}

const PREVIEW: Record<Audience, React.FC> = { hype: HypePreview, classic: ClassicPreview }

/**
 * First-run fork.
 *
 * Rendered only at the site root and only before a choice exists. Deep
 * links (a shared event, the app page, anything a crawler fetches) never
 * hit this, so it does not sit between an inbound visitor and the content
 * they asked for, and it does not behave as the kind of interstitial
 * search engines demote.
 *
 * Both options are previewed rather than described. Asking someone to pick
 * an interface they have not seen is not a real question.
 */
export const AudienceChooser: React.FC = () => {
  const { setAudience, acknowledge } = useAudience()
  const reduce = useReducedMotion()

  return (
    <main
      className="relative min-h-[100dvh] w-full"
      style={{ background: PALETTE.classic.bg, color: PALETTE.classic.ink }}
    >
      <div className="mx-auto flex min-h-[100dvh] max-w-[1400px] flex-col px-5 py-8 lg:px-8">
        <div className="flex items-center gap-2.5">
          <VenueSyncMark size={30} />
          <span className="text-[15px] font-semibold tracking-tight">VenueSync</span>
        </div>

        <div className="flex flex-1 flex-col justify-center py-10">
          <motion.h1
            initial={reduce ? false : { opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: EASE }}
            className="max-w-[16ch] text-balance text-3xl font-semibold leading-[1.08] tracking-tight sm:text-4xl lg:text-5xl"
          >
            Two ways to use this. Pick one.
          </motion.h1>
          <motion.p
            initial={reduce ? false : { opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.07, duration: 0.5, ease: EASE }}
            className="mt-4 max-w-[52ch] text-[0.9375rem] leading-relaxed"
            style={{ color: PALETTE.classic.muted }}
          >
            Same events, same tickets. Completely different site around them. You can switch back
            whenever you like.
          </motion.p>

          <div className="mt-10 grid gap-5 lg:mt-14 lg:grid-cols-2 lg:gap-6">
            {(["hype", "classic"] as Audience[]).map((key, i) => {
              const p = PALETTE[key]
              const copy = AUDIENCE_COPY[key]
              const Preview = PREVIEW[key]

              return (
                <motion.button
                  key={key}
                  onClick={() => setAudience(key)}
                  initial={reduce ? false : { opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.14 + i * 0.09, duration: 0.55, ease: EASE }}
                  whileHover={reduce ? undefined : { y: -4 }}
                  className="group focus-ring flex flex-col overflow-hidden p-6 text-left transition-colors sm:p-8"
                  style={{
                    background: p.bg,
                    border: `2px solid ${p.line}`,
                    borderRadius: p.radius,
                    color: p.ink,
                  }}
                >
                  <div className="flex items-baseline justify-between gap-4">
                    <span
                      className="text-2xl leading-none sm:text-[1.75rem]"
                      style={{
                        fontFamily: p.display,
                        textTransform: key === "hype" ? "uppercase" : "none",
                        fontWeight: key === "hype" ? 400 : 650,
                        letterSpacing: key === "hype" ? "0" : "-0.03em",
                      }}
                    >
                      {copy.name}
                    </span>
                    <span
                      className="font-mono text-[10px] uppercase tracking-[0.12em]"
                      style={{ color: p.second }}
                    >
                      {copy.forWho}
                    </span>
                  </div>

                  <p className="mt-3 max-w-[34ch] text-sm leading-relaxed" style={{ color: p.muted }}>
                    {copy.blurb}
                  </p>

                  <div className="my-7 flex-1">
                    <Preview />
                  </div>

                  <ul className="space-y-2">
                    {copy.bullets.map((b) => (
                      <li key={b} className="flex items-center gap-2 text-xs" style={{ color: p.muted }}>
                        <Check weight="bold" size={12} style={{ color: p.accent }} />
                        {b}
                      </li>
                    ))}
                  </ul>

                  <span
                    className="mt-7 flex h-11 items-center justify-center gap-2 text-sm font-semibold"
                    style={{
                      background: p.accent,
                      color: p.onAccent,
                      borderRadius: p.radius,
                      textTransform: key === "hype" ? "uppercase" : "none",
                      letterSpacing: key === "hype" ? "0.04em" : "0",
                    }}
                  >
                    Use {copy.name}
                    <ArrowRight weight="bold" size={15} />
                  </span>
                </motion.button>
              )
            })}
          </div>
        </div>

        <div className="flex justify-center pb-2">
          <button
            onClick={acknowledge}
            className="focus-ring rounded-sm px-3 py-2 text-sm underline-offset-4 transition-colors hover:underline"
            style={{ color: PALETTE.classic.muted }}
          >
            Skip, just show me the events
          </button>
        </div>
      </div>
    </main>
  )
}

export default AudienceChooser
