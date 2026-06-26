"use client"

import type React from "react"

import { useAuth } from "react-oidc-context"
import { useEffect, useState } from "react"
import type { PublishedEventSummary, SpringBootPagination } from "@/domain/domain"
import { listPublishedEvents, searchPublishedEvents } from "@/lib/api"
import { motion, useInView } from "framer-motion"
import { useRef } from "react"
import { TrendingUp, Users, Calendar, ArrowRight, Music, Dumbbell, Palette, Cpu, UtensilsCrossed, Laugh } from "lucide-react"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"
import SearchBar from "@/components/forms/search-bar"
import EventGrid from "@/components/events/event-grid"
import { Pagination } from "@/components/common/pagination"
import { Button } from "@/components/ui/button"
import { Link } from "react-router"

const stats = [
  { icon: Calendar, value: "10K+", label: "Events" },
  { icon: Users, value: "500K+", label: "Attendees" },
  { icon: TrendingUp, value: "98%", label: "Satisfaction" },
]

const categories = [
  { name: "Music", icon: Music },
  { name: "Sports", icon: Dumbbell },
  { name: "Arts", icon: Palette },
  { name: "Tech", icon: Cpu },
  { name: "Food", icon: UtensilsCrossed },
  { name: "Comedy", icon: Laugh },
]

/* Durations for line-by-line hero reveal */
const LINE_EASE = [0.22, 1, 0.36, 1] as const
const LINE_DUR = 0.7

