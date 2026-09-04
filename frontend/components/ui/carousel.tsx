"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { ChevronLeft, ChevronRight } from "lucide-react"

import { cn } from "@/lib/utils"

interface CarouselProps {
  images: string[]
  alt: string
  className?: string
}

export function Carousel({ images, alt, className }: CarouselProps) {
  const [current, setCurrent] = useState(0)
  const [isPaused, setIsPaused] = useState(false)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const total = images.length

  const goTo = useCallback(
    (index: number) => {
      setCurrent((index + total) % total)
    },
    [total]
  )

  const next = useCallback(() => goTo(current + 1), [current, goTo])
  const prev = useCallback(() => goTo(current - 1), [current, goTo])

  useEffect(() => {
    if (isPaused || total <= 1) {
      if (intervalRef.current) clearInterval(intervalRef.current)
      return
    }
    intervalRef.current = setInterval(next, 5000)
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [isPaused, next, total])

  if (total === 0) return null

  return (
    <div
      className={cn(
        "group relative w-full overflow-hidden rounded-lg bg-muted",
        className
      )}
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
    >
      {/* Track */}
      <div
        className="flex transition-transform duration-500 ease-in-out"
        style={{ transform: `translateX(-${current * 100}%)` }}
      >
        {images.map((src, i) => (
          <div
            key={src}
            className="relative aspect-video w-full shrink-0"
          >
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img
              src={src}
              alt={`${alt} - slide ${i + 1} of ${total}`}
              className="absolute inset-0 h-full w-full object-cover"
              loading={i === 0 ? "eager" : "lazy"}
            />
          </div>
        ))}
      </div>

      {/* Navigation arrows */}
      {total > 1 && (
        <>
          <button
            onClick={prev}
            aria-label="Previous slide"
            className={cn(
              "absolute left-3 top-1/2 z-10 flex size-9 -translate-y-1/2 items-center justify-center rounded-full",
              "bg-background/80 text-foreground backdrop-blur-sm transition-colors hover:bg-background",
              "opacity-0 group-hover:opacity-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring",
              "max-sm:left-1 max-sm:size-7"
            )}
          >
            <ChevronLeft className="size-5" />
          </button>
          <button
            onClick={next}
            aria-label="Next slide"
            className={cn(
              "absolute right-3 top-1/2 z-10 flex size-9 -translate-y-1/2 items-center justify-center rounded-full",
              "bg-background/80 text-foreground backdrop-blur-sm transition-colors hover:bg-background",
              "opacity-0 group-hover:opacity-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-ring",
              "max-sm:right-1 max-sm:size-7"
            )}
          >
            <ChevronRight className="size-5" />
          </button>
        </>
      )}

      {/* Dot indicators */}
      {total > 1 && (
        <div className="absolute bottom-3 left-1/2 z-10 flex -translate-x-1/2 gap-1.5 max-sm:bottom-2">
          {images.map((_, i) => (
            <button
              key={i}
              onClick={() => goTo(i)}
              aria-label={`Go to slide ${i + 1}`}
              className={cn(
                "size-2 rounded-full transition-all",
                i === current
                  ? "bg-primary w-4"
                  : "bg-primary/40 hover:bg-primary/60"
              )}
            />
          ))}
        </div>
      )}
    </div>
  )
}
