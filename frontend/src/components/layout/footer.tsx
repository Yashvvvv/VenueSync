"use client"

import type React from "react"
import { Link } from "react-router"
import { Sparkles, Instagram, Twitter, Github, Linkedin, ArrowUpRight, Mail } from "lucide-react"
import { motion } from "framer-motion"
import toast from "react-hot-toast"

const Footer: React.FC = () => {
  const currentYear = new Date().getFullYear()

  const footerLinks = {
    discover: [
      { label: "Browse Events", href: "/" },
      { label: "For Attendees", href: "/" },
      { label: "For Organizers", href: "/organizers" },
    ],
    company: [
      { label: "About", href: "/about" },
      { label: "Contact", href: "/contact" },
    ],
    support: [
      { label: "Help Center", href: "/help" },
      { label: "Terms of Service", href: "/terms" },
      { label: "Privacy Policy", href: "/privacy" },
    ],
    organizers: [
      { label: "Create Events", href: "/organizers" },
      { label: "Dashboard", href: "/dashboard" },
    ],
  }

  const socialLinks = [
    { icon: Twitter, href: "https://twitter.com", label: "Twitter" },
    { icon: Instagram, href: "https://instagram.com", label: "Instagram" },
    { icon: Github, href: "https://github.com", label: "Github" },
    { icon: Linkedin, href: "https://linkedin.com", label: "LinkedIn" },
  ]

  const handleSubscribe = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const form = e.currentTarget
    const emailInput = form.querySelector('input[type="email"]') as HTMLInputElement
    if (emailInput?.value) {
      toast.success("Thanks for subscribing! You'll hear from us soon.")
      emailInput.value = ""
    }
  }

  return (
    <footer className="relative bg-card/30 border-t border-border/40">
      {/* Background gradient */}
      <div className="absolute inset-0 gradient-mesh opacity-20 pointer-events-none" />

      <div className="container mx-auto px-4 lg:px-8 py-16 relative">
        {/* Top Section */}
        <div className="grid grid-cols-2 md:grid-cols-6 gap-8 lg:gap-12 mb-16">
          {/* Brand Column */}
          <div className="col-span-2">
            <Link to="/" className="flex items-center gap-3 mb-5 group">
              <motion.div
                whileHover={{ scale: 1.05, rotate: 5 }}
                className="w-10 h-10 rounded-xl gradient-primary flex items-center justify-center shadow-lg shadow-primary/25"
              >
                <Sparkles className="w-5 h-5 text-white" />
              </motion.div>
              <span className="text-xl font-bold text-foreground group-hover:text-gradient transition-all">
                EventHub
              </span>
            </Link>
            <p className="text-sm text-muted-foreground mb-6 max-w-xs leading-relaxed">
              Discover and experience unforgettable events. Your gateway to concerts, conferences, and everything in
              between.
            </p>

            {/* Social Links */}
            <div className="flex gap-2">
              {socialLinks.map((social) => {
                const Icon = social.icon
                return (
                  <motion.a
                    key={social.label}
                    href={social.href}
                    whileHover={{ scale: 1.1, y: -2 }}
                    whileTap={{ scale: 0.95 }}
                    aria-label={social.label}
                    className="w-10 h-10 rounded-xl bg-secondary/50 flex items-center justify-center text-muted-foreground hover:text-primary hover:bg-primary/10 transition-all duration-300"
                  >
                    <Icon className="w-4 h-4" />
                  </motion.a>
                )
              })}
            </div>
          </div>

          {/* Link Columns */}
          {Object.entries(footerLinks).map(([title, links]) => (
            <div key={title}>
              <h4 className="text-sm font-semibold text-foreground mb-4 capitalize">{title}</h4>
              <ul className="space-y-3">
                {links.map((link) => (
                  <li key={link.label}>
                    <Link
                      to={link.href}
                      className="group text-sm text-muted-foreground hover:text-foreground transition-colors flex items-center gap-1"
                    >
                      {link.label}
                      <ArrowUpRight className="w-3 h-3 opacity-0 -translate-y-0.5 group-hover:opacity-100 group-hover:translate-y-0 transition-all" />
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        {/* Newsletter Section */}
        <form onSubmit={handleSubscribe} className="glass rounded-2xl p-6 md:p-8 mb-12">
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
            <div>
              <h3 className="text-lg font-semibold text-foreground mb-1">Stay in the loop</h3>
              <p className="text-sm text-muted-foreground">
                Get the latest events and updates delivered to your inbox.
              </p>
            </div>
            <div className="flex gap-2">
              <div className="flex items-center gap-3 glass-strong rounded-xl px-4 py-2 flex-1 md:w-64">
                <Mail className="w-4 h-4 text-muted-foreground flex-shrink-0" />
                <input
                  type="email"
                  placeholder="Enter your email"
                  required
                  className="bg-transparent border-0 outline-none text-sm text-foreground placeholder:text-muted-foreground flex-1 min-w-0"
                />
              </div>
              <motion.button
                type="submit"
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                className="px-5 py-2 rounded-xl gradient-primary text-white text-sm font-medium shadow-lg shadow-primary/25"
              >
                Subscribe
              </motion.button>
            </div>
          </div>
        </form>

        {/* Bottom Section */}
        <div className="pt-8 border-t border-border/40 flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="text-sm text-muted-foreground">{currentYear} EventHub. All rights reserved.</p>
          <div className="flex gap-6">
            <Link to="/terms" className="text-sm text-muted-foreground hover:text-foreground transition-colors">
              Terms of Service
            </Link>
            <Link to="/privacy" className="text-sm text-muted-foreground hover:text-foreground transition-colors">
              Privacy Policy
            </Link>
          </div>
        </div>
      </div>
    </footer>
  )
}

export default Footer
