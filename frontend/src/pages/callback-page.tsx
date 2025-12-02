"use client"

import type React from "react"

import { useEffect } from "react"
import { useAuth } from "react-oidc-context"
import { useNavigate } from "react-router"
import { motion } from "framer-motion"
import { Ticket } from "lucide-react"

const CallbackPage: React.FC = () => {
  const { isLoading, isAuthenticated, error } = useAuth()
  const navigate = useNavigate()

  useEffect(() => {
    if (isLoading) {
      return
    }

    if (error) {
      console.error("Authentication error:", error)
      navigate("/login")
      return
    }

    if (isAuthenticated) {
      const redirectPath = localStorage.getItem("redirectPath")
      if (redirectPath) {
        localStorage.removeItem("redirectPath")
        navigate(redirectPath)
      } else {
        navigate("/")
      }
    } else {
      navigate("/login")
    }
  }, [isLoading, isAuthenticated, error, navigate])

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

        {error ? (
          <>
            <h2 className="text-xl font-semibold text-destructive mb-2">Authentication Failed</h2>
            <p className="text-muted-foreground">Redirecting to login...</p>
          </>
        ) : (
          <>
            <h2 className="text-xl font-semibold text-foreground mb-2">
              {isLoading ? "Processing..." : "Welcome Back!"}
            </h2>
            <p className="text-muted-foreground">{isLoading ? "Completing your sign in" : "Redirecting you now..."}</p>
          </>
        )}

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

export default CallbackPage
