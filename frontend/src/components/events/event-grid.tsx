import type React from "react"
import type { PublishedEventSummary } from "@/domain/domain"
import EventCard from "./event-card"
import { EventCardSkeleton } from "../common/loading-skeleton"
import { NoEventsFound } from "../common/empty-state"

interface EventGridProps {
  events: PublishedEventSummary[]
  isLoading?: boolean
  emptyMessage?: string
}

export const EventGrid: React.FC<EventGridProps> = ({ events, isLoading = false, emptyMessage }) => {
  if (isLoading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {Array.from({ length: 8 }).map((_, i) => (
          <EventCardSkeleton key={i} />
        ))}
      </div>
    )
  }

  if (!events || events.length === 0) {
    return <NoEventsFound />
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
      {events.map((event, index) => (
        <EventCard key={event.id} event={event} index={index} />
      ))}
    </div>
  )
}

export default EventGrid
