"use client"

import { useCallback, useState } from "react"
import { FileSearch } from "lucide-react"
import { useTranslations } from "next-intl"
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
  const t = useTranslations("regionalAdmin")
  const tc = useTranslations("common")
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
        title={t("audit.title")}
        description={t("audit.description")}
      />

      <PagedList<AuditLogSummary>
        key={entityType}
        fetcher={fetcher}
        searchPlaceholder={t("audit.searchPlaceholder")}
        emptyTitle={t("audit.emptyTitle")}
        emptyHint={t("audit.emptyHint")}
        emptyIcon={<FileSearch className="size-10" />}
        toolbar={
          <div className="flex items-center gap-2">
            <label
              htmlFor="audit-entity-type"
              className="text-[10px] font-bold uppercase tracking-widest text-muted-foreground"
            >
              {t("audit.entityLabel")}
            </label>
            <select
              id="audit-entity-type"
              value={entityType}
              onChange={(e) => setEntityType(e.target.value)}
              className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
            >
              {ENTITY_TYPES.map((option) => (
                <option key={option || "all"} value={option}>
                  {option || tc("all")}
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
                <span className="font-semibold text-foreground">{log.actorName || t("audit.unknownActor")}</span>
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
