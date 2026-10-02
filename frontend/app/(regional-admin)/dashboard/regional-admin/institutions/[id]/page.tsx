"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import {
  AlertTriangle, ArrowUpRight, BadgeCheck, ShieldCheck,
} from "lucide-react"
import { useTranslations } from "next-intl"
import { regionalAdminApi, type InstitutionGovernance } from "@/lib/regional-admin-api"
import {
  Chip, EmptyState, ErrorState, LoadingState, PageHeader, Panel, StatCard,
} from "@/components/dashboard/regional-admin/ui"

function errMsg(e: unknown, fallback: string): string {
  return e instanceof Error ? e.message : fallback
}

function isAccessError(e: unknown): boolean {
  return e instanceof Error && /API (403|404)/.test(e.message)
}

function pct(value: number | null): string {
  return value === null || value === undefined ? "—" : `${value.toFixed(1)}%`
}

function valueOr(value: string | null, fallback: string): string {
  return value && value.trim() ? value : fallback
}

function verificationTone(status: string): "success" | "warning" | "danger" | "muted" {
  if (status === "APPROVED") return "success"
  if (status === "PENDING") return "warning"
  if (status === "REJECTED") return "danger"
  return "muted"
}

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{label}</p>
      <p className="mt-1 text-sm text-foreground">{value}</p>
    </div>
  )
}

function AnalyticsLink({ href, title, hint }: { href: string; title: string; hint: string }) {
  return (
    <Link
      href={href}
      className="flex items-start justify-between gap-3 rounded-xl border border-border bg-background p-4 transition-colors hover:bg-muted"
    >
      <span className="min-w-0">
        <span className="block text-sm font-semibold text-foreground">{title}</span>
        <span className="mt-0.5 block text-xs text-muted-foreground">{hint}</span>
      </span>
      <ArrowUpRight className="size-4 shrink-0 text-muted-foreground" />
    </Link>
  )
}

