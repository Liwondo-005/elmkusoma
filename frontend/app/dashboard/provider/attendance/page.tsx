"use client"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { nfeApi } from "@/lib/nfe-api"
import { ProviderCreateDialog, type ProviderFormField } from "@/components/provider/provider-create-dialog"
import { Loader2, Plus, Search } from "lucide-react"

export default function AttendancePage() {
  const t = useTranslations("provider")
  const ta = useTranslations("attendance")
  const tt = useTranslations("teacher")
  const [records, setRecords] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [createOpen, setCreateOpen] = useState(false)
  const [sessions, setSessions] = useState<any[]>([])
  const [learners, setLearners] = useState<any[]>([])
  const [optionsLoaded, setOptionsLoaded] = useState(false)
  const [learnerLabels, setLearnerLabels] = useState<Record<string, string>>({})

  useEffect(() => {
    loadAttendance()
    // Rows only carry learnerId — resolve display labels from the learner list.
    nfeApi
      .listLearners()
      .then((l) => {
        if (Array.isArray(l)) {
          setLearnerLabels(
            Object.fromEntries(
              l.map((x: any) => [x.id, x.participantNumber || x.occupation || x.organization || String(x.userId || x.id).slice(0, 8)])
            )
          )
        }
      })
      .catch(() => {})
  }, [])

  async function loadAttendance() {
    try {
      setLoading(true)
      const data = await nfeApi.listAttendance()
      setRecords(data)
    } catch { /* empty */ } finally {
      setLoading(false)
    }
  }

  async function openCreate() {
    setCreateOpen(true)
    if (!optionsLoaded) {
      setOptionsLoaded(true)
      try {
        const [s, l] = await Promise.all([nfeApi.listSessions(), nfeApi.listLearners()])
        setSessions(s || [])
        setLearners(l || [])
      } catch { /* dialog selects stay empty */ }
    }
  }

  const filtered = records.filter((r) =>
    (r.learnerName || r.studentName || learnerLabels[r.learnerId] || "").toLowerCase().includes(search.toLowerCase())
  )

  const createFields: ProviderFormField[] = [
    {
      name: "sessionId",
      label: t("form.session"),
      type: "select",
      required: true,
      options: sessions.map((s) => ({
        value: s.id,
        label: `${s.title || s.id} (${String(s.scheduledAt || "").slice(0, 10)})`,
      })),
    },
    {
      name: "learnerId",
      label: t("form.learner"),
      type: "select",
      required: true,
      options: learners.map((l) => ({
        value: l.id,
        label: l.participantNumber || l.occupation || l.organization || String(l.userId || l.id).slice(0, 8),
      })),
    },
    {
      name: "status",
      label: t("form.status"),
      type: "select",
      required: true,
      options: ["PRESENT", "ABSENT", "LATE", "EXCUSED"].map((v) => ({ value: v, label: t(`form.attendanceStatus.${v}`) })),
    },
    { name: "remarks", label: t("form.remarks"), type: "text" },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{ta("title")}</h1>
          <p className="text-muted-foreground">{t("attendance.subtitle")}</p>
        </div>
        <button
          onClick={openCreate}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" /> {tt("attendance.markTab")}
        </button>
      </div>
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Search className="size-4 text-muted-foreground" />
            <input
              placeholder={t("attendance.searchPlaceholder")}
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
            <p className="py-4 text-center text-sm text-muted-foreground">{t("attendance.empty")}</p>
          ) : (
            <div className="space-y-3">
              {filtered.map((record) => (
                <div key={record.id} className="flex items-center justify-between rounded-lg border border-border p-4">
                  <div>
                    <p className="text-sm font-medium text-foreground">{record.learnerName || record.studentName || learnerLabels[record.learnerId] || String(record.learnerId || "").slice(0, 8)}</p>
                    <p className="text-xs text-muted-foreground">{record.date || record.attendanceDate || String(record.createdAt || "").slice(0, 10)}</p>
                  </div>
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${record.status === "PRESENT" ? "bg-green-100 text-green-700" : record.status === "ABSENT" ? "bg-red-100 text-red-700" : "bg-yellow-100 text-yellow-700"}`}>
                    {record.status || t("attendance.unknownFallback")}
                  </span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <ProviderCreateDialog
        open={createOpen}
        title={tt("attendance.markTab")}
        fields={createFields}
        onClose={() => setCreateOpen(false)}
        onSubmit={async (v) => {
          await nfeApi.markAttendance({
            sessionId: v.sessionId,
            learnerId: v.learnerId,
            status: v.status,
            remarks: v.remarks,
          })
          setCreateOpen(false)
          loadAttendance()
        }}
      />
    </div>
  )
}
