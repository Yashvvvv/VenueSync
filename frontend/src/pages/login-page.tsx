"use client"

import { useEffect, useRef } from "react"
import { useAuth } from "react-oidc-context"
import { PageLoader } from "@/components/common/loading-skeleton"

const LoginPage: React.FC = () => {
  const { isLoading, isAuthenticated, signinRedirect, error, activeNavigator } =
    useAuth()

  // Guards against re-entering the redirect. Without it, an auth failure sent
  // the user to /callback, which sent them back here, which redirected to the
  // IDP again - an infinite loop that also made the tab impossible to leave.
  // A ref (not state) because StrictMode runs effects twice in development and
  // would otherwise fire signinRedirect twice on a single mount.
  const attempted = useRef(false)

  useEffect(() => {
    if (isLoading || isAuthenticated || error || activeNavigator) {
      return
    }
    if (attempted.current) {
      return
    }
    attempted.current = true
    signinRedirect()
  }, [isLoading, isAuthenticated, error, activeNavigator, signinRedirect])

  if (error) {
    return (
      <div className="min-h-[100dvh] bg-background flex items-center justify-center p-6">
        <div className="max-w-lg w-full text-center">
          <h2 className="text-xl font-semibold text-destructive mb-2">
            Sign-in failed
          </h2>
          <p className="text-muted-foreground mb-4">
            The identity provider rejected the request.
          </p>
          <pre className="text-left text-xs bg-muted text-muted-foreground rounded-md p-3 overflow-x-auto mb-6">
            {error.message}
          </pre>
          <button
            type="button"
            className="underline text-primary"
            onClick={() => {
              attempted.current = false
              signinRedirect()
            }}
          >
            Try again
          </button>
        </div>
      </div>
    )
  }

  return <PageLoader />
}

export default LoginPage
