"use client"

import type React from "react"

import type { PublishedEventDetails, PublishedEventTicketTypeDetails } from "@/domain/domain"
import { getPublishedEvent } from "@/lib/api"
import { useCallback, useEffect, useState } from "react"
import { useAuth } from "react-oidc-context"
import { Link, useParams } from "react-router"
import { format } from "date-fns"
import { motion, useReducedMotion } from "framer-motion"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"
import EventHero from "@/components/events/event-hero"
import TicketSelector from "@/components/events/ticket-selector"
import { Skeleton } from "@/components/common/loading-skeleton"
import { Button } from "@/components/ui/button"
import { ArrowLeft, CalendarBlank, MapPin, WarningCircle } from "@/components/icons"
import { parseWallClockDate } from "@/lib/date-utils"
import { downloadIcs } from "@/lib/calendar"

const EASE = [0.16, 1, 0.3, 1] as const

const PublishedEventsPage: React.FC = () => {
  const { isLoading: isAuthLoading } = useAuth()
  const { id } = useParams()
  const reduce = useReducedMotion()

  const [error, setError] = useState<string | undefined>()
  const [isLoading, setIsLoading] = useState(true)
  const [publishedEvent, setPublishedEvent] = useState<PublishedEventDetails | undefined>()
  const [selectedTicketType, setSelectedTicketType] = useState<
    PublishedEventTicketTypeDetails | undefined
  >()

  const fetchEvent = useCallback(async () => {
    if (!id) {
      setError("No event was specified.")
      setIsLoading(false)
      return
    }

    setIsLoading(true)
    setError(undefined)
    try {
      const eventData = await getPublishedEvent(id)
      setPublishedEvent(eventData)
      if (eventData.ticketTypes.length > 0) {
        setSelectedTicketType(eventData.ticketTypes[0])
      }
    } catch (err) {
      console.error("Failed to load event:", err)
      setError(
        err instanceof Error && err.message
          ? err.message
          : "We could not load this event just now.",
      )
    } finally {
      setIsLoading(false)
    }
  }, [id])

  useEffect(() => {
    fetchEvent()
  }, [fetchEvent])

  const start = publishedEvent?.start ? parseWallClockDate(publishedEvent.start) : undefined
  const end = publishedEvent?.end ? parseWallClockDate(publishedEvent.end) : undefined
  const sameDay = start && end && format(start, "yyyy-MM-dd") === format(end, "yyyy-MM-dd")

  const handleAddToCalendar = () => {
    if (!publishedEvent || !start) return
    downloadIcs({
      id: publishedEvent.id,
      name: publishedEvent.name,
      venue: publishedEvent.venue,
      start,
      end,
      url: window.location.href,
    })
  }

  if (error) {
    return (
      <PageContainer>
        <Navbar />
        <div className="mx-auto flex min-h-[60dvh] max-w-[1400px] flex-col items-center justify-center gap-5 px-5 text-center lg:px-8">
          <WarningCircle weight="fill" size={28} className="text-destructive" />
          <div>
            <h1 className="text-xl font-semibold tracking-tight text-foreground">
              This event did not load
            </h1>
            <p className="mt-2 max-w-md text-sm leading-relaxed text-muted-foreground">{error}</p>
          </div>
          <div className="flex flex-wrap justify-center gap-2.5">
            <Button onClick={fetchEvent}>Try again</Button>
            <Link to="/events">
              <Button variant="outline">Browse events</Button>
            </Link>
          </div>
        </div>
        <Footer />
      </PageContainer>
    )
  }

  if (isLoading || isAuthLoading) {
    return (
      <PageContainer>
        <Navbar />
        {/* Mirrors the loaded layout so nothing jumps when data arrives. */}
        <Skeleton className="h-[46vh] min-h-[320px] w-full rounded-none" />
        <div className="mx-auto max-w-[1400px] px-5 py-12 lg:px-8">
          <div className="grid gap-10 lg:grid-cols-12">
            <div className="space-y-4 lg:col-span-7">
              <Skeleton className="h-10 w-3/4 rounded-sm" />
              <Skeleton className="h-4 w-1/3 rounded-sm" />
              <Skeleton className="h-4 w-1/2 rounded-sm" />
            </div>
            <div className="lg:col-span-4 lg:col-start-9">
              <Skeleton className="h-72 w-full rounded-md" />
            </div>
          </div>
        </div>
        <Footer />
      </PageContainer>
    )
  }

  return (
    <PageContainer>
      <Navbar />

      {publishedEvent && (
        <EventHero
          name={publishedEvent.name}
          venue={publishedEvent.venue}
          start={publishedEvent.start}
          end={publishedEvent.end}
          seed={publishedEvent.id}
        />
      )}

      <section className="py-14 lg:py-20">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <div className="grid gap-12 lg:grid-cols-12 lg:gap-10">
            <motion.div
              initial={reduce ? false : { opacity: 0, y: 16 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1, duration: 0.5, ease: EASE }}
              className="lg:col-span-7"
            >
              <Link
                to="/events"
                className="focus-ring inline-flex items-center gap-1.5 rounded-sm text-sm text-muted-foreground transition-colors hover:text-foreground"
              >
                <ArrowLeft weight="bold" size={14} />
                All events
              </Link>

              {/*
                There is no description on PublishedEventDetails, so this
                block shows what the event record actually contains. It
                previously printed a generated blurb ("an unforgettable
                experience...") on every event, which read as the organizer's
                own words and was not.
              */}
              <h2 className="mt-9 text-[0.6875rem] font-medium uppercase tracking-[0.14em] text-muted-foreground">
                Details
              </h2>

              <dl className="mt-6">
                {start && (
                  <div className="flex items-start gap-4 border-t border-border py-5">
                    <CalendarBlank weight="fill" size={17} className="mt-0.5 shrink-0 text-primary" />
                    <div>
                      <dt className="text-sm font-medium text-foreground">
                        {format(start, "EEEE d MMMM yyyy")}
                      </dt>
                      <dd className="mt-1 font-mono text-sm tabular-nums text-muted-foreground">
                        {format(start, "HH:mm")}
                        {end && !sameDay && <> until {format(end, "EEE d MMM, HH:mm")}</>}
                        {end && sameDay && <> until {format(end, "HH:mm")}</>}
                      </dd>
                    </div>
                  </div>
                )}

                {publishedEvent?.venue && (
                  <div className="flex items-start gap-4 border-t border-border py-5">
                    <MapPin weight="fill" size={17} className="mt-0.5 shrink-0 text-primary" />
                    <div>
                      <dt className="text-sm font-medium text-foreground">
                        {publishedEvent.venue}
                      </dt>
                      <dd className="mt-1 text-sm text-muted-foreground">Venue</dd>
                    </div>
                  </div>
                )}
              </dl>

              {start && (
                <Button variant="outline" className="mt-7 gap-2" onClick={handleAddToCalendar}>
                  <CalendarBlank weight="bold" size={15} />
                  Add to calendar
                </Button>
              )}
            </motion.div>

            <motion.div
              initial={reduce ? false : { opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.18, duration: 0.5, ease: EASE }}
              className="lg:col-span-4 lg:col-start-9"
            >
              <div className="rounded-md border border-border bg-card p-5 lg:sticky lg:top-24">
                {publishedEvent && publishedEvent.ticketTypes.length > 0 ? (
                  <TicketSelector
                    ticketTypes={publishedEvent.ticketTypes}
                    selectedTicketType={selectedTicketType}
                    onSelect={setSelectedTicketType}
                    eventId={publishedEvent.id}
                  />
                ) : (
                  <div className="py-10 text-center">
                    <p className="text-sm font-medium text-foreground">No tickets on sale</p>
                    <p className="mt-1.5 text-sm text-muted-foreground">
                      Nothing is released for this event yet.
                    </p>
                  </div>
                )}
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      <Footer />
    </PageContainer>
  )
}

export default PublishedEventsPage
