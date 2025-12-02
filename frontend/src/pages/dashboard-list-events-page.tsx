"use client"

import type React from "react"

import Navbar from "@/components/layout/navbar"
import PageContainer from "@/components/layout/page-container"
import { Pagination } from "@/components/common/pagination"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import { Button } from "@/components/ui/button"
import { type EventSummary, EventStatusEnum, type SpringBootPagination } from "@/domain/domain"
import { deleteEvent, listEvents } from "@/lib/api"
import { AlertCircle, Calendar, Clock, Edit, MapPin, Plus, Tag, Trash2, MoreVertical } from "lucide-react"
import { useEffect, useState } from "react"
import { useAuth } from "react-oidc-context"
import { Link } from "react-router"
import { motion } from "framer-motion"
import { format } from "date-fns"
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { EmptyState } from "@/components/common/empty-state"
import { EventCardSkeleton } from "@/components/common/loading-skeleton"
import toast from "react-hot-toast"

const statusConfig: Record<EventStatusEnum, { label: string; className: string }> = {
  [EventStatusEnum.DRAFT]: {
    label: "Draft",
    className: "bg-muted text-muted-foreground",
  },
  [EventStatusEnum.PUBLISHED]: {
    label: "Published",
    className: "bg-green-500/20 text-green-400",
  },
  [EventStatusEnum.CANCELLED]: {
    label: "Cancelled",
    className: "bg-destructive/20 text-destructive",
  },
  [EventStatusEnum.COMPLETED]: {
    label: "Completed",
    className: "bg-primary/20 text-primary",
  },
}

