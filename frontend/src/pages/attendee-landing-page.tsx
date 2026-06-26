"use client"

import type React from "react"

import { useAuth } from "react-oidc-context"
import { useEffect, useState } from "react"
import type { PublishedEventSummary, SpringBootPagination } from "@/domain/domain"
import { listPublishedEvents, searchPublishedEvents } from "@/lib/api"
import { motion } from "framer-motion"
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

const AttendeeLandingPage: React.FC = () => {
  const { isLoading: isAuthLoading } = useAuth()

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
        {/* Single ambient glow — much more restrained */}
        <div className="absolute inset-0 pointer-events-none">
          <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[720px] h-[420px] bg-primary/[0.07] rounded-full blur-[100px]" />
        </div>

        <div className="container mx-auto px-4 lg:px-8 relative">
          <motion.div
            initial={{ opacity: 0, y: 28 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.55, ease: [0.16, 1, 0.3, 1] }}
            className="text-center max-w-4xl mx-auto"
          >
            {/* Eyebrow */}
            <motion.div
              initial={{ opacity: 0, y: 12 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.05, duration: 0.4 }}
              className="eyebrow mb-8"
            >
              Discover Amazing Events
            </motion.div>

            {/* Main headline */}
            <h1 className="hero-heading mb-6 text-balance">
              Find Your Next<br />
              <span className="gradient-text">Unforgettable</span>
              <br />Experience
            </h1>

            <p className="text-lg text-muted-foreground max-w-xl mx-auto mb-10 leading-relaxed">
              Discover concerts, festivals, workshops, and more. Book tickets
              seamlessly and create memories that last a lifetime.
            </p>

            {/* Search */}
            <SearchBar
              value={query}
              onChange={setQuery}
              onSearch={handleSearch}
              className="max-w-2xl mx-auto mb-10"
            />

            {/* Category pills — clean, minimal */}
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.2, duration: 0.4 }}
              className="flex flex-wrap justify-center gap-2"
            >
              {categories.map((category) => {
                const Icon = category.icon
                return (
                  <button
                    key={category.name}
                    className="flex items-center gap-1.5 px-4 py-1.5 rounded-full text-sm font-medium text-muted-foreground border border-border/50 hover:border-primary/40 hover:text-foreground hover:bg-primary/[0.06] transition-all duration-150"
                  >
                    <Icon className="w-3.5 h-3.5" />
                    {category.name}
                  </button>
                )
              })}
            </motion.div>
          </motion.div>

          {/* Stats — understated horizontal row */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.35, duration: 0.4 }}
            className="flex items-center justify-center gap-12 mt-16 pt-8 border-t border-border/30"
          >
            {stats.map((stat, i) => {
              const Icon = stat.icon
              return (
                <div key={stat.label} className="text-center">
                  <p className="text-2xl font-bold text-foreground tracking-tight">{stat.value}</p>
                  <p className="text-xs text-muted-foreground mt-0.5 flex items-center gap-1 justify-center">
                    <Icon className="w-3 h-3" />
                    {stat.label}
                  </p>
                </div>
              )
            })}
          </motion.div>
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
              <Button variant="ghost" size="sm" className="gap-1.5 text-muted-foreground hover:text-foreground">
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
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
            className="relative overflow-hidden rounded-2xl border border-border/40 bg-card/40 p-10 md:p-14 text-center"
            style={{
              background: "linear-gradient(135deg, oklch(0.1 0.011 265 / 0.8) 0%, oklch(0.08 0.011 265 / 0.9) 100%)",
            }}
          >
            {/* Subtle ambient glow inside CTA */}
            <div className="absolute inset-0 pointer-events-none">
              <div className="absolute inset-0 gradient-mesh opacity-40" />
            </div>

            <div className="relative">
              <div className="eyebrow mx-auto mb-6">For Organizers</div>
              <h2 className="text-3xl md:text-4xl font-bold text-foreground tracking-tight mb-4">
                Ready to Host Your Event?
              </h2>
              <p className="text-muted-foreground max-w-md mx-auto mb-8 text-sm leading-relaxed">
                Join thousands of organizers who trust VenueSync to manage their events.
                Start selling tickets in minutes.
              </p>
              <Link to="/organizers">
                <Button
                  size="lg"
                  className="gradient-primary text-white shadow-lg shadow-primary/20 font-semibold px-8"
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
