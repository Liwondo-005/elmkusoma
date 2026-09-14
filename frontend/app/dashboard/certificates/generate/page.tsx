"use client"

import { useEffect, useState } from "react"
import { Award, Loader2, CheckCircle, XCircle, Send, Ban } from "lucide-react"
import { certificateApi, getInstitutionId, type CertificateResponse, type TemplateResponse } from "@/lib/api"

type Tab = "generate" | "manage"

export default function CertificateGeneratePage() {
  const [activeTab, setActiveTab] = useState<Tab>("generate")
  const institutionId = getInstitutionId()

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Certificates</h1>
        <p className="mt-1 text-sm text-muted-foreground">Generate, issue, and manage certificates.</p>
      </div>

      <div className="flex gap-1 rounded-xl border border-border bg-muted p-1">
        {([
          { key: "generate" as Tab, label: "Generate Certificate", icon: Award },
          { key: "manage" as Tab, label: "Manage Certificates", icon: CheckCircle },
        ]).map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={`flex flex-1 items-center justify-center gap-2 rounded-lg px-4 py-2.5 text-sm font-medium transition-colors ${
              activeTab === tab.key ? "bg-card text-foreground shadow-xs" : "text-muted-foreground hover:text-foreground"
            }`}
          >
            <tab.icon className="size-4" />
            {tab.label}
          </button>
        ))}
      </div>

      {!institutionId ? (
        <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center">
          <p className="text-sm font-medium text-destructive">No institution context found.</p>
        </div>
      ) : activeTab === "generate" ? (
        <GenerateForm institutionId={institutionId} />
      ) : (
        <ManageCertificates institutionId={institutionId} />
      )}
    </div>
  )
}

function GenerateForm({ institutionId }: { institutionId: string }) {
  const [templates, setTemplates] = useState<TemplateResponse[]>([])
  const [templateId, setTemplateId] = useState("")
  const [studentId, setStudentId] = useState("")
  const [title, setTitle] = useState("")
  const [completionDate, setCompletionDate] = useState("")
  const [description, setDescription] = useState("")
  const [saving, setSaving] = useState(false)
  const [result, setResult] = useState<CertificateResponse | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    certificateApi.listTemplates().then(setTemplates).catch(() => {})
  }, [])

  async function handleGenerate() {
    if (!templateId || !studentId || !title.trim()) return
    setSaving(true)
    setError(null)
    try {
      const cert = await certificateApi.generate({
        templateId,
        studentId,
        title: title.trim(),
        certificateType: "COMPLETION",
        studentName: studentId,
        completionDate: completionDate || new Date().toISOString().slice(0, 10),
        description: description || undefined,
      })
      setResult(cert)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to generate certificate")
    } finally {
      setSaving(false)
    }
  }

  if (result) {
    return (
      <div className="rounded-2xl border border-teal/20 bg-teal/5 p-6 text-center">
        <CheckCircle className="mx-auto size-10 text-teal" />
        <h3 className="mt-3 text-sm font-semibold text-foreground">Certificate Generated</h3>
        <p className="mt-1 text-xs text-muted-foreground">Serial: {result.serialNumber}</p>
        <p className="text-xs text-muted-foreground">Status: {result.status}</p>
        <button
          onClick={() => { setResult(null); setTitle(""); setStudentId(""); setTemplateId("") }}
          className="mt-4 flex h-9 items-center gap-2 rounded-lg bg-primary px-4 text-xs font-medium text-primary-foreground hover:bg-primary/90 mx-auto"
        >
          Generate Another
        </button>
      </div>
    )
  }

  return (
    <div className="rounded-2xl border border-border bg-card p-6 shadow-xs space-y-4">
      <h3 className="text-sm font-semibold text-foreground">Generate a New Certificate</h3>

      {error && (
        <div className="rounded-lg border border-destructive/20 bg-destructive/5 px-4 py-2 text-xs text-destructive">{error}</div>
      )}

      <select value={templateId} onChange={(e) => setTemplateId(e.target.value)} className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring">
        <option value="">Select template</option>
        {templates.map((t) => (
          <option key={t.id} value={t.id}>{t.name} ({t.templateType})</option>
        ))}
      </select>

      <input type="text" placeholder="Student ID" value={studentId} onChange={(e) => setStudentId(e.target.value)} className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring" />
      <input type="text" placeholder="Certificate title (e.g. Best Performance Award)" value={title} onChange={(e) => setTitle(e.target.value)} className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring" />
      <input type="date" placeholder="Completion date" value={completionDate} onChange={(e) => setCompletionDate(e.target.value)} className="h-10 w-full rounded-lg border border-border bg-background px-3 text-sm outline-none focus:border-ring" />
      <textarea placeholder="Description (optional)" value={description} onChange={(e) => setDescription(e.target.value)} rows={2} className="w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:border-ring" />

      <button
        onClick={handleGenerate}
        disabled={saving || !templateId || !studentId || !title.trim()}
        className="flex h-9 items-center gap-2 rounded-lg bg-primary px-4 text-xs font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
      >
        <Award className="size-3.5" /> {saving ? "Generating..." : "Generate Certificate"}
      </button>
    </div>
  )
}

