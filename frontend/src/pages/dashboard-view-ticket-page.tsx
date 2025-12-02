"use client"

import type React from "react"

import { type TicketDetails, TicketStatus } from "@/domain/domain"
import { getTicket, getTicketQr } from "@/lib/api"
import { format } from "date-fns"
import { Calendar, MapPin, Tag, DollarSign, ArrowLeft, Download, QrCode, CheckCircle, Clock, XCircle, Ticket, RefreshCw } from "lucide-react"
import { useEffect, useState, useCallback } from "react"
import { useAuth } from "react-oidc-context"
import { useParams, useNavigate } from "react-router"
import Navbar from "@/components/layout/navbar"
import PageContainer from "@/components/layout/page-container"
import { Button } from "@/components/ui/button"
import { motion } from "framer-motion"
import { Skeleton } from "@/components/common/loading-skeleton"
import { parseWallClockDate } from "@/lib/date-utils"

const AUTO_REFRESH_INTERVAL = 10000 // 10 seconds

const statusConfig: Record<TicketStatus, { label: string; className: string; icon: React.ReactNode; description: string }> = {
  [TicketStatus.PURCHASED]: {
    label: "Valid",
    className: "bg-green-500/20 text-green-400 border-green-500/30",
    icon: <Ticket className="w-4 h-4" />,
    description: "Present this QR code at the venue for entry",
  },
  [TicketStatus.USED]: {
    label: "Used",
    className: "bg-blue-500/20 text-blue-400 border-blue-500/30",
    icon: <CheckCircle className="w-4 h-4" />,
    description: "This ticket has already been scanned at the venue",
  },
  [TicketStatus.EXPIRED]: {
    label: "Expired",
    className: "bg-yellow-500/20 text-yellow-400 border-yellow-500/30",
    icon: <Clock className="w-4 h-4" />,
    description: "This ticket has expired as the event has ended",
  },
  [TicketStatus.CANCELLED]: {
    label: "Cancelled",
    className: "bg-destructive/20 text-destructive border-destructive/30",
    icon: <XCircle className="w-4 h-4" />,
    description: "This ticket has been cancelled",
  },
}

