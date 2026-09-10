"use client"

import { useState } from "react"
import { Send, Search, ArrowLeft, MoreVertical } from "lucide-react"

const mockConversations = [
  { id: "1", name: "Mrs. Mwamba", role: "Class Teacher", subject: "Class 4A", lastMessage: "Amina is doing well in Mathematics", time: "10:30 AM", unread: 1, avatar: "MM" },
  { id: "2", name: "Mr. Temu", role: "Subject Teacher", subject: "Science", lastMessage: "Please ensure Juma brings his lab coat", time: "Yesterday", unread: 0, avatar: "JT" },
  { id: "3", name: "School Admin", role: "Administration", subject: "Fee Payment", lastMessage: "Fee reminder for Term 2", time: "3 days ago", unread: 0, avatar: "SA" },
  { id: "4", name: "Mrs. Kimaro", role: "Head of Year", subject: "Year 4", lastMessage: "Parent meeting scheduled for Friday", time: "5 days ago", unread: 0, avatar: "KK" },
]

const mockMessages = [
  { id: "1", sender: "Mrs. Mwamba", text: "Good morning! I wanted to update you on Amina's progress.", time: "10:15 AM", isOwn: false },
  { id: "2", sender: "You", text: "Good morning! Yes, please go ahead.", time: "10:20 AM", isOwn: true },
  { id: "3", sender: "Mrs. Mwamba", text: "Amina is doing well in Mathematics. Her test scores have improved significantly over the past month.", time: "10:25 AM", isOwn: false },
  { id: "4", sender: "Mrs. Mwamba", text: "However, I noticed she could use more practice in English composition. Would you like some suggested activities?", time: "10:30 AM", isOwn: false },
]

export default function ParentMessagesPage() {
  const [selectedConvo, setSelectedConvo] = useState<string | null>(null)
  const [messageText, setMessageText] = useState("")
  const [messages, setMessages] = useState(mockMessages)

  function sendMessage() {
    if (!messageText.trim()) return
    setMessages((prev) => [
      ...prev,
      { id: String(Date.now()), sender: "You", text: messageText.trim(), time: "Now", isOwn: true },
    ])
    setMessageText("")
  }

  if (selectedConvo) {
    const convo = mockConversations.find((c) => c.id === selectedConvo)
    return (
      <div className="mx-auto max-w-3xl space-y-0">
        <div className="flex items-center gap-3 border-b border-border bg-card px-4 py-3">
          <button onClick={() => setSelectedConvo(null)} className="rounded-lg p-1 hover:bg-muted"><ArrowLeft className="size-4" /></button>
          <div className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">{convo?.avatar}</div>
          <div className="min-w-0 flex-1">
            <p className="text-sm font-semibold text-foreground">{convo?.name}</p>
            <p className="text-[10px] text-muted-foreground">{convo?.role} &middot; {convo?.subject}</p>
          </div>
          <button className="rounded-lg p-2 hover:bg-muted"><MoreVertical className="size-4 text-muted-foreground" /></button>
        </div>

        <div className="space-y-4 overflow-y-auto bg-muted/30 p-4" style={{ height: "calc(100vh - 280px)" }}>
          {messages.map((m) => (
            <div key={m.id} className={`flex ${m.isOwn ? "justify-end" : "justify-start"}`}>
              <div className={`max-w-[75%] rounded-2xl px-4 py-2.5 ${
                m.isOwn ? "bg-primary text-primary-foreground" : "bg-card border border-border"
              }`}>
                <p className="text-sm">{m.text}</p>
                <p className={`mt-1 text-[10px] ${m.isOwn ? "text-primary-foreground/70" : "text-muted-foreground"}`}>{m.time}</p>
              </div>
            </div>
          ))}
        </div>

        <div className="flex items-center gap-2 border-t border-border bg-card px-4 py-3">
          <input
            type="text"
            value={messageText}
            onChange={(e) => setMessageText(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && sendMessage()}
            placeholder="Type a message..."
            className="flex-1 rounded-xl border border-border bg-muted/50 px-4 py-2.5 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
          />
          <button
            onClick={sendMessage}
            className="rounded-xl bg-primary p-2.5 text-primary-foreground hover:bg-primary/90"
          >
            <Send className="size-4" />
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <div>
        <h1 className="text-xl font-bold text-foreground">Messages</h1>
        <p className="mt-1 text-sm text-muted-foreground">Communicate with teachers and school staff.</p>
      </div>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
        <input
          type="text"
          placeholder="Search conversations..."
          className="w-full rounded-xl border border-border bg-card py-2.5 pl-10 pr-4 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-primary/20"
        />
      </div>

      <div className="space-y-2">
        {mockConversations.map((c) => (
          <button
            key={c.id}
            onClick={() => setSelectedConvo(c.id)}
            className="flex w-full items-center gap-3 rounded-2xl border border-border bg-card p-4 shadow-xs text-left transition-all hover:-translate-y-0.5 hover:shadow-md"
          >
            <div className="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-bold text-primary">{c.avatar}</div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center justify-between">
                <p className="text-sm font-semibold text-foreground">{c.name}</p>
                <span className="text-[10px] text-muted-foreground">{c.time}</span>
              </div>
              <p className="text-[10px] text-muted-foreground">{c.role} &middot; {c.subject}</p>
              <p className="mt-0.5 truncate text-xs text-muted-foreground">{c.lastMessage}</p>
            </div>
            {c.unread > 0 && (
              <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground">{c.unread}</span>
            )}
          </button>
        ))}
      </div>
    </div>
  )
}
