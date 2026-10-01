"use client"

import { useCallback, useState } from "react"
import { FileSearch } from "lucide-react"
import {
  regionalAdminApi,
  type AuditLogSummary,
} from "@/lib/regional-admin-api"
import {
  Chip,
  PageHeader,
  PagedList,
  formatDateTime,
} from "@/components/dashboard/regional-admin/ui"

const ENTITY_TYPES = ["", "INSTITUTION", "USER", "COURSE", "VERIFICATION", "ANNOUNCEMENT"]

function actionTone(action: string): "success" | "info" | "danger" | "warning" | "default" {
  const a = action.toUpperCase()
  if (a.includes("DELETE")) return "danger"
  if (a.includes("CREATE") || a.includes("REGISTER")) return "success"
  if (a.includes("UPDATE") || a.includes("EDIT")) return "info"
  if (a.includes("REJECT") || a.includes("SUSPEND")) return "warning"
  return "default"
}

export default function AuditPage() {
  const [entityType, setEntityType] = useState("")

  // entityType is part of the fetcher identity so PagedList refetches on change.
  const fetcher = useCallback(
    (p: { page: number; size: number; search?: string }) =>
      regionalAdminApi.getAuditLogs({ ...p, entityType }),
    [entityType],
  )

  return (
    <div className="space-y-6 pb-8">
      <PageHeader
        title="Audit"
        description="Recorded actions on institutions, users and content inside your jurisdiction."
      />

      <PagedList<AuditLogSummary>
        key={entityType}
        fetcher={fetcher}
        searchPlaceholder="Search audit logs…"
        emptyTitle="No audit entries match this filter."
        emptyHint="Actions recorded inside your jurisdiction will appear here."
        emptyIcon={<FileSearch className="size-10" />}
        toolbar={
          <div className="flex items-center gap-2">
            <label
              htmlFor="audit-entity-type"
              className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground"
            >
              Entity
            </label>
            <select
              id="audit-entity-type"
              value={entityType}
              onChange={(e) => setEntityType(e.target.value)}
              className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
            >
              {ENTITY_TYPES.map((option) => (
                <option key={option || "all"} value={option}>
                  {option || "All"}
                </option>
              ))}
            </select>
          </div>
        }
        renderItem={(log) => (
          <article className="rounded-2xl border border-border bg-card p-4 shadow-sm">
            <div className="flex flex-wrap items-center gap-2">
              <Chip tone={actionTone(log.action)}>{log.action}</Chip>
              <Chip tone="muted">{log.entityType}</Chip>
              <span className="text-sm font-semibold text-foreground">
                {log.entityName || "—"}
              </span>
            </div>
            <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1 text-xs text-muted-foreground">
              <span>
                <span className="font-semibold text-foreground">{log.actorName || "Unknown actor"}</span>
                {log.actorRole && <> · {log.actorRole}</>}
              </span>
              {log.institutionName && <span>{log.institutionName}</span>}
              <span>{formatDateTime(log.timestamp)}</span>
            </div>
          </article>
        )}
      />
    </div>
  )
}
