import type React from "react"
import { Toaster } from "react-hot-toast"

export const ToastProvider: React.FC = () => {
  return (
    <Toaster
      position="bottom-right"
      toastOptions={{
        duration: 4000,
        style: {
          background: "var(--popover)",
          color: "var(--foreground)",
          border: "1px solid var(--border)",
          borderRadius: "var(--radius)",
          padding: "12px 14px",
          fontSize: "0.875rem",
          boxShadow: "0 24px 60px -20px oklch(0 0 0 / 0.7)",
        },
        success: {
          iconTheme: {
            primary: "var(--success)",
            secondary: "var(--popover)",
          },
        },
        error: {
          iconTheme: {
            primary: "var(--destructive)",
            secondary: "var(--popover)",
          },
        },
      }}
    />
  )
}

export default ToastProvider
