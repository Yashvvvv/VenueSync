"use client"

import type React from "react"

import { useEffect, useState } from "react"
import type { PublishedEventSummary, SpringBootPagination } from "@/domain/domain"
import { listAllPublishedEvents, searchAllPublishedEvents } from "@/lib/api"
import { motion } from "framer-motion"
import { Calendar, ArrowLeft } from "lucide-react"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"
import SearchBar from "@/components/forms/search-bar"
import EventGrid from "@/components/events/event-grid"
import { Pagination } from "@/components/common/pagination"
import { Button } from "@/components/ui/button"
import { Link } from "react-router"

const AllEventsPage: React.FC = () => {
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
      setPublishedEvents(await listAllPublishedEvents(page))
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
      setPublishedEvents(await searchAllPublishedEvents(query, page))
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

  return (
    <PageContainer>
      <Navbar />

      {/* Header Section */}
      <section className="relative pt-24 pb-12 overflow-hidden">
        {/* Background Effects */}
        <div className="absolute inset-0 gradient-mesh opacity-30" />

        <div className="container mx-auto px-4 lg:px-8 relative">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
          >
            {/* Back Button */}
            <Link to="/">
              <Button variant="ghost" className="mb-6 gap-2 text-muted-foreground hover:text-foreground">
                <ArrowLeft className="w-4 h-4" />
                Back to Home
              </Button>
            </Link>

            <div className="flex items-center gap-3 mb-4">
              <div className="w-12 h-12 rounded-xl gradient-primary flex items-center justify-center">
                <Calendar className="w-6 h-6 text-white" />
              </div>
              <div>
                <h1 className="text-3xl md:text-4xl font-bold text-foreground">All Events</h1>
                <p className="text-muted-foreground">
                  {publishedEvents?.totalElements 
                    ? `${publishedEvents.totalElements} events available`
                    : "Browse all available events"}
                </p>
              </div>
            </div>

            {/* Search Bar */}
            <div className="mt-8">
              <SearchBar 
                value={query} 
                onChange={setQuery} 
                onSearch={handleSearch} 
                className="max-w-2xl" 
              />
            </div>
          </motion.div>
        </div>
      </section>

      {/* Events Grid Section */}
      <section className="py-8 pb-16">
        <div className="container mx-auto px-4 lg:px-8">
          {query && (
            <motion.div
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              className="mb-6"
            >
              <p className="text-muted-foreground">
                Showing results for "<span className="text-foreground font-medium">{query}</span>"
              </p>
            </motion.div>
          )}

          <EventGrid events={publishedEvents?.content || []} isLoading={isLoading} />

          {publishedEvents && publishedEvents.totalPages > 1 && (
            <div className="mt-12">
              <Pagination pagination={publishedEvents} onPageChange={setPage} />
            </div>
          )}

          {!isLoading && publishedEvents?.content.length === 0 && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="text-center py-16"
            >
              <div className="w-16 h-16 rounded-full bg-muted/50 flex items-center justify-center mx-auto mb-4">
                <Calendar className="w-8 h-8 text-muted-foreground" />
              </div>
              <h3 className="text-lg font-medium text-foreground mb-2">No events found</h3>
              <p className="text-muted-foreground">
                {query ? "Try adjusting your search terms" : "Check back later for new events"}
              </p>
            </motion.div>
          )}
        </div>
      </section>

      <Footer />
    </PageContainer>
  )
}

export default AllEventsPage
