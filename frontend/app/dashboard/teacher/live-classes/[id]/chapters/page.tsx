"use client"

import { useCallback, useEffect, useMemo, useState } from "react"
import { useParams } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { appFetch } from "@/lib/fetch"
import { Button } from "@/components/ui/button"
import { announce } from "@/lib/announce"
import { ArrowLeft, ListOrdered, Loader2, Plus, Trash2 } from "lucide-react"

interface Replay {
  id: string
  liveSessionId?: string | null
  title?: string | null
  durationSeconds?: number | null
}

interface Chapter {
  id: string
  title: string
  positionSeconds: number
}

/**
 * Accepts the timecodes a teacher would type by hand - "90", "1:30", "1:02:03" - and
 * returns whole seconds, or null when the input is not a usable position.
 */
function parseTimecode(raw: string): number | null {
  const input = raw.trim()
  if (!input) return null
  if (!/^\d+(:\d{1,2}){0,2}$/.test(input)) return null

  const parts = input.split(":").map((p) => Number(p))
  // "1:75" would be a real 2m15s moment that the teacher probably did not mean.
  if (parts.some((p) => !Number.isInteger(p))) return null
  if (parts.length === 2 && parts[1] >= 60) return null
  if (parts.length === 3 && (parts[1] >= 60 || parts[2] >= 60)) return null

  const [h, m, s] = parts.length === 3
    ? parts
    : parts.length === 2
      ? [0, parts[0], parts[1]]
      : [0, 0, parts[0]]
  return h * 3600 + m * 60 + s
}

