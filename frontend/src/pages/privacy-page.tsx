"use client"

import type React from "react"
import { motion } from "framer-motion"
import { Shield } from "lucide-react"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"

const sections = [
  {
    title: "1. Information We Collect",
    content: `We collect information you provide directly to us, including:

• Account information (name, email, password)
• Profile information
• Event and ticket purchase details
• Payment information (processed securely by our payment providers)
• Communications with us

We also automatically collect certain information when you use our platform, including device information, log data, and usage patterns.`,
  },
  {
    title: "2. How We Use Your Information",
    content: `We use the information we collect to:

• Provide, maintain, and improve our services
• Process transactions and send related information
• Send you technical notices and support messages
• Respond to your comments and questions
• Communicate about events, promotions, and news
• Monitor and analyze trends and usage
• Detect and prevent fraudulent transactions`,
  },
  {
    title: "3. Information Sharing",
    content: `We may share your information in the following circumstances:

• With event organizers when you purchase tickets (name, email for attendee management)
• With service providers who assist in our operations
• In response to legal requests or to protect our rights
• In connection with a merger, sale, or acquisition
• With your consent or at your direction`,
  },
  {
    title: "4. Data Security",
    content: `We implement appropriate technical and organizational measures to protect your personal information. This includes:

• Encryption of data in transit and at rest
• Regular security assessments
• Access controls and authentication
• Secure payment processing through PCI-compliant providers

However, no method of transmission over the Internet is 100% secure, and we cannot guarantee absolute security.`,
  },
  {
    title: "5. Your Rights and Choices",
    content: `You have the right to:

• Access, update, or delete your personal information
• Opt out of marketing communications
• Request a copy of your data
• Object to certain processing of your information

You can exercise these rights through your account settings or by contacting us.`,
  },
  {
    title: "6. Cookies and Tracking",
    content: `We use cookies and similar technologies to:

• Keep you logged in
• Remember your preferences
• Understand how you use our platform
• Improve our services

You can control cookies through your browser settings, though this may affect platform functionality.`,
  },
  {
    title: "7. Data Retention",
    content: `We retain your information for as long as your account is active or as needed to provide services. We may retain certain information for legitimate business purposes or as required by law. Ticket and transaction records are retained for compliance and dispute resolution purposes.`,
  },
  {
    title: "8. Children's Privacy",
    content: `Our services are not directed to individuals under 16. We do not knowingly collect personal information from children. If we learn we have collected information from a child under 16, we will delete it promptly.`,
  },
  {
    title: "9. International Data Transfers",
    content: `Your information may be transferred to and processed in countries other than your own. We ensure appropriate safeguards are in place for such transfers in compliance with applicable data protection laws.`,
  },
  {
    title: "10. Changes to This Policy",
    content: `We may update this Privacy Policy from time to time. We will notify you of material changes by posting the new policy on this page and updating the "Last updated" date.`,
  },
  {
    title: "11. Contact Us",
    content: `If you have questions about this Privacy Policy or our data practices, please contact us at:

Email: privacy@eventhub.com
Or through our Contact page.`,
  },
]

const PrivacyPage: React.FC = () => {
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
              <Shield className="w-4 h-4 text-primary" />
              <span className="text-sm text-foreground">Privacy</span>
            </div>

            <h1 className="text-4xl md:text-5xl font-bold text-foreground mb-6">
              Privacy Policy
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
            <p className="text-muted-foreground leading-relaxed">
              At EventHub, we take your privacy seriously. This Privacy Policy explains how we collect, use, disclose, and safeguard your information when you use our event management platform.
            </p>

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

export default PrivacyPage
