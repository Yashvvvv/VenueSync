"use client"

import type React from "react"

import { Button } from "@/components/ui/button"
import { getPublishedEvent, purchaseTicket } from "@/lib/api"
import { useCallback, useEffect, useState } from "react"
import { useAuth } from "react-oidc-context"
import { useParams, Link } from "react-router"
import { format } from "date-fns"
import { motion, useReducedMotion } from "framer-motion"
import confetti from "canvas-confetti"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"
import { Skeleton } from "@/components/common/loading-skeleton"
import type { PublishedEventDetails, PublishedEventTicketTypeDetails } from "@/domain/domain"
import { parseWallClockDate } from "@/lib/date-utils"
import { VIBE_CONFETTI } from "@/lib/vibe"
import { useAudience } from "@/hooks/use-audience"
import { ArrowLeft, CheckCircle, Info, WarningCircle } from "@/components/icons"

const money = (value: number) => `$${value.toFixed(2)}`
const EASE = [0.16, 1, 0.3, 1] as const

const PurchaseTicketPage: React.FC = () => {
  const { eventId, ticketTypeId } = useParams()
  const { isLoading: isAuthLoading, user } = useAuth()
  const reduce = useReducedMotion()
  const { vibe } = useAudience()

  const [event, setEvent] = useState<PublishedEventDetails | undefined>()
  const [tier, setTier] = useState<PublishedEventTicketTypeDetails | undefined>()
  const [isLoadingEvent, setIsLoadingEvent] = useState(true)
  const [loadError, setLoadError] = useState<string | undefined>()

  const [error, setError] = useState<string | undefined>()
  const [isPurchaseSuccess, setIsPurchaseSuccess] = useState(false)
  const [isProcessing, setIsProcessing] = useState(false)

  /* The old page asked people to confirm a purchase showing nothing but two
     ids in the URL. Loading the event means the summary can state the event,
     the tier and the amount before anyone commits. */
  const loadEvent = useCallback(async () => {
    if (!eventId || !ticketTypeId) {
      setLoadError("This checkout link is incomplete.")
      setIsLoadingEvent(false)
      return
    }
    setIsLoadingEvent(true)
    setLoadError(undefined)
    try {
      const data = await getPublishedEvent(eventId)
      const found = data.ticketTypes.find((t) => t.id === ticketTypeId)
      setEvent(data)
      setTier(found)
      if (!found) setLoadError("That ticket type is no longer on sale for this event.")
    } catch (err) {
      console.error("Failed to load event for checkout:", err)
      setLoadError("We could not load this event.")
    } finally {
      setIsLoadingEvent(false)
    }
  }, [eventId, ticketTypeId])

  useEffect(() => {
    loadEvent()
  }, [loadEvent])

  useEffect(() => {
    if (!isPurchaseSuccess || reduce) return
    confetti({
      particleCount: 90,
      spread: 68,
      origin: { y: 0.6 },
      colors: VIBE_CONFETTI[vibe],
    })
    /* No forced redirect. The old page bounced to another route after three
       seconds, which pulled the confirmation away mid-read. */

    /* `vibe` is deliberately not a dependency. It is read once at the moment
       the purchase lands, which is correct; listing it would replay the
       confetti every time someone switched theme on the success screen. */
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isPurchaseSuccess, reduce])

  const handlePurchase = async () => {
    if (isAuthLoading || !user?.access_token || !eventId || !ticketTypeId) return

    setIsProcessing(true)
    setError(undefined)
    try {
      await purchaseTicket(user.access_token, eventId, ticketTypeId)
      setIsPurchaseSuccess(true)
    } catch (err) {
      console.error("Purchase failed:", err)
      setError(
        err instanceof Error && err.message ? err.message : "The purchase did not go through.",
      )
    } finally {
      setIsProcessing(false)
    }
  }

  const start = event?.start ? parseWallClockDate(event.start) : undefined

  return (
    <PageContainer>
      <Navbar />

      <div className="mx-auto max-w-[1400px] px-5 py-12 lg:px-8 lg:py-20">
        <div className="mx-auto w-full max-w-lg">
          {isPurchaseSuccess ? (
            <motion.div
              initial={reduce ? false : { opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.45, ease: EASE }}
              className="rounded-md border border-border bg-card p-8 text-center"
            >
              <CheckCircle
                weight="fill"
                size={30}
                className="mx-auto text-[var(--success)]"
              />
              <h1 className="mt-5 text-xl font-semibold tracking-tight text-foreground">
                That is yours
              </h1>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                The ticket is on your account with the code you scan at the door.
              </p>

              <div className="mt-7 flex flex-col gap-2.5 sm:flex-row">
                <Link to="/dashboard/tickets" className="flex-1">
                  <Button className="w-full">View my tickets</Button>
                </Link>
                <Link to={`/events/${eventId}`} className="flex-1">
                  <Button variant="outline" className="w-full">
                    Back to event
                  </Button>
                </Link>
              </div>
            </motion.div>
          ) : (
            <motion.div
              initial={reduce ? false : { opacity: 0, y: 14 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.45, ease: EASE }}
            >
              <Link
                to={`/events/${eventId}`}
                className="focus-ring inline-flex items-center gap-1.5 rounded-sm text-sm text-muted-foreground transition-colors hover:text-foreground"
              >
                <ArrowLeft weight="bold" size={14} />
                Back to event
              </Link>

              <h1 className="display-section mt-7">Confirm your ticket</h1>

              {/*
                Stated before the action, not in small print underneath it.
                This build takes no payment at all: the previous page showed
                card number, expiry and CVV fields that were never read by
                handlePurchase and never sent anywhere. Collecting card
                details that go nowhere is worse than collecting none.
              */}
              <div className="mt-6 flex items-start gap-3 rounded-md border border-primary/25 bg-primary/[0.06] p-4">
                <Info weight="fill" size={17} className="mt-0.5 shrink-0 text-primary" />
                <div>
                  <p className="text-sm font-medium text-foreground">No payment is taken</p>
                  <p className="mt-1 text-sm leading-relaxed text-muted-foreground">
                    Card processing is not connected yet, so this issues the ticket directly.
                    Do not enter card details anywhere on this site.
                  </p>
                </div>
              </div>

              <div className="mt-6 rounded-md border border-border bg-card p-5">
                {isLoadingEvent ? (
                  <div className="space-y-3">
                    <Skeleton className="h-5 w-3/4 rounded-sm" />
                    <Skeleton className="h-4 w-1/2 rounded-sm" />
                    <Skeleton className="h-4 w-1/3 rounded-sm" />
                  </div>
                ) : loadError ? (
                  <div className="flex items-start gap-3">
                    <WarningCircle weight="fill" size={17} className="mt-0.5 shrink-0 text-destructive" />
                    <div>
                      <p className="text-sm font-medium text-foreground">{loadError}</p>
                      <Button variant="outline" size="sm" className="mt-3" onClick={loadEvent}>
                        Try again
                      </Button>
                    </div>
                  </div>
                ) : (
                  <>
                    <h2 className="text-[0.6875rem] font-medium uppercase tracking-[0.14em] text-muted-foreground">
                      You are getting
                    </h2>

                    <p className="mt-4 text-lg font-semibold leading-snug tracking-tight text-foreground">
                      {event?.name}
                    </p>
                    {start && (
                      <p className="mt-1.5 font-mono text-xs uppercase tracking-[0.08em] text-primary">
                        {format(start, "EEE d MMM yyyy")} · {format(start, "HH:mm")}
                      </p>
                    )}
                    {event?.venue && (
                      <p className="mt-1 text-sm text-muted-foreground">{event.venue}</p>
                    )}

                    <hr className="perf my-5" />

                    <div className="flex items-baseline justify-between">
                      <span className="text-sm text-foreground">{tier?.name}</span>
                      <span className="font-mono text-sm tabular-nums text-muted-foreground">
                        {tier ? money(tier.price) : ""}
                      </span>
                    </div>

                    <div className="mt-4 flex items-baseline justify-between border-t border-border pt-4">
                      <span className="text-sm font-medium text-foreground">Total</span>
                      <span className="font-mono text-xl font-semibold tabular-nums tracking-tight text-foreground">
                        {tier ? money(tier.price) : ""}
                      </span>
                    </div>
                    <p className="mt-2 text-xs text-muted-foreground">
                      Nothing is added on top of that.
                    </p>
                  </>
                )}
              </div>

              {error && (
                <div
                  role="alert"
                  className="mt-5 rounded-md border border-destructive/40 bg-destructive/5 p-4"
                >
                  <p className="text-sm text-destructive">{error}</p>
                </div>
              )}

              <Button
                size="lg"
                className="mt-6 w-full"
                onClick={handlePurchase}
                disabled={isProcessing || isLoadingEvent || !!loadError || !tier}
              >
                {isProcessing ? "Issuing your ticket" : "Get my ticket"}
              </Button>
            </motion.div>
          )}
        </div>
      </div>

      <Footer />
    </PageContainer>
  )
}

export default PurchaseTicketPage
