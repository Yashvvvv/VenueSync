"use client"

import type React from "react"

import { useAuth } from "react-oidc-context"
import { Avatar, AvatarFallback } from "../ui/avatar"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu"
import { useRoles } from "@/hooks/use-roles"
import { Link, useLocation } from "react-router"
import { Button } from "../ui/button"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "../ui/alert-dialog"
import { useEffect, useState } from "react"
import { AnimatePresence, motion, useMotionValueEvent, useScroll } from "framer-motion"
import toast from "react-hot-toast"
import { API_BASE } from "@/lib/api"
import {
  VenueSyncMark,
  House,
  CalendarDots,
  Ticket,
  QrCode,
  SquaresFour,
  SignOut,
  List,
  X,
  DeviceMobile,
} from "@/components/icons"
import { AudienceSwitch } from "../audience-switch"
import { useAudience } from "@/hooks/use-audience"

const Navbar: React.FC = () => {
  const { user, signoutRedirect, signinRedirect, isAuthenticated, signinSilent } = useAuth()
  const { isOrganizer, isAttendee, isStaff } = useRoles()
  const [isScrolled, setIsScrolled] = useState(false)
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)
  const [isUpgrading, setIsUpgrading] = useState(false)
  const location = useLocation()
  const { audience } = useAudience()

  /* In the hype experience the bottom tab bar is the navigation. The top
     bar collapses to brand plus account so the two do not compete. */
  const isHype = audience === "hype"

  /* Motion's scroll value is batched off the main render path. A raw
     `window.addEventListener("scroll")` re-rendered this tree on every
     scroll frame. */
  const { scrollY } = useScroll()
  useMotionValueEvent(scrollY, "change", (latest) => {
    setIsScrolled(latest > 16)
  })

  useEffect(() => {
    setIsMobileMenuOpen(false)
  }, [location.pathname])

  const handleUpgradeToOrganizer = async () => {
    if (isUpgrading) return
    try {
      setIsUpgrading(true)
      const token = user?.access_token
      if (!token) return
      const res = await fetch(`${API_BASE}/api/v1/users/me/roles/organizer`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.ok) {
        await signinSilent()
        toast.success("You can create events now.")
      } else {
        toast.error("Could not upgrade your account. Try again in a moment.")
      }
    } catch (e) {
      console.error(e)
      toast.error("Network problem while upgrading your account.")
    } finally {
      setIsUpgrading(false)
    }
  }

  const navLinks = [
    { to: "/", label: "Discover", Icon: House, public: true },
    ...(isOrganizer
      ? [{ to: "/dashboard/events", label: "My events", Icon: CalendarDots, public: false }]
      : []),
    ...(isAttendee
      ? [{ to: "/dashboard/tickets", label: "My tickets", Icon: Ticket, public: false }]
      : []),
    ...(isStaff
      ? [{ to: "/dashboard/validate-qr", label: "Validate", Icon: QrCode, public: false }]
      : []),
  ]

  const visibleLinks = navLinks.filter((l) => l.public || isAuthenticated)

  return (
    <>
      <header
        className={`fixed inset-x-0 top-0 z-50 transition-[background-color,border-color] duration-300 ${
          isScrolled ? "chrome-blur border-b border-border" : "border-b border-transparent"
        }`}
      >
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          {/* 64px. A nav bar is chrome; it does not get to eat the viewport. */}
          <div className="flex h-16 items-center justify-between gap-6">
            <Link to="/" className="focus-ring group flex shrink-0 items-center gap-2.5 rounded-sm">
              <VenueSyncMark size={30} />
              <span className="text-[15px] font-semibold tracking-tight text-foreground">
                VenueSync
              </span>
            </Link>

            {/* Desktop nav. Underline marks the active route instead of a
                filled chip, which reads as a button the link is not. */}
            <nav className={`items-center gap-7 ${isHype ? "hidden" : "hidden lg:flex"}`}>
              {visibleLinks.map(({ to, label }) => {
                const isActive = location.pathname === to
                return (
                  <Link
                    key={to}
                    to={to}
                    aria-current={isActive ? "page" : undefined}
                    className={`focus-ring relative rounded-sm py-1 text-sm transition-colors duration-150 ${
                      isActive
                        ? "font-medium text-foreground"
                        : "text-muted-foreground hover:text-foreground"
                    }`}
                  >
                    {label}
                    {isActive && (
                      <motion.span
                        layoutId="nav-active"
                        className="absolute -bottom-0.5 left-0 h-px w-full bg-primary"
                        transition={{ duration: 0.28, ease: [0.16, 1, 0.3, 1] }}
                      />
                    )}
                  </Link>
                )
              })}
            </nav>

            <div className="flex items-center gap-2">
              {isAuthenticated ? (
                <>
                  {isAttendee && !isOrganizer && (
                    <AlertDialog>
                      <AlertDialogTrigger asChild>
                        <Button
                          variant="ghost"
                          size="sm"
                          className="hidden lg:inline-flex"
                          disabled={isUpgrading}
                        >
                          {isUpgrading ? "Upgrading" : "Host an event"}
                        </Button>
                      </AlertDialogTrigger>
                      <AlertDialogContent>
                        <AlertDialogHeader>
                          <AlertDialogTitle>Start hosting events</AlertDialogTitle>
                          <AlertDialogDescription>
                            Organizer access is free and it does not change anything about the
                            tickets you already hold. You get event creation, ticket tiers and
                            a sales dashboard.
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel>Cancel</AlertDialogCancel>
                          <AlertDialogAction onClick={handleUpgradeToOrganizer} disabled={isUpgrading}>
                            Enable organizer access
                          </AlertDialogAction>
                        </AlertDialogFooter>
                      </AlertDialogContent>
                    </AlertDialog>
                  )}

                  <Link to="/dashboard" className={isHype ? "hidden" : "hidden lg:block"}>
                    <Button variant="ghost" size="sm" className="gap-2">
                      <SquaresFour weight="fill" size={15} />
                      Dashboard
                    </Button>
                  </Link>

                  <DropdownMenu>
                    <DropdownMenuTrigger className="focus-ring ml-1 rounded-md">
                      <Avatar className="h-8 w-8 rounded-md border border-border transition-colors hover:border-primary">
                        <AvatarFallback className="rounded-md bg-secondary font-mono text-[11px] font-semibold tracking-wider text-foreground">
                          {user?.profile?.preferred_username?.slice(0, 2).toUpperCase()}
                        </AvatarFallback>
                      </Avatar>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent className="glass-strong w-60 p-1.5" align="end" sideOffset={10}>
                      <DropdownMenuLabel className="p-2.5 font-normal">
                        <p className="truncate text-sm font-medium text-foreground">
                          {user?.profile?.preferred_username}
                        </p>
                        <p className="mt-0.5 truncate font-mono text-[11px] text-muted-foreground">
                          {user?.profile?.email}
                        </p>
                      </DropdownMenuLabel>
                      <DropdownMenuSeparator />
                      <DropdownMenuItem
                        className="cursor-pointer gap-2.5 rounded-sm p-2.5 text-destructive focus:bg-destructive/10 focus:text-destructive"
                        onClick={() =>
                          signoutRedirect({ post_logout_redirect_uri: window.location.origin })
                        }
                      >
                        <SignOut weight="bold" size={15} />
                        Sign out
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </>
              ) : (
                <div className="flex items-center gap-1">
                  {/* Only one button box. The secondary action is a text
                      link so the eye is never asked to rank two fills. */}
                  <button
                    onClick={() => signinRedirect()}
                    className="focus-ring hidden rounded-sm px-3 py-2 text-sm text-muted-foreground transition-colors hover:text-foreground sm:block"
                  >
                    Log in
                  </button>
                  <Button onClick={() => signinRedirect({ prompt: "create" })} size="sm" className="px-4">
                    Sign up
                  </Button>
                </div>
              )}

              {/* The tab bar already covers navigation in hype, so the
                  hamburger would open a menu duplicating it. */}
              {!isHype && (
                <button
                  className="focus-ring -mr-1 rounded-sm p-2 text-foreground transition-colors hover:bg-secondary lg:hidden"
                  onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                  aria-expanded={isMobileMenuOpen}
                  aria-label={isMobileMenuOpen ? "Close menu" : "Open menu"}
                >
                  {isMobileMenuOpen ? (
                    <X weight="bold" size={19} />
                  ) : (
                    <List weight="bold" size={19} />
                  )}
                </button>
              )}
            </div>
          </div>
        </div>

        <AnimatePresence>
          {isMobileMenuOpen && !isHype && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              transition={{ duration: 0.26, ease: [0.32, 0.72, 0, 1] }}
              className="overflow-hidden border-b border-border bg-background lg:hidden"
            >
              <nav className="mx-auto max-w-[1400px] px-5 py-3">
                {visibleLinks.map(({ to, label, Icon }) => {
                  const isActive = location.pathname === to
                  return (
                    <Link
                      key={to}
                      to={to}
                      onClick={() => setIsMobileMenuOpen(false)}
                      className={`flex items-center gap-3 border-b border-border/60 py-3.5 text-[0.9375rem] transition-colors last:border-b-0 ${
                        isActive ? "text-primary" : "text-muted-foreground"
                      }`}
                    >
                      <Icon weight={isActive ? "fill" : "regular"} size={17} />
                      {label}
                    </Link>
                  )
                })}
                {isAuthenticated && (
                  <Link
                    to="/dashboard"
                    onClick={() => setIsMobileMenuOpen(false)}
                    className="flex items-center gap-3 border-t border-border py-3.5 text-[0.9375rem] text-muted-foreground"
                  >
                    <SquaresFour weight="regular" size={17} />
                    Dashboard
                  </Link>
                )}

                <Link
                  to="/app"
                  onClick={() => setIsMobileMenuOpen(false)}
                  className="flex items-center gap-3 border-t border-border py-3.5 text-[0.9375rem] text-muted-foreground"
                >
                  <DeviceMobile weight="regular" size={17} />
                  Get the app
                </Link>

                {/* The footer copy of this control is a long scroll away on a
                    phone, so the choice is reachable from the menu too. */}
                <div className="flex items-center justify-between border-t border-border pt-4">
                  <span className="text-sm text-muted-foreground">Experience</span>
                  <AudienceSwitch />
                </div>
              </nav>
            </motion.div>
          )}
        </AnimatePresence>
      </header>

      {/* Spacer matching the fixed header */}
      <div className="h-16" />
    </>
  )
}

export default Navbar
