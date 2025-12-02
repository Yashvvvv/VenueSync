"use client"

import React from "react"
import { motion } from "framer-motion"
import { AlertTriangle, RefreshCw, Home } from "lucide-react"
import { Button } from "../ui/button"
import { Link } from "react-router"

interface ErrorBoundaryState {
  hasError: boolean
  error?: Error
}

interface ErrorBoundaryProps {
  children: React.ReactNode
  fallback?: React.ReactNode
}

export class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error("Error caught by ErrorBoundary:", error, errorInfo)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: undefined })
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback
      }

      return <ErrorFallback error={this.state.error} onReset={this.handleReset} />
    }

    return this.props.children
  }
}

interface ErrorFallbackProps {
  error?: Error
  onReset?: () => void
}

export const ErrorFallback: React.FC<ErrorFallbackProps> = ({ error, onReset }) => {
  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <div className="absolute inset-0 gradient-mesh opacity-30" />

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative glass rounded-3xl p-8 max-w-md w-full text-center"
      >
        <motion.div
          initial={{ scale: 0 }}
          animate={{ scale: 1 }}
          transition={{ delay: 0.2, type: "spring", stiffness: 200 }}
          className="w-20 h-20 rounded-full bg-destructive/20 flex items-center justify-center mx-auto mb-6"
        >
          <AlertTriangle className="w-10 h-10 text-destructive" />
        </motion.div>

        <h1 className="text-2xl font-bold text-foreground mb-2">Something went wrong</h1>
        <p className="text-muted-foreground mb-6">
          We encountered an unexpected error. Please try again or return to the home page.
        </p>

        {error && (
          <div className="bg-destructive/10 border border-destructive/30 rounded-xl p-4 mb-6 text-left">
            <p className="text-sm text-destructive font-mono break-all">{error.message}</p>
          </div>
        )}

        <div className="flex flex-col sm:flex-row gap-3">
          {onReset && (
            <Button onClick={onReset} className="flex-1 gradient-primary text-white gap-2">
              <RefreshCw className="w-4 h-4" />
              Try Again
            </Button>
          )}
          <Link to="/" className="flex-1">
            <Button variant="outline" className="w-full glass border-border/50 gap-2 bg-transparent">
              <Home className="w-4 h-4" />
              Go Home
            </Button>
          </Link>
        </div>
      </motion.div>
    </div>
  )
}

export default ErrorBoundary
