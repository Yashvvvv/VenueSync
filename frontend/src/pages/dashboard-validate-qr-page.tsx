"use client"

import type React from "react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { useEffect, useState } from "react"
import { Link, useSearchParams } from "react-router"
import { Scanner } from "@yudiel/react-qr-scanner"
import {
  TicketValidationMethod,
  TicketValidationResponse,
  TicketValidationStatus,
} from "@/domain/domain"
import { useStaffingEvents } from "@/hooks/use-staffing-events"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { AlertCircle, Check, X, QrCode, Keyboard, RotateCcw, ScanLine, RefreshCw } from "lucide-react"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { validateTicket } from "@/lib/api"
import { useAuth } from "react-oidc-context"
import Navbar from "@/components/layout/navbar"
import PageContainer from "@/components/layout/page-container"
import { motion, AnimatePresence } from "framer-motion"
import { PageLoader } from "@/components/common/loading-skeleton"
import toast from "react-hot-toast"

const DashboardValidateQrPage: React.FC = () => {
  const { isLoading, user } = useAuth()
  const [isManual, setIsManual] = useState(false)
  const [data, setData] = useState<string | undefined>()
  const [error, setError] = useState<string | undefined>()
  const [validationStatus, setValidationStatus] = useState<
    TicketValidationStatus | undefined
  >()
  const [result, setResult] = useState<TicketValidationResponse>()
  const { events, isLoading: isEventsLoading, error: eventsError } = useStaffingEvents()
  const [params] = useSearchParams()
  const [eventId, setEventId] = useState<string>()

  // Start on the event from the invite link (?event=), else the first one; never on an event they can't scan.
  useEffect(() => {
    if (eventId && events.some((e) => e.id === eventId)) return
    const fromLink = params.get("event")
    setEventId(events.find((e) => e.id === fromLink)?.id ?? events[0]?.id)
  }, [events, params, eventId])

  const handleReset = () => {
    setIsManual(false)
    setData(undefined)
    setError(undefined)
    setValidationStatus(undefined)
    setResult(undefined)
  }

  const handleError = (err: unknown) => {
    if (err instanceof Error) {
      setError(err.message)
    } else if (typeof err === "string") {
      setError(err)
    } else {
      setError("An unknown error occurred")
    }
  }

  const handleValidate = async (id: string, method: TicketValidationMethod) => {
    if (!user?.access_token || !eventId) {
      return
    }
    try {
      setError(undefined)
      const response = await validateTicket(user.access_token, {
        id: id.trim(),
        method,
        eventId,
      })
      setValidationStatus(response.status)
      setResult(response)
      
      if (response.status === TicketValidationStatus.VALID) {
        toast.success("Ticket validated successfully!")
      } else if (response.status === TicketValidationStatus.INVALID) {
        toast.error("Invalid ticket!")
      } else if (response.status === TicketValidationStatus.EXPIRED) {
        toast.error("Ticket has expired!")
      } else if (response.status === TicketValidationStatus.ALREADY_USED) {
        toast.error("Ticket has already been used!")
      } else if (response.status === TicketValidationStatus.WRONG_EVENT) {
        toast.error(response.eventName ? `This ticket is for ${response.eventName}` : "Ticket is for another event")
      }
    } catch (err) {
      handleError(err)
    }
  }

  if (isLoading || !user?.access_token || isEventsLoading) {
    return <PageLoader />
  }

  if (events.length === 0) {
    return (
      <PageContainer>
        <Navbar />
        <div className="container mx-auto max-w-md px-4 pb-12 pt-24 text-center">
          <h1 className="mb-2 text-3xl font-bold text-foreground">Ticket Validation</h1>
          <p className="mb-6 text-muted-foreground">
            {eventsError ?? "You're not on the door team for any event yet. Ask the organizer for an invite code."}
          </p>
          <Link to="/staff/join">
            <Button>Enter an invite code</Button>
          </Link>
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
          className="text-center mb-8"
        >
          <div className="w-16 h-16 rounded-2xl gradient-primary flex items-center justify-center mx-auto mb-4">
            <ScanLine className="w-8 h-8 text-white" />
          </div>
          <h1 className="text-3xl font-bold text-foreground mb-2">Ticket Validation</h1>
          <p className="text-muted-foreground">Scan QR codes or enter ticket IDs manually</p>
        </motion.div>

        <div className="max-w-md mx-auto">
          {/* Which door: every scan is checked against this event */}
          <div className="mb-6">
            <label className="mb-2 block text-sm text-muted-foreground" htmlFor="scan-event">Scanning for</label>
            <Select value={eventId} onValueChange={(value) => { setEventId(value); handleReset() }}>
              <SelectTrigger id="scan-event" className="h-12 w-full">
                <SelectValue placeholder="Pick an event" />
              </SelectTrigger>
              <SelectContent>
                {events.map((event) => (
                  <SelectItem key={event.id} value={event.id}>{event.name}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          {/* Error Alert */}
          <AnimatePresence>
            {error && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -10 }}
              >
                <Alert variant="destructive" className="glass border-destructive/50 mb-6">
                  <AlertCircle className="h-4 w-4" />
                  <AlertTitle>Error</AlertTitle>
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Scanner Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="glass rounded-3xl p-6 mb-6"
          >
            {/* Scanner Viewport */}
            <div className="rounded-2xl overflow-hidden mb-6 relative bg-black/50 aspect-square">
              <Scanner
                key={`scanner-${data}-${validationStatus}`}
                onScan={(result) => {
                  if (result) {
                    const qrCodeId = result[0].rawValue
                    setData(qrCodeId)
                    handleValidate(qrCodeId, TicketValidationMethod.QR_SCAN)
                  }
                }}
                onError={handleError}
              />

              {/* Scan overlay */}
              {!validationStatus && (
                <div className="absolute inset-0 pointer-events-none flex items-center justify-center">
                  <div className="w-48 h-48 border-2 border-primary/50 rounded-2xl relative">
                    <div className="absolute top-0 left-0 w-6 h-6 border-t-2 border-l-2 border-primary rounded-tl-lg" />
                    <div className="absolute top-0 right-0 w-6 h-6 border-t-2 border-r-2 border-primary rounded-tr-lg" />
                    <div className="absolute bottom-0 left-0 w-6 h-6 border-b-2 border-l-2 border-primary rounded-bl-lg" />
                    <div className="absolute bottom-0 right-0 w-6 h-6 border-b-2 border-r-2 border-primary rounded-br-lg" />
                  </div>
                </div>
              )}

              {/* Validation Result Overlay */}
              <AnimatePresence>
                {validationStatus && (
                  <motion.div
                    initial={{ opacity: 0, scale: 0.5 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.5 }}
                    className="absolute inset-0 flex flex-col items-center justify-center bg-background/80 backdrop-blur-sm"
                  >
                    {validationStatus === TicketValidationStatus.VALID ? (
                      <motion.div
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        transition={{ type: "spring", stiffness: 200 }}
                        className="w-24 h-24 rounded-full bg-green-500/20 flex items-center justify-center glow-success"
                      >
                        <Check className="w-12 h-12 text-green-500" />
                      </motion.div>
                    ) : validationStatus === TicketValidationStatus.ALREADY_USED ? (
                      <motion.div
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        transition={{ type: "spring", stiffness: 200 }}
                        className="w-24 h-24 rounded-full bg-blue-500/20 flex items-center justify-center"
                      >
                        <RefreshCw className="w-12 h-12 text-blue-500" />
                      </motion.div>
                    ) : validationStatus === TicketValidationStatus.WRONG_EVENT ? (
                      <motion.div
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        transition={{ type: "spring", stiffness: 200 }}
                        className="w-24 h-24 rounded-full bg-yellow-500/20 flex items-center justify-center"
                      >
                        <AlertCircle className="w-12 h-12 text-yellow-500" />
                      </motion.div>
                    ) : validationStatus === TicketValidationStatus.EXPIRED ? (
                      <motion.div
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        transition={{ type: "spring", stiffness: 200 }}
                        className="w-24 h-24 rounded-full bg-yellow-500/20 flex items-center justify-center"
                      >
                        <AlertCircle className="w-12 h-12 text-yellow-500" />
                      </motion.div>
                    ) : (
                      <motion.div
                        initial={{ scale: 0 }}
                        animate={{ scale: 1 }}
                        transition={{ type: "spring", stiffness: 200 }}
                        className="w-24 h-24 rounded-full bg-destructive/20 flex items-center justify-center"
                      >
                        <X className="w-12 h-12 text-destructive" />
                      </motion.div>
                    )}
                    <motion.p
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: 0.2 }}
                      className="mt-4 text-lg font-medium text-foreground"
                    >
                      {validationStatus === TicketValidationStatus.VALID && "Ticket Valid!"}
                      {validationStatus === TicketValidationStatus.ALREADY_USED && "Already Used"}
                      {validationStatus === TicketValidationStatus.EXPIRED && "Ticket Expired"}
                      {validationStatus === TicketValidationStatus.INVALID && "Invalid Ticket"}
                      {validationStatus === TicketValidationStatus.WRONG_EVENT && "Wrong Event"}
                    </motion.p>
                    {validationStatus === TicketValidationStatus.VALID && result?.ticketTypeName && (
                      <p className="mt-1 font-mono text-sm text-muted-foreground">1 × {result.ticketTypeName}</p>
                    )}
                    {validationStatus === TicketValidationStatus.WRONG_EVENT && (
                      <p className="mt-1 text-sm text-muted-foreground">
                        {result?.eventName ? `This ticket is for ${result.eventName}` : "This ticket is for another event"}
                      </p>
                    )}
                  </motion.div>
                )}
              </AnimatePresence>
            </div>

            {/* Result Display */}
            <div className="glass rounded-xl p-4 mb-6 font-mono text-center min-h-[56px] flex items-center justify-center">
              {data ? (
                <span className="text-foreground break-all text-sm">{data}</span>
              ) : (
                <span className="text-muted-foreground">Waiting for scan...</span>
              )}
            </div>

            {/* Manual Entry */}
            <AnimatePresence mode="wait">
              {isManual ? (
                <motion.div
                  key="manual"
                  initial={{ opacity: 0, height: 0 }}
                  animate={{ opacity: 1, height: "auto" }}
                  exit={{ opacity: 0, height: 0 }}
                  className="space-y-4"
                >
                  <Input
                    className="w-full bg-secondary border-border text-foreground h-12 font-mono"
                    placeholder="Ticket code, e.g. F5A3-038B"
                    onChange={(e) => setData(e.target.value)}
                    value={data || ""}
                  />
                  <Button
                    className="w-full gradient-primary text-white h-14 text-lg font-semibold"
                    onClick={() =>
                      handleValidate(data || "", TicketValidationMethod.MANUAL)
                    }
                    disabled={!data}
                  >
                    <QrCode className="w-5 h-5 mr-2" />
                    Validate Ticket
                  </Button>
                </motion.div>
              ) : (
                <motion.div
                  key="buttons"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                >
                  <Button
                    variant="outline"
                    className="w-full glass border-border/50 h-14 text-lg bg-transparent"
                    onClick={() => setIsManual(true)}
                  >
                    <Keyboard className="w-5 h-5 mr-2" />
                    Manual Entry
                  </Button>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>

          {/* Reset Button */}
          <Button
            variant="outline"
            className="w-full glass border-border/50 h-12 bg-transparent"
            onClick={handleReset}
          >
            <RotateCcw className="w-4 h-4 mr-2" />
            Reset Scanner
          </Button>

          {/* Status Legend */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.3 }}
            className="mt-8 grid grid-cols-4 gap-3"
          >
            <div className="text-center">
              <div className="w-10 h-10 rounded-full bg-green-500/20 flex items-center justify-center mx-auto mb-2">
                <Check className="w-5 h-5 text-green-500" />
              </div>
              <p className="text-xs text-muted-foreground">Valid</p>
            </div>
            <div className="text-center">
              <div className="w-10 h-10 rounded-full bg-blue-500/20 flex items-center justify-center mx-auto mb-2">
                <RefreshCw className="w-5 h-5 text-blue-500" />
              </div>
              <p className="text-xs text-muted-foreground">Already Used</p>
            </div>
            <div className="text-center">
              <div className="w-10 h-10 rounded-full bg-yellow-500/20 flex items-center justify-center mx-auto mb-2">
                <AlertCircle className="w-5 h-5 text-yellow-500" />
              </div>
              <p className="text-xs text-muted-foreground">Expired</p>
            </div>
            <div className="text-center">
              <div className="w-10 h-10 rounded-full bg-destructive/20 flex items-center justify-center mx-auto mb-2">
                <X className="w-5 h-5 text-destructive" />
              </div>
              <p className="text-xs text-muted-foreground">Invalid</p>
            </div>
          </motion.div>
        </div>
      </div>
    </PageContainer>
  )
}

export default DashboardValidateQrPage
