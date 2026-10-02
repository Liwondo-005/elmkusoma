"use client"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { nfeApi } from "@/lib/nfe-api"
import { Loader2, Plus, Search } from "lucide-react"

export default function AssessmentsPage() {
  const t = useTranslations("provider")
  const ta = useTranslations("assessments")
  const tt = useTranslations("teacher")
  const [assessments, setAssessments] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")

  useEffect(() => {
    loadAssessments()
  }, [])

  async function loadAssessments() {
    try {
      setLoading(true)
      const data = await nfeApi.listAssessments()
      setAssessments(data)
    } catch { /* empty */ } finally {
      setLoading(false)
    }
  }

  const filtered = assessments.filter((a) =>
    (a.title || a.name || "").toLowerCase().includes(search.toLowerCase())
  )

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{ta("title")}</h1>
          <p className="text-muted-foreground">{t("assessments.subtitle")}</p>
        </div>
        <button className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          <Plus className="size-4" /> {tt("assessments.createAssessment")}
        </button>
      </div>
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Search className="size-4 text-muted-foreground" />
            <input
              placeholder={t("assessments.searchPlaceholder")}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
            />
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
          ) : filtered.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">{t("assessments.empty")}</p>
          ) : (
            <div className="space-y-3">
              {filtered.map((assessment) => (
                <div key={assessment.id} className="flex items-center justify-between rounded-lg border border-border p-4">
                  <div>
                    <p className="text-sm font-medium text-foreground">{assessment.title || assessment.name}</p>
                    <p className="text-xs text-muted-foreground">{assessment.totalMarks || 0} {t("assessments.marksSuffix")}</p>
                  </div>
                  <span className="rounded-full bg-orange-100 px-2 py-0.5 text-xs font-medium text-orange-700">{assessment.status || t("assessments.draftFallback")}</span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
