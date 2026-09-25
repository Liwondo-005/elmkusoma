"use client"

import Link from "next/link"
import { usePathname } from "next/navigation"
import {
  LayoutDashboard, Users, School, Video, CreditCard, Award,
  Shield, Activity, Settings, Search, Bell, ChevronDown, ChevronRight,
  AlertTriangle, BarChart3, Database, Globe, Zap, Package, Radio,
  MessageSquare, ShieldCheck, Key, BookOpen, Calendar, Image as ImageIcon,
  Library, SearchCode, LifeBuoy, Plug, HardDrive, FileSearch, Layers,
  Building2, GraduationCap, Mic2
} from "lucide-react"
import { Logo } from "@/components/logo"
import { cn } from "@/lib/utils"
import { useTranslations } from "next-intl"
import { useState, useEffect } from "react"
import { platformAdminApi, type PlatformHealth } from "@/lib/platform-admin-api"

interface NavItem { label: string; labelKey?: string; href: string; icon: typeof LayoutDashboard; badge?: string }
interface NavSection { title: string; titleKey?: string; items: NavItem[] }

const NAV_SECTIONS: NavSection[] = [
  { title: "COMMAND CENTER", titleKey: "groupCommandCenter", items: [
    { label: "Dashboard", labelKey: "dashboard", href: "/dashboard/platform-admin", icon: LayoutDashboard },
    { label: "Attention", labelKey: "attention", href: "/dashboard/platform-admin/attention", icon: AlertTriangle },
  ]},
  { title: "IDENTITY & ACCESS", titleKey: "groupIdentityAccess", items: [
    { label: "Users", labelKey: "users", href: "/dashboard/platform-admin/users", icon: Users },
    { label: "Admins", labelKey: "admins", href: "/dashboard/platform-admin/admins", icon: Shield },
    { label: "Delegations", labelKey: "delegations", href: "/dashboard/platform-admin/delegations", icon: Key },
  ]},
  { title: "ECOSYSTEM", titleKey: "groupEcosystem", items: [
    { label: "Institutions", labelKey: "institutions", href: "/dashboard/platform-admin/institutions", icon: School },
    { label: "Providers", labelKey: "providers", href: "/dashboard/platform-admin/providers", icon: Globe },
    { label: "Organizations", labelKey: "organizations", href: "/dashboard/platform-admin/organizations", icon: Building2 },
    { label: "Services", labelKey: "services", href: "/dashboard/platform-admin/services", icon: Package },
  ]},
  { title: "LEARNING", titleKey: "groupLearning", items: [
    { label: "Courses", labelKey: "courses", href: "/dashboard/platform-admin/courses", icon: BookOpen },
    { label: "Live Classes", labelKey: "liveClasses", href: "/dashboard/platform-admin/live-classes", icon: Video },
    { label: "Certificates", labelKey: "certificates", href: "/dashboard/platform-admin/certificates", icon: Award },
    { label: "Entitlements", labelKey: "entitlements", href: "/dashboard/platform-admin/entitlements", icon: Layers },
  ]},
  { title: "LIVE & EVENTS", titleKey: "groupLiveEvents", items: [
    { label: "Events", labelKey: "events", href: "/dashboard/platform-admin/events", icon: Calendar },
    { label: "Streaming", labelKey: "streaming", href: "/dashboard/platform-admin/streaming", icon: Radio },
  ]},
  { title: "MEDIA & RESOURCES", titleKey: "groupMediaResources", items: [
    { label: "Media", labelKey: "media", href: "/dashboard/platform-admin/media", icon: ImageIcon },
    { label: "Resources", labelKey: "resources", href: "/dashboard/platform-admin/resources", icon: Library },
  ]},
  { title: "COMMERCE", titleKey: "groupCommerce", items: [
    { label: "Payments", labelKey: "payments", href: "/dashboard/platform-admin/payments", icon: CreditCard },
    { label: "Packages", labelKey: "packages", href: "/dashboard/platform-admin/packages", icon: Package },
  ]},
  { title: "TRUST & SAFETY", titleKey: "groupTrustSafety", items: [
    { label: "Verifications", labelKey: "verifications", href: "/dashboard/platform-admin/verifications", icon: ShieldCheck },
    { label: "Moderation", labelKey: "moderation", href: "/dashboard/platform-admin/moderation", icon: FileSearch },
  ]},
  { title: "COMMUNICATION", titleKey: "groupCommunication", items: [
    { label: "Notifications", labelKey: "notifications", href: "/dashboard/platform-admin/communications", icon: MessageSquare },
  ]},
  { title: "INTELLIGENCE", titleKey: "groupIntelligence", items: [
    { label: "Analytics", labelKey: "analytics", href: "/dashboard/platform-admin/analytics", icon: BarChart3 },
  ]},
  { title: "SECURITY", titleKey: "groupSecurity", items: [
    { label: "Security", labelKey: "security", href: "/dashboard/platform-admin/security", icon: Shield },
    { label: "Audit Logs", labelKey: "auditLogs", href: "/dashboard/platform-admin/audit", icon: Activity },
  ]},
  { title: "OPERATIONS", titleKey: "groupOperations", items: [
    { label: "Incidents", labelKey: "incidents", href: "/dashboard/platform-admin/incidents", icon: AlertTriangle },
    { label: "Integrations", labelKey: "integrations", href: "/dashboard/platform-admin/integrations", icon: Plug },
    { label: "Health", labelKey: "health", href: "/dashboard/platform-admin/health", icon: Database },
  ]},
  { title: "DATA", titleKey: "groupData", items: [
    { label: "Governance", labelKey: "governance", href: "/dashboard/platform-admin/data", icon: HardDrive },
  ]},
  { title: "PLATFORM", titleKey: "groupPlatform", items: [
    { label: "Configuration", labelKey: "configuration", href: "/dashboard/platform-admin/config", icon: Settings },
    { label: "Lifecycle", labelKey: "lifecycle", href: "/dashboard/platform-admin/lifecycle", icon: Zap },
  ]},
  { title: "SUPPORT", titleKey: "groupSupport", items: [
    { label: "Cases", labelKey: "cases", href: "/dashboard/platform-admin/support", icon: LifeBuoy },
    { label: "Search", labelKey: "search", href: "/dashboard/platform-admin/search", icon: SearchCode },
  ]},
]

