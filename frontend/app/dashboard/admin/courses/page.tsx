"use client"

import { useEffect, useState } from "react"
import { BookOpen, Plus, Pencil, Trash2, Eye, EyeOff, Star, Loader2, X, Check, Search, Filter } from "lucide-react"
import { courseApi, getInstitutionId, type Course, type CourseStats } from "@/lib/api"

const LEVELS = ["ALL_LEVELS", "NURSERY", "PRIMARY", "SECONDARY", "COLLEGE", "VETA", "UNIVERSITY"]
const CATEGORIES = ["Mathematics", "Science", "English", "History", "Geography", "Arts", "Physical Education", "Computer Science", "Languages", "Other"]

export default function CoursesPage() {
  const [courses, setCourses] = useState<Course[]>([])
  const [stats, setStats] = useState<CourseStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showForm, setShowForm] = useState(false)
  const [editingCourse, setEditingCourse] = useState<Course | null>(null)
  const [searchQuery, setSearchQuery] = useState("")
  const [filterLevel, setFilterLevel] = useState("")

  const [form, setForm] = useState({
    title: "",
    description: "",
    level: "ALL_LEVELS",
    category: "",
    isPublished: false,
    isFeatured: false,
  })

  const loadData = async () => {
    try {
      setLoading(true)
      const [coursesData, statsData] = await Promise.all([
        courseApi.listCourses(""),
        courseApi.getStats(),
      ])
      setCourses(coursesData)
      setStats(statsData)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load courses")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadData()
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      if (editingCourse) {
        await courseApi.updateCourse(editingCourse.id, form)
      } else {
        await courseApi.createCourse(form)
      }
      setShowForm(false)
      setEditingCourse(null)
      resetForm()
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save course")
    }
  }

  const handleDelete = async (courseId: string) => {
    if (!confirm("Are you sure you want to delete this course?")) return
    try {
      await courseApi.deleteCourse(courseId)
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete course")
    }
  }

  const handleTogglePublish = async (courseId: string) => {
    try {
      await courseApi.togglePublish(courseId)
      loadData()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to toggle publish status")
    }
  }

  const handleEdit = (course: Course) => {
    setEditingCourse(course)
    setForm({
      title: course.title,
      description: course.description || "",
      level: course.level,
      category: course.category || "",
      isPublished: course.isPublished,
      isFeatured: course.isFeatured,
    })
    setShowForm(true)
  }

  const resetForm = () => {
    setForm({ title: "", description: "", level: "ALL_LEVELS", category: "", isPublished: false, isFeatured: false })
  }

  const filteredCourses = courses.filter((c) => {
    const matchesSearch = !searchQuery || c.title.toLowerCase().includes(searchQuery.toLowerCase()) || c.description?.toLowerCase().includes(searchQuery.toLowerCase())
    const matchesLevel = !filterLevel || c.level === filterLevel
    return matchesSearch && matchesLevel
  })

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Course Management</h1>
          <p className="mt-1 text-sm text-muted-foreground">Manage all courses across all education levels.</p>
        </div>
        <button
          onClick={() => { resetForm(); setEditingCourse(null); setShowForm(true) }}
          className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" />
          New Course
        </button>
      </div>

      {stats && (
        <div className="grid gap-4 sm:grid-cols-4">
          <div className="rounded-xl border border-border bg-card p-4">
            <p className="text-2xl font-bold text-foreground">{stats.totalCourses}</p>
            <p className="text-xs text-muted-foreground">Total Courses</p>
          </div>
          <div className="rounded-xl border border-border bg-card p-4">
            <p className="text-2xl font-bold text-emerald-600">{stats.publishedCourses}</p>
            <p className="text-xs text-muted-foreground">Published</p>
          </div>
          <div className="rounded-xl border border-border bg-card p-4">
            <p className="text-2xl font-bold text-yellow-600">{stats.draftCourses}</p>
            <p className="text-xs text-muted-foreground">Drafts</p>
          </div>
          <div className="rounded-xl border border-border bg-card p-4">
            <p className="text-2xl font-bold text-indigo-600">{stats.totalModules}</p>
            <p className="text-xs text-muted-foreground">Modules / {stats.totalLessons} Lessons</p>
          </div>
        </div>
      )}

      {loading && (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      )}

      {error && (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4 text-center">
          <p className="text-sm font-medium text-destructive">{error}</p>
          <button onClick={() => setError(null)} className="mt-2 text-xs text-destructive underline">Dismiss</button>
        </div>
      )}

      {!loading && (
        <div className="space-y-4">
          <div className="flex gap-3">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <input
                type="text"
                placeholder="Search courses..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full rounded-xl border border-border bg-card pl-10 pr-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>
            <select
              value={filterLevel}
              onChange={(e) => setFilterLevel(e.target.value)}
              className="rounded-xl border border-border bg-card px-4 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option value="">All Levels</option>
              {LEVELS.map((l) => (
                <option key={l} value={l}>{l.replace(/_/g, " ")}</option>
              ))}
            </select>
          </div>

          {filteredCourses.length === 0 ? (
            <div className="rounded-2xl border border-border bg-card py-16 text-center">
              <BookOpen className="mx-auto size-12 text-muted-foreground/50" />
              <p className="mt-4 text-sm text-muted-foreground">No courses found. Create your first course to get started.</p>
            </div>
          ) : (
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              {filteredCourses.map((course) => (
                <div key={course.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs transition-shadow hover:shadow-md">
                  <div className="flex items-start justify-between">
                    <div className="flex-1 min-w-0">
                      <h3 className="font-semibold text-foreground truncate">{course.title}</h3>
                      <p className="mt-1 text-xs text-muted-foreground line-clamp-2">{course.description || "No description"}</p>
                    </div>
                    {course.isFeatured && <Star className="size-4 shrink-0 text-amber-500 fill-amber-500" />}
                  </div>

                  <div className="mt-3 flex flex-wrap gap-2">
                    <span className="inline-flex items-center rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">
                      {course.level.replace(/_/g, " ")}
                    </span>
                    {course.category && (
                      <span className="inline-flex items-center rounded-full bg-secondary px-2.5 py-0.5 text-xs font-medium text-secondary-foreground">
                        {course.category}
                      </span>
                    )}
                    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${course.isPublished ? "bg-emerald-500/10 text-emerald-600" : "bg-muted text-muted-foreground"}`}>
                      {course.isPublished ? "Published" : "Draft"}
                    </span>
                  </div>

                  <div className="mt-3 flex items-center gap-4 text-xs text-muted-foreground">
                    <span>{course.moduleCount} modules</span>
                    <span>{course.lessonCount} lessons</span>
                    {course.createdByName && <span>by {course.createdByName}</span>}
                  </div>

                  <div className="mt-4 flex items-center gap-2 border-t border-border pt-3">
                    <button
                      onClick={() => handleTogglePublish(course.id)}
                      className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted"
                    >
                      {course.isPublished ? <EyeOff className="size-3.5" /> : <Eye className="size-3.5" />}
                      {course.isPublished ? "Unpublish" : "Publish"}
                    </button>
                    <button
                      onClick={() => handleEdit(course)}
                      className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium text-muted-foreground hover:bg-muted"
                    >
                      <Pencil className="size-3.5" />
                      Edit
                    </button>
                    <button
                      onClick={() => handleDelete(course.id)}
                      className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium text-destructive hover:bg-destructive/10 ml-auto"
                    >
                      <Trash2 className="size-3.5" />
                      Delete
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {showForm && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="mx-4 w-full max-w-lg rounded-2xl border border-border bg-card p-6 shadow-xl">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold text-foreground">{editingCourse ? "Edit Course" : "New Course"}</h2>
              <button onClick={() => { setShowForm(false); setEditingCourse(null) }} className="text-muted-foreground hover:text-foreground">
                <X className="size-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="mt-5 space-y-4">
              <div>
                <label className="text-sm font-medium text-foreground">Title *</label>
                <input
                  type="text"
                  required
                  value={form.title}
                  onChange={(e) => setForm({ ...form, title: e.target.value })}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
                  placeholder="e.g. Introduction to Mathematics"
                />
              </div>

              <div>
                <label className="text-sm font-medium text-foreground">Description</label>
                <textarea
                  value={form.description}
                  onChange={(e) => setForm({ ...form, description: e.target.value })}
                  rows={3}
                  className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
                  placeholder="Brief description of the course"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="text-sm font-medium text-foreground">Level</label>
                  <select
                    value={form.level}
                    onChange={(e) => setForm({ ...form, level: e.target.value })}
                    className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
                  >
                    {LEVELS.map((l) => (
                      <option key={l} value={l}>{l.replace(/_/g, " ")}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-sm font-medium text-foreground">Category</label>
                  <select
                    value={form.category}
                    onChange={(e) => setForm({ ...form, category: e.target.value })}
                    className="mt-1 w-full rounded-xl border border-border bg-background px-4 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
                  >
                    <option value="">Select category</option>
                    {CATEGORIES.map((c) => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="flex gap-6">
                <label className="flex items-center gap-2 text-sm text-foreground">
                  <input
                    type="checkbox"
                    checked={form.isPublished}
                    onChange={(e) => setForm({ ...form, isPublished: e.target.checked })}
                    className="size-4 rounded border-border"
                  />
                  Published
                </label>
                <label className="flex items-center gap-2 text-sm text-foreground">
                  <input
                    type="checkbox"
                    checked={form.isFeatured}
                    onChange={(e) => setForm({ ...form, isFeatured: e.target.checked })}
                    className="size-4 rounded border-border"
                  />
                  Featured
                </label>
              </div>

              <div className="flex justify-end gap-3 pt-2">
                <button
                  type="button"
                  onClick={() => { setShowForm(false); setEditingCourse(null) }}
                  className="rounded-xl border border-border px-4 py-2.5 text-sm font-medium text-muted-foreground hover:bg-muted"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90"
                >
                  <Check className="size-4" />
                  {editingCourse ? "Update" : "Create"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}
