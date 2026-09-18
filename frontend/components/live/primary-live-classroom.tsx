"use client"

import { useState, useEffect, useRef, useCallback } from "react"
import Link from "next/link"
import {
  ArrowLeft,
  Send,
  Users,
  Wifi,
  WifiOff,
  Crown,
  LogOut,
  MessageCircle,
  Sparkles,
} from "lucide-react"
import type { LiveClass } from "@/lib/learner-api"
import { useAuth } from "@/lib/auth"
import { cn } from "@/lib/utils"
import { LiveInteractivePanel } from "./live-interactive-panel"
import { ActivityCreator } from "./activity-creator"

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

export function PrimaryLiveClassroom({ liveClass }: { liveClass: LiveClass }) {
  const { user, token } = useAuth()
  const [participants, setParticipants] = useState<Participant[]>([])
  const [chat, setChat] = useState<ChatMessage[]>([])
  const [message, setMessage] = useState("")
  const [connected, setConnected] = useState(false)
  const [joinError, setJoinError] = useState("")
  const [hasJoined, setHasJoined] = useState(false)
  const wsRef = useRef<WebSocket | null>(null)
  const chatEndRef = useRef<HTMLDivElement>(null)
  const retryCountRef = useRef(0)

  const myUserId = user?.id || ""
  const isTeacher = user?.role === "Teacher" || user?.role === "Instructor"
  const teacherName = liveClass.teacherName || "Your Teacher"
  const isInProgress = liveClass.status === "IN_PROGRESS" || liveClass.status === "LIVE"
  const [showActivities, setShowActivities] = useState(false)

  useEffect(() => {
    chatEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [chat])

  useEffect(() => {
    if (!hasJoined || !isInProgress || !token || !user) return

    function connect() {
      const protocol = window.location.protocol === "https:" ? "wss:" : "ws:"
      const wsHost = process.env.NEXT_PUBLIC_WS_HOST || window.location.hostname
      const wsPort = process.env.NEXT_PUBLIC_WS_PORT || "8080"
      const wsUrl = `${protocol}//${wsHost}:${wsPort}/ws/live-class/${liveClass.id}?token=${encodeURIComponent(token ?? "")}`

      const ws = new WebSocket(wsUrl)
      wsRef.current = ws

      ws.onopen = () => {
        setConnected(true)
        setJoinError("")
        retryCountRef.current = 0
        ws.send(JSON.stringify({ type: "JOIN" }))

        fetch(`/v1/live-session/join/${liveClass.id}`, {
          method: "POST",
          headers: {
            Authorization: `Bearer ${token}`,
            "X-Institution-Id": user?.institutionId || "",
            "Content-Type": "application/json",
          },
        }).catch(() => {})
      }

      ws.onmessage = (event) => {
        const data = JSON.parse(event.data)
        switch (data.type) {
          case "USER_JOINED":
            setParticipants((prev) => {
              if (prev.some((p) => p.userId === data.userId)) return prev
              return [
                ...prev,
                {
                  userId: data.userId,
                  userName: data.userName,
                  role: "LEARNER",
                  joinedAt: data.timestamp,
                },
              ]
            })
            if (data.userId !== myUserId) {
              setChat((prev) => [
                ...prev,
                {
                  userId: "system",
                  userName: "System",
                  message: `${data.userName} joined`,
                  timestamp: data.timestamp,
                  system: true,
                },
              ])
            }
            break
          case "USER_LEFT":
            setParticipants((prev) => prev.filter((p) => p.userId !== data.userId))
            if (data.userId !== myUserId) {
              setChat((prev) => [
                ...prev,
                {
                  userId: "system",
                  userName: "System",
                  message: `${data.userName} left`,
                  timestamp: data.timestamp,
                  system: true,
                },
              ])
            }
            break
          case "PARTICIPANTS":
            if (data.participants) setParticipants(data.participants)
            break
          case "CHAT_MESSAGE":
            setChat((prev) => [
              ...prev,
              {
                userId: data.userId,
                userName: data.userName,
                message: data.message,
                timestamp: data.timestamp,
              },
            ])
            break
          case "CHAT_HISTORY":
            if (data.messages) {
              const history: ChatMessage[] = data.messages.map((m: any) => ({
                userId: m.userId,
                userName: m.userName,
                message: m.message,
                timestamp: m.timestamp,
              }))
              setChat((prev) => [...history, ...prev])
            }
            break
        }
      }

      ws.onclose = () => {
        setConnected(false)
        if (hasJoined && isInProgress && retryCountRef.current < 10) {
          const delay = Math.min(3000 * Math.pow(1.5, retryCountRef.current), 30000)
          retryCountRef.current++
          setTimeout(connect, delay)
        }
      }

      ws.onerror = () => {
        setJoinError("Could not connect to the class. Please try again.")
      }
    }

    connect()

    return () => {
      if (wsRef.current) {
        wsRef.current.close()
        wsRef.current = null
      }
    }
  }, [hasJoined, isInProgress, token, user, liveClass.id, myUserId])

  const sendMessage = useCallback(() => {
    if (!message.trim() || !wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return
    wsRef.current.send(
      JSON.stringify({
        type: "CHAT",
        message: message.trim(),
      })
    )
    setMessage("")
  }, [message])

  const handleLeave = useCallback(() => {
    if (wsRef.current) {
      wsRef.current.send(JSON.stringify({ type: "LEAVE" }))
      wsRef.current.close()
    }
    setHasJoined(false)
    setConnected(false)
    setParticipants([])
    setChat([])
  }, [])

  const handleJoin = useCallback(() => {
    setHasJoined(true)
  }, [])

  if (!hasJoined) {
    return (
      <div className="flex min-h-[80vh] items-center justify-center px-4">
        <div className="w-full max-w-md space-y-6 text-center">
          <div className="mx-auto flex size-20 items-center justify-center rounded-3xl bg-primary/10">
            <Users className="size-10 text-primary" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-foreground">{liveClass.title || "Live Class"}</h1>
            {liveClass.subjectName && (
              <p className="mt-1 text-sm font-medium text-primary">{liveClass.subjectName}</p>
            )}
            <p className="mt-2 text-sm text-muted-foreground">
              Teacher: {teacherName}
            </p>
            {liveClass.durationMinutes && (
              <p className="mt-1 text-xs text-muted-foreground">
                {liveClass.durationMinutes} minutes
              </p>
            )}
          </div>

          {!isInProgress ? (
            <div className="rounded-2xl border border-border bg-card p-6">
              <p className="text-sm text-muted-foreground">
                This class has not started yet. Please wait for your teacher to begin.
              </p>
              <Link
                href="/dashboard/live-classes"
                className="mt-4 inline-flex items-center gap-2 text-sm font-medium text-primary hover:underline"
              >
                <ArrowLeft className="size-4" />
                Go back
              </Link>
            </div>
          ) : (
            <button
              onClick={handleJoin}
              className="w-full rounded-2xl bg-primary px-8 py-4 text-lg font-bold text-primary-foreground shadow-lg transition-all hover:shadow-xl active:scale-[0.98]"
            >
              Join Class!
            </button>
          )}

          {joinError && (
            <p className="text-sm text-destructive">{joinError}</p>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-[calc(100vh-8rem)] flex-col">
      <div className="flex items-center justify-between border-b border-border px-4 py-3">
        <div className="flex items-center gap-3">
          <div
            className={cn(
              "flex size-3 rounded-full",
              connected ? "bg-green-500" : "bg-yellow-500 animate-pulse"
            )}
          />
          <span className="text-sm font-medium text-foreground">
            {connected ? "Connected" : "Connecting..."}
          </span>
        </div>
        <div className="flex items-center gap-4">
          <span className="flex items-center gap-1.5 text-sm text-muted-foreground">
            <Users className="size-4" />
            {participants.length}
          </span>
          <button
            onClick={() => setShowActivities(!showActivities)}
            className={cn(
              "flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-bold transition-colors",
              showActivities
                ? "bg-primary text-primary-foreground"
                : "bg-primary/10 text-primary hover:bg-primary/20"
            )}
          >
            <Sparkles className="size-4" />
            Activities
          </button>
          <button
            onClick={handleLeave}
            className="flex items-center gap-2 rounded-xl bg-destructive/10 px-4 py-2 text-sm font-bold text-destructive transition-colors hover:bg-destructive/20"
          >
            <LogOut className="size-4" />
            Leave Class
          </button>
        </div>
      </div>

      <div className="flex flex-1 overflow-hidden">
        <div className="flex flex-1 flex-col">
          <div className="flex-1 overflow-y-auto p-4">
            <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-4">
              {participants.map((p) => (
                <div
                  key={p.userId}
                  className="relative flex aspect-video flex-col items-center justify-center rounded-2xl border border-border bg-muted/50"
                >
                  <div className="flex size-12 items-center justify-center rounded-full bg-primary/10 text-lg font-bold text-primary">
                    {p.userName?.charAt(0)?.toUpperCase() || "?"}
                  </div>
                  <div className="mt-2 flex items-center gap-1">
                    {p.role === "TEACHER" && (
                      <Crown className="size-3.5 text-yellow-500" />
                    )}
                    <span className="max-w-[120px] truncate text-xs font-medium text-foreground">
                      {p.userId === myUserId ? "You" : p.userName}
                    </span>
                  </div>
                  {p.role === "TEACHER" && (
                    <span className="mt-0.5 text-[10px] font-semibold text-yellow-600">
                      Teacher
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>

          <div className="border-t border-border bg-card p-4">
            <div className="flex items-center gap-3">
              <input
                type="text"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && sendMessage()}
                placeholder="Type a message..."
                className="flex-1 rounded-xl border border-border bg-background px-4 py-3 text-sm outline-none focus:border-primary"
                disabled={!connected}
              />
              <button
                onClick={sendMessage}
                disabled={!message.trim() || !connected}
                className="flex size-12 items-center justify-center rounded-xl bg-primary text-primary-foreground transition-colors hover:bg-primary/90 disabled:opacity-50"
              >
                <Send className="size-5" />
              </button>
            </div>
          </div>
        </div>

        <div className="hidden w-80 border-l border-border bg-card lg:flex lg:flex-col">
          {showActivities ? (
            <LiveInteractivePanel
              liveClassId={liveClass.id}
              isTeacher={isTeacher}
              isOpen={showActivities}
              onClose={() => setShowActivities(false)}
            />
          ) : (
            <>
              <div className="border-b border-border px-4 py-3">
                <h3 className="text-sm font-semibold text-foreground">
                  Chat
                </h3>
              </div>
              <div className="flex-1 overflow-y-auto px-4 py-2">
                {chat.length === 0 ? (
                  <div className="flex flex-col items-center py-8 text-center">
                    <MessageCircle className="size-8 text-muted-foreground/40" />
                    <p className="mt-2 text-xs text-muted-foreground">
                      No messages yet. Say hello!
                    </p>
                  </div>
                ) : (
                  <div className="space-y-3">
                    {chat.map((msg, i) =>
                      msg.system ? (
                        <div key={i} className="text-center text-xs text-muted-foreground">
                          {msg.message}
                        </div>
                      ) : (
                        <div key={i}>
                          <span className="text-xs font-semibold text-foreground">
                            {msg.userId === myUserId ? "You" : msg.userName}
                          </span>
                          <p className="mt-0.5 text-sm text-muted-foreground">
                            {msg.message}
                          </p>
                        </div>
                      )
                    )}
                    <div ref={chatEndRef} />
                  </div>
                )}
              </div>
            </>
          )}
          {isTeacher && showActivities && (
            <div className="border-t border-border p-4">
              <ActivityCreator
                liveClassId={liveClass.id}
                onActivityCreated={() => {}}
              />
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
