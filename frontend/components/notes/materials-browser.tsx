"use client"

import { useState, useMemo } from "react"
import { useTranslations } from "next-intl"
import { studyMaterials } from "@/lib/data"
import { MaterialCard } from "@/components/notes/material-card"
import { cn } from "@/lib/utils"

const levelIds = ["all", "Nursery", "Primary", "Secondary", "College", "University"] as const

function levelLabel(id: string, allLevels: string) {
  return id === "all" ? allLevels : id
}

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
  const t = useTranslations("ui")
  const tc = useTranslations("common")
  const tl = useTranslations("learner")
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
            <input
              type="search"
              placeholder={t("materialsBrowser.searchPlaceholder")}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="h-10 w-full rounded-xl border border-border bg-background px-4 text-sm text-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-ring focus:ring-2 focus:ring-ring/20"
            />
          </div>

          <div className="flex flex-wrap gap-2">
            <select
              value={level}
              onChange={(e) => setLevel(e.target.value)}
              className="h-10 rounded-xl border border-border bg-background px-3 text-sm text-foreground outline-none transition-colors focus:border-ring focus:ring-2 focus:ring-ring/20"
            >
              {levelIds.map((id) => (
                <option key={id} value={id}>
                  {levelLabel(id, tc("allLevels"))}
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
                  {s === "All" ? t("materialsBrowser.allSubjects") : s}
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
            <p className="mt-4 text-sm font-medium text-foreground">{t("materialsBrowser.emptyTitle")}</p>
            <p className="mt-1 text-sm text-muted-foreground">
              {tl("tryAdjusting")}
            </p>
          </div>
        )}
      </div>
    </section>
  )
}
