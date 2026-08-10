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
import { motion, useReducedMotion } from "framer-motion"
import {
  CalendarDots,
  Ticket,
  QrCode,
  ShieldCheck,
  ArrowRight,
  ChartBar,
  UsersThree,
  Storefront,
} from "@/components/icons"

/* Sentence case, concrete claims, no filler verbs. Each line says what the
   thing does rather than how transformative it is. */
const capabilities = [
  {
    Icon: CalendarDots,
    title: "Build the event once",
    description: "Dates, venue, capacity and sales window in a single form that saves as you go.",
  },
  {
    Icon: Ticket,
    title: "Price it how you want",
    description: "Several tiers, early release pricing and per tier caps, all on the same page.",
  },
  {
    Icon: QrCode,
    title: "Scan at the door",
    description: "Staff check people in from a phone. Validated and rejected show up instantly.",
  },
  {
    Icon: ChartBar,
    title: "Watch sales as they land",
    description: "Revenue and remaining stock update live, so you know when to release more.",
  },
  {
    Icon: ShieldCheck,
    title: "Payments stay off your books",
    description: "Card handling sits with the processor, which keeps PCI scope away from you.",
  },
  {
    Icon: UsersThree,
    title: "Handle people, not inboxes",
    description: "Attendee lists, refunds and updates all live inside the same dashboard.",
  },
]

const flow = [
  {
    title: "Create",
    description: "Add the event, set the venue and choose when tickets go on sale.",
  },
  {
    title: "Publish",
    description: "The page goes live on VenueSync and starts taking payments straight away.",
  },
  {
    title: "Scan",
    description: "Your staff validate codes at the gate and you watch attendance fill in.",
  },
]

const EASE = [0.16, 1, 0.3, 1] as const

