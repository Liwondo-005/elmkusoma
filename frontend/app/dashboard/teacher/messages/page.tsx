"use client"

import { useState } from "react"
import { MessageSquare, Send, Search, Loader2 } from "lucide-react"
import { useAuth } from "@/lib/auth"

const mockConversations = [
  { id: "1", name: "Admin Office", role: "Admin", lastMessage: "Staff meeting tomorrow at 8am", time: "10m ago", unread: 1 },
  { id: "2", name: "John Parent", role: "Parent", lastMessage: "Thank you for the update on Amina", time: "1h ago", unread: 0 },
  { id: "3", name: "Mary Teacher", role: "Teacher", lastMessage: "Can you cover my 3rd period?", time: "3h ago", unread: 2 },
]

const mockMessages = [
  { id: "1", sender: "Admin Office", text: "Staff meeting tomorrow at 8am. Please confirm attendance.", time: "10:00 AM", isMe: false },
  { id: "2", sender: "Me", text: "I will be there. Thanks for the heads up.", time: "10:05 AM", isMe: true },
]

export default function TeacherMessagesPage() {
  const { user } = useAuth()
  const [selectedConvo, setSelectedConvo] = useState<string | null>(null)
  const [newMessage, setNewMessage] = useState("")
  const [messages, setMessages] = useState(mockMessages)
  const [searchQuery, setSearchQuery] = useState("")

  function handleSend() {
    if (!newMessage.trim()) return
    setMessages((prev) => [...prev, {
      id: String(prev.length + 1),
      sender: "Me",
      text: newMessage.trim(),
      time: new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
      isMe: true,
    }])
    setNewMessage("")
  }

  const filtered = mockConversations.filter((c) =>
    c.name.toLowerCase().includes(searchQuery.toLowerCase())
  )

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Messages</h1>
        <p className="mt-1 text-sm text-muted-foreground">Communicate with parents, students, and colleagues.</p>
      </div>

      <div className="grid gap-4 lg:grid-cols-[320px_1fr]">
        <div className="rounded-2xl border border-border bg-card shadow-xs">
          <div className="border-b border-border p-3">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
              <input
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search conversations..."
                className="w-full rounded-lg border border-border bg-background pl-9 pr-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none"
              />
            </div>
          </div>
          <div className="max-h-[500px] divide-y divide-border overflow-y-auto">
            {filtered.map((convo) => (
              <button
                key={convo.id}
                onClick={() => setSelectedConvo(convo.id)}
                className={`flex w-full items-start gap-3 px-4 py-3 text-left transition-colors hover:bg-muted ${
                  selectedConvo === convo.id ? "bg-muted" : ""
                }`}
              >
                <div className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
                  {convo.name.charAt(0)}
                </div>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between">
                    <p className="text-sm font-medium text-foreground">{convo.name}</p>
                    <span className="text-[10px] text-muted-foreground">{convo.time}</span>
                  </div>
                  <p className="mt-0.5 truncate text-xs text-muted-foreground">{convo.lastMessage}</p>
                </div>
                {convo.unread > 0 && (
                  <span className="flex size-5 shrink-0 items-center justify-center rounded-full bg-primary text-[10px] font-bold text-primary-foreground">
                    {convo.unread}
                  </span>
                )}
              </button>
            ))}
          </div>
        </div>

        <div className="rounded-2xl border border-border bg-card shadow-xs">
          {selectedConvo ? (
            <div className="flex h-[540px] flex-col">
              <div className="border-b border-border px-4 py-3">
                <p className="text-sm font-semibold text-foreground">
                  {mockConversations.find((c) => c.id === selectedConvo)?.name}
                </p>
              </div>
              <div className="flex-1 overflow-y-auto p-4 space-y-3">
                {messages.map((msg) => (
                  <div key={msg.id} className={`flex ${msg.isMe ? "justify-end" : "justify-start"}`}>
                    <div className={`max-w-[70%] rounded-xl px-3 py-2 text-sm ${
                      msg.isMe ? "bg-primary text-primary-foreground" : "bg-muted text-foreground"
                    }`}>
                      <p>{msg.text}</p>
                      <p className={`mt-1 text-[10px] ${msg.isMe ? "text-primary-foreground/70" : "text-muted-foreground"}`}>{msg.time}</p>
                    </div>
                  </div>
                ))}
              </div>
              <div className="border-t border-border p-3">
                <div className="flex gap-2">
                  <input
                    value={newMessage}
                    onChange={(e) => setNewMessage(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && handleSend()}
                    placeholder="Type a message..."
                    className="flex-1 rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:border-primary focus:outline-none"
                  />
                  <button onClick={handleSend} className="rounded-lg bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
                    <Send className="size-4" />
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="flex h-[540px] flex-col items-center justify-center text-center">
              <MessageSquare className="mb-3 size-10 text-muted-foreground" />
              <p className="text-sm font-medium text-foreground">Select a conversation</p>
              <p className="mt-1 text-xs text-muted-foreground">Choose from your conversations on the left.</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
