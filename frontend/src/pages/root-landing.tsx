"use client"

import type React from "react"
import { useAudience } from "@/hooks/use-audience"
import AudienceChooser from "@/components/audience-chooser"
import AttendeeLandingPage from "./attendee-landing-page"
import HypeLandingPage from "./hype-landing-page"

/**
 * The fork, and the only place the chooser can appear.
 *
 * Mounted at "/" alone. Every other route (a shared event link, /app,
 * /organizers, anything a crawler fetches) renders its content directly on
 * the stored or default audience, so the chooser never stands between an
 * inbound visitor and the thing they clicked.
 */
const RootLanding: React.FC = () => {
  const { audience, hasChosen } = useAudience()

  if (!hasChosen) return <AudienceChooser />

  return audience === "hype" ? <HypeLandingPage /> : <AttendeeLandingPage />
}

export default RootLanding
