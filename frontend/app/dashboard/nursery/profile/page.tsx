"use client"

import { useRequireAuth } from "@/lib/auth"
import { LoadingState } from "@/components/learner/shared"
import { ArrowLeft, User, Star, Mail, BookOpen, Trophy } from "lucide-react"
import Link from "next/link"
import { useEffect, useState } from "react"
import { nurseryApi, type NurseryMilestone } from "@/lib/nursery-api"

export default function NurseryProfilePage() {
  const { user } = useRequireAuth()
  const [milestones, setMilestones] = useState<NurseryMilestone[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!user?.id) return
    nurseryApi.getMilestones(user.id)
      .then(setMilestones)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [user])

  if (loading) return <LoadingState />

  const achieved = milestones.filter(m => m.status === "ACHIEVED").length
  const initials = user?.name?.split(" ").map(n => n[0]).join("").toUpperCase().slice(0, 2) || "LS"

  return (
    <div className="mx-auto max-w-4xl space-y-6 p-4 pb-24">
      <div className="flex items-center gap-3">
        <Link href="/dashboard/nursery" className="nursery-card flex size-10 items-center justify-center rounded-xl bg-gray-100">
          <ArrowLeft className="size-5 text-gray-600" />
        </Link>
        <div>
          <h1 className="text-xl font-bold text-gray-800">My Profile</h1>
          <p className="text-sm text-gray-500">All about me!</p>
        </div>
      </div>

      {/* Avatar Card */}
      <div className="nursery-card rounded-2xl bg-gradient-to-br from-purple-500 to-pink-500 p-6 text-center text-white">
        <div className="mx-auto flex size-20 items-center justify-center rounded-full bg-white/20 text-3xl font-bold">
          {initials}
        </div>
        <h2 className="mt-3 text-xl font-bold">{user?.name || "Little Star"}</h2>
        <p className="text-sm text-white/70">{user?.email}</p>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-3 gap-3">
        <div className="nursery-card rounded-2xl bg-yellow-50 p-4 text-center">
          <Trophy className="mx-auto size-6 text-yellow-500" />
          <div className="mt-1 text-xl font-bold text-yellow-600">{achieved}</div>
          <div className="text-[10px] font-bold text-yellow-700">Stars</div>
        </div>
        <div className="nursery-card rounded-2xl bg-blue-50 p-4 text-center">
          <BookOpen className="mx-auto size-6 text-blue-500" />
          <div className="mt-1 text-xl font-bold text-blue-600">—</div>
          <div className="text-[10px] font-bold text-blue-700">Books</div>
        </div>
        <div className="nursery-card rounded-2xl bg-green-50 p-4 text-center">
          <Star className="mx-auto size-6 text-green-500" />
          <div className="mt-1 text-xl font-bold text-green-600">—</div>
          <div className="text-[10px] font-bold text-green-700">Badges</div>
        </div>
      </div>

      {/* Info */}
      <div className="nursery-card space-y-3 rounded-2xl bg-white p-5">
        <h3 className="font-bold text-gray-800">About Me</h3>
        <div className="flex items-center gap-3 rounded-xl bg-gray-50 p-3">
          <User className="size-5 text-gray-400" />
          <div>
            <p className="text-xs text-gray-400">Name</p>
            <p className="text-sm font-bold text-gray-800">{user?.name}</p>
          </div>
        </div>
        <div className="flex items-center gap-3 rounded-xl bg-gray-50 p-3">
          <Mail className="size-5 text-gray-400" />
          <div>
            <p className="text-xs text-gray-400">Email</p>
            <p className="text-sm font-bold text-gray-800">{user?.email}</p>
          </div>
        </div>
        <div className="flex items-center gap-3 rounded-xl bg-gray-50 p-3">
          <BookOpen className="size-5 text-gray-400" />
          <div>
            <p className="text-xs text-gray-400">Level</p>
            <p className="text-sm font-bold text-gray-800">Nursery</p>
          </div>
        </div>
      </div>
    </div>
  )
}
