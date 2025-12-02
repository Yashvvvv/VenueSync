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

export const EventCardSkeleton: React.FC = () => {
  return (
    <div className="glass rounded-2xl overflow-hidden">
      <div className="relative">
        <Skeleton className="h-52 w-full rounded-none" />
        {/* Date badge skeleton */}
        <div className="absolute top-3 left-3 glass rounded-lg p-2 w-14 h-16">
          <Skeleton className="h-3 w-8 mb-1" />
          <Skeleton className="h-6 w-8" />
        </div>
      </div>
      <div className="p-5 space-y-4">
        <Skeleton className="h-6 w-4/5" />
        <div className="space-y-2">
          <div className="flex items-center gap-2">
            <Skeleton className="h-4 w-4 rounded" />
            <Skeleton className="h-4 w-1/2" />
          </div>
          <div className="flex items-center gap-2">
            <Skeleton className="h-4 w-4 rounded" />
            <Skeleton className="h-4 w-2/3" />
          </div>
        </div>
      </div>
    </div>
  )
}

export const TicketCardSkeleton: React.FC = () => {
  return (
    <div className="glass rounded-2xl p-5 flex items-center gap-4">
      <Skeleton className="w-14 h-14 rounded-xl flex-shrink-0" />
      <div className="flex-1 space-y-2">
        <div className="flex items-center gap-2">
          <Skeleton className="h-5 w-40" />
          <Skeleton className="h-5 w-16 rounded-full" />
        </div>
        <div className="flex items-center gap-4">
          <Skeleton className="h-4 w-16" />
          <Skeleton className="h-4 w-24" />
        </div>
      </div>
      <Skeleton className="w-5 h-5 rounded flex-shrink-0" />
    </div>
  )
}

export const DashboardCardSkeleton: React.FC = () => {
  return (
    <div className="glass rounded-2xl p-6">
      <div className="flex items-start justify-between mb-4">
        <div className="flex-1 space-y-2">
          <div className="flex items-center gap-3">
            <Skeleton className="h-6 w-48" />
            <Skeleton className="h-5 w-20 rounded-full" />
          </div>
        </div>
        <Skeleton className="w-8 h-8 rounded-lg" />
      </div>
      <div className="space-y-3">
        <div className="flex items-center gap-3">
          <Skeleton className="w-4 h-4 rounded" />
          <Skeleton className="h-4 w-40" />
        </div>
        <div className="flex items-center gap-3">
          <Skeleton className="w-4 h-4 rounded" />
          <Skeleton className="h-4 w-32" />
        </div>
        <div className="flex items-center gap-3">
          <Skeleton className="w-4 h-4 rounded" />
          <Skeleton className="h-4 w-36" />
        </div>
      </div>
      <div className="flex gap-2 mt-6 pt-4 border-t border-border/50">
        <Skeleton className="h-10 flex-1 rounded-lg" />
        <Skeleton className="h-10 w-10 rounded-lg" />
      </div>
    </div>
  )
}

export const PageLoader: React.FC = () => {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center">
      <div className="absolute inset-0 gradient-mesh opacity-30" />
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        className="relative text-center"
      >
        <motion.div
          animate={{ rotate: 360 }}
          transition={{ duration: 2, repeat: Number.POSITIVE_INFINITY, ease: "linear" }}
          className="w-16 h-16 rounded-2xl gradient-primary flex items-center justify-center mx-auto mb-6 shadow-lg shadow-primary/30"
        >
          <div className="w-8 h-8 border-2 border-white border-t-transparent rounded-full animate-spin" />
        </motion.div>
        <div className="flex justify-center gap-1">
          {[0, 1, 2].map((i) => (
            <motion.div
              key={i}
              animate={{ opacity: [0.3, 1, 0.3], scale: [0.8, 1, 0.8] }}
              transition={{ duration: 1.2, repeat: Number.POSITIVE_INFINITY, delay: i * 0.2 }}
              className="w-2 h-2 rounded-full bg-primary"
            />
          ))}
        </div>
      </motion.div>
    </div>
  )
}

export default Skeleton
