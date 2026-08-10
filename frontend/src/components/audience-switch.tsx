"use client"

import type React from "react"
import { useAudience } from "@/hooks/use-audience"
import { AUDIENCES, AUDIENCE_COPY, type Audience } from "@/lib/audience"

/**
 * Persistent way back to the other site. Lives in the footer and the
 * mobile menu so the first-run choice is never a one-way door.
 */
const SWATCH: Record<Audience, { fill: string; radius: string }> = {
  hype: { fill: "oklch(0.86 0.21 128)", radius: "0px" },
  classic: { fill: "oklch(0.72 0.142 50)", radius: "999px" },
}

export const AudienceSwitch: React.FC<{ className?: string }> = ({ className = "" }) => {
  const { audience, setAudience } = useAudience()

  return (
    <div
      role="radiogroup"
      aria-label="Site experience"
      className={`inline-flex items-center gap-0.5 rounded-md border border-border p-0.5 ${className}`}
    >
      {AUDIENCES.map((option) => {
        const isActive = audience === option
        return (
          <button
            key={option}
            role="radio"
            aria-checked={isActive}
            onClick={() => setAudience(option)}
            title={AUDIENCE_COPY[option].blurb}
            className={`focus-ring flex items-center gap-1.5 rounded-sm px-2.5 py-1 text-xs font-medium transition-colors duration-150 ${
              isActive ? "bg-secondary text-foreground" : "text-muted-foreground hover:text-foreground"
            }`}
          >
            <span
              aria-hidden
              className="inline-block h-2.5 w-2.5 shrink-0"
              style={{ background: SWATCH[option].fill, borderRadius: SWATCH[option].radius }}
            />
            {AUDIENCE_COPY[option].name}
          </button>
        )
      })}
    </div>
  )
}

export default AudienceSwitch
