"use client"

import type React from "react"

import type { PublishedEventTicketTypeDetails } from "@/domain/domain"
import { motion } from "framer-motion"
import { Check, Ticket, Info, CalendarBlank, ArrowRight } from "@/components/icons"
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

  if (isRolesLoading) return null

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-2 mb-4">
        <Ticket weight="fill" size={16} className="text-primary" />
        <h3 className="font-semibold text-foreground text-sm">Select Ticket Type</h3>
      </div>

      <div className="space-y-2">
        {ticketTypes.map((ticketType, index) => {
          const isSelected = selectedTicketType?.id === ticketType.id

          return (
            <motion.button
              key={ticketType.id}
              initial={{ opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.07, duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
              onClick={() => onSelect(ticketType)}
              className={`w-full p-4 rounded-xl text-left transition-all duration-150 border ${
                isSelected
                  ? "border-primary/50 bg-primary/[0.07]"
                  : "border-border/40 bg-card/30 hover:border-border/70 hover:bg-card/50"
              }`}
            >
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-0.5">
                    {/* Selection indicator */}
                    <div
                      className={`w-4 h-4 rounded-full border-2 flex items-center justify-center flex-shrink-0 transition-all ${
                        isSelected ? "border-primary bg-primary" : "border-border/60"
                      }`}
                    >
                      {isSelected && <Check weight="bold" size={10} color="white" />}
                    </div>
                    <span className="font-medium text-foreground text-sm">{ticketType.name}</span>
                  </div>
                  {ticketType.description && (
                    <p className="text-xs text-muted-foreground mt-1 pl-6 leading-relaxed">{ticketType.description}</p>
                  )}
                </div>
                <span className={`text-lg font-bold flex-shrink-0 ${isSelected ? "text-primary" : "text-foreground"}`}>
                  ${ticketType.price}
                </span>
              </div>
            </motion.button>
          )
        })}
      </div>

      {selectedTicketType && (
        <motion.div
          initial={{ opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="pt-4 border-t border-border/30 space-y-4"
        >
          <div className="flex items-center justify-between text-sm">
            <span className="text-muted-foreground">Total</span>
            <span className="text-xl font-bold text-foreground tracking-tight">${selectedTicketType.price}</span>
          </div>

          {isAttendee && (
            <Link to={`/events/${eventId}/purchase/${selectedTicketType.id}`}>
              <Button className="w-full gradient-primary text-white h-11 font-semibold shadow-md shadow-primary/20">
                Get Tickets
                <ArrowRight weight="bold" size={16} className="ml-2" />
              </Button>
            </Link>
          )}

          {isOrganizer && (
            <div className="flex items-start gap-2.5 p-3 rounded-lg bg-blue-500/[0.06] border border-blue-500/20">
              <CalendarBlank weight="fill" size={15} className="text-blue-400 flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-xs font-medium text-blue-400">Organizer Account</p>
                <p className="text-xs text-blue-400/70 mt-0.5 leading-relaxed">
                  Switch to an attendee account to purchase tickets.
                </p>
              </div>
            </div>
          )}

          {isStaff && (
            <div className="flex items-start gap-2.5 p-3 rounded-lg bg-primary/[0.06] border border-primary/20">
              <Info weight="fill" size={15} className="text-primary flex-shrink-0 mt-0.5" />
              <div>
                <p className="text-xs font-medium text-primary">Staff Account</p>
                <p className="text-xs text-primary/70 mt-0.5 leading-relaxed">
                  Use the QR validation page to scan attendee tickets.
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
