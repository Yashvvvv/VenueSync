"use client"

import type React from "react"

import Navbar from "@/components/layout/navbar"
import PageContainer from "@/components/layout/page-container"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { Badge } from "@/components/ui/badge"
import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Switch } from "@/components/ui/switch"
import { Textarea } from "@/components/ui/textarea"
import {
  type CreateEventRequest,
  type CreateTicketTypeRequest,
  type EventDetails,
  EventStatusEnum,
  type UpdateEventRequest,
  type UpdateTicketTypeRequest,
} from "@/domain/domain"
import { createEvent, getEvent, updateEvent, serializeEventRequest } from "@/lib/api"
import { format } from "date-fns"
import { AlertCircle, CalendarIcon, Edit, Plus, Ticket, Trash2, ArrowLeft, Save } from "lucide-react"
import { useEffect, useState } from "react"
import { useAuth } from "react-oidc-context"
import { useNavigate, useParams, Link } from "react-router"
import { motion } from "framer-motion"
import toast from "react-hot-toast"

interface DateTimeSelectProps {
  date: Date | undefined
  setDate: (date: Date) => void
  time: string | undefined
  setTime: (time: string) => void
  enabled: boolean
  setEnabled: (isEnabled: boolean) => void
  label: string
}

const DateTimeSelect: React.FC<DateTimeSelectProps> = ({
  date,
  setDate,
  time,
  setTime,
  enabled,
  setEnabled,
  label,
}) => {
  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <Label className="text-foreground font-medium">{label}</Label>
        <div className="flex items-center gap-2">
          <span className="text-xs text-muted-foreground">{enabled ? "Enabled" : "Disabled"}</span>
          <Switch checked={enabled} onCheckedChange={setEnabled} />
        </div>
      </div>

      <div className={`flex gap-3 transition-opacity ${enabled ? "opacity-100" : "opacity-50 pointer-events-none"}`}>
        <Popover>
          <PopoverTrigger asChild>
            <Button variant="outline" className="flex-1 justify-start glass border-border/50 bg-transparent">
              <CalendarIcon className="w-4 h-4 mr-2" />
              {date ? format(date, "PPP") : "Select date"}
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-auto p-0 glass" align="start">
            <Calendar
              mode="single"
              selected={date}
              onSelect={(selectedDate) => {
                if (!selectedDate) return
                const correctedDate = new Date(
                  selectedDate.getFullYear(),
                  selectedDate.getMonth(),
                  selectedDate.getDate(),
                )
                setDate(correctedDate)
                setEnabled(true)
              }}
            />
          </PopoverContent>
        </Popover>
        <Input
          type="time"
          className="w-32 bg-secondary border-border text-foreground"
          value={time || ""}
          onChange={(e) => {
            setTime(e.target.value)
            setEnabled(true)
          }}
        />
      </div>
    </div>
  )
}

const generateTempId = () => `temp_${crypto.randomUUID()}`
const isTempId = (id: string | undefined) => id && id.startsWith("temp_")

interface TicketTypeData {
  id: string | undefined
  name: string
  price: number
  totalAvailable?: number
  description: string
}

interface EventData {
  id: string | undefined
  name: string
  startDate: Date | undefined
  startTime: string | undefined
  endDate: Date | undefined
  endTime: string | undefined
  venueDetails: string
  salesStartDate: Date | undefined
  salesStartTime: string | undefined
  salesEndDate: Date | undefined
  salesEndTime: string | undefined
  ticketTypes: TicketTypeData[]
  status: EventStatusEnum
  createdAt: string | undefined
  updatedAt: string | undefined
}