const DashboardListEventsPage: React.FC = () => {
  const { isLoading: isAuthLoading, user } = useAuth()
  const [events, setEvents] = useState<SpringBootPagination<EventSummary> | undefined>()
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | undefined>()
  const [deleteEventError, setDeleteEventError] = useState<string | undefined>()
  const [page, setPage] = useState(0)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [eventToDelete, setEventToDelete] = useState<EventSummary | undefined>()

  useEffect(() => {
    if (isAuthLoading || !user?.access_token) return
    refreshEvents(user.access_token)
  }, [isAuthLoading, user, page])

  const refreshEvents = async (accessToken: string) => {
    setIsLoading(true)
    try {
      setEvents(await listEvents(accessToken, page))
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message)
      } else {
        setError("An unknown error has occurred")
      }
    } finally {
      setIsLoading(false)
    }
  }

  const handleDeleteEvent = async () => {
    if (!eventToDelete || isAuthLoading || !user?.access_token) return

    try {
      setDeleteEventError(undefined)
      await deleteEvent(user.access_token, eventToDelete.id)
      toast.success("Event deleted successfully")
      setEventToDelete(undefined)
      setDialogOpen(false)
      refreshEvents(user.access_token)
    } catch (err) {
      if (err instanceof Error) {
        setDeleteEventError(err.message)
      } else {
        setDeleteEventError("An unknown error has occurred")
      }
    }
  }

  const formatDate = (date?: Date) => {
    if (!date) return "TBD"
    return format(new Date(date), "MMM d, yyyy")
  }

  const formatTime = (date?: Date) => {
    if (!date) return ""
    return format(new Date(date), "h:mm a")
  }

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
          className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-10"
        >
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 rounded-2xl gradient-primary flex items-center justify-center shadow-lg shadow-primary/25">
              <Calendar className="w-7 h-7 text-white" />
            </div>
            <div>
              <h1 className="text-3xl font-bold text-foreground">My Events</h1>
              <p className="text-muted-foreground">Manage and track your events</p>
            </div>
          </div>
          <Link to="/dashboard/events/create">
            <Button className="gradient-primary text-white gap-2 shadow-lg shadow-primary/25">
              <Plus className="w-4 h-4" />
              Create Event
            </Button>
          </Link>
        </motion.div>

        {/* Events Grid */}
        {isLoading ? (
          <div className="grid md:grid-cols-2 gap-6">
            {Array.from({ length: 4 }).map((_, i) => (
              <EventCardSkeleton key={i} />
            ))}
          </div>
        ) : events?.content.length === 0 ? (
          <EmptyState
            icon={Calendar}
            title="No Events Yet"
            description="Create your first event and start selling tickets to your audience."
            actionLabel="Create Event"
            actionHref="/dashboard/events/create"
          />
        ) : (
          <div className="grid md:grid-cols-2 gap-6">
            {events?.content.map((event, index) => {
              const status = statusConfig[event.status]
              return (
                <motion.div
                  key={event.id}
                  initial={{ opacity: 0, y: 20 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.1 }}
                  className="relative rounded-2xl p-6 transition-all duration-300 hover:translate-y-[-2px] bg-gradient-to-br from-card/80 to-card/40 backdrop-blur-xl border border-border/60 hover:border-primary/40 shadow-lg shadow-black/5 hover:shadow-xl hover:shadow-primary/5"
                >
                  <div className="flex items-start justify-between mb-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-xl font-semibold text-foreground">{event.name}</h3>
                        <span className={`px-2.5 py-0.5 rounded-full text-xs font-medium ${status.className}`}>
                          {status.label}
                        </span>
                      </div>
                    </div>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                          <MoreVertical className="w-4 h-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="glass">
                        <DropdownMenuItem asChild>
                          <Link to={`/dashboard/events/update/${event.id}`} className="flex items-center gap-2">
                            <Edit className="w-4 h-4" />
                            Edit Event
                          </Link>
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          className="text-destructive focus:text-destructive"
                          onClick={() => {
                            setEventToDelete(event)
                            setDialogOpen(true)
                          }}
                        >
                          <Trash2 className="w-4 h-4 mr-2" />
                          Delete Event
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>

                  <div className="space-y-3">
                    <div className="flex items-center gap-3 text-muted-foreground">
                      <Calendar className="w-4 h-4 flex-shrink-0" />
                      <span className="text-sm">
                        {formatDate(event.start)} - {formatDate(event.end)}
                      </span>
                    </div>

                    {event.start && (
                      <div className="flex items-center gap-3 text-muted-foreground">
                        <Clock className="w-4 h-4 flex-shrink-0" />
                        <span className="text-sm">
                          {formatTime(event.start)} - {formatTime(event.end)}
                        </span>
                      </div>
                    )}

                    <div className="flex items-center gap-3 text-muted-foreground">
                      <MapPin className="w-4 h-4 flex-shrink-0" />
                      <span className="text-sm truncate">{event.venue}</span>
                    </div>

                    {event.ticketTypes.length > 0 && (
                      <div className="flex items-start gap-3 text-muted-foreground">
                        <Tag className="w-4 h-4 flex-shrink-0 mt-0.5" />
                        <div className="flex flex-wrap gap-2">
                          {event.ticketTypes.map((ticket) => (
                            <span key={ticket.id} className="text-xs px-2 py-1 rounded-md bg-secondary">
                              {ticket.name} - ${ticket.price}
                            </span>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>

                  <div className="flex gap-2 mt-6 pt-4 border-t border-border">
                    <Link to={`/dashboard/events/update/${event.id}`} className="flex-1">
                      <Button variant="outline" className="w-full glass border-border/50 bg-transparent">
                        <Edit className="w-4 h-4 mr-2" />
                        Edit
                      </Button>
                    </Link>
                    <Button
                      variant="outline"
                      className="glass border-destructive/30 text-destructive hover:bg-destructive/10 bg-transparent"
                      onClick={() => {
                        setEventToDelete(event)
                        setDialogOpen(true)
                      }}
                    >
                      <Trash2 className="w-4 h-4" />
                    </Button>
                  </div>
                </motion.div>
              )
            })}
          </div>
        )}

        {events && events.totalPages > 1 && (
          <div className="mt-8">
            <Pagination pagination={events} onPageChange={setPage} />
          </div>
        )}
      </div>

      {/* Delete Confirmation Dialog */}
      <AlertDialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <AlertDialogContent className="glass">
          <AlertDialogHeader>
            <AlertDialogTitle>Delete Event</AlertDialogTitle>
            <AlertDialogDescription>
              Are you sure you want to delete "{eventToDelete?.name}"? This action cannot be undone.
            </AlertDialogDescription>
          </AlertDialogHeader>
          {deleteEventError && (
            <Alert variant="destructive" className="border-destructive/50">
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>Error</AlertTitle>
              <AlertDescription>{deleteEventError}</AlertDescription>
            </Alert>
          )}
          <AlertDialogFooter>
            <AlertDialogCancel onClick={() => setEventToDelete(undefined)}>Cancel</AlertDialogCancel>
            <AlertDialogAction
              onClick={handleDeleteEvent}
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
            >
              Delete
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </PageContainer>
  )
}

export default DashboardListEventsPage
