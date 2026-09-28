"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { useTranslations } from "next-intl"
import {
  Building2,
  ChevronRight,
  GraduationCap,
  KeyRound,
  Layers,
  Loader2,
  RefreshCw,
  ShieldCheck,
  Users,
} from "lucide-react"
import { adminApi, type MyAccessResponse } from "@/lib/api"
import { collegeApi } from "@/lib/college-api"
import type { Department, Programme } from "@/lib/types/college"
import { cn } from "@/lib/utils"

function humanize(value: string | null | undefined): string {
  if (!value) return "—"
  return value
    .toLowerCase()
    .split("_")
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(" ")
}

function scopeLabelKey(scopeType: string | null | undefined): string {
  if (scopeType === "DEPARTMENT") return "scopeDepartment"
  if (scopeType === "CAMPUS") return "scopeCampus"
  return "scopeInstitution"
}

function InfoCard({
  icon: Icon,
  label,
  children,
}: {
  icon: typeof ShieldCheck
  label: string
  children: React.ReactNode
}) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-center gap-2">
        <span className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-primary">
          <Icon className="size-4" />
        </span>
        <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">{label}</p>
      </div>
      <div className="mt-3 text-sm text-foreground">{children}</div>
    </div>
  )
}

export default function AdminAccessPage() {
  const t = useTranslations("admin")
  const tc = useTranslations("common")

  const [access, setAccess] = useState<MyAccessResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [programmes, setProgrammes] = useState<Programme[] | null>(null)
  const [departments, setDepartments] = useState<Department[] | null>(null)
  const [scopeLoading, setScopeLoading] = useState(false)
  const [scopeError, setScopeError] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    adminApi
      .getMyAccess()
      .then(setAccess)
      .catch((err) =>
        setError(err instanceof Error ? err.message : t("access.failedToLoad")),
      )
      .finally(() => setLoading(false))
  }, [t])

  useEffect(() => {
    load()
  }, [load])

  const scopeType = access?.scope?.type ?? "INSTITUTION"

  useEffect(() => {
    if (!access) return
    if (scopeType !== "INSTITUTION" && scopeType !== "DEPARTMENT") return
    let cancelled = false
    setScopeLoading(true)
    setScopeError(null)

    const wantsProgrammes = scopeType === "INSTITUTION"
    const wantsDepartments = true

    Promise.all([
      wantsProgrammes
        ? collegeApi.listProgrammes().catch(() => null)
        : Promise.resolve(null),
      wantsDepartments
        ? collegeApi.listDepartments().catch(() => null)
        : Promise.resolve(null),
    ])
      .then(([programmeRes, departmentRes]) => {
        if (cancelled) return
        if (programmeRes) setProgrammes((programmeRes.data as Programme[] | undefined) ?? [])
        if (departmentRes) setDepartments((departmentRes.data as Department[] | undefined) ?? [])
        if ((wantsProgrammes && !programmeRes) || !departmentRes) {
          setScopeError(t("access.scopeUnavailable"))
        }
      })
      .catch(() => {
        if (!cancelled) setScopeError(t("access.scopeUnavailable"))
      })
      .finally(() => {
        if (!cancelled) setScopeLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [access, scopeType, t])

  const currentDepartment =
    scopeType === "DEPARTMENT" && access?.scope?.id
      ? (departments ?? []).find((d) => d.id === access.scope?.id) ?? null
      : null

  const permissions = access?.permissions ?? []
  const wildcard = permissions.includes("*")
  const delegations = access?.delegations ?? []
  const organizations = access?.organizations ?? []
  const membershipStatus = access?.membershipStatus ?? "—"

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("access.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("access.subtitle")}</p>
      </div>

      {loading && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {error && !loading && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
          <p className="text-sm font-medium text-destructive">{error}</p>
          <button
            type="button"
            onClick={load}
            className="mt-4 inline-flex h-9 items-center gap-2 rounded-lg border border-destructive/30 px-4 text-sm font-medium text-destructive hover:bg-destructive/10"
          >
            <RefreshCw className="size-4" />
            {tc("retry")}
          </button>
        </div>
      )}

      {!loading && !error && access && (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <InfoCard icon={ShieldCheck} label={t("access.currentRole")}>
              <p className="font-semibold">{humanize(access.membershipRole || access.systemRole)}</p>
              <p className="mt-1 text-xs text-muted-foreground">
                {t("access.systemRole")}: {humanize(access.systemRole)}
              </p>
            </InfoCard>

            <InfoCard icon={Building2} label={t("access.currentOrganization")}>
              <p className="truncate font-semibold">
                {access.institutionName || t("access.unnamedOrganization")}
              </p>
              <p className="mt-1 truncate text-xs text-muted-foreground">
                {humanize(access.institutionType)}
                {access.institutionId ? ` · ${access.institutionId}` : ""}
              </p>
            </InfoCard>

            <InfoCard icon={Users} label={t("access.membershipStatus")}>
              <span
                className={cn(
                  "inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold",
                  access.membershipActive
                    ? "bg-emerald-500/10 text-emerald-600 dark:text-emerald-400"
                    : "bg-amber-500/10 text-amber-600 dark:text-amber-400",
                )}
              >
                {humanize(membershipStatus)}
              </span>
              <p className="mt-1 truncate text-xs text-muted-foreground">{access.userEmail}</p>
            </InfoCard>

            <InfoCard icon={Layers} label={t("access.currentScope")}>
              <p className="font-semibold">{t(`access.${scopeLabelKey(scopeType)}`)}</p>
              <p className="mt-1 truncate text-xs text-muted-foreground">
                {access.scope?.id ?? t("access.wholeOrganization")}
              </p>
            </InfoCard>
          </div>

          {/* §012 My Scope — effective authority, only reachable levels shown */}
          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <div className="mb-4 flex items-center gap-2">
              <Layers className="size-4 text-muted-foreground" />
              <div>
                <h2 className="text-sm font-semibold text-foreground">{t("access.myScope")}</h2>
                <p className="text-xs text-muted-foreground">{t("access.myScopeSubtitle")}</p>
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-2 text-sm">
              <span className="inline-flex items-center gap-2 rounded-lg border border-border bg-muted/60 px-3 py-2 font-medium text-foreground">
                <Building2 className="size-4 text-muted-foreground" />
                {access.institutionName || t("access.unnamedOrganization")}
              </span>
              <ChevronRight className="size-4 text-muted-foreground" />

              {scopeType === "INSTITUTION" && (
                <span className="inline-flex items-center gap-2 rounded-lg border border-border px-3 py-2 text-muted-foreground">
                  <Layers className="size-4" />
                  {t("access.scopeInstitution")}
                </span>
              )}

              {scopeType === "DEPARTMENT" && (
                <span className="inline-flex items-center gap-2 rounded-lg border border-border px-3 py-2 text-muted-foreground">
                  <Users className="size-4" />
                  {currentDepartment?.name || t("access.scopeDepartment")}
                </span>
              )}

              {scopeType === "CAMPUS" && (
                <span className="inline-flex items-center gap-2 rounded-lg border border-border px-3 py-2 text-muted-foreground">
                  <Building2 className="size-4" />
                  {t("access.scopeCampus")}
                </span>
              )}
            </div>

            {scopeType === "INSTITUTION" && (
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                <ScopeRow
                  icon={GraduationCap}
                  label={t("access.programmes")}
                  href="/dashboard/admin/programmes"
                  loading={scopeLoading}
                  error={scopeError}
                  count={programmes?.length ?? null}
                />
                <ScopeRow
                  icon={Users}
                  label={t("access.departments")}
                  href="/dashboard/admin/departments"
                  loading={scopeLoading}
                  error={scopeError}
                  count={departments?.length ?? null}
                />
              </div>
            )}

            {scopeType !== "INSTITUTION" && (
              <p className="mt-4 text-xs text-muted-foreground">{t("access.narrowScopeNote")}</p>
            )}
          </div>

          {/* Effective permissions */}
          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <div className="mb-4 flex items-center gap-2">
              <KeyRound className="size-4 text-muted-foreground" />
              <div>
                <h2 className="text-sm font-semibold text-foreground">{t("access.effectivePermissions")}</h2>
                <p className="text-xs text-muted-foreground">
                  {t("access.permissionCount", { count: permissions.length })}
                </p>
              </div>
            </div>

            {permissions.length === 0 ? (
              <p className="rounded-xl border border-border bg-muted/40 px-4 py-6 text-center text-sm text-muted-foreground">
                {t("access.noPermissions")}
              </p>
            ) : wildcard ? (
              <span className="inline-flex items-center rounded-full bg-primary/10 px-3 py-1.5 text-xs font-semibold text-primary">
                {t("access.allPermissions")}
              </span>
            ) : (
              <div className="flex flex-wrap gap-2">
                {permissions.map((permission) => (
                  <span
                    key={permission}
                    className="inline-flex items-center rounded-full border border-border bg-muted/60 px-2.5 py-1 font-mono text-[11px] text-foreground"
                  >
                    {permission}
                  </span>
                ))}
              </div>
            )}
          </div>

          {/* Delegated responsibilities */}
          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <div className="mb-4 flex items-center gap-2">
              <ShieldCheck className="size-4 text-muted-foreground" />
              <div>
                <h2 className="text-sm font-semibold text-foreground">{t("access.delegatedResponsibilities")}</h2>
                <p className="text-xs text-muted-foreground">{t("access.delegatedSubtitle")}</p>
              </div>
            </div>

            {delegations.length === 0 ? (
              <p className="rounded-xl border border-border bg-muted/40 px-4 py-6 text-center text-sm text-muted-foreground">
                {t("access.noDelegations")}
              </p>
            ) : (
              <div className="space-y-3">
                {delegations.map((delegation) => (
                  <div
                    key={delegation.id}
                    className="rounded-xl border border-border px-4 py-3"
                  >
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <p className="text-sm font-medium text-foreground">
                        {humanize(delegation.scope)}
                      </p>
                      <span className="inline-flex items-center rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary">
                        {humanize(delegation.status)}
                      </span>
                    </div>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {t("access.validity")}: {delegation.startsAt ? new Date(delegation.startsAt).toLocaleDateString() : "—"}
                      {" → "}
                      {delegation.expiresAt ? new Date(delegation.expiresAt).toLocaleDateString() : t("access.noExpiry")}
                    </p>
                    {(delegation.permissions ?? []).length > 0 && (
                      <div className="mt-2 flex flex-wrap gap-1.5">
                        {(delegation.permissions ?? []).map((permission) => (
                          <span
                            key={permission}
                            className="rounded-full border border-border bg-muted/60 px-2 py-0.5 font-mono text-[10px] text-muted-foreground"
                          >
                            {permission}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Organizations the caller can reach */}
          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <div className="mb-4 flex items-center gap-2">
              <Building2 className="size-4 text-muted-foreground" />
              <div>
                <h2 className="text-sm font-semibold text-foreground">{t("access.yourOrganizations")}</h2>
                <p className="text-xs text-muted-foreground">{t("access.organizationsSubtitle")}</p>
              </div>
            </div>

            {organizations.length === 0 ? (
              <p className="rounded-xl border border-border bg-muted/40 px-4 py-6 text-center text-sm text-muted-foreground">
                {t("access.noOrganizations")}
              </p>
            ) : (
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {organizations.map((org) => (
                  <div
                    key={org.id}
                    className={cn(
                      "rounded-xl border px-4 py-3",
                      org.current ? "border-primary/40 bg-primary/5" : "border-border",
                    )}
                  >
                    <div className="flex items-center justify-between gap-2">
                      <p className="truncate text-sm font-medium text-foreground">{org.name}</p>
                      {org.current && (
                        <span className="shrink-0 rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-semibold uppercase text-primary">
                          {t("access.current")}
                        </span>
                      )}
                    </div>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {humanize(org.type)} · {org.active ? t("access.active") : t("access.inactive")}
                    </p>
                  </div>
                ))}
              </div>
            )}
            {organizations.length > 1 && (
              <p className="mt-3 text-xs text-muted-foreground">{t("access.switchHint")}</p>
            )}
          </div>
        </>
      )}
    </div>
  )
}

function ScopeRow({
  icon: Icon,
  label,
  href,
  loading,
  error,
  count,
}: {
  icon: typeof GraduationCap
  label: string
  href: string
  loading: boolean
  error: string | null
  count: number | null
}) {
  const t = useTranslations("admin")
  const tc = useTranslations("common")

  return (
    <Link
      href={href}
      className="flex items-center gap-3 rounded-xl border border-border px-4 py-3 transition-colors hover:border-primary/30 hover:bg-muted/60"
    >
      <span className="flex size-9 items-center justify-center rounded-lg bg-primary/10 text-primary">
        <Icon className="size-4" />
      </span>
      <span className="min-w-0 flex-1">
        <span className="block truncate text-sm font-medium text-foreground">{label}</span>
        <span className="block truncate text-xs text-muted-foreground">
          {loading ? (
            tc("loading")
          ) : error ? (
            error
          ) : count === null ? (
            t("access.countUnavailable")
          ) : (
            t("access.itemCount", { count })
          )}
        </span>
      </span>
      <ChevronRight className="size-4 shrink-0 text-muted-foreground" />
    </Link>
  )
}
