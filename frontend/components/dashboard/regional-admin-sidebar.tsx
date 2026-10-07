"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import {
  LayoutDashboard, Activity, MapPin, Building2, Users, GraduationCap,
  Briefcase, BookOpen, Library, Film, Video, BarChart3, ClipboardList,
  FileText, BookOpenCheck, ShieldCheck, Database, Scale, ClipboardCheck,
  FileBarChart, Megaphone, Bell, Zap, LifeBuoy, User, Lock, Search,
  ChevronDown, ChevronRight,
} from "lucide-react"
import { Logo } from "@/components/logo"
import { cn } from "@/lib/utils"
import { useTranslations } from "next-intl"
import { useState, useEffect } from "react"
import { regionalAdminApi, type RegionalDashboard } from "@/lib/regional-admin-api"
import { useCommandPalette } from "@/hooks/use-command-palette"

interface NavItem { label: string; labelKey?: string; href: string; icon: typeof LayoutDashboard; badgeKey?: "verifications" | "notifications"; external?: boolean }
interface NavSection { title: string; titleKey?: string; items: NavItem[] }

// Target information architecture per Regional Admin prompt §15.
// Wards are intentionally absent: the platform has no Ward domain model (§23 gap).
// Subjects/Lessons are surfaced through the existing Curriculum analytics instead of
// new duplicate list pages; Live Learning and the PERFORMANCE group reuse the existing
// jurisdiction-aware /oversight analytics (prompt: "If equivalent navigation already
// exists: extend/reuse it. Do not create duplicate navigation infrastructure.").
// Exported for the generic /dashboard shell (DashboardSidebar): a Regional
// Admin opening Profile/Settings gets these same workspace links instead of
// the student fall-through nav.
export const NAV_SECTIONS: NavSection[] = [
  {
    title: "COMMAND CENTER", titleKey: "groupCommandCenter", items: [
      { label: "Overview", labelKey: "overview", href: "/dashboard/regional-admin", icon: LayoutDashboard },
      { label: "Regional Pulse", labelKey: "regionalPulse", href: "/dashboard/regional-admin/pulse", icon: Activity },
    ],
  },
  {
    title: "GEOGRAPHY", titleKey: "groupGeography", items: [
      { label: "Districts", labelKey: "districts", href: "/dashboard/regional-admin/districts", icon: MapPin },
      { label: "Wards", labelKey: "wards", href: "/dashboard/regional-admin/wards", icon: MapPin },
      { label: "Schools & Institutions", labelKey: "schoolsAndInstitutions", href: "/dashboard/regional-admin/institutions", icon: Building2 },
    ],
  },
  {
    title: "PEOPLE", titleKey: "groupPeople", items: [
      { label: "Learners", labelKey: "learners", href: "/dashboard/regional-admin/learners", icon: GraduationCap },
      { label: "Teachers", labelKey: "teachers", href: "/dashboard/regional-admin/teachers", icon: Users },
      { label: "Education Staff", labelKey: "educationStaff", href: "/dashboard/regional-admin/education-staff", icon: Briefcase },
    ],
  },
  {
    title: "LEARNING", titleKey: "groupLearning", items: [
      { label: "Courses", labelKey: "courses", href: "/dashboard/regional-admin/learning/courses", icon: BookOpen },
      { label: "Resources", labelKey: "resources", href: "/dashboard/regional-admin/learning/resources", icon: Library },
      { label: "Video Tutorials", labelKey: "videoTutorials", href: "/dashboard/regional-admin/learning/videos", icon: Film },
      { label: "Live Learning", labelKey: "liveLearning", href: "/oversight/live-classes", icon: Video, external: true },
    ],
  },
  {
    title: "PERFORMANCE", titleKey: "groupPerformance", items: [
      { label: "Learner Progress", labelKey: "learnerProgress", href: "/oversight/performance", icon: BarChart3, external: true },
      { label: "Attendance", labelKey: "attendance", href: "/oversight/attendance", icon: ClipboardList, external: true },
      { label: "Assessments", labelKey: "assessments", href: "/oversight/assessments", icon: FileText, external: true },
      { label: "Curriculum", labelKey: "curriculum", href: "/oversight/curriculum", icon: BookOpenCheck, external: true },
    ],
  },
  {
    title: "GOVERNANCE", titleKey: "groupGovernance", items: [
      { label: "Verification", labelKey: "verification", href: "/dashboard/regional-admin/governance/verification", icon: ShieldCheck, badgeKey: "verifications" },
      { label: "Data Quality", labelKey: "dataQuality", href: "/dashboard/regional-admin/governance/data-quality", icon: Database },
      { label: "Compliance", labelKey: "compliance", href: "/dashboard/regional-admin/governance/compliance", icon: Scale },
      { label: "Audit", labelKey: "audit", href: "/dashboard/regional-admin/governance/audit", icon: ClipboardCheck },
    ],
  },
  {
    title: "REPORTS & ANALYTICS", titleKey: "groupReportsAnalytics", items: [
      { label: "Regional Reports", labelKey: "regionalReports", href: "/oversight/reports", icon: FileBarChart, external: true },
      { label: "Scheduled Reports", labelKey: "scheduledReports", href: "/dashboard/regional-admin/scheduled-reports", icon: FileBarChart },
      { label: "Analytics", labelKey: "analytics", href: "/oversight", icon: Activity, external: true },
    ],
  },
  {
    title: "COMMUNICATION", titleKey: "groupCommunication", items: [
      { label: "Regional Announcements", labelKey: "regionalAnnouncements", href: "/dashboard/regional-admin/communication", icon: Megaphone },
      { label: "Notifications", labelKey: "notifications", href: "/dashboard/regional-admin/notifications", icon: Bell, badgeKey: "notifications" },
    ],
  },
  {
    title: "SUPPORT", titleKey: "groupSupport", items: [
      { label: "Search", labelKey: "search", href: "/dashboard/regional-admin/search", icon: Search },
    ],
  },
]

