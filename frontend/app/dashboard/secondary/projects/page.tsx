"use client"

import { ArrowLeft, FolderOpen } from "lucide-react"
import Link from "next/link"

export default function SecondaryProjectsPage() {
  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Projects</h1>
          <p className="text-sm text-gray-500">Research, investigate, and create</p>
        </div>
      </div>

      <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
        <FolderOpen className="mx-auto size-12 text-gray-300" />
        <h3 className="mt-3 text-lg font-bold text-gray-800">Projects & Research</h3>
        <p className="mt-1 text-sm text-gray-500">
          Work on projects, conduct research, and present your findings. Projects assigned by your teachers will appear here.
        </p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {[
          { step: "1", title: "Question", desc: "Define what you want to investigate" },
          { step: "2", title: "Research", desc: "Gather information and sources" },
          { step: "3", title: "Plan", desc: "Organize your approach" },
          { step: "4", title: "Create", desc: "Build your project" },
          { step: "5", title: "Present", desc: "Share your findings" },
          { step: "6", title: "Improve", desc: "Incorporate feedback" },
        ].map(item => (
          <div key={item.step} className="flex items-center gap-3 rounded-2xl border border-gray-100 bg-white p-4">
            <div className="flex size-8 shrink-0 items-center justify-center rounded-full bg-indigo-50 text-sm font-bold text-indigo-600">
              {item.step}
            </div>
            <div>
              <p className="text-sm font-semibold text-gray-900">{item.title}</p>
              <p className="text-xs text-gray-400">{item.desc}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
