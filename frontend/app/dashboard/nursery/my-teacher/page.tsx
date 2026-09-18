"use client"

import { useEffect, useState } from "react"
import { ArrowLeft, User, MessageSquare, BookOpen, Star } from "lucide-react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { primaryApi, type TeacherInfo } from "@/lib/api"
import { LoadingState } from "@/components/learner/shared"

export default function MyTeacherPage() {
  const { user } = useRequireAuth()
  const [teachers, setTeachers] = useState<TeacherInfo[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    primaryApi.getTeachers()
      .then(setTeachers)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingState />

  const primaryTeacher = teachers.length > 0 ? teachers[0] : null

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

      {primaryTeacher ? (
        <div className="nursery-card rounded-2xl bg-white p-6 text-center">
          <div className="mx-auto flex size-20 items-center justify-center rounded-full bg-indigo-100">
            {primaryTeacher.profileImageUrl ? (
              <img src={primaryTeacher.profileImageUrl} alt="" className="size-20 rounded-full object-cover" />
            ) : (
              <User className="size-10 text-indigo-500" />
            )}
          </div>
          <h2 className="mt-4 text-lg font-bold text-gray-800">
            {primaryTeacher.firstName} {primaryTeacher.lastName}
          </h2>
          <p className="mt-1 text-sm text-indigo-600 font-medium">{primaryTeacher.subjectName}</p>
          {primaryTeacher.specialization && (
            <p className="mt-0.5 text-xs text-gray-500">{primaryTeacher.specialization}</p>
          )}
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
      ) : (
        <div className="nursery-card rounded-2xl bg-white p-6 text-center">
          <div className="mx-auto flex size-20 items-center justify-center rounded-full bg-indigo-100">
            <User className="size-10 text-indigo-500" />
          </div>
          <h2 className="mt-4 text-lg font-bold text-gray-800">Your Teacher</h2>
          <p className="mt-1 text-sm text-gray-500">
            Your teacher guides your learning journey and creates activities just for you.
          </p>
        </div>
      )}

      {teachers.length > 1 && (
        <div className="nursery-card rounded-2xl bg-white p-5">
          <h3 className="font-bold text-gray-800 mb-3">All My Teachers</h3>
          <div className="space-y-2">
            {teachers.map(teacher => (
              <div key={teacher.id} className="flex items-center gap-3 rounded-xl bg-gray-50 p-3">
                <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-indigo-100">
                  <User className="size-5 text-indigo-500" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-gray-900">{teacher.firstName} {teacher.lastName}</p>
                  <p className="text-xs text-gray-500">{teacher.subjectName}</p>
                </div>
                <Star className="size-4 text-amber-400" />
              </div>
            ))}
          </div>
        </div>
      )}

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
