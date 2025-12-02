"use client"

import type React from "react"

import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import type { PublishedEventDetails, PublishedEventTicketTypeDetails } from "@/domain/domain"
import { getPublishedEvent } from "@/lib/api"
import { AlertCircle } from "lucide-react"
import { useEffect, useState } from "react"
import { useAuth } from "react-oidc-context"
import { useParams } from "react-router"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"
import EventHero from "@/components/events/event-hero"
import TicketSelector from "@/components/events/ticket-selector"
import { motion } from "framer-motion"
import { Skeleton } from "@/components/common/loading-skeleton"

const PublishedEventsPage: React.FC = () => {
  const { isLoading: isAuthLoading } = useAuth()
  const { id } = useParams()
  const [error, setError] = useState<string | undefined>()
  const [isLoading, setIsLoading] = useState(true)
  const [publishedEvent, setPublishedEvent] = useState<PublishedEventDetails | undefined>()
  const [selectedTicketType, setSelectedTicketType] = useState<PublishedEventTicketTypeDetails | undefined>()

  useEffect(() => {
    if (!id) {
      setError("Event ID must be provided!")
      return
    }

    const fetchEvent = async () => {
      setIsLoading(true)
      try {
        const eventData = await getPublishedEvent(id)
        setPublishedEvent(eventData)
        if (eventData.ticketTypes.length > 0) {
          setSelectedTicketType(eventData.ticketTypes[0])
        }
      } catch (err) {
        if (err instanceof Error) {
          setError(err.message)
        } else if (typeof err === "string") {
          setError(err)
        } else {
          setError("An unknown error has occurred")
        }
      } finally {
        setIsLoading(false)
      }
    }

    fetchEvent()
  }, [id])

  if (error) {
    return (
      <PageContainer>
        <Navbar />
        <div className="container mx-auto px-4 pt-24">
          <Alert variant="destructive" className="glass border-destructive/50">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>Error</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        </div>
      </PageContainer>
    )
  }

  if (isLoading || isAuthLoading) {
    return (
      <PageContainer>
        <Navbar />
        <div className="pt-24">
          <Skeleton className="h-[50vh] w-full rounded-none" />
          <div className="container mx-auto px-4 lg:px-8 py-12">
            <div className="grid lg:grid-cols-3 gap-8">
              <div className="lg:col-span-2 space-y-4">
                <Skeleton className="h-8 w-3/4" />
                <Skeleton className="h-4 w-full" />
                <Skeleton className="h-4 w-2/3" />
              </div>
              <div className="space-y-4">
                <Skeleton className="h-40 w-full rounded-xl" />
                <Skeleton className="h-40 w-full rounded-xl" />
              </div>
            </div>
          </div>
        </div>
      </PageContainer>
    )
  }

  return (
    <PageContainer>
      <Navbar />

      {/* Hero Section */}
      {publishedEvent && (
        <EventHero
          name={publishedEvent.name}
          venue={publishedEvent.venue}
          start={publishedEvent.start}
          end={publishedEvent.end}
        />
      )}

      {/* Content */}
      <section className="py-12">
        <div className="container mx-auto px-4 lg:px-8">
          <div className="grid lg:grid-cols-3 gap-8">
            {/* Left Column - Event Info */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.2 }}
              className="lg:col-span-2 space-y-8"
            >
              <div className="glass rounded-2xl p-6">
                <h2 className="text-xl font-semibold text-foreground mb-4">About This Event</h2>
                <p className="text-muted-foreground leading-relaxed">
                  Join us for an unforgettable experience at {publishedEvent?.name}. This event promises to deliver an
                  amazing time for all attendees. Don't miss out on this incredible opportunity to be part of something
                  special.
                </p>
              </div>

              <div className="glass rounded-2xl p-6">
                <h2 className="text-xl font-semibold text-foreground mb-4">Venue</h2>
                <div className="flex items-start gap-4 p-4 rounded-xl bg-secondary/50 border border-border/30">
                  <div className="w-12 h-12 rounded-xl gradient-primary flex items-center justify-center flex-shrink-0">
                    <svg className="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z" />
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 11a3 3 0 11-6 0 3 3 0 016 0z" />
                    </svg>
                  </div>
                  <div>
                    <p className="text-foreground font-medium mb-1">Event Location</p>
                    <p className="text-muted-foreground">{publishedEvent?.venue}</p>
                  </div>
                </div>
              </div>
            </motion.div>

            {/* Right Column - Ticket Selection */}
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              className="lg:sticky lg:top-24 h-fit"
            >
              <div className="glass rounded-2xl p-6">
                {publishedEvent && publishedEvent.ticketTypes.length > 0 ? (
                  <TicketSelector
                    ticketTypes={publishedEvent.ticketTypes}
                    selectedTicketType={selectedTicketType}
                    onSelect={setSelectedTicketType}
                    eventId={publishedEvent.id}
                  />
                ) : (
                  <div className="text-center py-8">
                    <p className="text-muted-foreground">No tickets available at this time.</p>
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
