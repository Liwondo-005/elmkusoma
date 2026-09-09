"use client"

import { useEffect, useState } from "react"
import Link from "next/link"
import { useAuth } from "@/lib/auth"
import { learningApi, type Lesson } from "@/lib/api"
import { Button } from "@/components/ui/button"
import { BookOpen, Plus, Clock, CheckCircle, ArrowRight, Filter } from "lucide-react"

export default function TeacherCoursesPage() {
  const { user } = useAuth()
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user) return
    loadData()
  }, [user])

  async function loadData() {
    try {
      setLoading(true)
      const data = await learningApi.getLessonsByClass(user?.classGroupId || "")
      setLessons(data)
    } catch {
      setLessons([])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">My Courses</h1>
          <p className="mt-1 text-sm text-muted-foreground">Manage your courses and lessons.</p>
        </div>
        <Button className="gap-2">
          <Plus className="size-4" />
          Create Course
        </Button>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <div className="size-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
        </div>
      ) : lessons.length === 0 ? (
        <div className="rounded-2xl border border-border bg-card p-12 text-center">
          <BookOpen className="mx-auto size-12 text-muted-foreground/50" />
          <h3 className="mt-4 text-lg font-semibold text-foreground">No Courses Yet</h3>
          <p className="mt-2 text-sm text-muted-foreground">Create your first course to get started.</p>
        </div>
      ) : (
        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
          {lessons.map((lesson) => (
            <div
              key={lesson.id}
              className="group rounded-2xl border border-border bg-card p-5 transition-all hover:-translate-y-0.5 hover:shadow-md"
            >
              <div className="flex items-start justify-between">
                <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
                  <BookOpen className="size-5 text-primary" />
                </div>
                <span className="text-xs font-medium text-muted-foreground">
                  {lesson.isPublished ? "Published" : "Draft"}
                </span>
              </div>
              <h3 className="mt-3 text-sm font-semibold text-foreground line-clamp-1">{lesson.title}</h3>
              {lesson.description && (
                <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{lesson.description}</p>
              )}
              <div className="mt-4 flex items-center justify-between">
                <Link
                  href={`/dashboard/lessons/${lesson.id}`}
                  className="flex items-center gap-1 text-xs font-medium text-primary hover:underline"
                >
                  View Details <ArrowRight className="size-3" />
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
