"use client"

import React from "react"
import { motion } from "framer-motion"
import { WarningCircle } from "@/components/icons"
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
    <div className="flex min-h-[100dvh] items-center justify-center bg-background p-5">
      <motion.div
        initial={{ opacity: 0, y: 14 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4, ease: [0.16, 1, 0.3, 1] }}
        className="w-full max-w-md rounded-md border border-border bg-card p-8"
      >
        <WarningCircle weight="fill" size={26} className="text-destructive" />

        <h1 className="mt-5 text-xl font-semibold tracking-tight text-foreground">
          This page stopped working
        </h1>
        <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
          Something threw an error while rendering. Reloading usually clears it.
        </p>

        {error && (
          <pre className="mt-5 overflow-x-auto rounded-md border border-destructive/30 bg-destructive/5 p-3.5 font-mono text-xs leading-relaxed text-destructive">
            {error.message}
          </pre>
        )}

        <div className="mt-7 flex flex-col gap-2.5 sm:flex-row">
          {onReset && (
            <Button onClick={onReset} className="flex-1">
              Try again
            </Button>
          )}
          <Link to="/" className="flex-1">
            <Button variant="outline" className="w-full">
              Back to events
            </Button>
          </Link>
        </div>
      </motion.div>
    </div>
  )
}

export default ErrorBoundary
