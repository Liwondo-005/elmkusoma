"use client"

import { useTranslations } from "next-intl";

import { useEffect, useState } from "react"
import { useRequireAuth } from "@/lib/auth"
import { Building2, Users, GraduationCap, MapPin, AlertTriangle, TrendingUp } from "lucide-react"
import Link from "next/link"

interface Institution {
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

interface SchoolsResponse {
  institutions: Institution[]
}

export default function OversightSchoolsPage() {
  const t = useTranslations("oversight");
  const tc = useTranslations("common");
  const ts = useTranslations("status");
  const { user, loading: authLoading } = useRequireAuth()
  const [institutions, setInstitutions] = useState<Institution[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    if (!authLoading && user) {
      fetchSchools()
    }
  }, [user, authLoading])

  async function fetchSchools() {
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/v1/oversight/schools`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      if (res.ok) {
        const data = await res.json()
        setInstitutions(data.institutions || data)
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
        <div className="text-muted-foreground">{t("schools.loadingSchools")}</div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center gap-3">
          <div className="flex size-10 items-center justify-center rounded-xl bg-primary/10">
            <Building2 className="size-5 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">{t("schools.schoolsOversight")}</h1>
            <p className="text-sm text-muted-foreground">
              {t("schools.monitorSchoolsWithinYour")}</p>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-foreground">
            {t("schools.institutionsInYourJurisdiction", { p0: institutions.length })}</h2>
        </div>

        {institutions.length === 0 ? (
          <div className="text-center py-12">
            <Building2 className="size-12 mx-auto text-muted-foreground/50" />
            <p className="mt-4 text-muted-foreground">{t("schools.noInstitutionsFoundIn")}</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border">
                  <th className="text-left py-3 px-4 font-medium text-muted-foreground">{t("schools.name")}</th>
                  <th className="text-left py-3 px-4 font-medium text-muted-foreground">{t("schools.code")}</th>
                  <th className="text-left py-3 px-4 font-medium text-muted-foreground">{t("schools.type")}</th>
                  <th className="text-left py-3 px-4 font-medium text-muted-foreground">{t("schools.location")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("schools.teachers")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("schools.students")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("schools.classes")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("schools.attendance")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("schools.performance")}</th>
                  <th className="text-right py-3 px-4 font-medium text-muted-foreground">{t("schools.curriculum")}</th>
                  <th className="text-center py-3 px-4 font-medium text-muted-foreground">{t("schools.status")}</th>
                  <th className="text-center py-3 px-4 font-medium text-muted-foreground">{t("schools.actions")}</th>
                </tr>
              </thead>
              <tbody>
                {institutions.map((inst) => (
                  <tr key={inst.id} className="border-b border-border last:border-0 hover:bg-muted/50">
                    <td className="py-3 px-4 font-medium text-foreground">
                      <Link href={`/oversight/schools/${inst.id}`} className="hover:underline">
                        {inst.name}
                      </Link>
                    </td>
                    <td className="py-3 px-4 text-muted-foreground">{inst.code}</td>
                    <td className="py-3 px-4 text-muted-foreground">{inst.type}</td>
                    <td className="py-3 px-4 text-muted-foreground">
                      {inst.city}, {inst.districtName}, {inst.regionName}
                    </td>
                    <td className="py-3 px-4 text-right text-foreground">{inst.teacherCount}</td>
                    <td className="py-3 px-4 text-right text-foreground">{inst.studentCount}</td>
                    <td className="py-3 px-4 text-right text-foreground">{inst.classCount}</td>
                    <td className="py-3 px-4 text-right font-medium text-foreground">{inst.attendanceRate}%</td>
                    <td className="py-3 px-4 text-right font-medium text-foreground">{inst.averagePerformance}%</td>
                    <td className="py-3 px-4 text-right font-medium text-foreground">{inst.curriculumProgress}%</td>
                    <td className="py-3 px-4 text-center">
                      <span className={`inline-flex items-center rounded-full px-2 py-1 text-xs font-medium ${
                        inst.isActive ? "bg-green-100 text-green-700" : "bg-red-100 text-red-700"
                      }`}>
                        {inst.isActive ? ts("active") : ts("inactive")}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center">
                      <Link
                        href={`/oversight/schools/${inst.id}`}
                        className="text-primary hover:underline text-sm"
                      >
                        {tc("view")}</Link>
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