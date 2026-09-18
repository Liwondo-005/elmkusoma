"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryActivity, type NurseryMilestone } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import {
  Sparkles, BookOpen, Palette, Puzzle, Music, Star,
  Trophy, Heart, ArrowRight, Sun, Moon, CloudSun
} from "lucide-react"

function getGreeting() {
  const h = new Date().getHours()
  if (h < 12) return { text: "Good Morning", icon: Sun, emoji: "sun" }
  if (h < 17) return { text: "Good Afternoon", icon: CloudSun, emoji: "cloud" }
  return { text: "Good Evening", icon: Moon, emoji: "moon" }
}

const WORLDS = [
  { label: "Learning Journey", href: "/dashboard/nursery/learning-journey", icon: Sparkles, color: "bg-purple-100 text-purple-600", desc: "Follow your path" },
  { label: "Play & Learn", href: "/dashboard/nursery/play", icon: Puzzle, color: "bg-pink-100 text-pink-600", desc: "Fun games await" },
  { label: "Stories", href: "/dashboard/nursery/stories", icon: BookOpen, color: "bg-blue-100 text-blue-600", desc: "Read magical tales" },
  { label: "Discovery", href: "/dashboard/nursery/discovery", icon: Star, color: "bg-amber-100 text-amber-600", desc: "Explore the world" },
  { label: "Create Studio", href: "/dashboard/nursery/create", icon: Palette, color: "bg-green-100 text-green-600", desc: "Draw & make music" },
  { label: "My Progress", href: "/dashboard/nursery/progress", icon: Trophy, color: "bg-orange-100 text-orange-600", desc: "See how you grow" },
]

const ACTIVITY_ICONS: Record<string, typeof BookOpen> = {
  GAME: Puzzle,
  SONG: Music,
  STORY: BookOpen,
  CRAFT: Palette,
  PHYSICAL: Heart,
  EDUCATIONAL: Sparkles,
}

const MILESTONE_EMOJI: Record<string, string> = {
  PHYSICAL: "🏃",
  COGNITIVE: "🧠",
  SOCIAL: "🤝",
  EMOTIONAL: "❤️",
  LANGUAGE: "💬",
  MOTOR: "✋",
}

