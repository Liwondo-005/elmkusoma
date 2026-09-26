"use client"

import Link from "next/link"
import { useRequireAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { Palette, PenTool, BookOpen, Mic, Image, Film, ArrowRight, Loader2 } from "lucide-react"


export default function CreatePage() {
  const { user, loading } = useRequireAuth()
  const t = useTranslations("primary")
  const ts = useTranslations("status")
  const creationTypes = [
  { label: t("create.drawing"), description: t("create.drawingDesc"), icon: Palette, color: "bg-pink-500", href: "/dashboard/portfolio?type=DRAWING" },
  { label: t("create.writing"), description: t("create.writingDesc"), icon: PenTool, color: "bg-purple-500", href: "/dashboard/portfolio?type=STORY" },
  { label: t("create.reading"), description: t("create.readingDesc"), icon: BookOpen, color: "bg-blue-500", href: "/dashboard/portfolio?type=ESSAY" },
  { label: t("create.voice"), description: t("create.voiceDesc"), icon: Mic, color: "bg-green-500", href: "/dashboard/portfolio?type=VOICE_RECORDING" },
  { label: t("create.photo"), description: t("create.photoDesc"), icon: Image, color: "bg-orange-500", href: "/dashboard/portfolio?type=PHOTO" },
  { label: t("create.video"), description: t("create.videoDesc"), icon: Film, color: "bg-red-500", href: "/dashboard/portfolio?type=PROJECT" },
]

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center">
        <Loader2 className="size-8 animate-spin text-primary" />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-gradient-to-br from-pink-50 via-card to-purple-50 p-6 shadow-xs">
        <div className="flex items-center gap-3">
          <div className="flex size-12 items-center justify-center rounded-2xl bg-pink-500/10">
            <Palette className="size-6 text-pink-600" />
          </div>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("create.title")}</h1>
            <p className="text-sm text-muted-foreground">{t("create.subtitle")}</p>
          </div>
        </div>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {creationTypes.map((type) => (
          <Link
            key={type.label}
            href={type.href}
            className="group rounded-2xl border border-border bg-card p-5 transition-all hover:shadow-md hover:border-primary/30"
          >
            <div className={`flex size-10 items-center justify-center rounded-xl ${type.color} text-white mb-3`}>
              <type.icon className="size-5" />
            </div>
            <h3 className="text-base font-semibold text-foreground">{type.label}</h3>
            <p className="mt-1 text-sm text-muted-foreground">{type.description}</p>
            <div className="mt-3 flex items-center gap-1 text-xs font-medium text-primary opacity-0 transition-opacity group-hover:opacity-100">
              {t("create.startCreating")} <ArrowRight className="size-3" />
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}