function formatTimecode(seconds: number) {
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = Math.floor(seconds % 60)
  const pad = (n: number) => n.toString().padStart(2, "0")
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`
}

export default function ReplayChaptersPage() {
  const params = useParams()
  const liveClassId = params.id as string
  const t = useTranslations("teacher")
  const tc = useTranslations("common")

  const [replay, setReplay] = useState<Replay | null>(null)
  const [chapters, setChapters] = useState<Chapter[]>([])
  const [loading, setLoading] = useState(true)
  const [resolved, setResolved] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [title, setTitle] = useState("")
  const [position, setPosition] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      // The recording is the replay whose source session is this live class; the list
      // endpoint only returns playable ones, so a class without a published recording
      // simply comes back empty rather than needing a separate status call.
      const replays = await appFetch<Replay[]>("/v1/replays")
      const found = replays.find((r) => r.liveSessionId === liveClassId) ?? null
      setReplay(found)
      setResolved(true)
      if (found) {
        const list = await appFetch<Chapter[]>(`/v1/replays/${found.id}/chapters`)
        setChapters(Array.isArray(list) ? list : [])
      } else {
        setChapters([])
      }
    } catch (e: any) {
      setError(e.message || tc("error.load"))
    } finally {
      setLoading(false)
    }
  }, [liveClassId, tc])

  useEffect(() => {
    load()
  }, [load])

  const durationSeconds = replay?.durationSeconds ?? null
  const durationLabel = useMemo(
    () => (durationSeconds && durationSeconds > 0 ? formatTimecode(durationSeconds) : null),
    [durationSeconds]
  )

  /** Client-side mirror of the server's rules, so the teacher is told before a round trip. */
  function validate(): number | null {
    const seconds = parseTimecode(position)
    if (seconds === null) {
      setError(t("chapters.invalidPosition"))
      return null
    }
    if (durationSeconds != null && durationSeconds > 0 && seconds > durationSeconds) {
      setError(t("chapters.pastEnd", { duration: formatTimecode(durationSeconds) }))
      return null
    }
    if (chapters.some((c) => c.positionSeconds === seconds)) {
      setError(t("chapters.duplicate", { time: formatTimecode(seconds) }))
      return null
    }
    return seconds
  }

  async function handleAdd() {
    if (!replay || submitting) return
    const seconds = validate()
    if (seconds === null) return

    setSubmitting(true)
    setError(null)
    try {
      const created = await appFetch<Chapter>(`/v1/replays/${replay.id}/chapters`, {
        method: "POST",
        body: JSON.stringify({ title: title.trim(), positionSeconds: seconds }),
      })
      setChapters((prev) => [...prev, created].sort((a, b) => a.positionSeconds - b.positionSeconds))
      setTitle("")
      setPosition("")
      announce(t("chapters.added", { title: created.title }))
    } catch (e: any) {
      setError(e.message || tc("error.save"))
    } finally {
      setSubmitting(false)
    }
  }

  async function handleRemove(chapterId: string) {
    if (!replay || deletingId) return
    setDeletingId(chapterId)
    setError(null)
    try {
      await appFetch(`/v1/replays/${replay.id}/chapters/${chapterId}`, { method: "DELETE" })
      setChapters((prev) => prev.filter((c) => c.id !== chapterId))
      announce(t("chapters.removed"))
    } catch (e: any) {
      setError(e.message || tc("error.save"))
    } finally {
      setDeletingId(null)
    }
  }

  if (loading) {
    return (
      <main role="main" aria-label={t("chapters.title")} className="flex items-center justify-center py-16">
        <Loader2 className="size-8 animate-spin text-primary" aria-hidden="true" />
      </main>
    )
  }

  return (
    <main role="main" aria-label={t("chapters.title")} className="mx-auto max-w-2xl space-y-6">
      <Link
        href="/dashboard/teacher/live-classes"
        className="inline-flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" aria-hidden="true" /> {tc("back")}
      </Link>

      <div>
        <h1 className="flex items-center gap-2 text-2xl font-bold">
          <ListOrdered className="size-5 text-primary" aria-hidden="true" />
          {t("chapters.title")}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("chapters.description")}</p>
      </div>

      {error && (
        <div
          role="alert"
          className="rounded-lg border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive"
        >
          {error}
        </div>
      )}

      {!resolved ? (
        <div className="rounded-xl border border-border bg-card p-6 text-sm text-muted-foreground">
          {tc("error.load")}
        </div>
      ) : !replay ? (
        <div className="rounded-xl border border-dashed border-border bg-card p-6 text-sm text-muted-foreground">
          {t("chapters.noRecording")}
        </div>
      ) : (
        <>
          <div className="rounded-xl border border-border bg-card p-4 text-sm">
            <p className="font-medium">{replay.title || t("chapters.recording")}</p>
            {durationLabel && (
              <p className="mt-1 text-xs text-muted-foreground">
                {t("chapters.duration", { duration: durationLabel })}
              </p>
            )}
          </div>

          <section className="rounded-xl border border-border bg-card p-6" aria-label={t("chapters.addSection")}>
            <h2 className="mb-4 text-lg font-semibold">{t("chapters.addSection")}</h2>
            <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
              <div className="flex-1">
                <label htmlFor="chapter-title" className="mb-1 block text-sm font-medium">
                  {t("chapters.titleLabel")}
                </label>
                <input
                  id="chapter-title"
                  type="text"
                  maxLength={200}
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder={t("chapters.titlePlaceholder")}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm"
                />
              </div>
              <div className="sm:w-36">
                <label htmlFor="chapter-position" className="mb-1 block text-sm font-medium">
                  {t("chapters.positionLabel")}
                </label>
                <input
                  id="chapter-position"
                  type="text"
                  inputMode="numeric"
                  value={position}
                  onChange={(e) => setPosition(e.target.value)}
                  placeholder={t("chapters.positionPlaceholder")}
                  aria-describedby="chapter-position-hint"
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 font-mono text-sm"
                />
              </div>
              <Button
                type="button"
                onClick={handleAdd}
                disabled={submitting || !title.trim() || !position.trim()}
              >
                {submitting ? (
                  <Loader2 className="size-4 animate-spin" aria-hidden="true" />
                ) : (
                  <Plus className="size-4" aria-hidden="true" />
                )}
                {t("chapters.add")}
              </Button>
            </div>
            <p id="chapter-position-hint" className="mt-2 text-xs text-muted-foreground">
              {t("chapters.positionHint")}
            </p>
          </section>

          <section className="rounded-xl border border-border bg-card p-6" aria-label={t("chapters.listSection")}>
            <h2 className="mb-4 text-lg font-semibold">{t("chapters.listSection")}</h2>
            {chapters.length === 0 ? (
              <p className="text-sm text-muted-foreground">{t("chapters.empty")}</p>
            ) : (
              <ul className="divide-y divide-border">
                {chapters.map((chapter) => (
                  <li key={chapter.id} className="flex items-center justify-between gap-3 py-2">
                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">{chapter.title}</p>
                      <p className="font-mono text-xs text-muted-foreground">
                        {formatTimecode(chapter.positionSeconds)}
                      </p>
                    </div>
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => handleRemove(chapter.id)}
                      disabled={deletingId === chapter.id}
                      aria-label={`${t("chapters.remove")} ${chapter.title}`}
                    >
                      {deletingId === chapter.id ? (
                        <Loader2 className="size-4 animate-spin" aria-hidden="true" />
                      ) : (
                        <Trash2 className="size-4" aria-hidden="true" />
                      )}
                    </Button>
                  </li>
                ))}
              </ul>
            )}
          </section>
        </>
      )}
    </main>
  )
}
