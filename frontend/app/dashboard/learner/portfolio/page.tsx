"use client"

import { useEffect, useState } from "react"
import { useAuth } from "@/lib/auth"
import { collegeApi } from "@/lib/college-api"
import type { Portfolio, PortfolioItem } from "@/lib/types/college"
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared"
import { Briefcase, Award, FileText, Image, Star, Eye, EyeOff, AlertCircle } from "lucide-react"

function ItemTypeBadge({ type }: { type: string }) {
  const colors: Record<string, string> = {
    DOCUMENT: "bg-slate-100 text-slate-700 dark:bg-slate-800 dark:text-slate-300",
    CERTIFICATE: "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
    PROJECT_EVIDENCE: "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
    MEDIA: "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
    ACHIEVEMENT: "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400",
    WORK_SAMPLE: "bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-400",
    REFERENCE: "bg-pink-100 text-pink-700 dark:bg-pink-900/30 dark:text-pink-400",
    OTHER: "bg-muted text-muted-foreground",
  }
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${colors[type] || colors.OTHER}`}>
      {type.replace(/_/g, " ")}
    </span>
  )
}

export default function PortfolioPage() {
  const { user, loading: authLoading } = useAuth()
  const [portfolio, setPortfolio] = useState<Portfolio | null>(null)
  const [items, setItems] = useState<PortfolioItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadPortfolio()
  }, [user])

  async function loadPortfolio() {
    try {
      setLoading(true)
      const studentId = user?.id || ""
      const res = await collegeApi.getStudentPortfolio(studentId)
      const data = res.data as Portfolio | undefined
      if (data) {
        setPortfolio(data)
        setItems(data.items || [])
      }
    } catch { setError("Failed to load portfolio") } finally { setLoading(false) }
  }

  if (authLoading || loading) return <LoadingState />

  if (error && !portfolio) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <LearnerHeader firstName={user?.firstName || "Learner"} subtitle="Curate and showcase your achievements, certificates, and work samples." />
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700 flex items-center gap-2">
          <AlertCircle className="size-4 shrink-0" />
          <span>{error}</span>
          <button onClick={() => { setError(null); loadPortfolio() }} className="ml-auto text-xs underline">Retry</button>
        </div>
      </div>
    )
  }

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner"
  const certificates = items.filter(i => i.itemType === "CERTIFICATE").length
  const workSamples = items.filter(i => i.itemType === "WORK_SAMPLE" || i.itemType === "PROJECT_EVIDENCE").length
  const publicItems = items.filter(i => i.isVisible).length

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <LearnerHeader firstName={firstName} subtitle="Curate and showcase your achievements, certificates, and work samples." />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-primary/10"><Briefcase className="size-5 text-primary" /></div>
            <div><p className="text-xs font-medium text-muted-foreground">Total Items</p><p className="text-2xl font-extrabold text-foreground">{items.length}</p></div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-emerald-100 dark:bg-emerald-900/30"><Award className="size-5 text-emerald-600 dark:text-emerald-400" /></div>
            <div><p className="text-xs font-medium text-muted-foreground">Certificates</p><p className="text-2xl font-extrabold text-foreground">{certificates}</p></div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-blue-100 dark:bg-blue-900/30"><FileText className="size-5 text-blue-600 dark:text-blue-400" /></div>
            <div><p className="text-xs font-medium text-muted-foreground">Work Samples</p><p className="text-2xl font-extrabold text-foreground">{workSamples}</p></div>
          </div>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-center gap-3">
            <div className="flex size-10 items-center justify-center rounded-full bg-amber-100 dark:bg-amber-900/30"><Eye className="size-5 text-amber-600 dark:text-amber-400" /></div>
            <div><p className="text-xs font-medium text-muted-foreground">Public Items</p><p className="text-2xl font-extrabold text-foreground">{publicItems}</p></div>
          </div>
        </div>
      </div>

      {!portfolio ? (
        <EmptyState title="No portfolio yet" description="Start adding your achievements, certificates, and work samples to build your professional profile." />
      ) : items.length === 0 ? (
        <EmptyState title="Your portfolio is empty" description="Add items like certificates, project evidence, and work samples to showcase your skills." />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {items.map((item) => (
            <div key={item.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition hover:shadow-md">
              <div className="flex items-start justify-between">
                <h3 className="font-semibold text-foreground line-clamp-2">{item.title}</h3>
                <ItemTypeBadge type={item.itemType} />
              </div>
              {item.description && <p className="mt-2 line-clamp-2 text-sm text-muted-foreground">{item.description}</p>}
              <div className="mt-3 flex items-center gap-3 text-xs text-muted-foreground">
                {item.dateObtained && <span>{new Date(item.dateObtained).toLocaleDateString()}</span>}
                <span className="flex items-center gap-1">
                  {item.isVisible ? <Eye className="size-3" /> : <EyeOff className="size-3" />}
                  {item.isVisible ? "Public" : "Private"}
                </span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
