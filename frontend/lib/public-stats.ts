"use client"

import { useEffect, useState } from "react"

export interface PublicStats {
  learners: number
  instructors: number
  institutions: number
  courses: number
  liveClasses: number
}

export interface PublicCourse {
  id: string
  title: string
  description?: string | null
  level?: string | null
  category?: string | null
  thumbnailUrl?: string | null
}

// Module-level cache: multiple homepage sections share one request, and the
// promise survives client-side navigations back to the homepage.
let statsPromise: Promise<PublicStats | null> | null = null

function fetchStats(): Promise<PublicStats | null> {
  if (!statsPromise) {
    statsPromise = fetch("/v1/public/stats")
      .then((res) => (res.ok ? res.json() : null))
      .then((body: { success?: boolean; data?: PublicStats } | null) =>
        body?.success && body.data ? body.data : null,
      )
      .catch(() => null)
  }
  return statsPromise
}

/** Real platform statistics, or null while loading / on failure (hide-on-failure). */
export function usePublicStats(): PublicStats | null {
  const [stats, setStats] = useState<PublicStats | null>(null)

  useEffect(() => {
    let mounted = true
    fetchStats().then((data) => {
      if (mounted) setStats(data)
    })
    return () => {
      mounted = false
    }
  }, [])

  return stats
}

/** Published courses for anonymous visitors, or null while loading / on failure. */
export function usePublicCourses(limit = 6): PublicCourse[] | null {
  const [courses, setCourses] = useState<PublicCourse[] | null>(null)

  useEffect(() => {
    let mounted = true
    fetch(`/v1/public/courses?limit=${limit}`)
      .then((res) => (res.ok ? res.json() : null))
      .then((body: { success?: boolean; data?: PublicCourse[] } | null) => {
        if (!mounted) return
        setCourses(body?.success && Array.isArray(body.data) && body.data.length > 0 ? body.data : null)
      })
      .catch(() => {
        if (mounted) setCourses(null)
      })
    return () => {
      mounted = false
    }
  }, [limit])

  return courses
}
