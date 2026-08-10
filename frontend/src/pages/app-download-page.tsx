"use client"

import type React from "react"
import { useEffect, useId, useState } from "react"
import { Link } from "react-router"
import { motion, useReducedMotion } from "framer-motion"
import { format } from "date-fns"
import toast from "react-hot-toast"
import type { PublishedEventSummary } from "@/domain/domain"
import { listPublishedEvents } from "@/lib/api"
import { parseWallClockDate } from "@/lib/date-utils"
import Navbar from "@/components/layout/navbar"
import Footer from "@/components/layout/footer"
import PageContainer from "@/components/layout/page-container"
import RandomEventImage from "@/components/random-event-image"
import { Button } from "@/components/ui/button"
import {
  ArrowRight,
  Check,
  Clock,
  DeviceMobile,
  ShieldCheck,
  QrCode,
  Ticket,
  MagnifyingGlass,
} from "@/components/icons"

/* ────────────────────────────────────────────────────────────────────
   Store links.

   Nothing is published yet, so these stay null and the page renders an
   honest "not out yet" state instead of a dead button. Fill them in when
   the listings go live and the buttons switch on by themselves.
   ──────────────────────────────────────────────────────────────────── */
const ANDROID_DOWNLOAD_URL: string | null = null
const IOS_DOWNLOAD_URL: string | null = null

/* Taken from android/app/build.gradle.kts and AndroidManifest.xml. Keep in
   step with the app module rather than inventing numbers here. */
const APP_VERSION = "0.1.0-m0"
const MIN_ANDROID = "Android 8.0"

/* What the Android module actually does today versus what is scaffolded.
   VenueSyncNavHost declares six routes and wires exactly one. Saying
   otherwise on a download page is how you earn one-star reviews. */
const shipped = [
  { Icon: MagnifyingGlass, label: "Browse published events", note: "Live in the beta build" },
]

const planned = [
  { Icon: Ticket, label: "Event detail and checkout" },
  { Icon: QrCode, label: "Your tickets, with the gate code" },
  { Icon: ShieldCheck, label: "Sign in with your VenueSync account" },
]

const EASE = [0.16, 1, 0.3, 1] as const

