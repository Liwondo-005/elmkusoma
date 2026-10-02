"use client"

import { useEffect, useState, useCallback } from "react"
import { useTranslations } from "next-intl"
import { Loader2, AlertCircle, CheckCircle2, XCircle, MessageSquareWarning, ShieldCheck, KeyRound } from "lucide-react"
import { platformAdminApi, type DelegatedTask } from "@/lib/platform-admin-api"

export default function DelegatedWorkPage() {
  const t = useTranslations("platformAdmin")
  const tc = useTranslations("common")
  const [tasks, setTasks] = useState<DelegatedTask[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [flash, setFlash] = useState<string | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [notes, setNotes] = useState("")

  const load = useCallback(() => {
    setLoading(true); setError(null)
    platformAdminApi.delegatedTasks()
      .then(setTasks)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { load() }, [load])

  const decide = async (task: DelegatedTask, status: string) => {
    if (status !== "APPROVED" && !notes.trim()) {
      setError(t("delegatedWork.notesRequired")); return
    }
    setBusy(task.verificationId); setError(null); setFlash(null)
    try {
      await platformAdminApi.reviewProviderVerification(task.verificationId, status, notes || undefined)
      setFlash(t("delegatedWork.verificationFlash", { status: status.toLowerCase(), authority: task.authority }))
      setNotes("")
      setTasks((prev) => prev.filter((t) => t.verificationId !== task.verificationId))
    } catch (e: any) {
      setError(e.message)
    } finally { setBusy(null) }
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="flex items-center gap-2 text-2xl font-bold tracking-tight text-foreground">
          <KeyRound className="size-6 text-primary" /> {t("delegatedWork.title")}
        </h1>
        <p className="mt-1 text-sm text-muted-foreground">
          {t("delegatedWork.subtitle")}
        </p>
      </div>

      {flash && <div className="rounded-2xl border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-700">{flash}</div>}
      {error && <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700 flex items-center gap-2"><AlertCircle className="size-4 shrink-0" />{error}</div>}

      {loading ? (
        <div className="flex justify-center py-20"><Loader2 className="size-8 animate-spin text-primary" /></div>
      ) : tasks.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-10 text-center">
          <ShieldCheck className="mx-auto size-10 text-green-500/50" />
          <p className="mt-3 text-sm font-medium text-foreground">{t("delegatedWork.emptyTitle")}</p>
          <p className="mt-1 text-xs text-muted-foreground">{t("delegatedWork.emptyDesc")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          <input value={notes} onChange={(e) => setNotes(e.target.value)} placeholder={t("delegatedWork.notesPlaceholder")} className="w-full rounded-xl border border-border bg-card px-4 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/20" />
          {tasks.map((task) => (
            <div key={`${task.delegationId}-${task.verificationId}`} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <div className="flex flex-wrap items-center gap-2">
                    <p className="text-sm font-semibold text-foreground">{task.entityName ?? task.entityId.slice(0, 8)}</p>
                    <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium">{task.verificationType}</span>
                    <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">{task.authority}</span>
                    <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium">{task.scope}</span>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {task.entityType} · {t("delegatedWork.submittedLabel")} {task.submittedAt ? new Date(task.submittedAt).toLocaleString("en-GB") : "—"}
                  </p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <button onClick={() => decide(task, "APPROVED")} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-xl bg-green-600 px-3 py-1.5 text-xs font-medium text-white hover:bg-green-700 disabled:opacity-50">
                    {busy === task.verificationId ? <Loader2 className="size-3 animate-spin" /> : <CheckCircle2 className="size-3" />} {tc("verifyButton")}</button>
                  <button onClick={() => decide(task, "CHANGES_REQUIRED")} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-xl border border-orange-300 px-3 py-1.5 text-xs font-medium text-orange-700 hover:bg-orange-50 disabled:opacity-50">
                    <MessageSquareWarning className="size-3" /> {t("delegatedWork.requestChanges")}</button>
                  <button onClick={() => decide(task, "REJECTED")} disabled={busy !== null} className="inline-flex items-center gap-1 rounded-xl border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50 disabled:opacity-50">
                    <XCircle className="size-3" /> {t("verifications.reject")}</button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
