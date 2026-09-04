"use client"

import { useState } from "react"
import { secondaryForms, aLevelCombinations } from "@/lib/data"
import { cn } from "@/lib/utils"

export function SecondaryLevels() {
  const [activeTab, setActiveTab] = useState<"o-level" | "a-level">("o-level")
  const [activeFormId, setActiveFormId] = useState("form-1")
  const [activeComboId, setActiveComboId] = useState("pcm")

  const oLevelForms = secondaryForms.filter((f) => f.level === "O-Level")
  const activeForm = secondaryForms.find((f) => f.id === activeFormId) || oLevelForms[0]
  const activeCombo = aLevelCombinations.find((c) => c.id === activeComboId) || aLevelCombinations[0]

  return (
    <section className="py-12 lg:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <h2 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            Forms & Subjects
          </h2>
          <p className="mt-2 text-muted-foreground">
            Browse O-Level subjects by form or explore A-Level combination groups.
          </p>
        </div>

        {/* Tab switcher */}
        <div className="mt-6 flex items-center gap-1 border-b border-border">
          <button
            type="button"
            onClick={() => setActiveTab("o-level")}
            className={cn(
              "relative flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors",
              activeTab === "o-level" ? "text-primary" : "text-muted-foreground hover:text-foreground",
            )}
          >
            O-Level (Form I–IV)
            {activeTab === "o-level" && (
              <span className="absolute inset-x-2 -bottom-px h-0.5 rounded-full bg-primary" />
            )}
          </button>
          <button
            type="button"
            onClick={() => setActiveTab("a-level")}
            className={cn(
              "relative flex items-center gap-2 px-4 py-3 text-sm font-medium transition-colors",
              activeTab === "a-level" ? "text-primary" : "text-muted-foreground hover:text-foreground",
            )}
          >
            A-Level (Form V–VI)
            {activeTab === "a-level" && (
              <span className="absolute inset-x-2 -bottom-px h-0.5 rounded-full bg-primary" />
            )}
          </button>
        </div>

        {activeTab === "o-level" ? (
          <div className="mt-8 grid gap-6 lg:grid-cols-[260px_1fr]">
            {/* Form tabs */}
            <div className="flex flex-col gap-2 lg:sticky lg:top-24 lg:self-start">
              {oLevelForms.map((form) => (
                <button
                  key={form.id}
                  type="button"
                  onClick={() => setActiveFormId(form.id)}
                  className={cn(
                    "flex items-center gap-3 rounded-xl border px-4 py-3 text-left text-sm font-medium transition-all",
                    activeFormId === form.id
                      ? "border-primary bg-accent text-primary shadow-xs"
                      : "border-border bg-card text-foreground hover:border-primary/40 hover:shadow-xs",
                  )}
                >
                  <div>
                    <p className="font-semibold">{form.name}</p>
                    <p className="text-xs text-muted-foreground">{form.label} · {form.ages}</p>
                  </div>
                </button>
              ))}
            </div>

            {/* Subjects panel */}
            <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
              <div className="flex items-center gap-3">
                <div>
                  <h3 className="text-lg font-bold text-foreground">{activeForm.name}</h3>
                  <p className="text-sm text-muted-foreground">{activeForm.label} · {activeForm.ages} · O-Level</p>
                </div>
              </div>

              <div className="mt-6">
                <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Masomo / Subjects ({activeForm.subjects.length})
                </p>
                <div className="mt-3 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                  {activeForm.subjects.map((subject, i) => (
                    <div
                      key={subject}
                      className="flex items-center gap-2 rounded-lg border border-border px-3 py-2.5"
                    >
                      <span className="flex size-7 shrink-0 items-center justify-center rounded-md bg-accent text-xs font-bold text-primary">
                        {i + 1}
                      </span>
                      <span className="text-sm font-medium text-foreground">{subject}</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        ) : (
          <div className="mt-8 grid gap-6 lg:grid-cols-[260px_1fr]">
            {/* Combination tabs */}
            <div className="flex flex-col gap-2 lg:sticky lg:top-24 lg:self-start">
              {aLevelCombinations.map((combo) => (
                <button
                  key={combo.id}
                  type="button"
                  onClick={() => setActiveComboId(combo.id)}
                  className={cn(
                    "flex items-center gap-3 rounded-xl border px-4 py-3 text-left text-sm font-medium transition-all",
                    activeComboId === combo.id
                      ? "border-primary bg-accent text-primary shadow-xs"
                      : "border-border bg-card text-foreground hover:border-primary/40 hover:shadow-xs",
                  )}
                >
                  <div
                    className={cn(
                      "flex size-9 shrink-0 items-center justify-center rounded-lg text-xs font-bold",
                      activeComboId === combo.id ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground",
                    )}
                  >
                    {combo.abbreviation}
                  </div>
                  <div>
                    <p className="font-semibold">{combo.name}</p>
                    <p className="text-xs text-muted-foreground">{combo.abbreviation} · Form V–VI</p>
                  </div>
                </button>
              ))}
            </div>

            {/* Combination detail */}
            <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex size-11 items-center justify-center rounded-xl bg-teal text-teal-foreground text-sm font-bold">
                  {activeCombo.abbreviation}
                </div>
                <div>
                  <h3 className="text-lg font-bold text-foreground">{activeCombo.name}</h3>
                  <p className="text-sm text-muted-foreground">Form V–VI · A-Level</p>
                </div>
              </div>

              <p className="mt-4 text-sm text-muted-foreground">{activeCombo.description}</p>

              <div className="mt-6">
                <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                  Core Subjects ({activeCombo.subjects.length})
                </p>
                <div className="mt-3 grid gap-2 sm:grid-cols-2">
                  {activeCombo.subjects.map((subject, i) => (
                    <div
                      key={subject}
                      className="flex items-center gap-2 rounded-lg border border-border px-3 py-2.5"
                    >
                      <span className="flex size-7 shrink-0 items-center justify-center rounded-md bg-teal/10 text-xs font-bold text-teal">
                        {i + 1}
                      </span>
                      <span className="text-sm font-medium text-foreground">{subject}</span>
                    </div>
                  ))}
                </div>
              </div>

              <div className="mt-6 rounded-xl bg-muted/50 p-4">
                <p className="text-xs font-semibold text-muted-foreground">Also studied at A-Level</p>
                <p className="mt-1 text-sm text-foreground">
                  General Studies · Communication Skills
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </section>
  )
}