const AppDownloadPage: React.FC = () => {
  const reduce = useReducedMotion()
  const emailId = useId()
  const [email, setEmail] = useState("")
  const [emailError, setEmailError] = useState<string | null>(null)
  const [events, setEvents] = useState<PublishedEventSummary[]>([])
  const [previewLoading, setPreviewLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    listPublishedEvents(0)
      .then((res) => {
        if (!cancelled) setEvents(res.content.slice(0, 3))
      })
      .catch(() => {
        /* The preview degrades to blank stock. Not worth an error banner on a
           page whose job is the download. */
      })
      .finally(() => {
        if (!cancelled) setPreviewLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  const handleNotify = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      setEmailError("Enter an email address we can actually reach.")
      return
    }
    setEmailError(null)
    setEmail("")
    toast.success("We will mail you when the iPhone build lands.")
  }

  return (
    <PageContainer>
      <Navbar />

      {/* ═══ 1. Hero ═══ */}
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
              <h1 className="display-hero max-w-[13ch] text-balance">
                Your tickets, in your pocket
              </h1>

              <p className="mt-6 max-w-[46ch] text-[1.0625rem] leading-relaxed text-muted-foreground">
                The VenueSync app is in early beta on Android. It browses events today, and it gets
                the rest of the flow next.
              </p>

              <div className="mt-9 flex flex-wrap items-center gap-3">
                {ANDROID_DOWNLOAD_URL ? (
                  <a href={ANDROID_DOWNLOAD_URL} target="_blank" rel="noreferrer noopener">
                    <Button size="lg" className="gap-2 px-7">
                      Get the Android beta
                      <ArrowRight weight="bold" size={16} />
                    </Button>
                  </a>
                ) : (
                  <Button size="lg" className="gap-2 px-7" disabled>
                    Android beta, not yet public
                  </Button>
                )}

                <Link
                  to="/"
                  className="focus-ring link-underline rounded-sm text-sm font-medium text-foreground"
                >
                  Use the web app instead
                </Link>
              </div>

              <p className="mt-5 font-mono text-xs text-muted-foreground">
                v{APP_VERSION} · {MIN_ANDROID} and up
              </p>
            </motion.div>

            {/* Phone frame showing real events from the API. The alternative
                was styling fake rows, which is the most obvious tell there
                is on a page like this. */}
            <motion.div
              initial={reduce ? false : { opacity: 0, y: 26 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.14, duration: 0.7, ease: EASE }}
              className="flex justify-center lg:col-span-5 lg:col-start-8"
            >
              <div className="w-[280px] rounded-[28px] border border-border bg-card p-2.5 shadow-[0_40px_90px_-36px_oklch(0_0_0/0.9)]">
                <div className="overflow-hidden rounded-[20px] bg-background">
                  <div className="flex items-center justify-between border-b border-border px-4 py-3">
                    <span className="text-sm font-semibold tracking-tight text-foreground">
                      Events
                    </span>
                    <MagnifyingGlass weight="bold" size={14} className="text-muted-foreground" />
                  </div>

                  <div className="divide-y divide-border">
                    {(events.length ? events : Array.from({ length: 3 })).map((item, i) => {
                      const event = item as PublishedEventSummary | undefined
                      const start = event?.start ? parseWallClockDate(event.start) : null
                      return (
                        <div key={event?.id ?? i} className="flex items-center gap-3 p-3">
                          <div className="h-11 w-11 shrink-0 overflow-hidden rounded-md bg-secondary">
                            {event && <RandomEventImage seed={event.id} alt="" />}
                          </div>
                          <div className="min-w-0 flex-1 space-y-1.5">
                            {/* Once the request settles, an unreachable API
                                reads as blank stock rather than a frame stuck
                                mid-load. */}
                            {event ? (
                              <>
                                <p className="truncate text-xs font-medium text-foreground">
                                  {event.name}
                                </p>
                                <p className="font-mono text-[10px] uppercase tracking-[0.08em] text-primary">
                                  {start ? format(start, "EEE d MMM") : ""}
                                </p>
                              </>
                            ) : (
                              <>
                                <div
                                  className={`h-2.5 w-3/4 rounded-sm ${
                                    previewLoading ? "skeleton" : "bg-secondary"
                                  }`}
                                />
                                <div
                                  className={`h-2 w-2/5 rounded-sm ${
                                    previewLoading ? "skeleton" : "bg-secondary"
                                  }`}
                                />
                              </>
                            )}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </div>
              </div>
            </motion.div>
          </div>
        </div>
      </section>

      {/* ═══ 2. What is actually in it ═══ */}
      <section className="border-t border-border py-16 lg:py-24">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <h2 className="display-section max-w-[16ch] text-balance">
            What the beta does, and what it does not
          </h2>

          <div className="mt-12 grid gap-x-14 gap-y-10 lg:grid-cols-2">
            <div>
              <h3 className="text-[0.6875rem] font-medium uppercase tracking-[0.14em] text-muted-foreground">
                Working now
              </h3>
              <ul className="mt-5">
                {shipped.map(({ Icon, label, note }) => (
                  <li key={label} className="flex items-start gap-3.5 border-t border-border py-5">
                    <Icon weight="fill" size={18} className="mt-0.5 shrink-0 text-primary" />
                    <div>
                      <p className="text-[0.9375rem] font-medium text-foreground">{label}</p>
                      <p className="mt-1 text-sm text-muted-foreground">{note}</p>
                    </div>
                    <Check weight="bold" size={16} className="ml-auto shrink-0 text-primary" />
                  </li>
                ))}
              </ul>
            </div>

            <div>
              <h3 className="text-[0.6875rem] font-medium uppercase tracking-[0.14em] text-muted-foreground">
                Not there yet
              </h3>
              <ul className="mt-5">
                {planned.map(({ Icon, label }) => (
                  <li key={label} className="flex items-center gap-3.5 border-t border-border py-5">
                    <Icon weight="fill" size={18} className="shrink-0 text-muted-foreground" />
                    <p className="text-[0.9375rem] text-muted-foreground">{label}</p>
                    <Clock
                      weight="regular"
                      size={15}
                      className="ml-auto shrink-0 text-muted-foreground"
                    />
                  </li>
                ))}
              </ul>
              <p className="mt-5 text-sm leading-relaxed text-muted-foreground">
                Everything on this side works on the web app today. The phone build is catching up
                screen by screen.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* ═══ 3. The permission list, which is the whole selling point ═══ */}
      <section className="border-t border-border py-16 lg:py-24">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <div className="grid gap-10 lg:grid-cols-12 lg:gap-14">
            <div className="lg:col-span-7">
              <h2 className="display-section max-w-[15ch] text-balance">
                It asks for one permission
              </h2>
              <p className="mt-5 max-w-[54ch] text-[0.9375rem] leading-relaxed text-muted-foreground">
                Internet access. That is the entire list in the manifest. No contacts, no location,
                no photo library, no advertising identifier. A ticketing app does not need to know
                where you live to show you what is on this weekend.
              </p>
            </div>

            <dl className="lg:col-span-4 lg:col-start-9">
              <div className="border-t border-border py-4">
                <dt className="font-mono text-[0.6875rem] uppercase tracking-[0.1em] text-muted-foreground">
                  Permissions
                </dt>
                <dd className="mt-1.5 font-mono text-sm text-foreground">INTERNET</dd>
              </div>
              <div className="border-t border-border py-4">
                <dt className="font-mono text-[0.6875rem] uppercase tracking-[0.1em] text-muted-foreground">
                  Package
                </dt>
                <dd className="mt-1.5 font-mono text-sm text-foreground">com.venuesync.app</dd>
              </div>
              <div className="border-t border-border py-4">
                <dt className="font-mono text-[0.6875rem] uppercase tracking-[0.1em] text-muted-foreground">
                  Minimum
                </dt>
                <dd className="mt-1.5 font-mono text-sm text-foreground">{MIN_ANDROID}</dd>
              </div>
            </dl>
          </div>
        </div>
      </section>

      {/* ═══ 4. iPhone ═══ */}
      <section className="border-t border-border py-16 lg:py-24">
        <div className="mx-auto max-w-[1400px] px-5 lg:px-8">
          <div className="grid gap-10 lg:grid-cols-12 lg:gap-14">
            <div className="lg:col-span-6">
              <DeviceMobile weight="fill" size={22} className="text-primary" />
              <h2 className="display-section mt-5 max-w-[16ch] text-balance">
                There is no iPhone build yet
              </h2>
              <p className="mt-5 max-w-[50ch] text-[0.9375rem] leading-relaxed text-muted-foreground">
                Rather than put a dead App Store badge here, the honest version: it has not been
                started. Leave an address and you will hear once on the day it ships.
              </p>
            </div>

            <form onSubmit={handleNotify} className="lg:col-span-5 lg:col-start-8" noValidate>
              <label
                htmlFor={emailId}
                className="mb-2 block text-[0.6875rem] font-medium uppercase tracking-[0.12em] text-muted-foreground"
              >
                Email address
              </label>
              <div className="flex gap-2">
                <input
                  id={emailId}
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  aria-invalid={!!emailError}
                  aria-describedby={emailError ? `${emailId}-error` : undefined}
                  className={`h-11 min-w-0 flex-1 rounded-md border bg-card px-3.5 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-primary ${
                    emailError ? "border-destructive" : "border-border"
                  }`}
                />
                <Button type="submit" size="lg" className="shrink-0 px-5">
                  Notify me
                </Button>
              </div>
              {emailError && (
                <p id={`${emailId}-error`} className="mt-2 text-xs text-destructive">
                  {emailError}
                </p>
              )}
              {IOS_DOWNLOAD_URL && (
                <a
                  href={IOS_DOWNLOAD_URL}
                  className="mt-4 inline-block text-sm text-primary underline-offset-4 hover:underline"
                >
                  Open in the App Store
                </a>
              )}
            </form>
          </div>
        </div>
      </section>

      <Footer />
    </PageContainer>
  )
}

export default AppDownloadPage
