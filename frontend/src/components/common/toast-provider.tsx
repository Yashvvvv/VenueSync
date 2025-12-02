import type React from "react"
import { Toaster } from "react-hot-toast"

export const ToastProvider: React.FC = () => {
  return (
    <Toaster
      position="bottom-right"
      toastOptions={{
        duration: 4000,
        style: {
          background: "oklch(0.12 0.015 270)",
          color: "oklch(0.98 0 0)",
          border: "1px solid oklch(0.25 0.02 270)",
          borderRadius: "12px",
          padding: "16px",
        },
        success: {
          iconTheme: {
            primary: "oklch(0.65 0.25 280)",
            secondary: "white",
          },
        },
        error: {
          iconTheme: {
            primary: "oklch(0.55 0.22 25)",
            secondary: "white",
          },
        },
      }}
    />
  )
}

export default ToastProvider
