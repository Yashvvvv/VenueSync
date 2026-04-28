"use client"

import type React from "react"

import type { PublishedEventTicketTypeDetails } from "@/domain/domain"
import { motion } from "framer-motion"
import { Check, Ticket, AlertCircle, Calendar } from "lucide-react"
import { Button } from "../ui/button"
import { Link } from "react-router"
import { useRoles } from "@/hooks/use-roles"

interface TicketSelectorProps {
  ticketTypes: PublishedEventTicketTypeDetails[]
  selectedTicketType: PublishedEventTicketTypeDetails | undefined
  onSelect: (ticketType: PublishedEventTicketTypeDetails) => void
  eventId: string
}

export const TicketSelector: React.FC<TicketSelectorProps> = ({
  ticketTypes,
  selectedTicketType,
  onSelect,
  eventId,
}) => {
  const { isLoading: isRolesLoading, isOrganizer, isAttendee, isStaff } = useRoles()

  if (isRolesLoading) {
    return null
  }

  return (
    <div className="space-y-4">
      <h3 className="text-lg font-semibold text-foreground flex items-center gap-2">
        <Ticket className="w-5 h-5 text-primary" />
        Select Tickets
      </h3>

      <div className="space-y-3">
        {ticketTypes.map((ticketType, index) => {
          const isSelected = selectedTicketType?.id === ticketType.id

          return (
            <motion.button
              key={ticketType.id}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ delay: index * 0.1 }}
              onClick={() => onSelect(ticketType)}
              className={`w-full p-4 rounded-xl text-left transition-all ${
                isSelected ? "glass border-primary bg-primary/10" : "glass hover:border-primary/30"
              }`}
            >
              <div className="flex items-center justify-between">
                <div className="flex-1">
                  <div className="flex items-center gap-2">
                    <h4 className="font-medium text-foreground">{ticketType.name}</h4>
                    {isSelected && (
                      <motion.div
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        className="w-5 h-5 rounded-full bg-primary flex items-center justify-center"
                      >
                        <Check className="w-3 h-3 text-white" />
                      </motion.div>
                    )}
                  </div>
                  <p className="text-sm text-muted-foreground mt-1">{ticketType.description}</p>
                </div>
                <div className="text-right ml-4">
                  <p className="text-xl font-bold text-primary">${ticketType.price}</p>
                </div>
              </div>
            </motion.button>
          )
        })}
      </div>

      {selectedTicketType && (
        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          className="pt-4 border-t border-border"
        >
          <div className="flex items-center justify-between mb-4">
            <span className="text-muted-foreground">Total</span>
            <span className="text-2xl font-bold text-foreground">${selectedTicketType.price}</span>
          </div>

          {isAttendee && (
            <Link to={`/events/${eventId}/purchase/${selectedTicketType.id}`}>
              <Button className="w-full gradient-primary text-white h-12 text-lg font-semibold">Get Tickets</Button>
            </Link>
          )}

          {isOrganizer && (
            <div className="flex items-start gap-3 p-4 rounded-lg bg-blue-500/10 border border-blue-500/30">
              <Calendar className="w-5 h-5 text-blue-400 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <p className="text-sm font-medium text-blue-400">Manage Events</p>
                <p className="text-xs text-blue-400/80 mt-1">
                  As an organizer, you create and manage events. Attend events and purchase tickets by switching to an attendee account.
                </p>
              </div>
            </div>
          )}

          {isStaff && (
            <div className="flex items-start gap-3 p-4 rounded-lg bg-purple-500/10 border border-purple-500/30">
              <AlertCircle className="w-5 h-5 text-purple-400 flex-shrink-0 mt-0.5" />
              <div className="flex-1">
                <p className="text-sm font-medium text-purple-400">Validate Tickets</p>
                <p className="text-xs text-purple-400/80 mt-1">
                  As staff, you validate tickets at events. Visit the QR validation page to scan attendee tickets.
                </p>
              </div>
            </div>
          )}
        </motion.div>
      )}
    </div>
  )
}

export default TicketSelector
