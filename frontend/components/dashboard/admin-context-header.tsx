"use client"

import { useEffect, useRef, useState } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import {
  AlertCircle,
  Building2,
  Check,
  ChevronDown,
  KeyRound,
  Layers,
  Loader2,
  RefreshCw,
} from "lucide-react"
import {
  adminApi,
  ApiRequestError,
  getInstitutionId,
  setInstitutionId,
  type MyAccessResponse,
} from "@/lib/api"
import { useToast } from "@/components/toast"
import { cn } from "@/lib/utils"

function scopeLabelKey(scopeType: string | null | undefined): string {
  if (scopeType === "DEPARTMENT") return "scopeDepartment"
  if (scopeType === "CAMPUS") return "scopeCampus"
  return "scopeInstitution"
}

export function AdminContextHeader() {
  const t = useTranslations("admin")
  const tc = useTranslations("common")
  const { toast } = useToast()

  const [access, setAccess] = useState<MyAccessResponse | null>(null)
  const [enabledServices, setEnabledServices] = useState<string[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [switcherOpen, setSwitcherOpen] = useState(false)
  const [switching, setSwitching] = useState(false)
  const switcherRef = useRef<HTMLDivElement>(null)

  const load = () => {
    const institutionId = getInstitutionId()
    setLoading(true)
    setError(null)
    setSwitcherOpen(false)

    const accessRequest = adminApi.getMyAccess()
    const servicesRequest = institutionId
      ? adminApi.getEnabledServices(institutionId).catch(() => [] as string[])
      : Promise.resolve([] as string[])

    Promise.all([accessRequest, servicesRequest])
      .then(([accessData, services]) => {
        setAccess(accessData)
        setEnabledServices((services || []).map((s) => s.trim()).filter(Boolean))
      })
      .catch((err) =>
        setError(err instanceof Error ? err.message : t("context.failedToLoad")),
      )
      .finally(() => setLoading(false))
  }

  useEffect(load, [])

  useEffect(() => {
    if (!switcherOpen) return
    function onPointerDown(event: MouseEvent) {
      if (switcherRef.current && !switcherRef.current.contains(event.target as Node)) {
        setSwitcherOpen(false)
      }
    }
    document.addEventListener("mousedown", onPointerDown)
    return () => document.removeEventListener("mousedown", onPointerDown)
  }, [switcherOpen])

  async function switchOrganization(target: NonNullable<MyAccessResponse["organizations"][number]>) {
    if (target.current || switching) return
    setSwitching(true)
    try {
      await adminApi.verifyInstitutionAccess(target.id)
      setInstitutionId(target.id)
      window.location.reload()
    } catch (err) {
      const status = err instanceof ApiRequestError ? err.status : 0
      toast(
        status === 403 ? t("context.switchForbidden") : t("context.switchFailed"),
        "error",
      )
      setSwitching(false)
      setSwitcherOpen(false)
    }
  }

  const scopeType = access?.scope?.type ?? "INSTITUTION"
  const currentOrg = access?.organizations.find((o) => o.current) ?? null
  const organizations = access?.organizations ?? []
  const multiOrg = organizations.length > 1

  return (
    <section
      aria-label={t("context.organizationContext")}
      className="rounded-2xl border border-border bg-card p-4 shadow-xs sm:p-5"
    >
      {loading && (
        <div className="flex items-center gap-3 text-sm text-muted-foreground">
          <Loader2 className="size-4 animate-spin" />
          {tc("loading")}
        </div>
      )}

      {error && !loading && (
        <div className="flex flex-wrap items-center gap-3">
          <AlertCircle className="size-4 shrink-0 text-destructive" />
          <p className="min-w-0 flex-1 text-sm text-destructive">{error}</p>
          <button
            type="button"
            onClick={load}
            className="inline-flex h-8 items-center gap-1.5 rounded-lg border border-destructive/30 px-3 text-xs font-medium text-destructive hover:bg-destructive/10"
          >
            <RefreshCw className="size-3.5" />
            {tc("retry")}
          </button>
        </div>
      )}

      {!loading && !error && access && (
        <>
          <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div className="flex min-w-0 items-start gap-3">
              {currentOrg?.logoUrl ? (
                // eslint-disable-next-line @next/next/no-img-element
                <img
                  src={currentOrg.logoUrl}
                  alt=""
                  className="size-11 shrink-0 rounded-xl border border-border object-cover"
                />
              ) : (
                <span className="flex size-11 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
                  <Building2 className="size-5" />
                </span>
              )}
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2">
                  <h1 className="truncate text-lg font-semibold tracking-tight text-foreground">
                    {access.institutionName || t("context.unnamedOrganization")}
                  </h1>
                  <span
                    className={cn(
                      "inline-flex items-center rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide",
                      access.institutionActive
                        ? "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                        : "bg-amber-500/10 text-amber-600 dark:text-amber-400",
                    )}
                  >
                    {access.institutionActive ? t("context.active") : t("context.inactive")}
                  </span>
                </div>
                <div className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
                  <span className="inline-flex items-center gap-1">
                    <Building2 className="size-3.5" />
                    {access.institutionType || t("context.typeUnknown")}
                  </span>
                  <span aria-hidden="true">·</span>
                  <span className="inline-flex items-center gap-1">
                    <Layers className="size-3.5" />
                    {t(`context.${scopeLabelKey(scopeType)}`)}
                  </span>
                  <span aria-hidden="true">·</span>
                  <span>
                    {access.institutionId ? (
                      <code className="text-[11px]">{access.institutionId}</code>
                    ) : (
                      t("context.noOrganizationId")
                    )}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-muted/60 px-2.5 py-1.5 text-xs font-medium text-foreground">
                <KeyRound className="size-3.5 text-muted-foreground" />
                {access.membershipRole || access.systemRole}
              </span>

              <Link
                href="/dashboard/admin/access"
                className="inline-flex h-8 items-center gap-1.5 rounded-lg border border-border px-3 text-xs font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
              >
                {t("context.viewAccess")}
              </Link>

              {multiOrg && (
                <div className="relative" ref={switcherRef}>
                  <button
                    type="button"
                    onClick={() => setSwitcherOpen((v) => !v)}
                    disabled={switching}
                    aria-expanded={switcherOpen}
                    className="inline-flex h-8 items-center gap-1.5 rounded-lg border border-border px-3 text-xs font-medium text-foreground transition-colors hover:bg-muted disabled:opacity-60"
                  >
                    {switching ? (
                      <Loader2 className="size-3.5 animate-spin" />
                    ) : (
                      <ChevronDown className="size-3.5" />
                    )}
                    {t("context.switchOrganization")}
                  </button>

                  {switcherOpen && (
                    <div className="absolute right-0 z-50 mt-2 w-72 rounded-xl border border-border bg-card p-1 shadow-lg">
                      <p className="px-3 py-2 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
                        {t("context.yourOrganizations")}
                      </p>
                      {organizations.map((org) => (
                        <button
                          key={org.id}
                          type="button"
                          onClick={() => switchOrganization(org)}
                          disabled={switching || org.current}
                          className={cn(
                            "flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left text-sm transition-colors",
                            org.current
                              ? "bg-primary/10 text-primary"
                              : "text-muted-foreground hover:bg-muted hover:text-foreground",
                            switching && !org.current && "opacity-60",
                          )}
                        >
                          <span className="min-w-0 flex-1">
                            <span className="block truncate font-medium">{org.name}</span>
                            <span className="block truncate text-[11px] text-muted-foreground">
                              {org.type || t("context.typeUnknown")}
                              {org.active ? "" : ` · ${t("context.inactive")}`}
                            </span>
                          </span>
                          {org.current && <Check className="size-4 shrink-0" />}
                        </button>
                      ))}
                      <p className="border-t border-border px-3 py-2 text-[11px] leading-relaxed text-muted-foreground">
                        {t("context.switchHint")}
                      </p>
                    </div>
                  )}
                </div>
              )}
            </div>
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-1.5 border-t border-border pt-3">
            <span className="mr-1 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
              {t("context.enabledServices")}
            </span>
            {enabledServices.length === 0 ? (
              <span className="text-xs text-muted-foreground">{t("context.noServicesEnabled")}</span>
            ) : (
              <>
                {enabledServices.slice(0, 8).map((service) => (
                  <span
                    key={service}
                    className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-[11px] font-medium text-primary"
                  >
                    {service}
                  </span>
                ))}
                {enabledServices.length > 8 && (
                  <span className="text-[11px] text-muted-foreground">
                    {t("context.moreServices", { count: enabledServices.length - 8 })}
                  </span>
                )}
                <Link
                  href="/dashboard/admin/services"
                  className="ml-auto text-[11px] font-medium text-primary hover:underline"
                >
                  {t("context.manageServices")}
                </Link>
              </>
            )}
          </div>
        </>
      )}
    </section>
  )
}