export default function NurseryHomePage() {
  const { user, loading: authLoading } = useRequireAuth()
  const router = useRouter()
  const [activities, setActivities] = useState<NurseryActivity[]>([])
  const [milestones, setMilestones] = useState<NurseryMilestone[]>([])
  const [loading, setLoading] = useState(true)
  const firstName = user?.name?.split(" ")[0] || "Little Star"
  const greeting = getGreeting()
  const GreetingIcon = greeting.icon

  useEffect(() => {
    if (user?.role === "Parent") router.replace("/dashboard/parent")
    else if (user?.role === "Teacher" || user?.role === "Instructor") router.replace("/dashboard/teacher")
  }, [user, router])

  useEffect(() => {
    if (!user) return
    async function loadData() {
      try {
        const classGroupId = user?.classGroupId || ""
        if (classGroupId) {
          const acts = await nurseryApi.getActivities(classGroupId).catch(() => [])
          setActivities(acts)
        }
      } catch {}
      try {
        const studentId = user?.id || ""
        if (studentId) {
          const miles = await nurseryApi.getMilestones(studentId).catch(() => [])
          setMilestones(miles)
        }
      } catch {}
      setLoading(false)
    }
    loadData()
  }, [user])

  if (authLoading || loading) return <LoadingState />

  const achievedMilestones = milestones.filter(m => m.status === "ACHIEVED").length
  const totalMilestones = milestones.length
  const completedActivities = activities.filter(a => a.status === "COMPLETED").length
  const todayActivities = activities.filter(a => {
    const today = new Date().toISOString().split("T")[0]
    return a.activityDate === today
  })

  return (
    <div className="mx-auto max-w-5xl space-y-6 p-4 pb-24">
      {/* Welcome Banner */}
      <div className="nursery-card nursery-card-primary relative overflow-hidden rounded-3xl p-6 text-white">
        <div className="absolute -right-8 -top-8 size-32 rounded-full bg-white/10" />
        <div className="absolute -bottom-4 -left-4 size-24 rounded-full bg-white/10" />
        <div className="relative flex items-center gap-4">
          <div className="flex size-14 items-center justify-center rounded-2xl bg-white/20">
            <GreetingIcon className="size-7" />
          </div>
          <div>
            <h1 className="text-2xl font-bold">{greeting.text}, {firstName}!</h1>
            <p className="mt-1 text-sm text-white/80">Welcome to your fun learning world</p>
          </div>
        </div>
      </div>

      {/* Stats Row */}
      <div className="grid grid-cols-3 gap-3">
        <div className="nursery-card rounded-2xl bg-yellow-50 p-4 text-center">
          <div className="text-3xl font-bold text-yellow-600">{completedActivities}</div>
          <div className="mt-1 text-xs font-medium text-yellow-700">Fun Activities Done</div>
        </div>
        <div className="nursery-card rounded-2xl bg-green-50 p-4 text-center">
          <div className="text-3xl font-bold text-green-600">{achievedMilestones}</div>
          <div className="mt-1 text-xs font-medium text-green-700">Stars Earned</div>
        </div>
        <div className="nursery-card rounded-2xl bg-purple-50 p-4 text-center">
          <div className="text-3xl font-bold text-purple-600">{todayActivities.length}</div>
          <div className="mt-1 text-xs font-medium text-purple-700">Today&apos;s Fun</div>
        </div>
      </div>

      {/* Explore Worlds */}
      <section>
        <h2 className="mb-3 text-lg font-bold text-gray-800">Explore Your World</h2>
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
          {WORLDS.map((world) => (
            <Link
              key={world.href}
              href={world.href}
              className="nursery-card group flex flex-col items-center gap-2 rounded-2xl border-2 border-transparent bg-white p-4 text-center transition-all hover:border-primary hover:shadow-lg"
            >
              <div className={`flex size-12 items-center justify-center rounded-xl ${world.color} transition-transform group-hover:scale-110`}>
                <world.icon className="size-6" />
              </div>
              <span className="text-sm font-bold text-gray-800">{world.label}</span>
              <span className="text-[11px] text-gray-500">{world.desc}</span>
            </Link>
          ))}
        </div>
      </section>

      {/* Today's Activities */}
      {todayActivities.length > 0 && (
        <section>
          <h2 className="mb-3 text-lg font-bold text-gray-800">Today&apos;s Activities</h2>
          <div className="space-y-2">
            {todayActivities.map((activity) => {
              const Icon = ACTIVITY_ICONS[activity.activityType] || Sparkles
              return (
                <div key={activity.id} className="nursery-card flex items-center gap-3 rounded-2xl bg-white p-4">
                  <div className="flex size-10 items-center justify-center rounded-xl bg-blue-50">
                    <Icon className="size-5 text-blue-500" />
                  </div>
                  <div className="flex-1">
                    <p className="text-sm font-bold text-gray-800">{activity.activityName}</p>
                    <p className="text-xs text-gray-500">
                      {activity.activityType} • {activity.durationMinutes || 15} min
                    </p>
                  </div>
                  <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                    activity.status === "COMPLETED" ? "bg-green-100 text-green-700" :
                    activity.status === "IN_PROGRESS" ? "bg-yellow-100 text-yellow-700" :
                    "bg-blue-100 text-blue-700"
                  }`}>
                    {activity.status}
                  </span>
                </div>
              )
            })}
          </div>
        </section>
      )}

      {/* Recent Milestones */}
      {milestones.length > 0 && (
        <section>
          <h2 className="mb-3 text-lg font-bold text-gray-800">My Milestones</h2>
          <div className="space-y-2">
            {milestones.slice(0, 5).map((milestone) => (
              <div key={milestone.id} className="nursery-card flex items-center gap-3 rounded-2xl bg-white p-4">
                <span className="text-2xl">{MILESTONE_EMOJI[milestone.category] || "⭐"}</span>
                <div className="flex-1">
                  <p className="text-sm font-bold text-gray-800">{milestone.milestoneName}</p>
                  <p className="text-xs text-gray-500">{milestone.category}</p>
                </div>
                <span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${
                  milestone.status === "ACHIEVED" ? "bg-green-100 text-green-700" :
                  milestone.status === "IN_PROGRESS" ? "bg-yellow-100 text-yellow-700" :
                  "bg-gray-100 text-gray-500"
                }`}>
                  {milestone.status === "ACHIEVED" ? "Done!" : milestone.status}
                </span>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* Quick Actions */}
      <div className="grid grid-cols-2 gap-3">
        <Link
          href="/dashboard/nursery/play"
          className="nursery-card flex items-center justify-center gap-2 rounded-2xl bg-pink-500 p-4 text-center text-white transition-all hover:bg-pink-600 hover:shadow-lg"
        >
          <Puzzle className="size-5" />
          <span className="font-bold">Play Now!</span>
        </Link>
        <Link
          href="/dashboard/nursery/stories"
          className="nursery-card flex items-center justify-center gap-2 rounded-2xl bg-blue-500 p-4 text-center text-white transition-all hover:bg-blue-600 hover:shadow-lg"
        >
          <BookOpen className="size-5" />
          <span className="font-bold">Read a Story</span>
        </Link>
      </div>
    </div>
  )
}
