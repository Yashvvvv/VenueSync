"use client"

import type React from "react"

import { useAuth } from "react-oidc-context"
import { useEffect, useState } from "react"
import type { PublishedEventSummary, SpringBootPagination } from "@/domain/domain"
import { listPublishedEvents, searchPublishedEvents } from "@/lib/api"
import { motion } from "framer-motion"
import { Sparkles, TrendingUp, Users, Calendar, ArrowRight } from "lucide-react"
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
  { name: "Music", emoji: "🎵" },
  { name: "Sports", emoji: "⚽" },
  { name: "Arts", emoji: "🎨" },
  { name: "Tech", emoji: "💻" },
  { name: "Food", emoji: "🍕" },
  { name: "Comedy", emoji: "😂" },
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

      {/* Hero Section */}
      <section className="relative pt-24 pb-16 overflow-hidden">
        {/* Background Effects */}
        <div className="absolute inset-0 gradient-mesh opacity-50" />
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[800px] bg-primary/20 rounded-full blur-3xl" />

        <div className="container mx-auto px-4 lg:px-8 relative">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="text-center max-w-4xl mx-auto mb-12"
          >
            <div className="inline-flex items-center gap-2 glass px-4 py-2 rounded-full mb-6">
              <Sparkles className="w-4 h-4 text-primary" />
              <span className="text-sm text-foreground">Discover Amazing Events</span>
            </div>

            <h1 className="text-4xl md:text-6xl lg:text-7xl font-bold text-foreground mb-6 text-balance leading-tight">
              Find Your Next
              <span className="block text-gradient">Unforgettable Experience</span>
            </h1>

            <p className="text-lg text-muted-foreground max-w-2xl mx-auto mb-8">
              Discover concerts, festivals, workshops, and more. Book tickets seamlessly and create memories that last a
              lifetime.
            </p>

            {/* Search Bar */}
            <SearchBar value={query} onChange={setQuery} onSearch={handleSearch} className="max-w-2xl mx-auto" />
          </motion.div>

          {/* Categories */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3, duration: 0.5 }}
            className="flex flex-wrap justify-center gap-3 mb-12"
          >
            {categories.map((category) => (
              <button
                key={category.name}
                className="glass px-4 py-2 rounded-full text-sm font-medium text-foreground hover:bg-primary/10 hover:text-primary hover:border-primary/30 transition-all"
              >
                <span className="mr-2">{category.emoji}</span>
                {category.name}
              </button>
            ))}
          </motion.div>

          {/* Stats */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4, duration: 0.5 }}
            className="grid grid-cols-3 gap-4 max-w-xl mx-auto"
          >
            {stats.map((stat) => {
              const Icon = stat.icon
              return (
                <div key={stat.label} className="text-center glass rounded-xl p-4">
                  <Icon className="w-5 h-5 text-primary mx-auto mb-2" />
                  <p className="text-2xl font-bold text-foreground">{stat.value}</p>
                  <p className="text-xs text-muted-foreground">{stat.label}</p>
                </div>
              )
            })}
          </motion.div>
        </div>
      </section>

      {/* Events Section */}
      <section className="py-16">
        <div className="container mx-auto px-4 lg:px-8">
          <div className="flex items-center justify-between mb-8">
            <div>
              <h2 className="text-2xl md:text-3xl font-bold text-foreground mb-2">
                {query ? "Search Results" : "Upcoming Events"}
              </h2>
              <p className="text-muted-foreground">
                {query ? `Showing results for "${query}"` : "Don't miss out on these amazing events"}
              </p>
            </div>
            <Link to="/events">
              <Button variant="ghost" className="gap-2 text-primary">
                View All
                <ArrowRight className="w-4 h-4" />
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

      {/* CTA Section */}
      <section className="py-16">
        <div className="container mx-auto px-4 lg:px-8">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            className="relative overflow-hidden rounded-3xl gradient-primary p-8 md:p-12 text-center"
          >
            <div className="absolute inset-0 bg-[url('/organizers-landing-hero.png')] bg-cover bg-center opacity-20" />
            <div className="relative">
              <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">Ready to Host Your Event?</h2>
              <p className="text-white/80 max-w-xl mx-auto mb-8">
                Join thousands of organizers who trust EventHub to manage their events. Start selling tickets in
                minutes.
              </p>
              <Link to="/organizers">
                <Button size="lg" className="bg-white text-primary hover:bg-white/90 font-semibold">
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
