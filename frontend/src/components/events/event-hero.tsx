"use client"

import type React from "react"

import { motion, useReducedMotion } from "framer-motion"
import { format } from "date-fns"
import RandomEventImage from "../random-event-image"
import toast from "react-hot-toast"
import { parseWallClockDate } from "@/lib/date-utils"
import { ArrowUpRight } from "@/components/icons"

interface EventHeroProps {
  name: string
  venue: string
  start?: string
  end?: string
  /** Stable id so the stand-in photo does not change between visits. */
  seed?: string
}

/**
 * Full-bleed photo with the detail set as a printed strip along the bottom.
 *
 * The meta used to sit in frosted pills floating on the image. Labels
 * overlaid on photography always read as a template; a solid strip under
 * the image is easier to read and holds up at any crop.
 */
export const EventHero: React.FC<EventHeroProps> = ({ name, venue, start, end, seed }) => {
  const reduce = useReducedMotion()
  const parsedStart = start ? parseWallClockDate(start) : null
  const parsedEnd = end ? parseWallClockDate(end) : null

  const handleShare = async () => {
    const eventUrl = window.location.href
    const shareData = { title: name, text: `Have a look at ${name}`, url: eventUrl }
    try {
      if (navigator.share && navigator.canShare?.(shareData)) {
        await navigator.share(shareData)
        return
      }
      await navigator.clipboard.writeText(eventUrl)
      toast.success("Link copied")
    } catch (error) {
      if ((error as Error).name === "AbortError") return
      try {
        await navigator.clipboard.writeText(eventUrl)
        toast.success("Link copied")
      } catch {
        toast.error("Could not copy the link")
      }
    }
  }

  return (
    <header className="relative">
      <div className="relative h-[46vh] min-h-[320px] w-full overflow-hidden bg-secondary lg:h-[54vh]">
        <RandomEventImage seed={seed ?? name} alt="" priority />
        <div className="absolute inset-0 bg-gradient-to-t from-background via-background/55 to-background/10" />
      </div>

      <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
        <motion.div
          initial={reduce ? false : { opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.55, ease: [0.16, 1, 0.3, 1] }}
          className="relative -mt-24 lg:-mt-32"
        >
          <div className="flex flex-wrap items-end justify-between gap-6">
            <div className="max-w-3xl">
              {parsedStart && (
                <time
                  dateTime={start}
                  className="block text-[0.6875rem] font-medium uppercase tracking-[0.14em] text-primary"
                >
                  {format(parsedStart, "EEEE d MMMM yyyy")}
                </time>
              )}

              <h1 className="display-hero mt-4 text-balance">{name}</h1>

              {/* Printed detail line. One separator, not a chain of dots. */}
              <dl className="mt-6 flex flex-wrap items-center gap-x-8 gap-y-2 text-sm">
                <div>
                  <dt className="sr-only">Venue</dt>
                  <dd className="text-foreground">{venue}</dd>
                </div>
                {parsedStart && (
                  <div>
                    <dt className="sr-only">Time</dt>
                    <dd className="font-mono tabular-nums text-muted-foreground">
                      {format(parsedStart, "HH:mm")}
                      {parsedEnd && ` to ${format(parsedEnd, "HH:mm")}`}
                    </dd>
                  </div>
                )}
              </dl>
            </div>

            <button
              onClick={handleShare}
              className="btn-press focus-ring flex shrink-0 items-center gap-1.5 rounded-md border border-border px-3.5 py-2 text-sm text-muted-foreground transition-colors hover:border-foreground/30 hover:text-foreground"
            >
              Share
              <ArrowUpRight weight="bold" size={14} />
            </button>
          </div>
        </motion.div>
      </div>
    </header>
  )
}

export default EventHero
