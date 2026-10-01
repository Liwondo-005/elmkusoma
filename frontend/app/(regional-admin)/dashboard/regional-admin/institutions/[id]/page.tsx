"use client"

import { useCallback, useEffect, useState } from "react"
import Link from "next/link"
import { useParams } from "next/navigation"
import {
  AlertTriangle, ArrowUpRight, BadgeCheck, ShieldCheck,
} from "lucide-react"
import { regionalAdminApi, type InstitutionGovernance } from "@/lib/regional-admin-api"
import {
  Chip, EmptyState, ErrorState, LoadingState, PageHeader, Panel, StatCard,
} from "@/components/dashboard/regional-admin/ui"

function errMsg(e: unknown): string {
  return e instanceof Error ? e.message : "Unable to load this institution."
}

function isAccessError(e: unknown): boolean {
  return e instanceof Error && /API (403|404)/.test(e.message)
}

function pct(value: number | null): string {
  return value === null || value === undefined ? "—" : `${value.toFixed(1)}%`
}

function valueOr(value: string | null): string {
  return value && value.trim() ? value : "Not provided"
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
  const params = useParams() as { id: string }
  const id = params?.id ?? ""

  const [institution, setInstitution] = useState<InstitutionGovernance | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    if (!id) {
      setInstitution(null)
      setError("This institution could not be found.")
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
        setError(
          "This institution is outside your jurisdiction or does not exist. Contact your administrator if you believe this is a mistake.",
        )
      } else {
        setError(errMsg(e))
      }
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  if (loading) {
    return (
      <div className="space-y-6 pb-8">
        <PageHeader title="Institution" description="Loading governance record…" />
        <LoadingState label="Loading institution…" />
      </div>
    )
  }

  if (error || !institution) {
    return (
      <div className="space-y-6 pb-8">
        <PageHeader title="Institution" description="Governance record for an institution in your jurisdiction." />
        <ErrorState
          message={error ?? "Unable to load this institution."}
          onRetry={load}
        />
        <div className="flex justify-start">
          <Link
            href="/dashboard/regional-admin/institutions"
            className="text-sm font-medium text-primary hover:underline"
          >
            ← Schools & Institutions
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
            ← Schools & Institutions
          </Link>
        }
      />

      <div className="flex flex-wrap items-center gap-2">
        <Chip tone="info">{institution.code}</Chip>
        <Chip tone="default">{institution.type}</Chip>
        <Chip tone={institution.isActive ? "success" : "danger"}>
          {institution.isActive ? "Active" : "Inactive"}
        </Chip>
        {institution.regionName && <Chip tone="muted">{institution.regionName}</Chip>}
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Teachers" value={institution.teacherCount} />
        <StatCard label="Learners" value={institution.learnerCount} />
        <StatCard label="Classes" value={institution.classCount} />
        <StatCard label="Lessons" value={institution.lessonCount} />
        <StatCard label="Live classes" value={institution.liveClassCount} />
        <StatCard label="Attendance" value={pct(institution.attendanceRate)} />
        <StatCard label="Avg. performance" value={pct(institution.averagePerformance)} />
        <StatCard label="Curriculum progress" value={pct(institution.curriculumProgress)} />
      </div>

      <Panel title="Jurisdiction & identity">
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <Field label="District" value={valueOr(institution.districtName)} />
          <Field label="Region" value={valueOr(institution.regionName)} />
          <Field label="Contact email" value={valueOr(institution.contactEmail)} />
          <Field label="Contact phone" value={valueOr(institution.contactPhone)} />
          <Field label="Address" value={valueOr(institution.address)} />
          <Field label="City" value={valueOr(institution.city)} />
        </div>
      </Panel>

      <div className="grid gap-6 lg:grid-cols-2">
        <Panel
          title="Verification"
          actions={
            <Link
              href="/dashboard/regional-admin/governance/verification"
              className="text-xs font-semibold text-primary hover:underline"
            >
              Open verification queue →
            </Link>
          }
        >
          <div className="flex items-center gap-3 rounded-xl border border-border bg-background p-4">
            <BadgeCheck className="size-5 shrink-0 text-muted-foreground" />
            <div className="min-w-0">
              <p className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground">Status</p>
              <div className="mt-1">
                <Chip tone={verificationTone(institution.verificationStatus)}>
                  {institution.verificationStatus || "UNKNOWN"}
                </Chip>
              </div>
            </div>
          </div>
          <p className="mt-3 text-xs text-muted-foreground">
            Verification records, submitted documents and review decisions live in the shared verification queue.
          </p>
        </Panel>

        <Panel title="Data quality">
          {issues.length === 0 ? (
            <EmptyState
              icon={<ShieldCheck className="size-10" />}
              title="No data quality findings for this institution."
              hint="Automated checks have not flagged anything for this record."
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

      <Panel title="Jurisdiction analytics">
        <div className="grid gap-3 sm:grid-cols-3">
          <AnalyticsLink
            href={`/oversight/schools/${institution.id}`}
            title="School profile"
            hint="Attendance, performance and curriculum for this institution."
          />
          <AnalyticsLink
            href="/oversight/performance"
            title="Performance"
            hint="Compare assessment results across your jurisdiction."
          />
          <AnalyticsLink
            href="/oversight/attendance"
            title="Attendance"
            hint="Daily trends and schools at risk of absence."
          />
        </div>
        <p className="mt-4 text-xs text-muted-foreground">
          Analytics are scoped to the same jurisdiction as this record.
        </p>
      </Panel>
    </div>
  )
}
