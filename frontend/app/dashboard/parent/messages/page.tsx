"use client"

import { useState, useEffect } from "react"
import { Search, MessageSquare, Loader2, Send, ArrowLeft, Trash2, CheckCircle, Circle } from "lucide-react"
import { parentApi, type MessageData, type ChildOverview } from "@/lib/parent-api"

export default function ParentMessagesPage() {
  const [messages, setMessages] = useState<MessageData[]>([])
  const [sentMessages, setSentMessages] = useState<MessageData[]>([])
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [loading, setLoading] = useState(true)
  const [activeTab, setActiveTab] = useState<"inbox" | "sent">("inbox")
  const [selectedMessage, setSelectedMessage] = useState<MessageData | null>(null)
  const [composing, setComposing] = useState(false)
  const [searchQuery, setSearchQuery] = useState("")
  const [composeForm, setComposeForm] = useState({ recipientId: "", subject: "", body: "", messageType: "DIRECT" })
  const [sending, setSending] = useState(false)

  useEffect(() => {
    Promise.all([
      parentApi.getMessages().catch(() => []),
      parentApi.getSentMessages().catch(() => []),
      parentApi.getChildren().catch(() => []),
    ]).then(([inbox, sent, kids]) => {
      setMessages(inbox)
      setSentMessages(sent)
      setChildren(kids)
    }).finally(() => setLoading(false))
  }, [])

  async function handleSend() {
    if (!composeForm.recipientId || !composeForm.subject.trim() || !composeForm.body.trim()) return
    setSending(true)
    try {
      const newMsg = await parentApi.sendMessage(composeForm)
      setSentMessages((prev) => [newMsg, ...prev])
      setComposing(false)
      setComposeForm({ recipientId: "", subject: "", body: "", messageType: "DIRECT" })
    } catch {
    } finally {
      setSending(false)
    }
  }

  async function handleMarkRead(msg: MessageData) {
    if (!msg.isRead) {
      try {
        await parentApi.markMessageRead(msg.id)
        setMessages((prev) => prev.map((m) => (m.id === msg.id ? { ...m, isRead: true } : m)))
        setSelectedMessage({ ...msg, isRead: true })
      } catch {}
    }
  }

  async function handleDelete(msgId: string) {
    try {
      await parentApi.deleteMessage(msgId)
      setMessages((prev) => prev.filter((m) => m.id !== msgId))
      setSentMessages((prev) => prev.filter((m) => m.id !== msgId))
      setSelectedMessage(null)
    } catch {}
  }

  const filteredInbox = messages.filter(
    (m) =>
      m.subject.toLowerCase().includes(searchQuery.toLowerCase()) ||
      m.senderName.toLowerCase().includes(searchQuery.toLowerCase())
  )
  const filteredSent = sentMessages.filter(
    (m) =>
      m.subject.toLowerCase().includes(searchQuery.toLowerCase()) ||
      m.recipientName.toLowerCase().includes(searchQuery.toLowerCase())
  )

  const displayMessages = activeTab === "inbox" ? filteredInbox : filteredSent

  if (selectedMessage) {
    return (
      <div className="mx-auto max-w-3xl space-y-6">
        <button onClick={() => setSelectedMessage(null)} className="flex items-center gap-2 text-sm text-muted-foreground hover:text-foreground">
          <ArrowLeft className="size-4" /> Back to {activeTab}
        </button>
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs space-y-4">
          <div className="flex items-start justify-between">
            <div>
              <h2 className="text-lg font-semibold text-foreground">{selectedMessage.subject}</h2>
              <p className="mt-1 text-sm text-muted-foreground">
                {activeTab === "inbox" ? `From: ${selectedMessage.senderName}` : `To: ${selectedMessage.recipientName}`}
              </p>
              <p className="text-xs text-muted-foreground">
                {new Date(selectedMessage.createdAt).toLocaleString()}
              </p>
            </div>
            <div className="flex gap-2">
              <button onClick={() => handleDelete(selectedMessage.id)} className="rounded-lg border border-border p-2 text-muted-foreground hover:text-red-500">
                <Trash2 className="size-4" />
              </button>
            </div>
          </div>
          <div className="rounded-xl border border-border bg-muted/30 p-4">
            <p className="whitespace-pre-wrap text-sm text-foreground">{selectedMessage.body}</p>
          </div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span className={`inline-flex items-center gap-1 rounded-full px-2 py-0.5 text-xs font-medium ${
              selectedMessage.isRead ? "bg-green-500/10 text-green-600" : "bg-blue-500/10 text-blue-600"
            }`}>
              {selectedMessage.isRead ? <CheckCircle className="size-3" /> : <Circle className="size-3" />}
              {selectedMessage.isRead ? "Read" : "Unread"}
            </span>
            <span className="rounded-full bg-muted px-2 py-0.5">{selectedMessage.messageType}</span>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-foreground">Messages</h1>
          <p className="mt-1 text-sm text-muted-foreground">Communicate with teachers and school staff.</p>
        </div>
        <button
          onClick={() => setComposing(true)}
          className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
        >
          <Send className="size-4" /> New Message
        </button>
      </div>

      {composing && (
        <div className="rounded-2xl border border-border bg-card p-6 shadow-xs space-y-4">
          <h2 className="text-base font-semibold text-foreground">New Message</h2>
          <div>
            <label className="text-xs font-medium text-muted-foreground">Recipient</label>
            <select
              value={composeForm.recipientId}
              onChange={(e) => setComposeForm({ ...composeForm, recipientId: e.target.value })}
              className="mt-1 w-full rounded-xl border border-border bg-muted/50 px-4 py-2.5 text-sm text-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option value="">Select recipient...</option>
              {children.map((c) => (
                <option key={c.studentId} value={c.studentId}>{c.studentName} ({c.className})</option>
              ))}
            </select>
          </div>
          <div>
            <label className="text-xs font-medium text-muted-foreground">Subject</label>
            <input
              type="text"
              value={composeForm.subject}
              onChange={(e) => setComposeForm({ ...composeForm, subject: e.target.value })}
              placeholder="Message subject"
              className="mt-1 w-full rounded-xl border border-border bg-muted/50 px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <div>
            <label className="text-xs font-medium text-muted-foreground">Message</label>
            <textarea
              value={composeForm.body}
              onChange={(e) => setComposeForm({ ...composeForm, body: e.target.value })}
              placeholder="Write your message..."
              rows={5}
              className="mt-1 w-full rounded-xl border border-border bg-muted/50 px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <div className="flex gap-2">
            <button onClick={handleSend} disabled={sending} className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50">
              {sending ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />} Send
            </button>
            <button onClick={() => setComposing(false)} className="rounded-xl border border-border px-4 py-2 text-sm font-medium text-muted-foreground hover:text-foreground">Cancel</button>
          </div>
        </div>
      )}

      <div className="flex gap-1 rounded-lg border border-border bg-muted p-1">
        {(["inbox", "sent"] as const).map((tab) => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`flex flex-1 items-center justify-center gap-2 rounded-md px-3 py-2 text-sm font-medium transition-colors ${
              activeTab === tab ? "bg-card text-foreground shadow-xs" : "text-muted-foreground hover:text-foreground"
            }`}
          >
            {tab === "inbox" ? "Inbox" : "Sent"}
            {tab === "inbox" && messages.filter((m) => !m.isRead).length > 0 && (
              <span className="rounded-full bg-primary/10 px-1.5 py-0.5 text-xs font-medium text-primary">
                {messages.filter((m) => !m.isRead).length}
              </span>
            )}
          </button>
        ))}
      </div>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          placeholder="Search messages..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full rounded-xl border border-border bg-card py-2.5 pl-10 pr-4 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
        />
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="size-6 animate-spin text-muted-foreground" />
        </div>
      ) : displayMessages.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border py-16 text-center">
          <MessageSquare className="mx-auto mb-3 size-8 text-muted-foreground" />
          <p className="text-sm font-medium text-foreground">
            {searchQuery ? "No messages match your search." : activeTab === "inbox" ? "No messages in your inbox." : "No sent messages."}
          </p>
          <p className="mt-1 text-xs text-muted-foreground">
            {activeTab === "inbox" ? "Messages from teachers and staff will appear here." : "Messages you send will appear here."}
          </p>
        </div>
      ) : (
        <div className="space-y-2">
          {displayMessages.map((msg) => (
            <button
              key={msg.id}
              onClick={() => {
                setSelectedMessage(msg)
                handleMarkRead(msg)
              }}
              className={`w-full rounded-xl border border-border bg-card p-4 text-left transition-colors hover:bg-muted/50 ${
                !msg.isRead ? "border-l-4 border-l-primary" : ""
              }`}
            >
              <div className="flex items-start justify-between">
                <div className="min-w-0 flex-1">
                  <p className={`text-sm font-medium text-foreground ${!msg.isRead ? "font-semibold" : ""}`}>
                    {msg.subject}
                  </p>
                  <p className="mt-0.5 text-xs text-muted-foreground">
                    {activeTab === "inbox" ? `From: ${msg.senderName}` : `To: ${msg.recipientName}`}
                  </p>
                </div>
                <div className="flex flex-col items-end gap-1">
                  <span className="text-xs text-muted-foreground">
                    {new Date(msg.createdAt).toLocaleDateString()}
                  </span>
                  {!msg.isRead && (
                    <span className="size-2 rounded-full bg-primary" />
                  )}
                </div>
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
