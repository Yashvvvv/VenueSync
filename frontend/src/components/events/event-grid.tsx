import type React from "react"
import type { PublishedEventSummary } from "@/domain/domain"
import EventCard from "./event-card"
import { EventCardSkeleton } from "../common/loading-skeleton"
import { NoEventsFound } from "../common/empty-state"

interface EventGridProps {
  events: PublishedEventSummary[]
  isLoading?: boolean
  /** Active search term, so the empty state can tell "no match" from
      "nothing published yet". Omit when nothing was searched. */
  query?: string
}

export const EventGrid: React.FC<EventGridProps> = ({ events, isLoading = false, query }) => {
  const grid =
    "grid grid-cols-1 gap-x-5 gap-y-8 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4"

  if (isLoading) {
    return (
      <div className={grid}>
        {Array.from({ length: 8 }).map((_, i) => (
          <EventCardSkeleton key={i} />
        ))}
      </div>
    )
  }

  if (!events || events.length === 0) {
    return <NoEventsFound query={query} />
  }

  return (
    <div className={grid}>
      {events.map((event, index) => (
        <EventCard key={event.id} event={event} index={index} priority={index < 4} />
      ))}
    </div>
  )
}

export default EventGrid
