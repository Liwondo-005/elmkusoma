"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import { GraduationCap, Plus, Pencil, Trash2, Loader2, X, Search } from "lucide-react"
import { Button } from "@/components/ui/button"
import { collegeApi } from "@/lib/college-api"
import type { Programme, ProgrammeType, EducationLevelType } from "@/lib/types/college"

const PROGRAMME_TYPES: ProgrammeType[] = ["DIPLOMA", "CERTIFICATE", "DEGREE", "HIGHER_DIPLOMA", "PROFESSIONAL"]
const EDUCATION_LEVELS: EducationLevelType[] = ["COLLEGE", "VETA", "UNIVERSITY"]

type FormData = {
  name: string
  code: string
  description: string
  programmeType: ProgrammeType
  educationLevel: EducationLevelType
  durationMonths: string
  creditHours: string
  isActive: boolean
}

const defaultForm: FormData = {
  name: "",
  code: "",
  description: "",
  programmeType: "DIPLOMA",
  educationLevel: "COLLEGE",
  durationMonths: "",
  creditHours: "",
  isActive: true,
}

export default function ProgrammesPage() {
  const t = useTranslations("admin");
  const tc = useTranslations("common");
  const ts = useTranslations("status");
  const [programmes, setProgrammes] = useState<Programme[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editing, setEditing] = useState<Programme | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [saving, setSaving] = useState(false)
  const [deleteConfirm, setDeleteConfirm] = useState<string | null>(null)
  const [form, setForm] = useState<FormData>(defaultForm)

  const loadData = async () => {
    try {
      setLoading(true)
      const res = await collegeApi.listProgrammes()
      setProgrammes((res.data as Programme[] | undefined) || [])
    } catch (err) {
      setError(err instanceof Error ? err.message : t("programmes.failedToLoadProgrammes"))
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
        durationMonths: form.durationMonths ? Number(form.durationMonths) : undefined,
        creditHours: form.creditHours ? Number(form.creditHours) : undefined,
      }
      if (editing) {
        await collegeApi.updateProgramme(editing.id, payload)
      } else {
        await collegeApi.createProgramme(payload)
      }
      setShowForm(false)
      resetForm()
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("programmes.failedToSaveProgramme"))
    } finally {
      setSaving(false)
    }
  }

  const handleEdit = (prog: Programme) => {
    setForm({
      name: prog.name,
      code: prog.code,
      description: prog.description || "",
      programmeType: prog.programmeType,
      educationLevel: prog.educationLevel,
      durationMonths: prog.durationMonths?.toString() || "",
      creditHours: prog.creditHours?.toString() || "",
      isActive: prog.isActive,
    })
    setEditing(prog)
    setShowForm(true)
  }

  const handleDelete = async (id: string) => {
    try {
      await collegeApi.deleteProgramme(id)
      setDeleteConfirm(null)
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : t("programmes.failedToDeleteProgramme"))
    }
  }

  const filtered = programmes.filter((p) =>
    p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    p.code.toLowerCase().includes(searchQuery.toLowerCase()) ||
    p.programmeType.toLowerCase().includes(searchQuery.toLowerCase())
  )

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("programmes.programmes")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("programmes.manageAcademicProgrammesFor")}</p>
        </div>
        <Button
          onClick={() => { resetForm(); setShowForm(true) }}
          className="gap-2"
        >
          <Plus className="size-4" />
          {t("programmes.addProgramme")}</Button>
      </div>

      <div className="flex items-center gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder={t("programmes.searchProgrammes")}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full rounded-lg border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
          />
        </div>
        <span className="text-sm text-muted-foreground">{filtered.length} {t("programmes.programme")}{filtered.length !== 1 ? "s" : ""}</span>
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
          <GraduationCap className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">{t("programmes.noProgrammesFound")}</h3>
          <p className="mt-1 text-sm text-muted-foreground">
            {searchQuery ? t("programmes.tryADifferentSearch") : t("programmes.addYourFirstProgramme")}
          </p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("programmes.name")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("programmes.code")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("programmes.type")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("programmes.duration")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("programmes.level")}</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">{t("programmes.status")}</th>
                  <th className="px-5 py-3 text-right font-medium text-muted-foreground">{t("programmes.actions")}</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((prog) => (
                  <tr key={prog.id} className="border-b border-border last:border-0 hover:bg-muted/30 transition-colors">
                    <td className="px-5 py-3.5">
                      <div className="font-medium text-foreground">{prog.name}</div>
                      {prog.description && (
                        <div className="mt-0.5 text-xs text-muted-foreground line-clamp-1">{prog.description}</div>
                      )}
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="rounded bg-muted px-2 py-0.5 text-xs font-medium text-muted-foreground">{prog.code}</span>
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground">{prog.programmeType}</td>
                    <td className="px-5 py-3.5 text-muted-foreground">{prog.durationMonths ? `${prog.durationMonths} months` : "-"}</td>
                    <td className="px-5 py-3.5 text-muted-foreground">{prog.educationLevel}</td>
                    <td className="px-5 py-3.5">
                      <span className={`inline-flex rounded-full px-2 py-0.5 text-xs font-medium ${
                        prog.isActive ? "bg-green-500/10 text-green-600" : "bg-muted text-muted-foreground"
                      }`}>
                        {prog.isActive ? ts("active") : ts("inactive")}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center justify-end gap-1">
                        <button
                          onClick={() => handleEdit(prog)}
                          className="rounded-lg p-2 text-muted-foreground hover:bg-muted hover:text-foreground"
                        >
                          <Pencil className="size-4" />
                        </button>
                        {deleteConfirm === prog.id ? (
                          <div className="flex items-center gap-1">
                            <Button
                              variant="destructive"
                              size="xs"
                              onClick={() => handleDelete(prog.id)}
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
                            onClick={() => setDeleteConfirm(prog.id)}
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
              <h2 className="text-lg font-semibold text-foreground">{editing ? t("programmes.editProgramme") : t("programmes.addProgramme2")}</h2>
              <button onClick={() => { setShowForm(false); resetForm() }} className="rounded-lg p-1 hover:bg-muted">
                <X className="size-5" />
              </button>
            </div>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="mb-1 block text-sm font-medium">{t("programmes.name2")}</label>
                <input
                  required
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                  placeholder={t("programmes.eGBachelorOf")}
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">{t("programmes.code2")}</label>
                <input
                  required
                  value={form.code}
                  onChange={(e) => setForm({ ...form, code: e.target.value })}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                  placeholder={t("programmes.eGBscCs")}
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium">{t("programmes.description")}</label>
                <textarea
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  rows={3}
                  className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-sm font-medium">{t("programmes.programmeType")}</label>
                  <select
                    value={form.programmeType}
                    onChange={(e) => setForm({ ...form, programmeType: e.target.value as ProgrammeType })}
                    className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                  >
                    {PROGRAMME_TYPES.map((t) => <option key={t} value={t}>{t.replace(/_/g, " ")}</option>)}
                  </select>
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">{t("programmes.educationLevel")}</label>
                  <select
                    value={form.educationLevel}
                    onChange={(e) => setForm({ ...form, educationLevel: e.target.value as EducationLevelType })}
                    className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                  >
                    {EDUCATION_LEVELS.map((l) => <option key={l} value={l}>{l}</option>)}
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="mb-1 block text-sm font-medium">{t("programmes.durationMonths")}</label>
                  <input
                    type="number"
                    min="0"
                    value={form.durationMonths}
                    onChange={(e) => setForm({ ...form, durationMonths: e.target.value })}
                    className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                    placeholder={t("programmes.eG")}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium">{t("programmes.creditHours")}</label>
                  <input
                    type="number"
                    min="0"
                    value={form.creditHours}
                    onChange={(e) => setForm({ ...form, creditHours: e.target.value })}
                    className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-ring"
                    placeholder={t("programmes.eG2")}
                  />
                </div>
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
                  {editing ? t("programmes.saveChanges") : t("programmes.createProgramme")}
                </Button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
