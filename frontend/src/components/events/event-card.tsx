"use client"

import type React from "react"

import type { PublishedEventSummary } from "@/domain/domain"
import { motion } from "framer-motion"
import { Calendar, MapPin, ArrowUpRight } from "lucide-react"
import { format } from "date-fns"
import { Link } from "react-router"
import RandomEventImage from "../random-event-image"
import { parseWallClockDate } from "@/lib/date-utils"

interface EventCardProps {
  event: PublishedEventSummary
  index?: number
}

export const EventCard: React.FC<EventCardProps> = ({ event, index = 0 }) => {
  const parsedStart = event.start ? parseWallClockDate(event.start) : null
  const parsedEnd = event.end ? parseWallClockDate(event.end) : null

  return (
    <motion.div
      initial={{ opacity: 0, y: 16 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.06, duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
    >
      <Link to={`/events/${event.id}`} className="group block">
        <div className="rounded-2xl overflow-hidden border border-border/40 bg-card/50 card-hover">
          {/* ── Image ── */}
          <div className="relative h-[200px] overflow-hidden">
            <div className="absolute inset-0 transition-transform duration-500 ease-out group-hover:scale-[1.04]">
              <RandomEventImage />
            </div>

            {/* Gradient overlay — clean bottom fade */}
            <div className="absolute inset-0 bg-gradient-to-t from-[oklch(0.055_0.01_265)] via-[oklch(0.055_0.01_265)]/20 to-transparent" />

            {/* Date badge — top left */}
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

            {/* Arrow indicator — bottom right on hover */}
            <div className="absolute bottom-3 right-3 w-8 h-8 rounded-lg bg-primary flex items-center justify-center opacity-0 translate-y-2 group-hover:opacity-100 group-hover:translate-y-0 transition-all duration-200 shadow-lg shadow-primary/30">
              <ArrowUpRight className="w-4 h-4 text-white" />
            </div>
          </div>

          {/* ── Content ── */}
          <div className="p-4 space-y-3">
            <h3 className="font-semibold text-foreground group-hover:text-primary transition-colors duration-150 line-clamp-2 leading-snug">
              {event.name}
            </h3>

            <div className="space-y-1.5">
              {event.venue && (
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <MapPin className="w-3.5 h-3.5 flex-shrink-0" />
                  <span className="truncate">{event.venue}</span>
                </div>
              )}

              {parsedStart && parsedEnd && (
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <Calendar className="w-3.5 h-3.5 flex-shrink-0" />
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
