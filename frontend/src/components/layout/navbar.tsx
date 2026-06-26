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
import { useState, useEffect } from "react"
import { motion, AnimatePresence } from "framer-motion"
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
  Lightning,
} from "@/components/icons"

const Navbar: React.FC = () => {
  const { user, signoutRedirect, signinRedirect, isAuthenticated, signinSilent } = useAuth()
  const { isOrganizer, isAttendee, isStaff } = useRoles()
  const [isScrolled, setIsScrolled] = useState(false)
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)
  const [isUpgrading, setIsUpgrading] = useState(false)
  const location = useLocation()

  useEffect(() => {
    const handleScroll = () => setIsScrolled(window.scrollY > 20)
    window.addEventListener("scroll", handleScroll)
    return () => window.removeEventListener("scroll", handleScroll)
  }, [])

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
        await user?.profile
        await signinSilent()
      } else {
        alert("Failed to upgrade account. Please try again.")
      }
    } catch (e) {
      console.error(e)
      alert("Network error while trying to upgrade account.")
    } finally {
      setIsUpgrading(false)
    }
  }

  const navLinks = [
    { to: "/", label: "Discover", Icon: House, public: true },
    ...(isOrganizer ? [{ to: "/dashboard/events", label: "My Events", Icon: CalendarDots, public: false }] : []),
    ...(isAttendee  ? [{ to: "/dashboard/tickets", label: "My Tickets", Icon: Ticket, public: false }] : []),
    ...(isStaff     ? [{ to: "/dashboard/validate-qr", label: "Validate", Icon: QrCode, public: false }] : []),
  ]

  return (
    <>
      <motion.nav
        initial={{ opacity: 0, transform: "translateY(-100%)" }}
        animate={{ opacity: 1, transform: "translateY(0%)" }}
        transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
        className={`fixed top-0 left-0 right-0 z-50 transition-[background,border-color,box-shadow] duration-300 ${
          isScrolled
            ? "bg-background/80 backdrop-blur-[24px] border-b border-border/30 shadow-[0_1px_0_0_oklch(0.19_0.012_265/0.6)]"
            : "bg-transparent"
        }`}
      >
        <div className="container mx-auto px-4 lg:px-8">
          <div className="flex h-16 lg:h-18 items-center justify-between">

            {/* Logo */}
            <Link to="/" className="flex items-center gap-2.5 group">
              <div className="btn-press flex-shrink-0">
                <VenueSyncMark size={38} />
              </div>
              <span className="text-[17px] font-bold tracking-tight text-foreground group-hover:text-primary transition-colors duration-150">
                VenueSync
              </span>
            </Link>

            {/* Desktop nav */}
            <div className="hidden lg:flex items-center gap-1">
              {navLinks
                .filter((l) => l.public || isAuthenticated)
                .map(({ to, label, Icon }) => {
                  const isActive = location.pathname === to
                  return (
                    <Link key={to} to={to}>
                      <div
                        className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-colors duration-150 ${
                          isActive
                            ? "text-primary bg-primary/10"
                            : "text-muted-foreground hover:text-foreground hover:bg-secondary/50"
                        }`}
                      >
                        <Icon weight={isActive ? "fill" : "regular"} size={16} />
                        {label}
                      </div>
                    </Link>
                  )
                })}
            </div>

            {/* Right section */}
            <div className="flex items-center gap-3">
              {isAuthenticated ? (
                <>
                  {isAttendee && !isOrganizer && (
                    <AlertDialog>
                      <AlertDialogTrigger asChild>
                        <Button
                          variant="outline"
                          size="sm"
                          className="hidden lg:flex btn-press gap-2 border-primary/50 text-primary hover:bg-primary/10 mr-2"
                          disabled={isUpgrading}
                        >
                          <Lightning weight="fill" size={15} />
                          {isUpgrading ? "Upgrading..." : "Host an Event"}
                        </Button>
                      </AlertDialogTrigger>
                      <AlertDialogContent>
                        <AlertDialogHeader>
                          <AlertDialogTitle>Become an Organizer</AlertDialogTitle>
                          <AlertDialogDescription>
                            Ready to host your own events? Upgrading to an Organizer account is free and gives you full access to create events, manage tickets, and track sales on your dashboard.
                          </AlertDialogDescription>
                        </AlertDialogHeader>
                        <AlertDialogFooter>
                          <AlertDialogCancel>Cancel</AlertDialogCancel>
                          <AlertDialogAction onClick={handleUpgradeToOrganizer} disabled={isUpgrading}>
                            Yes, Upgrade My Account
                          </AlertDialogAction>
                        </AlertDialogFooter>
                      </AlertDialogContent>
                    </AlertDialog>
                  )}

                  <Link to="/dashboard" className="hidden lg:block">
                    <Button variant="ghost" size="sm" className="btn-press gap-2 hover:bg-secondary/50">
                      <SquaresFour weight="fill" size={16} />
                      Dashboard
                    </Button>
                  </Link>

                  <DropdownMenu>
                    <DropdownMenuTrigger className="focus-ring rounded-full">
                      <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
                        <Avatar className="h-9 w-9 border-2 border-primary/30 hover:border-primary/60 transition-colors shadow-lg shadow-primary/10">
                          <AvatarFallback className="bg-gradient-to-br from-primary/30 to-accent/30 text-foreground text-xs font-bold">
                            {user?.profile?.preferred_username?.slice(0, 2).toUpperCase()}
                          </AvatarFallback>
                        </Avatar>
                      </motion.div>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent className="w-60 glass-strong p-2" align="end" sideOffset={8}>
                      <DropdownMenuLabel className="font-normal p-3">
                        <p className="text-sm font-semibold text-foreground">{user?.profile?.preferred_username}</p>
                        <p className="text-xs text-muted-foreground mt-0.5">{user?.profile?.email}</p>
                      </DropdownMenuLabel>
                      <DropdownMenuSeparator className="bg-border/50" />
                      <DropdownMenuItem
                        className="cursor-pointer text-destructive focus:text-destructive focus:bg-destructive/10 rounded-lg p-3 mt-1 gap-3"
                        onClick={() => signoutRedirect({ post_logout_redirect_uri: window.location.origin })}
                      >
                        <SignOut weight="regular" size={16} />
                        Sign Out
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </>
              ) : (
                <div className="flex items-center gap-2">
                  <Button
                    variant="ghost"
                    onClick={() => signinRedirect()}
                    className="btn-press hover:bg-secondary/50 font-medium hidden sm:flex"
                  >
                    Log In
                  </Button>
                  <Button
                    onClick={() => signinRedirect({ prompt: "create" })}
                    className="btn-press gradient-primary text-white hover:opacity-90 transition-opacity duration-150 shadow-lg shadow-primary/25 px-6"
                  >
                    Sign Up
                  </Button>
                </div>
              )}

              {/* Mobile toggle */}
              <button
                className="btn-press lg:hidden p-2.5 rounded-xl hover:bg-secondary/50 transition-colors"
                onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                aria-label="Toggle menu"
              >
                <AnimatePresence mode="wait">
                  {isMobileMenuOpen ? (
                    <motion.div
                      key="close"
                      initial={{ rotate: -90, opacity: 0 }}
                      animate={{ rotate: 0, opacity: 1 }}
                      exit={{ rotate: 90, opacity: 0 }}
                      transition={{ duration: 0.18 }}
                    >
                      <X weight="bold" size={20} />
                    </motion.div>
                  ) : (
                    <motion.div
                      key="menu"
                      initial={{ rotate: 90, opacity: 0 }}
                      animate={{ rotate: 0, opacity: 1 }}
                      exit={{ rotate: -90, opacity: 0 }}
                      transition={{ duration: 0.18 }}
                    >
                      <List weight="bold" size={20} />
                    </motion.div>
                  )}
                </AnimatePresence>
              </button>
            </div>
          </div>
        </div>

        {/* Mobile menu */}
        <AnimatePresence>
          {isMobileMenuOpen && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              transition={{ duration: 0.28, ease: [0.32, 0.72, 0, 1] }}
              className="lg:hidden bg-background/95 backdrop-blur-[24px] border-b border-border/40 overflow-hidden"
            >
              <div className="container mx-auto px-4 py-4 space-y-1">
                {navLinks
                  .filter((l) => l.public || isAuthenticated)
                  .map(({ to, label, Icon }, index) => {
                    const isActive = location.pathname === to
                    return (
                      <motion.div
                        key={to}
                        initial={{ opacity: 0, transform: "translateX(-12px)" }}
                        animate={{ opacity: 1, transform: "translateX(0px)" }}
                        transition={{ delay: index * 0.045, duration: 0.25, ease: [0.22, 1, 0.36, 1] }}
                      >
                        <Link
                          to={to}
                          onClick={() => setIsMobileMenuOpen(false)}
                          className={`flex items-center gap-3 px-4 py-3.5 rounded-xl transition-colors ${
                            isActive
                              ? "text-primary bg-primary/10"
                              : "text-muted-foreground hover:text-foreground hover:bg-secondary/50"
                          }`}
                        >
                          <Icon weight={isActive ? "fill" : "regular"} size={18} />
                          <span className="font-medium">{label}</span>
                        </Link>
                      </motion.div>
                    )
                  })}
                {isAuthenticated && (
                  <motion.div
                    initial={{ opacity: 0, transform: "translateX(-12px)" }}
                    animate={{ opacity: 1, transform: "translateX(0px)" }}
                    transition={{ delay: navLinks.length * 0.045, duration: 0.25 }}
                  >
                    <Link
                      to="/dashboard"
                      onClick={() => setIsMobileMenuOpen(false)}
                      className="flex items-center gap-3 px-4 py-3.5 rounded-xl text-muted-foreground hover:text-foreground hover:bg-secondary/50 transition-colors"
                    >
                      <SquaresFour weight="regular" size={18} />
                      <span className="font-medium">Dashboard</span>
                    </Link>
                  </motion.div>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.nav>

      <div className="h-16 lg:h-18" />
    </>
  )
}

export default Navbar
