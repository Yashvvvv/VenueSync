import type React from "react"

import { FormEvent, useEffect, useRef, useState } from "react"
import { Link, useSearchParams } from "react-router"
import { useAuth } from "react-oidc-context"
import { ScanLine } from "lucide-react"

import Navbar from "@/components/layout/navbar"
import PageContainer from "@/components/layout/page-container"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Alert, AlertDescription } from "@/components/ui/alert"
import { AcceptStaffInviteResponse } from "@/domain/domain"
import { acceptStaffInvite } from "@/lib/api"
import { useStaffingEvents } from "@/hooks/use-staffing-events"

/**
 * Where an organizer's invite link lands: /staff/join?code=K7Q2M-9XH4P. The code is redeemed as soon as the
 * signed-in user opens the link (they clicked it on purpose); without a code, or after a failure, they can type one.
 */
const StaffJoinPage: React.FC = () => {
  const { user } = useAuth()
  const [params] = useSearchParams()
  const linkCode = params.get("code") ?? ""

  const [code, setCode] = useState(linkCode)
  const [joined, setJoined] = useState<AcceptStaffInviteResponse>()
  const [error, setError] = useState<string>()
  const [isJoining, setIsJoining] = useState(false)
  const triedLinkCode = useRef(false)
  const { refresh: refreshStaffingEvents } = useStaffingEvents()

  const join = async (value: string) => {
    if (!user?.access_token || !value.trim() || isJoining) return
    setIsJoining(true)
    setError(undefined)
    try {
      setJoined(await acceptStaffInvite(user.access_token, value))
      refreshStaffingEvents() // the navbar's Scan link and the scanner's event list include this event now
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not use that code")
    } finally {
      setIsJoining(false)
    }
  }

  // Redeem the link's code once. The ref keeps StrictMode's double effect from sending it twice
  // (harmless anyway: redeeming your own code again succeeds).
  useEffect(() => {
    if (linkCode && user?.access_token && !triedLinkCode.current) {
      triedLinkCode.current = true
      join(linkCode)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [linkCode, user?.access_token])

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault()
    join(code)
  }

  return (
    <PageContainer>
      <Navbar />
      <div className="container mx-auto max-w-md px-4 pb-12 pt-24">
        <h1 className="mb-2 text-3xl font-bold text-foreground">Join a door team</h1>

        {joined ? (
          <div className="mt-6 rounded-md border border-border bg-card p-6">
            <p className="mb-1 text-sm text-muted-foreground">You can now scan tickets for</p>
            <p className="mb-6 text-xl font-semibold text-foreground">{joined.eventName}</p>
            <Link to={`/dashboard/validate-qr?event=${joined.eventId}`}>
              <Button className="w-full">
                <ScanLine className="mr-2 h-4 w-4" /> Open the scanner
              </Button>
            </Link>
            <p className="mt-4 text-xs text-muted-foreground">
              On your phone, use Scan tickets in the VenueSync app. This event is already in your list there.
            </p>
          </div>
        ) : (
          <>
            <p className="mb-6 text-muted-foreground">
              Enter the invite code the organizer sent you. It looks like <span className="font-mono">K7Q2M-9XH4P</span>.
            </p>
            {error && (
              <Alert variant="destructive" className="mb-4">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}
            <form onSubmit={handleSubmit} className="space-y-4">
              <Input
                className="h-12 font-mono text-lg tracking-widest"
                placeholder="XXXXX-XXXXX"
                value={code}
                onChange={(e) => setCode(e.target.value)}
                autoCapitalize="characters"
                autoComplete="off"
                spellCheck={false}
                aria-label="Invite code"
              />
              <Button type="submit" className="h-12 w-full" disabled={!code.trim() || isJoining}>
                {isJoining ? "Joining…" : "Join"}
              </Button>
            </form>
          </>
        )}
      </div>
    </PageContainer>
  )
}

export default StaffJoinPage
