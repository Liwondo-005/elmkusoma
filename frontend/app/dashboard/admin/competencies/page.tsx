"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import { Target, Plus, Pencil, Trash2, Loader2, X, Search } from "lucide-react"
import { Button } from "@/components/ui/button"
import { collegeApi } from "@/lib/college-api"
import type { Competency, CompetencyType } from "@/lib/types/college"

const COMPETENCY_TYPES: CompetencyType[] = ["SKILL", "KNOWLEDGE", "PRACTICAL", "PROFESSIONAL"]

type FormData = {
  name: string
  code: string
  description: string
  competencyType: CompetencyType
  subjectId: string
  sortOrder: string
  isActive: boolean
}

const defaultForm: FormData = {
  name: "",
  code: "",
  description: "",
  competencyType: "SKILL",
  subjectId: "",
  sortOrder: "",
  isActive: true,
}

export default function CompetenciesPage() {
  const t = useTranslations("admin");
  const tc = useTranslations("common");
  const ts = useTranslations("status");
  const [competencies, setCompetencies] = useState<Competency[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState<Competency | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [saving, setSaving] = useState(false)
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null)
  const [form, setForm] = useState<FormData>(defaultForm)

  const loadData = async () => {
    try {
      setLoading(true)
      const res = await collegeApi.listCompetencies()
      setCompetencies((res.data as Competency[] | undefined) || [])
    } catch (err) {
      setError(err instanceof Error ? err.message : t("competencies.failedToLoadCompetencies"))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const resetForm = () => {
    setForm(defaultForm)
    setEditing(null)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const payload = {
        ...form,
        subjectId: form.subjectId || undefined,
        sortOrder: form.sortOrder ? Number(form.sortOrder) : undefined,
      }
      if (editing) {
        await collegeApi.updateCompetency(editing.id, payload)
      } else {
        await collegeApi.createCompetency(payload)
      }
      setShowForm(false)
      resetForm()
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("competencies.failedToSaveCompetency"))
    } finally {
      setSaving(false)
    }
  }

  const handleEdit = (comp: Competency) => {
    setForm({
      name: comp.name,
      code: comp.code || "",
      description: comp.description || "",
      competencyType: comp.competencyType,
      subjectId: comp.subjectId || "",
      sortOrder: comp.sortOrder?.toString() || "",
      isActive: comp.isActive,
    })
    setEditing(comp)
    setShowForm(true)
  }

  const handleDelete = async (id: string) => {
    try {
      await collegeApi.deleteCompetency(id)
      setDeleteConfirm(null)
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("competencies.failedToDeleteCompetency"))
    }
  }

  const filtered = competencies.filter((c) =>
    c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    (c.code || "").toLowerCase().includes(searchQuery.toLowerCase()) ||
    c.competencyType.toLowerCase().includes(searchQuery.toLowerCase())
  )

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("competencies.competencyFramework")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("competencies.defineAndManageCompetencies")}</p>
        </div>
        <Button
          onClick={() => { resetForm(); setShowForm(true) }}
          className="gap-2"
        >
          <Plus className="size-4" />
          {t("competencies.addCompetency")}</Button>
      </div>

      <div className="flex items-center gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder={t("competencies.searchCompetencies")}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-lg border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
        <span className="text-sm text-muted-foreground">{filtered.length !== 1 ? t("competencies.countPlural", { count: filtered.length }) : t("competencies.countSingle", { count: filtered.length })}</span>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        </div>
      ) : filtered.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <Target className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("competencies.noCompetenciesFound")}</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            {searchQuery ? t("competencies.tryADifferentSearch") : t("competencies.addYourFirstCompetency")}
          </p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("competencies.name")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("competencies.code")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("competencies.type")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("competencies.subject")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("competencies.status")}</th>
                  <th className="px-5 py-3 text-right font-medium text-muted-foreground">{t("competencies.actions")}</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((comp) => (
                  <tr key={comp.id} className="border-b border-border last:border-0 hover:bg-muted/30 transition-colors">
                    <td className="px-5 py-3.5">
                      <div className="font-medium text-foreground">{comp.name}</div>
                      {comp.description && (
                        <div className="mt-0.5 text-xs text-muted-foreground line-clamp-1">{comp.description}</div>
                      )}
                    </td>
                    <td className="px-5 py-3.5">
                      {comp.code ? (
                        <span className="rounded bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">{comp.code}</span>
                      ) : (
                        <span className="text-muted-foreground">-</span>
                      )}
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="rounded bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
                        {comp.competencyType}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground">{comp.subjectId || "-"}</td>
                    <td className="px-5 py-3.5">
                      <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                        comp.isActive ? "bg-green-500/10 text-green-600" : "bg-muted text-muted-foreground"
                      }`}>
                        {comp.isActive ? ts("active") : ts("inactive")}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          onClick={() => handleEdit(comp)}
                          className="rounded-lg p-2 text-muted-foreground hover:bg-muted hover:text-foreground"
                        >
                          <Pencil className="size-4" />
                        </button>
                        {deleteConfirm === comp.id ? (
                          <div className="flex items-center gap-1">
                            <Button
                              variant="destructive"
                              size="xs"
                              onClick={() => handleDelete(comp.id)}
                            >
                              {tc("delete")}</Button>
                            <Button
                              variant="outline"
                              size="xs"
                              onClick={() => setDeleteConfirm(null)}
                            >
                              {tc("cancel")}</Button>
                          </div>
                        ) : (
                          <button
                            onClick={() => setDeleteConfirm(comp.id)}
                            className="rounded-lg p-2 text-muted-foreground hover:bg-destructive/10 hover:text-destructive"
                          >
                            <Trash2 className="size-4" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-xl max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-lg font-semibold text-foreground">{editing ? t("competencies.editCompetency") : t("competencies.addCompetency2")}</h2>
              <button onClick={() => { setShowForm(false); resetForm() }} className="rounded-lg p-1 hover:bg-muted">
                <X className="size-5" />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium">{t("competencies.name2")}</label>
                <input
                  required
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                  placeholder={t("competencies.eGProblemSolving")}
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">{t("competencies.code2")}</label>
                <input
                  value={form.code}
                  onChange={(e) => setForm({ ...form, code: e.target.value })}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                  placeholder={t("competencies.eGComp")}
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">{t("competencies.description")}</label>
                <textarea
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  rows={3}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-sm font-medium">{t("competencies.competencyType")}</label>
                  <select
                    value={form.competencyType}
                    onChange={(e) => setForm({ ...form, competencyType: e.target.value as CompetencyType })}
                    className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                  >
                    {COMPETENCY_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">{t("competencies.subjectIdOptional")}</label>
                  <input
                    value={form.subjectId}
                    onChange={(e) => setForm({ ...form, subjectId: e.target.value })}
                    className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                    placeholder={t("competencies.optionalSubjectId")}
                  />
                </div>
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">{t("competencies.sortOrder")}</label>
                <input
                  type="number"
                  min="0"
                  value={form.sortOrder}
                  onChange={(e) => setForm({ ...form, sortOrder: e.target.value })}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                  placeholder={t("competencies.eG")}
                />
              </div>
              <div className="flex items-center gap-3">
                <label className="text-sm font-medium">{ts("active")}</label>
                <button
                  type="button"
                  onClick={() => setForm({ ...form, isActive: !form.isActive })}
                  className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
                    form.isActive ? "bg-primary" : "bg-muted"
                  }`}
                >
                  <span className={`inline-block size-4 rounded-full bg-white transition-transform ${
                    form.isActive ? "translate-x-6" : "translate-x-1"
                  }`} />
                </button>
              </div>
              <div className="flex justify-end gap-2 pt-2">
                <Button type="button" variant="outline" onClick={() => { setShowForm(false); resetForm() }}>
                  {tc("cancel")}</Button>
                <Button type="submit" disabled={saving}>
                  {saving && <Loader2 className="size-4 animate-spin" />}
                  {editing ? t("competencies.saveChanges") : t("competencies.createCompetency")}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
