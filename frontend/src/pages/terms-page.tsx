"use client"

import type React from "react"
import { motion } from "framer-motion"
import { FileText } from "lucide-react"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"

const sections = [
  {
    title: "1. Acceptance of Terms",
    content: `By accessing and using EventHub, you accept and agree to be bound by the terms and provisions of this agreement. If you do not agree to abide by these terms, please do not use this service.`,
  },
  {
    title: "2. Description of Service",
    content: `EventHub provides a platform for event organizers to create, manage, and sell tickets for events, and for attendees to discover and purchase tickets. Our services include event listing, ticket sales, QR code validation, and attendee management.`,
  },
  {
    title: "3. User Accounts",
    content: `To access certain features of our platform, you must create an account. You are responsible for maintaining the confidentiality of your account credentials and for all activities that occur under your account. You agree to notify us immediately of any unauthorized use of your account.`,
  },
  {
    title: "4. User Roles and Responsibilities",
    content: `Our platform supports three user roles:
    
• Attendees: Can browse events, purchase tickets, and manage their ticket collection.
• Organizers: Can create and manage events, set up ticket types, and track sales.
• Staff: Can validate tickets using QR code scanning at event venues.

Each role comes with specific responsibilities and access levels within the platform.`,
  },
  {
    title: "5. Event Listings and Tickets",
    content: `Event organizers are solely responsible for the accuracy of event information, including dates, venues, and ticket details. EventHub does not guarantee the occurrence of any event or the accuracy of event listings. Ticket purchases are subject to availability and the organizer's terms.`,
  },
  {
    title: "6. Payments and Refunds",
    content: `All payments are processed securely through our platform. Refund policies are determined by individual event organizers. EventHub facilitates refund requests but the final decision rests with the event organizer. Service fees may be non-refundable.`,
  },
  {
    title: "7. Prohibited Conduct",
    content: `Users agree not to:
    
• Use the service for any unlawful purpose
• Create fraudulent events or listings
• Attempt to circumvent our security measures
• Resell tickets at inflated prices without authorization
• Harass other users or EventHub staff
• Upload malicious content or code`,
  },
  {
    title: "8. Intellectual Property",
    content: `All content on EventHub, including logos, designs, and software, is the property of EventHub or its licensors. Users retain ownership of content they create but grant EventHub a license to use such content for platform operations.`,
  },
  {
    title: "9. Limitation of Liability",
    content: `EventHub is provided "as is" without warranties of any kind. We are not liable for any indirect, incidental, or consequential damages arising from your use of the platform. Our liability is limited to the amount paid for our services.`,
  },
  {
    title: "10. Changes to Terms",
    content: `We reserve the right to modify these terms at any time. Users will be notified of significant changes. Continued use of the platform after changes constitutes acceptance of the new terms.`,
  },
  {
    title: "11. Contact Information",
    content: `For questions about these Terms of Service, please contact us through our Contact page or email support@eventhub.com.`,
  },
]

const TermsPage: React.FC = () => {
  return (
    <PageContainer>
      <Navbar />

      {/* Hero Section */}
      <section className="relative pt-24 pb-12 overflow-hidden">
        <div className="absolute inset-0 gradient-mesh opacity-30" />

        <div className="container mx-auto px-4 lg:px-8 relative">
          <motion.div
            initial={{ opacity: 0, y: 30 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="text-center max-w-3xl mx-auto"
          >
            <div className="inline-flex items-center gap-2 glass px-4 py-2 rounded-full mb-6">
              <FileText className="w-4 h-4 text-primary" />
              <span className="text-sm text-foreground">Legal</span>
            </div>

            <h1 className="text-4xl md:text-5xl font-bold text-foreground mb-6">
              Terms of Service
            </h1>

            <p className="text-muted-foreground">
              Last updated: December 2, 2025
            </p>
          </motion.div>
        </div>
      </section>

      {/* Content */}
      <section className="py-12">
        <div className="container mx-auto px-4 lg:px-8 max-w-4xl">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="glass rounded-2xl p-8 md:p-12 space-y-8"
          >
            {sections.map((section, index) => (
              <div key={index} className="space-y-4">
                <h2 className="text-xl font-semibold text-foreground">{section.title}</h2>
                <p className="text-muted-foreground whitespace-pre-line leading-relaxed">
                  {section.content}
                </p>
              </div>
            ))}
          </motion.div>
        </div>
      </section>

      <Footer />
    </PageContainer>
  )
}

export default TermsPage
