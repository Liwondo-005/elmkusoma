"use client"

import { useCallback, useEffect, useState } from "react"
import {
  Plus, Loader2, AlertCircle, PenLine, Search, LayoutTemplate, Link2,
  History, Eye, X, CheckCircle2, Ban,
} from "lucide-react"
import {
  platformAdminApi,
  type PlatformTemplate,
  type TemplateInput,
  type TemplateVersion,
  type Signatory,
  type PageResponse,
} from "@/lib/platform-admin-api"
import { CertificateDocument, expandCertificateTokens } from "@/components/certificates/certificate-document"

const TEMPLATE_TYPES = ["COMPLETION", "ACHIEVEMENT", "PARTICIPATION", "TRANSCRIPT", "CUSTOM"]

interface TemplateFormState {
  id?: string
  name: string
  description: string
  templateType: string
  logoUrl: string
  institutionId: string // create only
}

interface InstitutionOption {
  id: string
  name: string
}

export function CertificateTemplatesSection() {
  const [data, setData] = useState<PageResponse<PlatformTemplate> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [typeFilter, setTypeFilter] = useState("")
  const [institutions, setInstitutions] = useState<InstitutionOption[]>([])

  const [form, setForm] = useState<TemplateFormState | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const [linkModal, setLinkModal] = useState<{ template: PlatformTemplate; selected: string[] } | null>(null)
  const [linkSignatories, setLinkSignatories] = useState<Signatory[] | null>(null)
  const [linkSaving, setLinkSaving] = useState(false)
  const [linkError, setLinkError] = useState<string | null>(null)

  const [versionsModal, setVersionsModal] = useState<{ template: PlatformTemplate; versions: TemplateVersion[] | null } | null>(null)
  const [previewTemplate, setPreviewTemplate] = useState<PlatformTemplate | null>(null)
  const [busy, setBusy] = useState<string | null>(null)

  const load = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await platformAdminApi.listCertificateTemplates(0, 50, {
        search: search.trim() || undefined,
        type: typeFilter || undefined,
      })
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load templates")
    } finally {
      setLoading(false)
    }
  }, [search, typeFilter])

  useEffect(() => {
    const t = setTimeout(load, 250)
    return () => clearTimeout(t)
  }, [load])

  useEffect(() => {
    platformAdminApi
      .listInstitutions(0, 100)
      .then((res) => setInstitutions(res.content.map((i) => ({ id: i.id, name: i.name }))))
      .catch(() => setInstitutions([]))
  }, [])

  const save = async () => {
    if (!form) return
    if (!form.name.trim()) {
      setFormError("Template name is required.")
      return
    }
    if (!form.id && !form.institutionId) {
      setFormError("Select the institution that owns this template.")
      return
    }
    setSaving(true)
    setFormError(null)
    const payload: TemplateInput = {
      name: form.name.trim(),
      description: form.description.trim() || null,
      templateType: form.templateType,
      logoUrl: form.logoUrl.trim() || null,
      ...(form.id ? {} : { institutionId: form.institutionId }),
    }
    try {
      if (form.id) {
        await platformAdminApi.updateCertificateTemplate(form.id, payload)
      } else {
        await platformAdminApi.createCertificateTemplate(payload)
      }
      setForm(null)
      await load()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "Failed to save template")
    } finally {
      setSaving(false)
    }
  }

  const toggleStatus = async (template: PlatformTemplate) => {
    setBusy(template.id)
    setError(null)
    try {
      await platformAdminApi.setCertificateTemplateStatus(template.id, !template.isActive)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update template status")
    } finally {
      setBusy(null)
    }
  }

  const openLink = async (template: PlatformTemplate) => {
    setLinkModal({ template, selected: template.signatories.map((s) => s.id) })
    setLinkSignatories(null)
    setLinkError(null)
    try {
      const res = await platformAdminApi.listCertificateSignatories(0, 100, { status: "ACTIVE" })
      setLinkSignatories(res.content)
    } catch (err) {
      setLinkError(err instanceof Error ? err.message : "Failed to load signatories")
    }
  }

  const saveLinks = async () => {
    if (!linkModal) return
    setLinkSaving(true)
    setLinkError(null)
    try {
      await platformAdminApi.replaceTemplateSignatories(linkModal.template.id, linkModal.selected)
      setLinkModal(null)
      await load()
    } catch (err) {
      setLinkError(err instanceof Error ? err.message : "Failed to update signatories")
    } finally {
      setLinkSaving(false)
    }
  }

  const openVersions = async (template: PlatformTemplate) => {
    setVersionsModal({ template, versions: null })
    try {
      const versions = await platformAdminApi.getTemplateVersions(template.id)
      setVersionsModal({ template, versions })
    } catch {
      setVersionsModal({ template, versions: [] }) // backend error → empty list; version count still shown
    }
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative min-w-[220px] flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search templates"
            className="w-full rounded-xl border border-border bg-background pl-9 pr-4 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
          />
        </div>
        <select
          value={typeFilter}
          onChange={(e) => setTypeFilter(e.target.value)}
          className="rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none"
        >
          <option value="">All types</option>
          {TEMPLATE_TYPES.map((t) => (
            <option key={t} value={t}>{t}</option>
          ))}
        </select>
        <button
          onClick={() => setForm({ name: "", description: "", templateType: "COMPLETION", logoUrl: "", institutionId: "" })}
          className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90"
        >
          <Plus className="size-4" /> New template
        </button>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="size-4" /> {error}
        </div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-16 text-center">
          <LayoutTemplate className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">No templates found</p>
          <p className="mt-1 text-sm text-muted-foreground">
            {search || typeFilter ? "No templates match your filters." : "No certificate templates exist yet."}
          </p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Template</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Institution</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Usage</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Signatories</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Status</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody>
                {data.content.map((t) => (
                  <tr key={t.id} className="border-b border-border last:border-0 hover:bg-muted/30 transition-colors">
                    <td className="px-5 py-3.5">
                      <p className="font-medium text-foreground">{t.name}</p>
                      <p className="text-xs text-muted-foreground">
                        {t.templateType} · v{t.version}
                      </p>
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground">{t.institutionName || "—"}</td>
                    <td className="px-5 py-3.5 text-muted-foreground">
                      {t.usageCount} certificate{t.usageCount === 1 ? "" : "s"}
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground">{t.signatories.length}</td>
                    <td className="px-5 py-3.5">
                      <span
                        className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${
                          t.isActive ? "bg-green-500/10 text-green-600" : "bg-red-500/10 text-red-600"
                        }`}
                      >
                        {t.isActive ? "ACTIVE" : "INACTIVE"}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex flex-wrap justify-end gap-1.5">
                        <button
                          onClick={() => setPreviewTemplate(t)}
                          title="Preview template"
                          className="inline-flex items-center gap-1 rounded-lg border border-border px-2 py-1 text-xs font-semibold text-foreground hover:bg-muted"
                        >
                          <Eye className="size-3" /> Preview
                        </button>
                        <button
                          onClick={() =>
                            setForm({
                              id: t.id,
                              name: t.name,
                              description: t.description || "",
                              templateType: t.templateType,
                              logoUrl: t.logoUrl || "",
                              institutionId: t.institutionId,
                            })
                          }
                          className="inline-flex items-center gap-1 rounded-lg border border-border px-2 py-1 text-xs font-semibold text-foreground hover:bg-muted"
                        >
                          <PenLine className="size-3" /> Edit
                        </button>
                        <button
                          onClick={() => openLink(t)}
                          className="inline-flex items-center gap-1 rounded-lg border border-border px-2 py-1 text-xs font-semibold text-foreground hover:bg-muted"
                        >
                          <Link2 className="size-3" /> Signatories
                        </button>
                        <button
                          onClick={() => openVersions(t)}
                          className="inline-flex items-center gap-1 rounded-lg border border-border px-2 py-1 text-xs font-semibold text-foreground hover:bg-muted"
                        >
                          <History className="size-3" /> v{t.version}
                        </button>
                        <button
                          onClick={() => toggleStatus(t)}
                          disabled={busy === t.id}
                          className={`inline-flex items-center gap-1 rounded-lg border px-2 py-1 text-xs font-semibold disabled:opacity-50 ${
                            t.isActive
                              ? "border-red-200 text-red-600 hover:bg-red-50"
                              : "border-green-200 text-green-600 hover:bg-green-50"
                          }`}
                        >
                          {t.isActive ? <Ban className="size-3" /> : <CheckCircle2 className="size-3" />}
                          {t.isActive ? "Deactivate" : "Activate"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Create / edit */}
      {form && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black/50 p-4 sm:p-8">
          <div className="mx-auto w-full max-w-xl rounded-2xl border border-border bg-card p-6 shadow-2xl">
            <div className="mb-5 flex items-center justify-between">
              <h2 className="text-base font-semibold text-foreground">
                {form.id ? "Edit template" : "New certificate template"}
              </h2>
              <button onClick={() => setForm(null)} className="rounded-lg border border-border p-1.5 text-muted-foreground hover:bg-muted" aria-label="Close">
                <X className="size-4" />
              </button>
            </div>

            {formError && (
              <div className="mb-4 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive flex items-center gap-2">
                <AlertCircle className="size-4 shrink-0" /> {formError}
              </div>
            )}

            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block sm:col-span-2">
                <span className="text-xs font-medium text-muted-foreground">Name *</span>
                <input
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
                  placeholder="e.g. Standard Completion Certificate"
                />
              </label>
              <label className="block">
                <span className="text-xs font-medium text-muted-foreground">Template type *</span>
                <select
                  value={form.templateType}
                  onChange={(e) => setForm({ ...form, templateType: e.target.value })}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none"
                >
                  {TEMPLATE_TYPES.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
              </label>
              {!form.id && (
                <label className="block">
                  <span className="text-xs font-medium text-muted-foreground">Owning institution *</span>
                  <select
                    value={form.institutionId}
                    onChange={(e) => setForm({ ...form, institutionId: e.target.value })}
                    className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none"
                  >
                    <option value="">Select institution…</option>
                    {institutions.map((inst) => (
                      <option key={inst.id} value={inst.id}>{inst.name}</option>
                    ))}
                  </select>
                </label>
              )}
              <label className="block sm:col-span-2">
                <span className="text-xs font-medium text-muted-foreground">
                  Statement (supports dynamic fields like {"{{recipient.fullName}}"} and {"{{course.title}}"})
                </span>
                <textarea
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  rows={3}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
                  placeholder={"Awarded to {{recipient.fullName}} for successfully completing {{course.title}}."}
                />
              </label>
              <label className="block sm:col-span-2">
                <span className="text-xs font-medium text-muted-foreground">Logo URL (optional)</span>
                <input
                  value={form.logoUrl}
                  onChange={(e) => setForm({ ...form, logoUrl: e.target.value })}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
                  placeholder="https://…"
                />
              </label>
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button onClick={() => setForm(null)} className="rounded-xl border border-border px-4 py-2 text-sm text-muted-foreground hover:bg-muted">
                Cancel
              </button>
              <button
                onClick={save}
                disabled={saving}
                className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50"
              >
                {saving && <Loader2 className="size-4 animate-spin" />}
                {form.id ? "Save changes" : "Create template"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Link signatories */}
      {linkModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black/50 p-4 sm:p-8">
          <div className="mx-auto w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-2xl">
            <div className="mb-4 flex items-center justify-between">
              <div>
                <h2 className="text-base font-semibold text-foreground">Authorised signatories</h2>
                <p className="text-xs text-muted-foreground">{linkModal.template.name}</p>
              </div>
              <button onClick={() => setLinkModal(null)} className="rounded-lg border border-border p-1.5 text-muted-foreground hover:bg-muted" aria-label="Close">
                <X className="size-4" />
              </button>
            </div>

            {linkError && (
              <div className="mb-4 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive flex items-center gap-2">
                <AlertCircle className="size-4 shrink-0" /> {linkError}
              </div>
            )}

            {linkSignatories === null ? (
              <div className="flex items-center justify-center py-10">
                <Loader2 className="size-5 animate-spin text-muted-foreground" />
              </div>
            ) : linkSignatories.length === 0 ? (
              <p className="rounded-xl border border-dashed border-border px-4 py-6 text-center text-sm text-muted-foreground">
                No active signatory profiles exist yet. Create one in the Signatories tab first.
              </p>
            ) : (
              <ul className="space-y-2">
                {linkSignatories.map((s) => {
                  const checked = linkModal.selected.includes(s.id)
                  return (
                    <li key={s.id}>
                      <label className="flex cursor-pointer items-center gap-3 rounded-xl border border-border px-4 py-3 hover:bg-muted/50">
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={() =>
                            setLinkModal({
                              ...linkModal,
                              selected: checked
                                ? linkModal.selected.filter((id) => id !== s.id)
                                : [...linkModal.selected, s.id],
                            })
                          }
                          className="size-4 accent-[var(--color-primary,theme(colors.primary.600))]"
                        />
                        <div className="min-w-0 flex-1">
                          <p className="truncate text-sm font-medium text-foreground">{s.fullName}</p>
                          <p className="truncate text-xs text-muted-foreground">
                            {[s.positionTitle, s.organization].filter(Boolean).join(" · ") || "—"}
                            {" · "}
                            {s.certificateTypes?.length ? s.certificateTypes.join(", ") : "all types"}
                          </p>
                        </div>
                      </label>
                    </li>
                  )
                })}
              </ul>
            )}

            <div className="mt-6 flex justify-end gap-2">
              <button onClick={() => setLinkModal(null)} className="rounded-xl border border-border px-4 py-2 text-sm text-muted-foreground hover:bg-muted">
                Cancel
              </button>
              <button
                onClick={saveLinks}
                disabled={linkSaving || linkSignatories === null}
                className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50"
              >
                {linkSaving && <Loader2 className="size-4 animate-spin" />} Save links
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Versions */}
      {versionsModal && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black/50 p-4 sm:p-8">
          <div className="mx-auto w-full max-w-xl rounded-2xl border border-border bg-card p-6 shadow-2xl">
            <div className="mb-4 flex items-center justify-between">
              <div>
                <h2 className="text-base font-semibold text-foreground">Template versions</h2>
                <p className="text-xs text-muted-foreground">
                  {versionsModal.template.name} — current v{versionsModal.template.version}
                </p>
              </div>
              <button onClick={() => setVersionsModal(null)} className="rounded-lg border border-border p-1.5 text-muted-foreground hover:bg-muted" aria-label="Close">
                <X className="size-4" />
              </button>
            </div>

            {versionsModal.versions === null ? (
              <div className="flex items-center justify-center py-10">
                <Loader2 className="size-5 animate-spin text-muted-foreground" />
              </div>
            ) : versionsModal.versions.length === 0 ? (
              <div className="rounded-xl border border-border bg-muted/40 px-4 py-6 text-center">
                <p className="text-sm font-medium text-foreground">
                  v{versionsModal.template.version} (current) — no archived edits yet
                </p>
                <p className="mt-1 text-xs text-muted-foreground">
                  Each edit archives the previous content as a numbered version.
                </p>
              </div>
            ) : (
              <ul className="space-y-2">
                <li className="rounded-xl border border-primary/30 bg-primary/5 px-4 py-3">
                  <p className="text-sm font-semibold text-foreground">
                    v{versionsModal.template.version} (current)
                  </p>
                  <p className="text-xs text-muted-foreground">{versionsModal.template.name}</p>
                </li>
                {versionsModal.versions.map((v) => (
                  <li key={v.id} className="rounded-xl border border-border px-4 py-3">
                    <div className="flex items-center justify-between">
                      <p className="text-sm font-semibold text-foreground">v{v.version}</p>
                      <p className="text-xs text-muted-foreground">
                        {v.archivedAt ? new Date(v.archivedAt).toLocaleString("en-GB", { dateStyle: "medium", timeStyle: "short" }) : "—"}
                      </p>
                    </div>
                    <p className="text-xs text-muted-foreground">{v.name}</p>
                    {v.description ? <p className="mt-1 line-clamp-2 text-xs text-muted-foreground">{v.description}</p> : null}
                    {v.archivedBy ? <p className="mt-1 text-[11px] text-muted-foreground">Archived by {v.archivedBy}</p> : null}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      )}

      {/* Sample preview */}
      {previewTemplate && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black/70 p-4 sm:p-8">
          <div className="mx-auto w-full max-w-5xl">
            <div className="mb-4 flex items-center justify-between">
              <p className="text-sm font-medium text-white">
                Template preview — sample layout (marked SAMPLE, not a real certificate)
              </p>
              <button
                onClick={() => setPreviewTemplate(null)}
                className="rounded-xl border border-white/30 px-4 py-2 text-sm text-white hover:bg-white/10"
              >
                Close
              </button>
            </div>
            <PreviewDocument template={previewTemplate} />
          </div>
        </div>
      )}
    </div>
  )
}

/**
 * Renders the template's statement (with real dynamic-field expansion against a
 * clearly-labelled placeholder context) inside the professional certificate layout.
 */
function PreviewDocument({ template }: { template: PlatformTemplate }) {
  const context: Record<string, string> = {
    "recipient.fullName": "[Recipient name]",
    "recipient.studentId": "[Student ID]",
    "certificate.serialNumber": "[SERIAL]",
    "certificate.number": "[SERIAL]",
    "certificate.title": template.name,
    "certificate.type": template.templateType,
    "certificate.status": "ISSUED",
    "certificate.grade": "[Grade]",
    "certificate.issueDate": "[Issue date]",
    "certificate.completionDate": "[Completion date]",
    "certificate.expiryDate": "",
    "certificate.description": template.description || "",
    "course.title": "[Course title]",
    "institution.name": template.institutionName || "[Institution]",
    "institution.logo": template.logoUrl || "",
    "instructor.name": "[Instructor]",
    skills: "[Skills]",
  }
  const statement = template.description ? expandCertificateTokens(template.description, context) : ""

  return (
    <CertificateDocument
      sample
      institutionName={template.institutionName}
      logoUrl={template.logoUrl}
      certificate={{
        serialNumber: "[SERIAL]",
        certificateType: template.templateType,
        title: template.name,
        courseOrProgramme: "[Course title]",
        description: statement || null,
        studentName: "[Recipient name]",
        studentIdNumber: "[Student ID]",
        grade: "[Grade]",
        completionDate: undefined,
        issueDate: undefined,
        verificationCode: "[CODE]",
        skills: [],
      }}
      signatories={template.signatories}
    />
  )
}
