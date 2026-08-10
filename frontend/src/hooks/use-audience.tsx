"use client"

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react"
import {
  AUDIENCE_VIBE,
  DEFAULT_AUDIENCE,
  applyAudience,
  readStoredAudience,
  storeAudience,
  type Audience,
} from "@/lib/audience"
import type { Vibe } from "@/lib/vibe"

interface AudienceContextValue {
  audience: Audience
  /** Token set the current audience paints with. */
  vibe: Vibe
  setAudience: (next: Audience) => void
  /** False only before the visitor has ever chosen. Drives the chooser. */
  hasChosen: boolean
  /** Accepts the current audience without changing it. Used by "skip". */
  acknowledge: () => void
}

const AudienceContext = createContext<AudienceContextValue | null>(null)

export const AudienceProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  /* Read synchronously during first render so the chooser is either present
     on the first paint or never. Doing this in an effect would flash the
     landing page and then cover it, which is the worst of both. */
  const [audience, setAudienceState] = useState<Audience>(
    () => readStoredAudience() ?? DEFAULT_AUDIENCE,
  )
  const [hasChosen, setHasChosen] = useState<boolean>(() => readStoredAudience() !== null)

  useEffect(() => {
    applyAudience(audience)
  }, [audience])

  const setAudience = useCallback((next: Audience) => {
    setAudienceState(next)
    setHasChosen(true)
    storeAudience(next)
  }, [])

  const acknowledge = useCallback(() => {
    setHasChosen(true)
    setAudienceState((current) => {
      storeAudience(current)
      return current
    })
  }, [])

  const value = useMemo(
    () => ({
      audience,
      vibe: AUDIENCE_VIBE[audience],
      setAudience,
      hasChosen,
      acknowledge,
    }),
    [audience, setAudience, hasChosen, acknowledge],
  )

  return <AudienceContext.Provider value={value}>{children}</AudienceContext.Provider>
}

export function useAudience(): AudienceContextValue {
  const ctx = useContext(AudienceContext)
  if (!ctx) throw new Error("useAudience must be used inside an AudienceProvider")
  return ctx
}
