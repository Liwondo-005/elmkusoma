"use client"

import { useEffect, useState, useCallback } from "react"
import { ScrollText, Loader2, User } from "lucide-react"
import { platformAdminApi, type AuditLogEntry } from "@/lib/platform-admin-api"

const PAGE_SIZE = 20

export default function AuditLogsPage() {
  const [logs, setLogs] = useState<AuditLogEntry[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const data = await platformAdminApi.getAuditLogs(page, PAGE_SIZE)
      setLogs(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load audit logs")
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    loadData()
  }, [loadData])

  const getActionBadge = (action: string) => {
    switch (action) {
      case "CREATE":
        return <span className="inline-block rounded-full bg-green-500/10 px-2.5 py-0.5 text-xs font-medium text-green-600">CREATE</span>
      case "UPDATE":
        return <span className="inline-block rounded-full bg-blue-500/10 px-2.5 py-0.5 text-xs font-medium text-blue-600">UPDATE</span>
      case "DELETE":
        return <span className="inline-block rounded-full bg-red-500/10 px-2.5 py-0.5 text-xs font-medium text-red-600">DELETE</span>
      case "LOGIN":
        return <span className="inline-block rounded-full bg-purple-500/10 px-2.5 py-0.5 text-xs font-medium text-purple-600">LOGIN</span>
      default:
        return <span className="inline-block rounded-full bg-muted px-2.5 py-0.5 text-xs font-medium text-muted-foreground">{action}</span>
    }
  }

  const formatDate = (d: string) => new Date(d).toLocaleString("en-GB", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" })

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Audit Logs</h1>
        <p className="mt-1 text-sm text-muted-foreground">Track all actions performed across the platform.</p>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        </div>
      ) : logs.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <ScrollText className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">No audit logs</p>
          <p className="mt-1 text-sm text-muted-foreground">No actions have been recorded yet.</p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Actor</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Action</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Entity</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id} className="border-b border-border last:border-0 hover:bg-muted/30 transition-colors">
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-2">
                        <div className="flex size-7 items-center justify-center rounded-full bg-muted">
                          <User className="size-3.5 text-muted-foreground" />
                        </div>
                        <div>
                          <p className="font-medium text-foreground">{log.performedBy}</p>
                          <p className="text-xs text-muted-foreground">{log.userRole}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-5 py-3.5">{getActionBadge(log.action)}</td>
                    <td className="px-5 py-3.5">
                      <div>
                        <p className="text-foreground">{log.entityType}</p>
                        {log.entityName && <p className="text-xs text-muted-foreground">{log.entityName}</p>}
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground">{formatDate(log.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {logs.length === PAGE_SIZE && (
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            Prev
          </button>
          <span className="text-sm text-muted-foreground">Page {page + 1}</span>
          <button
            onClick={() => setPage((p) => p + 1)}
            disabled={logs.length < PAGE_SIZE}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}
