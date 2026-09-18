"use client"

import { ArrowLeft, TrendingUp, BookOpen, FileText, AlertCircle, Calendar, Lightbulb, Brain, RefreshCw, Clock, FlaskConical, Search } from "lucide-react"
import Link from "next/link"

const QUICK_ACTIONS = [
  { icon: FileText, title: "Upcoming Assessments", desc: "Review what is coming next", color: "bg-amber-50 text-amber-600", href: "/dashboard/secondary/assess" },
  { icon: BookOpen, title: "Recently Studied", desc: "Revisit recent topics", color: "bg-blue-50 text-blue-600", href: "/dashboard/secondary/learn" },
  { icon: TrendingUp, title: "Practice Sets", desc: "Strengthen weak areas", color: "bg-green-50 text-green-600", href: "/dashboard/secondary/practice" },
  { icon: AlertCircle, title: "Exam Prep", desc: "Prepare for upcoming exams", color: "bg-orange-50 text-orange-600", href: "/dashboard/secondary/revision/exam-prep" },
  { icon: Calendar, title: "Study Planner", desc: "Plan your study sessions", color: "bg-indigo-50 text-indigo-600", href: "/dashboard/secondary/revision/study-planner" },
  { icon: Lightbulb, title: "Concept Explorer", desc: "Break down complex ideas", color: "bg-yellow-50 text-yellow-600", href: "/dashboard/secondary/learn/concept-explorer" },
  { icon: Brain, title: "Problem Solving", desc: "Practice solving problems", color: "bg-purple-50 text-purple-600", href: "/dashboard/secondary/learn/problem-solving" },
  { icon: RefreshCw, title: "Error Analysis", desc: "Learn from common mistakes", color: "bg-red-50 text-red-600", href: "/dashboard/secondary/learn/error-analysis" },
  { icon: Clock, title: "Exam Mode", desc: "Timed practice exam", color: "bg-rose-50 text-rose-600", href: "/dashboard/secondary/learn/exam-mode" },
]

export default function SecondaryRevisionPage() {
  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">Revision Center</h1>
          <p className="text-sm text-gray-500">Review, practice, and prepare</p>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {QUICK_ACTIONS.map(item => (
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
    </div>
  )
}
