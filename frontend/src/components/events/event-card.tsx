"use client"

import type React from "react"

import type { PublishedEventSummary } from "@/domain/domain"
import { motion } from "framer-motion"
import { format } from "date-fns"
import { Link } from "react-router"
import RandomEventImage from "../random-event-image"
import { parseWallClockDate } from "@/lib/date-utils"
import { MapPin, CalendarBlank, ArrowUpRight } from "@/components/icons"

interface EventCardProps {
  event: PublishedEventSummary
  index?: number
}

export const EventCard: React.FC<EventCardProps> = ({ event, index = 0 }) => {
  const parsedStart = event.start ? parseWallClockDate(event.start) : null
  const parsedEnd = event.end ? parseWallClockDate(event.end) : null

  return (
    <motion.div
      initial={{ opacity: 0, transform: "translateY(22px)" }}
      animate={{ opacity: 1, transform: "translateY(0px)" }}
      transition={{ delay: index * 0.06, duration: 0.45, ease: [0.22, 1, 0.36, 1] }}
    >
      <Link to={`/events/${event.id}`} className="group block">
        <div className="rounded-2xl overflow-hidden border border-border/40 bg-card/50 card-hover">

          {/* Image */}
          <div className="relative h-[200px] overflow-hidden">
            <div className="absolute inset-0 transition-transform duration-500 [transition-timing-function:cubic-bezier(0.22,1,0.36,1)] group-hover:scale-[1.05]">
              <RandomEventImage />
            </div>

            <div className="absolute inset-0 bg-gradient-to-t from-[oklch(0.055_0.01_265)] via-[oklch(0.055_0.01_265)]/20 to-transparent" />

            {/* Date badge */}
            {parsedStart && (
              <div className="absolute top-3 left-3 bg-background/90 backdrop-blur-md border border-border/50 rounded-xl px-3 py-2 text-center min-w-[48px]">
                <p className="text-[10px] font-semibold text-primary uppercase tracking-widest">
                  {format(parsedStart, "MMM")}
                </p>
                <p className="text-lg font-bold text-foreground leading-none mt-0.5">
                  {format(parsedStart, "dd")}
                </p>
              </div>
            )}

            {/* Arrow — hover-only, slides up from below */}
            <div className="hover-reveal absolute bottom-3 right-3 w-8 h-8 rounded-lg bg-primary flex items-center justify-center shadow-lg shadow-primary/30 opacity-0 translate-y-1.5 group-hover:opacity-100 group-hover:translate-y-0 transition-[opacity,transform] duration-200 [transition-timing-function:cubic-bezier(0.22,1,0.36,1)]">
              <ArrowUpRight weight="bold" size={16} color="white" />
            </div>
          </div>

          {/* Content */}
          <div className="p-4 space-y-3">
            <h3 className="font-semibold text-foreground group-hover:text-primary transition-colors duration-150 line-clamp-2 leading-snug">
              {event.name}
            </h3>

            <div className="space-y-1.5">
              {event.venue && (
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <MapPin weight="fill" size={13} className="flex-shrink-0 text-muted-foreground/60" />
                  <span className="truncate">{event.venue}</span>
                </div>
              )}

              {parsedStart && parsedEnd && (
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <CalendarBlank weight="fill" size={13} className="flex-shrink-0 text-muted-foreground/60" />
                  <span>
                    {format(parsedStart, "EEE, MMM d")}
                    {format(parsedStart, "MMM d") !== format(parsedEnd, "MMM d") && (
                      <> — {format(parsedEnd, "MMM d")}</>
                    )}
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>
      </Link>
    </motion.div>
  )
}

export default EventCard
