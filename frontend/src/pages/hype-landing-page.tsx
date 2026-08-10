"use client"

import type React from "react"
import { useCallback, useEffect, useRef, useState } from "react"
import { Link } from "react-router"
import { format } from "date-fns"
import { AnimatePresence, motion, useReducedMotion } from "framer-motion"
import toast from "react-hot-toast"
import type { PublishedEventSummary } from "@/domain/domain"
import { listPublishedEvents, searchPublishedEvents } from "@/lib/api"
import { parseWallClockDate } from "@/lib/date-utils"
import { downloadIcs } from "@/lib/calendar"
import RandomEventImage from "@/components/random-event-image"
import HypeTabBar from "@/components/layout/hype-tabbar"
import { useAudience } from "@/hooks/use-audience"
import {
  VenueSyncMark,
  ArrowRight,
  CalendarBlank,
  CaretDown,
  ShareNetwork,
  WarningCircle,
  X,
} from "@/components/icons"

const EASE = [0.16, 1, 0.3, 1] as const

/* A feed that ends after four panels is not a feed. The shared API helpers
   default to 4 for the grid layouts; this asks for more. */
const PAGE_SIZE = 8

/* ── One event, one screen ─────────────────────────────────────────── */

const EventPanel: React.FC<{ event: PublishedEventSummary; index: number }> = ({
  event,
  index,
}) => {
  const reduce = useReducedMotion()
  const start = event.start ? parseWallClockDate(event.start) : undefined
  const end = event.end ? parseWallClockDate(event.end) : undefined

  const handleShare = async () => {
    const url = `${window.location.origin}/events/${event.id}`
    try {
      if (navigator.share) {
        await navigator.share({ title: event.name, url })
        return
      }
      await navigator.clipboard.writeText(url)
      toast.success("Link copied")
    } catch (err) {
      if ((err as Error).name === "AbortError") return
      toast.error("Could not share that")
    }
  }

  const handleCalendar = () => {
    if (!start) return
    downloadIcs({
      id: event.id,
      name: event.name,
      venue: event.venue,
      start,
      end,
      url: `${window.location.origin}/events/${event.id}`,
    })
  }

  const rail =
    "focus-ring flex h-11 w-11 items-center justify-center border-2 border-border bg-background/70 text-foreground backdrop-blur-sm transition-colors hover:border-primary hover:text-primary"

  return (
    <section
      className="relative flex h-[100dvh] snap-start snap-always flex-col justify-end overflow-hidden"
      aria-label={event.name}
    >
      <div className="absolute inset-0">
        <RandomEventImage seed={event.id} alt="" priority={index < 2} />
        {/* Scrim carries the type. Without it a light photo eats the headline. */}
        <div className="absolute inset-0 bg-gradient-to-t from-background via-background/75 to-background/20" />
      </div>

      {/* Vertical action rail, thumb-reachable on the right edge */}
      <div className="absolute bottom-40 right-4 z-10 flex flex-col gap-2.5">
        <button onClick={handleShare} className={rail} aria-label={`Share ${event.name}`}>
          <ShareNetwork weight="bold" size={18} />
        </button>
        {start && (
          <button
            onClick={handleCalendar}
            className={rail}
            aria-label={`Add ${event.name} to calendar`}
          >
            <CalendarBlank weight="bold" size={18} />
          </button>
        )}
      </div>

      <motion.div
        initial={reduce ? false : { opacity: 0, y: 26 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, amount: 0.4 }}
        transition={{ duration: 0.5, ease: EASE }}
        className="relative z-10 px-5 pb-32 sm:px-8"
      >
        {start && (
          <p className="font-mono text-xs uppercase tracking-[0.16em] text-primary">
            {format(start, "EEE d MMM")} · {format(start, "HH:mm")}
          </p>
        )}

        <h2 className="display-hero mt-3 max-w-[11ch] text-balance">{event.name}</h2>

        {event.venue && (
          <p className="mt-4 max-w-[30ch] text-base text-muted-foreground">{event.venue}</p>
        )}

        <Link
          to={`/events/${event.id}`}
          className="focus-ring btn-press mt-7 inline-flex h-13 items-center gap-2.5 bg-primary px-7 text-base font-semibold uppercase tracking-[0.04em] text-primary-foreground"
        >
          Get ticket
          <ArrowRight weight="bold" size={18} />
        </Link>
      </motion.div>
    </section>
  )
}

