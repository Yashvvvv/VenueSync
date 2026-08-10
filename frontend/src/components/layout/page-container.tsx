"use client"

import type React from "react"

import { motion, useReducedMotion } from "framer-motion"
import type { ReactNode } from "react"

interface PageContainerProps {
  children: ReactNode
  className?: string
}

/**
 * Page-level entrance.
 *
 * Fade only. The previous version also translated the whole page on Y,
 * which moved the largest element on screen and pushed out LCP for the
 * sake of an effect nobody consciously sees. `100dvh` rather than
 * `100vh` so the first paint does not jump when mobile Safari collapses
 * its address bar.
 */
export const PageContainer: React.FC<PageContainerProps> = ({ children, className = "" }) => {
  const reduce = useReducedMotion()

  return (
    <motion.div
      initial={reduce ? false : { opacity: 0 }}
      animate={{ opacity: 1 }}
      transition={{ duration: 0.22, ease: "easeOut" }}
      className={`min-h-[100dvh] bg-background ${className}`}
    >
      {children}
    </motion.div>
  )
}

export default PageContainer
