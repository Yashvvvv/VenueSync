"use client"

import type React from "react"
import { motion, useReducedMotion } from "framer-motion"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"
import { Button } from "@/components/ui/button"
import { Link } from "react-router"
import { ArrowRight } from "@/components/icons"

/* Positions, not adjectives. "Community first" and "Passion" describe how a
   team would like to be seen; these describe what the product does when the
   choice is inconvenient. */
const principles = [
  {
    title: "The number on the card is the number you pay",
    body: "Ticketing has spent fifteen years perfecting the art of adding the fee on the last screen, after you have already decided you are going. It works, and it is the single most disliked thing about buying a ticket. Anything we add to a price belongs next to the price, on the first screen you see it.",
  },
  {
    title: "A ticket should work when the signal does not",
    body: "Venues are basements, fields and converted warehouses. Reception at the door is not a safe assumption, so the code renders once and stays on the device. Nobody should be the person holding up a queue while a page spins.",
  },
  {
    title: "A room of forty counts as much as a room of forty thousand",
    body: "The same tooling should serve the touring festival and the person putting on a poetry night above a pub. Capacity is a number in a form, not a tier of service, and it does not decide who gets the good software.",
  },
]

/* Verifiable claims about this repository, taken from the README and the
   source tree. The page previously showed "10,000+ events" and "500K+
   tickets sold" for a platform that has not launched. */
const status = [
  { label: "Backend", value: "Spring Boot", note: "Modular monolith, 5 domain modules" },
  { label: "Concurrency", value: "Pessimistic locking", note: "No overselling under concurrent buys" },
  { label: "Validation", value: "QR via ZXing", note: "Generated on purchase, scanned at the gate" },
  { label: "Auth", value: "Keycloak OIDC", note: "Organizer, attendee and staff roles" },
  { label: "Web", value: "React 19", note: "TypeScript, Vite, this app" },
  { label: "Android", value: "v0.1.0-m0", note: "Event browsing, early beta" },
]

const EASE = [0.16, 1, 0.3, 1] as const

const AboutPage: React.FC = () => {
  const reduce = useReducedMotion()

  return (
    <PageContainer>
      <Navbar />

      {/* ═══ 1. Statement. Left aligned, no blob behind it. ═══ */}
      <section className="relative overflow-hidden pb-16 pt-14 lg:pb-24 lg:pt-24">
        <div className="pointer-events-none absolute inset-x-0 top-0 h-[380px] gradient-mesh" />

        <div className="relative mx-auto max-w-[1400px] px-5 lg:px-8">
          <motion.div
            initial={reduce ? false : { opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, ease: EASE }}
          >
            <h1 className="display-hero max-w-[17ch] text-balance">
              Buying a ticket is the worst part of going out
            </h1>
            <p className="mt-7 max-w-[58ch] text-[1.0625rem] leading-relaxed text-muted-foreground">
              Everything either side of it has got better. Finding out a thing is happening,
              deciding to go, getting there, telling people afterwards. The ninety seconds in the
              middle where you actually pay have barely moved since 2011.
            </p>
          </motion.div>
        </div>
      </section>

      {/* ═══ 2. Principles. Full-width rows on tear lines. ═══ */}
      <section className="border-t border-border py-16 lg:py-24">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <h2 className="display-section max-w-[16ch] text-balance">What we hold to</h2>

          <div className="mt-12">
            {principles.map(({ title, body }, i) => (
              <motion.article
                key={title}
                initial={reduce ? false : { opacity: 0, y: 18 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.25 }}
                transition={{ delay: i * 0.08, duration: 0.5, ease: EASE }}
                className="grid gap-4 py-9 lg:grid-cols-12 lg:gap-10"
              >
                <h3 className="text-xl font-semibold leading-snug tracking-tight text-foreground lg:col-span-5">
                  {title}
                </h3>
                <p className="text-[0.9375rem] leading-relaxed text-muted-foreground lg:col-span-6 lg:col-start-7">
                  {body}
                </p>
                {i < principles.length - 1 && (
                  <hr className="perf mt-9 lg:col-span-12" aria-hidden />
                )}
              </motion.article>
            ))}
          </div>
        </div>
      </section>

      {/* ═══ 3. Honest status, in place of invented traction. ═══ */}
      <section className="border-t border-border py-16 lg:py-24">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <h2 className="display-section max-w-[18ch] text-balance">Where the build has got to</h2>
          <p className="mt-5 max-w-[56ch] text-[0.9375rem] leading-relaxed text-muted-foreground">
            VenueSync has not launched, so there are no attendance figures to quote and none are
            invented here. This is what exists in the repository today.
          </p>

          <dl className="mt-12 grid gap-x-10 sm:grid-cols-2 lg:grid-cols-3">
            {status.map(({ label, value, note }, i) => (
              <motion.div
                key={label}
                initial={reduce ? false : { opacity: 0, y: 14 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, amount: 0.35 }}
                transition={{ delay: (i % 3) * 0.06, duration: 0.45, ease: EASE }}
                className="border-t border-border py-7"
              >
                <dt className="font-mono text-[0.6875rem] uppercase tracking-[0.12em] text-muted-foreground">
                  {label}
                </dt>
                <dd className="mt-3 text-lg font-semibold tracking-tight text-foreground">
                  {value}
                </dd>
                <dd className="mt-1.5 text-sm leading-relaxed text-muted-foreground">{note}</dd>
              </motion.div>
            ))}
          </dl>
        </div>
      </section>

      {/* ═══ 4. Close. ═══ */}
      <section className="border-t border-border">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <div className="grid items-center gap-8 py-16 lg:grid-cols-12 lg:py-20">
            <div className="lg:col-span-7">
              <h2 className="display-section max-w-[16ch] text-balance">
                Putting something on yourself?
              </h2>
              <p className="mt-4 max-w-[52ch] text-[0.9375rem] leading-relaxed text-muted-foreground">
                Organizer access is free, and a poetry night gets the same dashboard as a festival.
              </p>
            </div>
            <div className="lg:col-span-4 lg:col-start-9 lg:justify-self-end">
              <Link to="/organizers">
                <Button size="lg" className="gap-2 px-7">
                  Host an event
                  <ArrowRight weight="bold" size={16} />
                </Button>
              </Link>
            </div>
          </div>
        </div>
      </section>

      <Footer />
    </PageContainer>
  )
}

export default AboutPage
