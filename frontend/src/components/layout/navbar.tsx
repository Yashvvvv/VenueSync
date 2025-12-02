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
import { LogOut, Menu, X, Calendar, Ticket, QrCode, LayoutDashboard, Sparkles, Home } from "lucide-react"
import { useRoles } from "@/hooks/use-roles"
import { Link, useLocation } from "react-router"
import { Button } from "../ui/button"
import { useState, useEffect } from "react"
import { motion, AnimatePresence } from "framer-motion"

const Navbar: React.FC = () => {
  const { user, signoutRedirect, signinRedirect, isAuthenticated } = useAuth()
  const { isOrganizer, isAttendee, isStaff } = useRoles()
  const [isScrolled, setIsScrolled] = useState(false)
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false)
  const location = useLocation()

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 20)
    }
    window.addEventListener("scroll", handleScroll)
    return () => window.removeEventListener("scroll", handleScroll)
  }, [])

  useEffect(() => {
    setIsMobileMenuOpen(false)
  }, [location.pathname])

  const navLinks = [
    { to: "/", label: "Discover", icon: Home, public: true },
    ...(isOrganizer ? [{ to: "/dashboard/events", label: "My Events", icon: Calendar, public: false }] : []),
    ...(isAttendee ? [{ to: "/dashboard/tickets", label: "My Tickets", icon: Ticket, public: false }] : []),
    ...(isStaff ? [{ to: "/dashboard/validate-qr", label: "Validate", icon: QrCode, public: false }] : []),
  ]

  return (
    <>
      <motion.nav
        initial={{ y: -100 }}
        animate={{ y: 0 }}
        transition={{ type: "spring", stiffness: 100, damping: 20 }}
        className={`fixed top-0 left-0 right-0 z-50 transition-all duration-500 ${
          isScrolled
            ? "bg-background/70 backdrop-blur-heavy border-b border-border/40 shadow-lg shadow-background/20"
            : "bg-transparent"
        }`}
      >
        <div className="container mx-auto px-4 lg:px-8">
          <div className="flex h-16 lg:h-18 items-center justify-between">
            {/* Logo */}
            <Link to="/" className="flex items-center gap-3 group">
              <motion.div
                whileHover={{ scale: 1.05, rotate: 5 }}
                whileTap={{ scale: 0.95 }}
                className="w-10 h-10 rounded-xl gradient-primary flex items-center justify-center shadow-lg shadow-primary/25"
              >
                <Sparkles className="w-5 h-5 text-white" />
              </motion.div>
              <span className="text-xl font-bold text-foreground group-hover:text-gradient transition-all duration-300">
                EventHub
              </span>
            </Link>

            {/* Desktop Navigation */}
            <div className="hidden lg:flex items-center gap-1">
              {navLinks
                .filter((link) => link.public || isAuthenticated)
                .map((link) => {
                  const Icon = link.icon
                  const isActive = location.pathname === link.to
                  return (
                    <Link key={link.to} to={link.to}>
                      <motion.div
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                        className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-300 ${
                          isActive
                            ? "text-primary bg-primary/10 shadow-sm"
                            : "text-muted-foreground hover:text-foreground hover:bg-secondary/50"
                        }`}
                      >
                        <Icon className="w-4 h-4" />
                        {link.label}
                      </motion.div>
                    </Link>
                  )
                })}
            </div>

            {/* Right Section */}
            <div className="flex items-center gap-3">
              {isAuthenticated ? (
                <>
                  {/* Dashboard Link */}
                  <Link to="/dashboard" className="hidden lg:block">
                    <Button variant="ghost" size="sm" className="gap-2 hover:bg-secondary/50">
                      <LayoutDashboard className="w-4 h-4" />
                      Dashboard
                    </Button>
                  </Link>

                  {/* User Dropdown */}
                  <DropdownMenu>
                    <DropdownMenuTrigger className="focus-ring rounded-full">
                      <motion.div whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }}>
                        <Avatar className="h-10 w-10 border-2 border-primary/30 hover:border-primary/60 transition-colors shadow-lg shadow-primary/10">
                          <AvatarFallback className="bg-gradient-to-br from-primary/30 to-accent/30 text-foreground font-semibold">
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
                        className="cursor-pointer text-destructive focus:text-destructive focus:bg-destructive/10 rounded-lg p-3 mt-1"
                        onClick={() => signoutRedirect({ post_logout_redirect_uri: window.location.origin })}
                      >
                        <LogOut className="w-4 h-4 mr-3" />
                        Sign Out
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </>
              ) : (
                <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
                  <Button
                    onClick={() => signinRedirect()}
                    className="gradient-primary text-white hover:opacity-90 transition-all duration-300 shadow-lg shadow-primary/25 px-6"
                  >
                    Sign In
                  </Button>
                </motion.div>
              )}

              {/* Mobile Menu Toggle */}
              <motion.button
                whileTap={{ scale: 0.9 }}
                className="lg:hidden p-2.5 rounded-xl hover:bg-secondary/50 transition-colors"
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
                      transition={{ duration: 0.2 }}
                    >
                      <X className="w-5 h-5" />
                    </motion.div>
                  ) : (
                    <motion.div
                      key="menu"
                      initial={{ rotate: 90, opacity: 0 }}
                      animate={{ rotate: 0, opacity: 1 }}
                      exit={{ rotate: -90, opacity: 0 }}
                      transition={{ duration: 0.2 }}
                    >
                      <Menu className="w-5 h-5" />
                    </motion.div>
                  )}
                </AnimatePresence>
              </motion.button>
            </div>
          </div>
        </div>

        {/* Mobile Menu */}
        <AnimatePresence>
          {isMobileMenuOpen && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              transition={{ duration: 0.3, ease: "easeInOut" }}
              className="lg:hidden bg-background/95 backdrop-blur-heavy border-b border-border/40 overflow-hidden"
            >
              <div className="container mx-auto px-4 py-4 space-y-1">
                {navLinks
                  .filter((link) => link.public || isAuthenticated)
                  .map((link, index) => {
                    const Icon = link.icon
                    const isActive = location.pathname === link.to
                    return (
                      <motion.div
                        key={link.to}
                        initial={{ opacity: 0, x: -20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: index * 0.05 }}
                      >
                        <Link
                          to={link.to}
                          onClick={() => setIsMobileMenuOpen(false)}
                          className={`flex items-center gap-3 px-4 py-3.5 rounded-xl transition-all ${
                            isActive
                              ? "text-primary bg-primary/10"
                              : "text-muted-foreground hover:text-foreground hover:bg-secondary/50"
                          }`}
                        >
                          <Icon className="w-5 h-5" />
                          <span className="font-medium">{link.label}</span>
                        </Link>
                      </motion.div>
                    )
                  })}
                {isAuthenticated && (
                  <motion.div
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: navLinks.length * 0.05 }}
                  >
                    <Link
                      to="/dashboard"
                      onClick={() => setIsMobileMenuOpen(false)}
                      className="flex items-center gap-3 px-4 py-3.5 rounded-xl text-muted-foreground hover:text-foreground hover:bg-secondary/50 transition-all"
                    >
                      <LayoutDashboard className="w-5 h-5" />
                      <span className="font-medium">Dashboard</span>
                    </Link>
                  </motion.div>
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </motion.nav>

      {/* Spacer for fixed navbar */}
      <div className="h-16 lg:h-18" />
    </>
  )
}

export default Navbar
