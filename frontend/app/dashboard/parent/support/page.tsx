"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { LifeBuoy, Loader2, Plus, Send, ChevronDown, ChevronUp } from "lucide-react"
import { parentApi, type SupportTicketItem } from "@/lib/parent-api"

export default function ParentSupportPage() {
  const t = useTranslations("parent")
  const tn = useTranslations("nav")
  const tc = useTranslations("common")
  const [tickets, setTickets] = useState<SupportTicketItem[]>([])
  const [openCount, setOpenCount] = useState(0)
  const [resolvedCount, setResolvedCount] = useState(0)
  const [loading, setLoading] = useState(true)
  const [showForm, setShowForm] = useState(false)
  const [expandedTicket, setExpandedTicket] = useState<string | null>(null)
  const [messages, setMessages] = useState<Record<string, Array<{ id: string; senderId: string; message: string; createdAt: string }>>>({})
  const [newMessage, setNewMessage] = useState("")
  const [form, setForm] = useState({ subject: "", description: "", category: "LEARNING", priority: "NORMAL" })
  const [submitting, setSubmitting] = useState(false)

  const CATEGORIES = [
    { value: "LEARNING", label: tn("learning") },
    { value: "PAYMENT", label: tn("payments") },
    { value: "LIVE_CLASS", label: tn("liveClasses") },
    { value: "TECHNICAL", label: t("support.catTechnical") },
    { value: "TEACHER_COMMUNICATION", label: t("support.catTeacherComm") },
    { value: "ACCOUNT", label: tn("account") },
    { value: "OTHER", label: t("support.catOther") },
  ]

  const PRIORITIES = [
    { value: "LOW", label: t("support.priorityLow") },
    { value: "NORMAL", label: t("support.priorityNormal") },
    { value: "HIGH", label: t("support.priorityHigh") },
    { value: "URGENT", label: t("support.priorityUrgent") },
  ]

  useEffect(() => {
    loadTickets()
  }, [])

  function loadTickets() {
    parentApi.getTickets().then((data) => {
      setTickets(data.tickets || [])
      setOpenCount(data.openCount || 0)
      setResolvedCount(data.resolvedCount || 0)
      setLoading(false)
    }).catch(() => setLoading(false))
  }

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault()
    setSubmitting(true)
    try {
      await parentApi.createSupportTicket(form)
      setForm({ subject: "", description: "", category: "LEARNING", priority: "NORMAL" })
      setShowForm(false)
      loadTickets()
    } finally {
      setSubmitting(false)
    }
  }

  async function loadMessages(ticketId: string) {
    if (messages[ticketId]) {
      setExpandedTicket(expandedTicket === ticketId ? null : ticketId)
      return
    }
    const msgs = await parentApi.getTicketMessages(ticketId).catch(() => [])
    setMessages((prev) => ({ ...prev, [ticketId]: msgs }))
    setExpandedTicket(ticketId)
  }

  async function handleSendMessage(ticketId: string) {
    if (!newMessage.trim()) return
    await parentApi.addTicketMessage(ticketId, newMessage)
    setNewMessage("")
    const msgs = await parentApi.getTicketMessages(ticketId).catch(() => [])
    setMessages((prev) => ({ ...prev, [ticketId]: msgs }))
  }

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("support.title")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("support.subtitle", { open: openCount, resolved: resolvedCount })}</p>
        </div>
        <button onClick={() => setShowForm(!showForm)} className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          <Plus className="size-4" /> {t("support.newTicket")}
        </button>
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
          <div>
            <label className="text-sm font-medium text-foreground">{t("support.subjectLabel")}</label>
            <input value={form.subject} onChange={(e) => setForm({ ...form, subject: e.target.value })} required
              className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm" placeholder={t("support.subjectPlaceholder")} />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="text-sm font-medium text-foreground">{t("support.categoryLabel")}</label>
              <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm">
                {CATEGORIES.map((c) => <option key={c.value} value={c.value}>{c.label}</option>)}
              </select>
            </div>
            <div>
              <label className="text-sm font-medium text-foreground">{t("support.priorityLabel")}</label>
              <select value={form.priority} onChange={(e) => setForm({ ...form, priority: e.target.value })}
                className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm">
                {PRIORITIES.map((p) => <option key={p.value} value={p.value}>{p.label}</option>)}
              </select>
            </div>
          </div>
          <div>
            <label className="text-sm font-medium text-foreground">{t("support.descLabel")}</label>
            <textarea value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} required rows={4}
              className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm" placeholder={t("support.descPlaceholder")} />
          </div>
          <div className="flex gap-2">
            <button type="submit" disabled={submitting} className="rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
              {submitting ? t("support.submitting") : t("support.submitTicket")}
            </button>
            <button type="button" onClick={() => setShowForm(false)} className="rounded-lg border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-muted">
              {tc("cancel")}
            </button>
          </div>
        </form>
      )}

      {tickets.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-16 text-center">
          <LifeBuoy className="mx-auto mb-3 size-10 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">{t("support.emptyTitle")}</p>
          <p className="mt-1 text-xs text-muted-foreground">{t("support.emptyDesc")}</p>
        </div>
      ) : (
        <div className="space-y-3">
          {tickets.map((ticket) => (
            <div key={ticket.id} className="rounded-2xl border border-border bg-card shadow-xs">
              <button onClick={() => loadMessages(ticket.id)} className="flex w-full items-center gap-3 p-4 text-left">
                <div className={`size-2 shrink-0 rounded-full ${ticket.status === "OPEN" ? "bg-orange" : "bg-green-500"}`} />
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-semibold text-foreground">{ticket.subject}</p>
                  <p className="text-xs text-muted-foreground">{ticket.category} · {ticket.priority} · {new Date(ticket.createdAt).toLocaleDateString("en-GB")}</p>
                </div>
                <span className={`shrink-0 rounded px-1.5 py-0.5 text-[10px] font-semibold ${ticket.status === "OPEN" ? "bg-orange-100 text-orange-700" : "bg-green-100 text-green-700"}`}>
                  {ticket.status}
                </span>
                {expandedTicket === ticket.id ? <ChevronUp className="size-4 shrink-0 text-muted-foreground" /> : <ChevronDown className="size-4 shrink-0 text-muted-foreground" />}
              </button>

              {expandedTicket === ticket.id && (
                <div className="border-t border-border px-4 pb-4">
                  <p className="mt-3 text-sm text-muted-foreground">{ticket.description}</p>
                  {messages[ticket.id] && messages[ticket.id].length > 0 && (
                    <div className="mt-3 space-y-2">
                      {messages[ticket.id].map((msg) => (
                        <div key={msg.id} className="rounded-lg bg-muted/50 p-3">
                          <p className="text-xs text-muted-foreground">{new Date(msg.createdAt).toLocaleString("en-GB")}</p>
                          <p className="mt-1 text-sm text-foreground">{msg.message}</p>
                        </div>
                      ))}
                    </div>
                  )}
                  {ticket.status === "OPEN" && (
                    <div className="mt-3 flex gap-2">
                      <input value={newMessage} onChange={(e) => setNewMessage(e.target.value)} onKeyDown={(e) => e.key === "Enter" && handleSendMessage(ticket.id)}
                        className="flex-1 rounded-lg border border-border bg-background px-3 py-2 text-sm" placeholder={t("support.messagePlaceholder")} />
                      <button onClick={() => handleSendMessage(ticket.id)} aria-label={t("support.sendMessage")} className="rounded-lg bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
                        <Send className="size-4" />
                      </button>
                    </div>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
