"use client"

import type React from "react"
import { Link, useLocation } from "react-router"
import { House, MagnifyingGlass, SquaresFour, Ticket } from "@/components/icons"

interface HypeTabBarProps {
  /**
   * Supplied only by the feed, which owns an inline search sheet. Without
   * it the middle tab becomes a link to the full listing instead, so the
   * bar keeps three destinations everywhere and never shows a control that
   * does nothing on the current page.
   */
  onSearch?: () => void
  searchOpen?: boolean
}

/**
 * Bottom tab bar: the whole navigation model for the hype experience.
 *
 * Sits in the thumb zone because this build is for one-handed phone use.
 * Three destinations, not five: bottom bars degrade past that, and the
 * research on this cohort points at fewer, larger targets rather than a
 * denser menu.
 *
 * `env(safe-area-inset-bottom)` keeps the row clear of the iOS home
 * indicator, which otherwise sits on top of the middle tab.
 */
export const HypeTabBar: React.FC<HypeTabBarProps> = ({ onSearch, searchOpen = false }) => {
  const { pathname } = useLocation()

  const item =
    "focus-ring flex flex-1 flex-col items-center justify-center gap-1 py-2.5 text-[10px] font-semibold uppercase tracking-[0.1em] transition-colors"
  const on = "text-primary"
  const off = "text-muted-foreground"

  const feedActive = pathname === "/" && !searchOpen

  return (
    <nav
      aria-label="Primary"
      className="fixed inset-x-0 bottom-0 z-40 border-t-2 border-border bg-background pb-[env(safe-area-inset-bottom)]"
    >
      <div className="mx-auto flex max-w-md items-stretch">
        <Link
          to="/"
          className={`${item} ${feedActive ? on : off}`}
          aria-current={feedActive ? "page" : undefined}
        >
          <House weight={feedActive ? "fill" : "regular"} size={20} />
          Feed
        </Link>

        {onSearch ? (
          <button
            onClick={onSearch}
            aria-expanded={searchOpen}
            className={`${item} ${searchOpen ? on : off}`}
          >
            <MagnifyingGlass weight={searchOpen ? "bold" : "regular"} size={20} />
            Search
          </button>
        ) : (
          <Link
            to="/events"
            className={`${item} ${pathname === "/events" ? on : off}`}
            aria-current={pathname === "/events" ? "page" : undefined}
          >
            <SquaresFour weight={pathname === "/events" ? "fill" : "regular"} size={20} />
            Browse
          </Link>
        )}

        <Link
          to="/dashboard/tickets"
          className={`${item} ${pathname.startsWith("/dashboard/tickets") ? on : off}`}
          aria-current={pathname.startsWith("/dashboard/tickets") ? "page" : undefined}
        >
          <Ticket weight={pathname.startsWith("/dashboard/tickets") ? "fill" : "regular"} size={20} />
          Tickets
        </Link>
      </div>
    </nav>
  )
}

export default HypeTabBar
