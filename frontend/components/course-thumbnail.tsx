"use client"

import Image from "next/image"
import { useState } from "react"
import { BookOpen } from "lucide-react"
import { cn } from "@/lib/utils"

/**
 * A course cover image, or a branded placeholder when there is nothing to show.
 *
 * <p>Exists because the landing page previously handed `thumbnailUrl` straight to `next/image`
 * with a static `/placeholder.svg` fallback for the *empty* case only. An image reference that
 * resolved to nothing — an upload that never persisted, a storage backend the browser cannot
 * reach — produced a broken-image glyph inside the card, and one card's failure was the whole
 * section's problem.</p>
 *
 * <p>Two reference shapes are accepted, because the backend genuinely returns both depending on
 * which storage provider is active:</p>
 * <ul>
 *   <li>a root-relative path such as {@code /v1/content/courses/<institutionId>/<file>.png},
 *       served through the app's own rewrite;</li>
 *   <li>an absolute URL, which is what object storage hands back.</li>
 * </ul>
 *
 * <p>Anything else — most importantly a {@code blob:} URL, which is a browser-local preview that
 * means nothing on another device — is treated as "no image" rather than rendered. That is the
 * case where a course looks fine on the uploader's screen and broken everywhere else.</p>
 */
export function CourseThumbnail({
  src,
  alt,
  className,
  sizes = "(max-width: 768px) 100vw, 33vw",
}: {
  src?: string | null
  alt: string
  className?: string
  sizes?: string
}) {
  const [failed, setFailed] = useState(false)

  const usable = normaliseImageReference(src)
  const showFallback = !usable || failed

  return (
    <>
      {usable && !failed && (
        <Image
          src={usable}
          alt={alt}
          fill
          sizes={sizes}
          onError={() => setFailed(true)}
          className={cn("object-cover", className)}
        />
      )}
      {showFallback && (
        <div
          // Decorative: the card's heading already names the course, so announcing the
          // placeholder would just repeat it to a screen reader.
          role="presentation"
          aria-hidden="true"
          className={cn(
            "absolute inset-0 flex items-center justify-center bg-gradient-to-br from-primary/10 via-primary/5 to-transparent",
            className
          )}
        >
          <BookOpen className="size-10 text-primary/40" />
        </div>
      )}
    </>
  )
}

/**
 * Returns a reference the browser can actually fetch, or null.
 *
 * <p>Rejects blob:, data: and relative filesystem paths. A `blob:` URL in particular is the
 * signature of an image that only ever existed in the uploader's tab.</p>
 */
export function normaliseImageReference(src?: string | null): string | null {
  if (!src) return null
  const trimmed = src.trim()
  if (!trimmed) return null
  if (/^blob:/i.test(trimmed)) return null
  if (/^data:/i.test(trimmed)) return null
  // A bare Windows or POSIX path is never a servable reference.
  if (/^[a-zA-Z]:[\\/]/.test(trimmed) || trimmed.startsWith("/home/") || trimmed.startsWith("C:\\")) {
    return null
  }
  if (trimmed.startsWith("/") || /^https?:\/\//i.test(trimmed)) return trimmed
  return null
}
