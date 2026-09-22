"use client"

import { useEffect, useState } from "react"
import { ArrowLeft, Users, BookOpen, HandHelping, User } from "lucide-react"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { teacherApi, type StudentInClass } from "@/lib/teacher-api"
import { LoadingState } from "@/components/learner/shared"
import { useTranslations } from "next-intl"

export default function LearnTogetherPage() {
  const t = useTranslations("nursery")
  const { user } = useRequireAuth()
  const [classmates, setClassmates] = useState<StudentInClass[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    teacherApi.getStudents()
      .then(students => setClassmates((students || []).slice(0, 8)))
      .catch(() => setError("Failed to load classmates"))
      .finally(() => setLoading(false))
  }, [])

  if (loading) return <LoadingState />

  if (error) {
    return (
      <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <p className="text-sm text-red-500">{error}</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24" role="main">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100" aria-label="Back to nursery dashboard">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">{t("learnTogether")}</h1>
          <p className="text-sm text-gray-500">{t("subtitle.together")}</p>
        </div>
      </div>

      <div className="nursery-card rounded-2xl bg-gradient-to-r from-blue-400 to-cyan-400 p-5 text-white">
        <div className="flex items-center gap-3">
          <Users className="size-8" />
          <div>
            <h2 className="text-lg font-bold">{t("groupActivities")}</h2>
            <p className="text-sm text-white/70">{t("doWithClassmates")}</p>
          </div>
        </div>
      </div>

      {classmates.length > 0 && (
        <div className="nursery-card rounded-2xl bg-white p-5">
          <h3 className="font-bold text-gray-800 mb-3">{t("myClassmates")}</h3>
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

      {classmates.length === 0 && (
        <div className="nursery-card rounded-2xl bg-white p-8 text-center">
          <Users className="mx-auto size-12 text-gray-300" />
          <h3 className="mt-3 text-lg font-bold text-gray-800">{t("empty.noClassmates")}</h3>
          <p className="mt-1 text-sm text-gray-500">{t("empty.noClassmates")}</p>
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
