"use client"

import type React from "react"
import { useRef } from "react"
import { useAuth } from "react-oidc-context"
import { useEffect, useState } from "react"
import type { PublishedEventSummary, SpringBootPagination } from "@/domain/domain"
import { listPublishedEvents, searchPublishedEvents } from "@/lib/api"
import { motion, useInView } from "framer-motion"
import {
  MusicNotes, Barbell, PaintBrush, CircuitBoard, ForkKnife, TheaterMasks,
  CalendarCheck, UsersThree, ChartLineUp,
  ArrowRight, Star, Quotes,
} from "@/components/icons"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"
import SearchBar from "@/components/forms/search-bar"
import EventGrid from "@/components/events/event-grid"
import { Pagination } from "@/components/common/pagination"
import { Button } from "@/components/ui/button"
import { Link } from "react-router"

/* ── Data ── */

const stats = [
  { Icon: CalendarCheck, value: "10K+", label: "Events" },
  { Icon: UsersThree,    value: "500K+", label: "Attendees" },
  { Icon: ChartLineUp,  value: "98%",   label: "Satisfaction" },
]

const categories = [
  { name: "Music",   Icon: MusicNotes    },
  { name: "Sports",  Icon: Barbell       },
  { name: "Arts",    Icon: PaintBrush    },
  { name: "Tech",    Icon: CircuitBoard  },
  { name: "Food",    Icon: ForkKnife     },
  { name: "Comedy",  Icon: TheaterMasks  },
]

const brands = [
  "Meridian Music",
  "Vertex Summit",
  "Pulse Athletics",
  "Nova Productions",
  "Studio Gallery",
  "Apex Events",
  "Harbor Fest",
  "Orbit Conference",
]

const testimonials = [
  {
    quote:
      "We moved our entire festival ticketing to VenueSync last season. Sales were up, refunds were down, and our team actually enjoyed managing it. The QR validation at the gate was flawless.",
    name: "Priya Mehta",
    role: "Co-founder",
    company: "Meridian Music Festival",
    avatar: "PM",
    stars: 5,
  },
  {
    quote:
      "VenueSync handles our monthly summit series end to end. Creating ticket tiers takes minutes. The real-time dashboard on event day is the only thing I have open.",
    name: "Jordan Kessler",
    role: "Head of Events",
    company: "Vertex Summit",
    avatar: "JK",
    stars: 5,
  },
  {
    quote:
      "I've bought tickets on a dozen platforms. VenueSync is the only one where I actually trust the checkout and feel confident my QR code will work at the door.",
    name: "Aisha Okonkwo",
    role: "Attendee",
    company: "Regular VenueSync user",
    avatar: "AO",
    stars: 5,
  },
]

/* ── Easing ── */
const EXPO = [0.22, 1, 0.36, 1] as const
const LINE_DUR = 0.68

