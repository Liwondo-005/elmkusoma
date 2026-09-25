"use client"

import { useEffect, useState } from "react"
import { Shield, Loader2 } from "lucide-react"
import { platformAdminApi, type EntitlementSummary, type PageResponse } from "@/lib/platform-admin-api"

export default function EntitlementsPage() {
  const [data, setData] = useState<PageResponse<EntitlementSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [filter, setFilter] = useState("")

  const load = (p = 0) => {
    setLoading(true)
    platformAdminApi.listEntitlements(p, 20, filter || undefined).then(setData).finally(() => setLoading(false))
  }

  useEffect(() => { load() }, [filter])

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Entitlements</h1>
        <p className="mt-1 text-sm text-muted-foreground">Platform-wide service entitlements and access grants</p>
      </div>

      <div className="flex gap-2">
        {["", "ACTIVE", "EXPIRED", "REVOKED"].map(s => (
          <button key={s} onClick={() => setFilter(s)} className={`rounded-lg px-3 py-1.5 text-xs font-medium ${filter === s ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground hover:bg-muted/80"}`}>
            {s || "All"}
          </button>
        ))}
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : !data || data.content.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-10 text-center">
          <Shield className="mx-auto size-10 text-muted-foreground/50" />
          <p className="mt-3 text-sm text-muted-foreground">No entitlements found</p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/30">
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground">Service</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground">Student</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground">Status</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground">Starts</th>
                <th className="px-4 py-3 text-left text-xs font-medium text-muted-foreground">Expires</th>
              </tr>
            </thead>
            <tbody>
              {data.content.map(ent => (
                <tr key={ent.id} className="border-b border-border last:border-0 hover:bg-muted/20">
                  <td className="px-4 py-3 font-medium">{ent.serviceType}</td>
                  <td className="px-4 py-3 text-xs font-mono text-muted-foreground">{ent.studentId?.slice(0, 8)}...</td>
                  <td className="px-4 py-3">
                    <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${ent.status === "ACTIVE" ? "bg-green-50 text-green-700" : ent.status === "EXPIRED" ? "bg-yellow-50 text-yellow-700" : "bg-red-50 text-red-700"}`}>
                      {ent.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{ent.startsAt ? new Date(ent.startsAt).toLocaleDateString("en-GB") : "-"}</td>
                  <td className="px-4 py-3 text-xs text-muted-foreground">{ent.expiresAt ? new Date(ent.expiresAt).toLocaleDateString("en-GB") : "Never"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
