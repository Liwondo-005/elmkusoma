"use client"

import { useEffect, useState } from "react"
import { Loader2, X, ShieldCheck, AlertCircle, UserRound } from "lucide-react"
import { platformAdminApi, ORG_MEMBER_ROLES, type OrgMember, type InstitutionSummary } from "@/lib/platform-admin-api"

interface OrgMembersModalProps {
  open: boolean
  institution: InstitutionSummary | null
  onClose: () => void
}

export function OrgMembersModal({ open, institution, onClose }: OrgMembersModalProps) {
  const [members, setMembers] = useState<OrgMember[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [savingUserId, setSavingUserId] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    if (!open || !institution) return
    setLoading(true)
    setError(null)
    setSuccess(null)
    platformAdminApi
      .listOrgMembers(institution.id)
      .then(setMembers)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load members"))
      .finally(() => setLoading(false))
  }, [open, institution])

  if (!open || !institution) return null

  const handleRoleChange = async (member: OrgMember, newRole: string) => {
    if (newRole === member.membershipRole) return
    setSavingUserId(member.userId)
    setError(null)
    setSuccess(null)
    try {
      const updated = await platformAdminApi.updateOrgMemberRole(institution.id, member.userId, newRole)
      setMembers((prev) => prev.map((m) => (m.userId === member.userId ? { ...m, ...updated } : m)))
      setSuccess(`${member.fullName || member.email || "Member"} is now ${newRole}`)
      setTimeout(() => setSuccess(null), 4000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update role")
    } finally {
      setSavingUserId(null)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />
      <div className="relative flex max-h-[85vh] w-full max-w-2xl flex-col rounded-2xl border border-border bg-card p-6 shadow-lg">
        <div className="mb-4 flex items-start justify-between">
          <div>
            <h2 className="flex items-center gap-2 text-lg font-bold text-foreground">
              <ShieldCheck className="size-5 text-primary" /> Members &amp; Roles
            </h2>
            <p className="mt-0.5 text-xs text-muted-foreground">
              {institution.name} — assign roles to organization members.
            </p>
          </div>
          <button type="button" onClick={onClose} className="rounded-lg p-1 text-muted-foreground hover:text-foreground" aria-label="Close">
            <X className="size-5" />
          </button>
        </div>

        {error && (
          <div className="mb-3 flex items-center gap-2 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-2.5 text-sm text-destructive">
            <AlertCircle className="size-4 shrink-0" /> {error}
          </div>
        )}
        {success && (
          <div className="mb-3 rounded-xl border border-green-500/20 bg-green-500/5 px-4 py-2.5 text-sm text-green-600">{success}</div>
        )}

        <div className="-mx-2 flex-1 overflow-y-auto px-2">
          {loading ? (
            <div className="flex items-center justify-center py-14">
              <Loader2 className="size-6 animate-spin text-muted-foreground" />
            </div>
          ) : members.length === 0 ? (
            <div className="flex flex-col items-center py-14 text-center">
              <UserRound className="size-8 text-muted-foreground/50" />
              <p className="mt-3 text-sm text-muted-foreground">No members found for this organization.</p>
            </div>
          ) : (
            <ul className="space-y-2">
              {members.map((m) => (
                <li key={m.userId} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-background px-4 py-3">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-foreground">
                      {m.fullName || m.email || m.userId}
                    </p>
                    <p className="truncate text-xs text-muted-foreground">
                      {m.email}{m.userRole ? ` · ${m.userRole}` : ""}
                      {!m.isActive && <span className="ml-1 text-red-600">(inactive membership)</span>}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <span className="rounded-full bg-primary/10 px-2 py-0.5 text-[11px] font-semibold text-primary">
                      {m.membershipRole ?? "—"}
                    </span>
                    <select
                      value={m.membershipRole ?? ""}
                      disabled={savingUserId === m.userId}
                      onChange={(e) => handleRoleChange(m, e.target.value)}
                      className="rounded-lg border border-border bg-background px-2 py-1.5 text-xs font-medium outline-none focus:ring-2 focus:ring-ring disabled:opacity-50"
                    >
                      {!m.membershipRole && <option value="">Select role…</option>}
                      {ORG_MEMBER_ROLES.map((r) => (
                        <option key={r} value={r}>{r.replace(/_/g, " ")}</option>
                      ))}
                    </select>
                    {savingUserId === m.userId && <Loader2 className="size-4 animate-spin text-muted-foreground" />}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="mt-4 flex justify-end pt-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-lg border border-border px-4 py-2 text-sm font-medium hover:bg-muted"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  )
}
