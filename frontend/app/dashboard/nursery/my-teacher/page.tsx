"use client"

import { ArrowLeft, User, MessageSquare } from "lucide-react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"

export default function MyTeacherPage() {
  const { user } = useRequireAuth()

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">My Teacher</h1>
          <p className="text-sm text-gray-500">Your nursery teacher</p>
        </div>
      </div>

      <div className="nursery-card rounded-2xl bg-white p-6 text-center">
        <div className="mx-auto flex size-20 items-center justify-center rounded-full bg-indigo-100">
          <User className="size-10 text-indigo-500" />
        </div>
        <h2 className="mt-4 text-lg font-bold text-gray-800">Your Teacher</h2>
        <p className="mt-1 text-sm text-gray-500">
          Your teacher guides your learning journey and creates activities just for you.
        </p>
        <div className="mt-4 grid grid-cols-2 gap-3">
          <div className="rounded-xl bg-green-50 p-3">
            <p className="text-lg font-bold text-green-600">Classes</p>
            <p className="text-xs text-gray-500">With your teacher</p>
          </div>
          <div className="rounded-xl bg-blue-50 p-3">
            <p className="text-lg font-bold text-blue-600">Activities</p>
            <p className="text-xs text-gray-500">Created for you</p>
          </div>
        </div>
      </div>

      <div className="nursery-card rounded-2xl bg-white p-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-full bg-pink-100">
            <MessageSquare className="size-5 text-pink-500" />
          </div>
          <div>
            <p className="font-bold text-gray-800">Send a Message</p>
            <p className="text-xs text-gray-500">Tell your teacher something</p>
          </div>
        </div>
        <textarea
          className="mt-3 w-full rounded-xl border border-gray-200 p-3 text-sm"
          rows={3}
          placeholder="Type a message to your teacher..."
          readOnly
        />
        <button className="mt-2 w-full rounded-xl bg-indigo-100 py-2 text-sm font-medium text-indigo-600" disabled>
          Send Message
        </button>
      </div>
    </div>
  )
}
