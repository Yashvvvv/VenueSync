"use client"

import type React from "react"
import { useId, useState } from "react"
import { Link } from "react-router"
import toast from "react-hot-toast"
import { Button } from "../ui/button"
import { AudienceSwitch } from "../audience-switch"
import {
  VenueSyncMark,
  XLogo,
  InstagramLogo,
  GithubLogo,
  LinkedinLogo,
} from "@/components/icons"

const footerLinks: { title: string; links: { label: string; href: string }[] }[] = [
  {
    title: "Attend",
    links: [
      { label: "Browse events", href: "/events" },
      { label: "My tickets", href: "/dashboard/tickets" },
      { label: "Get the app", href: "/app" },
      { label: "Help center", href: "/help" },
    ],
  },
  {
    title: "Organize",
    links: [
      { label: "Host an event", href: "/organizers" },
      { label: "Dashboard", href: "/dashboard" },
    ],
  },
  {
    title: "Company",
    links: [
      { label: "About", href: "/about" },
      { label: "Contact", href: "/contact" },
    ],
  },
  {
    title: "Legal",
    links: [
      { label: "Terms", href: "/terms" },
      { label: "Privacy", href: "/privacy" },
    ],
  },
]

const socials = [
  { Icon: XLogo, href: "https://x.com", label: "VenueSync on X" },
  { Icon: InstagramLogo, href: "https://instagram.com", label: "VenueSync on Instagram" },
  { Icon: GithubLogo, href: "https://github.com", label: "VenueSync on GitHub" },
  { Icon: LinkedinLogo, href: "https://linkedin.com", label: "VenueSync on LinkedIn" },
]

const Footer: React.FC = () => {
  const year = new Date().getFullYear()
  const emailId = useId()
  const [email, setEmail] = useState("")
  const [error, setError] = useState<string | null>(null)

  const handleSubscribe = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setError("Enter an email address we can actually reach.")
      return
    }
    setError(null)
    setEmail("")
    toast.success("You are on the list.")
  }

  return (
    <footer className="border-t border-border bg-background">
      <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
        {/* Sign-up sits above the tear line, the sitemap below it */}
        <div className="grid gap-10 py-14 lg:grid-cols-12 lg:gap-16">
          <div className="lg:col-span-5">
            <h2 className="text-2xl font-semibold tracking-tight text-foreground">
              Know before the tickets go
            </h2>
            <p className="mt-2.5 max-w-sm text-sm leading-relaxed text-muted-foreground">
              A short email when something worth going to opens up near you. Nothing else.
            </p>
          </div>

          <form onSubmit={handleSubscribe} className="lg:col-span-7 lg:pt-1.5" noValidate>
            {/* Label above the input, in markup and on screen. A placeholder
                disappears the moment someone starts typing. */}
            <label
              htmlFor={emailId}
              className="mb-2 block text-[0.6875rem] font-medium uppercase tracking-[0.12em] text-muted-foreground"
            >
              Email address
            </label>
            <div className="flex max-w-md gap-2">
              <input
                id={emailId}
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@example.com"
                aria-invalid={!!error}
                aria-describedby={error ? `${emailId}-error` : undefined}
                className={`h-10 min-w-0 flex-1 rounded-md border bg-card px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-primary ${
                  error ? "border-destructive" : "border-border"
                }`}
              />
              <Button type="submit" className="shrink-0 px-5">
                Subscribe
              </Button>
            </div>
            {error && (
              <p id={`${emailId}-error`} className="mt-2 text-xs text-destructive">
                {error}
              </p>
            )}
          </form>
        </div>

        <hr className="perf" />

        <div className="grid gap-10 py-14 sm:grid-cols-2 lg:grid-cols-12">
          <div className="lg:col-span-4">
            <Link to="/" className="focus-ring inline-flex items-center gap-2.5 rounded-sm">
              <VenueSyncMark size={30} />
              <span className="text-[15px] font-semibold tracking-tight text-foreground">
                VenueSync
              </span>
            </Link>
            <p className="mt-4 max-w-[26ch] text-sm leading-relaxed text-muted-foreground">
              Event ticketing without the friction, for the people running the show and the people
              turning up to it.
            </p>

            <div className="mt-6 flex gap-1">
              {socials.map(({ Icon, href, label }) => (
                <a
                  key={label}
                  href={href}
                  aria-label={label}
                  target="_blank"
                  rel="noreferrer noopener"
                  className="focus-ring rounded-md p-2 text-muted-foreground transition-colors hover:bg-secondary hover:text-foreground"
                >
                  <Icon weight="fill" size={17} />
                </a>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-2 gap-8 sm:grid-cols-4 lg:col-span-8">
            {footerLinks.map(({ title, links }) => (
              <div key={title}>
                <h3 className="text-[0.6875rem] font-medium uppercase tracking-[0.12em] text-muted-foreground">
                  {title}
                </h3>
                <ul className="mt-4 space-y-3">
                  {links.map((link) => (
                    <li key={link.label}>
                      <Link
                        to={link.href}
                        className="focus-ring rounded-sm text-sm text-foreground/75 transition-colors hover:text-primary"
                      >
                        {link.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-5 border-t border-border py-7 sm:flex-row sm:items-center sm:justify-between">
          <p className="font-mono text-xs text-muted-foreground">© {year} VenueSync</p>
          {/* Permanent way back to the other experience, so the first-run
              choice is never a one-way door. */}
          <AudienceSwitch />
        </div>
      </div>
    </footer>
  )
}

export default Footer
