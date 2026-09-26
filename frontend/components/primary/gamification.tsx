"use client"

import { useTranslations } from "next-intl"
import {
  Award,
  Flame,
  Star,
  Trophy,
  Sparkles,
  BookOpen,
  Target,
  Zap,
  Heart,
  type LucideIcon,
} from "lucide-react"
import type { StudentBadge, StreakInfo } from "@/lib/api"
import { cn } from "@/lib/utils"

interface BadgeGridProps {
  badges: StudentBadge[]
  loading: boolean
}

export function BadgeGrid({ badges, loading }: BadgeGridProps) {
  const t = useTranslations("ui")
  if (loading) {
    return (
      <div className="space-y-4">
        <div className="flex items-center gap-2">
          <div className="size-5 animate-pulse rounded bg-muted" />
          <div className="h-4 w-32 animate-pulse rounded bg-muted" />
        </div>
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 lg:grid-cols-6">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="aspect-square animate-pulse rounded-2xl bg-muted" />
          ))}
        </div>
      </div>
    )
  }

  const earned = badges.length

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="flex items-center gap-2 text-sm font-semibold text-foreground">
          <Award className="size-4 text-yellow-500" />
          {t("gamification.badges")}
        </h3>
        <span className="text-xs font-medium text-muted-foreground">
          {t("gamification.earnedCount", { count: earned })}
        </span>
      </div>

      {earned === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-8 text-center">
          <Trophy className="mx-auto size-8 text-muted-foreground/40" />
          <p className="mt-2 text-sm text-muted-foreground">
            {t("gamification.emptyBadges")}
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-3 gap-3 sm:grid-cols-4 lg:grid-cols-6">
          {badges.map((badge) => (
            <div
              key={badge.id}
              className="flex flex-col items-center rounded-2xl border border-border bg-card p-3 transition-all hover:shadow-md"
            >
              <div className="flex size-12 items-center justify-center rounded-xl bg-gradient-to-br from-yellow-100 to-orange-100">
                <Trophy className="size-6 text-yellow-600" />
              </div>
              <span className="mt-2 text-xs font-semibold text-foreground text-center line-clamp-2">
                {badge.badgeName}
              </span>
              <span className="mt-0.5 text-[10px] font-medium text-muted-foreground">
                {badge.points} pts
              </span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

interface StreakDisplayProps {
  streak: StreakInfo | null
  loading: boolean
}

export function StreakDisplay({ streak, loading }: StreakDisplayProps) {
  const t = useTranslations("ui")
  if (loading) {
    return (
      <div className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4">
        <div className="size-10 animate-pulse rounded-xl bg-muted" />
        <div className="space-y-1.5">
          <div className="h-4 w-24 animate-pulse rounded bg-muted" />
          <div className="h-3 w-16 animate-pulse rounded bg-muted" />
        </div>
      </div>
    )
  }

  const current = streak?.currentStreak || 0
  const total = streak?.totalPoints || 0

  return (
    <div className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4">
      <div className="flex size-12 items-center justify-center rounded-xl bg-gradient-to-br from-orange-100 to-red-100">
        <Flame className="size-6 text-orange-500" />
      </div>
      <div>
        <div className="flex items-baseline gap-1.5">
          <span className="text-2xl font-bold text-foreground">{current}</span>
          <span className="text-sm font-medium text-muted-foreground">{t("gamification.dayStreak")}</span>
        </div>
        <div className="flex items-center gap-1 text-xs text-muted-foreground">
          <Star className="size-3 text-yellow-500" />
          {t("gamification.totalPoints", { count: total })}
        </div>
      </div>
    </div>
  )
}

interface StarRatingProps {
  score: number
  size?: "sm" | "md" | "lg"
}

export function StarRating({ score, size = "md" }: StarRatingProps) {
  const clamped = Math.max(0, Math.min(100, score))
  const filledStars = Math.round((clamped / 100) * 5)

  const sizeClasses = {
    sm: "size-4",
    md: "size-5",
    lg: "size-7",
  }

  const colorClass =
    clamped >= 90
      ? "text-green-500"
      : clamped >= 70
        ? "text-yellow-500"
        : "text-red-500"

  return (
    <div className="flex items-center gap-0.5">
      {Array.from({ length: 5 }).map((_, i) => (
        <Star
          key={i}
          className={cn(sizeClasses[size], i < filledStars ? colorClass : "text-gray-200", i < filledStars && "fill-current")}
        />
      ))}
    </div>
  )
}

interface ProgressRingProps {
  value: number
  size?: number
  label?: string
  color?: string
}

export function ProgressRing({ value, size = 80, label, color }: ProgressRingProps) {
  const clamped = Math.max(0, Math.min(100, value))
  const strokeWidth = 6
  const radius = (size - strokeWidth) / 2
  const circumference = 2 * Math.PI * radius
  const offset = circumference - (clamped / 100) * circumference

  const resolvedColor = color || (clamped >= 90 ? "#22c55e" : clamped >= 70 ? "#eab308" : "#ef4444")

  return (
    <div className="flex flex-col items-center gap-1">
      <div className="relative" style={{ width: size, height: size }}>
      <svg width={size} height={size} className="-rotate-90">
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke="currentColor"
          strokeWidth={strokeWidth}
          className="text-muted/30"
          fill="none"
        />
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          stroke={resolvedColor}
          strokeWidth={strokeWidth}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
          strokeLinecap="round"
          fill="none"
          className="transition-all duration-700 ease-out"
        />
      </svg>
      <div className="absolute inset-0 flex items-center justify-center">
        <span className="text-sm font-bold text-foreground">{Math.round(clamped)}%</span>
      </div>
      </div>
      {label && (
        <span className="mt-1 text-xs font-medium text-muted-foreground">{label}</span>
      )}
    </div>
  )
}

interface EncouragingMessageProps {
  type: "completion" | "attempt" | "streak" | "badge"
}

const MESSAGE_KEYS: Record<string, string[]> = {
  completion: [
    "gamificationMessages.completion.m0",
    "gamificationMessages.completion.m1",
    "gamificationMessages.completion.m2",
    "gamificationMessages.completion.m3",
    "gamificationMessages.completion.m4",
  ],
  attempt: [
    "gamificationMessages.attempt.m0",
    "gamificationMessages.attempt.m1",
    "gamificationMessages.attempt.m2",
    "gamificationMessages.attempt.m3",
    "gamificationMessages.attempt.m4",
  ],
  streak: [
    "gamificationMessages.streak.m0",
    "gamificationMessages.streak.m1",
    "gamificationMessages.streak.m2",
    "gamificationMessages.streak.m3",
    "gamificationMessages.streak.m4",
  ],
  badge: [
    "gamificationMessages.badge.m0",
    "gamificationMessages.badge.m1",
    "gamificationMessages.badge.m2",
    "gamificationMessages.badge.m3",
    "gamificationMessages.badge.m4",
  ],
}

export function EncouragingMessage({ type }: EncouragingMessageProps) {
  const t = useTranslations("ui")
  const keys = MESSAGE_KEYS[type] || MESSAGE_KEYS.attempt
  const msg = t(keys[Math.floor(Math.random() * keys.length)])

  return (
    <span className="text-sm font-medium text-foreground">{msg}</span>
  )
}

interface EmptyStateProps {
  icon: LucideIcon
  title: string
  description: string
  action?: {
    label: string
    href: string
  }
}

export function EmptyState({ icon: Icon, title, description, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center rounded-2xl border border-dashed border-border py-12 text-center">
      <div className="flex size-16 items-center justify-center rounded-2xl bg-muted">
        <Icon className="size-8 text-muted-foreground/50" />
      </div>
      <h3 className="mt-4 text-lg font-semibold text-foreground">{title}</h3>
      <p className="mt-2 max-w-sm text-sm text-muted-foreground">{description}</p>
      {action && (
        <a
          href={action.href}
          className="mt-4 inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
        >
          {action.label}
        </a>
      )}
    </div>
  )
}
