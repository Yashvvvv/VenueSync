"use client"

import type React from "react"

import { type TicketSummary, TicketStatus } from "@/domain/domain"
import { motion } from "framer-motion"
import { Ticket, ChevronRight, QrCode, Calendar, CheckCircle, XCircle, Clock } from "lucide-react"
import { Link } from "react-router"
import { format } from "date-fns"
import { parseWallClockDate } from "@/lib/date-utils"

interface TicketCardProps {
  ticket: TicketSummary
  index?: number
}

const statusConfig: Record<TicketStatus, { label: string; dotColor: string; textColor: string; icon: React.ReactNode }> = {
  [TicketStatus.PURCHASED]: {
    label: "Active",
    dotColor: "bg-emerald-500",
    textColor: "text-emerald-400",
    icon: <Ticket className="w-3 h-3" />,
  },
  [TicketStatus.USED]: {
    label: "Used",
    dotColor: "bg-blue-400",
    textColor: "text-blue-400",
    icon: <CheckCircle className="w-3 h-3" />,
  },
  [TicketStatus.EXPIRED]: {
    label: "Expired",
    dotColor: "bg-yellow-400",
    textColor: "text-yellow-400",
    icon: <Clock className="w-3 h-3" />,
  },
  [TicketStatus.CANCELLED]: {
    label: "Cancelled",
    dotColor: "bg-red-400",
    textColor: "text-red-400",
    icon: <XCircle className="w-3 h-3" />,
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
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: index * 0.07, duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
    >
      <Link to={`/dashboard/tickets/${ticket.id}`} className="group block">
        <div
          className={`relative rounded-xl border transition-all duration-200 overflow-hidden ${
            isPast
              ? "border-border/30 bg-card/30 opacity-70"
              : "border-border/50 bg-card/50 hover:border-primary/30 hover:bg-card/70"
          }`}
        >
          {/* Left accent stripe */}
          <div
            className={`absolute left-0 top-0 bottom-0 w-1 rounded-l-xl ${isPast ? "bg-border/30" : "gradient-primary"}`}
          />

          <div className="flex items-center gap-4 px-5 py-4 pl-6">
            {/* Icon */}
            <div
              className={`w-12 h-12 rounded-xl flex items-center justify-center flex-shrink-0 relative ${
                isPast ? "bg-secondary/50" : "gradient-primary shadow-md shadow-primary/20"
              }`}
            >
              <Ticket className={`w-6 h-6 ${isPast ? "text-muted-foreground" : "text-white"}`} />
              <div
                className={`absolute -bottom-1 -right-1 w-5 h-5 rounded-lg bg-background border border-border flex items-center justify-center ${
                  isPast ? "opacity-40" : ""
                }`}
              >
                <QrCode className={`w-3 h-3 ${isPast ? "text-muted-foreground" : "text-primary"}`} />
              </div>
            </div>

            {/* Content */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 mb-0.5">
                <h3 className="font-semibold text-foreground truncate text-sm group-hover:text-primary transition-colors duration-150">
                  {ticket.eventName}
                </h3>
                {/* Status dot + label */}
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
                    <Calendar className="w-3 h-3" />
                    {format(parseWallClockDate(ticket.eventStart)!, "MMM d, yyyy")}
                  </span>
                )}
                <span className="font-mono text-muted-foreground/60">#{ticket.id.slice(0, 8)}</span>
              </div>
            </div>

            {/* Arrow */}
            <ChevronRight className="w-4 h-4 text-muted-foreground/40 group-hover:text-primary/60 transition-colors duration-150 flex-shrink-0" />
          </div>
        </div>
      </Link>
    </motion.div>
  )
}

export default TicketCard
