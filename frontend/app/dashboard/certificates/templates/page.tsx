"use client"

import { useEffect, useState } from "react"
import { FileText, Loader2, Plus, Trash2, X, Save } from "lucide-react"
import { certificateApi, getInstitutionId, type TemplateResponse } from "@/lib/api"

export default function CertificateTemplatesPage() {
  const [templates, setTemplates] = useState<TemplateResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showNew, setShowNew] = useState(false)
  const [newName, setNewName] = useState("")
  const [newType, setNewType] = useState("COMPLETION")
  const [newDescription, setNewDescription] = useState("")
  const [newHtml, setNewHtml] = useState("<div class='certificate'><h1>{{title}}</h1><p>Awarded to {{studentName}}</p></div>")
  const [saving, setSaving] = useState(false)

  const institutionId = getInstitutionId()

  useEffect(() => {
    if (!institutionId) { setError("No institution context found."); setLoading(false); return }
    certificateApi
      .listTemplates()
      .then(setTemplates)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load templates"))
      .finally(() => setLoading(false))
  }, [institutionId])

  async function handleCreate() {
    if (!institutionId || !newName.trim()) return
    setSaving(true)
    try {
      const created = await certificateApi.createTemplate({
        name: newName.trim(),
        templateType: newType,
        description: newDescription || undefined,
        htmlContent: newHtml,
      })
      setTemplates((prev) => [...prev, created])
      setShowNew(false)
      setNewName("")
      setNewType("COMPLETION")
      setNewDescription("")
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create template")
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Certificate Templates</h1>
          <p className="mt-1 text-sm text-muted-foreground">Manage certificate templates for your institution.</p>
        </div>
        <button
          onClick={() => setShowNew(true)}
          className="flex h-9 items-center gap-2 rounded-lg bg-primary px-4 text-xs font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-3.5" /> New Template
        </button>
      </div>

      {loading && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
          <p className="text-sm font-medium text-destructive">{error}</p>
        </div>
      )}

      {showNew && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
          <h3 className="mb-4 text-sm font-semibold text-foreground">New Template</h3>
          <div className="space-y-3">
            <input
              type="text"
              placeholder="Template name"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
            <select
              value={newType}
              onChange={(e) => setNewType(e.target.value)}
              className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            >
              <option value="COMPLETION">Completion</option>
              <option value="ACHIEVEMENT">Achievement</option>
              <option value="PARTICIPATION">Participation</option>
            </select>
            <input
              type="text"
              placeholder="Description (optional)"
              value={newDescription}
              onChange={(e) => setNewDescription(e.target.value)}
              className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring"
            />
            <textarea
              placeholder="Template HTML"
              value={newHtml}
              onChange={(e) => setNewHtml(e.target.value)}
              rows={6}
              className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm font-mono outline-none focus:border-ring"
            />
            <div className="flex gap-2">
              <button
                onClick={handleCreate}
                disabled={saving || !newName.trim()}
                className="flex h-9 items-center gap-2 rounded-lg bg-primary px-4 text-xs font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
              >
                <Save className="size-3.5" /> {saving ? "Creating..." : "Create Template"}
              </button>
              <button
                onClick={() => { setShowNew(false); setNewName(""); setNewDescription("") }}
                className="flex h-9 items-center gap-2 rounded-lg border border-border px-4 text-xs font-medium text-foreground hover:bg-muted"
              >
                <X className="size-3.5" /> Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {!loading && !error && templates.length === 0 && (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <FileText className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">No templates yet</p>
          <p className="mt-1 text-sm text-muted-foreground">Create a template to start issuing certificates.</p>
        </div>
      )}

      {!loading && templates.length > 0 && (
        <div className="space-y-3">
          {templates.map((t) => (
            <div key={t.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-start justify-between gap-4">
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2">
                    <span className="text-sm font-semibold text-foreground">{t.name}</span>
                    <span className="rounded bg-primary/10 px-2 py-0.5 text-[10px] font-semibold text-primary">{t.templateType}</span>
                    {t.isActive && (
                      <span className="rounded bg-teal/10 px-2 py-0.5 text-[10px] font-medium text-teal">Active</span>
                    )}
                  </div>
                  {t.description && <p className="mt-1 text-xs text-muted-foreground">{t.description}</p>}
                </div>
                <span className="shrink-0 text-xs text-muted-foreground">
                  {new Date(t.createdAt).toLocaleDateString()}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
