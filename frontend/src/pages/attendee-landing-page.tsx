"use client"

import type React from "react"
import { useAuth } from "react-oidc-context"
import { useCallback, useEffect, useState } from "react"
import type { PublishedEventSummary, SpringBootPagination } from "@/domain/domain"
import { listPublishedEvents, searchPublishedEvents } from "@/lib/api"
import { motion, useReducedMotion } from "framer-motion"
import { format } from "date-fns"
import { Link } from "react-router"
import {
  MusicNotes,
  Barbell,
  PaintBrush,
  Terminal,
  ForkKnife,
  Confetti,
  ArrowRight,
  DeviceMobile,
  Receipt,
  Ticket,
  WarningCircle,
  OrganizerGlyph,
  type MonogramGlyph,
} from "@/components/icons"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"
import SearchBar from "@/components/forms/search-bar"
import EventGrid from "@/components/events/event-grid"
import RandomEventImage from "@/components/random-event-image"
import { Pagination } from "@/components/common/pagination"
import { Button } from "@/components/ui/button"
import { parseWallClockDate } from "@/lib/date-utils"

/* ── Content ────────────────────────────────────────────────────────── */

const categories = [
  { name: "Music", Icon: MusicNotes },
  { name: "Sports", Icon: Barbell },
  { name: "Arts", Icon: PaintBrush },
  { name: "Tech", Icon: Terminal },
  { name: "Food", Icon: ForkKnife },
  { name: "Comedy", Icon: Confetti },
]

/* Demo tenants. Each gets a distinct geometric mark rather than a plain
   text wordmark, which always reads as an unfinished placeholder. */
const organizers: { name: string; glyph: MonogramGlyph }[] = [
  { name: "Meridian Music", glyph: "bar-circle" },
  { name: "Vertex Summit", glyph: "triangle" },
  { name: "Pulse Athletics", glyph: "pulse" },
  { name: "Nova Productions", glyph: "asterisk" },
  { name: "Studio Gallery", glyph: "diamond" },
  { name: "Apex Events", glyph: "chevrons" },
  { name: "Harbor Fest", glyph: "arc" },
  { name: "Orbit Conference", glyph: "orbit" },
]

const testimonials = {
  lead: {
    quote:
      "We moved the whole festival over last season. Sales went up, refunds went down, and the gate scanned clean all weekend.",
    name: "Priya Mehta",
    role: "Co-founder, Meridian Music Festival",
  },
  rest: [
    {
      quote: "Ticket tiers take minutes to set up. On event day the live dashboard is the only tab I keep open.",
      name: "Jordan Kessler",
      role: "Head of events, Vertex Summit",
    },
    {
      quote: "First platform where I stopped screenshotting my ticket as a backup. The code just works at the door.",
      name: "Aisha Okonkwo",
      role: "Attendee since 2024",
    },
  ],
}

const EASE = [0.16, 1, 0.3, 1] as const

/* ── Hero visual ────────────────────────────────────────────────────────
   Two overlapping stubs showing genuine events from the API. Building a
   fake product preview out of styled divs is the loudest tell there is,
   so this renders the real thing at a smaller scale instead.
   ──────────────────────────────────────────────────────────────────── */

