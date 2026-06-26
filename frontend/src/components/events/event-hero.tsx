"use client"

import type React from "react"

import { motion } from "framer-motion"
import { Calendar, MapPin, Clock, Share2 } from "lucide-react"
import { format } from "date-fns"
import RandomEventImage from "../random-event-image"
import toast from "react-hot-toast"
import { parseWallClockDate } from "@/lib/date-utils"

interface EventHeroProps {
  name: string
  venue: string
  start?: string
  end?: string
}

export const EventHero: React.FC<EventHeroProps> = ({ name, venue, start, end }) => {
  const parsedStart = start ? parseWallClockDate(start) : null
  const parsedEnd = end ? parseWallClockDate(end) : null

  const handleShare = async () => {
    const eventUrl = window.location.href
    const shareData = { title: name, text: `Check out this event: ${name}`, url: eventUrl }
    try {
      if (navigator.share && navigator.canShare?.(shareData)) {
        await navigator.share(shareData)
        toast.success("Shared successfully!")
      } else {
        await navigator.clipboard.writeText(eventUrl)
        toast.success("Link copied to clipboard!")
      }
    } catch (error) {
      if ((error as Error).name !== "AbortError") {
        try {
          await navigator.clipboard.writeText(eventUrl)
          toast.success("Link copied to clipboard!")
        } catch {
          toast.error("Failed to share event")
        }
      }
    }
  }

  return (
    <div className="relative min-h-[55vh] overflow-hidden">
      {/* Full-bleed background image */}
      <div className="absolute inset-0">
        <div className="absolute inset-0 w-full h-full">
          <RandomEventImage />
        </div>
        {/* Multi-layer gradient for editorial text legibility */}
        <div
          className="absolute inset-0"
          style={{
            background:
              "linear-gradient(to top, oklch(0.055 0.01 265) 0%, oklch(0.055 0.01 265 / 0.85) 30%, oklch(0.055 0.01 265 / 0.4) 60%, oklch(0.055 0.01 265 / 0.15) 100%)",
          }}
        />
      </div>

      {/* Content — anchored to bottom of hero */}
      <div className="relative container mx-auto px-4 lg:px-8 pt-40 pb-10 flex flex-col justify-end min-h-[55vh]">
        <motion.div
          initial={{ opacity: 0, y: 24 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
          className="max-w-3xl"
        >
          {/* Share button */}
          <motion.button
            whileTap={{ scale: 0.95 }}
            onClick={handleShare}
            className="mb-5 flex items-center gap-2 text-sm text-white/70 hover:text-white border border-white/20 hover:border-white/40 rounded-full px-4 py-1.5 transition-all backdrop-blur-sm bg-white/5"
          >
            <Share2 className="w-3.5 h-3.5" />
            Share Event
          </motion.button>

          {/* Event Title */}
          <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-white mb-6 tracking-tight text-balance leading-[1.05]">
            {name}
          </h1>

          {/* Meta pills */}
          <div className="flex flex-wrap gap-3">
            <div className="flex items-center gap-2 bg-white/[0.08] backdrop-blur-md border border-white/[0.12] rounded-full px-4 py-2">
              <MapPin className="w-4 h-4 text-white/60" />
              <span className="text-sm font-medium text-white">{venue}</span>
            </div>

            {parsedStart && (
              <div className="flex items-center gap-2 bg-white/[0.08] backdrop-blur-md border border-white/[0.12] rounded-full px-4 py-2">
                <Calendar className="w-4 h-4 text-white/60" />
                <span className="text-sm font-medium text-white">
                  {format(parsedStart, "EEE, MMM d, yyyy")}
                </span>
              </div>
            )}

            {parsedStart && (
              <div className="flex items-center gap-2 bg-white/[0.08] backdrop-blur-md border border-white/[0.12] rounded-full px-4 py-2">
                <Clock className="w-4 h-4 text-white/60" />
                <span className="text-sm font-medium text-white">
                  {format(parsedStart, "h:mm a")}
                  {parsedEnd && ` — ${format(parsedEnd, "h:mm a")}`}
                </span>
              </div>
            )}
          </div>
        </motion.div>
      </div>
    </div>
  )
}

export default EventHero
