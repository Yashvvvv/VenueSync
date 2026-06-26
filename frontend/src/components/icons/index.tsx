/**
 * VenueSync icon system
 *
 * Custom SVG brand mark + Phosphor Icons replacing generic Lucide icons.
 * Phosphor has duotone/fill/bold variants — far more contextual than Lucide's
 * single-weight strokes. All semantic icons use "fill" weight for presence.
 * Utility icons (close, chevron, menu) use "regular" for restraint.
 */

import { useId } from "react"

/* ─── Brand mark ──────────────────────────────────────────────────────── */

interface MarkProps {
  size?: number
  className?: string
}

/** Custom ticket-shaped VenueSync logomark. */
export const VenueSyncMark: React.FC<MarkProps> = ({ size = 36, className = "" }) => {
  const uid = useId().replace(/:/g, "")

  return (
    <svg
      width={size}
      height={Math.round(size * 0.72)}
      viewBox="0 0 40 29"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      className={className}
      aria-hidden
    >
      <defs>
        {/* Cut the circular notches out of the ticket body */}
        <mask id={`tm-${uid}`}>
          <rect width="40" height="29" rx="5" fill="white" />
          <circle cx="0"  cy="14.5" r="4.8" fill="black" />
          <circle cx="40" cy="14.5" r="4.8" fill="black" />
        </mask>
        <linearGradient id={`bg-${uid}`} x1="0" y1="0" x2="40" y2="29" gradientUnits="userSpaceOnUse">
          <stop offset="0%"   stopColor="oklch(0.72 0.20 275)" />
          <stop offset="100%" stopColor="oklch(0.60 0.18 318)" />
        </linearGradient>
      </defs>

      {/* Ticket body */}
      <rect width="40" height="29" rx="5" fill={`url(#bg-${uid})`} mask={`url(#tm-${uid})`} />

      {/* Perforation line — dashed, centered */}
      <line
        x1="20" y1="3.5" x2="20" y2="25.5"
        stroke="white" strokeWidth="1.1" strokeDasharray="2.8 2.2"
        strokeOpacity="0.32"
      />

      {/* Lightning bolt — the "sync" spark, right of perforation */}
      <path
        d="M24.5 5.5 L18 15.5 H23 L20.5 23.5 L29 13 H23.5 Z"
        fill="white"
        fillOpacity="0.93"
      />
    </svg>
  )
}

/* ─── Re-exports from Phosphor ──────────────────────────────────────────── */
/*
 * Import only what the app uses. All with explicit weight so the bundle
 * only includes the variants we need (Phosphor is tree-shakeable).
 */

export {
  /* Navigation */
  House,
  CalendarDots,
  Ticket,
  QrCode,
  SquaresFour,
  SignOut,
  List,
  X,
  UserCircle,

  /* Landing — categories */
  MusicNotes,
  Barbell,
  PaintBrush,
  Terminal,
  ForkKnife,
  Confetti,

  /* Landing — stats / social proof */
  CalendarCheck,
  UsersThree,
  ChartLineUp,
  Star,
  Quotes,

  /* Event cards / detail */
  MapPin,
  CalendarBlank,
  ArrowUpRight,
  ArrowRight,
  CaretRight,

  /* Ticket status */
  CheckCircle,
  XCircle,
  Clock,
  SealCheck,

  /* Organizer features */
  ChartBar,
  ShieldCheck,
  SlidersHorizontal,
  Lightning,

  /* Form / utility */
  MagnifyingGlass,
  Check,
  Info,
  WarningCircle,

} from "@phosphor-icons/react"
