"use client"

import type React from "react"

import { useRoles } from "@/hooks/use-roles"
import { useNavigate } from "react-router"
import { useEffect } from "react"
import { motion } from "framer-motion"
import { Ticket } from "lucide-react"

const DashboardPage: React.FC = () => {
  const { isLoading, isOrganizer, isStaff } = useRoles()
  const navigate = useNavigate()

  useEffect(() => {
    if (isLoading) return

    if (isOrganizer) {
      navigate("/dashboard/events", { replace: true })
    } else if (isStaff) {
      navigate("/dashboard/validate-qr", { replace: true })
    } else {
      navigate("/dashboard/tickets", { replace: true })
    }
  }, [isLoading, isOrganizer, isStaff, navigate])

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
          className="w-16 h-16 rounded-2xl gradient-primary flex items-center justify-center mx-auto mb-6"
        >
          <Ticket className="w-8 h-8 text-white" />
        </motion.div>

        <h2 className="text-xl font-semibold text-foreground mb-2">Loading Dashboard</h2>
        <p className="text-muted-foreground">Preparing your personalized experience...</p>

        <div className="mt-6 flex justify-center gap-1">
          {[0, 1, 2].map((i) => (
            <motion.div
              key={i}
              animate={{ opacity: [0.3, 1, 0.3] }}
              transition={{ duration: 1, repeat: Number.POSITIVE_INFINITY, delay: i * 0.2 }}
              className="w-2 h-2 rounded-full bg-primary"
            />
          ))}
        </div>
      </motion.div>
    </div>
  )
}

export default DashboardPage