const AttendeeLandingPage: React.FC = () => {
  const { isLoading: isAuthLoading } = useAuth()

  const brandsRef = useRef<HTMLDivElement>(null)
  const testimonialsRef = useRef<HTMLDivElement>(null)
  const ctaRef = useRef<HTMLDivElement>(null)

  const brandsInView = useInView(brandsRef, { once: true, margin: "-60px" })
  const testimonialsInView = useInView(testimonialsRef, { once: true, margin: "-80px" })
  const ctaInView = useInView(ctaRef, { once: true, margin: "-80px" })

  const [page, setPage] = useState(0)
  const [publishedEvents, setPublishedEvents] = useState<SpringBootPagination<PublishedEventSummary> | undefined>()
  const [isLoading, setIsLoading] = useState(true)
  const [query, setQuery] = useState("")

  useEffect(() => {
    if (query && query.length > 0) {
      queryPublishedEvents()
    } else {
      refreshPublishedEvents()
    }
  }, [page])

  const refreshPublishedEvents = async () => {
    setIsLoading(true)
    try {
      setPublishedEvents(await listPublishedEvents(page))
    } catch (err) {
      console.error("Failed to load events:", err)
    } finally {
      setIsLoading(false)
    }
  }

  const queryPublishedEvents = async () => {
    if (!query) { await refreshPublishedEvents(); return }
    setIsLoading(true)
    try {
      setPublishedEvents(await searchPublishedEvents(query, page))
    } catch (err) {
      console.error("Failed to search events:", err)
    } finally {
      setIsLoading(false)
    }
  }

  const handleSearch = () => { setPage(0); queryPublishedEvents() }

  if (isAuthLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <div className="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin" />
      </div>
    )
  }

  return (
    <PageContainer>
      <Navbar />

      {/* ══════════════════════════════
          HERO
      ══════════════════════════════ */}
      <section className="relative pt-36 pb-20 overflow-hidden">
        {/* Single ambient glow */}
        <div
          className="absolute top-0 left-1/2 -translate-x-1/2 w-[720px] h-[440px] pointer-events-none"
          style={{
            background:
              "radial-gradient(ellipse at center top, oklch(0.68 0.19 278 / 0.10) 0%, transparent 68%)",
          }}
        />

        <div className="container mx-auto px-4 lg:px-8 relative">
          <div className="text-center max-w-4xl mx-auto">

            {/* Eyebrow — one location only */}
            <motion.div
              initial={{ opacity: 0, transform: "translateY(8px)" }}
              animate={{ opacity: 1, transform: "translateY(0px)" }}
              transition={{ duration: 0.38, ease: [0.25, 1, 0.5, 1] }}
              className="eyebrow mb-8 mx-auto"
            >
              Discover Amazing Events
            </motion.div>

            {/* ── Line-by-line headline reveal ── */}
            {/* Each word group rises from behind its own overflow-hidden mask */}
            <h1 className="hero-heading mb-6" style={{ textWrap: "balance" } as React.CSSProperties}>
              <span className="block overflow-hidden pb-1">
                <motion.span
                  className="block"
                  initial={{ transform: "translateY(110%)", opacity: 0 }}
                  animate={{ transform: "translateY(0%)", opacity: 1 }}
                  transition={{ delay: 0.10, duration: LINE_DUR, ease: EXPO }}
                >
                  Find Your Next
                </motion.span>
              </span>
              <span className="block overflow-hidden pb-1">
                <motion.span
                  className="block text-primary italic"
                  initial={{ transform: "translateY(110%)", opacity: 0 }}
                  animate={{ transform: "translateY(0%)", opacity: 1 }}
                  transition={{ delay: 0.20, duration: LINE_DUR, ease: EXPO }}
                >
                  Unforgettable
                </motion.span>
              </span>
              <span className="block overflow-hidden">
                <motion.span
                  className="block"
                  initial={{ transform: "translateY(110%)", opacity: 0 }}
                  animate={{ transform: "translateY(0%)", opacity: 1 }}
                  transition={{ delay: 0.30, duration: LINE_DUR, ease: EXPO }}
                >
                  Experience
                </motion.span>
              </span>
            </h1>

            <motion.p
              className="text-lg text-muted-foreground max-w-xl mx-auto mb-10 leading-relaxed"
              initial={{ opacity: 0, transform: "translateY(10px)" }}
              animate={{ opacity: 1, transform: "translateY(0px)" }}
              transition={{ delay: 0.52, duration: 0.48, ease: [0.25, 1, 0.5, 1] }}
            >
              Concerts, festivals, workshops, and more. Book tickets instantly
              and create memories that last.
            </motion.p>

            <motion.div
              initial={{ opacity: 0, transform: "translateY(10px)" }}
              animate={{ opacity: 1, transform: "translateY(0px)" }}
              transition={{ delay: 0.62, duration: 0.48, ease: [0.25, 1, 0.5, 1] }}
            >
              <SearchBar
                value={query}
                onChange={setQuery}
                onSearch={handleSearch}
                className="max-w-2xl mx-auto mb-10"
              />
            </motion.div>

            {/* Category pills — per-item CSS stagger */}
            <div className="flex flex-wrap justify-center gap-2">
              {categories.map((category, i) => {
                const Icon = category.Icon
                return (
                  <button
                    key={category.name}
                    style={{ "--i": i + 11 } as React.CSSProperties}
                    className="stagger-item btn-press flex items-center gap-1.5 px-4 py-1.5 rounded-full text-sm font-medium text-muted-foreground border border-border/50 hover:border-primary/40 hover:text-foreground hover:bg-primary/[0.06] transition-colors duration-150"
                  >
                    <Icon className="w-3.5 h-3.5" />
                    {category.name}
                  </button>
                )
              })}
            </div>
          </div>

          {/* Stats row — each number staggers in after headline */}
          <div className="flex items-center justify-center gap-12 mt-16 pt-8 border-t border-border/30">
            {stats.map((stat, i) => {
              const Icon = stat.Icon
              return (
                <motion.div
                  key={stat.label}
                  className="text-center"
                  initial={{ opacity: 0, transform: "translateY(10px)" }}
                  animate={{ opacity: 1, transform: "translateY(0px)" }}
                  transition={{ delay: 0.84 + i * 0.09, duration: 0.4, ease: [0.25, 1, 0.5, 1] }}
                >
                  <p className="text-2xl font-bold text-foreground tracking-tight">{stat.value}</p>
                  <p className="text-xs text-muted-foreground mt-0.5 flex items-center gap-1 justify-center">
                    <Icon weight="fill" size={12} />
                    {stat.label}
                  </p>
                </motion.div>
              )
            })}
          </div>
        </div>
      </section>

      {/* ══════════════════════════════
          TRUSTED BY
          Horizontal brand name strip.
          Scroll-triggered fade per brand.
      ══════════════════════════════ */}
      <section className="py-14 border-y border-border/20" ref={brandsRef}>
        <div className="container mx-auto px-4 lg:px-8">
          <p className="text-center text-xs font-semibold tracking-[0.18em] uppercase text-muted-foreground/50 mb-8">
            Trusted by leading organizations
          </p>

          <div className="flex flex-wrap items-center justify-center gap-x-10 gap-y-4">
            {brands.map((name, i) => (
              <motion.span
                key={name}
                initial={{ opacity: 0, transform: "translateY(6px)" }}
                animate={brandsInView ? { opacity: 1, transform: "translateY(0px)" } : {}}
                transition={{ delay: i * 0.055, duration: 0.38, ease: [0.25, 1, 0.5, 1] }}
                className="text-sm font-semibold text-muted-foreground/40 hover:text-muted-foreground/70 transition-colors duration-200 tracking-tight cursor-default select-none"
              >
                {name}
              </motion.span>
            ))}
          </div>
        </div>
      </section>

      {/* ══════════════════════════════
          EVENTS
      ══════════════════════════════ */}
      <section className="py-20">
        <div className="container mx-auto px-4 lg:px-8">
          <div className="flex items-center justify-between mb-10">
            <div>
              <h2 className="text-2xl font-bold text-foreground tracking-tight mb-1">
                {query ? "Search Results" : "Upcoming Events"}
              </h2>
              <p className="text-sm text-muted-foreground">
                {query ? `Results for "${query}"` : "Curated events happening near you"}
              </p>
            </div>
            <Link to="/events">
              <Button variant="ghost" size="sm" className="btn-press gap-1.5 text-muted-foreground hover:text-foreground">
                View All
                <ArrowRight className="w-3.5 h-3.5" />
              </Button>
            </Link>
          </div>

          <EventGrid events={publishedEvents?.content || []} isLoading={isLoading} />

          {publishedEvents && publishedEvents.totalPages > 1 && (
            <div className="mt-12">
              <Pagination pagination={publishedEvents} onPageChange={setPage} />
            </div>
          )}
        </div>
      </section>

      {/* ══════════════════════════════
          TESTIMONIALS
          3-column card grid.
          Lu.ma style: clean avatar,
          quote, name + role.
      ══════════════════════════════ */}
      <section className="py-20 border-t border-border/20" ref={testimonialsRef}>
        <div className="container mx-auto px-4 lg:px-8">
          {/* Section header */}
          <div className="max-w-xl mb-14">
            <h2 className="section-heading mb-3">
              Loved by organizers<br />and attendees alike
            </h2>
            <p className="text-sm text-muted-foreground leading-relaxed">
              From first-time creators to large-scale festival producers, people trust VenueSync to handle the details.
            </p>
          </div>

          {/* Cards */}
          <div className="grid md:grid-cols-3 gap-5">
            {testimonials.map((t, i) => (
              <motion.div
                key={t.name}
                initial={{ opacity: 0, transform: "translateY(18px)" }}
                animate={testimonialsInView ? { opacity: 1, transform: "translateY(0px)" } : {}}
                transition={{ delay: i * 0.10, duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
                className="flex flex-col gap-5 rounded-2xl border border-border/40 bg-card/30 p-6 hover:border-border/60 transition-colors duration-200"
              >
                {/* Stars */}
                <div className="flex gap-0.5">
                  {Array.from({ length: t.stars }).map((_, s) => (
                    <Star key={s} weight="fill" size={14} className="text-primary" />
                  ))}
                </div>

                {/* Quote */}
                <blockquote className="text-sm text-foreground/80 leading-relaxed flex-1">
                  <Quotes weight="fill" size={18} className="text-primary/40 mb-2" />
                  {t.quote}
                </blockquote>

                {/* Author */}
                <div className="flex items-center gap-3 pt-2 border-t border-border/30">
                  <div className="w-9 h-9 rounded-full gradient-primary flex items-center justify-center flex-shrink-0 shadow-sm shadow-primary/20">
                    <span className="text-[11px] font-bold text-white">{t.avatar}</span>
                  </div>
                  <div className="min-w-0">
                    <p className="text-sm font-semibold text-foreground truncate">{t.name}</p>
                    <p className="text-xs text-muted-foreground truncate">
                      {t.role} · {t.company}
                    </p>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ══════════════════════════════
          CTA
      ══════════════════════════════ */}
      <section className="py-20 border-t border-border/20">
        <div className="container mx-auto px-4 lg:px-8">
          <motion.div
            ref={ctaRef}
            initial={{ opacity: 0, transform: "translateY(20px)" }}
            animate={ctaInView ? { opacity: 1, transform: "translateY(0px)" } : {}}
            transition={{ duration: 0.55, ease: [0.22, 1, 0.36, 1] }}
            className="relative overflow-hidden rounded-2xl border border-border/40 p-12 md:p-16 text-center"
            style={{
              background:
                "linear-gradient(160deg, oklch(0.11 0.013 265 / 0.9) 0%, oklch(0.075 0.01 265 / 0.95) 100%)",
            }}
          >
            <div className="absolute inset-0 pointer-events-none gradient-mesh opacity-50" />

            <div className="relative max-w-lg mx-auto">
              <h2 className="text-3xl md:text-4xl font-bold text-foreground tracking-tight mb-4 text-balance">
                Ready to host your<br />own event?
              </h2>
              <p className="text-muted-foreground mb-8 text-sm leading-relaxed">
                Join thousands of organizers who use VenueSync. Set up your first event
                in under 10 minutes — no credit card required.
              </p>
              <div className="flex flex-col sm:flex-row gap-3 justify-center">
                <Link to="/organizers">
                  <Button
                    size="lg"
                    className="btn-press gradient-primary text-white shadow-lg shadow-primary/20 font-semibold px-8"
                  >
                    Start for free
                    <ArrowRight className="w-4 h-4 ml-2" />
                  </Button>
                </Link>
                <Link to="/">
                  <Button
                    size="lg"
                    variant="outline"
                    className="btn-press border-border/50 text-muted-foreground hover:text-foreground hover:border-border px-8"
                  >
                    Browse events
                  </Button>
                </Link>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      <Footer />
    </PageContainer>
  )
}

export default AttendeeLandingPage
