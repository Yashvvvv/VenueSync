"use client"

import type React from "react"

import { motion } from "framer-motion"
import { type LucideIcon, Calendar, Ticket, Search, Sparkles } from "lucide-react"
import { Button } from "../ui/button"
import { Link } from "react-router"

interface EmptyStateProps {
  icon?: LucideIcon
  title: string
  description: string
  actionLabel?: string
  actionHref?: string
  onAction?: () => void
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon: Icon = Calendar,
  title,
  description,
  actionLabel,
  actionHref,
  onAction,
}) => {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="flex flex-col items-center justify-center py-20 px-4 text-center"
    >
      <motion.div
        animate={{ y: [0, -8, 0] }}
        transition={{ duration: 3, repeat: Number.POSITIVE_INFINITY, ease: "easeInOut" }}
        className="relative mb-8"
      >
        <div className="absolute inset-0 w-24 h-24 rounded-3xl gradient-primary opacity-20 blur-xl" />
        <div className="relative w-24 h-24 rounded-3xl glass border border-primary/20 flex items-center justify-center">
          <Icon className="w-12 h-12 text-primary" />
        </div>
        {/* Decorative sparkles */}
        <motion.div
          animate={{ scale: [1, 1.2, 1], opacity: [0.5, 1, 0.5] }}
          transition={{ duration: 2, repeat: Number.POSITIVE_INFINITY }}
          className="absolute -top-2 -right-2"
        >
          <Sparkles className="w-5 h-5 text-primary" />
        </motion.div>
      </motion.div>

      <h3 className="text-2xl font-bold text-foreground mb-3">{title}</h3>
      <p className="text-muted-foreground max-w-md mb-8 leading-relaxed">{description}</p>

      {actionLabel && (actionHref || onAction) && (
        <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
          {actionHref ? (
            <Link to={actionHref}>
              <Button size="lg" className="gradient-primary text-white shadow-lg shadow-primary/25 px-8">
                {actionLabel}
              </Button>
            </Link>
          ) : (
            <Button
              size="lg"
              className="gradient-primary text-white shadow-lg shadow-primary/25 px-8"
              onClick={onAction}
            >
              {actionLabel}
            </Button>
          )}
        </motion.div>
      )}
    </motion.div>
  )
}

export const NoEventsFound: React.FC = () => (
  <EmptyState
    icon={Search}
    title="No Events Found"
    description="We couldn't find any events matching your criteria. Try adjusting your search or explore all available events."
    actionLabel="Browse All Events"
    actionHref="/"
  />
)

export const NoTickets: React.FC = () => (
  <EmptyState
    icon={Ticket}
    title="No Tickets Yet"
    description="You haven't purchased any tickets yet. Explore upcoming events and get your tickets to experience something amazing!"
    actionLabel="Discover Events"
    actionHref="/"
  />
)

export default EmptyState
