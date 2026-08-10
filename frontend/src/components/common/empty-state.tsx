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
      className="flex flex-col items-center justify-center px-4 py-24 text-center"
    >
      {/* An unpunched stub: the shape of the thing that is missing */}
      <div className="relative mb-7 w-40 rounded-md border border-dashed border-border bg-card/40">
        <div className="flex h-16 items-center justify-center">
          <Icon weight="regular" size={22} className="text-muted-foreground/50" />
        </div>
        <hr className="perf mx-3" />
        <div className="h-8" />
      </div>

      <h3 className="mb-2 text-lg font-semibold tracking-tight text-foreground">{title}</h3>
      <p className="mb-8 max-w-sm text-sm leading-relaxed text-muted-foreground">{description}</p>

      {actionLabel && (actionHref || onAction) && (
        <>
          {actionHref ? (
            <Link to={actionHref}>
              <Button size="sm" className="btn-press px-6">
                {actionLabel}
              </Button>
            </Link>
          ) : (
            <Button size="sm" className="btn-press px-6" onClick={onAction}>
              {actionLabel}
            </Button>
          )}
        </>
      )}
    </motion.div>
  )
}

/**
 * Two different situations, two different messages.
 *
 * Showing "nothing matched that, try a shorter term" when the visitor never
 * typed anything reads as a broken search, or a broken site. It is the
 * difference between "your query found nothing" and "there is nothing on
 * sale yet", and only the first one is the reader's problem to solve.
 */
export const NoEventsFound: React.FC<{ query?: string }> = ({ query }) =>
  query ? (
    <EmptyState
      icon={MagnifyingGlass}
      title="Nothing matched that"
      description={`No events came back for "${query}". Try a shorter term, or a city name on its own.`}
      actionLabel="Show all events"
      actionHref="/events"
    />
  ) : (
    <EmptyState
      icon={CalendarDots}
      title="Nothing on sale right now"
      description="No events are published yet. New ones appear here as soon as an organizer puts them on sale."
      actionLabel="Host an event"
      actionHref="/organizers"
    />
  )

export const NoTickets: React.FC = () => (
  <EmptyState
    icon={Ticket}
    title="No tickets yet"
    description="Tickets you buy show up here, with the code you scan at the door."
    actionLabel="Browse events"
    actionHref="/"
  />
)

export default EmptyState
