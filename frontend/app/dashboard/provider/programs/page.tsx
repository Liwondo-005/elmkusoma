"use client"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { nfeApi } from "@/lib/nfe-api"
import { ProviderCreateDialog, type ProviderFormField } from "@/components/provider/provider-create-dialog"
import { useToast } from "@/components/toast"
import { Loader2, Plus, Search } from "lucide-react"

export default function ProgramsPage() {
  const t = useTranslations("provider")
  const ts = useTranslations("status")
  const { toast } = useToast()
  const [programs, setPrograms] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [createOpen, setCreateOpen] = useState(false)
  const [editRow, setEditRow] = useState<any | null>(null)
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null)

  useEffect(() => {
    loadPrograms()
  }, [])

  async function loadPrograms() {
    try {
      setLoading(true)
      const data = await nfeApi.listPrograms()
      setPrograms(data)
    } catch { /* empty */ } finally {
      setLoading(false)
    }
  }

  async function handleDelete(row: any) {
    setConfirmDeleteId(null)
    try {
      await nfeApi.deleteProgram(row.id)
      toast(t("form.deleteSuccess"), "success")
      if (editRow?.id === row.id) setEditRow(null)
      loadPrograms()
    } catch (e) {
      toast(e instanceof Error && e.message ? e.message : t("form.submitError"), "error")
    }
  }

  const filtered = programs.filter((p) =>
    p.name?.toLowerCase().includes(search.toLowerCase()) ||
    p.title?.toLowerCase().includes(search.toLowerCase())
  )

  const createFields: ProviderFormField[] = [
    { name: "title", label: t("form.title"), type: "text", required: true },
    {
      name: "programType",
      label: t("form.type"),
      type: "select",
      required: true,
      options: ["PROGRAM", "COURSE", "SEMINAR", "WORKSHOP"].map((v) => ({ value: v, label: t(`form.programTypes.${v}`) })),
    },
    { name: "description", label: t("form.description"), type: "textarea" },
    { name: "startDate", label: t("form.startDate"), type: "datetime-local" },
    { name: "endDate", label: t("form.endDate"), type: "datetime-local" },
    { name: "maxParticipants", label: t("form.maxParticipants"), type: "number" },
    {
      name: "isPublished",
      label: t("form.visibility"),
      type: "select",
      options: [
        { value: "false", label: t("form.draft") },
        { value: "true", label: t("form.published") },
      ],
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{t("programs.title")}</h1>
          <p className="text-muted-foreground">{t("programs.subtitle")}</p>
        </div>
        <button
          onClick={() => setCreateOpen(true)}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" /> {t("programs.createProgram")}
        </button>
      </div>
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Search className="size-4 text-muted-foreground" />
            <input
              placeholder={t("programs.searchPlaceholder")}
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
            <p className="py-4 text-center text-sm text-muted-foreground">{t("programs.empty")}</p>
          ) : (
            <div className="space-y-3">
              {filtered.map((program) => (
                <div key={program.id} className="flex items-center justify-between rounded-lg border border-border p-4">
                  <div>
                    <p className="text-sm font-medium text-foreground">{program.name || program.title}</p>
                    <p className="text-xs text-muted-foreground">{program.description || t("programs.noDescriptionFallback")}</p>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className="rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">{program.status || ts("active")}</span>
                    <div className="flex items-center gap-1">
                      <button
                        onClick={() => { setConfirmDeleteId(null); setEditRow(program) }}
                        className="rounded px-2 py-1 text-xs font-medium text-blue-600 hover:bg-blue-50"
                      >
                        {t("form.edit")}
                      </button>
                      <button
                        onClick={() => { if (confirmDeleteId === program.id) handleDelete(program); else setConfirmDeleteId(program.id) }}
                        className={`rounded px-2 py-1 text-xs font-medium transition-colors ${confirmDeleteId === program.id ? "bg-red-600 text-white hover:bg-red-700" : "text-red-600 hover:bg-red-50"}`}
                      >
                        {confirmDeleteId === program.id ? t("form.confirmDelete") : t("form.delete")}
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
        title={editRow ? t("form.editTitle", { name: editRow.title || "" }) : t("programs.createProgram")}
        submitLabel={editRow ? t("form.save") : undefined}
        fields={createFields}
        initialValues={editRow || undefined}
        onClose={() => { setCreateOpen(false); setEditRow(null) }}
        onSubmit={async (v) => {
          if (editRow) {
            await nfeApi.updateProgram(editRow.id, { ...v, providerId: editRow.providerId })
            toast(t("form.updateSuccess"), "success")
            setEditRow(null)
          } else {
            await nfeApi.createProgram({
              title: v.title,
              programType: v.programType,
              description: v.description,
              startDate: v.startDate,
              endDate: v.endDate,
              maxParticipants: v.maxParticipants,
              isPublished: v.isPublished === undefined ? undefined : v.isPublished === "true",
            })
            toast(t("form.createSuccess"), "success")
            setCreateOpen(false)
          }
          loadPrograms()
        }}
      />
    </div>
  )
}