export function RegionalAdminSidebar({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname()
  const t = useTranslations("nav")
  const tc = useTranslations("common")
  const tr = useTranslations("roles")
  const palette = useCommandPalette()
  const [expandedSections, setExpandedSections] = useState<Set<string>>(
    () => new Set(NAV_SECTIONS.map((s) => s.title)),
  )
  const [dashboard, setDashboard] = useState<RegionalDashboard | null>(null)
  const [unread, setUnread] = useState<number | null>(null)

  useEffect(() => {
    regionalAdminApi.getDashboard().then(setDashboard).catch(() => {})
    regionalAdminApi.getUnreadNotificationCount().then((r) => setUnread(r.count)).catch(() => {})
  }, [pathname])

  function toggleSection(title: string) {
    setExpandedSections((prev) => {
      const n = new Set(prev)
      if (n.has(title)) n.delete(title)
      else n.add(title)
      return n
    })
  }

  const jurisdiction = dashboard?.jurisdictionSummary
  const pendingVerifications = dashboard?.pendingVerifications ?? 0

  return (
    <div className="flex h-full flex-col bg-card">
      <div className="flex h-16 items-center gap-2 border-b border-border px-4">
        <Logo />
        <div className="ml-1 min-w-0">
          <p className="truncate text-xs font-bold tracking-tight text-foreground uppercase">{tr("regionalAdmin")}</p>
          <p className="truncate text-[10px] text-muted-foreground">
            {jurisdiction ? `${jurisdiction.name} · ${jurisdiction.code}` : t("educationGovernance")}
          </p>
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-3 space-y-1">
        {NAV_SECTIONS.map((section) => {
          const isExpanded = expandedSections.has(section.title)
          const sectionActive = section.items.some(
            (item) => pathname === item.href || (item.href !== "/dashboard/regional-admin" && pathname.startsWith(item.href)),
          )
          return (
            <div key={section.title}>
              <button
                type="button"
                onClick={() => toggleSection(section.title)}
                aria-expanded={isExpanded}
                className={cn(
                  "flex w-full items-center gap-1.5 rounded-lg px-2 py-1.5 text-[10px] font-bold uppercase tracking-widest transition-colors",
                  sectionActive ? "text-primary" : "text-muted-foreground hover:text-foreground",
                )}
              >
                {isExpanded ? <ChevronDown className="size-3" /> : <ChevronRight className="size-3" />}
                {section.titleKey ? t(section.titleKey) : section.title}
              </button>
              {isExpanded && (
                <div className="mt-0.5 space-y-0.5">
                  {section.items.map((item) => {
                    const isActiveItem =
                      pathname === item.href ||
                      (item.href !== "/dashboard/regional-admin" && pathname.startsWith(item.href))
                    const badge =
                      item.badgeKey === "verifications" && pendingVerifications > 0
                        ? pendingVerifications
                        : item.badgeKey === "notifications" && unread
                          ? unread
                          : null
                    return (
                      <Link
                        key={item.href}
                        href={item.href}
                        onClick={onNavigate}
                        title={item.labelKey ? t(item.labelKey) : item.label}
                        className={cn(
                          "flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition-all",
                          isActiveItem ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-muted hover:text-foreground",
                        )}
                      >
                        <item.icon className="size-4 shrink-0" aria-hidden="true" />
                        <span className="flex-1 truncate">{item.labelKey ? t(item.labelKey) : item.label}</span>
                        {badge !== null && (
                          <span className="rounded-full bg-primary px-1.5 py-0.5 text-[10px] font-bold text-primary-foreground tabular-nums">
                            {badge}
                          </span>
                        )}
                      </Link>
                    )
                  })}
                </div>
              )}
            </div>
          )
        })}
      </nav>

      <div className="border-t border-border p-3 space-y-1">
        <button
          type="button"
          onClick={() => palette.open()}
          className="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
          <Zap className="size-4 shrink-0" />
          {t("quickActions")}
          <kbd className="ml-auto rounded border border-border px-1.5 py-0.5 text-[10px] font-mono text-muted-foreground">⌘K</kbd>
        </button>
        <Link
          href="/support"
          onClick={onNavigate}
          className="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
          <LifeBuoy className="size-4 shrink-0" />
          {t("support")}
        </Link>
        <Link
          href="/dashboard/profile"
          onClick={onNavigate}
          className="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
          <User className="size-4 shrink-0" />
          {t("profile")}
        </Link>
        <Link
          href="/dashboard/settings"
          onClick={onNavigate}
          className="flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium text-muted-foreground transition-colors hover:bg-muted hover:text-foreground"
        >
          <Lock className="size-4 shrink-0" />
          {t("security")}
        </Link>
        <div className="rounded-xl bg-muted/50 p-3">
          <div className="flex items-center gap-2">
            <div className="flex size-7 items-center justify-center rounded-lg bg-primary/10">
              <MapPin className="size-3.5 text-primary" />
            </div>
            <div className="min-w-0">
              <p className="truncate text-xs font-medium text-foreground">
                {jurisdiction ? jurisdiction.name : tc("loading")}
              </p>
              <p className="truncate text-[10px] text-muted-foreground">
                {jurisdiction ? t("jurisdictionType", { type: jurisdiction.type }) : t("resolvingJurisdiction")}
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
