"use client"

import type React from "react"

import { type TicketSummary, TicketStatus } from "@/domain/domain"
import { motion } from "framer-motion"
import { Ticket, ChevronRight, QrCode } from "lucide-react"
import { Link } from "react-router"

interface TicketCardProps {
  ticket: TicketSummary
  index?: number
}

const statusConfig: Record<TicketStatus, { label: string; className: string }> = {
  [TicketStatus.PURCHASED]: {
    label: "Active",
    className: "status-published",
  },
  [TicketStatus.CANCELLED]: {
    label: "Cancelled",
    className: "status-cancelled",
  },
}

export const TicketCard: React.FC<TicketCardProps> = ({ ticket, index = 0 }) => {
  const status = statusConfig[ticket.status]

  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: index * 0.08 }}
      whileHover={{ x: 4 }}
    >
      <Link to={`/dashboard/tickets/${ticket.id}`}>
        <div className="glass rounded-2xl p-5 hover:border-primary/30 transition-all group card-hover">
          <div className="flex items-center gap-4">
            <motion.div
              whileHover={{ rotate: 10 }}
              className="relative w-16 h-16 rounded-xl gradient-primary flex items-center justify-center flex-shrink-0 shadow-lg shadow-primary/25"
            >
              <Ticket className="w-8 h-8 text-white" />
              {/* QR indicator */}
              <div className="absolute -bottom-1 -right-1 w-6 h-6 rounded-lg bg-background border border-border flex items-center justify-center">
                <QrCode className="w-3.5 h-3.5 text-primary" />
              </div>
            </motion.div>

            {/* Content */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-3 mb-1.5">
                <h3 className="font-semibold text-foreground truncate text-lg group-hover:text-primary transition-colors">
                  {ticket.ticketType.name}
                </h3>
                <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${status.className}`}>
                  {status.label}
                </span>
              </div>
              <div className="flex items-center gap-4 text-sm">
                <span className="font-semibold text-primary">${ticket.ticketType.price.toFixed(2)}</span>
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
