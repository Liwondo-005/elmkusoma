"use client"

import { useState } from "react"
import Link from "next/link"
import Image from "next/image"
import type { LiveClass } from "@/lib/data"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"

const initialChat = [
  { name: "Aisha M.", time: "2:15 PM", text: "Good explanation!" },
  { name: "Brian K.", time: "2:16 PM", text: "Can you explain that step again?" },
  { name: "Neema T.", time: "2:16 PM", text: "This makes sense now, thank you!" },
  { name: "David O.", time: "2:17 PM", text: "So helpful!" },
]

const infoTabs = ["Class Info", "Notes", "Resources", "Q&A"] as const

export function LiveClassroom({ item }: { item: LiveClass }) {
  const [micOn, setMicOn] = useState(false)
  const [camOn, setCamOn] = useState(false)
  const [tab, setTab] = useState<(typeof infoTabs)[number]>("Class Info")
  const [chat, setChat] = useState(initialChat)
  const [message, setMessage] = useState("")

  function sendMessage(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = message.trim()
    if (!trimmed) return
    setChat((prev) => [
      ...prev,
      { name: "You", time: "Now", text: trimmed },
    ])
    setMessage("")
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <Link
            href="/live-classes"
            className="inline-flex items-center gap-1 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
          >
            Back to Live Classes
          </Link>
        </div>
        <Button variant="outline" className="h-9 border-destructive/30 text-destructive hover:bg-destructive/10">
          Leave Class
        </Button>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <span className="inline-flex items-center gap-1.5 rounded-md bg-teal px-2.5 py-1 text-xs font-semibold text-teal-foreground">
          <span className="size-1.5 animate-pulse rounded-full bg-white" />
          LIVE
        </span>
        <div>
          <h1 className="text-lg font-bold text-foreground">
            {item.title} — {item.subtitle}
          </h1>
          <p className="text-xs text-muted-foreground">{item.instructor}</p>
        </div>
        <div className="ml-auto flex items-center gap-4 text-sm text-muted-foreground">
          <span className="inline-flex items-center gap-1 font-mono">
            01:24:35
          </span>
          <span className="inline-flex items-center gap-1">
            {item.watching ?? 128}
          </span>
        </div>
      </div>

      <div className="grid gap-5 lg:grid-cols-[1fr_340px]">
        {/* Stage */}
        <div className="flex flex-col gap-5">
          <div className="overflow-hidden rounded-2xl border border-border bg-card shadow-sm">
            <div className="relative aspect-video bg-slate-900">
              <Image
                src={item.image || "/placeholder.svg"}
                alt={`${item.instructor} teaching ${item.title}`}
                fill
                className="object-cover"
                sizes="(max-width: 1024px) 100vw, 70vw"
                priority
              />
              <span className="absolute left-4 top-4 inline-flex items-center gap-1.5 rounded-md bg-teal px-2.5 py-1 text-xs font-semibold text-teal-foreground">
                <span className="size-1.5 animate-pulse rounded-full bg-white" />
                LIVE
              </span>

              {/* Controls */}
              <div className="absolute inset-x-0 bottom-0 flex items-center justify-center gap-2 bg-gradient-to-t from-black/70 to-transparent p-4">
                <ControlButton
                  active={camOn}
                  onClick={() => setCamOn((v) => !v)}
                  label={camOn ? "Turn off camera" : "Turn on camera"}
                >
                  {camOn ? "Cam On" : "Cam Off"}
                </ControlButton>
                <ControlButton
                  active={micOn}
                  onClick={() => setMicOn((v) => !v)}
                  label={micOn ? "Mute microphone" : "Unmute microphone"}
                >
                  {micOn ? "Mic On" : "Mic Off"}
                </ControlButton>
                <ControlButton label="Share screen">
                  Share
                </ControlButton>
                <ControlButton label="More options">
                  More
                </ControlButton>
              </div>
            </div>
          </div>

          {/* Info tabs */}
          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <div className="flex flex-wrap gap-1 border-b border-border">
              {infoTabs.map((t) => (
                <button
                  key={t}
                  type="button"
                  onClick={() => setTab(t)}
                  className={cn(
                    "relative px-3 py-2.5 text-sm font-medium transition-colors",
                    tab === t ? "text-primary" : "text-muted-foreground hover:text-foreground",
                  )}
                >
                  {t}
                  {tab === t && <span className="absolute inset-x-2 -bottom-px h-0.5 rounded-full bg-primary" />}
                </button>
              ))}
            </div>

            <div className="pt-5">
              {tab === "Class Info" && (
                <>
                  <p className="text-sm leading-relaxed text-muted-foreground">
                    In this session, we&apos;ll continue with integration techniques and their application to
                    real-world problems, including worked examples and a short practice set.
                  </p>
                  <dl className="mt-5 grid grid-cols-2 gap-4 sm:grid-cols-4">
                    <MetaItem label="Level" value={item.level} />
                    <MetaItem label="Duration" value="90 mins" />
                    <MetaItem label="Next Class" value="Tomorrow, 10 AM" />
                    <MetaItem label="Materials" value="5 Files" />
                  </dl>
                </>
              )}
              {tab === "Notes" && (
                <p className="text-sm leading-relaxed text-muted-foreground">
                  Your personal notes for this session will appear here. Start typing during class to keep track of key
                  concepts and formulas.
                </p>
              )}
              {tab === "Resources" && (
                <ul className="space-y-2">
                  {["Lecture Slides.pdf", "Practice Problems.pdf", "Formula Sheet.pdf"].map((r) => (
                    <li
                      key={r}
                      className="flex items-center gap-3 rounded-lg border border-border p-3 text-sm text-foreground"
                    >
                      {r}
                    </li>
                  ))}
                </ul>
              )}
              {tab === "Q&A" && (
                <p className="text-sm leading-relaxed text-muted-foreground">
                  Ask a question and the teacher will answer live. Questions from other students appear here too.
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Sidebar */}
        <div className="flex flex-col gap-5">
          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-foreground">Participants (128)</h2>
              <button type="button" className="text-xs font-medium text-primary hover:underline">
                View All
              </button>
            </div>
            <div className="mt-4 flex flex-wrap items-center gap-1.5">
              {Array.from({ length: 7 }).map((_, i) => (
                <span
                  key={i}
                  className="flex size-8 items-center justify-center rounded-full border border-border bg-accent text-primary"
                >
                  {i + 1}
                </span>
              ))}
              <span className="flex h-8 items-center justify-center rounded-full bg-muted px-2.5 text-xs font-semibold text-muted-foreground">
                +121
              </span>
            </div>
          </div>

          <div className="flex min-h-96 flex-col rounded-2xl border border-border bg-card shadow-sm">
            <div className="flex items-center justify-between border-b border-border px-5 py-3.5">
              <h2 className="text-sm font-semibold text-foreground">Live Chat</h2>
              <span className="text-xs text-muted-foreground">···</span>
            </div>
            <div className="flex-1 space-y-4 overflow-y-auto p-5">
              {chat.map((c, i) => (
                <div key={i} className="flex gap-3">
                  <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-accent text-xs font-semibold text-primary">
                    {c.name.charAt(0)}
                  </span>
                  <div className="min-w-0">
                    <div className="flex items-baseline gap-2">
                      <span className="text-xs font-semibold text-foreground">{c.name}</span>
                      <span className="text-[10px] text-muted-foreground">{c.time}</span>
                    </div>
                    <p className="mt-0.5 text-sm text-muted-foreground">{c.text}</p>
                  </div>
                </div>
              ))}
            </div>
            <form onSubmit={sendMessage} className="flex items-center gap-2 border-t border-border p-3">
              <input
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                placeholder="Type a message..."
                className="h-10 flex-1 rounded-lg border border-border bg-muted/60 px-3 text-sm outline-none placeholder:text-muted-foreground focus:border-ring focus:bg-background"
              />
              <Button type="submit" size="icon" className="size-10 shrink-0" aria-label="Send message">
                Send
              </Button>
            </form>
          </div>
        </div>
      </div>
    </div>
  )
}

function ControlButton({
  children,
  label,
  active,
  onClick,
}: {
  children: React.ReactNode
  label: string
  active?: boolean
  onClick?: () => void
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-label={label}
      aria-pressed={active}
      className={cn(
        "flex size-11 items-center justify-center rounded-full border text-white transition-colors",
        active
          ? "border-transparent bg-primary hover:bg-primary/90"
          : "border-white/20 bg-white/10 hover:bg-white/20",
      )}
    >
      {children}
    </button>
  )
}

function MetaItem({
  label,
  value,
}: {
  label: string
  value: string
}) {
  return (
    <div className="flex items-start gap-2">
      <div>
        <dt className="text-xs text-muted-foreground">{label}</dt>
        <dd className="text-sm font-medium text-foreground">{value}</dd>
      </div>
    </div>
  )
}
