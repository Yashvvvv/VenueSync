"use client"

import type React from "react"

import { motion } from "framer-motion"
import { type LucideIcon, Calendar, Ticket, Search } from "lucide-react"
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
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
      className="flex flex-col items-center justify-center py-20 px-4 text-center"
    >
      {/* Icon container — clean, minimal */}
      <div className="w-16 h-16 rounded-2xl border border-border/40 bg-card/40 flex items-center justify-center mb-6">
        <Icon className="w-7 h-7 text-muted-foreground/60" />
      </div>

      <h3 className="text-lg font-semibold text-foreground mb-2 tracking-tight">{title}</h3>
      <p className="text-sm text-muted-foreground max-w-sm mb-8 leading-relaxed">{description}</p>

      {actionLabel && (actionHref || onAction) && (
        <>
          {actionHref ? (
            <Link to={actionHref}>
              <Button size="sm" className="gradient-primary text-white shadow-md shadow-primary/20 px-6">
                {actionLabel}
              </Button>
            </Link>
          ) : (
            <Button
              size="sm"
              className="gradient-primary text-white shadow-md shadow-primary/20 px-6"
              onClick={onAction}
            >
              {actionLabel}
            </Button>
          )}
        </>
      )}
    </motion.div>
  )
}

export const NoEventsFound: React.FC = () => (
  <EmptyState
    icon={Search}
    title="No Events Found"
    description="We couldn't find any events matching your criteria. Try adjusting your search or browse all available events."
    actionLabel="Browse All Events"
    actionHref="/"
  />
)

export const NoTickets: React.FC = () => (
  <EmptyState
    icon={Ticket}
    title="No Tickets Yet"
    description="You haven't purchased any tickets yet. Explore upcoming events and get your tickets to experience something amazing."
    actionLabel="Discover Events"
    actionHref="/"
  />
)

export default EmptyState
