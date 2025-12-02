"use client"

import type React from "react"
import { useState } from "react"
import { motion, AnimatePresence } from "framer-motion"
import {
  Sparkles,
  ChevronDown,
  Search,
  Ticket,
  Calendar,
  QrCode,
  CreditCard,
  Users,
  Settings,
} from "lucide-react"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"
import { Input } from "@/components/ui/input"

const categories = [
  {
    icon: Ticket,
    title: "Buying Tickets",
    questions: [
      {
        q: "How do I purchase a ticket?",
        a: "Browse events on our homepage, select the event you're interested in, choose your ticket type, and proceed to checkout. You'll need to be logged in to complete the purchase.",
      },
      {
        q: "Where can I find my tickets?",
        a: "After purchase, your tickets are available in your Dashboard under 'My Tickets'. You can view the QR code for each ticket there.",
      },
      {
        q: "Can I get a refund?",
        a: "Refund policies vary by event. Check the event details for specific refund information, or contact the event organizer directly.",
      },
    ],
  },
  {
    icon: Calendar,
    title: "Creating Events",
    questions: [
      {
        q: "How do I create an event?",
        a: "Go to the Organizers page and click 'Create Event'. Fill in your event details, set up ticket types, and publish when ready.",
      },
      {
        q: "Can I edit my event after publishing?",
        a: "Yes! You can edit your event anytime from your Dashboard. Go to 'My Events', find your event, and click 'Edit'.",
      },
      {
        q: "How do I add multiple ticket types?",
        a: "When creating or editing an event, use the 'Add Ticket Type' button to create different tiers (e.g., Early Bird, VIP, General Admission).",
      },
    ],
  },
  {
    icon: QrCode,
    title: "QR Code & Check-in",
    questions: [
      {
        q: "How does QR code validation work?",
        a: "Staff members can access the QR Scanner from the Dashboard. Simply scan attendee QR codes to validate tickets at the venue.",
      },
      {
        q: "What if an attendee's QR code doesn't scan?",
        a: "Ensure good lighting and a steady hand. If issues persist, you can manually search for the ticket using the attendee's email or ticket ID.",
      },
    ],
  },
  {
    icon: CreditCard,
    title: "Payments",
    questions: [
      {
        q: "What payment methods are accepted?",
        a: "We accept major credit cards, debit cards, and digital wallets. All payments are processed securely.",
      },
      {
        q: "When do organizers receive their payouts?",
        a: "Payouts are processed within 7 business days after the event concludes, minus any applicable fees.",
      },
    ],
  },
  {
    icon: Users,
    title: "Account & Roles",
    questions: [
      {
        q: "What are the different user roles?",
        a: "There are three roles: ATTENDEE (can browse and purchase tickets), ORGANIZER (can create and manage events), and STAFF (can validate QR codes at events).",
      },
      {
        q: "How do I become an organizer?",
        a: "Contact our support team or request an organizer role upgrade from your account settings.",
      },
    ],
  },
  {
    icon: Settings,
    title: "Technical Support",
    questions: [
      {
        q: "What browsers are supported?",
        a: "EventHub works best on the latest versions of Chrome, Firefox, Safari, and Edge.",
      },
      {
        q: "I'm having trouble logging in. What should I do?",
        a: "Try clearing your browser cache and cookies. If issues persist, use the 'Forgot Password' option or contact support.",
      },
    ],
  },
]

const HelpCenterPage: React.FC = () => {
  const [searchQuery, setSearchQuery] = useState("")
  const [expandedItems, setExpandedItems] = useState<Record<string, boolean>>({})

  const toggleItem = (key: string) => {
    setExpandedItems((prev) => ({ ...prev, [key]: !prev[key] }))
  }

  const filteredCategories = categories
    .map((category) => ({
      ...category,
      questions: category.questions.filter(
        (q) =>
          q.q.toLowerCase().includes(searchQuery.toLowerCase()) ||
          q.a.toLowerCase().includes(searchQuery.toLowerCase())
      ),
    }))
    .filter((category) => category.questions.length > 0)

  return (
    <PageContainer>
      <Navbar />

      {/* Hero Section */}
      <section className="relative pt-24 pb-16 overflow-hidden">
        <div className="absolute inset-0 gradient-mesh opacity-50" />

        <div className="container mx-auto px-4 lg:px-8 relative">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="text-center max-w-3xl mx-auto"
          >
            <div className="inline-flex items-center gap-2 glass px-4 py-2 rounded-full mb-6">
              <Sparkles className="w-4 h-4 text-primary" />
              <span className="text-sm text-foreground">Help Center</span>
            </div>

            <h1 className="text-4xl md:text-5xl font-bold text-foreground mb-6">
              How can we <span className="text-gradient">help you?</span>
            </h1>

            <p className="text-lg text-muted-foreground mb-8">
              Find answers to common questions about using EventHub.
            </p>

            {/* Search */}
            <div className="relative max-w-xl mx-auto">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
              <Input
                type="text"
                placeholder="Search for answers..."
                value={searchQuery}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => setSearchQuery(e.target.value)}
                className="pl-12 h-14 text-lg bg-secondary/50 border-border/50"
              />
            </div>
          </motion.div>
        </div>
      </section>

      {/* FAQ Categories */}
      <section className="py-16">
        <div className="container mx-auto px-4 lg:px-8 max-w-4xl">
          {(searchQuery ? filteredCategories : categories).map((category, categoryIndex) => {
            const Icon = category.icon
            return (
              <motion.div
                key={category.title}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ delay: categoryIndex * 0.1 }}
                className="mb-8"
              >
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-10 h-10 rounded-xl gradient-primary flex items-center justify-center">
                    <Icon className="w-5 h-5 text-white" />
                  </div>
                  <h2 className="text-xl font-semibold text-foreground">{category.title}</h2>
                </div>

                <div className="space-y-3">
                  {category.questions.map((item, itemIndex) => {
                    const key = `${categoryIndex}-${itemIndex}`
                    const isExpanded = expandedItems[key]

                    return (
                      <div key={key} className="glass rounded-xl overflow-hidden">
                        <button
                          onClick={() => toggleItem(key)}
                          className="w-full flex items-center justify-between p-4 text-left"
                        >
                          <span className="font-medium text-foreground pr-4">{item.q}</span>
                          <ChevronDown
                            className={`w-5 h-5 text-muted-foreground flex-shrink-0 transition-transform ${
                              isExpanded ? "rotate-180" : ""
                            }`}
                          />
                        </button>
                        <AnimatePresence>
                          {isExpanded && (
                            <motion.div
                              initial={{ height: 0, opacity: 0 }}
                              animate={{ height: "auto", opacity: 1 }}
                              exit={{ height: 0, opacity: 0 }}
                              transition={{ duration: 0.2 }}
                            >
                              <p className="px-4 pb-4 text-muted-foreground">{item.a}</p>
                            </motion.div>
                          )}
                        </AnimatePresence>
                      </div>
                    )
                  })}
                </div>
              </motion.div>
            )
          })}

          {searchQuery && filteredCategories.length === 0 && (
            <div className="text-center py-12">
              <p className="text-muted-foreground">No results found for "{searchQuery}"</p>
            </div>
          )}
        </div>
      </section>

      <Footer />
    </PageContainer>
  )
}

export default HelpCenterPage
