"use client"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { nfeApi } from "@/lib/nfe-api"
import { ProviderCreateDialog, type ProviderFormField } from "@/components/provider/provider-create-dialog"
import { useToast } from "@/components/toast"
import { Loader2, Plus, Search } from "lucide-react"

export default function AssessmentsPage() {
  const t = useTranslations("provider")
  const ta = useTranslations("assessments")
  const tt = useTranslations("teacher")
  const { toast } = useToast()
  const [assessments, setAssessments] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [createOpen, setCreateOpen] = useState(false)
  const [editRow, setEditRow] = useState<any | null>(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  useEffect(() => {
    loadAssessments()
  }, [])

  async function loadAssessments() {
    try {
      setLoading(true)
      const data = await nfeApi.listAssessments()
      setAssessments(data)
    } catch { /* empty */ } finally {
      setLoading(false)
    }
  }

  async function handleDelete(row: any) {
    setConfirmDeleteId(null)
    try {
      await nfeApi.deleteAssessment(row.id)
      toast(t("form.deleteSuccess"), "success")
      if (editRow?.id === row.id) setEditRow(null)
      loadAssessments()
    } catch (e) {
      toast(e instanceof Error && e.message ? e.message : t("form.submitError"), "error")
    }
  }

  const filtered = assessments.filter((a) =>
    (a.title || a.name || "").toLowerCase().includes(search.toLowerCase())
  )

  const createFields: ProviderFormField[] = [
    { name: "title", label: t("form.title"), type: "text", required: true },
    {
      name: "assessmentType",
      label: t("form.type"),
      type: "select",
      required: true,
      options: ["QUIZ", "EXAM", "SURVEY", "FEEDBACK"].map((v) => ({ value: v, label: t(`form.assessmentTypes.${v}`) })),
    },
    { name: "description", label: t("form.description"), type: "textarea" },
    { name: "totalMarks", label: t("form.totalMarks"), type: "number" },
    { name: "passMarks", label: t("form.passMarks"), type: "number" },
    { name: "timeLimitMinutes", label: t("form.timeLimit"), type: "number" },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{ta("title")}</h1>
          <p className="text-muted-foreground">{t("assessments.subtitle")}</p>
        </div>
        <button
          onClick={() => setCreateOpen(true)}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" /> {tt("assessments.createAssessment")}
        </button>
      </div>
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Search className="size-4 text-muted-foreground" />
            <input
              placeholder={t("assessments.searchPlaceholder")}
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
            <p className="py-4 text-center text-sm text-muted-foreground">{t("assessments.empty")}</p>
          ) : (
            <div className="space-y-3">
              {filtered.map((assessment) => (
                <div key={assessment.id} className="flex items-center justify-between rounded-lg border border-border p-4">
                  <div>
                    <p className="text-sm font-medium text-foreground">{assessment.title || assessment.name}</p>
                    <p className="text-xs text-muted-foreground">{assessment.totalMarks || 0} {t("assessments.marksSuffix")}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-700">{assessment.isPublished ? t("form.published") : t("assessments.draftFallback")}</span>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => { setConfirmDeleteId(null); setEditRow(assessment) }}
                        className="rounded px-2 py-1 text-xs font-medium text-blue-600 hover:bg-blue-50"
                      >
                        {t("form.edit")}
                      </button>
                      <button
                        onClick={() => { if (confirmDeleteId === assessment.id) handleDelete(assessment); else setConfirmDeleteId(assessment.id) }}
                        className={`rounded px-2 py-1 text-xs font-medium transition-colors ${confirmDeleteId === assessment.id ? "bg-red-600 text-white hover:bg-red-700" : "text-red-600 hover:bg-red-50"}`}
                      >
                        {confirmDeleteId === assessment.id ? t("form.confirmDelete") : t("form.delete")}
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
        title={editRow ? t("form.editTitle", { name: editRow.title || "" }) : tt("assessments.createAssessment")}
        submitLabel={editRow ? t("form.save") : undefined}
        fields={createFields}
        initialValues={editRow || undefined}
        onClose={() => { setCreateOpen(false); setEditRow(null) }}
        onSubmit={async (v) => {
          if (editRow) {
            await nfeApi.updateAssessment(editRow.id, v)
            toast(t("form.updateSuccess"), "success")
            setEditRow(null)
          } else {
            await nfeApi.createAssessment({
              title: v.title,
              assessmentType: v.assessmentType,
              description: v.description,
              totalMarks: v.totalMarks,
              passMarks: v.passMarks,
              timeLimitMinutes: v.timeLimitMinutes,
            })
            toast(t("form.createSuccess"), "success")
            setCreateOpen(false)
          }
          loadAssessments()
        }}
      />
    </div>
  )
}