const DashboardViewTicketPage: React.FC = () => {
  const [ticket, setTicket] = useState<TicketDetails | undefined>()
  const [qrCodeUrl, setQrCodeUrl] = useState<string | undefined>()
  const [isLoading, setIsLoading] = useState(true)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [error, setError] = useState<string | undefined>()
  const [lastUpdated, setLastUpdated] = useState<Date | undefined>()

  const { id } = useParams()
  const { isLoading: isAuthLoading, user } = useAuth()
  const navigate = useNavigate()

  const fetchTicketData = useCallback(async (showLoading = true) => {
    if (!user?.access_token || !id) return

    if (showLoading) {
      setIsLoading(true)
    } else {
      setIsRefreshing(true)
    }
    
    try {
      const [ticketData, qrBlob] = await Promise.all([
        getTicket(user.access_token, id),
        getTicketQr(user.access_token, id),
      ])
      setTicket(ticketData)
      setQrCodeUrl(URL.createObjectURL(qrBlob))
      setLastUpdated(new Date())
      setError(undefined)
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message)
      } else {
        setError("An unknown error has occurred")
      }
    } finally {
      setIsLoading(false)
      setIsRefreshing(false)
    }
  }, [user?.access_token, id])

  // Initial fetch
  useEffect(() => {
    if (isAuthLoading || !user?.access_token || !id) return
    fetchTicketData(true)
  }, [user?.access_token, isAuthLoading, id, fetchTicketData])

  // Auto-refresh every 10 seconds for active tickets
  useEffect(() => {
    if (!ticket || ticket.status === TicketStatus.EXPIRED || ticket.status === TicketStatus.CANCELLED) {
      return
    }

    const intervalId = setInterval(() => {
      fetchTicketData(false)
    }, AUTO_REFRESH_INTERVAL)

    return () => clearInterval(intervalId)
  }, [ticket, fetchTicketData])

  // Cleanup QR code URL on unmount
  useEffect(() => {
    return () => {
      if (qrCodeUrl) {
        URL.revokeObjectURL(qrCodeUrl)
      }
    }
  }, [qrCodeUrl])

  if (isLoading) {
    return (
      <PageContainer>
        <Navbar />
        <div className="container mx-auto px-4 lg:px-8 pt-24 pb-12 flex justify-center">
          <div className="w-full max-w-md space-y-6">
            <Skeleton className="h-[500px] rounded-3xl" />
          </div>
        </div>
      </PageContainer>
    )
  }

  if (error || !ticket) {
    return (
      <PageContainer>
        <Navbar />
        <div className="container mx-auto px-4 lg:px-8 pt-24 pb-12">
          <div className="text-center">
            <p className="text-destructive mb-4">{error || "Ticket not found"}</p>
            <Button 
              variant="outline" 
              className="gap-2 bg-transparent"
              onClick={() => navigate(-1)}
            >
              <ArrowLeft className="w-4 h-4" />
              Go Back
            </Button>
          </div>
        </div>
      </PageContainer>
    )
  }

  const status = statusConfig[ticket.status]
  const isInactive = ticket.status === TicketStatus.USED || 
                     ticket.status === TicketStatus.EXPIRED || 
                     ticket.status === TicketStatus.CANCELLED

  return (
    <PageContainer>
      <Navbar />

      <div className="container mx-auto px-4 lg:px-8 pt-24 pb-12">
        {/* Back Button and Refresh */}
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={() => navigate(-1)}
            className="inline-flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Go Back
          </button>
          
          <button
            onClick={() => fetchTicketData(false)}
            disabled={isRefreshing}
            className="inline-flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors disabled:opacity-50"
          >
            <RefreshCw className={`w-4 h-4 ${isRefreshing ? 'animate-spin' : ''}`} />
            {isRefreshing ? 'Refreshing...' : 'Refresh'}
          </button>
        </div>

        <div className="flex justify-center">
          <motion.div initial={{ opacity: 0, y: 30 }} animate={{ opacity: 1, y: 0 }} className="w-full max-w-md">
            {/* Ticket Card */}
            <div className="relative">
              {/* Main Ticket */}
              <div className={`relative rounded-3xl p-1 ${isInactive ? 'bg-muted/50' : 'gradient-primary'}`}>
                {/* Cutouts that sit on top of the gradient border */}
                <div className="absolute left-0 top-1/2 w-5 h-10 -ml-2 rounded-r-full z-10" style={{ backgroundColor: 'hsl(var(--background))' }} />
                <div className="absolute right-0 top-1/2 w-5 h-10 -mr-2 rounded-l-full z-10" style={{ backgroundColor: 'hsl(var(--background))' }} />
                
                <div className="bg-background/95 backdrop-blur-xl rounded-[22px] p-6 relative">
                  {/* Status Badge */}
                  <div className="flex flex-col items-center mb-6">
                    <span className={`px-4 py-1.5 rounded-full text-sm font-medium border flex items-center gap-2 ${status.className}`}>
                      {status.icon}
                      {status.label}
                    </span>
                    {lastUpdated && (
                      <p className="text-xs text-muted-foreground mt-2 flex items-center gap-1">
                        {isRefreshing && <RefreshCw className="w-3 h-3 animate-spin" />}
                        Updated {format(lastUpdated, "h:mm:ss a")}
                      </p>
                    )}
                  </div>

                  {/* Event Info */}
                  <div className="text-center mb-6">
                    <h1 className="text-2xl font-bold text-foreground mb-2">{ticket.eventName}</h1>
                    <div className="flex items-center justify-center gap-2 text-muted-foreground">
                      <MapPin className="w-4 h-4" />
                      <span>{ticket.eventVenue}</span>
                    </div>
                  </div>

                  {/* Date/Time */}
                  <div className="flex items-center justify-center gap-2 text-muted-foreground mb-8">
                    <Calendar className="w-4 h-4" />
                    <span>
                      {format(parseWallClockDate(ticket.eventStart)!, "EEE, MMM d, yyyy")} at{" "}
                      {format(parseWallClockDate(ticket.eventStart)!, "h:mm a")}
                    </span>
                  </div>

                  {/* QR Code */}
                  <div className="flex justify-center mb-6">
                    <motion.div
                      initial={{ scale: 0.8, opacity: 0 }}
                      animate={{ scale: 1, opacity: 1 }}
                      transition={{ delay: 0.2 }}
                      className={`bg-white p-4 rounded-2xl shadow-lg ${isInactive ? 'opacity-50 grayscale' : ''}`}
                    >
                      {qrCodeUrl ? (
                        <img
                          src={qrCodeUrl || "/placeholder.svg"}
                          alt="Ticket QR Code"
                          className="w-40 h-40 object-contain"
                        />
                      ) : (
                        <div className="w-40 h-40 flex items-center justify-center">
                          <QrCode className="w-16 h-16 text-muted-foreground" />
                        </div>
                      )}
                    </motion.div>
                  </div>

                  <p className={`text-center text-sm mb-6 ${isInactive ? 'text-muted-foreground' : 'text-muted-foreground'}`}>
                    {status.description}
                  </p>

                  {/* Divider with dashed line */}
                  <div className="relative my-6">
                    <div className="border-t-2 border-dashed border-border" />
                  </div>

                  {/* Ticket Details */}
                  <div className="space-y-4">
                    <div className="flex items-center gap-3">
                      <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${isInactive ? 'bg-muted/50' : 'bg-primary/10'}`}>
                        <Tag className={`w-5 h-5 ${isInactive ? 'text-muted-foreground' : 'text-primary'}`} />
                      </div>
                      <div>
                        <p className="text-sm text-muted-foreground">Ticket Type</p>
                        <p className="font-medium text-foreground">{ticket.description}</p>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <div className={`w-10 h-10 rounded-lg flex items-center justify-center ${isInactive ? 'bg-muted/50' : 'bg-primary/10'}`}>
                        <DollarSign className={`w-5 h-5 ${isInactive ? 'text-muted-foreground' : 'text-primary'}`} />
                      </div>
                      <div>
                        <p className="text-sm text-muted-foreground">Price Paid</p>
                        <p className="font-medium text-foreground">${ticket.price}</p>
                      </div>
                    </div>
                  </div>

                  {/* Ticket ID */}
                  <div className="mt-6 pt-4 border-t border-border text-center">
                    <p className="text-xs text-muted-foreground mb-1">Ticket ID</p>
                    <p className="font-mono text-sm text-foreground">{ticket.id}</p>
                  </div>
                </div>
              </div>

              {/* Download Button */}
              <div className="mt-6">
                <Button
                  variant="outline"
                  className="w-full gap-2 glass border-border/50 h-12 bg-transparent"
                  onClick={() => {
                    // Could implement PDF download here
                  }}
                >
                  <Download className="w-4 h-4" />
                  Download Ticket
                </Button>
              </div>
            </div>
          </motion.div>
        </div>
      </div>
    </PageContainer>
  )
}

export default DashboardViewTicketPage
