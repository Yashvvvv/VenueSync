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

const statusConfig: Record<TicketStatus, { label: string; className: string; icon: React.ReactNode }> = {
  [TicketStatus.PURCHASED]: {
    label: "Active",
    className: "status-published",
    icon: <Ticket className="w-3 h-3" />,
  },
  [TicketStatus.USED]: {
    label: "Used",
    className: "bg-blue-500/20 text-blue-400 border border-blue-500/30",
    icon: <CheckCircle className="w-3 h-3" />,
  },
  [TicketStatus.EXPIRED]: {
    label: "Expired",
    className: "bg-yellow-500/20 text-yellow-400 border border-yellow-500/30",
    icon: <Clock className="w-3 h-3" />,
  },
  [TicketStatus.CANCELLED]: {
    label: "Cancelled",
    className: "status-cancelled",
    icon: <XCircle className="w-3 h-3" />,
  },
}

export const TicketCard: React.FC<TicketCardProps> = ({ ticket, index = 0 }) => {
  const status = statusConfig[ticket.status]
  const isPast = ticket.status === TicketStatus.USED || 
                 ticket.status === TicketStatus.EXPIRED || 
                 ticket.status === TicketStatus.CANCELLED

  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.08 }}
      whileHover={{ x: 4 }}
    >
      <Link to={`/dashboard/tickets/${ticket.id}`}>
        <div className={`glass rounded-2xl p-5 hover:border-primary/30 transition-all group card-hover ${isPast ? 'opacity-75' : ''}`}>
          <div className="flex items-center gap-4">
            <motion.div
              whileHover={{ rotate: 10 }}
              className={`relative w-16 h-16 rounded-xl flex items-center justify-center flex-shrink-0 shadow-lg ${
                isPast 
                  ? 'bg-muted/50' 
                  : 'gradient-primary shadow-primary/25'
              }`}
            >
              <Ticket className={`w-8 h-8 ${isPast ? 'text-muted-foreground' : 'text-white'}`} />
              {/* QR indicator - dimmed for past tickets */}
              <div className={`absolute -bottom-1 -right-1 w-6 h-6 rounded-lg bg-background border border-border flex items-center justify-center ${isPast ? 'opacity-50' : ''}`}>
                <QrCode className={`w-3.5 h-3.5 ${isPast ? 'text-muted-foreground' : 'text-primary'}`} />
              </div>
            </motion.div>

            {/* Content */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-3 mb-1">
                <h3 className="font-semibold text-foreground truncate text-lg group-hover:text-primary transition-colors">
                  {ticket.eventName}
                </h3>
                <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium flex items-center gap-1 ${status.className}`}>
                  {status.icon}
                  {status.label}
                </span>
              </div>
              <p className="text-sm text-muted-foreground mb-1.5 truncate">
                {ticket.ticketType.name}
              </p>
              <div className="flex items-center gap-4 text-sm">
                <span className="font-semibold text-primary">${ticket.ticketType.price.toFixed(2)}</span>
                {ticket.eventStart && (
                  <span className="flex items-center gap-1 text-muted-foreground text-xs">
                    <Calendar className="w-3 h-3" />
                    {format(parseWallClockDate(ticket.eventStart)!, "MMM d, yyyy")}
                  </span>
                )}
                <span className="text-muted-foreground font-mono text-xs bg-secondary/50 px-2 py-0.5 rounded">
                  #{ticket.id.slice(0, 8)}
                </span>
              </div>
            </div>

            {/* Arrow */}
            <motion.div
              initial={{ x: 0 }}
              whileHover={{ x: 4 }}
              className="w-10 h-10 rounded-xl bg-secondary/50 flex items-center justify-center group-hover:bg-primary/10 transition-colors"
            >
              <ChevronRight className="w-5 h-5 text-muted-foreground group-hover:text-primary transition-colors" />
            </motion.div>
          </div>
        </div>
      </Link>
    </motion.div>
  )
}

export default TicketCard
