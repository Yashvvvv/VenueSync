"use client"

import { useEffect } from "react"
import { Outlet, useLocation } from "react-router"
import { useAudience } from "@/hooks/use-audience"
import HypeTabBar from "./hype-tabbar"

const RootLayout: React.FC = () => {
  const { pathname } = useLocation()
  const { audience } = useAudience()

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  /* The feed at "/" owns its own bar because it also owns the search
     sheet. Everywhere else the shell provides it, so the hype experience
     keeps one navigation model across the whole app rather than being a
     bottom bar on the homepage and a top bar everywhere else. */
  const showTabBar = audience === "hype" && pathname !== "/"

  return (
    <>
      <Outlet />
      {showTabBar && (
        <>
          {/* Reserves the bar's height so it never covers a footer link. */}
          <div aria-hidden className="h-[calc(64px+env(safe-area-inset-bottom))]" />
          <HypeTabBar />
        </>
      )}
    </>
  )
}

export default RootLayout
