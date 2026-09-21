"use client"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { nurseryApi, type NurseryActivity, type NurseryMilestone, type NurseryStory } from "@/lib/nursery-api"
import { LoadingState } from "@/components/learner/shared"
import {
  Sparkles, BookOpen, Palette, Puzzle, Music,
  Trophy, Heart, ArrowRight, Sun, Moon, CloudSun,
  Play, Headphones, Eye, Compass
} from "lucide-react"

function getGreeting() {
  const h = new Date().getHours()
  if (h < 12) return { text: "Good morning", icon: Sun, emoji: "\u{1F31E}" }
  if (h < 17) return { text: "Good afternoon", icon: CloudSun, emoji: "\u{26C5}" }
  return { text: "Good evening", icon: Moon, emoji: "\u{1F319}" }
}

const DISCOVERY_AREAS = [
  { emoji: "\u{1F522}", label: "Numbers", desc: "Count & explore", href: "/dashboard/nursery/learning-journey", color: "bg-primary/10 border-primary/20 text-primary" },
  { emoji: "\u{1F418}", label: "Animals", desc: "Discover creatures", href: "/dashboard/nursery/discovery", color: "bg-orange/10 border-orange/20 text-orange" },
  { emoji: "\u{1F524}", label: "Letters", desc: "Listen & learn", href: "/dashboard/nursery/speak-listen", color: "bg-teal/10 border-teal/20 text-teal" },
]

const ACTIVITY_ICONS: Record<string, typeof BookOpen> = {
  GAME: Puzzle,
  SONG: Music,
  STORY: BookOpen,
  CRAFT: Palette,
  PHYSICAL: Heart,
  EDUCATIONAL: Sparkles,
}

const ACTIVITY_TYPE_LABELS: Record<string, string> = {
  GAME: "Fun Game",
  SONG: "Song Time",
  STORY: "Story",
  CRAFT: "Creative Craft",
  PHYSICAL: "Movement",
  EDUCATIONAL: "Learning",
}

const MILESTONE_CATEGORY_EMOJI: Record<string, string> = {
  PHYSICAL: "\u{1F3C3}",
  COGNITIVE: "\u{1F9E0}",
  SOCIAL: "\u{1F91D}",
  EMOTIONAL: "\u{2764}\u{FE0F}",
  LANGUAGE: "\u{1F4AC}",
  MOTOR: "\u{270B}",
}

