"use client"

import { ArrowLeft, TrendingUp, BookOpen, FileText, Clock, AlertCircle } from "lucide-react"
import Link from "next/link"

export default function SecondaryRevisionPage() {
  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Revision Center</h1>
          <p className="text-sm text-gray-500">Review and prepare for assessments</p>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2">
        {[
          { icon: FileText, title: "Upcoming Assessments", desc: "Review what is coming next", color: "bg-amber-50 text-amber-600", href: "/dashboard/secondary/assess" },
          { icon: BookOpen, title: "Recently Studied", desc: "Revisit recent topics", color: "bg-blue-50 text-blue-600", href: "/dashboard/secondary/learn" },
          { icon: TrendingUp, title: "Practice Sets", desc: "Strengthen weak areas", color: "bg-green-50 text-green-600", href: "/dashboard/secondary/practice" },
          { icon: AlertCircle, title: "Topics to Review", desc: "Focus on areas needing practice", color: "bg-orange-50 text-orange-600", href: "/dashboard/secondary/learn" },
        ].map(item => (
          <Link
            key={item.title}
            href={item.href}
            className="flex items-center gap-4 rounded-2xl border border-gray-100 bg-white p-5 transition-all hover:border-indigo-200 hover:shadow-sm"
          >
            <div className={`flex size-12 shrink-0 items-center justify-center rounded-xl ${item.color}`}>
              <item.icon className="size-6" />
            </div>
            <div>
              <p className="font-semibold text-gray-900">{item.title}</p>
              <p className="text-sm text-gray-400">{item.desc}</p>
            </div>
          </Link>
        ))}
      </div>

      <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
        <TrendingUp className="mx-auto size-12 text-gray-300" />
        <h3 className="mt-3 text-lg font-bold text-gray-800">Build your revision plan</h3>
        <p className="mt-1 text-sm text-gray-500">
          Use the sections above to review topics, practice problems, and prepare for upcoming assessments.
        </p>
      </div>
    </div>
  )
}
