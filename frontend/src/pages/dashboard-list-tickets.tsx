"use client"

import type React from "react"

import Navbar from "@/components/layout/navbar"
import PageContainer from "@/components/layout/page-container"
import { Pagination } from "@/components/common/pagination"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import type { SpringBootPagination, TicketSummary } from "@/domain/domain"
import { TicketStatus } from "@/domain/domain"
import { listTickets } from "@/lib/api"
import { AlertCircle, Ticket, Wallet, Calendar, History } from "lucide-react"
import { useEffect, useState } from "react"
import { useAuth } from "react-oidc-context"
import { motion } from "framer-motion"
import TicketCard from "@/components/tickets/ticket-card"
import { NoTickets } from "@/components/common/empty-state"
import { TicketCardSkeleton } from "@/components/common/loading-skeleton"
import { Button } from "@/components/ui/button"
import { Link } from "react-router"
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/tabs"

type TicketFilter = "active" | "past"

const DashboardListTickets: React.FC = () => {
  const { isLoading: isAuthLoading, user } = useAuth()
  const [activeTickets, setActiveTickets] = useState<SpringBootPagination<TicketSummary> | undefined>()
  const [pastTickets, setPastTickets] = useState<SpringBootPagination<TicketSummary> | undefined>()
  const [error, setError] = useState<string | undefined>()
  const [isLoading, setIsLoading] = useState(true)
  const [activePage, setActivePage] = useState(0)
  const [pastPage, setPastPage] = useState(0)
  const [currentTab, setCurrentTab] = useState<TicketFilter>("active")

  useEffect(() => {
    if (isAuthLoading || !user?.access_token) return

    const fetchTickets = async () => {
      setIsLoading(true)
      try {
        const [active, past] = await Promise.all([
          listTickets(user.access_token, activePage, "active"),
          listTickets(user.access_token, pastPage, "past"),
        ])
        setActiveTickets(active)
        setPastTickets(past)
      } catch (err) {
        if (err instanceof Error) {
          setError(err.message)
        } else {
          setError("An unknown error occurred")
        }
      } finally {
        setIsLoading(false)
      }
    }

    fetchTickets()
  }, [isAuthLoading, user?.access_token, activePage, pastPage])

  const currentTickets = currentTab === "active" ? activeTickets : pastTickets
  const currentPage = currentTab === "active" ? activePage : pastPage
  const setCurrentPage = currentTab === "active" ? setActivePage : setPastPage

  // Calculate stats
  const totalActive = activeTickets?.totalElements ?? 0
  const totalPast = pastTickets?.totalElements ?? 0
  const totalUsed = pastTickets?.content.filter((t) => t.status === TicketStatus.USED).length ?? 0
  const totalExpired = pastTickets?.content.filter((t) => t.status === TicketStatus.EXPIRED).length ?? 0

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

  return (
    <PageContainer>
      <Navbar />

      <div className="container mx-auto px-4 lg:px-8 pt-24 pb-12">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-8"
        >
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl gradient-primary flex items-center justify-center">
              <Wallet className="w-7 h-7 text-white" />
            </div>
            <div>
              <h1 className="text-3xl font-bold text-foreground">My Tickets</h1>
              <p className="text-muted-foreground">Your purchased event tickets</p>
            </div>
          </div>
          <Link to="/">
            <Button variant="outline" className="gap-2 glass border-border/50 bg-transparent">
              <Ticket className="w-4 h-4" />
              Browse Events
            </Button>
          </Link>
        </motion.div>

        {/* Stats */}
        {!isLoading && (totalActive > 0 || totalPast > 0) && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8"
          >
            <div className="glass rounded-xl p-4 text-center">
              <p className="text-2xl font-bold text-green-400">{totalActive}</p>
              <p className="text-sm text-muted-foreground">Upcoming</p>
            </div>
            <div className="glass rounded-xl p-4 text-center">
              <p className="text-2xl font-bold text-blue-400">{totalUsed}</p>
              <p className="text-sm text-muted-foreground">Used</p>
            </div>
            <div className="glass rounded-xl p-4 text-center">
              <p className="text-2xl font-bold text-yellow-400">{totalExpired}</p>
              <p className="text-sm text-muted-foreground">Expired</p>
            </div>
            <div className="glass rounded-xl p-4 text-center">
              <p className="text-2xl font-bold text-foreground">{totalActive + totalPast}</p>
              <p className="text-sm text-muted-foreground">Total</p>
            </div>
          </motion.div>
        )}

        {/* Tabs */}
        <Tabs value={currentTab} onValueChange={(v) => setCurrentTab(v as TicketFilter)} className="w-full">
          <TabsList className="grid w-full max-w-md mx-auto grid-cols-2 mb-8">
            <TabsTrigger value="active" className="gap-2">
              <Calendar className="w-4 h-4" />
              Upcoming
              {totalActive > 0 && (
                <span className="ml-1 px-2 py-0.5 text-xs rounded-full bg-green-500/20 text-green-400">
                  {totalActive}
                </span>
              )}
            </TabsTrigger>
            <TabsTrigger value="past" className="gap-2">
              <History className="w-4 h-4" />
              Past
              {totalPast > 0 && (
                <span className="ml-1 px-2 py-0.5 text-xs rounded-full bg-muted text-muted-foreground">
                  {totalPast}
                </span>
              )}
            </TabsTrigger>
          </TabsList>

          <TabsContent value="active">
            {isLoading ? (
              <div className="space-y-4 max-w-2xl mx-auto">
                {Array.from({ length: 4 }).map((_, i) => (
                  <TicketCardSkeleton key={i} />
                ))}
              </div>
            ) : activeTickets?.content.length === 0 ? (
              <NoTickets />
            ) : (
              <div className="space-y-4 max-w-2xl mx-auto">
                {activeTickets?.content.map((ticket, index) => (
                  <TicketCard key={ticket.id} ticket={ticket} index={index} />
                ))}
              </div>
            )}

            {activeTickets && activeTickets.totalPages > 1 && (
              <div className="mt-8">
                <Pagination pagination={activeTickets} onPageChange={setActivePage} />
              </div>
            )}
          </TabsContent>

          <TabsContent value="past">
            {isLoading ? (
              <div className="space-y-4 max-w-2xl mx-auto">
                {Array.from({ length: 4 }).map((_, i) => (
                  <TicketCardSkeleton key={i} />
                ))}
              </div>
            ) : pastTickets?.content.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-16 text-center">
                <div className="w-20 h-20 rounded-full bg-muted/50 flex items-center justify-center mb-4">
                  <History className="w-10 h-10 text-muted-foreground" />
                </div>
                <h3 className="text-xl font-semibold text-foreground mb-2">No Past Tickets</h3>
                <p className="text-muted-foreground max-w-md">
                  Your used and expired tickets will appear here after events have passed.
                </p>
              </div>
            ) : (
              <div className="space-y-4 max-w-2xl mx-auto">
                {pastTickets?.content.map((ticket, index) => (
                  <TicketCard key={ticket.id} ticket={ticket} index={index} />
                ))}
              </div>
            )}

            {pastTickets && pastTickets.totalPages > 1 && (
              <div className="mt-8">
                <Pagination pagination={pastTickets} onPageChange={setPastPage} />
              </div>
            )}
          </TabsContent>
        </Tabs>
      </div>
    </PageContainer>
  )
}

export default DashboardListTickets
