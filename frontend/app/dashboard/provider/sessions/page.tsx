"use client"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { nfeApi } from "@/lib/nfe-api"
import { ProviderCreateDialog, type ProviderFormField } from "@/components/provider/provider-create-dialog"
import { Loader2, Plus, Search } from "lucide-react"

export default function SessionsPage() {
  const t = useTranslations("provider")
  const ts = useTranslations("status")
  const [sessions, setSessions] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [createOpen, setCreateOpen] = useState(false)

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
                  <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">{session.status || ts("scheduled")}</span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <ProviderCreateDialog
        open={createOpen}
        title={t("sessions.createSession")}
        fields={createFields}
        onClose={() => setCreateOpen(false)}
        onSubmit={async (v) => {
          await nfeApi.createSession({
            title: v.title,
            sessionType: v.sessionType,
            scheduledAt: v.scheduledAt,
            durationMinutes: v.durationMinutes,
            meetingUrl: v.meetingUrl,
            description: v.description,
          })
          setCreateOpen(false)
          loadSessions()
        }}
      />
    </div>
  )
}
