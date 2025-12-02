"use client"

import { useEffect } from "react"
import { useAuth } from "react-oidc-context"
import { PageLoader } from "@/components/common/loading-skeleton"

const LoginPage: React.FC = () => {
  const { isLoading, isAuthenticated, signinRedirect } = useAuth()

  useEffect(() => {
    if (isLoading) {
      return
    }
    if (!isAuthenticated) {
      signinRedirect()
    }
  }, [isLoading, isAuthenticated, signinRedirect])

  return <PageLoader />
}

export default LoginPage
