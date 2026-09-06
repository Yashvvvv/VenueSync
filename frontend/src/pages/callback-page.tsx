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

    // Deliberately does NOT navigate to /login on error. /login immediately
    // re-attempts signinRedirect, so bouncing back there turned every auth
    // failure into an infinite redirect loop that trapped the tab. Render the
    // error instead - it is the only place the IDP's reason is visible.
    if (error) {
      console.error("Authentication error:", error)
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
    <div className="min-h-[100dvh] bg-background flex items-center justify-center">
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
            <pre className="text-left text-xs bg-muted text-muted-foreground rounded-md p-3 overflow-x-auto max-w-lg mx-auto mb-4">
              {error.message}
            </pre>
            <a href="/login" className="underline text-primary">
              Back to sign in
            </a>
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
