"use client"

import { useEffect, useState } from "react"
import { Users, Loader2, ShieldOff } from "lucide-react"
import { platformAdminApi, type DelegationSummary } from "@/lib/platform-admin-api"

export default function DelegationsPage() {
  const [delegations, setDelegations] = useState<DelegationSummary[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    platformAdminApi.listDelegations().then(setDelegations).finally(() => setLoading(false))
  }, [])

  async function revoke(id: string) {
    if (!confirm("Revoke this delegation?")) return
    await platformAdminApi.revokeDelegation(id, "Revoked by admin")
    setDelegations(prev => prev.map(d => d.id === id ? { ...d, status: "REVOKED" } : d))
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Admin Delegations</h1>
        <p className="mt-1 text-sm text-muted-foreground">Manage delegated administrative access</p>
      </div>

      {loading ? (
        <div className="flex justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
      ) : delegations.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-10 text-center">
          <Users className="mx-auto size-10 text-muted-foreground/50" />
          <p className="mt-3 text-sm text-muted-foreground">No delegations configured</p>
        </div>
      ) : (
        <div className="space-y-3">
          {delegations.map(d => (
            <div key={d.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center justify-between">
                <div>
                  <div className="flex items-center gap-2">
                    <p className="text-sm font-semibold text-foreground">Delegation {d.id.slice(0, 8)}...</p>
                    <span className={`rounded-full px-2 py-0.5 text-[10px] font-semibold ${d.status === "ACTIVE" ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"}`}>{d.status}</span>
                    <span className="rounded-full bg-muted px-2 py-0.5 text-[10px] font-medium">{d.scope}</span>
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">Delegator: {d.delegatorId?.slice(0, 8)}... Delegate: {d.delegateId?.slice(0, 8)}...</p>
                  <p className="mt-1 text-xs text-muted-foreground">Expires: {d.expiresAt ? new Date(d.expiresAt).toLocaleDateString("en-GB") : "Never"}</p>
                </div>
                {d.status === "ACTIVE" && (
                  <button onClick={() => revoke(d.id)} className="inline-flex items-center gap-1 rounded-xl border border-red-200 px-3 py-1.5 text-xs font-medium text-red-600 hover:bg-red-50">
                    <ShieldOff className="size-3" /> Revoke
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
