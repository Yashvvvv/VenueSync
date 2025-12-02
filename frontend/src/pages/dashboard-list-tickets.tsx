"use client"

import type React from "react"

import Navbar from "@/components/layout/navbar"
import PageContainer from "@/components/layout/page-container"
import { Pagination } from "@/components/common/pagination"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import type { SpringBootPagination, TicketSummary } from "@/domain/domain"
import { listTickets } from "@/lib/api"
import { AlertCircle, Ticket, Wallet } from "lucide-react"
import { useEffect, useState } from "react"
import { useAuth } from "react-oidc-context"
import { motion } from "framer-motion"
import TicketCard from "@/components/tickets/ticket-card"
import { NoTickets } from "@/components/common/empty-state"
import { TicketCardSkeleton } from "@/components/common/loading-skeleton"
import { Button } from "@/components/ui/button"
import { Link } from "react-router"

const DashboardListTickets: React.FC = () => {
  const { isLoading: isAuthLoading, user } = useAuth()
  const [tickets, setTickets] = useState<SpringBootPagination<TicketSummary> | undefined>()
  const [error, setError] = useState<string | undefined>()
  const [isLoading, setIsLoading] = useState(true)
  const [page, setPage] = useState(0)

  useEffect(() => {
    if (isAuthLoading || !user?.access_token) return

    const fetchTickets = async () => {
      setIsLoading(true)
      try {
        setTickets(await listTickets(user.access_token, page))
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
  }, [isAuthLoading, user?.access_token, page])

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
        {tickets && tickets.content.length > 0 && (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-8"
          >
            <div className="glass rounded-xl p-4 text-center">
              <p className="text-2xl font-bold text-foreground">{tickets.totalElements}</p>
              <p className="text-sm text-muted-foreground">Total Tickets</p>
            </div>
            <div className="glass rounded-xl p-4 text-center">
              <p className="text-2xl font-bold text-green-400">
                {tickets.content.filter((t) => t.status === "PURCHASED").length}
              </p>
              <p className="text-sm text-muted-foreground">Active</p>
            </div>
            <div className="glass rounded-xl p-4 text-center">
              <p className="text-2xl font-bold text-primary">
                ${tickets.content.reduce((sum, t) => sum + t.ticketType.price, 0).toFixed(2)}
              </p>
              <p className="text-sm text-muted-foreground">Total Value</p>
            </div>
            <div className="glass rounded-xl p-4 text-center">
              <p className="text-2xl font-bold text-foreground">{tickets.totalPages}</p>
              <p className="text-sm text-muted-foreground">Pages</p>
            </div>
          </motion.div>
        )}

        {/* Tickets List */}
        {isLoading ? (
          <div className="space-y-4 max-w-2xl mx-auto">
            {Array.from({ length: 4 }).map((_, i) => (
              <TicketCardSkeleton key={i} />
            ))}
          </div>
        ) : tickets?.content.length === 0 ? (
          <NoTickets />
        ) : (
          <div className="space-y-4 max-w-2xl mx-auto">
            {tickets?.content.map((ticket, index) => (
              <TicketCard key={ticket.id} ticket={ticket} index={index} />
            ))}
          </div>
        )}

        {tickets && tickets.totalPages > 1 && (
          <div className="mt-8">
            <Pagination pagination={tickets} onPageChange={setPage} />
          </div>
        )}
      </div>
    </PageContainer>
  )
}

export default DashboardListTickets