/* ── Page ──────────────────────────────────────────────────────────── */

const HypeLandingPage: React.FC = () => {
  const reduce = useReducedMotion()
  const { setAudience } = useAudience()
  const scroller = useRef<HTMLDivElement>(null)

  const [events, setEvents] = useState<PublishedEventSummary[]>([])
  const [page, setPage] = useState(0)
  const [isLast, setIsLast] = useState(true)
  const [isLoading, setIsLoading] = useState(true)
  const [isLoadingMore, setIsLoadingMore] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [searchOpen, setSearchOpen] = useState(false)
  const [query, setQuery] = useState("")
  /* The term the current results belong to, as opposed to whatever is
     half-typed in the box. Paging and the empty state both need this. */
  const [activeQuery, setActiveQuery] = useState("")

  const load = useCallback(async (term: string, pageNum: number, append: boolean) => {
    if (append) setIsLoadingMore(true)
    else setIsLoading(true)
    setError(null)
    try {
      const res = term
        ? await searchPublishedEvents(term, pageNum, PAGE_SIZE)
        : await listPublishedEvents(pageNum, PAGE_SIZE)
      setEvents((prev) => (append ? [...prev, ...res.content] : res.content))
      setPage(res.number)
      setIsLast(res.last)
      setActiveQuery(term)
    } catch (err) {
      console.error("Failed to load events:", err)
      setError("Could not reach the events service.")
    } finally {
      if (append) setIsLoadingMore(false)
      else setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    load("", 0, false)
  }, [load])

  /* Jump back to the first panel after a search so the user is not left
     halfway down a feed that just changed underneath them. */
  const runSearch = () => {
    load(query.trim(), 0, false)
    scroller.current?.scrollTo({ top: 0, behavior: reduce ? "auto" : "smooth" })
  }

  /* Appends the next page when the closing panel comes into view, so the
     feed keeps going instead of stopping dead at the first page. */
  const loadMore = () => {
    if (isLast || isLoadingMore || isLoading) return
    load(activeQuery, page + 1, true)
  }

  return (
    <div className="relative h-[100dvh] overflow-hidden bg-background">
      {/* Floating brand. No top nav: the tab bar is the navigation. */}
      <div className="pointer-events-none absolute inset-x-0 top-0 z-30 flex items-center justify-between px-5 pt-5 sm:px-8">
        <Link to="/" className="pointer-events-auto focus-ring flex items-center gap-2">
          <VenueSyncMark size={28} />
        </Link>
        <button
          onClick={() => setAudience("classic")}
          className="pointer-events-auto focus-ring border-2 border-border bg-background/70 px-3 py-1.5 font-mono text-[10px] uppercase tracking-[0.12em] text-muted-foreground backdrop-blur-sm transition-colors hover:border-primary hover:text-primary"
        >
          Classic view
        </button>
      </div>

      {/* Search sheet, raised from the tab bar */}
      <AnimatePresence>
        {searchOpen && (
          <motion.div
            initial={reduce ? false : { y: "100%" }}
            animate={{ y: 0 }}
            exit={reduce ? undefined : { y: "100%" }}
            transition={{ duration: 0.3, ease: [0.32, 0.72, 0, 1] }}
            className="fixed inset-x-0 bottom-[calc(64px+env(safe-area-inset-bottom))] z-40 border-t-2 border-border bg-background p-5"
          >
            <div className="mx-auto flex max-w-md items-center gap-2">
              <input
                autoFocus
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && runSearch()}
                placeholder="what are you looking for"
                aria-label="Search events"
                className="h-12 min-w-0 flex-1 border-2 border-border bg-card px-3.5 text-base text-foreground outline-none placeholder:text-muted-foreground focus:border-primary"
              />
              <button
                onClick={runSearch}
                className="btn-press focus-ring h-12 shrink-0 bg-primary px-5 text-sm font-semibold uppercase tracking-[0.04em] text-primary-foreground"
              >
                Go
              </button>
              <button
                onClick={() => setSearchOpen(false)}
                aria-label="Close search"
                className="focus-ring h-12 w-12 shrink-0 border-2 border-border text-muted-foreground"
              >
                <X weight="bold" size={17} className="mx-auto" />
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* The feed. One screen per event, snapped. */}
      <div
        ref={scroller}
        className="h-[100dvh] snap-y snap-mandatory overflow-y-scroll overscroll-y-contain [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
      >
        {isLoading ? (
          <div className="flex h-[100dvh] items-center justify-center">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-border border-t-primary" />
          </div>
        ) : error ? (
          <div className="flex h-[100dvh] flex-col items-center justify-center gap-5 px-8 text-center">
            <WarningCircle weight="fill" size={30} className="text-destructive" />
            <p className="text-base text-foreground">{error}</p>
            <button
              onClick={() => load(activeQuery, 0, false)}
              className="btn-press focus-ring bg-primary px-6 py-3 text-sm font-semibold uppercase tracking-[0.04em] text-primary-foreground"
            >
              Try again
            </button>
          </div>
        ) : events.length === 0 ? (
          /* Two different situations. "Nothing matched that" in front of
             someone who never searched reads as a broken site, which is
             exactly how an empty catalogue was being misreported. */
          <div className="flex h-[100dvh] flex-col items-center justify-center gap-5 px-8 text-center">
            {activeQuery ? (
              <>
                <p className="display-section max-w-[12ch] text-balance">Nothing matched that</p>
                <p className="max-w-[30ch] text-sm text-muted-foreground">
                  No events came back for “{activeQuery}”.
                </p>
                <button
                  onClick={() => {
                    setQuery("")
                    load("", 0, false)
                  }}
                  className="btn-press focus-ring bg-primary px-6 py-3 text-sm font-semibold uppercase tracking-[0.04em] text-primary-foreground"
                >
                  Show everything
                </button>
              </>
            ) : (
              <>
                <p className="display-section max-w-[13ch] text-balance">Nothing on sale yet</p>
                <p className="max-w-[32ch] text-sm text-muted-foreground">
                  No events are published right now. New ones land here the moment an organizer puts
                  them on sale.
                </p>
                <Link
                  to="/organizers"
                  className="btn-press focus-ring bg-primary px-6 py-3 text-sm font-semibold uppercase tracking-[0.04em] text-primary-foreground"
                >
                  Host an event
                </Link>
              </>
            )}
          </div>
        ) : (
          <>
            {events.map((event, i) => (
              <EventPanel key={event.id} event={event} index={i} />
            ))}

            {/* Closing panel. Entering it pulls the next page, so the feed
                continues instead of stopping at the first batch. */}
            <motion.section
              onViewportEnter={loadMore}
              viewport={{ amount: 0.3 }}
              className="flex h-[100dvh] snap-start flex-col items-center justify-center gap-6 px-8 pb-28 text-center"
            >
              {isLoadingMore ? (
                <div className="h-8 w-8 animate-spin rounded-full border-2 border-border border-t-primary" />
              ) : (
                <p className="display-section max-w-[14ch] text-balance">
                  {isLast ? "That is everything on sale" : "Keep scrolling"}
                </p>
              )}
              <div className="flex flex-col gap-3">
                <Link
                  to="/events"
                  className="btn-press focus-ring bg-primary px-7 py-3.5 text-sm font-semibold uppercase tracking-[0.04em] text-primary-foreground"
                >
                  Browse the full list
                </Link>
                <Link
                  to="/organizers"
                  className="focus-ring border-2 border-border px-7 py-3.5 text-sm font-semibold uppercase tracking-[0.04em] text-muted-foreground transition-colors hover:border-primary hover:text-primary"
                >
                  Host an event
                </Link>
              </div>
            </motion.section>
          </>
        )}
      </div>

      {/* Scroll affordance: gesture vocabulary is assumed, but the first
          screen still has to say the feed continues. Fades after panel 1. */}
      {!isLoading && !error && events.length > 0 && (
        <motion.div
          initial={reduce ? false : { opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 1.2, duration: 0.5 }}
          className="pointer-events-none absolute bottom-24 left-1/2 z-20 -translate-x-1/2"
        >
          <CaretDown weight="bold" size={18} className="text-muted-foreground" />
        </motion.div>
      )}

      <HypeTabBar onSearch={() => setSearchOpen((v) => !v)} searchOpen={searchOpen} />
    </div>
  )
}

export default HypeLandingPage
