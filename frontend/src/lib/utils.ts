import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

/** "₹1,499", "₹12.50", "₹0": rupees with Indian digit grouping (VenueSync is India-only). */
export function formatInr(value: number): string {
  const whole = Number.isInteger(value)
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    minimumFractionDigits: whole ? 0 : 2,
    maximumFractionDigits: whole ? 0 : 2,
  }).format(value)
}

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
