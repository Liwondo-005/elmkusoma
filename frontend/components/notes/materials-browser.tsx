"use client"

import { useState, useMemo } from "react"
import { Search, BookOpenCheck } from "lucide-react"
import { studyMaterials } from "@/lib/data"
import type { EducationLevel } from "@/lib/data"
import { MaterialCard } from "@/components/notes/material-card"
import { cn } from "@/lib/utils"

const levels: { id: string; label: string }[] = [
  { id: "all", label: "All Levels" },
  { id: "Nursery", label: "Nursery" },
  { id: "Primary", label: "Primary" },
  { id: "Secondary", label: "Secondary" },
  { id: "College", label: "College" },
  { id: "University", label: "University" },
]

const subjects = [
  "All",
  "Mathematics",
  "English",
  "Kiswahili",
  "Science",
  "Physics",
  "Chemistry",
  "Biology",
  "Computer Science",
  "Geography",
  "History",
  "Civic Education",
  "Religious Studies",
  "Business Studies",
  "Creative Arts",
]

export function MaterialsBrowser() {
  const [search, setSearch] = useState("")
  const [level, setLevel] = useState("all")
  const [subject, setSubject] = useState("All")

  const filtered = useMemo(() => {
    return studyMaterials.filter((m) => {
      const matchesSearch =
        !search ||
        m.title.toLowerCase().includes(search.toLowerCase()) ||
        m.subject.toLowerCase().includes(search.toLowerCase()) ||
        m.instructor.toLowerCase().includes(search.toLowerCase())
      const matchesLevel = level === "all" || m.level === level
      const matchesSubject = subject === "All" || m.subject === subject
      return matchesSearch && matchesLevel && matchesSubject
    })
  }, [search, level, subject])

  return (
    <section className="py-10">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="relative flex-1 sm:max-w-md">
            <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <input
              type="search"
              placeholder="Search notes, subjects, instructors..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="h-10 w-full rounded-xl border border-border bg-background pl-10 pr-4 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:ring-2 focus:ring-ring/20"
            />
          </div>

          <div className="flex flex-wrap gap-2">
            <select
              value={level}
              onChange={(e) => setLevel(e.target.value)}
              className="h-10 rounded-xl border border-border bg-background px-3 text-sm text-foreground outline-none transition-colors focus:border-ring focus:ring-2 focus:ring-ring/20"
            >
              {levels.map((l) => (
                <option key={l.id} value={l.id}>
                  {l.label}
                </option>
              ))}
            </select>

            <select
              value={subject}
              onChange={(e) => setSubject(e.target.value)}
              className="h-10 rounded-xl border border-border bg-background px-3 text-sm text-foreground outline-none transition-colors focus:border-ring focus:ring-2 focus:ring-ring/20"
            >
              {subjects.map((s) => (
                <option key={s} value={s}>
                  {s === "All" ? "All Subjects" : s}
                </option>
              ))}
            </select>
          </div>
        </div>

        {filtered.length > 0 ? (
          <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
            {filtered.map((material) => (
              <MaterialCard key={material.id} material={material} />
            ))}
          </div>
        ) : (
          <div className="mt-8 flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/40 py-20 text-center">
            <BookOpenCheck className="mx-auto size-10 text-muted-foreground/40" />
            <p className="mt-4 text-sm font-medium text-foreground">No materials found</p>
            <p className="mt-1 text-sm text-muted-foreground">
              Try adjusting your search or filters.
            </p>
          </div>
        )}
      </div>
    </section>
  )
}