export function PlatformAdminSidebar({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname()
  const t = useTranslations("nav")
  const [expandedSections, setExpandedSections] = useState<Set<string>>(new Set(NAV_SECTIONS.map(s => s.title)))
  const [health, setHealth] = useState<PlatformHealth | null>(null)

  useEffect(() => { platformAdminApi.getHealth().then(setHealth).catch(() => {}) }, [])

  function toggleSection(title: string) {
    setExpandedSections(prev => { const n = new Set(prev); if (n.has(title)) n.delete(title); else n.add(title); return n })
  }

  return (
    <div className="flex h-full flex-col">
      <div className="flex h-16 items-center gap-2 border-b border-border px-4">
        <Logo />
        <div className="ml-1 min-w-0">
          <p className="text-xs font-bold tracking-tight text-foreground truncate">PLATFORM ADMIN</p>
          <p className="text-[10px] text-muted-foreground">Operations & Governance</p>
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-3 space-y-1">
        {NAV_SECTIONS.map(section => {
          const isExpanded = expandedSections.has(section.title)
          const isActive = section.items.some(item => pathname === item.href || pathname.startsWith(item.href + "/"))
          return (
            <div key={section.title}>
              <button onClick={() => toggleSection(section.title)} className={cn("flex w-full items-center gap-1.5 rounded-lg px-2 py-1.5 text-[10px] font-bold tracking-widest uppercase transition-colors", isActive ? "text-primary" : "text-muted-foreground hover:text-foreground")}>
                {isExpanded ? <ChevronDown className="size-3" /> : <ChevronRight className="size-3" />}{section.titleKey ? t(section.titleKey) : section.title}
              </button>
              {isExpanded && (
                <div className="mt-0.5 space-y-0.5">
                  {section.items.map(item => {
                    const isActiveItem = pathname === item.href || pathname.startsWith(item.href + "/")
                    return (
                      <Link key={item.href} href={item.href} onClick={onNavigate} className={cn("flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition-all", isActiveItem ? "bg-primary/10 text-primary" : "text-muted-foreground hover:bg-muted hover:text-foreground")}>
                        <item.icon className="size-4 shrink-0" />{item.labelKey ? t(item.labelKey) : item.label}
                      </Link>
                    )
                  })}
                </div>
              )}
            </div>
          )
        })}
      </nav>

      <div className="border-t border-border p-3">
        <div className="rounded-xl bg-muted/50 p-3">
          <div className="flex items-center gap-2">
            <div className="flex size-7 items-center justify-center rounded-lg bg-primary/10"><Zap className="size-3.5 text-primary" /></div>
            <div className="min-w-0">
              <p className="text-xs font-medium text-foreground">Platform Status</p>
              {health ? <p className={`text-[10px] font-medium ${health.databaseStatus === "Operational" ? "text-emerald-600" : "text-amber-600"}`}>DB: {health.databaseStatus} | API: {health.apiStatus}</p> : <p className="text-[10px] text-muted-foreground">Checking...</p>}
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