export default function NurseryHomePage() {
  const { user, loading: authLoading } = useRequireAuth()
  const router = useRouter()
  const [activities, setActivities] = useState<NurseryActivity[]>([])
  const [milestones, setMilestones] = useState<NurseryMilestone[]>([])
  const [stories, setStories] = useState<NurseryStory[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const firstName = user?.name?.split(" ")[0] || "Little Star"
  const greeting = getGreeting()

  useEffect(() => {
    if (user?.role === "Parent") router.replace("/dashboard/parent")
    else if (user?.role === "Teacher" || user?.role === "Instructor") router.replace("/dashboard/teacher")
  }, [user, router])

  useEffect(() => {
    if (!user) return
    async function loadData() {
      try {
        const classGroupId = user?.classGroupId || ""
        const studentId = user?.id || ""

        const results = await Promise.allSettled([
          classGroupId ? nurseryApi.getActivities(classGroupId) : Promise.resolve([]),
          studentId ? nurseryApi.getMilestones(studentId) : Promise.resolve([]),
          classGroupId ? nurseryApi.getStories(classGroupId) : Promise.resolve([]),
        ])

        if (results[0].status === "fulfilled") setActivities(results[0].value)
        if (results[1].status === "fulfilled") setMilestones(results[1].value)
        if (results[2].status === "fulfilled") setStories(results[2].value)
      } catch {
        setError(true)
      }
      setLoading(false)
    }
    loadData()
  }, [user])

  if (authLoading || loading) return <LoadingState />

  if (error) {
    return (
      <div className="mx-auto flex min-h-[50vh] max-w-5xl flex-col items-center justify-center p-4 text-center">
        <div className="mb-4 flex size-16 items-center justify-center rounded-2xl bg-red-50">
          <Sparkles className="size-8 text-red-400" />
        </div>
        <h2 className="text-lg font-bold text-gray-800">Something went wrong</h2>
        <p className="mt-1 text-sm text-gray-500">We couldn&apos;t load your world right now.</p>
        <button
          type="button"
          onClick={() => window.location.reload()}
          className="mt-4 rounded-xl bg-primary px-5 py-2.5 text-sm font-bold text-white hover:bg-primary/90"
        >
          Try Again
        </button>
      </div>
    )
  }

  const today = new Date().toISOString().split("T")[0]
  const todayActivities = activities.filter(a => a.activityDate === today)
  const completedCount = activities.filter(a => a.status === "COMPLETED").length
  const achievedCount = milestones.filter(m => m.status === "ACHIEVED").length

  const todaysAdventure = todayActivities.find(a => a.status === "PLANNED" || a.status === "IN_PROGRESS")
    || todayActivities[0]
    || activities.find(a => a.status === "IN_PROGRESS")
    || activities.find(a => a.status === "PLANNED")

  const continueLearning = activities.find(a => a.status === "IN_PROGRESS")
    || activities.filter(a => a.status === "PLANNED" || a.status === "COMPLETED").sort((a, b) =>
      (a.activityDate > b.activityDate ? -1 : 1)
    )[0]

  const storyOfTheDay = stories.length > 0
    ? stories.find(s => s.isPublished !== false) || stories[0]
    : null

  const playActivity = todayActivities.find(a => a.activityType === "GAME")
    || activities.find(a => a.activityType === "GAME" && a.status !== "COMPLETED")
    || activities.find(a => a.activityType === "GAME")

  const uniqueCategories = [...new Set(milestones.filter(m => m.status === "ACHIEVED").map(m => m.category))].slice(0, 3)
  const dynamicDiscoveries = uniqueCategories.length >= 3
    ? uniqueCategories.map(cat => ({
        emoji: MILESTONE_CATEGORY_EMOJI[cat] || "\u{2B50}",
        label: cat.charAt(0) + cat.slice(1).toLowerCase(),
        desc: "Explore more",
        href: "/dashboard/nursery/learning-journey",
        color: "bg-teal/10 border-teal/20 text-teal",
      }))
    : null

  const discoveries = dynamicDiscoveries || DISCOVERY_AREAS

  return (
    <div className="mx-auto max-w-5xl space-y-5 p-4 pb-24 sm:space-y-6">

      {/* ─── Section 01: Welcome ─── */}
      <div className="nursery-card nursery-card-primary relative overflow-hidden rounded-3xl p-6 text-white sm:p-8">
        <div className="absolute -right-6 -top-6 size-28 rounded-full bg-white/10" />
        <div className="absolute -bottom-4 -left-4 size-20 rounded-full bg-white/10" />
        <div className="absolute right-12 bottom-2 size-10 rounded-full bg-white/5" />
        <div className="relative flex items-center gap-4">
          <div className="flex size-14 shrink-0 items-center justify-center rounded-2xl bg-white/20 text-2xl sm:size-16">
            {greeting.emoji}
          </div>
          <div>
            <h1 className="text-xl font-bold sm:text-2xl">
              {greeting.text}, {firstName}!
            </h1>
            <p className="mt-1 text-sm text-white/80">
              Ready for a new discovery today?
            </p>
          </div>
        </div>
      </div>

      {/* ─── Section 02: Today's Adventure ─── */}
      <section>
        <h2 className="mb-3 flex items-center gap-2 text-lg font-bold text-gray-800">
          <span className="text-xl">{"\u{1F31F}"}</span> Today&apos;s Adventure
        </h2>
        {todaysAdventure ? (
          <div className="nursery-card overflow-hidden rounded-2xl border-2 border-orange/20 bg-gradient-to-br from-orange/5 via-white to-amber-50">
            <div className="p-5 sm:p-6">
              <div className="flex items-start gap-4">
                <div className="flex size-14 shrink-0 items-center justify-center rounded-2xl bg-orange/10 text-2xl">
                  {todaysAdventure.activityType === "GAME" && "\u{1F3AE}"}
                  {todaysAdventure.activityType === "SONG" && "\u{1F3B5}"}
                  {todaysAdventure.activityType === "STORY" && "\u{1F4D6}"}
                  {todaysAdventure.activityType === "CRAFT" && "\u{1F3A8}"}
                  {todaysAdventure.activityType === "PHYSICAL" && "\u{1F3C3}"}
                  {todaysAdventure.activityType === "EDUCATIONAL" && "\u{1F4D1}"}
                  {!["GAME", "SONG", "STORY", "CRAFT", "PHYSICAL", "EDUCATIONAL"].includes(todaysAdventure.activityType) && "\u{2B50}"}
                </div>
                <div className="flex-1">
                  <p className="text-xs font-bold uppercase tracking-wide text-orange">
                    {ACTIVITY_TYPE_LABELS[todaysAdventure.activityType] || "Adventure"}
                  </p>
                  <h3 className="mt-1 text-lg font-bold text-gray-800">
                    {todaysAdventure.activityName}
                  </h3>
                  {todaysAdventure.description && (
                    <p className="mt-1 line-clamp-2 text-sm text-gray-600">
                      {todaysAdventure.description}
                    </p>
                  )}
                  <div className="mt-3 flex items-center gap-3">
                    {todaysAdventure.durationMinutes && (
                      <span className="text-xs text-gray-500">
                        {"\u{23F1}\u{FE0F}"} {todaysAdventure.durationMinutes} min
                      </span>
                    )}
                    <span className={`rounded-full px-2.5 py-0.5 text-[11px] font-bold ${
                      todaysAdventure.status === "IN_PROGRESS"
                        ? "bg-orange/10 text-orange"
                        : "bg-primary/10 text-primary"
                    }`}>
                      {todaysAdventure.status === "IN_PROGRESS" ? "In Progress" : "Ready!"}
                    </span>
                  </div>
                </div>
              </div>
              <Link
                href="/dashboard/nursery/play"
                className="mt-5 flex w-full items-center justify-center gap-2 rounded-xl bg-orange px-5 py-3 text-sm font-bold text-white transition-all hover:bg-orange/90 hover:shadow-md active:scale-[0.98]"
              >
                <Play className="size-4" />
                Start Adventure
                <ArrowRight className="size-4" />
              </Link>
            </div>
          </div>
        ) : (
          <div className="nursery-card rounded-2xl border-2 border-dashed border-primary/20 bg-primary/5 p-6 text-center">
            <span className="text-3xl">{"\u{1F30D}"}</span>
            <p className="mt-2 text-sm font-bold text-gray-700">No adventure scheduled today</p>
            <p className="mt-1 text-xs text-gray-500">Explore your world and find something fun!</p>
            <Link
              href="/dashboard/nursery/play"
              className="mt-3 inline-flex items-center gap-1.5 rounded-xl bg-primary/10 px-4 py-2 text-xs font-bold text-primary transition-colors hover:bg-primary/20"
            >
              <Compass className="size-3.5" /> Explore Activities
            </Link>
          </div>
        )}
      </section>

      {/* ─── Section 03: Continue Learning ─── */}
      <section>
        <h2 className="mb-3 flex items-center gap-2 text-lg font-bold text-gray-800">
          <span className="text-xl">{"\u{25B6}\u{FE0F}"}</span> Continue Learning
        </h2>
        {continueLearning ? (
          <div className="nursery-card rounded-2xl border border-gray-100 bg-white p-4 shadow-sm sm:p-5">
            <div className="flex items-center gap-3">
              <div className={`flex size-11 shrink-0 items-center justify-center rounded-xl ${
                continueLearning.activityType === "GAME" ? "bg-primary/10" :
                continueLearning.activityType === "STORY" ? "bg-teal/10" :
                continueLearning.activityType === "SONG" ? "bg-teal/10" :
                continueLearning.activityType === "CRAFT" ? "bg-teal/10" :
                continueLearning.activityType === "PHYSICAL" ? "bg-orange/10" :
                "bg-primary/10"
              }`}>
                {(() => {
                  const Icon = ACTIVITY_ICONS[continueLearning.activityType] || Sparkles
                  return <Icon className="size-5 text-gray-600" />
                })()}
              </div>
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm font-bold text-gray-800">{continueLearning.activityName}</p>
                <p className="text-xs text-gray-500">
                  {continueLearning.status === "IN_PROGRESS"
                    ? "You were working on this"
                    : `Scheduled for ${new Date(continueLearning.activityDate).toLocaleDateString("en-US", { month: "short", day: "numeric" })}`
                  }
                </p>
              </div>
              <Link
                href="/dashboard/nursery/play"
                className="flex shrink-0 items-center gap-1.5 rounded-xl bg-primary px-4 py-2.5 text-xs font-bold text-white transition-all hover:bg-primary/90 hover:shadow-md active:scale-[0.98]"
              >
                {continueLearning.status === "IN_PROGRESS" ? "Continue" : "Start"}
                <ArrowRight className="size-3.5" />
              </Link>
            </div>
          </div>
        ) : (
          <div className="nursery-card rounded-2xl border border-dashed border-gray-200 bg-gray-50/50 p-5 text-center">
            <span className="text-2xl">{"\u{1F331}"}</span>
            <p className="mt-2 text-sm font-bold text-gray-700">Nothing to continue yet</p>
            <p className="mt-1 text-xs text-gray-500">Start your first discovery above!</p>
          </div>
        )}
      </section>

      {/* ─── Section 04: Today's Discoveries ─── */}
      <section>
        <h2 className="mb-3 flex items-center gap-2 text-lg font-bold text-gray-800">
          <span className="text-xl">{"\u{2728}"}</span> Today&apos;s Discoveries
        </h2>
        <div className="grid grid-cols-3 gap-2 sm:gap-3">
          {discoveries.map((d, i) => (
            <Link
              key={`${d.label}-${i}`}
              href={d.href}
              className={`nursery-card group rounded-2xl border-2 p-3 text-center transition-all hover:shadow-md active:scale-[0.97] sm:p-4 ${d.color}`}
            >
              <span className="text-2xl">{d.emoji}</span>
              <p className="mt-2 text-xs font-bold sm:text-sm">{d.label}</p>
              <p className="mt-0.5 text-[10px] opacity-70 sm:text-xs">{d.desc}</p>
            </Link>
          ))}
        </div>
      </section>

      {/* ─── Section 05: Play & Learn + Story of the Day ─── */}
      <section className="grid gap-3 sm:grid-cols-2 sm:gap-4">

        {/* Play & Learn Card */}
        <div className="nursery-card overflow-hidden rounded-2xl border border-primary/20 bg-gradient-to-br from-primary/5 to-teal/5">
          <div className="p-4 sm:p-5">
            <div className="flex items-center gap-2">
              <span className="text-lg">{"\u{1F3AE}"}</span>
              <h3 className="text-sm font-bold text-gray-800">Play &amp; Learn</h3>
            </div>
            {playActivity ? (
              <>
                <p className="mt-2 text-sm font-bold text-gray-700">{playActivity.activityName}</p>
                {playActivity.description && (
                  <p className="mt-1 line-clamp-2 text-xs text-gray-500">{playActivity.description}</p>
                )}
              </>
            ) : (
              <p className="mt-2 text-xs text-gray-500">Fun games are waiting for you!</p>
            )}
            <Link
              href="/dashboard/nursery/play"
              className="mt-4 flex w-full items-center justify-center gap-1.5 rounded-xl bg-primary px-4 py-2.5 text-xs font-bold text-white transition-all hover:bg-primary/90 active:scale-[0.98]"
            >
              <Play className="size-3.5" /> Play
            </Link>
          </div>
        </div>

        {/* Story of the Day Card */}
        <div className="nursery-card overflow-hidden rounded-2xl border border-teal/20 bg-gradient-to-br from-teal/5 to-primary/5">
          <div className="p-4 sm:p-5">
            <div className="flex items-center gap-2">
              <span className="text-lg">{"\u{1F4D6}"}</span>
              <h3 className="text-sm font-bold text-gray-800">Story of the Day</h3>
            </div>
            {storyOfTheDay ? (
              <>
                <p className="mt-2 text-sm font-bold text-gray-700">{storyOfTheDay.title}</p>
                {storyOfTheDay.storyType && (
                  <p className="mt-1 text-xs text-gray-500">{storyOfTheDay.storyType.replace("_", " ")}</p>
                )}
                <div className="mt-3 flex items-center gap-2">
                  <Link
                    href="/dashboard/nursery/stories"
                    className="flex flex-1 items-center justify-center gap-1 rounded-xl bg-primary px-3 py-2 text-[11px] font-bold text-white transition-all hover:bg-primary/90 active:scale-[0.98]"
                  >
                    <Eye className="size-3" /> Read
                  </Link>
                  {storyOfTheDay.audioUrl && (
                    <Link
                      href="/dashboard/nursery/stories"
                      className="flex flex-1 items-center justify-center gap-1 rounded-xl bg-teal px-3 py-2 text-[11px] font-bold text-white transition-all hover:bg-teal/90 active:scale-[0.98]"
                    >
                      <Headphones className="size-3" /> Listen
                    </Link>
                  )}
                </div>
              </>
            ) : (
              <>
                <p className="mt-2 text-xs text-gray-500">No story today yet.</p>
                <Link
                  href="/dashboard/nursery/stories"
                  className="mt-3 flex w-full items-center justify-center gap-1.5 rounded-xl bg-primary px-4 py-2.5 text-xs font-bold text-white transition-all hover:bg-primary/90 active:scale-[0.98]"
                >
                  <BookOpen className="size-3.5" /> Browse Stories
                </Link>
              </>
            )}
          </div>
        </div>
      </section>

      {/* ─── Quick Stats Footer ─── */}
      {(completedCount > 0 || achievedCount > 0) && (
        <div className="grid grid-cols-2 gap-3">
          <div className="nursery-card flex items-center gap-3 rounded-2xl bg-teal/10 p-4">
            <div className="flex size-10 items-center justify-center rounded-xl bg-teal/20">
              <Trophy className="size-5 text-teal" />
            </div>
            <div>
              <p className="text-lg font-bold text-teal">{achievedCount}</p>
              <p className="text-[11px] font-medium text-teal">Stars Earned</p>
            </div>
          </div>
          <div className="nursery-card flex items-center gap-3 rounded-2xl bg-orange/10 p-4">
            <div className="flex size-10 items-center justify-center rounded-xl bg-orange/20">
              <Sparkles className="size-5 text-orange" />
            </div>
            <div>
              <p className="text-lg font-bold text-orange">{completedCount}</p>
              <p className="text-[11px] font-medium text-orange">Activities Done</p>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
