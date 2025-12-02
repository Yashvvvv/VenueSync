"use client"

import { useEffect } from "react"
import { Outlet, useLocation } from "react-router"

const RootLayout: React.FC = () => {
  const { pathname } = useLocation()

  useEffect(() => {
    window.scrollTo(0, 0)
  }, [pathname])

  return <Outlet />
}

export default RootLayout
