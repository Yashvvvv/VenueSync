"use client"

import type React from "react"

import { Button } from "@/components/ui/button"
import { useAuth } from "react-oidc-context"
import { useNavigate, Link } from "react-router"
import { useRoles } from "@/hooks/use-roles"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"
import { PageLoader } from "@/components/common/loading-skeleton"
import { motion } from "framer-motion"
import {
  Calendar,
  Ticket,
  QrCode,
  TrendingUp,
  Users,
  Shield,
  ArrowRight,
  Sparkles,
  CheckCircle,
  BarChart3,
} from "lucide-react"

const features = [
  {
    icon: Calendar,
    title: "Easy Event Creation",
    description: "Create and customize events in minutes with our intuitive event builder.",
  },
  {
    icon: Ticket,
    title: "Flexible Ticketing",
    description: "Multiple ticket types, early bird pricing, and promotional codes support.",
  },
  {
    icon: QrCode,
    title: "QR Code Validation",
    description: "Seamless check-in with instant QR code scanning and validation.",
  },
  {
    icon: BarChart3,
    title: "Real-time Analytics",
    description: "Track sales, attendance, and revenue with comprehensive dashboards.",
  },
  {
    icon: Shield,
    title: "Secure Payments",
    description: "PCI-compliant payment processing with multiple payment methods.",
  },
  {
    icon: Users,
    title: "Attendee Management",
    description: "Manage attendees, send updates, and handle refunds effortlessly.",
  },
]

const stats = [
  { value: "10K+", label: "Events Created" },
  { value: "500K+", label: "Tickets Sold" },
  { value: "98%", label: "Satisfaction Rate" },
  { value: "$2M+", label: "Revenue Generated" },
]

