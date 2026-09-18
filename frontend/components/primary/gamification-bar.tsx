"use client"

import { Flame, Star, Award } from "lucide-react"
import { cn } from "@/lib/utils"

interface GamificationBarProps {
  streak: number
  points: number
  badgeCount: number
  loading: boolean
}

export function GamificationBar({ streak, points, badgeCount, loading }: GamificationBarProps) {
  if (loading) {
    return (
      <div className="flex items-center gap-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="h-8 w-20 animate-pulse rounded-full bg-muted" />
        ))}
      </div>
    )
  }

  const items = [
    { icon: Flame, label: `${streak}`, color: "text-orange-500", bg: "bg-orange-500/10" },
    { icon: Star, label: `${points.toLocaleString()}`, color: "text-yellow-500", bg: "bg-yellow-500/10" },
    { icon: Award, label: `${badgeCount}`, color: "text-purple-500", bg: "bg-purple-500/10" },
  ]

  return (
    <div className="flex items-center gap-2">
      {items.map((item, i) => (
        <div
          key={i}
          className={cn(
            "flex items-center gap-1.5 rounded-full px-3 py-1.5",
            item.bg
          )}
        >
          <item.icon className={cn("size-3.5", item.color)} />
          <span className="text-xs font-bold text-foreground">{item.label}</span>
        </div>
      ))}
    </div>
  )
}
