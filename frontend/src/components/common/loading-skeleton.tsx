"use client"

import type React from "react"
import { cn } from "@/lib/utils"
import { motion } from "framer-motion"

interface SkeletonProps {
  className?: string
}

export const Skeleton: React.FC<SkeletonProps> = ({ className }) => {
  return <div className={cn("skeleton rounded-lg", className)} />
}

/** Mirrors the real card exactly, notches included, so nothing shifts on load. */
export const EventCardSkeleton: React.FC = () => {
  return (
    <div className="relative rounded-md border border-border bg-card">
      <Skeleton className="aspect-[5/3] w-full rounded-none rounded-t-md" />
      <div className="relative">
        <div className="notch notch-l top-1/2 -translate-y-1/2" />
        <div className="notch notch-r top-1/2 -translate-y-1/2" />
        <hr className="perf mx-4" />
      </div>
      <div className="space-y-3 p-4">
        <Skeleton className="h-4 w-4/5 rounded-sm" />
        <div className="space-y-2">
          <Skeleton className="h-3 w-2/5 rounded-sm" />
          <Skeleton className="h-3 w-3/5 rounded-sm" />
        </div>
      </div>
    </div>
  )
}

export const TicketCardSkeleton: React.FC = () => {
  return (
    <div className="rounded-xl border border-border/30 bg-card/30 overflow-hidden">
      <div className="flex items-center gap-4 px-5 py-4 pl-6">
        <Skeleton className="w-12 h-12 rounded-xl flex-shrink-0" />
        <div className="flex-1 space-y-2">
          <Skeleton className="h-4 w-48" />
          <div className="flex items-center gap-3">
            <Skeleton className="h-3.5 w-14" />
            <Skeleton className="h-3.5 w-24" />
          </div>
        </div>
        <Skeleton className="w-4 h-4 rounded flex-shrink-0" />
      </div>
    </div>
  )
}

export const DashboardCardSkeleton: React.FC = () => {
  return (
    <div className="rounded-xl border border-border/30 bg-card/30 p-6">
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1 space-y-2">
          <div className="flex items-center gap-3">
            <Skeleton className="h-5 w-44" />
            <Skeleton className="h-4 w-16 rounded-full" />
          </div>
        </div>
        <Skeleton className="w-8 h-8 rounded-lg" />
      </div>
      <div className="space-y-2.5">
        {[40, 32, 36].map((w, i) => (
          <div key={i} className="flex items-center gap-3">
            <Skeleton className="w-4 h-4 rounded" />
            <Skeleton className={`h-4 w-${w}`} />
          </div>
        ))}
      </div>
      <div className="flex gap-2 mt-5 pt-4 border-t border-border/30">
        <Skeleton className="h-9 flex-1 rounded-lg" />
        <Skeleton className="h-9 w-9 rounded-lg" />
      </div>
    </div>
  )
}

export const PageLoader: React.FC = () => {
  return (
    <div className="flex min-h-[100dvh] items-center justify-center bg-background">
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        transition={{ duration: 0.3, delay: 0.15 }}
        className="flex flex-col items-center gap-4"
      >
        <div className="h-7 w-7 animate-spin rounded-full border-2 border-border border-t-primary" />
        <p className="font-mono text-xs uppercase tracking-[0.12em] text-muted-foreground">
          Loading
        </p>
      </motion.div>
    </div>
  )
}

export default Skeleton
