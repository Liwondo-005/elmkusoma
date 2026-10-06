"use client"
import { useState, useEffect } from "react"
import { useTranslations } from "next-intl"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { nfeApi } from "@/lib/nfe-api"
import { adminApi, getInstitutionId } from "@/lib/api"
import { ProviderCreateDialog, type ProviderFormField } from "@/components/provider/provider-create-dialog"
import { Loader2, Plus, Search } from "lucide-react"

export default function LearnersPage() {
  const t = useTranslations("provider")
  const tn = useTranslations("nav")
  const ts = useTranslations("status")
  const [learners, setLearners] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState("")
  const [createOpen, setCreateOpen] = useState(false)
  const [people, setPeople] = useState<Array<{ userId: string; fullName: string; email: string }>>([])
  const [peopleLoaded, setPeopleLoaded] = useState(false)

  useEffect(() => {
    loadLearners()
  }, [])

  async function loadLearners() {
    try {
      setLoading(true)
      const data = await nfeApi.listLearners()
      setLearners(data)
    } catch { /* empty */ } finally {
      setLoading(false)
    }
  }

  async function openCreate() {
    setCreateOpen(true)
    if (!peopleLoaded) {
      setPeopleLoaded(true)
      try {
        const data = await adminApi.listPeople(getInstitutionId() || "", 0, 100)
        setPeople(data || [])
      } catch { /* dialog select stays empty */ }
    }
  }

  const filtered = learners.filter((l) =>
    (l.name || l.fullName || l.participantNumber || l.occupation || "").toLowerCase().includes(search.toLowerCase()) ||
    (l.email || l.organization || "").toLowerCase().includes(search.toLowerCase())
  )

  const createFields: ProviderFormField[] = [
    {
      name: "userId",
      label: t("form.learner"),
      type: "select",
      required: true,
      options: people.map((p) => ({ value: p.userId, label: `${p.fullName} (${p.email})` })),
    },
    { name: "participantNumber", label: t("form.participantNumber"), type: "text" },
    { name: "occupation", label: t("form.occupation"), type: "text" },
    { name: "organization", label: t("form.organization"), type: "text" },
  ]

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{tn("learners")}</h1>
          <p className="text-muted-foreground">{t("learners.subtitle")}</p>
        </div>
        <button
          onClick={openCreate}
          className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Plus className="size-4" /> {t("learners.addLearner")}
        </button>
      </div>
      <Card>
        <CardHeader>
          <div className="flex items-center gap-2">
            <Search className="size-4 text-muted-foreground" />
            <input
              placeholder={t("learners.searchPlaceholder")}
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
            <p className="py-4 text-center text-sm text-muted-foreground">{t("learners.empty")}</p>
          ) : (
            <div className="space-y-3">
              {filtered.map((learner) => (
                <div key={learner.id} className="flex items-center justify-between rounded-lg border border-border p-4">
                  <div>
                    <p className="text-sm font-medium text-foreground">{learner.name || learner.fullName || learner.participantNumber || learner.occupation || String(learner.userId || "").slice(0, 8)}</p>
                    <p className="text-xs text-muted-foreground">{learner.email || learner.phone || learner.organization || t("learners.noContactFallback")}</p>
                  </div>
                  <span className="rounded-full bg-green-100 px-2 py-0.5 text-xs font-medium text-green-700">{learner.status || ts("active")}</span>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <ProviderCreateDialog
        open={createOpen}
        title={t("learners.addLearner")}
        fields={createFields}
        onClose={() => setCreateOpen(false)}
        onSubmit={async (v) => {
          await nfeApi.createLearner({
            userId: v.userId,
            participantNumber: v.participantNumber,
            occupation: v.occupation,
            organization: v.organization,
          })
          setCreateOpen(false)
          loadLearners()
        }}
      />
    </div>
  )
}
