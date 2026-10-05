"use client"

import type React from "react"
import { useEffect, useMemo, useRef, useState } from "react"
import { ImagePlus, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import { API_BASE } from "@/lib/api"

/** What the server takes: JPEG, PNG or WebP, up to 2 MB. Anything bigger is scaled down here first. */
const MAX_BYTES = 2 * 1024 * 1024
const MAX_EDGE = 1600
const ACCEPTED = ["image/jpeg", "image/png", "image/webp"]

/**
 * A photo ready to upload: as picked when it already fits, otherwise redrawn at most 1600 px on its long edge as a
 * JPEG. A phone photo is often 5 MB or more; scaling it here is kinder than refusing it.
 */
async function prepareEventPhoto(file: File): Promise<Blob> {
  if (ACCEPTED.includes(file.type) && file.size <= MAX_BYTES) return file
  const bitmap = await createImageBitmap(file).catch(() => {
    throw new Error("That file isn't a photo this browser can read. Use a JPEG, PNG or WebP image.")
  })
  const scale = Math.min(1, MAX_EDGE / Math.max(bitmap.width, bitmap.height))
  const canvas = document.createElement("canvas")
  canvas.width = Math.round(bitmap.width * scale)
  canvas.height = Math.round(bitmap.height * scale)
  canvas.getContext("2d")?.drawImage(bitmap, 0, 0, canvas.width, canvas.height)
  bitmap.close()
  const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, "image/jpeg", 0.85))
  if (!blob || blob.size > MAX_BYTES) throw new Error("That photo is too large even after resizing. Try a smaller one.")
  return blob
}

interface EventPhotoPickerProps {
  /** The photo the event has now (its imageUrl), if any. */
  currentUrl?: string | null
  /** A newly picked photo, not uploaded yet. */
  picked: Blob | null
  /** The current photo is to be removed on save. */
  removed: boolean
  onPick: (photo: Blob) => void
  onRemove: () => void
}

/** The event's photo on the form: a 5:3 preview (the card's shape), choose or replace, and remove. Saved with the event. */
export const EventPhotoPicker: React.FC<EventPhotoPickerProps> = ({ currentUrl, picked, removed, onPick, onRemove }) => {
  const input = useRef<HTMLInputElement>(null)
  const [error, setError] = useState<string | null>(null)
  const pickedUrl = useMemo(() => (picked ? URL.createObjectURL(picked) : null), [picked])
  useEffect(() => () => { if (pickedUrl) URL.revokeObjectURL(pickedUrl) }, [pickedUrl])

  const preview = pickedUrl ?? (currentUrl && !removed ? `${API_BASE}${currentUrl}` : null)

  const choose = async (file: File | undefined) => {
    if (!file) return
    setError(null)
    try {
      onPick(await prepareEventPhoto(file))
    } catch (e) {
      setError(e instanceof Error ? e.message : "That photo couldn't be used.")
    } finally {
      if (input.current) input.current.value = "" // picking the same file again still fires
    }
  }

  return (
    <div className="space-y-3">
      <div className="relative aspect-[5/3] overflow-hidden rounded-md border border-border bg-secondary">
        {preview ? (
          <img src={preview} alt="The event's photo" className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full items-center justify-center px-6 text-center text-sm text-muted-foreground">
            No photo yet. Events with one stand out in the feed; without one, a stand-in is shown.
          </div>
        )}
      </div>
      <input
        ref={input}
        type="file"
        accept={ACCEPTED.join(",")}
        className="hidden"
        onChange={(e) => choose(e.target.files?.[0])}
      />
      <div className="flex gap-2">
        <Button type="button" variant="outline" size="sm" onClick={() => input.current?.click()}>
          <ImagePlus className="mr-2 h-4 w-4" />
          {preview ? "Replace photo" : "Add a photo"}
        </Button>
        {preview && (
          <Button type="button" variant="ghost" size="sm" onClick={onRemove}>
            <Trash2 className="mr-2 h-4 w-4" />
            Remove
          </Button>
        )}
      </div>
      {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
      <p className="text-xs text-muted-foreground">JPEG, PNG or WebP. Large photos are scaled down to fit.</p>
    </div>
  )
}

export default EventPhotoPicker
