"use client"

import type React from "react"

import { motion } from "framer-motion"
import { Button } from "../ui/button"
import { Link } from "react-router"
import { CalendarDots, Ticket, MagnifyingGlass } from "@/components/icons"

type PhosphorIconComponent = typeof CalendarDots

interface EmptyStateProps {
  icon?: PhosphorIconComponent
  title: string
  description: string
  actionLabel?: string
  actionHref?: string
  onAction?: () => void
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon: Icon = CalendarDots,
  title,
  description,
  actionLabel,
  actionHref,
  onAction,
}) => {
  return (
    <motion.div
      initial={{ opacity: 0, transform: "translateY(12px)" }}
      animate={{ opacity: 1, transform: "translateY(0px)" }}
      transition={{ duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
      className="flex flex-col items-center justify-center py-20 px-4 text-center"
    >
      <div className="w-14 h-14 rounded-2xl border border-border/40 bg-card/40 flex items-center justify-center mb-6">
        <Icon weight="regular" size={26} className="text-muted-foreground/50" />
      </div>

      <h3 className="text-lg font-semibold text-foreground mb-2 tracking-tight">{title}</h3>
      <p className="text-sm text-muted-foreground max-w-sm mb-8 leading-relaxed">{description}</p>

      {actionLabel && (actionHref || onAction) && (
        <>
          {actionHref ? (
            <Link to={actionHref}>
              <Button size="sm" className="btn-press gradient-primary text-white shadow-md shadow-primary/20 px-6">
                {actionLabel}
              </Button>
            </Link>
          ) : (
            <Button
              size="sm"
              className="btn-press gradient-primary text-white shadow-md shadow-primary/20 px-6"
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
    icon={MagnifyingGlass}
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
    description="You haven't purchased any tickets yet. Explore upcoming events and get your tickets."
    actionLabel="Discover Events"
    actionHref="/"
  />
)

export default EmptyState
