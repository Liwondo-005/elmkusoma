"use client"

import { useMemo, useState } from "react"

import type { CurriculumSubject, SubjectCategory } from "@/lib/data"
import { SubjectCard } from "@/components/courses/subject-card"
import { cn } from "@/lib/utils"

export function ClassSubjectsBrowser({
  subjects,
  categories,
  levelSlug,
  classNameSlug,
  displayName,
}: {
  subjects: CurriculumSubject[]
  categories: SubjectCategory[]
  levelSlug: string
  classNameSlug: string
  displayName: string
}) {
  const [search, setSearch] = useState("")
  const [selectedCategory, setSelectedCategory] = useState<SubjectCategory | "All">("All")

  const filtered = useMemo(() => {
    return subjects.filter((s) => {
      if (selectedCategory !== "All" && s.category !== selectedCategory) return false
      if (search.trim()) {
        const q = search.toLowerCase()
        return (
          s.name.toLowerCase().includes(q) ||
          s.description.toLowerCase().includes(q) ||
          s.category.toLowerCase().includes(q)
        )
      }
      return true
    })
  }, [search, selectedCategory, subjects])

  const hasFilters = search || selectedCategory !== "All"

  function clearFilters() {
    setSearch("")
    setSelectedCategory("All")
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <label className="relative flex items-center sm:flex-1 sm:max-w-md">
          <input
            type="search"
            placeholder="Search subjects..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="h-10 w-full rounded-lg border border-border bg-muted/60 px-3 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:bg-background"
          />
        </label>
        {hasFilters && (
          <button
            onClick={clearFilters}
            className="inline-flex items-center gap-1.5 text-xs font-medium text-muted-foreground transition-colors hover:text-foreground"
          >
            Clear filters
          </button>
        )}
      </div>

      {categories.length > 1 && (
        <div className="mt-4 flex flex-wrap gap-1.5">
          <button
            onClick={() => setSelectedCategory("All")}
            className={cn(
              "rounded-md px-3 py-1.5 text-xs font-medium transition-colors",
              selectedCategory === "All"
                ? "bg-primary text-primary-foreground"
                : "bg-muted text-muted-foreground hover:text-foreground"
            )}
          >
            All
          </button>
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={cn(
                "rounded-md px-3 py-1.5 text-xs font-medium transition-colors",
                selectedCategory === cat
                  ? "bg-primary text-primary-foreground"
                  : "bg-muted text-muted-foreground hover:text-foreground"
              )}
            >
              {cat}
            </button>
          ))}
        </div>
      )}

      <div className="mt-6 flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          <span className="font-semibold text-foreground">{filtered.length}</span>{" "}
          {filtered.length === 1 ? "subject" : "subjects"} found
        </p>
      </div>

      {filtered.length > 0 ? (
        <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {filtered.map((subject) => (
            <SubjectCard key={subject.id} subject={subject} levelSlug={levelSlug} classNameSlug={classNameSlug} />
          ))}
        </div>
      ) : (
        <div className="mt-12 flex flex-col items-center justify-center text-center">
          <div className="flex size-14 items-center justify-center rounded-full bg-muted">
          </div>
          <h3 className="mt-4 text-sm font-semibold text-foreground">No subjects found</h3>
          <p className="mt-1 max-w-sm text-sm text-muted-foreground">
            Try adjusting your search or category filter.
          </p>
          <button
            onClick={clearFilters}
            className="mt-4 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90"
          >
            Clear filters
          </button>
        </div>
      )}
    </div>
  )
}
