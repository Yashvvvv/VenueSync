"use client"

import type React from "react"

import { type TicketSummary, TicketStatus } from "@/domain/domain"
import { motion } from "framer-motion"
import { Link } from "react-router"
import { format } from "date-fns"
import { parseWallClockDate } from "@/lib/date-utils"
import {
  Ticket,
  CaretRight,
  QrCode,
  CalendarBlank,
  CheckCircle,
  XCircle,
  Clock,
} from "@/components/icons"

interface TicketCardProps {
  ticket: TicketSummary
  index?: number
}

const statusConfig: Record<
  TicketStatus,
  { label: string; dotColor: string; textColor: string; Icon: typeof CheckCircle; weight: "fill" | "regular" }
> = {
  [TicketStatus.PURCHASED]: {
    label: "Active",
    dotColor: "bg-emerald-500",
    textColor: "text-emerald-400",
    Icon: Ticket,
    weight: "fill",
  },
  [TicketStatus.USED]: {
    label: "Used",
    dotColor: "bg-blue-400",
    textColor: "text-blue-400",
    Icon: CheckCircle,
    weight: "fill",
  },
  [TicketStatus.EXPIRED]: {
    label: "Expired",
    dotColor: "bg-yellow-400",
    textColor: "text-yellow-400",
    Icon: Clock,
    weight: "fill",
  },
  [TicketStatus.CANCELLED]: {
    label: "Cancelled",
    dotColor: "bg-red-400",
    textColor: "text-red-400",
    Icon: XCircle,
    weight: "fill",
  },
}

export const TicketCard: React.FC<TicketCardProps> = ({ ticket, index = 0 }) => {
  const eventEnd = ticket.eventEnd ? parseWallClockDate(ticket.eventEnd) : null
  const isEventEnded = eventEnd ? eventEnd < new Date() : false

  const displayStatus =
    ticket.status === TicketStatus.PURCHASED && isEventEnded ? TicketStatus.EXPIRED : ticket.status

  const status = statusConfig[displayStatus]
  const isPast =
    displayStatus === TicketStatus.USED ||
    displayStatus === TicketStatus.EXPIRED ||
    displayStatus === TicketStatus.CANCELLED

  return (
    <motion.div
      initial={{ opacity: 0, transform: "translateY(12px)" }}
      animate={{ opacity: 1, transform: "translateY(0px)" }}
      transition={{ delay: index * 0.07, duration: 0.4, ease: [0.22, 1, 0.36, 1] }}
    >
      <Link to={`/dashboard/tickets/${ticket.id}`} className="group block">
        <div
          className={`relative rounded-xl border transition-colors duration-150 overflow-hidden ${
            isPast
              ? "border-border/30 bg-card/30 opacity-70"
              : "border-border/50 bg-card/50 hover:border-primary/30 hover:bg-card/70"
          }`}
        >
          {/* Left accent stripe */}
          <div
            className={`absolute left-0 top-0 bottom-0 w-[3px] ${
              isPast ? "bg-border/30" : "gradient-primary"
            }`}
          />

          <div className="flex items-center gap-4 px-5 py-4 pl-6">
            {/* Icon */}
            <div
              className={`w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0 relative ${
                isPast ? "bg-secondary/50" : "gradient-primary shadow-md shadow-primary/20"
              }`}
            >
              <Ticket
                weight="fill"
                size={22}
                color={isPast ? "oklch(0.55 0.012 265)" : "white"}
              />
              <div
                className={`absolute -bottom-1 -right-1 w-5 h-5 rounded-md bg-background border border-border flex items-center justify-center ${
                  isPast ? "opacity-40" : ""
                }`}
              >
                <QrCode
                  weight="bold"
                  size={11}
                  color={isPast ? "oklch(0.55 0.012 265)" : "oklch(0.68 0.19 278)"}
                />
              </div>
            </div>

            {/* Content */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-0.5">
                <h3 className="font-semibold text-foreground truncate text-sm group-hover:text-primary transition-colors duration-150">
                  {ticket.eventName}
                </h3>
                <span className={`flex items-center gap-1 text-[10px] font-semibold ${status.textColor} flex-shrink-0`}>
                  <span className={`w-1.5 h-1.5 rounded-full ${status.dotColor} inline-block`} />
                  {status.label}
                </span>
              </div>
              <p className="text-xs text-muted-foreground mb-1.5 truncate">{ticket.ticketType.name}</p>
              <div className="flex items-center gap-3 text-xs">
                <span className="font-semibold text-primary">${ticket.ticketType.price.toFixed(2)}</span>
                {ticket.eventStart && (
                  <span className="flex items-center gap-1 text-muted-foreground">
                    <CalendarBlank weight="fill" size={11} />
                    {format(parseWallClockDate(ticket.eventStart)!, "MMM d, yyyy")}
                  </span>
                )}
                <span className="font-mono text-muted-foreground/60">#{ticket.id.slice(0, 8)}</span>
              </div>
            </div>

            {/* Arrow */}
            <CaretRight
              weight="bold"
              size={14}
              className="text-muted-foreground/40 group-hover:text-primary/60 transition-colors duration-150 flex-shrink-0"
            />
          </div>
        </div>
      </Link>
    </motion.div>
  )
}

export default TicketCard
