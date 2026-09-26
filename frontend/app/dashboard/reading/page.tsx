"use client"

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { primaryApi, type ReadingAdventure } from "@/lib/api"
import { type LearningLevel, primarySubjects } from "@/lib/learner-config"
import { BookOpen, Heart, Clock, Eye, Filter, Star, ArrowLeft } from "lucide-react"

const levelColors: Record<string, string> = {
  Beginner: "bg-green-100 text-green-700",
  Intermediate: "bg-amber-100 text-amber-700",
  Advanced: "bg-red-100 text-red-700",
}

export default function ReadingPage() {
  const { user } = useRequireAuth()
  const t = useTranslations("primary")
  const ts = useTranslations("status")
  const [adventures, setAdventures] = useState<ReadingAdventure[]>([])
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState<"all" | "favorites" | string>("all")
  const [selectedAdventure, setSelectedAdventure] = useState<ReadingAdventure | null>(null)
  const level = user?.learningLevel as LearningLevel | null
  const isPrimary = level?.toUpperCase() === "PRIMARY"

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await primaryApi.getReadingAdventures().catch(() => [])
      setAdventures(data)
    } catch {
      setAdventures([])
    } finally {
      setLoading(false)
    }
  }

  async function handleToggleFavorite(id: string) {
    try {
      const updated = await primaryApi.toggleFavoriteReading(id)
      setAdventures((prev) => prev.map((a) => (a.id === id ? updated : a)))
      if (selectedAdventure?.id === id) setSelectedAdventure(updated)
    } catch {
      // handle silently
    }
  }

  async function handleMarkComplete(id: string) {
    try {
      const updated = await primaryApi.markReadingComplete(id)
      setAdventures((prev) => prev.map((a) => (a.id === id ? updated : a)))
      if (selectedAdventure?.id === id) setSelectedAdventure(updated)
    } catch {
      // handle silently
    }
  }

  const totalRead = adventures.reduce((sum, a) => sum + a.timesRead, 0)
  const totalTime = adventures.reduce((sum, a) => sum + a.readTimeMinutes * a.timesRead, 0)
  const favoriteSubject = adventures.reduce((acc, a) => {
    if (a.subjectName) acc[a.subjectName] = (acc[a.subjectName] || 0) + 1
    return acc
  }, {} as Record<string, number>)
  const topSubject = Object.entries(favoriteSubject).sort((a, b) => b[1] - a[1])[0]?.[0] || "-"

  const filtered = adventures.filter((a) => {
    if (filter === "favorites") return a.isFavorite
    if (filter !== "all") return a.subjectName === filter
    return true
  })

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
      </div>
    )
  }

  if (!isPrimary) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-2xl bg-primary/10">
            <BookOpen className="size-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("reading.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("reading.subtitle")}</p>
          </div>
        </div>
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <BookOpen className="size-12 text-muted-foreground/30" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("reading.primaryOnlyTitle")}</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            {t("reading.primaryOnlyDesc")}
          </p>
        </div>
      </div>
    )
  }

  if (selectedAdventure) {
    return (
      <div className="mx-auto max-w-4xl space-y-6">
        <button
          onClick={() => setSelectedAdventure(null)}
          className="inline-flex items-center gap-2 text-sm font-medium text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="size-4" />
          {t("reading.backToAdventures")}
        </button>

        <div className="rounded-2xl border border-border bg-card p-8 shadow-xs">
          <div className="flex items-start justify-between">
            <div>
              <h1 className="text-2xl font-bold text-foreground">{selectedAdventure.title}</h1>
              <div className="mt-2 flex items-center gap-3">
                {selectedAdventure.subjectName && (
                  <span className="rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
                    {selectedAdventure.subjectName}
                  </span>
                )}
                <span className={`rounded-full px-3 py-1 text-xs font-medium ${levelColors[selectedAdventure.readingLevel] || "bg-muted text-muted-foreground"}`}>
                  {selectedAdventure.readingLevel}
                </span>
                <span className="text-xs text-muted-foreground">
                  {t("reading.metaLine", { words: selectedAdventure.wordCount, mins: selectedAdventure.readTimeMinutes })}
                </span>
              </div>
            </div>
            <button
              onClick={() => handleToggleFavorite(selectedAdventure.id)}
              className="text-muted-foreground hover:text-red-500 transition-colors"
            >
              <Heart className={`size-6 ${selectedAdventure.isFavorite ? "fill-red-500 text-red-500" : ""}`} />
            </button>
          </div>

          <div className="mt-6 rounded-xl bg-muted/30 p-6">
            <div className="prose prose-sm max-w-none text-foreground">
              {selectedAdventure.content.split("\n").map((paragraph, i) => (
                <p key={i} className="mb-4 text-base leading-relaxed">{paragraph}</p>
              ))}
            </div>
          </div>

          <div className="mt-6 flex items-center gap-4">
            <button
              onClick={() => handleMarkComplete(selectedAdventure.id)}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
            >
              <BookOpen className="size-4" />
              {t("reading.finishedButton")}
            </button>
            <span className="text-sm text-muted-foreground">
              {t("reading.timesRead", { count: selectedAdventure.timesRead })}
            </span>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-blue-500/5 via-card to-primary/5 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-blue-500/10">
            <BookOpen className="size-6 text-blue-500" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("reading.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("reading.subtitle")}</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-blue-50">
              <BookOpen className="size-5 text-blue-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{totalRead}</p>
              <p className="text-xs text-muted-foreground">{t("reading.storiesRead")}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-amber-50">
              <Clock className="size-5 text-amber-600" />
            </div>
            <div>
              <p className="text-2xl font-extrabold text-foreground">{totalTime}</p>
              <p className="text-xs text-muted-foreground">{t("reading.minutesReading")}</p>
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-xl bg-green-50">
              <Star className="size-5 text-green-600" />
            </div>
            <div>
              <p className="text-lg font-extrabold text-foreground truncate">{topSubject}</p>
              <p className="text-xs text-muted-foreground">{t("reading.favoriteSubject")}</p>
            </div>
          </div>
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-2">
        <Filter className="size-4 text-muted-foreground" />
        <button
          onClick={() => setFilter("all")}
          className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors ${
            filter === "all"
              ? "bg-primary text-primary-foreground"
              : "bg-muted text-muted-foreground hover:bg-muted/80"
          }`}
        >
          {t("reading.filterAll")}
        </button>
        <button
          onClick={() => setFilter("favorites")}
          className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors ${
            filter === "favorites"
              ? "bg-primary text-primary-foreground"
              : "bg-muted text-muted-foreground hover:bg-muted/80"
          }`}
        >
          {t("reading.filterFavorites")}
        </button>
        {primarySubjects.map((s) => (
          <button
            key={s.name}
            onClick={() => setFilter(filter === s.name ? "all" : s.name)}
            className={`rounded-full px-3 py-1.5 text-xs font-medium transition-colors ${
              filter === s.name
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:bg-muted/80"
            }`}
          >
            {s.name}
          </button>
        ))}
      </div>

      {filtered.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10">
            <BookOpen className="size-8 text-primary" />
          </div>
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("reading.emptyTitle")}</h3>
          <p className="mt-2 max-w-sm text-sm text-muted-foreground">
            {filter === "favorites"
              ? t("reading.emptyFav")
              : t("reading.emptyDefault")}
          </p>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((adventure) => (
            <div
              key={adventure.id}
              className="group rounded-2xl border border-border bg-card shadow-xs overflow-hidden transition-all hover:shadow-md cursor-pointer"
              onClick={() => setSelectedAdventure(adventure)}
            >
              <div
                className="h-3 w-full"
                style={{ backgroundColor: adventure.coverColor || "#6366f1" }}
              />
              <div className="p-5">
                <div className="flex items-start justify-between">
                  <h3 className="text-sm font-bold text-foreground line-clamp-1">{adventure.title}</h3>
                  <button
                    onClick={(e) => {
                      e.stopPropagation()
                      handleToggleFavorite(adventure.id)
                    }}
                    className="shrink-0 text-muted-foreground hover:text-red-500 transition-colors"
                  >
                    <Heart className={`size-4 ${adventure.isFavorite ? "fill-red-500 text-red-500" : ""}`} />
                  </button>
                </div>

                <div className="mt-2 flex items-center gap-2">
                  {adventure.subjectName && (
                    <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-medium text-primary">
                      {adventure.subjectName}
                    </span>
                  )}
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-medium ${levelColors[adventure.readingLevel] || "bg-muted text-muted-foreground"}`}>
                    {adventure.readingLevel}
                  </span>
                </div>

                <div className="mt-3 flex items-center gap-3 text-[10px] text-muted-foreground">
                  <span className="flex items-center gap-1">
                    <BookOpen className="size-3" /> {t("reading.wordsCount", { count: adventure.wordCount })}
                  </span>
                  <span className="flex items-center gap-1">
                    <Clock className="size-3" /> {t("reading.minsCount", { count: adventure.readTimeMinutes })}
                  </span>
                  <span className="flex items-center gap-1">
                    <Eye className="size-3" /> {adventure.timesRead}x
                  </span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