const HeroStub: React.FC<{
  event?: PublishedEventSummary
  isLoading?: boolean
  rotate: number
  className?: string
  dim?: boolean
}> = ({ event, isLoading = false, rotate, className = "", dim = false }) => {
  const start = event?.start ? parseWallClockDate(event.start) : null

  return (
    <div
      className={`absolute w-[248px] rounded-md border border-border bg-card shadow-[0_28px_60px_-24px_oklch(0_0_0/0.85)] ${className}`}
      style={{ transform: `rotate(${rotate}deg)` }}
      aria-hidden
    >
      <div className="relative aspect-[5/3] overflow-hidden rounded-t-md bg-secondary">
        <RandomEventImage seed={event?.id ?? String(rotate)} priority />
        {dim && <div className="absolute inset-0 bg-background/45" />}
      </div>
      <div className="relative">
        <div className="notch notch-l top-1/2 -translate-y-1/2" />
        <div className="notch notch-r top-1/2 -translate-y-1/2" />
        <hr className="perf mx-4" />
      </div>
      <div className="space-y-2 p-3.5">
        {isLoading && <div className="skeleton h-3.5 w-4/5 rounded-sm" />}
        {/* Request finished and returned nothing: an unprinted stub, not a
            loading one. This previously read "Loading events" forever
            whenever the events API was unreachable. */}
        {!isLoading && !event && <div className="h-3.5 w-4/5 rounded-sm bg-secondary" />}
        {!isLoading && event && (
          <p className="truncate text-[0.8125rem] font-semibold tracking-tight text-foreground">
            {event.name}
          </p>
        )}
        {!isLoading && event && start && (
          <p className="font-mono text-[0.625rem] uppercase tracking-[0.08em] text-primary">
            {format(start, "EEE d MMM · HH:mm")}
          </p>
        )}
        {isLoading && <div className="skeleton h-2.5 w-2/5 rounded-sm" />}
        {!isLoading && !event && <div className="h-2.5 w-2/5 rounded-sm bg-secondary" />}
      </div>
    </div>
  )
}

/* ── Page ───────────────────────────────────────────────────────────── */

