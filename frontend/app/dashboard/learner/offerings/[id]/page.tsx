"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import Link from "next/link"
import { useTranslations } from "next-intl"
import { useAuth } from "@/lib/auth"
import { learnerApi, LearnerApiError, type LearningOffering } from "@/lib/learner-api"
import { LoadingState } from "@/components/learner/shared"
import { Compass, ArrowLeft, ArrowRight, Loader2, AlertCircle, User, BookOpen } from "lucide-react"

export default function OfferingDetailPage() {
  const { user } = useAuth()
  const t = useTranslations("highered")
  const params = useParams()
  const offeringId = params.id as string

  const [offering, setOffering] = useState<LearningOffering | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!user) return
    loadOffering()
  }, [user, offeringId])

  async function loadOffering() {
    try {
      setLoading(true)
      setError(null)
      const data = await learnerApi.getOffering(offeringId)
      setOffering(data)
    } catch (err) {
      const status = err instanceof LearnerApiError ? err.status : undefined
      if (status === 404 || status === 403) {
        setError(t("offerings.notFound"))
      } else {
        setError(err instanceof Error && err.message ? err.message : t("offerings.detailLoadError"))
      }
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <Loader2 className="size-6 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (error || !offering) {
    return (
      <div className="mx-auto max-w-3xl space-y-4 py-12 text-center">
        <div className="mx-auto flex size-12 items-center justify-center rounded-full bg-destructive/10">
          <AlertCircle className="size-6 text-destructive" />
        </div>
        <p className="text-sm text-muted-foreground">{error ?? t("offerings.notFound")}</p>
        <Link
          href="/dashboard/learner/courses"
          className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline"
        >
          <ArrowLeft className="size-4" />
          {t("offerings.back")}
        </Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <Link
        href="/dashboard/learner/courses"
        className="inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        {t("offerings.back")}
      </Link>

      <div className="rounded-2xl border border-border bg-card shadow-xs overflow-hidden">
        {offering.thumbnailUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={offering.thumbnailUrl} alt="" className="h-44 w-full object-cover" />
        ) : (
          <div className="flex h-32 w-full items-center justify-center bg-primary/10">
            <Compass className="size-12 text-primary/40" />
          </div>
        )}

        <div className="space-y-4 p-6">
          <div>
            <div className="flex items-center gap-2 flex-wrap">
              <span className="inline-flex rounded-full bg-muted px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wide text-muted-foreground">
                {t("offerings.tabLabel")}
              </span>
              {offering.educationLevel && (
                <span className="rounded-full bg-primary/10 px-2.5 py-1 text-[10px] font-semibold text-primary">
                  {offering.educationLevel}
                </span>
              )}
              {offering.subjectName && (
                <span className="rounded-full bg-muted px-2.5 py-1 text-[10px] font-medium text-muted-foreground">
                  {offering.subjectName}
                </span>
              )}
            </div>
            <h1 className="mt-3 text-2xl font-bold tracking-tight text-foreground">{offering.title}</h1>
            <p className="mt-2 flex items-center gap-1.5 text-sm text-muted-foreground">
              <User className="size-4" />
              {t("offerings.byTeacher", { name: offering.ownerName ?? "—" })}
            </p>
          </div>

          <div>
            <h2 className="text-sm font-semibold text-foreground">{t("offerings.descriptionLabel")}</h2>
            <p className="mt-2 whitespace-pre-wrap text-sm leading-relaxed text-muted-foreground">
              {offering.description || t("offerings.noDescription")}
            </p>
          </div>

          {offering.courseId && (
            <div className="rounded-xl border border-border bg-muted/50 p-4">
              <h2 className="flex items-center gap-2 text-sm font-semibold text-foreground">
                <BookOpen className="size-4 text-primary" />
                {t("offerings.courseLabel")}
              </h2>
              <Link
                href={`/dashboard/learner/courses/${offering.courseId}`}
                className="mt-3 inline-flex items-center gap-1.5 rounded-lg bg-primary px-4 py-2 text-xs font-medium text-primary-foreground transition-colors hover:bg-primary/90"
              >
                {t("offerings.openCourse")}
                <ArrowRight className="size-3.5" />
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
