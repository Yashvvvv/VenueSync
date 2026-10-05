import { useState } from "react"
import { API_BASE } from "@/lib/api"

/**
 * An event's photo, or stand-in photography for events that have none (or whose photo fails to load).
 *
 * The picture is derived from a seed (the event id) instead of Math.random,
 * so a given event keeps the same photo across renders, pagination and
 * revisits. Picking randomly on mount made an event visibly change identity
 * every time the grid re-rendered.
 */

const IMAGE_COUNT = 4

function seedToIndex(seed: string): number {
  let hash = 0
  for (let i = 0; i < seed.length; i++) {
    hash = (hash << 5) - hash + seed.charCodeAt(i)
    hash |= 0
  }
  return (Math.abs(hash) % IMAGE_COUNT) + 1
}

interface RandomEventImageProps {
  /** Stable identifier. Same seed always resolves to the same stand-in. */
  seed?: string
  /** The event's own photo (its imageUrl, relative to the API). Wins over the stand-in. */
  src?: string | null
  alt?: string
  className?: string
  /** Set on the first card of the first fold so it is not lazy-loaded. */
  priority?: boolean
}

const RandomEventImage: React.FC<RandomEventImageProps> = ({
  seed = "venuesync",
  src,
  alt = "",
  className = "",
  priority = false,
}) => {
  const index = seedToIndex(seed)
  // A photo that 404s or won't decode falls back to the stand-in instead of a broken image.
  const [failed, setFailed] = useState(false)
  const photo = src && !failed ? `${API_BASE}${src}` : `/event-image-${index}.webp`

  return (
    <img
      src={photo}
      onError={() => setFailed(true)}
      alt={alt}
      width={800}
      height={600}
      loading={priority ? "eager" : "lazy"}
      decoding={priority ? "sync" : "async"}
      fetchPriority={priority ? "high" : "auto"}
      className={`h-full w-full object-cover ${className}`}
    />
  )
}

export default RandomEventImage