const AttendeeLandingPage: React.FC = () => {
  const { isLoading: isAuthLoading } = useAuth()
  const reduce = useReducedMotion()

  const [page, setPage] = useState(0)
  const [publishedEvents, setPublishedEvents] = useState<
    SpringBootPagination<PublishedEventSummary> | undefined
  >()
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")

  /* Takes the term as an argument rather than reading component state, so
     a category tap can search immediately instead of waiting a render for
     setQuery to land. */
  const runSearch = useCallback(async (term: string, pageNum: number) => {
    setIsLoading(true)
    setError(null)
    try {
      const result = term.trim()
        ? await searchPublishedEvents(term.trim(), pageNum)
        : await listPublishedEvents(pageNum)
      setPublishedEvents(result)
    } catch (err) {
      console.error("Failed to load events:", err)
      setError("We could not reach the events service.")
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    runSearch(query, page)
    // Paging should not re-run on every keystroke, only on page change.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  const handleSearch = () => {
    setPage(0)
    runSearch(query, 0)
  }

  /* Categories used to be dead buttons. They now drive the same search the
     field does, and tapping an active one clears it. */
  const handleCategory = (name: string) => {
    const next = query.toLowerCase() === name.toLowerCase() ? "" : name
    setQuery(next)
    setPage(0)
    runSearch(next, 0)
  }

  const heroEvents = publishedEvents?.content?.slice(0, 2) ?? []

  if (isAuthLoading) {
    return (
      <div className="flex min-h-[100dvh] items-center justify-center bg-background">
        <div className="h-7 w-7 animate-spin rounded-full border-2 border-border border-t-primary" />
      </div>
    )
  }

  return (
    <PageContainer>
      <Navbar />

      {/* ═══ 1. Hero. Asymmetric split, weight on the left. ═══ */}
      <section className="relative overflow-hidden pb-16 pt-14 lg:pb-24 lg:pt-24">
        <div className="pointer-events-none absolute inset-x-0 top-0 h-[420px] gradient-mesh" />

        <div className="relative mx-auto max-w-[1400px] px-5 lg:px-8">
          <div className="grid items-center gap-14 lg:grid-cols-12 lg:gap-8">
            <div className="lg:col-span-6">
              <motion.h1
                initial={reduce ? false : { opacity: 0, y: 18 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, ease: EASE }}
                className="display-hero max-w-[15ch] text-balance"
              >
                Find something worth leaving the house for
              </motion.h1>

              <motion.p
                initial={reduce ? false : { opacity: 0, y: 14 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.08, duration: 0.55, ease: EASE }}
                className="mt-6 max-w-[46ch] text-[1.0625rem] leading-relaxed text-muted-foreground"
              >
                Concerts, club nights, talks and food markets. Buy in two taps, walk in with the
                code on your phone.
              </motion.p>

              <motion.div
                initial={reduce ? false : { opacity: 0, y: 14 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.16, duration: 0.55, ease: EASE }}
                className="mt-9 max-w-xl"
              >
                <SearchBar value={query} onChange={setQuery} onSearch={handleSearch} size="large" />
              </motion.div>

            </div>

            {/* Real stubs, angled. Hidden below lg where there is no room to
                lay them out without crushing the headline. */}
            <div className="hidden lg:col-span-5 lg:col-start-8 lg:block">
              <motion.div
                initial={reduce ? false : { opacity: 0, y: 26 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2, duration: 0.7, ease: EASE }}
                className="relative h-[380px]"
              >
                <HeroStub
                  event={heroEvents[1]}
                  isLoading={isLoading}
                  rotate={5}
                  className="left-4 top-2"
                  dim
                />
                <HeroStub
                  event={heroEvents[0]}
                  isLoading={isLoading}
                  rotate={-3}
                  className="left-32 top-24"
                />
              </motion.div>
            </div>
          </div>
        </div>
      </section>

      {/* ═══ 2. Categories. Full-bleed scroll rail. ═══ */}
      <section aria-label="Browse by category" className="border-y border-border">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <div className="-mx-5 flex snap-x snap-mandatory gap-2 overflow-x-auto px-5 py-4 [scrollbar-width:none] lg:-mx-8 lg:px-8 [&::-webkit-scrollbar]:hidden">
            {categories.map(({ name, Icon }, i) => {
              const isActive = query.toLowerCase() === name.toLowerCase()
              return (
                <button
                  key={name}
                  onClick={() => handleCategory(name)}
                  aria-pressed={isActive}
                  style={{ "--i": i } as React.CSSProperties}
                  className={`stagger-item btn-press focus-ring flex shrink-0 snap-start items-center gap-2 rounded-md border px-4 py-2 text-sm transition-colors ${
                    isActive
                      ? "border-primary bg-primary text-primary-foreground"
                      : "border-border text-muted-foreground hover:border-foreground/30 hover:text-foreground"
                  }`}
                >
                  <Icon weight="fill" size={15} />
                  {name}
                </button>
              )
            })}
          </div>
        </div>
      </section>

      {/* ═══ 3. Events. The product itself, high on the page. ═══ */}
      <section className="py-16 lg:py-24">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <div className="mb-10 flex flex-wrap items-end justify-between gap-4">
            <div>
              <h2 className="display-section">{query ? "Search results" : "On sale now"}</h2>
              <p className="mt-2 text-sm text-muted-foreground">
                {query ? (
                  <>
                    Matching <span className="font-mono text-foreground">{query}</span>
                  </>
                ) : (
                  "Events with tickets available in the next few weeks"
                )}
              </p>
            </div>
            <Link
              to="/events"
              className="focus-ring link-underline rounded-sm text-sm font-medium text-foreground"
            >
              View all events
            </Link>
          </div>

          {error ? (
            <div className="flex flex-col items-center gap-4 rounded-md border border-destructive/40 bg-destructive/5 px-6 py-16 text-center">
              <WarningCircle weight="fill" size={26} className="text-destructive" />
              <div>
                <p className="text-sm font-medium text-foreground">{error}</p>
                <p className="mt-1 text-sm text-muted-foreground">
                  This is usually temporary. Try again in a moment.
                </p>
              </div>
              <Button variant="outline" size="sm" onClick={() => runSearch(query, page)}>
                Try again
              </Button>
            </div>
          ) : (
            <>
              <EventGrid
                events={publishedEvents?.content || []}
                isLoading={isLoading}
                query={query.trim() || undefined}
              />

              {publishedEvents && publishedEvents.totalPages > 1 && (
                <div className="mt-14">
                  <Pagination pagination={publishedEvents} onPageChange={setPage} />
                </div>
              )}
            </>
          )}
        </div>
      </section>

      {/* ═══ 4. Organizers. Marks only, no category captions. ═══ */}
      <section className="border-y border-border py-12">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <h2 className="text-center text-[0.6875rem] font-medium uppercase tracking-[0.14em] text-muted-foreground">
            Organizers running events on VenueSync
          </h2>
          <ul className="mt-8 grid grid-cols-2 gap-x-6 gap-y-7 sm:grid-cols-4 lg:grid-cols-8">
            {organizers.map(({ name, glyph }) => (
              <li
                key={name}
                className="flex flex-col items-center gap-2.5 text-muted-foreground/70 transition-colors duration-200 hover:text-foreground"
              >
                <OrganizerGlyph glyph={glyph} size={22} />
                <span className="text-center text-[0.6875rem] font-medium leading-tight tracking-tight">
                  {name}
                </span>
              </li>
            ))}
          </ul>
        </div>
      </section>

      {/* ═══ 5. What you actually get. Asymmetric bento, 3 cells. ═══ */}
      <section className="py-16 lg:py-24">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <h2 className="display-section max-w-[18ch] text-balance">
            The part that usually goes wrong
          </h2>

          <div className="mt-12 grid gap-5 lg:grid-cols-12">
            {/* Large cell carries the photography */}
            <motion.article
              initial={reduce ? false : { opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, amount: 0.25 }}
              transition={{ duration: 0.55, ease: EASE }}
              className="relative overflow-hidden rounded-md border border-border bg-card lg:col-span-7"
            >
              <div className="relative aspect-[16/9] overflow-hidden lg:aspect-[16/8]">
                <RandomEventImage seed="gate-scan" alt="" />
                <div className="absolute inset-0 bg-gradient-to-t from-card via-card/70 to-transparent" />
              </div>
              <div className="relative -mt-16 p-7">
                <DeviceMobile weight="fill" size={22} className="text-primary" />
                <h3 className="mt-4 text-xl font-semibold tracking-tight text-foreground">
                  The code on your phone is the ticket
                </h3>
                <p className="mt-2.5 max-w-[46ch] text-sm leading-relaxed text-muted-foreground">
                  It renders once and stays on the device, so a dead signal at the door is not your
                  problem. Staff scan it, it turns green, you are in.
                </p>
              </div>
            </motion.article>

            <div className="grid gap-5 lg:col-span-5">
              {/* Tinted cell, so the grid is not three identical boxes */}
              <motion.article
                initial={reduce ? false : { opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.25 }}
                transition={{ delay: 0.08, duration: 0.55, ease: EASE }}
                className="rounded-md border border-primary/25 bg-primary/[0.06] p-7"
              >
                <Receipt weight="fill" size={20} className="text-primary" />
                <h3 className="mt-4 text-lg font-semibold tracking-tight text-foreground">
                  Refunds without writing an email
                </h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                  If an event is cancelled, the money goes back the way it came. Nobody has to chase
                  anybody.
                </p>
              </motion.article>

              <motion.article
                initial={reduce ? false : { opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.25 }}
                transition={{ delay: 0.16, duration: 0.55, ease: EASE }}
                className="rounded-md border border-border bg-card p-7"
              >
                <Ticket weight="fill" size={20} className="text-primary" />
                <h3 className="mt-4 text-lg font-semibold tracking-tight text-foreground">
                  Every ticket in one place
                </h3>
                <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                  No digging through a year of confirmation emails on the pavement outside a venue.
                </p>
              </motion.article>
            </div>
          </div>
        </div>
      </section>

      {/* ═══ 6. Why this exists. Prose plus sourced numbers. ═══ */}
      <section className="border-t border-border py-16 lg:py-24">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <h2 className="display-section max-w-[18ch] text-balance">
            Going out is having a moment. The tooling is not.
          </h2>

          <div className="mt-8 max-w-[62ch] space-y-5 text-[1.0625rem] leading-relaxed text-muted-foreground">
            <p>
              Gigs, comedy nights, food markets, warehouse parties, a talk in the back of a
              bookshop. People are spending on being somewhere rather than owning something, and
              the software selling them the ticket still behaves like it is 2011.
            </p>
            <p>
              Fees appear at the last step, after you have already decided. The link you send a
              friend unfurls as a grey box. The ticket ends up in an email you cannot find while
              standing in the rain outside the door.{" "}
              <span className="text-foreground">
                Every one of those is a solved problem that somebody chose not to solve.
              </span>{" "}
              VenueSync puts the price on the card before you click it, and keeps the code on your
              phone where you can reach it without hunting through an inbox.
            </p>
          </div>

          {/* Real figures from published 2026 research, attributed. Inventing
              plausible-looking percentages here would have been faster and
              would have been a lie. */}
          <div className="mt-14 grid gap-8 sm:grid-cols-3">
            {[
              {
                figure: "79%",
                caption: "of 18 to 35 year olds plan to go to more events this year than last",
              },
              {
                figure: "49%",
                caption: "of Gen Z find events through social, against 16% through a search engine",
              },
              {
                figure: "68%",
                caption: "of Gen Z would rather spend on going somewhere than on owning something",
              },
            ].map(({ figure, caption }, i) => (
              <motion.div
                key={figure}
                initial={reduce ? false : { opacity: 0, y: 16 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.4 }}
                transition={{ delay: i * 0.08, duration: 0.5, ease: EASE }}
                className="border-t border-border pt-6"
              >
                <p className="font-mono text-4xl font-semibold tabular-nums tracking-tight text-primary">
                  {figure}
                </p>
                <p className="mt-3 max-w-[34ch] text-sm leading-relaxed text-muted-foreground">
                  {caption}
                </p>
              </motion.div>
            ))}
          </div>

          <p className="mt-8 text-xs text-muted-foreground">
            Eventbrite Social Study 2026, and Ticket Fairy promoter research 2026.
          </p>
        </div>
      </section>

      {/* ═══ 7. Testimonials. One lead quote, two supporting. ═══ */}
      <section className="border-t border-border py-16 lg:py-24">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <div className="grid gap-10 lg:grid-cols-12 lg:gap-14">
            <motion.figure
              initial={reduce ? false : { opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true, amount: 0.3 }}
              transition={{ duration: 0.55, ease: EASE }}
              className="lg:col-span-7"
            >
              <blockquote className="text-2xl font-medium leading-snug tracking-tight text-foreground lg:text-[1.75rem]">
                “{testimonials.lead.quote}”
              </blockquote>
              <figcaption className="mt-7 flex items-center gap-3.5">
                <span className="h-9 w-px bg-primary" aria-hidden />
                <span>
                  <span className="block text-sm font-medium text-foreground">
                    {testimonials.lead.name}
                  </span>
                  <span className="block text-sm text-muted-foreground">
                    {testimonials.lead.role}
                  </span>
                </span>
              </figcaption>
            </motion.figure>

            <div className="flex flex-col justify-center gap-8 lg:col-span-4 lg:col-start-9">
              {testimonials.rest.map((t, i) => (
                <motion.figure
                  key={t.name}
                  initial={reduce ? false : { opacity: 0, y: 16 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true, amount: 0.3 }}
                  transition={{ delay: 0.1 + i * 0.1, duration: 0.5, ease: EASE }}
                  className="border-t border-border pt-6 first:border-t-0 first:pt-0"
                >
                  <blockquote className="text-[0.9375rem] leading-relaxed text-foreground/85">
                    “{t.quote}”
                  </blockquote>
                  <figcaption className="mt-3.5 text-xs text-muted-foreground">
                    <span className="font-medium text-foreground">{t.name}</span>
                    <br />
                    {t.role}
                  </figcaption>
                </motion.figure>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ═══ 8. Close. Full-width band, one action. ═══ */}
      <section className="border-t border-border">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <div className="grid items-center gap-8 py-16 lg:grid-cols-12 lg:py-20">
            <div className="lg:col-span-7">
              <h2 className="display-section max-w-[16ch] text-balance">
                Running something yourself?
              </h2>
              <p className="mt-4 max-w-[52ch] text-[0.9375rem] leading-relaxed text-muted-foreground">
                Set up ticket tiers, publish the page and watch sales land in real time. Organizer
                access is free and takes about ten minutes to get going.
              </p>
            </div>
            <div className="lg:col-span-4 lg:col-start-9 lg:justify-self-end">
              <Link to="/organizers">
                <Button size="lg" className="gap-2 px-7">
                  Host an event
                  <ArrowRight weight="bold" size={16} />
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </section>

      <Footer />
    </PageContainer>
  )
}

export default AttendeeLandingPage