const DashboardManageEventPage: React.FC = () => {
  const { isLoading: isAuthLoading, user } = useAuth()
  const { id } = useParams()
  const isEditMode = !!id
  const navigate = useNavigate()

  const [eventData, setEventData] = useState<EventData>({
    id: undefined,
    name: "",
    startDate: undefined,
    startTime: undefined,
    endDate: undefined,
    endTime: undefined,
    venueDetails: "",
    salesStartDate: undefined,
    salesStartTime: undefined,
    salesEndDate: undefined,
    salesEndTime: undefined,
    ticketTypes: [],
    status: EventStatusEnum.DRAFT,
    createdAt: undefined,
    updatedAt: undefined,
  })

  const [currentTicketType, setCurrentTicketType] = useState<TicketTypeData | undefined>()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [eventDateEnabled, setEventDateEnabled] = useState(false)
  const [eventSalesDateEnabled, setEventSalesDateEnabled] = useState(false)
  const [error, setError] = useState<string | undefined>()
  const [isSubmitting, setIsSubmitting] = useState(false)

  const updateField = (field: keyof EventData, value: unknown) => {
    setEventData((prev) => ({ ...prev, [field]: value }))
  }

  useEffect(() => {
    if (isEditMode && !isAuthLoading && user?.access_token) {
      const fetchEvent = async () => {
        try {
          const event: EventDetails = await getEvent(user.access_token, id)
          setEventData({
            id: event.id,
            name: event.name,
            startDate: event.start ? parseDateFromString(event.start) : undefined,
            startTime: event.start ? formatTimeFromDate(event.start) : undefined,
            endDate: event.end ? parseDateFromString(event.end) : undefined,
            endTime: event.end ? formatTimeFromDate(event.end) : undefined,
            venueDetails: event.venue,
            salesStartDate: event.salesStart ? parseDateFromString(event.salesStart) : undefined,
            salesStartTime: event.salesStart ? formatTimeFromDate(event.salesStart) : undefined,
            salesEndDate: event.salesEnd ? parseDateFromString(event.salesEnd) : undefined,
            salesEndTime: event.salesEnd ? formatTimeFromDate(event.salesEnd) : undefined,
            status: event.status,
            ticketTypes: event.ticketTypes.map((ticket) => ({
              id: ticket.id,
              name: ticket.name,
              description: ticket.description,
              price: ticket.price,
              totalAvailable: ticket.totalAvailable,
            })),
            createdAt: event.createdAt,
            updatedAt: event.updatedAt,
          })
          setEventDateEnabled(!!(event.start || event.end))
          setEventSalesDateEnabled(!!(event.salesStart || event.salesEnd))
        } catch (err) {
          if (err instanceof Error) {
            setError(err.message)
          }
        }
      }
      fetchEvent()
    }
  }, [id, user, isAuthLoading, isEditMode])

  // Extract time from a date string (treats as local/wall clock time)
  const formatTimeFromDate = (dateString: string): string => {
    // Parse the date string and extract time components
    // Backend returns format like "2025-12-01T14:00:00" (no timezone = wall clock time)
    const match = dateString.match(/T(\d{2}):(\d{2})/);
    if (match) {
      return `${match[1]}:${match[2]}`;
    }
    // Fallback: parse as date and get hours/minutes
    const date = new Date(dateString);
    const hours = date.getHours().toString().padStart(2, "0");
    const minutes = date.getMinutes().toString().padStart(2, "0");
    return `${hours}:${minutes}`;
  }

  // Extract date from a date string (treats as local/wall clock time)
  const parseDateFromString = (dateString: string): Date => {
    // Parse "2025-12-01T14:00:00" format
    // Create date from components to avoid timezone shifts
    const match = dateString.match(/(\d{4})-(\d{2})-(\d{2})/);
    if (match) {
      return new Date(parseInt(match[1]), parseInt(match[2]) - 1, parseInt(match[3]));
    }
    return new Date(dateString);
  }

  // Combines date and time into a LocalDateTime string for the backend
  // NO timezone conversion - stores the exact wall clock time the user entered
  const combineDateTime = (date: Date, time: string): string => {
    const year = date.getFullYear();
    const month = (date.getMonth() + 1).toString().padStart(2, "0");
    const day = date.getDate().toString().padStart(2, "0");
    const [hours, minutes] = time.split(":");
    
    // Return LocalDateTime format: "2025-12-01T14:00:00"
    // This is wall clock time - no timezone conversion
    return `${year}-${month}-${day}T${hours}:${minutes}:00`;
  }

  const handleFormSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(undefined)
    setIsSubmitting(true)

    if (isAuthLoading || !user?.access_token) {
      setError("Authentication required")
      setIsSubmitting(false)
      return
    }

    try {
      if (isEditMode && eventData.id) {
        const ticketTypes: UpdateTicketTypeRequest[] = eventData.ticketTypes.map((tt) => ({
          id: isTempId(tt.id) ? undefined : tt.id,
          name: tt.name,
          price: tt.price,
          description: tt.description,
          totalAvailable: tt.totalAvailable,
        }))

        const request: UpdateEventRequest = {
          id: eventData.id,
          name: eventData.name,
          start:
            eventDateEnabled && eventData.startDate
              ? combineDateTime(eventData.startDate, eventData.startTime ?? "00:00")
              : undefined,
          end:
            eventDateEnabled && eventData.endDate
              ? combineDateTime(eventData.endDate, eventData.endTime ?? "00:00")
              : undefined,
          venue: eventData.venueDetails,
          salesStart:
            eventSalesDateEnabled && eventData.salesStartDate
              ? combineDateTime(eventData.salesStartDate, eventData.salesStartTime ?? "00:00")
              : undefined,
          salesEnd:
            eventSalesDateEnabled && eventData.salesEndDate
              ? combineDateTime(eventData.salesEndDate, eventData.salesEndTime ?? "00:00")
              : undefined,
          status: eventData.status,
          ticketTypes,
        }

        console.log("Update payload:", serializeEventRequest(request))
        await updateEvent(user.access_token, eventData.id, request)
        toast.success("Event updated successfully")
      } else {
        const ticketTypes: CreateTicketTypeRequest[] = eventData.ticketTypes.map((tt) => ({
          name: tt.name,
          price: tt.price,
          description: tt.description,
          totalAvailable: tt.totalAvailable,
        }))

        const request: CreateEventRequest = {
          name: eventData.name,
          start:
            eventDateEnabled && eventData.startDate
              ? combineDateTime(eventData.startDate, eventData.startTime ?? "00:00")
              : undefined,
          end:
            eventDateEnabled && eventData.endDate
              ? combineDateTime(eventData.endDate, eventData.endTime ?? "00:00")
              : undefined,
          venue: eventData.venueDetails,
          salesStart:
            eventSalesDateEnabled && eventData.salesStartDate
              ? combineDateTime(eventData.salesStartDate, eventData.salesStartTime ?? "00:00")
              : undefined,
          salesEnd:
            eventSalesDateEnabled && eventData.salesEndDate
              ? combineDateTime(eventData.salesEndDate, eventData.salesEndTime ?? "00:00")
              : undefined,
          status: eventData.status,
          ticketTypes,
        }

        await createEvent(user.access_token, request)
        toast.success("Event created successfully")
      }
      navigate("/dashboard/events")
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message)
      } else {
        setError("An unknown error occurred")
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleSaveTicketType = () => {
    if (!currentTicketType) return

    const newTicketTypes = [...eventData.ticketTypes]
    if (currentTicketType.id) {
      const index = newTicketTypes.findIndex((t) => t.id === currentTicketType.id)
      if (index !== -1) {
        newTicketTypes[index] = currentTicketType
      }
    } else {
      newTicketTypes.push({ ...currentTicketType, id: generateTempId() })
    }

    updateField("ticketTypes", newTicketTypes)
    setDialogOpen(false)
    setCurrentTicketType(undefined)
  }

  return (
    <PageContainer>
      <Navbar />

      <div className="container mx-auto px-4 lg:px-8 pt-24 pb-12">
        {/* Header */}
        <div className="mb-8">
          <Link
            to="/dashboard/events"
            className="inline-flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors mb-4"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to events
          </Link>
          <h1 className="text-3xl font-bold text-foreground">{isEditMode ? "Edit Event" : "Create New Event"}</h1>
          {isEditMode && eventData.updatedAt && (
            <p className="text-sm text-muted-foreground mt-2">
              Last updated: {format(new Date(eventData.updatedAt), "PPP")}
            </p>
          )}
        </div>

        {error && (
          <Alert variant="destructive" className="mb-6 glass border-destructive/50">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>Error</AlertTitle>
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <form onSubmit={handleFormSubmit}>
          <div className="grid lg:grid-cols-3 gap-8">
            {/* Main Form */}
            <div className="lg:col-span-2 space-y-6">
              {/* Basic Info */}
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                className="glass rounded-2xl p-6"
              >
                <h2 className="text-lg font-semibold text-foreground mb-4">Basic Information</h2>
                <div className="space-y-4">
                  <div className="space-y-2">
                    <Label className="text-foreground">Event Name</Label>
                    <Input
                      value={eventData.name}
                      onChange={(e) => updateField("name", e.target.value)}
                      placeholder="Enter event name"
                      className="bg-secondary border-border text-foreground"
                      required
                    />
                  </div>

                  <div className="space-y-2">
                    <Label className="text-foreground">Venue</Label>
                    <Textarea
                      value={eventData.venueDetails}
                      onChange={(e) => updateField("venueDetails", e.target.value)}
                      placeholder="Enter venue details"
                      className="bg-secondary border-border text-foreground min-h-[100px]"
                    />
                  </div>

                  <div className="space-y-2">
                    <Label className="text-foreground">Status</Label>
                    <Select
                      value={eventData.status}
                      onValueChange={(value) => updateField("status", value as EventStatusEnum)}
                    >
                      <SelectTrigger className="bg-secondary border-border text-foreground">
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent className="glass">
                        {Object.values(EventStatusEnum).map((status) => (
                          <SelectItem key={status} value={status}>
                            {status}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                </div>
              </motion.div>

              {/* Date & Time */}
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="glass rounded-2xl p-6"
              >
                <h2 className="text-lg font-semibold text-foreground mb-4">Date & Time</h2>
                <div className="space-y-6">
                  <DateTimeSelect
                    label="Event Start"
                    date={eventData.startDate}
                    setDate={(date) => updateField("startDate", date)}
                    time={eventData.startTime}
                    setTime={(time) => updateField("startTime", time)}
                    enabled={eventDateEnabled}
                    setEnabled={setEventDateEnabled}
                  />
                  <DateTimeSelect
                    label="Event End"
                    date={eventData.endDate}
                    setDate={(date) => updateField("endDate", date)}
                    time={eventData.endTime}
                    setTime={(time) => updateField("endTime", time)}
                    enabled={eventDateEnabled}
                    setEnabled={setEventDateEnabled}
                  />
                </div>
              </motion.div>

              {/* Sales Period */}
              <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 }}
                className="glass rounded-2xl p-6"
              >
                <h2 className="text-lg font-semibold text-foreground mb-4">Sales Period</h2>
                <div className="space-y-6">
                  <DateTimeSelect
                    label="Sales Start"
                    date={eventData.salesStartDate}
                    setDate={(date) => updateField("salesStartDate", date)}
                    time={eventData.salesStartTime}
                    setTime={(time) => updateField("salesStartTime", time)}
                    enabled={eventSalesDateEnabled}
                    setEnabled={setEventSalesDateEnabled}
                  />
                  <DateTimeSelect
                    label="Sales End"
                    date={eventData.salesEndDate}
                    setDate={(date) => updateField("salesEndDate", date)}
                    time={eventData.salesEndTime}
                    setTime={(time) => updateField("salesEndTime", time)}
                    enabled={eventSalesDateEnabled}
                    setEnabled={setEventSalesDateEnabled}
                  />
                </div>
              </motion.div>
            </div>

            {/* Sidebar */}
            <div className="space-y-6">
              {/* Ticket Types */}
              <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: 0.3 }}>
                <Card className="glass border-border/50">
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <CardTitle className="flex items-center gap-2 text-foreground">
                        <Ticket className="w-5 h-5 text-primary" />
                        Ticket Types
                      </CardTitle>
                      <Button
                        type="button"
                        size="sm"
                        variant="outline"
                        className="gap-1 glass border-border/50 bg-transparent"
                        onClick={() => {
                          setCurrentTicketType({
                            id: undefined,
                            name: "",
                            price: 0,
                            totalAvailable: undefined,
                            description: "",
                          })
                          setDialogOpen(true)
                        }}
                      >
                        <Plus className="w-4 h-4" />
                        Add
                      </Button>
                    </div>
                  </CardHeader>
                  <CardContent className="space-y-3">
                    {eventData.ticketTypes.length === 0 ? (
                      <p className="text-sm text-muted-foreground text-center py-4">No ticket types added yet</p>
                    ) : (
                      eventData.ticketTypes.map((ticket) => (
                        <div key={ticket.id} className="p-3 rounded-xl bg-secondary/50 border border-border/30">
                          <div className="flex items-start justify-between">
                            <div>
                              <div className="flex items-center gap-2">
                                <span className="font-medium text-foreground">{ticket.name}</span>
                                <Badge variant="outline" className="text-primary border-primary/30">
                                  ${ticket.price}
                                </Badge>
                              </div>
                              {ticket.totalAvailable && (
                                <p className="text-xs text-muted-foreground mt-1">{ticket.totalAvailable} available</p>
                              )}
                            </div>
                            <div className="flex gap-1">
                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                className="h-8 w-8 p-0"
                                onClick={() => {
                                  setCurrentTicketType(ticket)
                                  setDialogOpen(true)
                                }}
                              >
                                <Edit className="w-3 h-3" />
                              </Button>
                              <Button
                                type="button"
                                variant="ghost"
                                size="sm"
                                className="h-8 w-8 p-0 text-destructive hover:text-destructive"
                                onClick={() => {
                                  updateField(
                                    "ticketTypes",
                                    eventData.ticketTypes.filter((t) => t.id !== ticket.id),
                                  )
                                }}
                              >
                                <Trash2 className="w-3 h-3" />
                              </Button>
                            </div>
                          </div>
                        </div>
                      ))
                    )}
                  </CardContent>
                </Card>
              </motion.div>

              {/* Submit Button */}
              <Button type="submit" className="w-full gradient-primary text-white h-12" disabled={isSubmitting}>
                {isSubmitting ? (
                  <div className="flex items-center gap-2">
                    <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    Saving...
                  </div>
                ) : (
                  <>
                    <Save className="w-4 h-4 mr-2" />
                    {isEditMode ? "Update Event" : "Create Event"}
                  </>
                )}
              </Button>
            </div>
          </div>
        </form>
      </div>

      {/* Ticket Type Dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="glass">
          <DialogHeader>
            <DialogTitle className="text-foreground">
              {currentTicketType?.id ? "Edit Ticket Type" : "Add Ticket Type"}
            </DialogTitle>
            <DialogDescription>Configure the details for this ticket type.</DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label className="text-foreground">Name</Label>
              <Input
                value={currentTicketType?.name || ""}
                onChange={(e) => setCurrentTicketType((prev) => (prev ? { ...prev, name: e.target.value } : undefined))}
                placeholder="e.g., General Admission"
                className="bg-secondary border-border text-foreground"
              />
            </div>

            <div className="space-y-2">
              <Label className="text-foreground">Price ($)</Label>
              <Input
                type="number"
                min="0"
                step="0.01"
                value={currentTicketType?.price || 0}
                onChange={(e) =>
                  setCurrentTicketType((prev) =>
                    prev ? { ...prev, price: Number.parseFloat(e.target.value) || 0 } : undefined,
                  )
                }
                className="bg-secondary border-border text-foreground"
              />
            </div>

            <div className="space-y-2">
              <Label className="text-foreground">Total Available (optional)</Label>
              <Input
                type="number"
                min="0"
                value={currentTicketType?.totalAvailable || ""}
                onChange={(e) =>
                  setCurrentTicketType((prev) =>
                    prev
                      ? {
                          ...prev,
                          totalAvailable: e.target.value ? Number.parseInt(e.target.value) : undefined,
                        }
                      : undefined,
                  )
                }
                placeholder="Leave empty for unlimited"
                className="bg-secondary border-border text-foreground"
              />
            </div>

            <div className="space-y-2">
              <Label className="text-foreground">Description</Label>
              <Textarea
                value={currentTicketType?.description || ""}
                onChange={(e) =>
                  setCurrentTicketType((prev) => (prev ? { ...prev, description: e.target.value } : undefined))
                }
                placeholder="Describe what's included"
                className="bg-secondary border-border text-foreground"
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => {
                setDialogOpen(false)
                setCurrentTicketType(undefined)
              }}
              className="glass border-border/50"
            >
              Cancel
            </Button>
            <Button onClick={handleSaveTicketType} className="gradient-primary text-white">
              Save
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </PageContainer>
  )
}

export default DashboardManageEventPage
