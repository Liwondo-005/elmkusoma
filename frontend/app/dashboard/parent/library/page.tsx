"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { BookOpen, Loader2, FileText, Video, Image, Link as LinkIcon } from "lucide-react"
import { parentApi, type LibraryCategory } from "@/lib/parent-api"

const TYPE_ICONS: Record<string, typeof BookOpen> = {
  DOCUMENT: FileText,
  VIDEO: Video,
  IMAGE: Image,
  LINK: LinkIcon,
  AUDIO: BookOpen,
  OTHER: BookOpen,
}

export default function ParentLibraryPage() {
  const t = useTranslations("parent")
  const [categories, setCategories] = useState<LibraryCategory[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    parentApi.getLibrary().then((data) => {
      setCategories(data.categories || [])
    }).catch(() => setCategories([])).finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("library.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("library.subtitle")}</p>
      </div>

      {categories.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-16 text-center">
          <BookOpen className="mx-auto mb-3 size-10 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">{t("library.emptyTitle")}</p>
          <p className="mt-1 text-xs text-muted-foreground">{t("library.emptyDesc")}</p>
        </div>
      ) : (
        <div className="space-y-6">
          {categories.map((cat) => (
            <section key={cat.name} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <h2 className="text-base font-semibold text-foreground">{cat.name}</h2>
              <p className="text-xs text-muted-foreground">{cat.description}</p>
              <div className="mt-3 space-y-2">
                {cat.items.map((item) => {
                  const Icon = TYPE_ICONS[item.resourceType] || BookOpen
                  return (
                    <div key={item.id} className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-3">
                      <Icon className="size-4 shrink-0 text-muted-foreground" />
                      <div className="min-w-0 flex-1">
                        <p className="text-sm font-medium text-foreground">{item.title}</p>
                        {item.description && <p className="text-xs text-muted-foreground line-clamp-1">{item.description}</p>}
                      </div>
                      <span className="shrink-0 rounded bg-muted px-1.5 py-0.5 text-[10px] font-semibold text-muted-foreground">
                        {item.resourceType}
                      </span>
                      {item.fileUrl && (
                        <a href={item.fileUrl} target="_blank" rel="noopener noreferrer"
                          className="shrink-0 text-xs font-medium text-primary hover:underline">
                          {t("library.open")}
                        </a>
                      )}
                    </div>
                  )
                })}
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
  )
}
