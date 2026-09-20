"use client"

import { useEffect, useState } from "react"
import { Shield, Loader2, Search, Users } from "lucide-react"
import { platformAdminApi, type UserSummary, type PageResponse } from "@/lib/platform-admin-api"

export default function AdminsPage() {
  const [admins, setAdmins] = useState<UserSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")

  useEffect(() => {
    setLoading(true)
    platformAdminApi.listUsers(0, 100, "ADMIN")
      .then((res) => setAdmins(res.content.filter(u => u.role === "ADMIN" || u.role === "INSTITUTION_ADMIN")))
      .catch(() => setAdmins([]))
      .finally(() => setLoading(false))
  }, [])

  const filtered = admins.filter(a =>
    `${a.firstName} ${a.lastName} ${a.email}`.toLowerCase().includes(search.toLowerCase())
  )

  if (loading) return (
    <div className="flex items-center justify-center py-32">
      <Loader2 className="size-8 animate-spin text-primary" />
    </div>
  )

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Administrator Accounts</h1>
        <p className="mt-1 text-sm text-muted-foreground">Platform and institution administrators</p>
      </div>

      <div className="flex items-center gap-3">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search admins..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full rounded-xl border border-border bg-card py-2 pl-10 pr-4 text-sm outline-none focus:ring-2 focus:ring-primary/20"
          />
        </div>
      </div>

      {filtered.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border p-12 text-center">
          <Shield className="mx-auto size-10 text-muted-foreground" />
          <p className="mt-4 text-sm font-medium text-foreground">No administrators found</p>
          <p className="mt-1 text-xs text-muted-foreground">No admin accounts match your search</p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border bg-muted/50">
                <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Name</th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Email</th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Role</th>
                <th className="px-5 py-3 text-left text-xs font-semibold text-muted-foreground uppercase tracking-wider">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {filtered.map((admin) => (
                <tr key={admin.id} className="hover:bg-muted/30 transition-colors">
                  <td className="px-5 py-3.5">
                    <div className="flex items-center gap-3">
                      <div className="flex size-8 items-center justify-center rounded-lg bg-primary/10 text-xs font-bold text-primary">
                        {admin.firstName?.charAt(0)}{admin.lastName?.charAt(0)}
                      </div>
                      <span className="text-sm font-medium text-foreground">{admin.firstName} {admin.lastName}</span>
                    </div>
                  </td>
                  <td className="px-5 py-3.5 text-sm text-muted-foreground">{admin.email}</td>
                  <td className="px-5 py-3.5">
                    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                      admin.role === "ADMIN" ? "bg-purple-100 text-purple-700" : "bg-blue-100 text-blue-700"
                    }`}>{admin.role}</span>
                  </td>
                  <td className="px-5 py-3.5">
                    <span className={`inline-flex rounded-full px-2.5 py-0.5 text-xs font-semibold ${
                      admin.isActive ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"
                    }`}>{admin.isActive ? "Active" : "Inactive"}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