const OrganizersLandingPage: React.FC = () => {
  const { isLoading, isAuthenticated, signinRedirect } = useAuth()
  const { isOrganizer, isAttendee, isStaff, isLoading: isRolesLoading } = useRoles()
  const navigate = useNavigate()

  if (isLoading || isRolesLoading) {
    return <PageLoader />
  }

  const handlePrimaryAction = () => {
    if (!isAuthenticated) {
      // Not logged in - redirect to sign in
      signinRedirect()
    } else if (isOrganizer) {
      // Is organizer - go to create event
      navigate("/dashboard/events/create")
    } else if (isStaff) {
      // Staff - go to validation
      navigate("/dashboard/validate-qr")
    } else if (isAttendee) {
      // Attendee - go to their tickets
      navigate("/dashboard/tickets")
    } else {
      navigate("/")
    }
  }

  // Determine primary button text and icon based on role
  const getPrimaryButtonText = () => {
    if (!isAuthenticated) return "Sign In to Get Started"
    if (isOrganizer) return "Create an Event"
    if (isStaff) return "Validate Tickets"
    if (isAttendee) return "View My Tickets"
    return "Get Started"
  }

  // Check if user is not an organizer (to show different messaging)
  const isNonOrganizer = isAuthenticated && !isOrganizer

  return (
    <PageContainer>
      <Navbar />

      {/* Hero Section */}
      <section className="relative pt-24 pb-16 overflow-hidden">
        <div className="absolute inset-0 gradient-mesh opacity-50" />
        <div className="absolute top-1/2 left-1/4 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-primary/20 rounded-full blur-3xl" />
        <div className="absolute top-1/3 right-1/4 w-[400px] h-[400px] bg-accent/15 rounded-full blur-3xl" />

        <div className="container mx-auto px-4 lg:px-8 relative">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            {/* Left Column */}
            <motion.div
              initial={{ opacity: 0, x: -30 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.6 }}
              className="space-y-8"
            >
              <div className="inline-flex items-center gap-2 glass px-4 py-2 rounded-full">
                <Sparkles className="w-4 h-4 text-primary" />
                <span className="text-sm text-foreground">For Event Organizers</span>
              </div>

              <h1 className="text-4xl md:text-5xl lg:text-6xl font-bold text-foreground leading-tight">
                Create, Manage & Sell{" "}
                <span className="gradient-text">Event Tickets</span> with Ease
              </h1>

              <p className="text-lg text-muted-foreground max-w-xl">
                A complete platform for event organizers to create stunning events, sell tickets
                seamlessly, and validate attendees with QR codes. Start selling in minutes.
              </p>

              <div className="flex flex-wrap gap-4">
                <motion.div whileHover={{ scale: 1.02 }} whileTap={{ scale: 0.98 }}>
                  <Button
                    size="lg"
                    className="gradient-primary text-white shadow-lg shadow-primary/25 px-8 gap-2"
                    onClick={handlePrimaryAction}
                  >
                    {getPrimaryButtonText()}
                    <ArrowRight className="w-4 h-4" />
                  </Button>
                </motion.div>
                <Link to="/">
                  <Button
                    size="lg"
                    variant="outline"
                    className="glass border-border/50 px-8 bg-transparent"
                  >
                    Browse Events
                  </Button>
                </Link>
              </div>

              {/* Info for non-organizers */}
              {isNonOrganizer && (
                <motion.div
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: 0.3 }}
                  className="glass rounded-xl p-4 border border-primary/20 max-w-md"
                >
                  <p className="text-sm text-muted-foreground">
                    <span className="text-primary font-medium">Want to create events?</span>{" "}
                    Contact us to upgrade your account to an Organizer role.
                  </p>
                </motion.div>
              )}

              {/* Quick Stats */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-4">
                {stats.map((stat, index) => (
                  <motion.div
                    key={stat.label}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.3 + index * 0.1 }}
                    className="text-center"
                  >
                    <p className="text-2xl font-bold text-foreground">{stat.value}</p>
                    <p className="text-xs text-muted-foreground">{stat.label}</p>
                  </motion.div>
                ))}
              </div>
            </motion.div>

            {/* Right Column - Hero Image */}
            <motion.div
              initial={{ opacity: 0, x: 30 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.6, delay: 0.2 }}
              className="relative"
            >
              <div className="relative rounded-3xl overflow-hidden glass border border-primary/20">
                <div className="absolute inset-0 gradient-primary opacity-10" />
                <img
                  src="/organizers-landing-hero.png"
                  alt="Event management dashboard"
                  className="w-full h-auto object-cover"
                  onError={(e) => {
                    e.currentTarget.src = "https://images.unsplash.com/photo-1540575467063-178a50e2df87?w=800&h=600&fit=crop"
                  }}
                />
              </div>
              {/* Floating badges */}
              <motion.div
                animate={{ y: [0, -10, 0] }}
                transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
                className="absolute -top-4 -right-4 glass rounded-xl p-3 shadow-lg"
              >
                <TrendingUp className="w-6 h-6 text-green-500" />
              </motion.div>
              <motion.div
                animate={{ y: [0, 10, 0] }}
                transition={{ duration: 3, repeat: Infinity, ease: "easeInOut", delay: 0.5 }}
                className="absolute -bottom-4 -left-4 glass rounded-xl p-3 shadow-lg"
              >
                <Ticket className="w-6 h-6 text-primary" />
              </motion.div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20">
        <div className="container mx-auto px-4 lg:px-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <h2 className="text-3xl md:text-4xl font-bold text-foreground mb-4">
              Everything You Need to Succeed
            </h2>
            <p className="text-muted-foreground max-w-2xl mx-auto">
              Powerful tools designed to help you create memorable events and maximize ticket sales.
            </p>
          </motion.div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
            {features.map((feature, index) => {
              const Icon = feature.icon
              return (
                <motion.div
                  key={feature.title}
                  initial={{ opacity: 0, y: 20 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: index * 0.1 }}
                  className="glass rounded-2xl p-6 hover:border-primary/30 transition-all card-hover"
                >
                  <div className="w-12 h-12 rounded-xl gradient-primary flex items-center justify-center mb-4">
                    <Icon className="w-6 h-6 text-white" />
                  </div>
                  <h3 className="text-lg font-semibold text-foreground mb-2">{feature.title}</h3>
                  <p className="text-muted-foreground text-sm">{feature.description}</p>
                </motion.div>
              )
            })}
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section className="py-20">
        <div className="container mx-auto px-4 lg:px-8">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-center mb-16"
          >
            <h2 className="text-3xl md:text-4xl font-bold text-foreground mb-4">
              Start Selling in 3 Easy Steps
            </h2>
          </motion.div>

          <div className="grid md:grid-cols-3 gap-8 max-w-4xl mx-auto">
            {[
              { step: "1", title: "Create Event", description: "Set up your event with details, dates, and venue." },
              { step: "2", title: "Add Tickets", description: "Create ticket types with pricing and availability." },
              { step: "3", title: "Start Selling", description: "Publish and start selling tickets instantly." },
            ].map((item, index) => (
              <motion.div
                key={item.step}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: index * 0.2 }}
                className="text-center"
              >
                <div className="w-16 h-16 rounded-2xl gradient-primary flex items-center justify-center mx-auto mb-4 text-2xl font-bold text-white">
                  {item.step}
                </div>
                <h3 className="text-lg font-semibold text-foreground mb-2">{item.title}</h3>
                <p className="text-muted-foreground text-sm">{item.description}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16">
        <div className="container mx-auto px-4 lg:px-8">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            whileInView={{ opacity: 1, scale: 1 }}
            viewport={{ once: true }}
            className="relative overflow-hidden rounded-3xl gradient-primary p-8 md:p-12 text-center"
          >
            <div className="absolute inset-0 bg-[url('/pattern.svg')] opacity-10" />
            <div className="relative">
              <CheckCircle className="w-12 h-12 text-white mx-auto mb-4" />
              <h2 className="text-3xl md:text-4xl font-bold text-white mb-4">
                Ready to Create Your First Event?
              </h2>
              <p className="text-white/80 max-w-xl mx-auto mb-8">
                Join thousands of organizers who trust EventHub to manage their events. 
                Start selling tickets in minutes.
              </p>
              <Button
                size="lg"
                className="bg-white text-primary hover:bg-white/90 font-semibold px-8"
                onClick={handlePrimaryAction}
              >
                {isOrganizer ? "Create Your Event" : "Get Started"}
                <ArrowRight className="w-4 h-4 ml-2" />
              </Button>
            </div>
          </motion.div>
        </div>
      </section>

      <Footer />
    </PageContainer>
  )
}

export default OrganizersLandingPage
