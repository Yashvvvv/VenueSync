"use client"

import type React from "react"

import { motion } from "framer-motion"
import { Calendar, MapPin, Clock, Share2, Heart } from "lucide-react"
import { format } from "date-fns"
import RandomEventImage from "../random-event-image"
import { useState } from "react"
import toast from "react-hot-toast"
import { parseWallClockDate } from "@/lib/date-utils"

interface EventHeroProps {
  name: string
  venue: string
  start?: string
  end?: string
}

export const EventHero: React.FC<EventHeroProps> = ({ name, venue, start, end }) => {
  const [isLiked, setIsLiked] = useState(false)

  const handleShare = async () => {
    const eventUrl = window.location.href
    const shareData = {
      title: name,
      text: `Check out this event: ${name}`,
      url: eventUrl,
    }

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
    <div className="relative min-h-[50vh] overflow-hidden">
      {/* Background Image */}
      <div className="absolute inset-0">
        <RandomEventImage />
        <div className="absolute inset-0 bg-gradient-to-t from-background via-background/80 to-background/20" />
      </div>

      {/* Content */}
      <div className="relative container mx-auto px-4 lg:px-8 pt-32 pb-12">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="max-w-3xl"
        >
          {/* Action Buttons */}
          <div className="flex gap-2 mb-6">
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={() => setIsLiked(!isLiked)}
              className={`w-10 h-10 rounded-xl glass flex items-center justify-center transition-colors ${
                isLiked ? "text-red-500" : "text-foreground hover:text-red-400"
              }`}
            >
              <Heart className={`w-5 h-5 ${isLiked ? "fill-current" : ""}`} />
            </motion.button>
            <motion.button
              whileTap={{ scale: 0.95 }}
              onClick={handleShare}
              className="w-10 h-10 rounded-xl glass flex items-center justify-center text-foreground hover:text-primary transition-colors"
            >
              <Share2 className="w-5 h-5" />
            </motion.button>
          </div>

          {/* Event Title */}
          <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-foreground mb-6 text-balance">{name}</h1>

          {/* Event Details */}
          <div className="flex flex-wrap gap-6">
            <div className="flex items-center gap-3 glass px-4 py-3 rounded-xl">
              <div className="w-10 h-10 rounded-lg bg-primary/20 flex items-center justify-center">
                <MapPin className="w-5 h-5 text-primary" />
              </div>
              <div>
                <p className="text-sm text-muted-foreground">Location</p>
                <p className="font-medium text-foreground">{venue}</p>
              </div>
            </div>

            {start && (
              <div className="flex items-center gap-3 glass px-4 py-3 rounded-xl">
                <div className="w-10 h-10 rounded-lg bg-primary/20 flex items-center justify-center">
                  <Calendar className="w-5 h-5 text-primary" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Date</p>
                  <p className="font-medium text-foreground">{format(parseWallClockDate(start)!, "EEE, MMM d, yyyy")}</p>
                </div>
              </div>
            )}

            {start && (
              <div className="flex items-center gap-3 glass px-4 py-3 rounded-xl">
                <div className="w-10 h-10 rounded-lg bg-primary/20 flex items-center justify-center">
                  <Clock className="w-5 h-5 text-primary" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">Time</p>
                  <p className="font-medium text-foreground">
                    {format(parseWallClockDate(start)!, "h:mm a")}
                    {end && ` - ${format(parseWallClockDate(end)!, "h:mm a")}`}
                  </p>
                </div>
              </div>
            )}
          </div>
        </motion.div>
      </div>
    </div>
  )
}

export default EventHero
