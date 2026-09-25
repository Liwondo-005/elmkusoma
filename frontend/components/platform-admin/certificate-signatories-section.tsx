"use client"

import { useCallback, useEffect, useState } from "react"
import {
  Plus, Loader2, AlertCircle, PenLine, Trash2, Search, Users, X, ShieldCheck,
} from "lucide-react"
import {
  platformAdminApi,
  type Signatory,
  type SignatoryInput,
  type PageResponse,
} from "@/lib/platform-admin-api"

const CERT_TYPES = ["COMPLETION", "ACHIEVEMENT", "PARTICIPATION", "TRANSCRIPT"]
const MAX_SIGNATURE_CHARS = 300_000 // ~225 KB decoded — mirrors backend validation

interface SignatoryFormState {
  id?: string
  fullName: string
  positionTitle: string
  organization: string
  status: string
  certificateTypes: string[]
  institutionId: string // "" = platform-wide
  validFrom: string
  validUntil: string
  signatureImage: string | null
}

const emptyForm: SignatoryFormState = {
  fullName: "",
  positionTitle: "",
  organization: "",
  status: "ACTIVE",
  certificateTypes: [],
  institutionId: "",
  validFrom: "",
  validUntil: "",
  signatureImage: null,
}

interface InstitutionOption {
  id: string
  name: string
}