const OrganizersLandingPage: React.FC = () => {
  const { isLoading, isAuthenticated, signinRedirect } = useAuth()
  const { isOrganizer, isAttendee, isStaff, isLoading: isRolesLoading } = useRoles()
  const navigate = useNavigate()
  const reduce = useReducedMotion()

  if (isLoading || isRolesLoading) {
    return <PageLoader />
  }

  const handlePrimaryAction = () => {
    if (!isAuthenticated) {
      signinRedirect()
    } else if (isOrganizer) {
      navigate("/dashboard/events/create")
    } else if (isStaff) {
      navigate("/dashboard/validate-qr")
    } else if (isAttendee) {
      navigate("/dashboard/tickets")
    } else {
      navigate("/")
    }
  }

  /* One label per intent, reused verbatim at the top and bottom of the page
     so the two calls to action never read as two different offers. */
  const primaryLabel = !isAuthenticated
    ? "Log in to start"
    : isOrganizer
      ? "Create an event"
      : isStaff
        ? "Validate tickets"
        : isAttendee
          ? "View my tickets"
          : "Get started"

  const isNonOrganizer = isAuthenticated && !isOrganizer && !isStaff

  return (
    <PageContainer>
      <Navbar />

      {/* ═══ 1. Hero. Split, real dashboard on the right. ═══ */}
      <section className="relative overflow-hidden pb-16 pt-14 lg:pb-24 lg:pt-24">
        <div className="pointer-events-none absolute inset-x-0 top-0 h-[400px] gradient-mesh" />

        <div className="relative mx-auto max-w-[1400px] px-5 lg:px-8">
          <div className="grid items-center gap-14 lg:grid-cols-12 lg:gap-10">
            <motion.div
              initial={reduce ? false : { opacity: 0, y: 18 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.6, ease: EASE }}
              className="lg:col-span-6"
            >
              <h1 className="display-hero max-w-[14ch] text-balance">
                Sell the tickets. Skip the spreadsheet.
              </h1>

              <p className="mt-6 max-w-[48ch] text-[1.0625rem] leading-relaxed text-muted-foreground">
                Set up an event, put tickets on sale and check people in at the door, from one
                dashboard.
              </p>

              <div className="mt-9 flex flex-wrap items-center gap-6">
                <Button size="lg" className="gap-2 px-7" onClick={handlePrimaryAction}>
                  {primaryLabel}
                  <ArrowRight weight="bold" size={16} />
                </Button>
                {/* Text link, not a second button box */}
                <Link
                  to="/"
                  className="focus-ring link-underline rounded-sm text-sm font-medium text-foreground"
                >
                  Browse events
                </Link>
              </div>
            </motion.div>

            <motion.div
              initial={reduce ? false : { opacity: 0, y: 26 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.14, duration: 0.7, ease: EASE }}
              className="lg:col-span-6"
            >
              <figure className="overflow-hidden rounded-md border border-border bg-card shadow-[0_36px_80px_-32px_oklch(0_0_0/0.9)]">
                <img
                  src="/organizers-landing-hero.png"
                  alt="The VenueSync organizer dashboard, showing an event with its ticket tiers and live sales figures."
                  width={1200}
                  height={860}
                  loading="eager"
                  decoding="sync"
                  fetchPriority="high"
                  className="h-auto w-full object-cover"
                  onError={(e) => {
                    e.currentTarget.src = "/event-image-2.webp"
                  }}
                />
              </figure>
            </motion.div>
          </div>

          {/* Sits under the hero, not inside it */}
          {isNonOrganizer && (
            <div className="mt-12 flex max-w-2xl items-start gap-3 rounded-md border border-primary/25 bg-primary/[0.06] p-4">
              <Storefront weight="fill" size={18} className="mt-0.5 shrink-0 text-primary" />
              <p className="text-sm leading-relaxed text-muted-foreground">
                Your account can host events too. Use{" "}
                <span className="font-medium text-foreground">Host an event</span> in the top bar to
                switch it on. It is free and it does not affect tickets you already hold.
              </p>
            </div>
          )}
        </div>
      </section>

      {/* ═══ 2. Capabilities. Hairline grid, no card boxes. ═══ */}
      <section className="border-t border-border py-16 lg:py-24">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <h2 className="display-section max-w-[16ch] text-balance">
            What you get on the first day
          </h2>

          {/* Grouped by negative space and 1px rules. Six boxes would have
              made this the same feature-card row as every other SaaS page. */}
          <div className="mt-12 grid gap-x-10 gap-y-px sm:grid-cols-2 lg:grid-cols-3">
            {capabilities.map(({ Icon, title, description }, i) => (
              <motion.div
                key={title}
                initial={reduce ? false : { opacity: 0, y: 16 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.4 }}
                transition={{ delay: (i % 3) * 0.07, duration: 0.45, ease: EASE }}
                className="border-t border-border py-8"
              >
                <Icon weight="fill" size={19} className="text-primary" />
                <h3 className="mt-4 text-[0.9375rem] font-semibold tracking-tight text-foreground">
                  {title}
                </h3>
                <p className="mt-2 max-w-[38ch] text-sm leading-relaxed text-muted-foreground">
                  {description}
                </p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* ═══ 3. The flow. A perforated rail, on concept. ═══ */}
      <section className="border-t border-border py-16 lg:py-24">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <h2 className="display-section max-w-[14ch] text-balance">From empty page to open doors</h2>

          <div className="relative mt-14">
            {/* Tear line running behind the three moments */}
            <div
              className="absolute left-0 right-0 top-[9px] hidden border-t border-dashed border-border md:block"
              aria-hidden
            />

            <ol className="relative grid gap-10 md:grid-cols-3 md:gap-8">
              {flow.map((item, i) => (
                <motion.li
                  key={item.title}
                  initial={reduce ? false : { opacity: 0, y: 18 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true, amount: 0.4 }}
                  transition={{ delay: i * 0.12, duration: 0.5, ease: EASE }}
                >
                  {/* A punched hole on the tear line marks each moment. The
                      name of the moment is the label, so there is no
                      "Step 1 of 3" to read past. */}
                  <span
                    className="mb-6 block h-[18px] w-[18px] rounded-full border border-border bg-primary"
                    aria-hidden
                  />
                  <h3 className="text-xl font-semibold tracking-tight text-foreground">
                    {item.title}
                  </h3>
                  <p className="mt-2.5 max-w-[36ch] text-sm leading-relaxed text-muted-foreground">
                    {item.description}
                  </p>
                </motion.li>
              ))}
            </ol>
          </div>
        </div>
      </section>

      {/* ═══ 4. One quote, given room. ═══ */}
      <section className="border-t border-border py-16 lg:py-24">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <motion.figure
            initial={reduce ? false : { opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true, amount: 0.3 }}
            transition={{ duration: 0.55, ease: EASE }}
            className="max-w-4xl"
          >
            <blockquote className="text-2xl font-medium leading-snug tracking-tight text-foreground lg:text-[2rem]">
              “We ran four thousand people through the gate on a Saturday with two scanners and
              nobody queued for more than a minute.”
            </blockquote>
            <figcaption className="mt-8 flex items-center gap-3.5">
              <span className="h-9 w-px bg-primary" aria-hidden />
              <span>
                <span className="block text-sm font-medium text-foreground">Priya Mehta</span>
                <span className="block text-sm text-muted-foreground">
                  Co-founder, Meridian Music Festival
                </span>
              </span>
            </figcaption>
          </motion.figure>
        </div>
      </section>

      {/* ═══ 5. Close. The one colour block on the page. ═══ */}
      <section className="bg-primary text-primary-foreground">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <div className="grid items-center gap-8 py-16 lg:grid-cols-12 lg:py-20">
            <div className="lg:col-span-7">
              <h2 className="max-w-[18ch] text-balance text-3xl font-semibold leading-tight tracking-tight lg:text-[2.5rem]">
                Put your next event on sale this afternoon
              </h2>
              <p className="mt-4 max-w-[52ch] text-[0.9375rem] leading-relaxed text-primary-foreground/75">
                Organizer access is free. There is no contract and nothing to install.
              </p>
            </div>
            <div className="lg:col-span-4 lg:col-start-9 lg:justify-self-end">
              {/* Ink fill on ember. White on ember measures 2.3:1 and fails. */}
              <button
                onClick={handlePrimaryAction}
                className="btn-press inline-flex h-11 items-center gap-2 rounded-md bg-[oklch(0.15_0.03_48)] px-7 text-[0.9375rem] font-medium text-primary outline-none transition-colors hover:bg-[oklch(0.19_0.035_48)] focus-visible:ring-2 focus-visible:ring-[oklch(0.15_0.03_48)] focus-visible:ring-offset-2 focus-visible:ring-offset-primary"
              >
                {primaryLabel}
                <ArrowRight weight="bold" size={16} />
              </button>
            </div>
          </div>
        </div>
      </section>

      <Footer />
    </PageContainer>
  )
}

export default OrganizersLandingPage