function ManageCertificates({ institutionId }: { institutionId: string }) {
  const [certs, setCerts] = useState<CertificateResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [actingId, setActingId] = useState<string | null>(null)

  useEffect(() => {
    certificateApi
      .list()
      .then(setCerts)
      .catch((err) => setError(err instanceof Error ? err.message : "Failed to load certificates"))
      .finally(() => setLoading(false))
  }, [institutionId])

  async function handleIssue(id: string) {
    setActingId(id)
    try {
      const updated = await certificateApi.issue(id)
      setCerts((prev) => prev.map((c) => (c.id === id ? updated : c)))
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to issue certificate")
    } finally {
      setActingId(null)
    }
  }

  async function handleRevoke(id: string) {
    const reason = prompt("Reason for revocation:")
    if (!reason) return
    setActingId(id)
    try {
      const updated = await certificateApi.revoke(id, reason)
      setCerts((prev) => prev.map((c) => (c.id === id ? updated : c)))
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to revoke certificate")
    } finally {
      setActingId(null)
    }
  }

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
  if (error) return <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-10 text-center"><p className="text-sm font-medium text-destructive">{error}</p></div>
  if (certs.length === 0) return (
    <div className="flex flex-col items-center justify-center rounded-2xl border border-dashed border-border bg-muted/30 py-20 text-center">
      <Award className="size-10 text-muted-foreground/50" />
      <p className="mt-4 text-sm font-medium text-foreground">No certificates yet</p>
      <p className="mt-1 text-sm text-muted-foreground">Generate a certificate to get started.</p>
    </div>
  )

  return (
    <div className="space-y-3">
      {certs.map((cert) => (
        <div key={cert.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2">
                <span className="text-sm font-semibold text-foreground">{cert.title}</span>
                <span className={`rounded px-2 py-0.5 text-[10px] font-semibold ${
                  cert.status === "ISSUED" ? "bg-teal/10 text-teal"
                    : cert.status === "REVOKED" ? "bg-destructive/10 text-destructive"
                    : "bg-muted text-muted-foreground"
                }`}>{cert.status}</span>
              </div>
              <p className="mt-0.5 text-xs text-muted-foreground">Serial: {cert.serialNumber}</p>
              {cert.studentName && <p className="text-xs text-muted-foreground">Student: {cert.studentName}</p>}
              {cert.completionDate && <p className="text-xs text-muted-foreground">Date: {cert.completionDate}</p>}
            </div>
            <div className="flex items-center gap-2">
              {cert.status === "DRAFT" && (
                <button
                  onClick={() => handleIssue(cert.id)}
                  disabled={actingId === cert.id}
                  className="flex h-7 items-center gap-1.5 rounded-lg bg-teal/10 px-2.5 text-[10px] font-medium text-teal hover:bg-teal/20 disabled:opacity-50"
                >
                  <Send className="size-3" /> Issue
                </button>
              )}
              {cert.status === "ISSUED" && (
                <button
                  onClick={() => handleRevoke(cert.id)}
                  disabled={actingId === cert.id}
                  className="flex h-7 items-center gap-1.5 rounded-lg border border-destructive/20 px-2.5 text-[10px] font-medium text-destructive hover:bg-destructive/5 disabled:opacity-50"
                >
                  <Ban className="size-3" /> Revoke
                </button>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  )
}
