"use client"

import type React from "react"

import type { PublishedEventSummary } from "@/domain/domain"
import { motion } from "framer-motion"
import { Calendar, MapPin, Heart, ArrowUpRight, Share2 } from "lucide-react"
import { format } from "date-fns"
import { Link } from "react-router"
import { useState } from "react"
import RandomEventImage from "../random-event-image"
import toast from "react-hot-toast"

interface EventCardProps {
  event: PublishedEventSummary
  index?: number
}

export const EventCard: React.FC<EventCardProps> = ({ event, index = 0 }) => {
  const [isLiked, setIsLiked] = useState(false)

  const handleLike = (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setIsLiked(!isLiked)
  }

  const handleShare = async (e: React.MouseEvent) => {
    e.preventDefault()
    e.stopPropagation()
    
    const eventUrl = `${window.location.origin}/events/${event.id}`
    const shareData = {
      title: event.name,
      text: `Check out this event: ${event.name}`,
      url: eventUrl,
    }

    try {
      // Try Web Share API first (works on mobile and some desktop browsers)
      if (navigator.share && navigator.canShare?.(shareData)) {
        await navigator.share(shareData)
        toast.success("Shared successfully!")
      } else {
        // Fallback: copy link to clipboard
        await navigator.clipboard.writeText(eventUrl)
        toast.success("Link copied to clipboard!")
      }
    } catch (error) {
      // User cancelled share or error occurred
      if ((error as Error).name !== "AbortError") {
        // If clipboard also fails, show error
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
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.08, duration: 0.4 }}
      whileHover={{ y: -6 }}
      className="group"
    >
      <Link to={`/events/${event.id}`}>
        <div className="glass rounded-2xl overflow-hidden card-hover">
          {/* Image Container */}
          <div className="relative h-52 overflow-hidden">
            <motion.div className="absolute inset-0" whileHover={{ scale: 1.05 }} transition={{ duration: 0.4 }}>
              <RandomEventImage />
            </motion.div>
            <div className="absolute inset-0 bg-gradient-to-t from-background via-background/30 to-transparent" />

            {event.start && (
              <motion.div
                initial={{ opacity: 0, x: -10 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: index * 0.08 + 0.2 }}
                className="absolute top-4 left-4 glass-strong rounded-xl px-3 py-2 text-center min-w-[52px]"
              >
                <p className="text-xs font-bold text-primary uppercase tracking-wider">
                  {format(new Date(event.start), "MMM")}
                </p>
                <p className="text-xl font-bold text-foreground leading-none mt-0.5">
                  {format(new Date(event.start), "dd")}
                </p>
              </motion.div>
            )}

            <div className="absolute top-4 right-4 flex gap-2 opacity-0 group-hover:opacity-100 transition-all duration-300 translate-y-2 group-hover:translate-y-0">
              <motion.button
                whileTap={{ scale: 0.9 }}
                onClick={handleShare}
                className="w-10 h-10 rounded-xl glass-strong flex items-center justify-center text-foreground hover:text-primary transition-colors"
                aria-label="Share event"
              >
                <Share2 className="w-5 h-5" />
              </motion.button>
              <motion.button
                whileTap={{ scale: 0.9 }}
                onClick={handleLike}
                className={`w-10 h-10 rounded-xl glass-strong flex items-center justify-center transition-colors ${
                  isLiked ? "text-red-500" : "text-foreground hover:text-red-400"
                }`}
                aria-label={isLiked ? "Remove from favorites" : "Add to favorites"}
              >
                <Heart className={`w-5 h-5 ${isLiked ? "fill-current" : ""}`} />
              </motion.button>
            </div>

            <motion.div
              initial={{ opacity: 0, scale: 0.5 }}
              whileHover={{ opacity: 1, scale: 1 }}
              className="absolute bottom-4 right-4 w-10 h-10 rounded-xl gradient-primary flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity shadow-lg shadow-primary/30"
            >
              <ArrowUpRight className="w-5 h-5 text-white" />
            </motion.div>
          </div>

          {/* Content */}
          <div className="p-5 space-y-3">
            <h3 className="font-semibold text-foreground group-hover:text-primary transition-colors line-clamp-2 text-lg leading-tight">
              {event.name}
            </h3>

            <div className="space-y-2">
              <div className="flex items-center gap-2.5 text-sm text-muted-foreground">
                <div className="w-8 h-8 rounded-lg bg-secondary/50 flex items-center justify-center flex-shrink-0">
                  <MapPin className="w-4 h-4 text-muted-foreground" />
                </div>
                <span className="truncate">{event.venue}</span>
              </div>

              {event.start && event.end && (
                <div className="flex items-center gap-2.5 text-sm text-muted-foreground">
                  <div className="w-8 h-8 rounded-lg bg-secondary/50 flex items-center justify-center flex-shrink-0">
                    <Calendar className="w-4 h-4 text-muted-foreground" />
                  </div>
                  <span>
                    {format(new Date(event.start), "EEE, MMM d")} - {format(new Date(event.end), "MMM d")}
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
