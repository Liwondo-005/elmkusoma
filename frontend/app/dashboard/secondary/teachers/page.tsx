"use client"

import { ArrowLeft, Users } from "lucide-react"
import Link from "next/link"

export default function SecondaryTeachersPage() {
  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/secondary" className="flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-900">My Teachers</h1>
          <p className="text-sm text-gray-500">Your subject teachers and their feedback</p>
        </div>
      </div>

      <div className="rounded-2xl border border-gray-100 bg-white p-8 text-center">
        <Users className="mx-auto size-12 text-gray-300" />
        <h3 className="mt-3 text-lg font-bold text-gray-800">Your Teachers</h3>
        <p className="mt-1 text-sm text-gray-500">
          Information about your subject teachers, their feedback, and how to reach them will appear here.
        </p>
      </div>
    </div>
  )
}
