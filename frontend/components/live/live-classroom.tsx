"use client"

import { useState, useEffect, useRef, useCallback } from "react"
import Link from "next/link"
import {
  ChevronLeft,
  Video,
  VideoOff,
  Mic,
  MicOff,
  MonitorUp,
  MoreHorizontal,
  Send,
  Users,
  Clock,
  ExternalLink,
  Wifi,
  WifiOff,
} from "lucide-react"
import type { LiveClass } from "@/lib/learner-api"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"

interface Participant {
  userId: string
  userName: string
  role: string
  joinedAt: string | null
}

interface ChatMessage {
  userId: string
  userName: string
  message: string
  timestamp: string
  system?: boolean
}

export function LiveClassroom({ liveClass }: { liveClass: LiveClass }) {
  const { user, token } = useAuth()
  const [participants, setParticipants] = useState<Participant[]>([])
  const [chat, setChat] = useState<ChatMessage[]>([])
  const [message, setMessage] = useState("")
  const [connected, setConnected] = useState(false)
  const [reconnecting, setReconnecting] = useState(false)
  const [elapsed, setElapsed] = useState("00:00:00")
  const [joinError, setJoinError] = useState("")
  const wsRef = useRef<WebSocket | null>(null)
  const chatEndRef = useRef<HTMLDivElement>(null)
  const startTimeRef = useRef<Date | null>(null)
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null)
  const retryCountRef = useRef(0)

  const isInProgress = liveClass.status === "IN_PROGRESS"
  const myUserId = user?.id || ""

  const getElapsed = useCallback(() => {
    if (!startTimeRef.current) return "00:00:00"
    const diff = Math.floor((Date.now() - startTimeRef.current.getTime()) / 1000)
    const h = String(Math.floor(diff / 3600)).padStart(2, "0")
    const m = String(Math.floor((diff % 3600) / 60)).padStart(2, "0")
    const s = String(diff % 60).padStart(2, "0")
    return `${h}:${m}:${s}`
  }, [])

  useEffect(() => {
    if (!isInProgress) return
    startTimeRef.current = new Date()
    const interval = setInterval(() => setElapsed(getElapsed()), 1000)
    return () => clearInterval(interval)
  }, [isInProgress, getElapsed])

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [chat])

  useEffect(() => {
    if (!isInProgress || !token || !user) return

    function connect() {
      const protocol = window.location.protocol === "https:" ? "wss:" : "ws:"
      const wsHost = process.env.NEXT_PUBLIC_WS_HOST || window.location.hostname
      const wsPort = process.env.NEXT_PUBLIC_WS_PORT || "8080"
      const wsUrl = `${protocol}//${wsHost}:${wsPort}/ws/live-class/${liveClass.id}?token=${encodeURIComponent(token)}`

      const ws = new WebSocket(wsUrl)
      wsRef.current = ws

      ws.onopen = () => {
        setConnected(true)
        setReconnecting(false)
        setJoinError("")
        retryCountRef.current = 0
        ws.send(JSON.stringify({
          type: "JOIN",
        }))
      }

      ws.onmessage = (event) => {
        const data = JSON.parse(event.data)
        switch (data.type) {
          case "USER_JOINED":
            setParticipants((prev) => {
              if (prev.some((p) => p.userId === data.userId)) return prev
              return [...prev, { userId: data.userId, userName: data.userName, role: "LEARNER", joinedAt: data.timestamp }]
            })
            if (data.userId !== myUserId) {
              setChat((prev) => [...prev, {
                userId: "system",
                userName: "System",
                message: `${data.userName} joined`,
                timestamp: data.timestamp,
                system: true,
              }])
            }
            break
          case "USER_LEFT":
            setParticipants((prev) => prev.filter((p) => p.userId !== data.userId))
            if (data.userId !== myUserId) {
              setChat((prev) => [...prev, {
                userId: "system",
                userName: "System",
                message: `${data.userName} left`,
                timestamp: data.timestamp,
                system: true,
              }])
            }
            break
          case "PARTICIPANTS":
            if (data.participants) setParticipants(data.participants)
            break
          case "CHAT_MESSAGE":
            setChat((prev) => [...prev, { userId: data.userId, userName: data.userName, message: data.message, timestamp: data.timestamp }])
            break
          case "ERROR":
            setJoinError(data.error)
            break
        }
      }

      ws.onclose = () => {
        setConnected(false)
        if (isInProgress && retryCountRef.current < 10) {
          setReconnecting(true)
          const delay = Math.min(3000 * Math.pow(1.5, retryCountRef.current), 30000)
          retryCountRef.current++
          reconnectTimeoutRef.current = setTimeout(connect, delay)
        }
      }

      ws.onerror = () => {
        ws.close()
      }
    }

    connect()

    return () => {
      retryCountRef.current = 10
      if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current)
      if (wsRef.current) {
        if (wsRef.current.readyState === WebSocket.OPEN || wsRef.current.readyState === WebSocket.CONNECTING) {
          wsRef.current.send(JSON.stringify({ type: "LEAVE" }))
          wsRef.current.close()
        }
        wsRef.current = null
      }
    }
  }, [isInProgress, liveClass.id, token, user, myUserId])

  function sendMessage(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = message.trim()
    if (!trimmed || !wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return

    wsRef.current.send(JSON.stringify({ type: "CHAT", message: trimmed }))
    setMessage("")
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      sendMessage(e)
    }
  }

  function handleLeave() {
    retryCountRef.current = 10
    if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current)
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ type: "LEAVE" }))
      wsRef.current.close()
    }
  }

  const formatTime = (d: string) => {
    return new Date(d).toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" })
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <Link
            href="/dashboard/learner/live-classes"
            className="inline-flex items-center gap-1 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
          >
            <ChevronLeft className="size-4" />
            Back to Live Classes
          </Link>
        </div>
        <div className="flex items-center gap-2">
          {liveClass.meetingUrl && (
            <a
              href={liveClass.meetingUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              <ExternalLink className="size-4" />
              Open Meeting
            </a>
          )}
          <Button variant="outline" className="h-9 border-destructive/30 text-destructive hover:bg-destructive/10" onClick={handleLeave}>
            Leave Class
          </Button>
        </div>
      </div>

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <span className={cn(
          "inline-flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-semibold",
          isInProgress ? "bg-teal text-teal-foreground" :
          liveClass.status === "COMPLETED" ? "bg-gray-100 text-gray-600" :
          "bg-blue-100 text-blue-700"
        )}>
          {isInProgress && <span className="size-1.5 animate-pulse rounded-full bg-white" />}
          {liveClass.status}
        </span>
        <div>
          <h1 className="text-lg font-bold text-foreground">{liveClass.title}</h1>
          {liveClass.description && <p className="text-xs text-muted-foreground line-clamp-1">{liveClass.description}</p>}
        </div>
        <div className="ml-auto flex items-center gap-4 text-sm text-muted-foreground">
          {isInProgress && (
            <span className="inline-flex items-center gap-1 font-mono">
              <Clock className="size-4" /> {elapsed}
            </span>
          )}
          <span className="inline-flex items-center gap-1">
            <Users className="size-4 text-teal" /> {participants.length}
          </span>
          <span className="inline-flex items-center gap-1">
            {connected ? (
              <><Wifi className="size-3.5 text-teal" /> <span className="text-xs text-teal">Connected</span></>
            ) : reconnecting ? (
              <><WifiOff className="size-3.5 text-amber-500 animate-pulse" /> <span className="text-xs text-amber-500">Reconnecting...</span></>
            ) : (
              <><WifiOff className="size-3.5 text-muted-foreground" /> <span className="text-xs text-muted-foreground">Disconnected</span></>
            )}
          </span>
        </div>
      </div>

      {joinError && (
        <div className="mb-4 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-700">{joinError}</div>
      )}

      <div className="grid gap-5 lg:grid-cols-[1fr_340px]">
        <div className="flex flex-col gap-5">
          <div className="overflow-hidden rounded-2xl border border-border bg-card shadow-sm">
            <div className="relative aspect-video bg-slate-900 flex items-center justify-center">
              {isInProgress ? (
                <div className="text-center text-white">
                  <Video className="mx-auto size-12 mb-3 opacity-50" />
                  <p className="text-sm opacity-75">Session is live</p>
                  {liveClass.meetingUrl && (
                    <a
                      href={liveClass.meetingUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="mt-3 inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
                    >
                      <ExternalLink className="size-4" />
                      Join Video Session
                    </a>
                  )}
                </div>
              ) : liveClass.status === "COMPLETED" ? (
                <div className="text-center text-white">
                  <p className="text-sm opacity-75">This session has ended</p>
                </div>
              ) : (
                <div className="text-center text-white">
                  <Clock className="mx-auto size-12 mb-3 opacity-50" />
                  <p className="text-sm opacity-75">Session starts at {liveClass.scheduledAt ? formatTime(liveClass.scheduledAt) : "TBD"}</p>
                </div>
              )}

              {isInProgress && (
                <div className="absolute inset-x-0 bottom-0 flex items-center justify-center gap-2 bg-gradient-to-t from-black/70 to-transparent p-4">
                  <ControlButton active={false} label="Camera (coming soon)">
                    <VideoOff className="size-5" />
                  </ControlButton>
                  <ControlButton active={false} label="Microphone (coming soon)">
                    <MicOff className="size-5" />
                  </ControlButton>
                  <ControlButton label="Screen share (coming soon)">
                    <MonitorUp className="size-5" />
                  </ControlButton>
                  <ControlButton label="More options">
                    <MoreHorizontal className="size-5" />
                  </ControlButton>
                </div>
              )}
            </div>
          </div>

          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <h2 className="text-sm font-semibold text-foreground mb-3">Class Details</h2>
            <div className="space-y-2 text-sm text-muted-foreground">
              {liveClass.scheduledAt && <p>Scheduled: {new Date(liveClass.scheduledAt).toLocaleString()}</p>}
              {liveClass.durationMinutes && <p>Duration: {liveClass.durationMinutes} minutes</p>}
              {liveClass.maxParticipants && <p>Max participants: {liveClass.maxParticipants}</p>}
              {liveClass.description && <p className="whitespace-pre-line">{liveClass.description}</p>}
            </div>
          </div>
        </div>

        <div className="flex flex-col gap-5">
          <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-semibold text-foreground">Participants ({participants.length})</h2>
            </div>
            <div className="mt-4 space-y-2 max-h-48 overflow-y-auto">
              {participants.length === 0 ? (
                <p className="text-xs text-muted-foreground">No participants yet</p>
              ) : (
                participants.map((p) => (
                  <div key={p.userId} className="flex items-center gap-2">
                    <span className={cn(
                      "flex size-7 items-center justify-center rounded-full text-xs font-semibold",
                      p.userId === myUserId ? "bg-primary text-primary-foreground" : "bg-accent text-primary"
                    )}>
                      {p.userName?.charAt(0) || "?"}
                    </span>
                    <div className="min-w-0">
                      <p className="text-xs font-medium text-foreground truncate">
                        {p.userName}
                        {p.userId === myUserId && <span className="text-muted-foreground"> (you)</span>}
                      </p>
                      {p.joinedAt && <p className="text-[10px] text-muted-foreground">Joined {formatTime(p.joinedAt)}</p>}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="flex min-h-96 flex-col rounded-2xl border border-border bg-card shadow-sm">
            <div className="flex items-center justify-between border-b border-border px-5 py-3.5">
              <h2 className="text-sm font-semibold text-foreground">Live Chat</h2>
              <span className="inline-flex items-center gap-1">
                {connected && <span className="size-1.5 rounded-full bg-teal animate-pulse" />}
                <span className="text-[10px] text-muted-foreground">
                  {connected ? "Live" : reconnecting ? "Reconnecting..." : "Offline"}
                </span>
              </span>
            </div>
            <div className="flex-1 space-y-3 overflow-y-auto p-5">
              {chat.length === 0 ? (
                <p className="text-center text-xs text-muted-foreground py-8">No messages yet. Start the conversation!</p>
              ) : (
                chat.map((c, i) => (
                  <div key={i} className={cn("flex gap-3", c.system && "justify-center")}>
                    {c.system ? (
                      <span className="text-[10px] text-muted-foreground italic">{c.message}</span>
                    ) : (
                      <>
                        <span className={cn(
                          "flex size-8 shrink-0 items-center justify-center rounded-full text-xs font-semibold",
                          c.userId === myUserId ? "bg-primary text-primary-foreground" : "bg-accent text-primary"
                        )}>
                          {c.userName?.charAt(0) || "?"}
                        </span>
                        <div className="min-w-0">
                          <div className="flex items-baseline gap-2">
                            <span className="text-xs font-semibold text-foreground">{c.userName}</span>
                            <span className="text-[10px] text-muted-foreground">{formatTime(c.timestamp)}</span>
                          </div>
                          <p className="mt-0.5 text-sm text-muted-foreground">{c.message}</p>
                        </div>
                      </>
                    )}
                  </div>
                ))
              )}
              <div ref={chatEndRef} />
            </div>
            <form onSubmit={sendMessage} className="flex items-center gap-2 border-t border-border p-3">
              <input
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder={isInProgress ? "Type a message..." : "Chat available during live session"}
                disabled={!isInProgress || !connected}
                className="h-10 flex-1 rounded-lg border border-border bg-muted/60 px-3 text-sm outline-none placeholder:text-muted-foreground focus:border-ring focus:bg-background disabled:opacity-50"
              />
              <Button type="submit" size="icon" className="size-10 shrink-0" aria-label="Send message" disabled={!isInProgress || !connected || !message.trim()}>
                <Send className="size-4" />
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
      title={label}
    >
      {children}
    </button>
  )
}
