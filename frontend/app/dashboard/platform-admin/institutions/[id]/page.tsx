"use client"

import { useEffect, useState } from "react"
import { useParams, useRouter } from "next/navigation"
import { Building2, Loader2, ArrowLeft, Users, GraduationCap, BookOpen, MapPin, Phone, Mail, Calendar, Shield, ShieldOff } from "lucide-react"
import { platformAdminApi, type InstitutionDetail } from "@/lib/platform-admin-api"

export default function InstitutionDetailPage() {
  const params = useParams()
  const router = useRouter()
  const id = params.id as string
  const [inst, setInst] = useState<InstitutionDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [toggling, setToggling] = useState(false)

  useEffect(() => {
    if (!id) return
    setLoading(true)
    platformAdminApi.getInstitution(id)
      .then(setInst)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load institution"))
      .finally(() => setLoading(false))
  }, [id])

  const handleToggleStatus = async () => {
    if (!inst) return
    try {
      setToggling(true)
      const updated = await platformAdminApi.updateInstitutionStatus(inst.id, !inst.isActive)
      setInst((prev) => prev ? { ...prev, isActive: !prev.isActive } : prev)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update status")
    } finally {
      setToggling(false)
    }
  }

  const formatDate = (d: string) => new Date(d).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" })

  if (loading) return (
    <div className="flex items-center justify-center py-32">
      <Loader2 className="size-8 animate-spin text-muted-foreground" />
    </div>
  )

  if (error) return (
    <div className="mx-auto max-w-4xl space-y-6 py-10">
      <button onClick={() => router.back()} className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Back
      </button>
      <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-sm text-destructive">{error}</div>
    </div>
  )

  if (!inst) return null

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <button onClick={() => router.back()} className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> Back to Institutions
      </button>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-4">
            <div className="flex size-14 items-center justify-center rounded-xl bg-primary/10">
              <Building2 className="size-7 text-primary" />
            </div>
            <div>
              <h1 className="text-xl font-bold text-foreground">{inst.name}</h1>
              <p className="text-sm text-muted-foreground">{inst.code}</p>
              <div className="mt-2 flex items-center gap-2">
                <span className={`inline-block rounded-full px-2.5 py-0.5 text-xs font-medium ${inst.isActive ? "bg-green-500/10 text-green-600" : "bg-red-500/10 text-red-600"}`}>
                  {inst.isActive ? "Active" : "Inactive"}
                </span>
                <span className="inline-block rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">{inst.type}</span>
              </div>
            </div>
          </div>
          <button
            onClick={handleToggleStatus}
            disabled={toggling}
            className={`inline-flex items-center gap-2 rounded-lg border px-4 py-2 text-sm font-medium transition-colors disabled:opacity-50 ${
              inst.isActive
                ? "border-red-200 text-red-600 hover:bg-red-50 dark:border-red-800"
                : "border-green-200 text-green-600 hover:bg-green-50 dark:border-green-800"
            }`}
          >
            {toggling ? <Loader2 className="size-4 animate-spin" /> : inst.isActive ? <ShieldOff className="size-4" /> : <Shield className="size-4" />}
            {inst.isActive ? "Deactivate" : "Activate"}
          </button>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-4">
        {[
          { icon: Users, label: "Total Users", value: inst.totalUsers, color: "bg-blue-500/10 text-blue-600" },
          { icon: GraduationCap, label: "Students", value: inst.totalStudents, color: "bg-emerald-500/10 text-emerald-600" },
          { icon: BookOpen, label: "Teachers", value: inst.totalTeachers, color: "bg-amber-500/10 text-amber-600" },
          { icon: Calendar, label: "Created", value: formatDate(inst.createdAt), color: "bg-purple-500/10 text-purple-600", isText: true },
        ].map(({ icon: Icon, label, value, color, isText }) => (
          <div key={label} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
            <div className="flex items-center gap-3">
              <div className={`flex size-10 items-center justify-center rounded-xl ${color}`}>
                <Icon className="size-5" />
              </div>
              <div>
                <p className="text-xs text-muted-foreground">{label}</p>
                <p className={`font-bold ${isText ? "text-sm" : "text-xl"} text-foreground`}>{value}</p>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground mb-4">Contact Information</h2>
          <div className="space-y-3">
            {inst.address && (
              <div className="flex items-center gap-3 text-sm">
                <MapPin className="size-4 text-muted-foreground" />
                <span className="text-foreground">{inst.address}{inst.city ? `, ${inst.city}` : ""}{inst.region ? `, ${inst.region}` : ""}{inst.country ? `, ${inst.country}` : ""}</span>
              </div>
            )}
            {inst.phone && (
              <div className="flex items-center gap-3 text-sm">
                <Phone className="size-4 text-muted-foreground" />
                <span className="text-foreground">{inst.phone}</span>
              </div>
            )}
            {inst.email && (
              <div className="flex items-center gap-3 text-sm">
                <Mail className="size-4 text-muted-foreground" />
                <span className="text-foreground">{inst.email}</span>
              </div>
            )}
            {!inst.address && !inst.phone && !inst.email && (
              <p className="text-sm text-muted-foreground">No contact information available.</p>
            )}
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <h2 className="text-base font-semibold text-foreground mb-4">Quick Actions</h2>
          <div className="space-y-2">
            <button
              onClick={() => router.push(`/dashboard/platform-admin/users?institution=${inst.id}`)}
              className="flex w-full items-center gap-3 rounded-lg border border-border px-4 py-3 text-sm font-medium text-foreground hover:bg-muted transition-colors"
            >
              <Users className="size-4 text-muted-foreground" />
              View Institution Users
            </button>
            <button
              onClick={() => router.push("/dashboard/platform-admin/audit")}
              className="flex w-full items-center gap-3 rounded-lg border border-border px-4 py-3 text-sm font-medium text-foreground hover:bg-muted transition-colors"
            >
              <Calendar className="size-4 text-muted-foreground" />
              View Audit Logs
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
