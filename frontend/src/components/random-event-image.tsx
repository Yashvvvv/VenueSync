/**
 * Stand-in photography for events that have no uploaded image yet.
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
  /** Stable identifier. Same seed always resolves to the same photo. */
  seed?: string
  alt?: string
  className?: string
  /** Set on the first card of the first fold so it is not lazy-loaded. */
  priority?: boolean
}

const RandomEventImage: React.FC<RandomEventImageProps> = ({
  seed = "venuesync",
  alt = "",
  className = "",
  priority = false,
}) => {
  const index = seedToIndex(seed)

  return (
    <img
      src={`/event-image-${index}.webp`}
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
