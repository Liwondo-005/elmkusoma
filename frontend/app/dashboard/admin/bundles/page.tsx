"use client"

import { useCallback, useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import {
  listInstitutionBundles,
  getInstitutionBundle,
  createBundle,
  updateBundle,
  publishBundle,
  suspendBundle,
  listServiceTypeCatalogue,
  type Bundle,
} from "@/lib/bundle-api"

/**
 * Authoring bundles for this institution.
 *
 * <p>Everything the server enforces is reflected here so the UI does not pretend to be the rule:
 * a bundle arrives as a DRAFT, only a draft can be edited or published, a bundle must contain at
 * least one service, the same service cannot be listed twice, and publication is refused without
 * a duration. The buttons are shaped by that, but the authority is the server - hiding a button is
 * presentation, and {@code BundleAdminController} refuses the call regardless.
 *
 * <p>No price field exists anywhere on this page. A bundle's price comes from the approved
 * pricing policy at checkout, so accepting one here would create a second, ungoverned way to
 * change what a learner pays.
 */

/** A labelled input. The label is associated by id so the control is announced correctly. */
function Field({
  label,
  id,
  children,
}: {
  label: string
  id: string
  children: React.ReactNode
}) {
  return (
    <label htmlFor={id} className="text-sm">
      <span className="block text-muted-foreground">{label}</span>
      {children}
    </label>
  )
}
/**
 * A service type, chosen from the types the platform actually sells.
 *
 * <p>The server already refuses a name it cannot price, so this is not where the rule lives - it
 * is what stops the author from having to know those names by heart and discover them through a
 * failed submit. Two details matter:
 *
 * <p>The current value is always offered as an option even when the catalogue no longer lists it.
 * A draft naming a service that has since been retired must still open with that service showing,
 * because a select that silently fell back to a blank option would rewrite the bundle the moment
 * anyone pressed save - quietly trading a refusal they could see for a change they could not.
 *
 * <p>And when no catalogue is available - nothing sold yet, or the lookup failed - it stays a
 * plain text field. An empty picker would be a dead end, and the server is the authority anyway.
 */
function ServiceTypeControl({
  options,
  value,
  onChange,
  id,
  label,
  placeholder,
  inline,
}: {
  options: string[] | null
  value: string
  onChange: (next: string) => void
  id?: string
  label: string
  /** Shown only by the text fallback; a select carries its prompt as the blank option instead. */
  placeholder?: string
  /** Inside an item row there is no label element to hang spacing off, so it is dropped. */
  inline?: boolean
}) {
  const sizing = "w-full rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
  const className = inline ? sizing : `mt-1 ${sizing}`

  if (options === null || options.length === 0) {
    return (
      <input
        id={id}
        aria-label={label}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className={className}
      />
    )
  }

  const current = value.trim()
  const offered = options.includes(current)
  return (
    <select
      id={id}
      aria-label={label}
      value={current}
      onChange={(e) => onChange(e.target.value)}
      className={className}
    >
      <option value="">{label}</option>
      {!offered && current !== "" && <option value={current}>{current}</option>}
      {options.map((option) => (
        <option key={option} value={option}>
          {option}
        </option>
      ))}
    </select>
  )
}

type ItemDraft = { serviceType: string; serviceId: string }

type FormState = {
  name: string
  description: string
  serviceType: string
  accessDays: string
  items: ItemDraft[]
}

const EMPTY_FORM: FormState = {
  name: "",
  description: "",
  serviceType: "",
  accessDays: "",
  items: [{ serviceType: "", serviceId: "" }],
}

export default function AdminBundlesPage() {
  const t = useTranslations("bundlesAdmin")
  const [bundles, setBundles] = useState<Bundle[] | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)

  // null means the form is closed; otherwise it is creating, or editing this bundle id.
  const [editing, setEditing] = useState<string | null>(null)
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [formError, setFormError] = useState<string | null>(null)
  // The item list is loaded on its own because update replaces the whole set: an editor that
  // opened without it would delete every service in the bundle simply by saving.
  const [openingId, setOpeningId] = useState<string | null>(null)
  // Service types the platform sells, for the picker. Loaded separately from the bundle list so a
  // catalogue that cannot be read never stops an administrator from seeing or editing their
  // bundles - it only falls back to typing the name. null means "not known yet", not "empty".
  const [serviceTypes, setServiceTypes] = useState<string[] | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setBundles(await listInstitutionBundles())
    } catch (e) {
      setBundles(null)
      setError(e instanceof Error ? e.message : t("loadFailed"))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    let cancelled = false
    void listServiceTypeCatalogue()
      .then((names) => {
        if (!cancelled) setServiceTypes(names)
      })
      .catch(() => {
        // Left null on purpose: the picker degrades to a text field rather than showing a
        // catalogue of nothing, and the server still refuses any name it cannot price.
        if (!cancelled) setServiceTypes(null)
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  /**
   * Mirrors BundleAdminService.requireDistinctItems so the admin is told about a duplicate before
   * the round trip. The server still refuses it; this is only to avoid a pointless failure.
   */
  function validate(current: FormState): string | null {
    if (!current.name.trim()) return t("needName")
    if (!current.serviceType.trim()) return t("needServiceType")
    const cleaned = current.items.filter(
      (i) => i.serviceType.trim() !== "" && i.serviceId.trim() !== ""
    )
    if (cleaned.length === 0) {
      // A bundle with nothing in it could never grant access.
      return t("needItem")
    }
    const seen = new Set<string>()
    for (const item of cleaned) {
      const key = `${item.serviceType.trim()}:${item.serviceId.trim()}`
      if (seen.has(key)) return t("duplicateService", { serviceType: item.serviceType.trim() })
      seen.add(key)
    }
    // A malformed UUID would be rejected by the server as a bad request; say so here instead.
    const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
    if (cleaned.some((i) => !uuidPattern.test(i.serviceId.trim()))) return t("badServiceId")
    return null
  }

  function openCreate() {
    setForm(EMPTY_FORM)
    setFormError(null)
    setError(null)
    setNotice(null)
    setEditing("new")
  }

  async function openEdit(bundle: Bundle) {
    // The server refuses edits to anything but a draft, so the button is not offered either.
    if (bundle.status !== "DRAFT") return
    setError(null)
    setNotice(null)
    setFormError(null)
    setOpeningId(bundle.id)
    try {
      const detail = await getInstitutionBundle(bundle.id)
      setForm({
        name: detail.bundle.name,
        description: detail.bundle.description ?? "",
        serviceType: detail.bundle.serviceType,
        accessDays: detail.bundle.accessDays ? String(detail.bundle.accessDays) : "",
        items:
          detail.items.length > 0
            ? detail.items.map((item) => ({
                serviceType: item.serviceType,
                serviceId: item.serviceId,
              }))
            : [{ serviceType: "", serviceId: "" }],
      })
      setEditing(bundle.id)
    } catch (e) {
      // Without the items the form cannot be opened safely, so it is not opened at all.
      setError(e instanceof Error ? e.message : t("loadFailed"))
    } finally {
      setOpeningId(null)
    }
  }

  function removeItem(index: number) {
    setForm((f) => {
      const items = f.items.filter((_, i) => i !== index)
      // Never leave the editor with nothing to type into; one empty row is clearer than none.
      return { ...f, items: items.length > 0 ? items : [{ serviceType: "", serviceId: "" }] }
    })
  }

  function setItem(index: number, patch: Partial<ItemDraft>) {
    setForm((f) => ({
      ...f,
      items: f.items.map((item, i) => (i === index ? { ...item, ...patch } : item)),
    }))
  }

  async function submit() {
    const problem = validate(form)
    setFormError(problem)
    if (problem) return

    const payload = {
      name: form.name.trim(),
      description: form.description.trim() || undefined,
      serviceType: form.serviceType.trim(),
      accessDays: form.accessDays.trim() ? Number(form.accessDays) : null,
      items: form.items
        .filter((i) => i.serviceType.trim() && i.serviceId.trim())
        .map((i) => ({ serviceType: i.serviceType.trim(), serviceId: i.serviceId.trim() })),
    }

    setError(null)
    setNotice(null)
    setBusyId(editing ?? "new")
    try {
      if (editing === "new") {
        await createBundle(payload)
        setNotice(t("created"))
      } else if (editing) {
        await updateBundle(editing, payload)
        setNotice(t("updated"))
      }
      setEditing(null)
      setForm(EMPTY_FORM)
      await load()
    } catch (e) {
      // The server's refusal is the message worth showing.
      setError(e instanceof Error ? e.message : t("actionFailed"))
    } finally {
      setBusyId(null)
    }
  }

  async function act(bundle: Bundle, action: "publish" | "suspend") {
    setError(null)
    setNotice(null)
    setBusyId(bundle.id)
    try {
      if (action === "publish") {
        await publishBundle(bundle.id)
        setNotice(t("published", { name: bundle.name }))
      } else {
        await suspendBundle(bundle.id)
        setNotice(t("suspended", { name: bundle.name }))
      }
      await load()
    } catch (e) {
      // "A bundle needs an access duration" is more useful to an administrator than anything
      // invented client-side.
      setError(e instanceof Error ? e.message : t("actionFailed"))
    } finally {
      setBusyId(null)
    }
  }

  if (loading) {
    return (
      <div className="p-6">
        <p className="text-sm text-muted-foreground">{t("loading")}</p>
      </div>
    )
  }

  return (
    <div className="space-y-6 p-6">
      <header>
        <h1 className="text-xl font-semibold text-foreground">{t("title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("subtitle")}</p>
      </header>

      <div className="flex justify-end">
        <button
          type="button"
          onClick={openCreate}
          className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground hover:bg-muted"
        >
          {t("newButton")}
        </button>
      </div>

      {editing !== null && (
        <section className="rounded-2xl border border-border bg-card p-5">
          <h2 className="text-base font-semibold text-foreground">
            {editing === "new" ? t("newTitle") : t("editTitle")}
          </h2>
          <p className="mt-1 text-sm text-muted-foreground">{t("newHint")}</p>

          {formError && (
            <p role="alert" className="mt-3 text-sm text-foreground">
              {formError}
            </p>
          )}

          <div className="mt-4 grid gap-3 sm:grid-cols-2">
            <Field label={t("name")} id="bundle-name">
              <input
                id="bundle-name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                className="mt-1 w-full rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
              />
            </Field>
            <Field label={t("serviceType")} id="bundle-service-type">
              <ServiceTypeControl
                id="bundle-service-type"
                options={serviceTypes}
                value={form.serviceType}
                onChange={(next) => setForm({ ...form, serviceType: next })}
                label={t("serviceType")}
              />
            </Field>
            <Field label={t("description")} id="bundle-description">
              <input
                id="bundle-description"
                value={form.description}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                className="mt-1 w-full rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
              />
            </Field>
            <Field label={t("accessDays")} id="bundle-access-days">
              <input
                id="bundle-access-days"
                type="number"
                min={1}
                value={form.accessDays}
                onChange={(e) => setForm({ ...form, accessDays: e.target.value })}
                className="mt-1 w-full rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
              />
            </Field>
          </div>

          <fieldset className="mt-4">
            <legend className="text-sm font-medium text-foreground">{t("items")}</legend>
            {editing !== "new" && (
              <p className="mt-1 text-xs text-muted-foreground">{t("editReplacesItems")}</p>
            )}
            <ul className="mt-2 space-y-2">
              {form.items.map((item, index) => (
                <li
                  key={index}
                  className="grid gap-2 sm:grid-cols-[1fr_1fr_auto] sm:items-center"
                >
                  <ServiceTypeControl
                    inline
                    options={serviceTypes}
                    value={item.serviceType}
                    onChange={(next) => setItem(index, { serviceType: next })}
                    label={t("itemServiceType")}
                    placeholder={t("itemServiceType")}
                  />
                  <input
                    aria-label={t("itemServiceId")}
                    placeholder={t("itemServiceId")}
                    value={item.serviceId}
                    onChange={(e) => setItem(index, { serviceId: e.target.value })}
                    className="w-full rounded-lg border border-input bg-background px-2.5 py-1.5 text-sm"
                  />
                  {form.items.length > 1 && (
                    <button
                      type="button"
                      aria-label={t("removeItem")}
                      onClick={() => removeItem(index)}
                      className="rounded-lg border border-border px-2 py-1.5 text-sm text-muted-foreground hover:bg-muted"
                    >
                      &times;
                    </button>
                  )}
                </li>
              ))}
            </ul>
            <button
              type="button"
              onClick={() =>
                setForm({ ...form, items: [...form.items, { serviceType: "", serviceId: "" }] })
              }
              className="mt-2 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground hover:bg-muted"
            >
              {t("addItem")}
            </button>
          </fieldset>

          <div className="mt-4 flex gap-2">
            <button
              type="button"
              disabled={busyId !== null}
              onClick={() => void submit()}
              className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground disabled:opacity-50 hover:bg-muted"
            >
              {busyId ? t("working") : editing === "new" ? t("create") : t("save")}
            </button>
            <button
              type="button"
              onClick={() => setEditing(null)}
              className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-muted-foreground hover:bg-muted"
            >
              {t("cancel")}
            </button>
          </div>
        </section>
      )}

      {error && (
        <section className="rounded-2xl border border-border bg-card p-5">
          <p className="text-sm text-foreground">{error}</p>
          <button
            type="button"
            onClick={() => void load()}
            className="mt-3 rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground hover:bg-muted"
          >
            {t("retry")}
          </button>
        </section>
      )}

      {notice && !error && (
        <p className="text-sm text-muted-foreground">{notice}</p>
      )}

      {!error && (bundles?.length ?? 0) === 0 && (
        <section className="rounded-2xl border border-dashed border-border bg-card p-5">
          <h2 className="text-base font-semibold text-foreground">{t("emptyTitle")}</h2>
          <p className="mt-1 text-sm text-muted-foreground">{t("emptyBody")}</p>
        </section>
      )}

      {!error && (bundles?.length ?? 0) > 0 && (
        <ul className="space-y-3">
          {bundles!.map((bundle) => (
            <li key={bundle.id} className="rounded-2xl border border-border bg-card p-5">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h2 className="text-base font-semibold text-foreground">{bundle.name}</h2>
                  {bundle.description && (
                    <p className="mt-1 text-sm text-muted-foreground">{bundle.description}</p>
                  )}
                  <p className="mt-1 text-xs text-muted-foreground">
                    {t("serviceType")}: {bundle.serviceType} &middot;{" "}
                    {t("duration")}:{" "}
                    {bundle.accessDays
                      ? t("days", { count: bundle.accessDays })
                      : t("durationUnset")}
                  </p>
                </div>
                <span className="rounded-full bg-muted px-3 py-1 text-xs text-muted-foreground">
                  {t(`status.${bundle.status}`)}
                </span>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                {bundle.status === "DRAFT" && (
                  <>
                    <button
                      type="button"
                      disabled={busyId === bundle.id || openingId === bundle.id}
                      onClick={() => void openEdit(bundle)}
                      className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground disabled:opacity-50 hover:bg-muted"
                    >
                      {openingId === bundle.id ? t("opening") : t("edit")}
                    </button>
                    <button
                      type="button"
                      disabled={busyId === bundle.id}
                      onClick={() => void act(bundle, "publish")}
                      className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground disabled:opacity-50 hover:bg-muted"
                    >
                      {busyId === bundle.id ? t("working") : t("publish")}
                    </button>
                  </>
                )}
                {bundle.status === "PUBLISHED" && (
                  <button
                    type="button"
                    disabled={busyId === bundle.id}
                    onClick={() => void act(bundle, "suspend")}
                    className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium text-foreground disabled:opacity-50 hover:bg-muted"
                  >
                    {busyId === bundle.id ? t("working") : t("suspend")}
                  </button>
                )}
                {/* No delete. A bundle that has been bought cannot be unbought, and removing the
                    row would orphan entitlements learners already hold. Suspension is the way to
                    stop it being sold. */}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}