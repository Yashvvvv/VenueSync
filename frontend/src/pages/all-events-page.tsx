"use client"

import type React from "react"

import { useCallback, useEffect, useState } from "react"
import type { PublishedEventSummary, SpringBootPagination } from "@/domain/domain"
import { listAllPublishedEvents, searchAllPublishedEvents } from "@/lib/api"
import { motion, useReducedMotion } from "framer-motion"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"
import SearchBar from "@/components/forms/search-bar"
import EventGrid from "@/components/events/event-grid"
import { Pagination } from "@/components/common/pagination"
import { Button } from "@/components/ui/button"
import { ArrowLeft, WarningCircle } from "@/components/icons"
import { Link } from "react-router"

const AllEventsPage: React.FC = () => {
  const reduce = useReducedMotion()
  const [page, setPage] = useState(0)
  const [publishedEvents, setPublishedEvents] = useState<
    SpringBootPagination<PublishedEventSummary> | undefined
  >()
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [query, setQuery] = useState("")

  const runSearch = useCallback(async (term: string, pageNum: number) => {
    setIsLoading(true)
    setError(null)
    try {
      const result = term.trim()
        ? await searchAllPublishedEvents(term.trim(), pageNum)
        : await listAllPublishedEvents(pageNum)
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [page])

  const handleSearch = () => {
    setPage(0)
    runSearch(query, 0)
  }

  const total = publishedEvents?.totalElements

  return (
    <PageContainer>
      <Navbar />

      <section className="border-b border-border py-12 lg:py-16">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <motion.div
            initial={reduce ? false : { opacity: 0, y: 16 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, ease: [0.16, 1, 0.3, 1] }}
          >
            <Link
              to="/"
              className="focus-ring inline-flex items-center gap-1.5 rounded-sm text-sm text-muted-foreground transition-colors hover:text-foreground"
            >
              <ArrowLeft weight="bold" size={14} />
              Discover
            </Link>

            <div className="mt-6 flex flex-wrap items-end justify-between gap-6">
              <div>
                <h1 className="display-section">All events</h1>
                <p className="mt-2 text-sm text-muted-foreground">
                  {typeof total === "number" ? (
                    <>
                      <span className="font-mono tabular-nums text-foreground">{total}</span>{" "}
                      {total === 1 ? "event" : "events"} listed
                    </>
                  ) : (
                    "Everything currently published on VenueSync"
                  )}
                </p>
              </div>

              <SearchBar
                value={query}
                onChange={setQuery}
                onSearch={handleSearch}
                className="w-full max-w-xl"
              />
            </div>
          </motion.div>
        </div>
      </section>

      <section className="py-12 lg:py-16">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
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
              {/* EventGrid already renders the empty state, so there is no
                  second "no events found" block competing with it. */}
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

      <Footer />
    </PageContainer>
  )
}

export default AllEventsPage
