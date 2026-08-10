"use client"

import type React from "react"

import type { PublishedEventSummary } from "@/domain/domain"
import { motion, useReducedMotion } from "framer-motion"
import { format } from "date-fns"
import { Link } from "react-router"
import RandomEventImage from "../random-event-image"
import { parseWallClockDate } from "@/lib/date-utils"

interface EventCardProps {
  event: PublishedEventSummary
  index?: number
  priority?: boolean
}

/**
 * The card is a ticket stub, not a content box.
 *
 * Photo on the counterfoil, a perforation with two punched notches, then
 * the printed detail below. The notches read as holes because they are
 * filled with the page colour, which is why the page background has to
 * stay flat.
 */
export const EventCard: React.FC<EventCardProps> = ({ event, index = 0, priority = false }) => {
  const reduce = useReducedMotion()
  const start = event.start ? parseWallClockDate(event.start) : null
  const end = event.end ? parseWallClockDate(event.end) : null

  const sameDay = start && end && format(start, "yyyy-MM-dd") === format(end, "yyyy-MM-dd")

  return (
    <motion.div
      initial={reduce ? false : { opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: Math.min(index, 7) * 0.05, duration: 0.45, ease: [0.16, 1, 0.3, 1] }}
    >
      <Link to={`/events/${event.id}`} className="group block focus-ring rounded-md">
        <article className="card-hover relative rounded-md border border-border bg-card">
          {/* Counterfoil */}
          <div className="relative aspect-[5/3] overflow-hidden rounded-t-md bg-secondary">
            <div className="absolute inset-0 transition-transform duration-[600ms] ease-[cubic-bezier(0.16,1,0.3,1)] group-hover:scale-[1.04]">
              <RandomEventImage seed={event.id} priority={priority} />
            </div>
            {/* Weighs the photo down so the stub below reads as the same object */}
            <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-card to-transparent" />
          </div>

          {/* Tear line */}
          <div className="relative">
            <div className="notch notch-l top-1/2 -translate-y-1/2" />
            <div className="notch notch-r top-1/2 -translate-y-1/2" />
            <hr className="perf mx-4" />
          </div>

          {/* Printed detail */}
          <div className="space-y-3 p-4 pt-4">
            <h3 className="line-clamp-2 text-[0.9375rem] font-semibold leading-snug tracking-tight text-foreground transition-colors duration-150 group-hover:text-primary">
              {event.name}
            </h3>

            <div className="space-y-1.5">
              {start && (
                <time
                  dateTime={event.start}
                  className="block text-[0.6875rem] font-medium uppercase tracking-[0.08em] text-primary"
                >
                  {format(start, "EEE d MMM")}
                  {end && !sameDay && <>{" to "}{format(end, "d MMM")}</>}
                  <span className="text-muted-foreground"> · {format(start, "HH:mm")}</span>
                </time>
              )}

              {event.venue && (
                <p className="truncate text-xs leading-relaxed text-muted-foreground">
                  {event.venue}
                </p>
              )}
            </div>
          </div>
        </article>
      </Link>
    </motion.div>
  )
}

export default EventCard
