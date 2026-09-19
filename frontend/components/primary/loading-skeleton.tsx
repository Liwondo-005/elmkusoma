"use client"

import { cn } from "@/lib/utils"

interface LoadingSkeletonProps {
  type?: "text" | "card" | "avatar" | "button"
  size?: "sm" | "md" | "lg"
  className?: string
  count?: number
}

export function LoadingSkeleton({
  type = "text",
  size = "md",
  className,
  count = 1,
}: LoadingSkeletonProps) {
  const sizeClasses = {
    sm: { text: "h-3 w-24", avatar: "size-8", button: "h-8 w-20", card: "h-24 w-full" },
    md: { text: "h-4 w-48", avatar: "size-12", button: "h-10 w-28", card: "h-32 w-full" },
    lg: { text: "h-5 w-64", avatar: "size-16", button: "h-12 w-36", card: "h-40 w-full" },
  }

  return (
    <div className="space-y-3" role="status" aria-label="Loading">
      {Array.from({ length: count }).map((_, i) => (
        <div
          key={i}
          className={cn(
            "animate-pulse rounded-lg bg-muted",
            sizeClasses[size][type],
            className
          )}
        />
      ))}
      <span className="sr-only">Loading...</span>
    </div>
  )
}
