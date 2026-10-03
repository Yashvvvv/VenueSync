import { format } from "date-fns"
import type { PublishedEventDetails, PublishedEventTicketTypeDetails } from "@/domain/domain"
import { parseWallClockDate } from "@/lib/date-utils"

/** What a ticket type offers right now. Same rule as the Android app (Event.availabilityOf). */
export type Availability =
  | { kind: "buyable" }
  | { kind: "soldOut" }
  | { kind: "upcoming"; start?: string }
  | { kind: "ended" }

/**
 * The sales window is the server's call (salesStatus), never the visitor's clock. A missing or unknown status
 * stays buyable: a wrong "can't buy" is worse than a purchase the server refuses with a clear message.
 */
export const availabilityOf = (
  event: Pick<PublishedEventDetails, "salesStatus" | "salesStart">,
  ticketType: Pick<PublishedEventTicketTypeDetails, "soldOut">,
): Availability => {
  if (event.salesStatus === "ENDED") return { kind: "ended" }
  if (event.salesStatus === "UPCOMING") return { kind: "upcoming", start: event.salesStart }
  if (ticketType.soldOut) return { kind: "soldOut" }
  return { kind: "buyable" }
}

/** "Sold out", "On sale Fri 10 Oct, 18:00", "Sales ended"; nothing when it can be bought. */
export const availabilityLabel = (availability: Availability): string | undefined => {
  switch (availability.kind) {
    case "buyable":
      return undefined
    case "soldOut":
      return "Sold out"
    case "ended":
      return "Sales ended"
    case "upcoming": {
      const start = parseWallClockDate(availability.start)
      return start ? `On sale ${format(start, "EEE d MMM, HH:mm")}` : "Not on sale yet"
    }
  }
}
