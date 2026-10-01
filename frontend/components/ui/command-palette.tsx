"use client"

import { useRef, useEffect, useMemo } from "react"
import { X, Search, Zap, HelpCircle, Users, Shield, BookOpen, Plus, Upload, Calendar, Settings, LayoutDashboard, GraduationCap, Film, FileText, Clock, LifeBuoy, BookMarked, ExternalLink, ClipboardList, Award, ClipboardCheck } from "lucide-react"
import { useTranslations } from "next-intl"
import { useRouter, usePathname } from "next/navigation"
import { useAuth } from "@/lib/auth"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"
import {
  CommandPaletteItem,
  CommandPaletteCategory,
  CommandPaletteEmpty,
  CommandPaletteLoading,
} from "@/components/ui/command-palette-item"
import { useCommandPalette, type CommandPaletteCategory as CategoryType, type CommandPaletteItem as ItemType } from "@/hooks/use-command-palette"

export function CommandPalette() {
  const t = useTranslations("commandPalette")
  const tc = useTranslations("common")
  const tn = useTranslations("nav")
  const router = useRouter()
  const pathname = usePathname()
  const { user } = useAuth()
  const palette = useCommandPalette()

  const isAdmin = user?.role === "Admin"
  const isOrgAdmin = user?.role === "Institution Admin"
  const isTeacher = user?.role === "Teacher" || user?.role === "Instructor"
  const isLearner = user?.role === "Other Learner" || user?.role === "Student"
  const isRegional = user?.role === "Regional Admin" || user?.role === "District Admin"

  const navigationItems = useMemo((): ItemType[] => {
    const items: ItemType[] = [
      {
        id: "nav-dashboard",
        title: tn("dashboard"),
        description: t("nav.dashboardDesc"),
        category: t("categories.navigation"),
        shortcut: "⌘1",
        icon: <LayoutDashboard className="size-4" />,
        action: () => router.push("/dashboard"),
        keywords: ["home", "overview", "main"],
      },
      {
        id: "nav-courses",
        title: tn("courses"),
        description: t("nav.coursesDesc"),
        category: t("categories.navigation"),
        shortcut: "⌘2",
        icon: <GraduationCap className="size-4" />,
        action: () => router.push(isLearner ? "/dashboard/learner/courses" : isTeacher ? "/dashboard/teacher/courses" : "/dashboard/admin/courses"),
        keywords: ["course", "class", "subject"],
      },
      {
        id: "nav-resources",
        title: tn("resources"),
        description: t("nav.resourcesDesc"),
        category: t("categories.navigation"),
        shortcut: "⌘3",
        icon: <BookOpen className="size-4" />,
        action: () => router.push(isLearner ? "/dashboard/learner/resources" : isTeacher ? "/dashboard/teacher/resources" : "/dashboard/admin/resources"),
        keywords: ["resource", "material", "document"],
      },
      {
        id: "nav-videos",
        title: tn("videoLibrary"),
        description: t("nav.videosDesc"),
        category: t("categories.navigation"),
        shortcut: "⌘4",
        icon: <Film className="size-4" />,
        action: () => router.push(isLearner ? "/dashboard/learner/video-library" : isTeacher ? "/dashboard/teacher/video-library" : "/dashboard/admin/videos"),
        keywords: ["video", "media", "recording"],
      },
      {
        id: "nav-lessons",
        title: tn("lessons"),
        description: t("nav.lessonsDesc"),
        category: t("categories.navigation"),
        shortcut: "⌘5",
        icon: <FileText className="size-4" />,
        action: () => router.push(isLearner ? "/dashboard/learner/my-learning" : isTeacher ? "/dashboard/teacher/lessons" : "/dashboard/admin/lessons"),
        keywords: ["lesson", "session", "lecture"],
      },
      {
        id: "nav-calendar",
        title: tn("calendar"),
        description: t("nav.calendarDesc"),
        category: t("categories.navigation"),
        shortcut: "⌘6",
        icon: <Calendar className="size-4" />,
        action: () => router.push("/dashboard/learner/calendar"),
        keywords: ["calendar", "schedule", "event", "date"],
      },
      {
        id: "nav-settings",
        title: tn("settings"),
        description: t("nav.settingsDesc"),
        category: t("categories.navigation"),
        shortcut: "⌘,",
        icon: <Settings className="size-4" />,
        action: () => router.push("/dashboard/settings"),
        keywords: ["settings", "preferences", "config"],
      },
    ]

    if (isLearner) {
      items.push(
        {
          id: "nav-live-classes",
          title: tn("liveClasses"),
          description: t("nav.liveClassesDesc"),
          category: t("categories.navigation"),
          shortcut: "⌘L",
          icon: <Zap className="size-4" />,
          action: () => router.push("/dashboard/learner/live-classes"),
          keywords: ["live", "class", "session", "now"],
        },
        {
          id: "nav-progress",
          title: tn("progress"),
          description: t("nav.progressDesc"),
          category: t("categories.navigation"),
          icon: <BookMarked className="size-4" />,
          action: () => router.push("/dashboard/learner/progress"),
          keywords: ["progress", "tracking", "completion"],
        },
        {
          id: "nav-certificates",
          title: tn("certificates"),
          description: t("nav.certificatesDesc"),
          category: t("categories.navigation"),
          icon: <Award className="size-4" />,
          action: () => router.push("/dashboard/learner/certificates"),
          keywords: ["certificate", "achievement", "credential"],
        }
      )
    }

    if (isTeacher) {
      items.push(
        {
          id: "nav-assignments",
          title: tn("assignments"),
          description: t("nav.assignmentsDesc"),
          category: t("categories.navigation"),
          icon: <ClipboardList className="size-4" />,
          action: () => router.push("/dashboard/teacher/assignments"),
          keywords: ["assignment", "homework", "task"],
        },
        {
          id: "nav-grading",
          title: tn("grading"),
          description: t("nav.gradingDesc"),
          category: t("categories.navigation"),
          icon: <Award className="size-4" />,
          action: () => router.push("/dashboard/teacher/grading"),
          keywords: ["grade", "mark", "score"],
        },
        {
          id: "nav-attendance",
          title: tn("attendance"),
          description: t("nav.attendanceDesc"),
          category: t("categories.navigation"),
          icon: <ClipboardCheck className="size-4" />,
          action: () => router.push("/dashboard/teacher/attendance"),
          keywords: ["attendance", "roll", "present"],
        }
      )
    }

    if (isAdmin || isOrgAdmin) {
      items.push(
        {
          id: "nav-admin-dashboard",
          title: tn("dashboard"),
          description: t("nav.adminDashboardDesc"),
          category: t("categories.navigation"),
          icon: <Shield className="size-4" />,
          action: () => router.push(isAdmin ? "/dashboard/platform-admin" : "/dashboard/admin"),
          keywords: ["admin", "administration", "manage"],
        },
        {
          id: "nav-institutions",
          title: tn("institutions"),
          description: t("nav.institutionsDesc"),
          category: t("categories.navigation"),
          icon: <Users className="size-4" />,
          action: () => router.push(isAdmin ? "/dashboard/platform-admin/institutions" : "/dashboard/admin/institutions"),
          keywords: ["institution", "school", "organization"],
        }
      )
    }

    if (isRegional) {
      items.push(
        {
          id: "nav-regional-overview",
          title: tn("overview"),
          description: t("nav.governanceDesc"),
          category: t("categories.navigation"),
          icon: <Shield className="size-4" />,
          action: () => router.push("/dashboard/regional-admin"),
          keywords: ["regional", "district", "overview", "command center", "governance"],
        },
        {
          id: "nav-regional-districts",
          title: tn("districts"),
          description: t("nav.districtsDesc"),
          category: t("categories.navigation"),
          icon: <Shield className="size-4" />,
          action: () => router.push("/dashboard/regional-admin/districts"),
          keywords: ["district", "geography", "region"],
        },
        {
          id: "nav-regional-institutions",
          title: tn("institutions"),
          description: t("nav.institutionsDesc"),
          category: t("categories.navigation"),
          icon: <Users className="size-4" />,
          action: () => router.push("/dashboard/regional-admin/institutions"),
          keywords: ["institution", "school", "find"],
        },
        {
          id: "nav-regional-learners",
          title: tn("learners"),
          description: t("nav.learnersDesc"),
          category: t("categories.navigation"),
          icon: <GraduationCap className="size-4" />,
          action: () => router.push("/dashboard/regional-admin/learners"),
          keywords: ["learner", "student", "find"],
        },
        {
          id: "nav-regional-teachers",
          title: tn("teachers"),
          description: t("nav.teachersDesc"),
          category: t("categories.navigation"),
          icon: <Users className="size-4" />,
          action: () => router.push("/dashboard/regional-admin/teachers"),
          keywords: ["teacher", "instructor", "find"],
        },
        {
          id: "nav-regional-reports",
          title: tn("reports"),
          description: t("nav.reportsDesc"),
          category: t("categories.navigation"),
          icon: <ClipboardList className="size-4" />,
          action: () => router.push("/oversight/reports"),
          keywords: ["report", "analytics", "open"],
        },
        {
          id: "nav-regional-verification",
          title: tn("verification"),
          description: t("nav.verificationDesc"),
          category: t("categories.navigation"),
          icon: <ClipboardCheck className="size-4" />,
          action: () => router.push("/dashboard/regional-admin/governance/verification"),
          keywords: ["verification", "review", "governance"],
        },
        {
          id: "nav-regional-data-quality",
          title: tn("dataQuality"),
          description: t("nav.dataQualityDesc"),
          category: t("categories.navigation"),
          icon: <Shield className="size-4" />,
          action: () => router.push("/dashboard/regional-admin/governance/data-quality"),
          keywords: ["data quality", "governance", "open"],
        },
        {
          id: "nav-regional-search",
          title: tn("search"),
          description: t("placeholder"),
          category: t("categories.navigation"),
          icon: <Search className="size-4" />,
          action: () => router.push("/dashboard/regional-admin/search"),
          keywords: ["search", "find", "jurisdiction"],
        },
      )
    }

    return items
  }, [router, isAdmin, isOrgAdmin, isTeacher, isLearner, isRegional, t, tn])

  const actionItems = useMemo((): ItemType[] => {
    const items: ItemType[] = []

    if (isTeacher) {
      items.push(
        {
          id: "action-create-lesson",
          title: t("actions.createLesson"),
          description: t("actions.createLessonDesc"),
          category: t("categories.actions"),
          shortcut: "⌘N",
          icon: <Plus className="size-4" />,
          action: () => router.push("/dashboard/teacher/lessons/new"),
          keywords: ["create", "new", "lesson", "add"],
        },
        {
          id: "action-create-resource",
          title: t("actions.createResource"),
          description: t("actions.createResourceDesc"),
          category: t("categories.actions"),
          icon: <Upload className="size-4" />,
          action: () => router.push("/dashboard/teacher/resources/new"),
          keywords: ["create", "new", "resource", "upload"],
        },
        {
          id: "action-upload-video",
          title: t("actions.uploadVideo"),
          description: t("actions.uploadVideoDesc"),
          category: t("categories.actions"),
          icon: <Upload className="size-4" />,
          action: () => router.push("/dashboard/teacher/video-library/upload"),
          keywords: ["upload", "video", "media"],
        },
        {
          id: "action-create-assignment",
          title: t("actions.createAssignment"),
          description: t("actions.createAssignmentDesc"),
          category: t("categories.actions"),
          icon: <Plus className="size-4" />,
          action: () => router.push("/dashboard/teacher/assignments/new"),
          keywords: ["create", "new", "assignment", "homework"],
        }
      )
    }

    if (isLearner) {
      items.push(
        {
          id: "action-bookmark",
          title: t("actions.bookmark"),
          description: t("actions.bookmarkDesc"),
          category: t("categories.actions"),
          shortcut: "⌘B",
          icon: <BookMarked className="size-4" />,
          action: () => router.push("/dashboard/learner/bookmarks"),
          keywords: ["bookmark", "save", "favorite"],
        },
        {
          id: "action-history",
          title: t("actions.history"),
          description: t("actions.historyDesc"),
          category: t("categories.actions"),
          shortcut: "⌘H",
          icon: <Clock className="size-4" />,
          action: () => router.push("/dashboard/learner/history"),
          keywords: ["history", "recent", "activity"],
        }
      )
    }

    if (isAdmin || isOrgAdmin) {
      items.push(
        {
          id: "action-create-course",
          title: t("actions.createCourse"),
          description: t("actions.createCourseDesc"),
          category: t("categories.actions"),
          icon: <Plus className="size-4" />,
          action: () => router.push(isAdmin ? "/dashboard/platform-admin/courses/new" : "/dashboard/admin/courses/new"),
          keywords: ["create", "new", "course"],
        },
        {
          id: "action-create-event",
          title: t("actions.createEvent"),
          description: t("actions.createEventDesc"),
          category: t("categories.actions"),
          icon: <Calendar className="size-4" />,
          action: () => router.push("/dashboard/admin/events/new"),
          keywords: ["create", "new", "event", "calendar"],
        }
      )
    }

    if (isRegional) {
      items.push(
        {
          id: "action-create-announcement",
          title: tn("regionalAnnouncements"),
          description: t("nav.announcementsDesc"),
          category: t("categories.actions"),
          shortcut: "⌘N",
          icon: <Plus className="size-4" />,
          action: () => router.push("/dashboard/regional-admin/communication?compose=1"),
          keywords: ["announcement", "create", "communicate", "regional"],
        },
        {
          id: "action-review-verification",
          title: tn("verification"),
          description: t("nav.verificationDesc"),
          category: t("categories.actions"),
          icon: <ClipboardCheck className="size-4" />,
          action: () => router.push("/dashboard/regional-admin/governance/verification"),
          keywords: ["review", "verification", "approve", "reject"],
        },
      )
    }

    return items
  }, [router, isAdmin, isOrgAdmin, isTeacher, isLearner, isRegional, t])

  const adminItems = useMemo((): ItemType[] => {
    if (!isAdmin && !isOrgAdmin) return []

    const items: ItemType[] = [
      {
        id: "admin-users",
        title: tn("users"),
        description: t("admin.usersDesc"),
        category: t("categories.admin"),
        icon: <Users className="size-4" />,
        action: () => router.push(isAdmin ? "/dashboard/platform-admin/users" : "/dashboard/admin/people"),
        keywords: ["user", "people", "member", "account"],
      },
      {
        id: "admin-institutions",
        title: tn("institutions"),
        description: t("admin.institutionsDesc"),
        category: t("categories.admin"),
        icon: <Shield className="size-4" />,
        action: () => router.push(isAdmin ? "/dashboard/platform-admin/institutions" : "/dashboard/admin/institutions"),
        keywords: ["institution", "school", "organization", "tenant"],
      },
    ]

    if (isAdmin) {
      items.push(
        {
          id: "admin-services",
          title: tn("services"),
          description: t("admin.servicesDesc"),
          category: t("categories.admin"),
          icon: <Settings className="size-4" />,
          action: () => router.push("/dashboard/platform-admin/services"),
          keywords: ["service", "configuration", "config"],
        },
        {
          id: "admin-delegations",
          title: tn("delegations"),
          description: t("admin.delegationsDesc"),
          category: t("categories.admin"),
          icon: <Users className="size-4" />,
          action: () => router.push("/dashboard/platform-admin/delegations"),
          keywords: ["delegation", "delegate", "authority"],
        },
        {
          id: "admin-verifications",
          title: tn("verifications"),
          description: t("admin.verificationsDesc"),
          category: t("categories.admin"),
          icon: <Shield className="size-4" />,
          action: () => router.push("/dashboard/platform-admin/verifications"),
          keywords: ["verification", "verify", "certificate"],
        }
      )
    }

    return items
  }, [router, isAdmin, isOrgAdmin, t, tn])

  const helpItems = useMemo((): ItemType[] => [
    {
      id: "help-shortcuts",
      title: t("help.shortcuts"),
      description: t("help.shortcutsDesc"),
      category: t("categories.help"),
      shortcut: "?",
      icon: <HelpCircle className="size-4" />,
      action: () => router.push("/shortcuts"),
      keywords: ["shortcut", "keyboard", "hotkey", "key"],
    },
    {
      id: "help-documentation",
      title: t("help.documentation"),
      description: t("help.documentationDesc"),
      category: t("categories.help"),
      icon: <BookOpen className="size-4" />,
      action: () => window.open("/docs", "_blank"),
      keywords: ["doc", "documentation", "guide", "manual"],
    },
    {
      id: "help-support",
      title: t("help.support"),
      description: t("help.supportDesc"),
      category: t("categories.help"),
      icon: <LifeBuoy className="size-4" />,
      action: () => router.push("/support"),
      keywords: ["support", "help", "contact", "ticket"],
    },
    {
      id: "help-feedback",
      title: t("help.feedback"),
      description: t("help.feedbackDesc"),
      category: t("categories.help"),
      icon: <ExternalLink className="size-4" />,
      action: () => window.open("/feedback", "_blank"),
      keywords: ["feedback", "suggest", "report", "bug"],
    },
  ], [router, t])

  const allCategories = useMemo((): CategoryType[] => {
    const categories: CategoryType[] = [
      { id: "navigation", label: t("categories.navigation"), items: navigationItems },
      { id: "actions", label: t("categories.actions"), items: actionItems },
    ]

    if (adminItems.length > 0) {
      categories.push({ id: "admin", label: t("categories.admin"), items: adminItems })
    }

    categories.push({ id: "help", label: t("categories.help"), items: helpItems })

    return categories
  }, [navigationItems, actionItems, adminItems, helpItems, t])

  useEffect(() => {
    palette.registerCategories(allCategories)
  }, [allCategories, palette])

  if (!palette.isOpen) return null

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center pt-20" role="dialog" aria-modal="true" aria-label={t("title")}>
      <div className="absolute inset-0 bg-foreground/40" onClick={palette.close} />
      <div className="relative w-full max-w-2xl rounded-2xl border border-border bg-card shadow-xl overflow-hidden animate-in slide-in-from-top-2 duration-200">
        <div className="flex items-center gap-3 border-b border-border px-4 py-3">
          <Search className="size-5 text-muted-foreground shrink-0" aria-hidden="true" />
          <input
            ref={palette.inputRef}
            type="text"
            value={palette.query}
            onChange={(e) => palette.setQuery(e.target.value)}
            onKeyDown={palette.handleKeyDown}
            placeholder={t("placeholder")}
            className="flex-1 bg-transparent py-2 text-sm outline-none placeholder:text-muted-foreground"
            aria-label={t("placeholder")}
            autoComplete="off"
          />
          <kbd className="hidden rounded border border-border px-2 py-0.5 text-[10px] font-mono text-muted-foreground sm:inline-flex">⌘K</kbd>
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            onClick={palette.close}
            aria-label={tc("close")}
          >
            <X className="size-4" />
          </Button>
        </div>

        <div className="max-h-[60vh] overflow-y-auto p-2">
          {palette.query && palette.allFilteredItems.length === 0 && (
            <CommandPaletteEmpty query={palette.query} />
          )}

          {!palette.query && palette.recentItems.length > 0 && (
            <CommandPaletteCategory label={t("categories.recent")} itemCount={palette.recentItems.length}>
              {palette.recentItems.map((item, index) => (
                <CommandPaletteItem
                  key={item.id}
                  item={{ ...item, shortcut: undefined }}
                  isSelected={palette.selectedIndex === index}
                  onSelect={() => palette.selectItem(index)}
                />
              ))}
            </CommandPaletteCategory>
          )}

          {palette.categories.map((category, catIndex) => (
            <CommandPaletteCategory key={category.id} label={category.label} itemCount={category.items.length}>
              {category.items.map((item, itemIndex) => {
                const globalIndex = palette.allFilteredItems.findIndex(i => i.id === item.id)
                return (
                  <CommandPaletteItem
                    key={item.id}
                    item={item}
                    isSelected={globalIndex === palette.selectedIndex}
                    onSelect={() => palette.selectItem(globalIndex)}
                  />
                )
              })}
            </CommandPaletteCategory>
          ))}

          {palette.categories.length === 0 && !palette.query && (
            <CommandPaletteEmpty query="" />
          )}
        </div>

        <div className="border-t border-border px-3 py-2 text-xs text-muted-foreground flex items-center justify-between">
          <kbd className="rounded border border-border px-1.5 py-0.5 font-mono">⌘K</kbd>
          <span>{t("footerHint")}</span>
          <kbd className="rounded border border-border px-1.5 py-0.5 font-mono">Esc</kbd>
        </div>
      </div>
    </div>
  )
}

export function CommandPaletteTrigger() {
  const palette = useCommandPalette()
  const t = useTranslations("commandPalette")
  const tc = useTranslations("common")

  return (
    <Button
      variant="ghost"
      size="sm"
      className="hidden items-center gap-2 rounded-lg border border-border bg-muted/60 px-3 py-2 text-sm text-muted-foreground transition-colors hover:bg-muted sm:flex"
      onClick={palette.open}
      aria-label={t("open")}
    >
      <Search className="size-4" />
      <span>{t("open")}</span>
      <kbd className="ml-4 rounded border border-border px-1.5 py-0.5 text-[10px] font-mono">⌘K</kbd>
    </Button>
  )
}