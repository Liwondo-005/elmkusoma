"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { Building2, Users, GraduationCap, MapPin } from "lucide-react"

interface Institution {
  id: string
  name: string
  code: string
  type: string
  teacherCount: number
  studentCount: number
  isActive: boolean
}

interface DistrictStats {
  totalInstitutions: number
  totalTeachers: number
  totalStudents: number
}

export default function DistrictDashboardPage() {
  const t = useTranslations("oversight");
  const ts = useTranslations("status");
  const { user, loading: authLoading } = useRequireAuth()
  const [institutions, setInstitutions] = useState<Institution[]>([])
  const [stats, setStats] = useState<DistrictStats | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!authLoading && user) {
      fetchDashboard()
    }
  }, [user, authLoading])

  async function fetchDashboard() {
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const districtId = user?.districtId || ""

      const [dashRes, instRes] = await Promise.all([
        fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/oversight/dashboard${districtId ? `?districtId=${districtId}` : ""}`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
        districtId
          ? fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/oversight/districts/${districtId}/institutions`, {
              headers: { Authorization: `Bearer ${token}` },
            })
          : null,
      ])

      if (dashRes.ok) {
        const data = await dashRes.json()
        setStats({
          totalInstitutions: data.totalInstitutions,
          totalTeachers: data.totalTeachers,
          totalStudents: data.totalStudents,
        })
      }

      if (instRes?.ok) {
        setInstitutions(await instRes.json())
      }
    } catch {
      // Dashboard unavailable
    } finally {
      setLoading(false)
    }
  }

  if (authLoading || loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="text-muted-foreground">{t("district.loadingDistrictDashboard")}</div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-orange-500/10">
            <MapPin className="size-5 text-orange-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">{t("district.districtEducationOversight")}</h1>
            <p className="text-sm text-muted-foreground">{t("district.welcome", { p0: user?.name ?? "" })}</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        <StatCard icon={<Building2 className="size-5" />} label="Institutions" value={stats?.totalInstitutions ?? 0} color="bg-blue-500/10 text-blue-600" />
        <StatCard icon={<Users className="size-5" />} label="Teachers" value={stats?.totalTeachers ?? 0} color="bg-green-500/10 text-green-600" />
        <StatCard icon={<GraduationCap className="size-5" />} label="Students" value={stats?.totalStudents ?? 0} color="bg-purple-500/10 text-purple-600" />
      </div>

      <div className="rounded-2xl border border-border bg-card p-6">
        <h2 className="text-lg font-semibold text-foreground mb-4">{t("district.institutionsInYourDistrict")}</h2>
        {institutions.length === 0 ? (
          <p className="text-muted-foreground">{t("district.noInstitutionsFound")}</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left py-3 px-4 font-medium text-muted-foreground">{t("district.name")}</th>
                  <th className="text-left py-3 px-4 font-medium text-muted-foreground">{t("district.code")}</th>
                  <th className="text-left py-3 px-4 font-medium text-muted-foreground">{t("district.type")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("district.teachers")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("district.students")}</th>
                  <th className="text-center py-3 px-4 font-medium text-muted-foreground">{t("district.status")}</th>
                </tr>
              </thead>
              <tbody>
                {institutions.map((inst) => (
                  <tr key={inst.id} className="border-b border-border last:border-0 hover:bg-muted/50">
                    <td className="py-3 px-4 font-medium text-foreground">{inst.name}</td>
                    <td className="py-3 px-4 text-muted-foreground">{inst.code}</td>
                    <td className="py-3 px-4 text-muted-foreground">{inst.type}</td>
                    <td className="py-3 px-4 text-right text-foreground">{inst.teacherCount}</td>
                    <td className="py-3 px-4 text-right text-foreground">{inst.studentCount}</td>
                    <td className="py-3 px-4 text-center">
                      <span className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${
                        inst.isActive ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"
                      }`}>
                        {inst.isActive ? ts("active") : ts("inactive")}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

function StatCard({ icon, label, value, color }: { icon: React.ReactNode; label: string; value: number; color: string }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
      <div className="flex items-center gap-3">
        <div className={`flex size-10 items-center justify-center rounded-xl ${color}`}>
          {icon}
        </div>
        <div>
          <p className="text-2xl font-bold text-foreground">{value.toLocaleString()}</p>
          <p className="text-sm text-muted-foreground">{label}</p>
        </div>
      </div>
    </div>
  )
}
