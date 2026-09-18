"use client"

import { useEffect, useState } from "react"
import { ArrowLeft, Users, BookOpen, HandHelping, User } from "lucide-react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { teacherApi, type StudentInClass } from "@/lib/teacher-api"
import { LoadingState } from "@/components/learner/shared"

export default function LearnTogetherPage() {
  const { user } = useRequireAuth()
  const [classmates, setClassmates] = useState<StudentInClass[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    teacherApi.getStudents()
      .then(students => setClassmates(students.slice(0, 8)))
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingState />

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">Learn Together</h1>
          <p className="text-sm text-gray-500">Learning is better with friends</p>
        </div>
      </div>

      <div className="nursery-card rounded-2xl bg-gradient-to-r from-blue-400 to-cyan-400 p-5 text-white">
        <div className="flex items-center gap-3">
          <Users className="size-8" />
          <div>
            <h2 className="text-lg font-bold">Group Activities</h2>
            <p className="text-sm text-white/70">Do activities with your classmates</p>
          </div>
        </div>
      </div>

      {classmates.length > 0 && (
        <div className="nursery-card rounded-2xl bg-white p-5">
          <h3 className="font-bold text-gray-800 mb-3">My Classmates</h3>
          <div className="grid grid-cols-4 gap-3">
            {classmates.map(student => (
              <div key={student.id} className="flex flex-col items-center gap-1">
                <div className="flex size-12 items-center justify-center rounded-full bg-indigo-100">
                  <User className="size-6 text-indigo-500" />
                </div>
                <p className="text-xs font-medium text-gray-700 text-center truncate w-full">
                  {student.firstName || "Friend"}
                </p>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="space-y-3">
        {[
          { icon: BookOpen, title: "Story Circle", desc: "Listen and share stories together", color: "bg-blue-100 text-blue-500" },
          { icon: HandHelping, title: "Helping Hands", desc: "Work together to solve problems", color: "bg-green-100 text-green-500" },
          { icon: Users, title: "Show & Tell", desc: "Share something special with the class", color: "bg-purple-100 text-purple-500" },
        ].map(item => (
          <div key={item.title} className="nursery-card flex items-center gap-4 rounded-2xl bg-white p-4">
            <div className={`flex size-12 items-center justify-center rounded-2xl ${item.color}`}>
              <item.icon className="size-6" />
            </div>
            <div>
              <p className="font-bold text-gray-800">{item.title}</p>
              <p className="text-xs text-gray-500">{item.desc}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
