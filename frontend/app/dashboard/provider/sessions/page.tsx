"use client"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { nfeApi } from "@/lib/nfe-api"
import { ProviderCreateDialog, type ProviderFormField } from "@/components/provider/provider-create-dialog"
import { useToast } from "@/components/toast"
import { Loader2, Plus, Search } from "lucide-react"

export default function SessionsPage() {
  const t = useTranslations("provider")
  const ts = useTranslations("status")
  const { toast } = useToast()
  const [sessions, setSessions] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [createOpen, setCreateOpen] = useState(false)
  const [editRow, setEditRow] = useState<any | null>(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  useEffect(() => {
    loadSessions()
  }, [])

  async function loadSessions() {
    try {
      setLoading(true)
      const data = await nfeApi.listSessions()
      setSessions(data)
    } catch { /* empty */ } finally {
      setLoading(false)
    }
  }

  async function handleDelete(row: any) {
    setConfirmDeleteId(null)
    try {
      await nfeApi.deleteSession(row.id)
      toast(t("form.deleteSuccess"), "success")
      if (editRow?.id === row.id) setEditRow(null)
      loadSessions()
    } catch (e) {
      toast(e instanceof Error && e.message ? e.message : t("form.submitError"), "error")
    }
  }

  const filtered = sessions.filter((s) =>
    (s.title || s.name || "").toLowerCase().includes(search.toLowerCase())
  )

  const createFields: ProviderFormField[] = [
    { name: "title", label: t("form.title"), type: "text", required: true },
    {
      name: "sessionType",
      label: t("form.type"),
      type: "select",
      required: true,
      options: ["LIVE", "SEMINAR", "WORKSHOP", "WEBINAR"].map((v) => ({ value: v, label: t(`form.sessionTypes.${v}`) })),
    },
    { name: "scheduledAt", label: t("form.scheduledAt"), type: "datetime-local", required: true },
    { name: "durationMinutes", label: t("form.durationMinutes"), type: "number" },
    { name: "meetingUrl", label: t("form.meetingUrl"), type: "text" },
    { name: "description", label: t("form.description"), type: "textarea" },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{t("sessions.title")}</h1>
          <p className="text-muted-foreground">{t("sessions.subtitle")}</p>
        </div>
        <button
          onClick={() => setCreateOpen(true)}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" /> {t("sessions.createSession")}
        </button>
      </div>
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Search className="size-4 text-muted-foreground" />
            <input
              placeholder={t("sessions.searchPlaceholder")}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
            />
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
          ) : filtered.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">{t("sessions.empty")}</p>
          ) : (
            <div className="space-y-3">
              {filtered.map((session) => (
                <div key={session.id} className="flex items-center justify-between rounded-lg border border-border p-4">
                  <div>
                    <p className="text-sm font-medium text-foreground">{session.title || session.name}</p>
                    <p className="text-xs text-muted-foreground">{session.date || session.scheduledAt || t("sessions.noDateFallback")}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">{session.status || ts("scheduled")}</span>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => { setConfirmDeleteId(null); setEditRow(session) }}
                        className="rounded px-2 py-1 text-xs font-medium text-blue-600 hover:bg-blue-50"
                      >
                        {t("form.edit")}
                      </button>
                      <button
                        onClick={() => { if (confirmDeleteId === session.id) handleDelete(session); else setConfirmDeleteId(session.id) }}
                        className={`rounded px-2 py-1 text-xs font-medium transition-colors ${confirmDeleteId === session.id ? "bg-red-600 text-white hover:bg-red-700" : "text-red-600 hover:bg-red-50"}`}
                      >
                        {confirmDeleteId === session.id ? t("form.confirmDelete") : t("form.delete")}
                      </button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <ProviderCreateDialog
        open={createOpen || editRow !== null}
        title={editRow ? t("form.editTitle", { name: editRow.title || "" }) : t("sessions.createSession")}
        submitLabel={editRow ? t("form.save") : undefined}
        fields={createFields}
        initialValues={editRow || undefined}
        onClose={() => { setCreateOpen(false); setEditRow(null) }}
        onSubmit={async (v) => {
          if (editRow) {
            await nfeApi.updateSession(editRow.id, { ...v, providerId: editRow.providerId })
            toast(t("form.updateSuccess"), "success")
            setEditRow(null)
          } else {
            await nfeApi.createSession({
              title: v.title,
              sessionType: v.sessionType,
              scheduledAt: v.scheduledAt,
              durationMinutes: v.durationMinutes,
              meetingUrl: v.meetingUrl,
              description: v.description,
            })
            toast(t("form.createSuccess"), "success")
            setCreateOpen(false)
          }
          loadSessions()
        }}
      />
    </div>
  )
}
