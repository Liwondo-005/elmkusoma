"use client"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { nfeApi } from "@/lib/nfe-api"
import { ProviderCreateDialog, type ProviderFormField } from "@/components/provider/provider-create-dialog"
import { Loader2, Plus, Search } from "lucide-react"

export default function CertificatesPage() {
  const t = useTranslations("provider")
  const tn = useTranslations("nav")
  const ts = useTranslations("status")
  const tl = useTranslations("learner")
  const [certificates, setCertificates] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [createOpen, setCreateOpen] = useState(false)

  useEffect(() => {
    loadCertificates()
  }, [])

  async function loadCertificates() {
    try {
      setLoading(true)
      const data = await nfeApi.listCertificates()
      setCertificates(data)
    } catch { /* empty */ } finally {
      setLoading(false)
    }
  }

  const filtered = certificates.filter((c) =>
    (c.studentName || c.learnerName || "").toLowerCase().includes(search.toLowerCase()) ||
    (c.serialNumber || "").toLowerCase().includes(search.toLowerCase())
  )

  const createFields: ProviderFormField[] = [
    {
      name: "certificateType",
      label: t("form.certificateType"),
      type: "select",
      required: true,
      options: ["COMPLETION", "PARTICIPATION", "ACHIEVEMENT"].map((v) => ({ value: v, label: t(`form.certificateTypes.${v}`) })),
    },
    { name: "title", label: t("form.title"), type: "text", required: true },
    { name: "studentName", label: t("form.studentName"), type: "text", required: true },
    { name: "serialNumber", label: t("form.serialNumber"), type: "text" },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{tn("certificates")}</h1>
          <p className="text-muted-foreground">{t("certificates.subtitle")}</p>
        </div>
        <button
          onClick={() => setCreateOpen(true)}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" /> {tl("certGenerate.generateButton")}
        </button>
      </div>
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Search className="size-4 text-muted-foreground" />
            <input
              placeholder={t("certificates.searchPlaceholder")}
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
            <p className="py-4 text-center text-sm text-muted-foreground">{t("certificates.empty")}</p>
          ) : (
            <div className="space-y-3">
              {filtered.map((cert) => (
                <div key={cert.id} className="flex items-center justify-between rounded-lg border border-border p-4">
                  <div>
                    <p className="text-sm font-medium text-foreground">{cert.studentName || cert.learnerName}</p>
                    <p className="text-xs text-muted-foreground">{cert.title || cert.serialNumber || t("certificates.titleFallback")}</p>
                  </div>
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${cert.status === "ISSUED" ? "bg-green-100 text-green-700" : cert.status === "REVOKED" ? "bg-red-100 text-red-700" : "bg-blue-100 text-blue-700"}`}>
                    {cert.status || ts("pending")}
                  </span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <ProviderCreateDialog
        open={createOpen}
        title={tl("certGenerate.generateButton")}
        fields={createFields}
        onClose={() => setCreateOpen(false)}
        onSubmit={async (v) => {
          await nfeApi.generateCertificate({
            certificateType: v.certificateType,
            title: v.title,
            studentName: v.studentName,
            serialNumber: v.serialNumber,
          })
          setCreateOpen(false)
          loadCertificates()
        }}
      />
    </div>
  )
}
