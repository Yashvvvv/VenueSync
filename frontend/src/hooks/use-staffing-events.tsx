import { useCallback, useEffect, useState } from "react"
import { useAuth } from "react-oidc-context"
import { PublishedEventSummary } from "@/domain/domain"
import { listMyStaffingEvents } from "@/lib/api"

// One request per signed-in session, shared by the navbar and the scanner. Keyed by the access token, so signing in
// as someone else (or a refreshed token) fetches again.
let cache: { token: string; events: Promise<PublishedEventSummary[]> } | undefined

const load = (token: string, force = false) => {
  if (force || cache?.token !== token) {
    const events = listMyStaffingEvents(token)
    cache = { token, events }
    events.catch(() => {
      if (cache?.events === events) cache = undefined // a failure is not cached
    })
  }
  return cache!.events
}

/** Events the signed-in user can scan: organized plus staffed. Empty when signed out. */
export const useStaffingEvents = () => {
  const { user } = useAuth()
  const token = user?.access_token
  const [events, setEvents] = useState<PublishedEventSummary[]>([])
  const [isLoading, setIsLoading] = useState(Boolean(token))
  const [error, setError] = useState<string>()

  const run = useCallback(
    (force: boolean) => {
      if (!token) {
        setEvents([])
        setIsLoading(false)
        return
      }
      let active = true
      setIsLoading(true)
      load(token, force)
        .then((list) => active && (setEvents(list), setError(undefined)))
        .catch((err) => active && setError(err instanceof Error ? err.message : "Could not load your events"))
        .finally(() => active && setIsLoading(false))
      return () => {
        active = false
      }
    },
    [token],
  )

  useEffect(() => run(false), [run])

  return { events, isLoading, error, refresh: () => run(true) }
}