export default function RegionalInstitutionGovernancePage() {
  const t = useTranslations("regionalAdmin")
  const ts = useTranslations("status")
  const params = useParams() as { id: string }
  const id = params?.id ?? ""

  const [institution, setInstitution] = useState<InstitutionGovernance | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    if (!id) {
      setInstitution(null)
      setError(t("institutionDetail.notFoundError"))
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const data = await regionalAdminApi.getInstitutionGovernance(id)
      setInstitution(data)
    } catch (e) {
      setInstitution(null)
      if (isAccessError(e)) {
        setError(t("institutionDetail.accessError"))
      } else {
        setError(errMsg(e, t("institutionDetail.loadError")))
      }
    } finally {
      setLoading(false)
    }
  }, [id, t])

  useEffect(() => {
    load()
  }, [load])

  if (loading) {
    return (
      <div className="space-y-6 pb-8">
        <PageHeader title={t("institutionDetail.loadingTitle")} description={t("institutionDetail.loadingDescription")} />
        <LoadingState label={t("institutionDetail.loadingLabel")} />
      </div>
    )
  }

  if (error || !institution) {
    return (
      <div className="space-y-6 pb-8">
        <PageHeader title={t("institutionDetail.errorTitle")} description={t("institutionDetail.errorDescription")} />
        <ErrorState
          message={error ?? t("institutionDetail.loadError")}
          onRetry={load}
        />
        <div className="flex justify-start">
          <Link
            href="/dashboard/regional-admin/institutions"
            className="text-sm font-medium text-primary hover:underline"
          >
            {t("institutionDetail.backLink")}
          </Link>
        </div>
      </div>
    )
  }

  const issues = institution.dataQualityIssues ?? []

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title={institution.name}
        actions={
          <Link
            href="/dashboard/regional-admin/institutions"
            className="inline-flex items-center gap-1 rounded-lg border border-border px-3 py-2 text-sm font-medium text-foreground hover:bg-muted"
          >
            {t("institutionDetail.backLink")}
          </Link>
        }
      />

      <div className="flex flex-wrap items-center gap-2">
        <Chip tone="info">{institution.code}</Chip>
        <Chip tone="default">{institution.type}</Chip>
        <Chip tone={institution.isActive ? "success" : "danger"}>
          {institution.isActive ? ts("active") : ts("inactive")}
        </Chip>
        {institution.regionName && <Chip tone="muted">{institution.regionName}</Chip>}
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label={t("institutionDetail.statTeachers")} value={institution.teacherCount} />
        <StatCard label={t("institutionDetail.statLearners")} value={institution.learnerCount} />
        <StatCard label={t("institutionDetail.statClasses")} value={institution.classCount} />
        <StatCard label={t("institutionDetail.statLessons")} value={institution.lessonCount} />
        <StatCard label={t("institutionDetail.statLiveClasses")} value={institution.liveClassCount} />
        <StatCard label={t("institutionDetail.statAttendance")} value={pct(institution.attendanceRate)} />
        <StatCard label={t("institutionDetail.statPerformance")} value={pct(institution.averagePerformance)} />
        <StatCard label={t("institutionDetail.statCurriculum")} value={pct(institution.curriculumProgress)} />
      </div>

      <Panel title={t("institutionDetail.jurisdictionTitle")}>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Field label={t("institutionDetail.fieldDistrict")} value={valueOr(institution.districtName, t("institutionDetail.notProvided"))} />
          <Field label={t("institutionDetail.fieldRegion")} value={valueOr(institution.regionName, t("institutionDetail.notProvided"))} />
          <Field label={t("institutionDetail.fieldEmail")} value={valueOr(institution.contactEmail, t("institutionDetail.notProvided"))} />
          <Field label={t("institutionDetail.fieldPhone")} value={valueOr(institution.contactPhone, t("institutionDetail.notProvided"))} />
          <Field label={t("institutionDetail.fieldAddress")} value={valueOr(institution.address, t("institutionDetail.notProvided"))} />
          <Field label={t("institutionDetail.fieldCity")} value={valueOr(institution.city, t("institutionDetail.notProvided"))} />
        </div>
      </Panel>

      <div className="grid gap-6 lg:grid-cols-2">
        <Panel
          title={t("institutionDetail.verificationTitle")}
          actions={
            <Link
              href="/dashboard/regional-admin/governance/verification"
              className="text-xs font-semibold text-primary hover:underline"
            >
              {t("institutionDetail.openVerificationQueue")}
            </Link>
          }
        >
          <div className="flex items-center gap-3 rounded-xl border border-border bg-background p-4">
            <BadgeCheck className="size-5 shrink-0 text-muted-foreground" />
            <div className="min-w-0">
              <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">{t("institutionDetail.statusLabel")}</p>
              <div className="mt-1">
                <Chip tone={verificationTone(institution.verificationStatus)}>
                  {institution.verificationStatus || t("institutionDetail.unknownStatus")}
                </Chip>
              </div>
            </div>
          </div>
          <p className="mt-3 text-xs text-muted-foreground">
            {t("institutionDetail.verificationHint")}
          </p>
        </Panel>

        <Panel title={t("institutionDetail.dataQualityTitle")}>
          {issues.length === 0 ? (
            <EmptyState
              icon={<ShieldCheck className="size-10" />}
              title={t("institutionDetail.noFindingsTitle")}
              hint={t("institutionDetail.noFindingsHint")}
            />
          ) : (
            <ul className="space-y-2">
              {issues.map((issue, i) => (
                <li
                  key={`${issue}-${i}`}
                  className="flex items-start gap-3 rounded-xl border border-amber-500/20 bg-amber-500/5 p-3"
                >
                  <AlertTriangle className="mt-0.5 size-4 shrink-0 text-amber-600" />
                  <span className="text-sm text-foreground">{issue}</span>
                </li>
              ))}
            </ul>
          )}
        </Panel>
      </div>

      <Panel title={t("institutionDetail.analyticsTitle")}>
        <div className="grid gap-3 sm:grid-cols-3">
          <AnalyticsLink
            href={`/oversight/schools/${institution.id}`}
            title={t("institutionDetail.analyticsProfileTitle")}
            hint={t("institutionDetail.analyticsProfileHint")}
          />
          <AnalyticsLink
            href="/oversight/performance"
            title={t("institutionDetail.analyticsPerformanceTitle")}
            hint={t("institutionDetail.analyticsPerformanceHint")}
          />
          <AnalyticsLink
            href="/oversight/attendance"
            title={t("institutionDetail.analyticsAttendanceTitle")}
            hint={t("institutionDetail.analyticsAttendanceHint")}
          />
        </div>
        <p className="mt-4 text-xs text-muted-foreground">
          {t("institutionDetail.analyticsFootnote")}
        </p>
      </Panel>
    </div>
  )
}
