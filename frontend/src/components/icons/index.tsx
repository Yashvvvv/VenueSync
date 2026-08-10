/**
 * VenueSync icon system
 *
 * One icon family only (Phosphor). Mixing families is the fastest way to
 * make an interface look assembled rather than designed, so lucide-react
 * is not imported anywhere in the app.
 *
 * Weight convention:
 *   fill    - semantic icons that carry meaning (status, category, action)
 *   regular - utility chrome (close, menu, caret)
 */

import { useId } from "react"

/* ─── Brand mark ──────────────────────────────────────────────────────── */

interface MarkProps {
  size?: number
  className?: string
}

/**
 * The logomark is a ticket stub: a rounded body with two notches punched
 * out of the sides and a perforation you would tear along. Flat ember,
 * no gradient. The bolt on the right half is the "sync".
 */
export const VenueSyncMark: React.FC<MarkProps> = ({ size = 34, className = "" }) => {
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
        <mask id={`tm-${uid}`}>
          <rect width="40" height="29" rx="4.5" fill="white" />
          <circle cx="0" cy="14.5" r="4.6" fill="black" />
          <circle cx="40" cy="14.5" r="4.6" fill="black" />
        </mask>
      </defs>

      {/* Painted from the theme tokens rather than fixed ember, so the mark
          follows whichever vibe is active instead of clashing with it. */}
      <rect width="40" height="29" rx="4.5" fill="var(--primary)" mask={`url(#tm-${uid})`} />

      {/* Tear line */}
      <line
        x1="15"
        y1="4"
        x2="15"
        y2="25"
        stroke="var(--primary-foreground)"
        strokeWidth="1.2"
        strokeDasharray="2.4 2.2"
        strokeOpacity="0.55"
      />

      {/* The spark, on the stub side of the tear */}
      <path
        d="M28 5.5 L21.5 15.5 H26 L23.5 23.5 L31.5 13 H26.5 Z"
        fill="var(--primary-foreground)"
        fillOpacity="0.9"
      />
    </svg>
  )
}

/* ─── Organizer monograms ─────────────────────────────────────────────── */

/**
 * The organizers on the landing page are demo tenants, so there is no real
 * logo to license. Rather than setting their names as plain text (which
 * always reads as a placeholder), each gets a distinct geometric glyph.
 * Deliberately varied shapes so the wall does not look like one mark
 * repeated eight times.
 */
export type MonogramGlyph =
  | "bar-circle"
  | "triangle"
  | "pulse"
  | "asterisk"
  | "diamond"
  | "chevrons"
  | "arc"
  | "orbit"

export const OrganizerGlyph: React.FC<{ glyph: MonogramGlyph; size?: number }> = ({
  glyph,
  size = 20,
}) => {
  const common = {
    width: size,
    height: size,
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: 1.6,
    strokeLinecap: "round" as const,
    strokeLinejoin: "round" as const,
    "aria-hidden": true,
  }

  switch (glyph) {
    case "bar-circle":
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="8.5" />
          <path d="M3.5 12h17" />
        </svg>
      )
    case "triangle":
      return (
        <svg {...common}>
          <path d="M12 4 21 20H3z" />
        </svg>
      )
    case "pulse":
      return (
        <svg {...common}>
          <path d="M2 12h4l3-7 5 14 3-7h5" />
        </svg>
      )
    case "asterisk":
      return (
        <svg {...common}>
          <path d="M12 3v18M4 7l16 10M20 7 4 17" />
        </svg>
      )
    case "diamond":
      return (
        <svg {...common}>
          <rect x="6" y="6" width="12" height="12" transform="rotate(45 12 12)" />
        </svg>
      )
    case "chevrons":
      return (
        <svg {...common}>
          <path d="m4 14 8-7 8 7M4 20l8-7 8 7" />
        </svg>
      )
    case "arc":
      return (
        <svg {...common}>
          <path d="M3 18a9 9 0 0 1 18 0" />
          <path d="M3 18h18" />
        </svg>
      )
    case "orbit":
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="4" />
          <ellipse cx="12" cy="12" rx="10" ry="4.5" transform="rotate(-24 12 12)" />
        </svg>
      )
  }
}

/* ─── Re-exports from Phosphor ──────────────────────────────────────────── */

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

  /* Categories */
  MusicNotes,
  Barbell,
  PaintBrush,
  Terminal,
  ForkKnife,
  Confetti,

  /* Social proof */
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
  ArrowLeft,
  ArrowDown,
  CaretRight,
  CaretLeft,

  /* Ticket lifecycle */
  CheckCircle,
  XCircle,
  Clock,
  SealCheck,
  DeviceMobile,
  Receipt,

  /* Organizer features */
  ChartBar,
  ShieldCheck,
  SlidersHorizontal,
  Lightning,
  Storefront,

  /* Feed actions */
  ShareNetwork,
  BookmarkSimple,
  CaretDown,

  /* Form / utility */
  MagnifyingGlass,
  Check,
  Info,
  WarningCircle,
  EnvelopeSimple,

  /* Social */
  XLogo,
  InstagramLogo,
  GithubLogo,
  LinkedinLogo,
} from "@phosphor-icons/react"
