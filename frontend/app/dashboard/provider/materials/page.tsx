"use client"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { nfeApi } from "@/lib/nfe-api"
import { ProviderCreateDialog, type ProviderFormField } from "@/components/provider/provider-create-dialog"
import { Loader2, Plus, Search } from "lucide-react"

export default function MaterialsPage() {
  const t = useTranslations("provider")
  const [materials, setMaterials] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [createOpen, setCreateOpen] = useState(false)

  useEffect(() => {
    loadMaterials()
  }, [])

  async function loadMaterials() {
    try {
      setLoading(true)
      const data = await nfeApi.listMaterials()
      setMaterials(data)
    } catch { /* empty */ } finally {
      setLoading(false)
    }
  }

  const filtered = materials.filter((m) =>
    (m.title || m.name || "").toLowerCase().includes(search.toLowerCase())
  )

  const createFields: ProviderFormField[] = [
    { name: "title", label: t("form.title"), type: "text", required: true },
    {
      name: "materialType",
      label: t("form.type"),
      type: "select",
      required: true,
      options: ["DOCUMENT", "VIDEO", "AUDIO", "LINK", "FILE"].map((v) => ({ value: v, label: t(`form.materialTypes.${v}`) })),
    },
    { name: "description", label: t("form.description"), type: "textarea" },
    { name: "contentUrl", label: t("form.contentUrl"), type: "text" },
    {
      name: "isFree",
      label: t("form.access"),
      type: "select",
      options: [
        { value: "true", label: t("form.free") },
        { value: "false", label: t("form.paid") },
      ],
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{t("materials.title")}</h1>
          <p className="text-muted-foreground">{t("materials.subtitle")}</p>
        </div>
        <button
          onClick={() => setCreateOpen(true)}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" /> {t("materials.addMaterial")}
        </button>
      </div>
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Search className="size-4 text-muted-foreground" />
            <input
              placeholder={t("materials.searchPlaceholder")}
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground"
            />
          </div>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex items-center justify-center py-8"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
          ) : filtered.length === 0 ? (
            <p className="py-4 text-center text-sm text-muted-foreground">{t("materials.empty")}</p>
          ) : (
            <div className="space-y-3">
              {filtered.map((material) => (
                <div key={material.id} className="flex items-center justify-between rounded-lg border border-border p-4">
                  <div>
                    <p className="text-sm font-medium text-foreground">{material.title || material.name}</p>
                    <p className="text-xs text-muted-foreground">{material.type || material.fileType || t("materials.fileFallback")}</p>
                  </div>
                  <span className="rounded-full bg-purple-100 px-2 py-0.5 text-xs font-medium text-purple-700">{material.status || t("materials.availableFallback")}</span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <ProviderCreateDialog
        open={createOpen}
        title={t("materials.addMaterial")}
        fields={createFields}
        onClose={() => setCreateOpen(false)}
        onSubmit={async (v) => {
          await nfeApi.createMaterial({
            title: v.title,
            materialType: v.materialType,
            description: v.description,
            contentUrl: v.contentUrl,
            isFree: v.isFree === undefined ? undefined : v.isFree === "true",
          })
          setCreateOpen(false)
          loadMaterials()
        }}
      />
    </div>
  )
}
