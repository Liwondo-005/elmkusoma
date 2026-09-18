"use client"

import { useEffect, useState } from "react"
import { useParams } from "next/navigation"
import { useRequireAuth } from "@/lib/auth"
import { Building2, Users, GraduationCap, MapPin, AlertTriangle, TrendingUp, Target, BookOpen, ClipboardList, Video, FileBarChart } from "lucide-react"
import Link from "next/link"

interface InstitutionDetail {
  id: string
  name: string
  code: string
  type: string
  teacherCount: number
  studentCount: number
  classCount: number
  lessonCount: number
  activeLiveClasses: number
  attendanceRate: number
  averagePerformance: number
  curriculumProgress: number
  isActive: boolean
  address: string
  city: string
  regionName: string
  districtName: string
}

export default function OversightSchoolDetailPage() {
  const params = useParams()
  const institutionId = params.id as string
  const { user, loading: authLoading } = useRequireAuth()
  const [school, setSchool] = useState<InstitutionDetail | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!authLoading && user && institutionId) {
      fetchSchool()
    }
  }, [user, authLoading, institutionId])

  async function fetchSchool() {
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/oversight/institutions/${institutionId}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.ok) {
        setSchool(await res.json())
      }
    } catch {
      // unavailable
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">Loading school details...</div>
      </div>
    )
  }

  if (!school) {
    return (
      <div className="mx-auto max-w-6xl space-y-6">
        <div className="rounded-2xl border border-border bg-card p-6">
          <h1 className="text-2xl font-bold text-foreground">School Not Found</h1>
          <p className="mt-2 text-muted-foreground">The requested school could not be found.</p>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <Link
            href="/oversight/schools"
            className="text-sm text-muted-foreground hover:underline mb-2 inline-block"
          >
            ← Back to Schools
          </Link>
          <h1 className="text-2xl font-bold text-foreground">{school.name}</h1>
          <p className="text-sm text-muted-foreground">{school.code} • {school.type}</p>
        </div>
        <div className="flex items-center gap-4">
          <span className={`inline-flex items-center rounded-full px-3 py-1 text-sm font-medium ${
            school.isActive ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"
          }`}>
            {school.isActive ? "Active" : "Inactive"}
          </span>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<Users className="size-5" />} label="Teachers" value={school.teacherCount} color="bg-green-500/10 text-green-600" />
        <StatCard icon={<GraduationCap className="size-5" />} label="Students" value={school.studentCount} color="bg-purple-500/10 text-purple-600" />
        <StatCard icon={<Users className="size-5" />} label="Classes" value={school.classCount} color="bg-blue-500/10 text-blue-600" />
        <StatCard icon={<BookOpen className="size-5" />} label="Lessons" value={school.lessonCount} color="bg-orange-500/10 text-orange-600" />
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={<Video className="size-5" />} label="Live Classes" value={school.activeLiveClasses} color="bg-red-500/10 text-red-600" />
        <StatCard icon={<Target className="size-5" />} label="Attendance Rate" value={`${school.attendanceRate}%`} color="bg-teal-500/10 text-teal-600" />
        <StatCard icon={<TrendingUp className="size-5" />} label="Avg Performance" value={`${school.averagePerformance}%`} color="bg-indigo-500/10 text-indigo-600" />
        <StatCard icon={<BookOpen className="size-5" />} label="Curriculum Progress" value={`${school.curriculumProgress}%`} color="bg-pink-500/10 text-pink-600" />
      </div>

      <div className="grid gap-6">
        <div className="rounded-2xl border border-border bg-card p-6">
          <h2 className="text-lg font-semibold text-foreground mb-4 flex items-center gap-2">
            <MapPin className="size-5 text-muted-foreground" />
            Location & Details
          </h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <DetailRow label="Address" value={school.address || "Not provided"} />
            <DetailRow label="City" value={school.city || "Not provided"} />
            <DetailRow label="District" value={school.districtName || "Not provided"} />
            <DetailRow label="Region" value={school.regionName || "Not provided"} />
          </div>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <QuickActionCard
            icon={<TrendingUp className="size-5" />}
            title="Performance"
            description="View academic performance details"
            href={`/oversight/performance?school=${school.id}`}
          />
          <QuickActionCard
            icon={<ClipboardList className="size-5" />}
            title="Attendance"
            description="View attendance analytics"
            href={`/oversight/attendance?school=${school.id}`}
          />
          <QuickActionCard
            icon={<BookOpen className="size-5" />}
            title="Curriculum"
            description="View curriculum progress"
            href={`/oversight/curriculum?school=${school.id}`}
          />
          <QuickActionCard
            icon={<Video className="size-5" />}
            title="Live Classes"
            description="View active live classes"
            href={`/oversight/live-classes?school=${school.id}`}
          />
        </div>
      </div>
    </div>
  )
}

function StatCard({ icon, label, value, color }: { icon: React.ReactNode; label: string; value: number | string; color: string }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-center gap-3">
        <div className={`flex size-10 items-center justify-center rounded-xl ${color}`}>
          {icon}
        </div>
        <div>
          <p className="text-2xl font-bold text-foreground">{value}</p>
          <p className="text-sm text-muted-foreground">{label}</p>
        </div>
      </div>
    </div>
  )
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="text-foreground font-medium">{value}</p>
    </div>
  )
}

function QuickActionCard({ icon, title, description, href }: { icon: React.ReactNode; title: string; description: string; href: string }) {
  return (
    <Link
      href={href}
      className="rounded-2xl border border-border bg-card p-5 hover:bg-muted/50 transition-colors"
    >
      <div className="flex items-center gap-3">
        <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
          {icon}
        </div>
        <div>
          <p className="font-medium text-foreground">{title}</p>
          <p className="text-sm text-muted-foreground">{description}</p>
        </div>
      </div>
    </Link>
  )
}