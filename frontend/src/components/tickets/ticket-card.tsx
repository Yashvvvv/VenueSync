"use client"

import type React from "react"

import { type TicketSummary, TicketStatus } from "@/domain/domain"
import { motion, useReducedMotion } from "framer-motion"
import { Link } from "react-router"
import { format } from "date-fns"
import { parseWallClockDate } from "@/lib/date-utils"
import { CaretRight, CheckCircle, Clock, Ticket, XCircle } from "@/components/icons"

interface TicketCardProps {
  ticket: TicketSummary
  index?: number
}

/* Status colours come from the semantic tokens, not from raw Tailwind
   palette classes, so they stay in step with the rest of the theme. */
const statusConfig: Record<
  TicketStatus,
  { label: string; className: string; Icon: typeof CheckCircle }
> = {
  [TicketStatus.PURCHASED]: {
    label: "Valid",
    className: "text-[var(--success)]",
    Icon: Ticket,
  },
  [TicketStatus.USED]: {
    label: "Used",
    className: "text-muted-foreground",
    Icon: CheckCircle,
  },
  [TicketStatus.EXPIRED]: {
    label: "Expired",
    className: "text-[var(--warning)]",
    Icon: Clock,
  },
  [TicketStatus.CANCELLED]: {
    label: "Cancelled",
    className: "text-destructive",
    Icon: XCircle,
  },
}

/**
 * A ticket rendered the way a ticket is actually printed: a counterfoil on
 * the left holding the price and serial, a vertical perforation with a
 * punched hole at each end, and the event detail on the body.
 */
export const TicketCard: React.FC<TicketCardProps> = ({ ticket, index = 0 }) => {
  const reduce = useReducedMotion()
  const eventEnd = ticket.eventEnd ? parseWallClockDate(ticket.eventEnd) : null
  const isEventEnded = eventEnd ? eventEnd < new Date() : false

  const displayStatus =
    ticket.status === TicketStatus.PURCHASED && isEventEnded ? TicketStatus.EXPIRED : ticket.status

  const status = statusConfig[displayStatus]
  const StatusIcon = status.Icon
  const isSpent = displayStatus !== TicketStatus.PURCHASED

  return (
    <motion.div
      initial={reduce ? false : { opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: Math.min(index, 8) * 0.05, duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
    >
      <Link to={`/dashboard/tickets/${ticket.id}`} className="group block focus-ring rounded-md">
        <article
          className={`card-hover relative flex items-stretch rounded-md border bg-card ${
            isSpent ? "border-border/60 opacity-65" : "border-border"
          }`}
        >
          {/* Counterfoil */}
          <div className="flex w-[104px] shrink-0 flex-col justify-center gap-1 px-4 py-4 sm:w-[124px]">
            <p
              className={`font-mono text-base font-semibold tabular-nums tracking-tight ${
                isSpent ? "text-muted-foreground" : "text-primary"
              }`}
            >
              ${ticket.ticketType.price.toFixed(2)}
            </p>
            <p className="font-mono text-[10px] uppercase tracking-[0.1em] text-muted-foreground">
              {ticket.id.slice(0, 8)}
            </p>
          </div>

          {/* Perforation, punched top and bottom */}
          <div className="relative shrink-0 border-l border-dashed border-border" aria-hidden>
            <span className="notch -left-[10px] -top-[10px]" />
            <span className="notch -bottom-[10px] -left-[10px]" />
          </div>

          {/* Body */}
          <div className="flex min-w-0 flex-1 items-center gap-4 px-4 py-4 sm:px-5">
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2.5">
                <h3 className="truncate text-sm font-semibold tracking-tight text-foreground transition-colors duration-150 group-hover:text-primary">
                  {ticket.eventName}
                </h3>
                <span
                  className={`flex shrink-0 items-center gap-1 text-[10px] font-medium uppercase tracking-[0.08em] ${status.className}`}
                >
                  <StatusIcon weight="fill" size={11} />
                  {status.label}
                </span>
              </div>

              <p className="mt-1 truncate text-xs text-muted-foreground">
                {ticket.ticketType.name}
              </p>

              {ticket.eventStart && (
                <time
                  dateTime={ticket.eventStart}
                  className="mt-1.5 block text-[0.6875rem] uppercase tracking-[0.08em] text-muted-foreground"
                >
                  {format(parseWallClockDate(ticket.eventStart)!, "EEE d MMM yyyy · HH:mm")}
                </time>
              )}
            </div>

            <CaretRight
              weight="bold"
              size={14}
              className="shrink-0 text-muted-foreground transition-colors duration-150 group-hover:text-primary"
            />
          </div>
        </article>
      </Link>
    </motion.div>
  )
}

export default TicketCard
