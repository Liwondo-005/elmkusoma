"use client"

import { useEffect, useState, useCallback } from "react"
import { Users, Loader2, Search, UserCheck, UserX } from "lucide-react"
import { platformAdminApi, type UserSummary, type PageResponse } from "@/lib/platform-admin-api"

const ROLES = ["", "STUDENT", "TEACHER", "PARENT", "OTHER_LEARNER", "ADMIN", "INSTITUTION_ADMIN", "PROVIDER_ADMIN"]
const PAGE_SIZE = 20

export default function PlatformUsersPage() {
  const [data, setData] = useState<PageResponse<UserSummary> | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [role, setRole] = useState("")
  const [search, setSearch] = useState("")
  const [searchInput, setSearchInput] = useState("")
  const [toggling, setToggling] = useState<string | null>(null)

  const loadData = useCallback(async () => {
    try {
      setLoading(true)
      setError(null)
      const res = await platformAdminApi.listUsers(page, PAGE_SIZE, role || undefined, search || undefined)
      setData(res)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load users")
    } finally {
      setLoading(false)
    }
  }, [page, role, search])

  useEffect(() => {
    loadData()
  }, [loadData])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    setSearch(searchInput)
  }

  const handleToggleStatus = async (user: UserSummary) => {
    try {
      setToggling(user.id)
      await platformAdminApi.updateUserStatus(user.id, !user.isActive)
      setData((prev) => {
        if (!prev) return prev
        return {
          ...prev,
          content: prev.content.map((u) =>
            u.id === user.id ? { ...u, isActive: !u.isActive } : u
          ),
        }
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update user status")
    } finally {
      setToggling(null)
    }
  }

  const formatDate = (d: string) => new Date(d).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" })

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Users</h1>
        <p className="mt-1 text-sm text-muted-foreground">Manage platform users across all institutions.</p>
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
        <form onSubmit={handleSearch} className="relative flex-1">
          <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search by name or email..."
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            className="w-full rounded-lg border border-border bg-background pl-10 pr-4 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
          />
        </form>
        <select
          value={role}
          onChange={(e) => { setRole(e.target.value); setPage(0) }}
          className="rounded-lg border border-border bg-background px-3 py-2.5 text-sm outline-none focus:ring-2 focus:ring-ring"
        >
          {ROLES.map((r) => (
            <option key={r} value={r}>{r || "All Roles"}</option>
          ))}
        </select>
      </div>

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive">{error}</div>
      )}

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-8 animate-spin text-muted-foreground" />
        </div>
      ) : !data || data.content.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
          <Users className="size-10 text-muted-foreground/50" />
          <p className="mt-4 text-sm font-medium text-foreground">No users found</p>
          <p className="mt-1 text-sm text-muted-foreground">
            {search ? "Try a different search term." : "No users available."}
          </p>
        </div>
      ) : (
        <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border bg-muted/50">
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Name</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Email</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Role</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Status</th>
                  <th className="px-5 py-3 text-left font-medium text-muted-foreground">Created</th>
                  <th className="px-5 py-3 text-right font-medium text-muted-foreground">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.content.map((user) => (
                  <tr key={user.id} className="border-b border-border last:border-0 hover:bg-muted/30 transition-colors">
                    <td className="px-5 py-3.5 font-medium text-foreground">{user.firstName} {user.lastName}</td>
                    <td className="px-5 py-3.5 text-muted-foreground">{user.email}</td>
                    <td className="px-5 py-3.5">
                      <span className="inline-block rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                        {user.role}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${user.isActive ? "bg-green-500/10 text-green-600" : "bg-red-500/10 text-red-600"}`}>
                        {user.isActive ? "Active" : "Suspended"}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-muted-foreground">{formatDate(user.createdAt)}</td>
                    <td className="px-5 py-3.5 text-right">
                      <button
                        onClick={() => handleToggleStatus(user)}
                        disabled={toggling === user.id}
                        className={`inline-flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-xs font-medium transition-colors disabled:opacity-50 ${
                          user.isActive
                            ? "border-red-200 text-red-600 hover:bg-red-50 dark:border-red-800"
                            : "border-green-200 text-green-600 hover:bg-green-50 dark:border-green-800"
                        }`}
                      >
                        {toggling === user.id ? (
                          <Loader2 className="size-3.5 animate-spin" />
                        ) : user.isActive ? (
                          <UserX className="size-3.5" />
                        ) : (
                          <UserCheck className="size-3.5" />
                        )}
                        {user.isActive ? "Suspend" : "Activate"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {data && data.totalElements > PAGE_SIZE && (
        <div className="flex items-center justify-center gap-2">
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            Prev
          </button>
          <span className="text-sm text-muted-foreground">
            Page {page + 1} of {data.totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
            disabled={page >= data.totalPages - 1}
            className="rounded-lg border border-border px-3 py-1.5 text-sm font-medium hover:bg-muted disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  )
}
