/**
 * Calendar export.
 *
 * The backend stores event times as LocalDateTime with no zone, and the app
 * shows them as wall clock times (see date-utils). RFC 5545 has an exact
 * match for that: a DATE-TIME with no "Z" and no TZID is a *floating* time,
 * meaning it happens at that clock reading wherever the viewer is. Stamping
 * these as UTC instead would shift a 20:00 gig by the reader's offset.
 *
 * DTSTAMP is the one field that must be real UTC: it records when the file
 * was produced, not when the event happens.
 */

/** Escapes the characters RFC 5545 reserves inside a text value. */
function escapeText(value: string): string {
  return value
    .replace(/\\/g, "\\\\")
    .replace(/;/g, "\\;")
    .replace(/,/g, "\\,")
    .replace(/\r?\n/g, "\\n")
}

const pad = (n: number) => String(n).padStart(2, "0")

/** Floating local time: YYYYMMDDTHHMMSS, deliberately without a Z. */
function floatingStamp(date: Date): string {
  return (
    `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}` +
    `T${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
  )
}

/** UTC stamp, for DTSTAMP only. */
function utcStamp(date: Date): string {
  return (
    `${date.getUTCFullYear()}${pad(date.getUTCMonth() + 1)}${pad(date.getUTCDate())}` +
    `T${pad(date.getUTCHours())}${pad(date.getUTCMinutes())}${pad(date.getUTCSeconds())}Z`
  )
}

/**
 * RFC 5545 caps a content line at 75 octets, continuing with CRLF + a single
 * space. Long event names silently corrupt the file in strict parsers
 * without this.
 */
function foldLine(line: string): string {
  const bytes = new TextEncoder().encode(line)
  if (bytes.length <= 75) return line

  const chunks: string[] = []
  let current = ""
  let currentBytes = 0

  for (const char of line) {
    const size = new TextEncoder().encode(char).length
    // 74 leaves room for the leading space on continuation lines.
    if (currentBytes + size > (chunks.length === 0 ? 75 : 74)) {
      chunks.push(current)
      current = ""
      currentBytes = 0
    }
    current += char
    currentBytes += size
  }
  if (current) chunks.push(current)

  return chunks.join("\r\n ")
}

export interface CalendarEventInput {
  id: string
  name: string
  venue?: string
  /** Wall clock start, already parsed. */
  start: Date
  /** Wall clock end. When absent a two hour block is assumed. */
  end?: Date
  url?: string
}

export function buildIcs({ id, name, venue, start, end, url }: CalendarEventInput): string {
  /* No end time in the data, so the file states an explicit two hour block
     rather than a zero length event, which several clients render as an
     unreadable sliver. */
  const finish = end ?? new Date(start.getTime() + 2 * 60 * 60 * 1000)

  const lines = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "PRODID:-//VenueSync//Event Ticketing//EN",
    "CALSCALE:GREGORIAN",
    "METHOD:PUBLISH",
    "BEGIN:VEVENT",
    `UID:${id}@venuesync`,
    `DTSTAMP:${utcStamp(new Date())}`,
    `DTSTART:${floatingStamp(start)}`,
    `DTEND:${floatingStamp(finish)}`,
    `SUMMARY:${escapeText(name)}`,
    ...(venue ? [`LOCATION:${escapeText(venue)}`] : []),
    ...(url ? [`URL:${escapeText(url)}`] : []),
    "END:VEVENT",
    "END:VCALENDAR",
  ]

  return lines.map(foldLine).join("\r\n")
}

/** Builds the file and hands it to the browser's download flow. */
export function downloadIcs(input: CalendarEventInput): void {
  const blob = new Blob([buildIcs(input)], { type: "text/calendar;charset=utf-8" })
  const href = URL.createObjectURL(blob)
  const anchor = document.createElement("a")

  anchor.href = href
  anchor.download = `${input.name.replace(/[^\w\d]+/g, "-").toLowerCase().slice(0, 60)}.ics`
  document.body.appendChild(anchor)
  anchor.click()
  document.body.removeChild(anchor)

  /* Revoking synchronously can cancel the download in some browsers. */
  setTimeout(() => URL.revokeObjectURL(href), 10_000)
}