const AttendeeLandingPage: React.FC = () => {
  const { isLoading: isAuthLoading } = useAuth()
  const ctaRef = useRef<HTMLDivElement>(null)
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
    if (!query) {
      await refreshPublishedEvents()
      return
    }

    setIsLoading(true)
    try {
      setPublishedEvents(await searchPublishedEvents(query, page))
    } catch (err) {
      console.error("Failed to search events:", err)
    } finally {
      setIsLoading(false)
    }
  }

  const handleSearch = () => {
    setPage(0)
    queryPublishedEvents()
  }

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

      {/* ── Hero ── */}
      <section className="relative pt-36 pb-24 overflow-hidden">
        {/* Ambient glow — single, restrained */}
        <div
          className="absolute top-0 left-1/2 -translate-x-1/2 w-[640px] h-[380px] pointer-events-none"
          style={{
            background: "radial-gradient(ellipse at center, oklch(0.68 0.19 278 / 0.09) 0%, transparent 70%)",
          }}
        />

        <div className="container mx-auto px-4 lg:px-8 relative">
          <div className="text-center max-w-4xl mx-auto">

            {/* Eyebrow — one, here, nowhere else */}
            <motion.div
              initial={{ opacity: 0, transform: "translateY(8px)" }}
              animate={{ opacity: 1, transform: "translateY(0px)" }}
              transition={{ duration: 0.4, ease: [0.25, 1, 0.5, 1] }}
              className="eyebrow mb-8 mx-auto"
            >
              Discover Amazing Events
            </motion.div>

            {/* ── Line-by-line headline reveal ── */}
            {/* Each line has overflow-hidden so text rises from the floor — editorial */}
            <h1 className="hero-heading mb-6 text-balance">
              <span className="block overflow-hidden pb-1">
                <motion.span
                  initial={{ transform: "translateY(110%)", opacity: 0 }}
                  animate={{ transform: "translateY(0%)", opacity: 1 }}
                  transition={{ delay: 0.12, duration: LINE_DUR, ease: LINE_EASE }}
                  className="block"
                >
                  Find Your Next
                </motion.span>
              </span>

              <span className="block overflow-hidden pb-1">
                <motion.span
                  initial={{ transform: "translateY(110%)", opacity: 0 }}
                  animate={{ transform: "translateY(0%)", opacity: 1 }}
                  transition={{ delay: 0.22, duration: LINE_DUR, ease: LINE_EASE }}
                  className="block text-primary italic"
                >
                  Unforgettable
                </motion.span>
              </span>

              <span className="block overflow-hidden">
                <motion.span
                  initial={{ transform: "translateY(110%)", opacity: 0 }}
                  animate={{ transform: "translateY(0%)", opacity: 1 }}
                  transition={{ delay: 0.32, duration: LINE_DUR, ease: LINE_EASE }}
                  className="block"
                >
                  Experience
                </motion.span>
              </span>
            </h1>

            {/* Subtext */}
            <motion.p
              initial={{ opacity: 0, transform: "translateY(12px)" }}
              animate={{ opacity: 1, transform: "translateY(0px)" }}
              transition={{ delay: 0.55, duration: 0.5, ease: [0.25, 1, 0.5, 1] }}
              className="text-lg text-muted-foreground max-w-xl mx-auto mb-10 leading-relaxed"
            >
              Concerts, festivals, workshops, and more. Book tickets instantly
              and create memories that last.
            </motion.p>

            {/* Search */}
            <motion.div
              initial={{ opacity: 0, transform: "translateY(12px)" }}
              animate={{ opacity: 1, transform: "translateY(0px)" }}
              transition={{ delay: 0.65, duration: 0.5, ease: [0.25, 1, 0.5, 1] }}
            >
              <SearchBar
                value={query}
                onChange={setQuery}
                onSearch={handleSearch}
                className="max-w-2xl mx-auto mb-10"
              />
            </motion.div>

            {/* Category pills — CSS stagger via --i custom property */}
            <div className="flex flex-wrap justify-center gap-2">
              {categories.map((category, i) => {
                const Icon = category.icon
                return (
                  <button
                    key={category.name}
                    style={{ "--i": i + 12 } as React.CSSProperties}
                    className="stagger-item btn-press flex items-center gap-1.5 px-4 py-1.5 rounded-full text-sm font-medium text-muted-foreground border border-border/50 hover:border-primary/40 hover:text-foreground hover:bg-primary/[0.06] transition-colors duration-150"
                  >
                    <Icon className="w-3.5 h-3.5" />
                    {category.name}
                  </button>
                )
              })}
            </div>
          </div>

          {/* Stats — stagger each number independently */}
          <div className="flex items-center justify-center gap-12 mt-16 pt-8 border-t border-border/30">
            {stats.map((stat, i) => {
              const Icon = stat.icon
              return (
                <motion.div
                  key={stat.label}
                  initial={{ opacity: 0, transform: "translateY(10px)" }}
                  animate={{ opacity: 1, transform: "translateY(0px)" }}
                  transition={{ delay: 0.85 + i * 0.08, duration: 0.4, ease: [0.25, 1, 0.5, 1] }}
                  className="text-center"
                >
                  <p className="text-2xl font-bold text-foreground tracking-tight">{stat.value}</p>
                  <p className="text-xs text-muted-foreground mt-0.5 flex items-center gap-1 justify-center">
                    <Icon className="w-3 h-3" />
                    {stat.label}
                  </p>
                </motion.div>
              )
            })}
          </div>
        </div>
      </section>

      {/* ── Events Section ── */}
      <section className="py-16">
        <div className="container mx-auto px-4 lg:px-8">
          <div className="flex items-center justify-between mb-10">
            <div>
              <h2 className="text-2xl font-bold text-foreground tracking-tight mb-1">
                {query ? "Search Results" : "Upcoming Events"}
              </h2>
              <p className="text-sm text-muted-foreground">
                {query ? `Results for "${query}"` : "Don't miss out on these amazing events"}
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

      {/* ── CTA Section ── */}
      <section className="py-16">
        <div className="container mx-auto px-4 lg:px-8">
          <motion.div
            ref={ctaRef}
            initial={{ opacity: 0, transform: "translateY(20px)" }}
            animate={ctaInView ? { opacity: 1, transform: "translateY(0px)" } : {}}
            transition={{ duration: 0.55, ease: [0.22, 1, 0.36, 1] }}
            className="relative overflow-hidden rounded-2xl border border-border/40 p-10 md:p-14 text-center"
            style={{
              background: "linear-gradient(135deg, oklch(0.1 0.011 265 / 0.8) 0%, oklch(0.08 0.011 265 / 0.9) 100%)",
            }}
          >
            <div className="absolute inset-0 pointer-events-none">
              <div className="absolute inset-0 gradient-mesh opacity-40" />
            </div>

            <div className="relative">
              <h2 className="text-3xl md:text-4xl font-bold text-foreground tracking-tight mb-4">
                Ready to Host Your Event?
              </h2>
              <p className="text-muted-foreground max-w-md mx-auto mb-8 text-sm leading-relaxed">
                Join thousands of organizers who use VenueSync to manage their events.
                Start selling tickets in minutes.
              </p>
              <Link to="/organizers">
                <Button
                  size="lg"
                  className="btn-press gradient-primary text-white shadow-lg shadow-primary/20 font-semibold px-8"
                >
                  Get Started as Organizer
                  <ArrowRight className="w-4 h-4 ml-2" />
                </Button>
              </Link>
            </div>
          </motion.div>
        </div>
      </section>

      <Footer />
    </PageContainer>
  )
}

export default AttendeeLandingPage