export function CertificateSignatoriesSection() {
  const [data, setData] = useState<PageResponse<Signatory> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [statusFilter, setStatusFilter] = useState("")
  const [institutions, setInstitutions] = useState<InstitutionOption[]>([])

  const [form, setForm] = useState<SignatoryFormState | null>(null)
  const [saving, setSaving] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState<string | null>(null)
  const [deleteConfirm, setDeleteConfirm] = useState<Signatory | null>(null)

  const load = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await platformAdminApi.listCertificateSignatories(0, 50, {
        search: search.trim() || undefined,
        status: statusFilter || undefined,
      })
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load signatories")
    } finally {
      setLoading(false)
    }
  }, [search, statusFilter])

  useEffect(() => {
    const t = setTimeout(load, 250) // debounce typing
    return () => clearTimeout(t)
  }, [load])

  useEffect(() => {
    platformAdminApi
      .listInstitutions(0, 100)
      .then((res) => setInstitutions(res.content.map((i) => ({ id: i.id, name: i.name }))))
      .catch(() => setInstitutions([])) // scope select falls back to platform-wide only
  }, [])

  const save = async () => {
    if (!form) return
    if (!form.fullName.trim()) {
      setFormError("Full name is required.")
      return
    }
    setSaving(true)
    setFormError(null)
    const payload: SignatoryInput = {
      fullName: form.fullName.trim(),
      positionTitle: form.positionTitle.trim() || null,
      organization: form.organization.trim() || null,
      signatureImage: form.signatureImage,
      certificateTypes: form.certificateTypes.length > 0 ? form.certificateTypes : null,
      status: form.status,
      validFrom: form.validFrom || null,
      validUntil: form.validUntil || null,
      institutionId: form.institutionId || null,
    }
    try {
      if (form.id) {
        await platformAdminApi.updateCertificateSignatory(form.id, payload)
      } else {
        await platformAdminApi.createCertificateSignatory(payload)
      }
      setForm(null)
      await load()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : "Failed to save signatory")
    } finally {
      setSaving(false)
    }
  }

  const remove = async (signatory: Signatory) => {
    setDeleting(signatory.id)
    setError(null)
    try {
      await platformAdminApi.deleteCertificateSignatory(signatory.id)
      setDeleteConfirm(null)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to remove signatory")
      setDeleteConfirm(null)
    } finally {
      setDeleting(null)
    }
  }

  const onSignatureFile = (file: File | null) => {
    if (!file) return
    if (!file.type.startsWith("image/")) {
      setFormError("Signature must be an image file.")
      return
    }
    const reader = new FileReader()
    reader.onload = () => {
      const value = String(reader.result || "")
      if (value.length > MAX_SIGNATURE_CHARS) {
        setFormError("Signature image is too large (max ~200 KB).")
        return
      }
      setFormError(null)
      setForm((f) => (f ? { ...f, signatureImage: value } : f))
    }
    reader.readAsDataURL(file)
  }

  const toggleType = (type: string) =>
    setForm((f) =>
      f
        ? {
            ...f,
            certificateTypes: f.certificateTypes.includes(type)
              ? f.certificateTypes.filter((t) => t !== type)
              : [...f.certificateTypes, type],
          }
        : f
    )

  return (
    <div className="space-y-4">
      {/* Toolbar */}
      <div className="flex flex-wrap items-center gap-3">
        <div className="relative flex-1 min-w-[220px]">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search name or organisation"
            className="w-full rounded-xl border border-border bg-background pl-9 pr-4 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
          />
        </div>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none"
        >
          <option value="">All statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="INACTIVE">Inactive</option>
        </select>
        <button
          onClick={() => setForm({ ...emptyForm })}
          className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90"
        >
          <Plus className="size-4" /> New signatory
        </button>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="size-4" /> {error}
        </div>
      )}

      {deleteConfirm && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 p-4 space-y-3">
          <p className="text-sm font-semibold text-destructive">
            Remove signatory “{deleteConfirm.fullName}”?
          </p>
          <p className="text-sm text-muted-foreground">
            The profile is deactivated and its template links are removed (soft delete — audit history is preserved).
            Certificates already issued keep their own record.
          </p>
          <div className="flex gap-2">
            <button
              onClick={() => remove(deleteConfirm)}
              disabled={deleting === deleteConfirm.id}
              className="inline-flex items-center gap-2 rounded-xl bg-destructive px-4 py-2 text-sm font-semibold text-destructive-foreground hover:opacity-90 disabled:opacity-50"
            >
              {deleting === deleteConfirm.id ? <Loader2 className="size-4 animate-spin" /> : <Trash2 className="size-4" />}
              Confirm remove
            </button>
            <button
              onClick={() => setDeleteConfirm(null)}
              className="rounded-xl border border-border px-4 py-2 text-sm text-muted-foreground"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* List */}
      {loading ? (
        <div className="flex items-center justify-center py-16">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-16 text-center">
          <Users className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">No signatories found</p>
          <p className="mt-1 text-sm text-muted-foreground">
            {search || statusFilter
              ? "No signatories match your filters."
              : "No signatory profiles have been created yet."}
          </p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Name</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Scope</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Authorised types</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Validity</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Status</th>
                  <th className="px-5 py-3" />
                </tr>
              </thead>
              <tbody>
                {data.content.map((s) => (
                  <tr key={s.id} className="border-b border-border last:border-0 hover:bg-muted/30 transition-colors">
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-3">
                        {s.signatureImage ? (
                          // eslint-disable-next-line @next/next/no-img-element
                          <img src={s.signatureImage} alt="" className="h-8 w-16 rounded object-contain" />
                        ) : null}
                        <div>
                          <p className="font-medium text-foreground">{s.fullName}</p>
                          <p className="text-xs text-muted-foreground">
                            {[s.positionTitle, s.organization].filter(Boolean).join(" · ") || "—"}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground">
                      {s.institutionId
                        ? s.institutionName || "Institution-scoped"
                        : "Platform-wide"}
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground">
                      {s.certificateTypes && s.certificateTypes.length > 0
                        ? s.certificateTypes.join(", ")
                        : "All types"}
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground">
                      {(s.validFrom || s.validUntil)
                        ? `${s.validFrom || "…"} → ${s.validUntil || "…"}`
                        : "No limit"}
                    </td>
                    <td className="px-5 py-3.5">
                      <span
                        className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${
                          s.status === "ACTIVE" ? "bg-green-500/10 text-green-600" : "bg-muted text-muted-foreground"
                        }`}
                      >
                        {s.status}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-right">
                      <div className="inline-flex gap-1.5">
                        <button
                          onClick={() =>
                            setForm({
                              id: s.id,
                              fullName: s.fullName,
                              positionTitle: s.positionTitle || "",
                              organization: s.organization || "",
                              status: s.status,
                              certificateTypes: s.certificateTypes || [],
                              institutionId: s.institutionId || "",
                              validFrom: s.validFrom || "",
                              validUntil: s.validUntil || "",
                              signatureImage: s.signatureImage,
                            })
                          }
                          className="inline-flex items-center gap-1 rounded-lg border border-border px-2.5 py-1 text-xs font-semibold text-foreground hover:bg-muted"
                        >
                          <PenLine className="size-3" /> Edit
                        </button>
                        <button
                          onClick={() => setDeleteConfirm(s)}
                          className="inline-flex items-center gap-1 rounded-lg border border-red-200 px-2.5 py-1 text-xs font-semibold text-red-600 hover:bg-red-50"
                        >
                          <Trash2 className="size-3" /> Remove
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

      {/* Create / edit modal */}
      {form && (
        <div className="fixed inset-0 z-50 overflow-y-auto bg-black/50 p-4 sm:p-8">
          <div className="mx-auto w-full max-w-2xl rounded-2xl border border-border bg-card p-6 shadow-2xl">
            <div className="mb-5 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShieldCheck className="size-5 text-primary" />
                <h2 className="text-base font-semibold text-foreground">
                  {form.id ? "Edit signatory" : "New signatory"}
                </h2>
              </div>
              <button
                onClick={() => setForm(null)}
                className="rounded-lg border border-border p-1.5 text-muted-foreground hover:bg-muted"
                aria-label="Close"
              >
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
                <span className="text-xs font-medium text-muted-foreground">Full name *</span>
                <input
                  value={form.fullName}
                  onChange={(e) => setForm({ ...form, fullName: e.target.value })}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
                  placeholder="e.g. Dr. Neema Mushi"
                />
              </label>
              <label className="block">
                <span className="text-xs font-medium text-muted-foreground">Position / title</span>
                <input
                  value={form.positionTitle}
                  onChange={(e) => setForm({ ...form, positionTitle: e.target.value })}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
                  placeholder="e.g. Programme Director"
                />
              </label>
              <label className="block">
                <span className="text-xs font-medium text-muted-foreground">Organisation</span>
                <input
                  value={form.organization}
                  onChange={(e) => setForm({ ...form, organization: e.target.value })}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
                  placeholder="e.g. ELMKUSOMA"
                />
              </label>
              <label className="block">
                <span className="text-xs font-medium text-muted-foreground">Status</span>
                <select
                  value={form.status}
                  onChange={(e) => setForm({ ...form, status: e.target.value })}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none"
                >
                  <option value="ACTIVE">Active (authorised)</option>
                  <option value="INACTIVE">Inactive (not authorised)</option>
                </select>
              </label>
              <label className="block">
                <span className="text-xs font-medium text-muted-foreground">Institution scope</span>
                <select
                  value={form.institutionId}
                  onChange={(e) => setForm({ ...form, institutionId: e.target.value })}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none"
                >
                  <option value="">Platform-wide</option>
                  {institutions.map((inst) => (
                    <option key={inst.id} value={inst.id}>
                      {inst.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="block">
                <span className="text-xs font-medium text-muted-foreground">Valid from</span>
                <input
                  type="date"
                  value={form.validFrom}
                  onChange={(e) => setForm({ ...form, validFrom: e.target.value })}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none"
                />
              </label>
              <label className="block">
                <span className="text-xs font-medium text-muted-foreground">Valid until</span>
                <input
                  type="date"
                  value={form.validUntil}
                  onChange={(e) => setForm({ ...form, validUntil: e.target.value })}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none"
                />
              </label>

              <fieldset className="sm:col-span-2">
                <legend className="text-xs font-medium text-muted-foreground">
                  Authorised certificate types (none selected = all types)
                </legend>
                <div className="mt-2 flex flex-wrap gap-2">
                  {CERT_TYPES.map((type) => (
                    <label
                      key={type}
                      className={`cursor-pointer rounded-lg border px-3 py-1.5 text-xs font-medium ${
                        form.certificateTypes.includes(type)
                          ? "border-primary bg-primary/10 text-primary"
                          : "border-border text-muted-foreground hover:bg-muted"
                      }`}
                    >
                      <input
                        type="checkbox"
                        className="sr-only"
                        checked={form.certificateTypes.includes(type)}
                        onChange={() => toggleType(type)}
                      />
                      {type}
                    </label>
                  ))}
                </div>
              </fieldset>

              <div className="sm:col-span-2">
                <p className="text-xs font-medium text-muted-foreground">Signature image (PNG/JPG/WEBP, max ~200 KB)</p>
                <div className="mt-2 flex items-center gap-4">
                  <input
                    type="file"
                    accept="image/*"
                    onChange={(e) => onSignatureFile(e.target.files?.[0] ?? null)}
                    className="text-sm text-muted-foreground file:mr-3 file:rounded-lg file:border-0 file:bg-muted file:px-3 file:py-1.5 file:text-xs file:font-semibold file:text-foreground"
                  />
                  {form.signatureImage && (
                    <div className="flex items-center gap-2">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img src={form.signatureImage} alt="Signature preview" className="h-10 w-28 rounded border border-border bg-white object-contain" />
                      <button
                        onClick={() => setForm({ ...form, signatureImage: null })}
                        className="rounded-lg border border-border p-1 text-muted-foreground hover:bg-muted"
                        aria-label="Remove signature"
                      >
                        <X className="size-3.5" />
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>

            <div className="mt-6 flex justify-end gap-2">
              <button
                onClick={() => setForm(null)}
                className="rounded-xl border border-border px-4 py-2 text-sm text-muted-foreground hover:bg-muted"
              >
                Cancel
              </button>
              <button
                onClick={save}
                disabled={saving}
                className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground hover:opacity-90 disabled:opacity-50"
              >
                {saving && <Loader2 className="size-4 animate-spin" />}
                {form.id ? "Save changes" : "Create signatory"}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
