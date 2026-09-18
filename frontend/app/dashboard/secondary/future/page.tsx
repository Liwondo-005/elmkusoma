"use client"

import { ArrowLeft, Globe, GraduationCap, Wrench, Briefcase, Lightbulb } from "lucide-react"
import Link from "next/link"

const CAREER_PATHS = [
  { icon: GraduationCap, title: "University", desc: "Bachelor's degree programs", color: "bg-blue-50 text-blue-600" },
  { icon: Wrench, title: "TVET / College", desc: "Technical and vocational training", color: "bg-green-50 text-green-600" },
  { icon: Briefcase, title: "Career Paths", desc: "Explore professional opportunities", color: "bg-purple-50 text-purple-600" },
  { icon: Lightbulb, title: "Skills & Industries", desc: "Discover in-demand skills", color: "bg-amber-50 text-amber-600" },
]

const FIELDS = [
  "Medicine & Health Sciences",
  "Engineering & Technology",
  "Computer Science & IT",
  "Business & Economics",
  "Education & Teaching",
  "Agriculture & Environmental Science",
  "Law & Social Sciences",
  "Arts & Humanities",
  "Natural Sciences",
  "Hospitality & Tourism",
]

export default function SecondaryFuturePage() {
  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Future World</h1>
          <p className="text-sm text-gray-500">Explore what comes after secondary school</p>
        </div>
      </div>

      <div className="rounded-2xl bg-gradient-to-r from-indigo-600 to-blue-500 p-5 text-white">
        <h2 className="text-lg font-bold">Explore Possibilities</h2>
        <p className="mt-1 text-sm text-white/70">
          Your learning journey opens doors to many paths. Discover what excites you.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {CAREER_PATHS.map(path => (
          <div key={path.title} className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-5">
            <div className={`flex size-12 shrink-0 items-center justify-center rounded-xl ${path.color}`}>
              <path.icon className="size-6" />
            </div>
            <div>
              <p className="font-semibold text-gray-900">{path.title}</p>
              <p className="text-sm text-gray-400">{path.desc}</p>
            </div>
          </div>
        ))}
      </div>

      <section className="rounded-2xl border border-gray-100 bg-white p-5">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-gray-500">University Fields</h2>
        <div className="mt-3 grid gap-2 sm:grid-cols-2">
          {FIELDS.map(field => (
            <div key={field} className="flex items-center gap-2 rounded-xl bg-gray-50 px-4 py-2.5">
              <Globe className="size-3.5 text-gray-400" />
              <span className="text-sm text-gray-700">{field}</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
