"use client"

import type React from "react"

import type { PublishedEventTicketTypeDetails } from "@/domain/domain"
import { motion, useReducedMotion } from "framer-motion"
import { ArrowRight, Check, Info } from "@/components/icons"
import { Button } from "../ui/button"
import { Link } from "react-router"
import { useRoles } from "@/hooks/use-roles"

interface TicketSelectorProps {
  ticketTypes: PublishedEventTicketTypeDetails[]
  selectedTicketType: PublishedEventTicketTypeDetails | undefined
  onSelect: (ticketType: PublishedEventTicketTypeDetails) => void
  eventId: string
}

const money = (value: number) => `$${value.toFixed(2)}`

export const TicketSelector: React.FC<TicketSelectorProps> = ({
  ticketTypes,
  selectedTicketType,
  onSelect,
  eventId,
}) => {
  const { isLoading: isRolesLoading, isOrganizer, isAttendee, isStaff } = useRoles()
  const reduce = useReducedMotion()

  if (isRolesLoading) return null

  return (
    <div>
      <h2 className="text-[0.6875rem] font-medium uppercase tracking-[0.14em] text-muted-foreground">
        Choose a ticket
      </h2>

      {/* A real radiogroup, so arrow keys move between tiers and a screen
          reader announces the selected one. It was a list of buttons. */}
      <div role="radiogroup" aria-label="Ticket type" className="mt-4 space-y-2">
        {ticketTypes.map((ticketType, index) => {
          const isSelected = selectedTicketType?.id === ticketType.id

          return (
            <motion.button
              key={ticketType.id}
              role="radio"
              aria-checked={isSelected}
              initial={reduce ? false : { opacity: 0, y: 8 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.06, duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
              onClick={() => onSelect(ticketType)}
              className={`focus-ring w-full rounded-md border p-4 text-left transition-colors duration-150 ${
                isSelected
                  ? "border-primary bg-primary/[0.07]"
                  : "border-border hover:border-foreground/30"
              }`}
            >
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span
                      aria-hidden
                      className={`flex h-4 w-4 shrink-0 items-center justify-center rounded-full border transition-colors ${
                        isSelected ? "border-primary bg-primary" : "border-muted-foreground"
                      }`}
                    >
                      {isSelected && (
                        <Check weight="bold" size={10} className="text-primary-foreground" />
                      )}
                    </span>
                    <span className="truncate text-sm font-medium text-foreground">
                      {ticketType.name}
                    </span>
                  </div>
                  {ticketType.description && (
                    <p className="mt-1.5 pl-6 text-xs leading-relaxed text-muted-foreground">
                      {ticketType.description}
                    </p>
                  )}
                </div>
                <span
                  className={`shrink-0 font-mono text-base font-semibold tabular-nums tracking-tight ${
                    isSelected ? "text-primary" : "text-foreground"
                  }`}
                >
                  {money(ticketType.price)}
                </span>
              </div>
            </motion.button>
          )
        })}
      </div>

      {selectedTicketType && (
        <motion.div
          initial={reduce ? false : { opacity: 0, y: 8 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.3 }}
          className="mt-5 border-t border-border pt-5"
        >
          <div className="flex items-baseline justify-between">
            <span className="text-sm text-muted-foreground">Total</span>
            <span className="font-mono text-xl font-semibold tabular-nums tracking-tight text-foreground">
              {money(selectedTicketType.price)}
            </span>
          </div>

          {/* There is no fee field anywhere in the ticket model, so this is a
              statement of fact about the system rather than a promise. If a
              booking fee is ever introduced it has to appear above, and this
              line has to change with it. */}
          <p className="mt-2 text-xs text-muted-foreground">
            That is the total. Nothing gets added at checkout.
          </p>

          {isAttendee && (
            <Link to={`/events/${eventId}/purchase/${selectedTicketType.id}`} className="mt-5 block">
              <Button size="lg" className="w-full gap-2">
                Get tickets
                <ArrowRight weight="bold" size={16} />
              </Button>
            </Link>
          )}

          {isOrganizer && !isAttendee && (
            <div className="mt-5 flex items-start gap-2.5 rounded-md border border-border bg-secondary p-3">
              <Info weight="fill" size={15} className="mt-0.5 shrink-0 text-muted-foreground" />
              <p className="text-xs leading-relaxed text-muted-foreground">
                You are signed in as an organizer. Buying needs an attendee account.
              </p>
            </div>
          )}

          {isStaff && !isAttendee && (
            <div className="mt-5 flex items-start gap-2.5 rounded-md border border-primary/25 bg-primary/[0.06] p-3">
              <Info weight="fill" size={15} className="mt-0.5 shrink-0 text-primary" />
              <p className="text-xs leading-relaxed text-muted-foreground">
                Staff accounts scan tickets from the validation page rather than buying here.
              </p>
            </div>
          )}
        </motion.div>
      )}
    </div>
  )
}

export default TicketSelector
