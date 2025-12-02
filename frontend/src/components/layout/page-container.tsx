"use client"

import type React from "react"

import { motion } from "framer-motion"
import type { ReactNode } from "react"

interface PageContainerProps {
  children: ReactNode
  className?: string
}

const pageVariants = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  exit: { opacity: 0, y: -20 },
}

export const PageContainer: React.FC<PageContainerProps> = ({ children, className = "" }) => {
  return (
    <motion.div
      variants={pageVariants}
      initial="initial"
      animate="animate"
      exit="exit"
      transition={{ duration: 0.3, ease: "easeOut" }}
      className={`min-h-screen bg-background ${className}`}
    >
      {children}
    </motion.div>
  )
}

export default PageContainer
