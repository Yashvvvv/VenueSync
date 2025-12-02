"use client"

import type React from "react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { purchaseTicket } from "@/lib/api"
import { CheckCircle, CreditCard, Lock, ArrowLeft, Ticket } from "lucide-react"
import { useEffect, useState } from "react"
import { useAuth } from "react-oidc-context"
import { useNavigate, useParams, Link } from "react-router"
import Navbar from "@/components/layout/navbar"
import PageContainer from "@/components/layout/page-container"
import { motion, AnimatePresence } from "framer-motion"
import confetti from "canvas-confetti"

const PurchaseTicketPage: React.FC = () => {
  const { eventId, ticketTypeId } = useParams()
  const { isLoading, user } = useAuth()
  const navigate = useNavigate()
  const [error, setError] = useState<string | undefined>()
  const [isPurchaseSuccess, setIsPurchaseSuccess] = useState(false)
  const [isProcessing, setIsProcessing] = useState(false)

  useEffect(() => {
    if (!isPurchaseSuccess) {
      return
    }

    // Trigger confetti animation
    confetti({
      particleCount: 100,
      spread: 70,
      origin: { y: 0.6 },
      colors: ["#8b5cf6", "#a855f7", "#c084fc"],
    })

    const timer = setTimeout(() => {
      navigate("/dashboard/tickets")
    }, 3000)

    return () => clearTimeout(timer)
  }, [isPurchaseSuccess, navigate])

  const handlePurchase = async () => {
    if (isLoading || !user?.access_token || !eventId || !ticketTypeId) {
      return
    }

    setIsProcessing(true)
    setError(undefined)

    try {
      await purchaseTicket(user.access_token, eventId, ticketTypeId)
      setIsPurchaseSuccess(true)
    } catch (err) {
      if (err instanceof Error) {
        setError(err.message)
      } else if (typeof err === "string") {
        setError(err)
      } else {
        setError("An unknown error occurred")
      }
    } finally {
      setIsProcessing(false)
    }
  }

  return (
    <PageContainer>
      <Navbar />

      <div className="min-h-[calc(100vh-64px)] flex items-center justify-center py-12 px-4">
        <div className="absolute inset-0 gradient-mesh opacity-30" />

        <AnimatePresence mode="wait">
          {isPurchaseSuccess ? (
            <motion.div
              key="success"
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9 }}
              className="relative glass rounded-3xl p-8 max-w-md w-full text-center"
            >
              <motion.div
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                transition={{ delay: 0.2, type: "spring", stiffness: 200 }}
                className="w-20 h-20 rounded-full bg-green-500/20 flex items-center justify-center mx-auto mb-6"
              >
                <CheckCircle className="w-10 h-10 text-green-500" />
              </motion.div>

              <h2 className="text-2xl font-bold text-foreground mb-2">Purchase Complete!</h2>
              <p className="text-muted-foreground mb-6">
                Your ticket has been added to your wallet. Get ready for an amazing experience!
              </p>

              <div className="flex justify-center gap-1">
                {[0, 1, 2].map((i) => (
                  <motion.div
                    key={i}
                    animate={{ opacity: [0.3, 1, 0.3] }}
                    transition={{ duration: 1, repeat: Number.POSITIVE_INFINITY, delay: i * 0.2 }}
                    className="w-2 h-2 rounded-full bg-primary"
                  />
                ))}
              </div>
              <p className="text-sm text-muted-foreground mt-4">Redirecting to your tickets...</p>
            </motion.div>
          ) : (
            <motion.div
              key="form"
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              className="relative glass rounded-3xl p-8 max-w-md w-full"
            >
              {/* Back Button */}
              <Link
                to={`/events/${eventId}`}
                className="inline-flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors mb-6"
              >
                <ArrowLeft className="w-4 h-4" />
                Back to event
              </Link>

              {/* Header */}
              <div className="flex items-center gap-3 mb-6">
                <div className="w-12 h-12 rounded-xl gradient-primary flex items-center justify-center">
                  <Ticket className="w-6 h-6 text-white" />
                </div>
                <div>
                  <h1 className="text-xl font-bold text-foreground">Complete Purchase</h1>
                  <p className="text-sm text-muted-foreground">Secure checkout</p>
                </div>
              </div>

              {/* Error Alert */}
              {error && (
                <motion.div
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="mb-6 p-4 rounded-xl bg-destructive/10 border border-destructive/30"
                >
                  <p className="text-sm text-destructive">{error}</p>
                </motion.div>
              )}

              {/* Form */}
              <div className="space-y-4">
                <div className="space-y-2">
                  <Label className="text-foreground">Card Number</Label>
                  <div className="relative">
                    <Input
                      type="text"
                      placeholder="1234 5678 9012 3456"
                      maxLength={19}
                      className="pl-10 bg-secondary border-border text-foreground"
                    />
                    <CreditCard className="absolute w-4 h-4 text-muted-foreground top-3 left-3" />
                  </div>
                </div>

                <div className="space-y-2">
                  <Label className="text-foreground">Cardholder Name</Label>
                  <Input type="text" placeholder="John Smith" className="bg-secondary border-border text-foreground" />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label className="text-foreground">Expiry Date</Label>
                    <Input
                      type="text"
                      placeholder="MM/YY"
                      maxLength={5}
                      className="bg-secondary border-border text-foreground"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label className="text-foreground">CVV</Label>
                    <Input
                      type="text"
                      placeholder="123"
                      maxLength={4}
                      className="bg-secondary border-border text-foreground"
                    />
                  </div>
                </div>

                <Button
                  className="w-full gradient-primary text-white h-12 text-lg font-semibold mt-6"
                  onClick={handlePurchase}
                  disabled={isProcessing}
                >
                  {isProcessing ? (
                    <div className="flex items-center gap-2">
                      <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                      Processing...
                    </div>
                  ) : (
                    "Complete Purchase"
                  )}
                </Button>

                <div className="flex items-center justify-center gap-2 text-xs text-muted-foreground pt-2">
                  <Lock className="w-3 h-3" />
                  <span>This is a demo. No real payment will be processed.</span>
                </div>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </PageContainer>
  )
}

export default PurchaseTicketPage
