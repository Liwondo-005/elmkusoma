"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { Loader2 } from "lucide-react"

export interface ProviderFormField {
  name: string
  label: string
  type: "text" | "textarea" | "select" | "number" | "datetime-local"
  required?: boolean
  options?: Array<{ value: string; label: string }>
}

export type ProviderFormValues = Record<string, string | number | undefined>

interface ProviderCreateDialogProps {
  open: boolean
  title: string
  fields: ProviderFormField[]
  onClose: () => void
  /** Receives coerced values (numbers as numbers); throws to surface an error. */
  onSubmit: (values: ProviderFormValues) => Promise<void>
}

/**
 * Shared create dialog for the provider workspace. Real POSTs through nfeApi
 * (server resolves institution scope + providerId — never trusted from here).
 */
export function ProviderCreateDialog({ open, title, fields, onClose, onSubmit }: ProviderCreateDialogProps) {
  const t = useTranslations("provider")
  const [values, setValues] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      setValues({})
      setError(null)
      setSubmitting(false)
    }
  }, [open])

  if (!open) return null

  function set(name: string, v: string) {
    setValues((prev) => ({ ...prev, [name]: v }))
  }

  async function handleSubmit() {
    setError(null)
    const missing = fields.find((f) => f.required && !String(values[f.name] ?? "").trim())
    if (missing) {
      setError(t("form.required"))
      return
    }
    setSubmitting(true)
    try {
      const coerced: ProviderFormValues = {}
      for (const f of fields) {
        const raw = values[f.name]
        if (raw === undefined || raw === null || String(raw).trim() === "") {
          coerced[f.name] = undefined
        } else if (f.type === "number") {
          coerced[f.name] = Number(raw)
        } else {
          coerced[f.name] = raw
        }
      }
      await onSubmit(coerced)
    } catch (e) {
      setError(e instanceof Error && e.message ? e.message : t("form.submitError"))
    } finally {
      setSubmitting(false)
    }
  }

  const inputClass =
    "mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      onClick={() => {
        if (!submitting) onClose()
      }}
      role="presentation"
    >
      <div
        className="max-h-[90vh] w-full max-w-lg overflow-y-auto rounded-xl border border-border bg-card p-6 shadow-xl"
        role="dialog"
        aria-modal="true"
        aria-label={title}
        onClick={(e) => e.stopPropagation()}
      >
        <h2 className="mb-4 text-lg font-semibold text-foreground">{title}</h2>

        {error && (
          <div className="mb-4 rounded-lg border border-destructive/20 bg-destructive/5 px-3 py-2 text-sm text-destructive">
            {error}
          </div>
        )}

        <div className="space-y-4">
          {fields.map((f) => (
            <div key={f.name}>
              <label className="text-sm font-medium text-foreground" htmlFor={`pcd-${f.name}`}>
                {f.label}
                {f.required && <span className="ml-1 text-destructive">*</span>}
              </label>
              {f.type === "textarea" ? (
                <textarea
                  id={`pcd-${f.name}`}
                  value={values[f.name] ?? ""}
                  onChange={(e) => set(f.name, e.target.value)}
                  rows={3}
                  className={inputClass}
                />
              ) : f.type === "select" ? (
                <select
                  id={`pcd-${f.name}`}
                  value={values[f.name] ?? ""}
                  onChange={(e) => set(f.name, e.target.value)}
                  className={inputClass}
                >
                  <option value="">{t("form.selectPlaceholder")}</option>
                  {(f.options ?? []).map((o) => (
                    <option key={o.value} value={o.value}>
                      {o.label}
                    </option>
                  ))}
                </select>
              ) : (
                <input
                  id={`pcd-${f.name}`}
                  type={f.type}
                  value={values[f.name] ?? ""}
                  onChange={(e) => set(f.name, e.target.value)}
                  className={inputClass}
                />
              )}
            </div>
          ))}
        </div>

        <div className="mt-6 flex justify-end gap-2">
          <button
            type="button"
            disabled={submitting}
            onClick={onClose}
            className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-muted disabled:opacity-60"
          >
            {t("form.cancel")}
          </button>
          <button
            type="button"
            disabled={submitting}
            onClick={handleSubmit}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-60"
          >
            {submitting && <Loader2 className="size-4 animate-spin" />}
            {submitting ? t("form.creating") : title}
          </button>
        </div>
      </div>
    </div>
  )
}
