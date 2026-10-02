import type React from "react"

import { useCallback, useEffect, useState } from "react"
import { Link, useParams } from "react-router"
import { useAuth } from "react-oidc-context"
import { format } from "date-fns"
import toast from "react-hot-toast"
import { ArrowLeft, Copy, UserMinus, UserPlus } from "lucide-react"

import Navbar from "@/components/layout/navbar"
import PageContainer from "@/components/layout/page-container"
import { PageLoader } from "@/components/common/loading-skeleton"
import { Button } from "@/components/ui/button"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { EventStaffMember, StaffInvite } from "@/domain/domain"
import { createStaffInvite, getEvent, listEventStaff, removeEventStaff } from "@/lib/api"
import { formatWallClockDate } from "@/lib/date-utils"

/** The link a staff member opens; the code alone works too (typed into the app). */
const inviteLink = (code: string) => `${globalThis.location.origin}/staff/join?code=${encodeURIComponent(code)}`

/**
 * An organizer builds the door team for one event. Each invite is a one-time code: the organizer hands it to one
 * person (WhatsApp, SMS, in person), that person redeems it and can then scan this event's tickets, and only these.
 */
const DashboardEventStaffPage: React.FC = () => {
  const { id } = useParams()
  const { user, isLoading: isAuthLoading } = useAuth()
  const token = user?.access_token

  const [eventName, setEventName] = useState<string>()
  const [staff, setStaff] = useState<EventStaffMember[]>()
  const [invite, setInvite] = useState<StaffInvite>()
  const [error, setError] = useState<string>()
  const [isInviting, setIsInviting] = useState(false)
  const [removingId, setRemovingId] = useState<string>()

  const load = useCallback(async () => {
    if (!token || !id) return
    try {
      setError(undefined)
      const [event, members] = await Promise.all([getEvent(token, id), listEventStaff(token, id)])
      setEventName(event.name)
      setStaff(members)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load the door staff")
    }
  }, [token, id])

  useEffect(() => {
    load()
  }, [load])

  const handleInvite = async () => {
    if (!token || !id || isInviting) return
    setIsInviting(true)
    try {
      setInvite(await createStaffInvite(token, id))
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Could not create an invite")
    } finally {
      setIsInviting(false)
    }
  }

  const handleCopy = async (text: string, what: string) => {
    try {
      await navigator.clipboard.writeText(text)
      toast.success(`${what} copied`)
    } catch {
      toast.error("Copy failed. Select the text and copy it yourself.")
    }
  }

  const handleRemove = async (member: EventStaffMember) => {
    if (!token || !id || removingId) return
    setRemovingId(member.userId)
    try {
      await removeEventStaff(token, id, member.userId)
      setStaff((current) => current?.filter((m) => m.userId !== member.userId))
      toast.success(`${member.name ?? "Staff member"} can no longer scan this event`)
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Could not remove them")
    } finally {
      setRemovingId(undefined)
    }
  }

  if (isAuthLoading || (!staff && !error)) {
    return <PageLoader />
  }

  return (
    <PageContainer>
      <Navbar />
      <div className="container mx-auto max-w-2xl px-4 pb-12 pt-24 lg:px-8">
        <Link to="/dashboard/events" className="mb-6 inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="h-4 w-4" /> Your events
        </Link>

        <h1 className="mb-1 text-3xl font-bold text-foreground">Door staff</h1>
        <p className="mb-8 text-muted-foreground">{eventName}</p>

        {error && (
          <Alert variant="destructive" className="mb-6">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        <section className="mb-10 rounded-md border border-border bg-card p-6">
          <h2 className="mb-2 text-lg font-semibold text-foreground">Invite someone to scan tickets</h2>
          <p className="mb-5 text-sm text-muted-foreground">
            Each code works once, for this event only, and expires after 7 days. Send it to one person. They open the link,
            or type the code in the VenueSync app under Scan tickets.
          </p>

          {invite ? (
            <div className="space-y-4">
              <div className="flex items-center justify-between gap-4 rounded-md border border-dashed border-border px-4 py-3">
                <span className="font-mono text-2xl tracking-widest text-foreground">{invite.code}</span>
                <Button variant="outline" size="sm" onClick={() => handleCopy(invite.code, "Code")}>
                  <Copy className="mr-2 h-4 w-4" /> Copy code
                </Button>
              </div>
              <div className="flex items-center justify-between gap-4">
                <span className="truncate font-mono text-xs text-muted-foreground">{inviteLink(invite.code)}</span>
                <Button variant="outline" size="sm" onClick={() => handleCopy(inviteLink(invite.code), "Link")}>
                  <Copy className="mr-2 h-4 w-4" /> Copy link
                </Button>
              </div>
              <p className="font-mono text-xs text-muted-foreground">
                Expires {formatWallClockDate(invite.expiresAt, format, "EEE d MMM, HH:mm")}
              </p>
              <Button variant="ghost" onClick={handleInvite} disabled={isInviting}>
                <UserPlus className="mr-2 h-4 w-4" /> Create another code
              </Button>
            </div>
          ) : (
            <Button onClick={handleInvite} disabled={isInviting}>
              <UserPlus className="mr-2 h-4 w-4" /> {isInviting ? "Creating…" : "Create invite code"}
            </Button>
          )}
        </section>

        <section>
          <h2 className="mb-4 text-lg font-semibold text-foreground">Current door staff</h2>
          {staff && staff.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              Nobody yet. You can always scan your own event yourself.
            </p>
          ) : (
            <ul className="divide-y divide-border rounded-md border border-border">
              {staff?.map((member) => (
                <li key={member.userId} className="flex items-center justify-between gap-4 px-4 py-3">
                  <div className="min-w-0">
                    <p className="truncate font-medium text-foreground">{member.name ?? "Unnamed"}</p>
                    <p className="truncate font-mono text-xs text-muted-foreground">{member.email}</p>
                  </div>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleRemove(member)}
                    disabled={removingId === member.userId}
                    aria-label={`Remove ${member.name ?? "staff member"}`}
                  >
                    <UserMinus className="mr-2 h-4 w-4" /> Remove
                  </Button>
                </li>
              ))}
            </ul>
          )}
        </section>
      </div>
    </PageContainer>
  )
}

export default DashboardEventStaffPage
