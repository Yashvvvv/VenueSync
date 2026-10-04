"use client"

import type React from "react"
import { Link, useLocation } from "react-router"
import { CalendarDots, House, MagnifyingGlass, Ticket } from "@/components/icons"
import { useRoles } from "@/hooks/use-roles"

/**
 * Bottom tab bar: the whole navigation model for the hype experience.
 *
 * Feed, Explore (every event, searchable) and Tickets for everyone, and Events for organizers: their main
 * destination, too important to hide in the account menu. Four at most: bottom bars degrade past that, and this
 * cohort wants fewer, larger targets. The same tabs as the Android app.
 *
 * `env(safe-area-inset-bottom)` keeps the row clear of the iOS home
 * indicator, which otherwise sits on top of the middle tab.
 */
export const HypeTabBar: React.FC = () => {
  const { pathname } = useLocation()
  const { isOrganizer } = useRoles()

  const item =
    "focus-ring flex flex-1 flex-col items-center justify-center gap-1 py-2.5 text-[10px] font-semibold uppercase tracking-[0.1em] transition-colors"

  const tabs = [
    { to: "/", label: "Feed", Icon: House, active: pathname === "/" },
    { to: "/events", label: "Explore", Icon: MagnifyingGlass, active: pathname === "/events" },
    { to: "/dashboard/tickets", label: "Tickets", Icon: Ticket, active: pathname.startsWith("/dashboard/tickets") },
    ...(isOrganizer
      ? [{ to: "/dashboard/events", label: "Events", Icon: CalendarDots, active: pathname.startsWith("/dashboard/events") }]
      : []),
  ]

  return (
    <nav
      aria-label="Primary"
      className="fixed inset-x-0 bottom-0 z-40 border-t-2 border-border bg-background pb-[env(safe-area-inset-bottom)]"
    >
      <div className="mx-auto flex max-w-md items-stretch">
        {tabs.map(({ to, label, Icon, active }) => (
          <Link
            key={to}
            to={to}
            className={`${item} ${active ? "text-primary" : "text-muted-foreground"}`}
            aria-current={active ? "page" : undefined}
          >
            <Icon weight={active ? "fill" : "regular"} size={20} />
            {label}
          </Link>
        ))}
      </div>
    </nav>
  )
}

export default HypeTabBar
