"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import Link from "next/link"
import { learnerApi, type LiveClass } from "@/lib/learner-api"
import { LiveClassroom } from "@/components/live/live-classroom"
import { AuthGuard } from "@/components/auth/auth-guard"
import { SiteHeader } from "@/components/site-header"
import { Loader2, AlertCircle } from "lucide-react"

export default function LearnerClassroomPage() {
  const params = useParams()
  const id = params.id as string
  const [liveClass, setLiveClass] = useState<LiveClass | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")

  useEffect(() => {
    async function load() {
      try {
        const data = await learnerApi.getLiveClass(id)
        setLiveClass(data)
      } catch (e: any) {
        setError(e.message || "Failed to load live class")
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [id])

  return (
    <AuthGuard>
      <div className="flex min-h-dvh flex-col">
        <SiteHeader />
        <main className="flex-1 bg-muted/40">
          {loading ? (
            <div className="flex items-center justify-center py-20">
              <Loader2 className="size-8 animate-spin text-primary" />
            </div>
          ) : error ? (
            <div className="mx-auto max-w-lg py-20 text-center">
              <AlertCircle className="mx-auto size-12 text-destructive/40" />
              <p className="mt-4 text-lg font-medium">{error}</p>
              <Link
                href="/dashboard/learner/live-classes"
                className="mt-4 inline-block text-sm text-primary hover:underline"
              >
                Back to Live Classes
              </Link>
            </div>
          ) : liveClass ? (
            <LiveClassroom liveClass={liveClass} />
          ) : null}
        </main>
      </div>
    </AuthGuard>
  )
}
