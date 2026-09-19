"use client"

import { useEffect, useState } from "react"
import { Eye, Loader2, Filter, ChevronLeft, ChevronRight } from "lucide-react"
import { adminApi, getInstitutionId, type InstitutionAuditLogResponse } from "@/lib/api"

const actionColors: Record<string, string> = {
  CREATE: "bg-emerald-100 text-emerald-800",
  UPDATE: "bg-blue-100 text-blue-800",
  UPDATE_PROFILE: "bg-blue-100 text-blue-800",
  UPDATE_ROLE: "bg-purple-100 text-purple-800",
  UPDATE_SERVICES: "bg-indigo-100 text-indigo-800",
  DELETE: "bg-red-100 text-red-800",
  DEACTIVATE_USER: "bg-orange-100 text-orange-800",
  ACTIVATE_USER: "bg-emerald-100 text-emerald-800",
  INVITE_USER: "bg-cyan-100 text-cyan-800",
  CANCEL_INVITATION: "bg-yellow-100 text-yellow-800",
}

export default function AuditLogPage() {
  const [logs, setLogs] = useState<InstitutionAuditLogResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [actionFilter, setActionFilter] = useState("")
  const pageSize = 30

  useEffect(() => { loadLogs() }, [page])

  async function loadLogs() {
    const institutionId = getInstitutionId()
    if (!institutionId) { setError("No institution context"); setLoading(false); return }
    try {
      const data = await adminApi.getAuditLog(institutionId, page, pageSize)
      setLogs(data)
    } catch (err) { setError(err instanceof Error ? err.message : "Failed to load audit logs") }
    finally { setLoading(false) }
  }

  const filteredLogs = actionFilter
    ? logs.filter((l) => l.action.includes(actionFilter))
    : logs

  const actionTypes = [...new Set(logs.map((l) => l.action))]

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <div className="flex items-center justify-between">
        <div><h1 className="text-2xl font-bold tracking-tight text-foreground">Audit Log</h1>
          <p className="mt-1 text-sm text-muted-foreground">Track all administrative actions in your institution</p></div>
      </div>

      <div className="flex items-center gap-3">
        <Filter className="size-4 text-muted-foreground" />
        <select value={actionFilter} onChange={(e) => setActionFilter(e.target.value)} className="rounded-lg border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary">
          <option value="">All Actions</option>
          {actionTypes.map((a) => <option key={a} value={a}>{a}</option>)}
        </select>
      </div>

      {loading && <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>}
      {error && <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4"><p className="text-sm font-medium text-destructive">{error}</p></div>}

      {!loading && (
        <div className="rounded-2xl border border-border bg-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead><tr className="border-b border-border bg-muted/50">
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">Time</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">Actor</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">Action</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">Target</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">Details</th>
              </tr></thead>
              <tbody className="divide-y divide-border">
                {filteredLogs.map((log) => (
                  <tr key={log.id} className="hover:bg-muted/30 transition-colors">
                    <td className="px-4 py-3 text-xs text-muted-foreground whitespace-nowrap">{new Date(log.createdAt).toLocaleString()}</td>
                    <td className="px-4 py-3"><div><p className="text-sm font-medium text-foreground">{log.actorEmail}</p><p className="text-xs text-muted-foreground">{log.actorRole}</p></div></td>
                    <td className="px-4 py-3"><span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium ${actionColors[log.action] || "bg-gray-100 text-gray-800"}`}>{log.action}</span></td>
                    <td className="px-4 py-3"><div><p className="text-xs text-muted-foreground">{log.targetType}</p><p className="text-xs text-foreground font-mono truncate max-w-[200px]">{log.targetId}</p></div></td>
                    <td className="px-4 py-3 text-xs text-muted-foreground max-w-[300px] truncate">{log.details}</td>
                  </tr>
                ))}
                {filteredLogs.length === 0 && <tr><td colSpan={5} className="px-4 py-10 text-center text-sm text-muted-foreground">No audit logs found</td></tr>}
              </tbody>
            </table>
          </div>
          <div className="flex items-center justify-between border-t border-border px-4 py-3">
            <button onClick={() => setPage(Math.max(0, page - 1))} disabled={page === 0} className="inline-flex items-center gap-1 rounded-lg px-3 py-1.5 text-sm font-medium text-muted-foreground hover:bg-muted disabled:opacity-50">
              <ChevronLeft className="size-4" />Previous
            </button>
            <span className="text-sm text-muted-foreground">Page {page + 1}</span>
            <button onClick={() => setPage(page + 1)} disabled={logs.length < pageSize} className="inline-flex items-center gap-1 rounded-lg px-3 py-1.5 text-sm font-medium text-muted-foreground hover:bg-muted disabled:opacity-50">
              Next<ChevronRight className="size-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  )
}

