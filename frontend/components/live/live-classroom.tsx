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
  Send,
  Users,
  Clock,
  ExternalLink,
  Wifi,
  WifiOff,
  Hand,
  XCircle,
  Flag,
} from "lucide-react"
import type { LiveClass } from "@/lib/learner-api"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"
import { Room, RoomEvent, Track, Participant as LKParticipant, TrackPublication } from "livekit-client"

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
  const [handRaised, setHandRaised] = useState(false)
  const [screenSharing, setScreenSharing] = useState(false)
  const [cameraEnabled, setCameraEnabled] = useState(false)
  const [micEnabled, setMicEnabled] = useState(false)
  const [liveKitAvailable, setLiveKitAvailable] = useState(false)
  const [videoTracks, setVideoTracks] = useState<Map<string, MediaStream>>(new Map())
  const [localStream, setLocalStream] = useState<MediaStream | null>(null)
  const [screenStream, setScreenStream] = useState<MediaStream | null>(null)
  const [serviceMode, setServiceMode] = useState<"full" | "chat-only" | "unknown">("unknown")
  const [liveKitToken, setLiveKitToken] = useState<string | null>(null)
  const [liveKitUrl, setLiveKitUrl] = useState<string | null>(null)
  const [roomName, setRoomName] = useState<string | null>(null)
  const [remoteParticipants, setRemoteParticipants] = useState<Map<string, LKParticipant>>(new Map())
  const [remoteVideoTrack, setRemoteVideoTrack] = useState<TrackPublication | null>(null)
  const [remoteAudioTrack, setRemoteAudioTrack] = useState<TrackPublication | null>(null)
  const [showIssueModal, setShowIssueModal] = useState(false)
  const [issueType, setIssueType] = useState("CONNECTION_PROBLEM")
  const [issueDescription, setIssueDescription] = useState("")
  const [issueSubmitting, setIssueSubmitting] = useState(false)
  const [issueSent, setIssueSent] = useState(false)
  const roomRef = useRef<Room | null>(null)
  const remoteVideoRef = useRef<HTMLVideoElement>(null)
  const remoteAudioRef = useRef<HTMLAudioElement>(null)
  const wsRef = useRef<WebSocket | null>(null)
  const chatEndRef = useRef<HTMLDivElement>(null)
  const startTimeRef = useRef<Date | null>(null)
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null)
  const retryCountRef = useRef(0)
  const heartbeatRef = useRef<NodeJS.Timeout | null>(null)
  const localVideoRef = useRef<HTMLVideoElement>(null)
  const screenVideoRef = useRef<HTMLVideoElement>(null)

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
    if (!isInProgress || !liveKitToken || !liveKitUrl || serviceMode !== "full") return

    const room = new Room({
      adaptiveStream: true,
      dynacast: true,
    })
    roomRef.current = room

    room.on(RoomEvent.Connected, () => {
      setConnected(true)
      setReconnecting(false)
      retryCountRef.current = 0
    })

    room.on(RoomEvent.Disconnected, () => {
      if (isInProgress && retryCountRef.current < 10) {
        setReconnecting(true)
        const delay = Math.min(3000 * Math.pow(1.5, retryCountRef.current), 30000)
        retryCountRef.current++
        setTimeout(() => {
          if (roomRef.current && liveKitToken && liveKitUrl) {
            roomRef.current.connect(liveKitUrl, liveKitToken).catch(() => {})
          }
        }, delay)
      }
    })

    room.on(RoomEvent.ParticipantConnected, (participant: LKParticipant) => {
      setRemoteParticipants(prev => new Map(prev).set(participant.identity, participant))
      participant.on(RoomEvent.TrackSubscribed, (pub: TrackPublication) => {
        if (pub.kind === Track.Kind.Video) setRemoteVideoTrack(pub)
        if (pub.kind === Track.Kind.Audio) setRemoteAudioTrack(pub)
      })
    })

    room.on(RoomEvent.ParticipantDisconnected, (participant: LKParticipant) => {
      setRemoteParticipants(prev => {
        const next = new Map(prev)
        next.delete(participant.identity)
        return next
      })
    })

    room.on(RoomEvent.TrackSubscribed, (pub: TrackPublication, track: Track, participant: LKParticipant) => {
      if (pub.kind === Track.Kind.Video) setRemoteVideoTrack(pub)
      if (pub.kind === Track.Kind.Audio) setRemoteAudioTrack(pub)
    })

    room.connect(liveKitUrl, liveKitToken).catch(err => {
      console.error("LiveKit connection failed:", err)
      setServiceMode("chat-only")
    })

    return () => {
      room.disconnect()
      roomRef.current = null
    }
  }, [isInProgress, liveKitToken, liveKitUrl, serviceMode])

  useEffect(() => {
    if (!isInProgress) return
    heartbeatRef.current = setInterval(() => {
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify({ type: "HEARTBEAT" }))
      }
    }, 30000)
    return () => {
      if (heartbeatRef.current) clearInterval(heartbeatRef.current)
    }
  }, [isInProgress])

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
        ws.send(JSON.stringify({ type: "JOIN" }))

        fetch(`/v1/live-session/join/${liveClass.id}`, {
          method: "POST",
          headers: {
            "Authorization": `Bearer ${token}`,
            "X-Institution-Id": user?.institutionId || "",
            "Content-Type": "application/json",
          },
        }).then(r => r.json()).then(data => {
          if (data?.data?.liveKitAvailable) {
            setServiceMode("full")
            setLiveKitToken(data.data.liveKitToken)
            setLiveKitUrl(data.data.liveKitUrl)
            setRoomName(data.data.roomName)
          } else {
            setServiceMode("chat-only")
          }
        }).catch(() => {
          setServiceMode("chat-only")
        })
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
          case "HAND_RAISED":
            setChat((prev) => [...prev, {
              userId: "system",
              userName: "System",
              message: `${data.userName} raised their hand`,
              timestamp: data.timestamp,
              system: true,
            }])
            break
          case "HAND_LOWERED":
            setChat((prev) => [...prev, {
              userId: "system",
              userName: "System",
              message: `${data.userName} lowered their hand`,
              timestamp: data.timestamp,
              system: true,
            }])
            break
          case "SCREEN_SHARE_STARTED":
            setChat((prev) => [...prev, {
              userId: "system",
              userName: "System",
              message: `${data.userName} started screen sharing`,
              timestamp: data.timestamp,
              system: true,
            }])
            break
          case "SCREEN_SHARE_STOPPED":
            setChat((prev) => [...prev, {
              userId: "system",
              userName: "System",
              message: `${data.userName} stopped screen sharing`,
              timestamp: data.timestamp,
              system: true,
            }])
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
      if (heartbeatRef.current) clearInterval(heartbeatRef.current)
      if (roomRef.current) {
        roomRef.current.disconnect()
        roomRef.current = null
      }
      if (wsRef.current) {
        if (wsRef.current.readyState === WebSocket.OPEN || wsRef.current.readyState === WebSocket.CONNECTING) {
          wsRef.current.send(JSON.stringify({ type: "LEAVE" }))
          wsRef.current.close()
        }
        wsRef.current = null
      }
    }
  }, [isInProgress, liveClass.id, token, user, myUserId])

  async function toggleCamera() {
    if (cameraEnabled) {
      if (localStream) {
        localStream.getTracks().forEach((t) => t.stop())
        setLocalStream(null)
      }
      setCameraEnabled(false)
      setVideoTracks((prev) => {
        const next = new Map(prev)
        next.delete("local-camera")
        return next
      })
    } else {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: true, audio: false })
        setLocalStream(stream)
        setCameraEnabled(true)
        setVideoTracks((prev) => new Map(prev).set("local-camera", stream))
        if (localVideoRef.current) {
          localVideoRef.current.srcObject = stream
        }
      } catch (err) {
        setJoinError("Could not access camera. Please check permissions.")
      }
    }
  }

  async function toggleMic() {
    if (micEnabled) {
      if (localStream) {
        localStream.getAudioTracks().forEach((t) => { t.enabled = false })
      }
      setMicEnabled(false)
    } else {
      try {
        const stream = localStream || await navigator.mediaDevices.getUserMedia({ video: false, audio: true })
        if (!localStream) setLocalStream(stream)
        stream.getAudioTracks().forEach((t) => { t.enabled = true })
        setMicEnabled(true)
      } catch (err) {
        setJoinError("Could not access microphone. Please check permissions.")
      }
    }
  }

  async function toggleScreenShare() {
    if (screenSharing) {
      if (screenStream) {
        screenStream.getTracks().forEach((t) => t.stop())
        setScreenStream(null)
      }
      setScreenSharing(false)
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        wsRef.current.send(JSON.stringify({ type: "SCREEN_SHARE_STOP" }))
      }
    } else {
      try {
        const stream = await navigator.mediaDevices.getDisplayMedia({ video: true })
        setScreenStream(stream)
        setScreenSharing(true)
        setVideoTracks((prev) => new Map(prev).set("local-screen", stream))
        if (screenVideoRef.current) {
          screenVideoRef.current.srcObject = stream
        }
        stream.getVideoTracks()[0].onended = () => {
          setScreenSharing(false)
          setScreenStream(null)
          setVideoTracks((prev) => {
            const next = new Map(prev)
            next.delete("local-screen")
            return next
          })
          if (wsRef.current?.readyState === WebSocket.OPEN) {
            wsRef.current.send(JSON.stringify({ type: "SCREEN_SHARE_STOP" }))
          }
        }
        if (wsRef.current?.readyState === WebSocket.OPEN) {
          wsRef.current.send(JSON.stringify({ type: "SCREEN_SHARE_START" }))
        }
      } catch (err) {
        setJoinError("Could not share screen. Please check permissions.")
      }
    }
  }

  function toggleHand() {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return
    if (handRaised) {
      wsRef.current.send(JSON.stringify({ type: "LOWER_HAND" }))
    } else {
      wsRef.current.send(JSON.stringify({ type: "RAISE_HAND" }))
    }
    setHandRaised(!handRaised)
  }

  function sendChatMessage(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = message.trim()
    if (!trimmed || !wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return
    wsRef.current.send(JSON.stringify({ type: "CHAT", message: trimmed }))
    setMessage("")
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      sendChatMessage(e)
    }
  }

  function handleLeave() {
    retryCountRef.current = 10
    if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current)
    if (heartbeatRef.current) clearInterval(heartbeatRef.current)
    if (roomRef.current) {
      roomRef.current.disconnect()
      roomRef.current = null
    }
    if (localStream) {
      localStream.getTracks().forEach((t) => t.stop())
      setLocalStream(null)
    }
    if (screenStream) {
      screenStream.getTracks().forEach((t) => t.stop())
      setScreenStream(null)
    }
    if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ type: "LEAVE" }))
      wsRef.current.close()
    }
  }

  const formatTime = (d: string) => {
    return new Date(d).toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" })
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-4 sm:px-6 lg:px-8">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <Link
            href="/dashboard/learner/live-classes"
            className="inline-flex items-center gap-1 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
          >
            <ChevronLeft className="size-4" />
            Back
          </Link>
        </div>
        <div className="flex items-center gap-2">
          {liveClass.meetingUrl && (
            <a
              href={liveClass.meetingUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90"
            >
              <ExternalLink className="size-3" />
              External Tool
            </a>
          )}
          <Button variant="outline" size="sm" className="border-destructive/30 text-destructive hover:bg-destructive/10" onClick={handleLeave}>
            Leave
          </Button>
        </div>
      </div>

      <div className="mb-3 flex flex-wrap items-center gap-3">
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
          <h1 className="text-base font-bold text-foreground">{liveClass.title}</h1>
          {liveClass.description && <p className="text-xs text-muted-foreground line-clamp-1">{liveClass.description}</p>}
        </div>
        <div className="ml-auto flex items-center gap-3 text-xs text-muted-foreground">
          {isInProgress && (
            <span className="inline-flex items-center gap-1 font-mono">
              <Clock className="size-3.5" /> {elapsed}
            </span>
          )}
          <span className="inline-flex items-center gap-1">
            <Users className="size-3.5 text-teal" /> {participants.length}
          </span>
          <span className="inline-flex items-center gap-1">
            {connected ? (
              <><Wifi className="size-3 text-teal" /> <span className="text-teal">Connected</span></>
            ) : reconnecting ? (
              <><WifiOff className="size-3 text-amber-500 animate-pulse" /> <span className="text-amber-500">Reconnecting...</span></>
            ) : (
              <><WifiOff className="size-3 text-muted-foreground" /> <span className="text-muted-foreground">Offline</span></>
            )}
          </span>
        </div>
      </div>

      {joinError && (
        <div className="mb-3 rounded-lg border border-red-200 bg-red-50 p-2 text-xs text-red-700 flex items-center justify-between">
          <span>{joinError}</span>
          <button onClick={() => setJoinError("")} className="text-red-500 hover:text-red-700"><XCircle className="size-3.5" /></button>
        </div>
      )}

      {serviceMode === "chat-only" && isInProgress && (
        <div className="mb-3 rounded-lg border border-amber-200 bg-amber-50 p-2.5 text-xs text-amber-700">
          <p className="font-medium">Video service unavailable</p>
          <p className="mt-0.5 text-amber-600">Live video is temporarily unavailable. You can still participate via chat. The teacher has been notified.</p>
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <div className="flex flex-col gap-4">
          <div className="overflow-hidden rounded-xl border border-border bg-card shadow-sm">
            <div className="relative aspect-video bg-slate-900 flex items-center justify-center">
              {isInProgress ? (
                <>
                  {remoteVideoTrack ? (
                    <video
                      ref={el => {
                        if (el && remoteVideoTrack.videoTrack) {
                          el.srcObject = new MediaStream([remoteVideoTrack.videoTrack.mediaStreamTrack!])
                        }
                      }}
                      autoPlay
                      playsInline
                      className="absolute inset-0 w-full h-full object-contain"
                    />
                  ) : screenStream ? (
                    <video
                      ref={screenVideoRef}
                      autoPlay
                      playsInline
                      muted
                      className="absolute inset-0 w-full h-full object-contain"
                    />
                  ) : localStream && cameraEnabled ? (
                    <video
                      ref={localVideoRef}
                      autoPlay
                      playsInline
                      muted
                      className="absolute inset-0 w-full h-full object-cover"
                    />
                  ) : (
                    <div className="text-center text-white">
                      <Video className="mx-auto size-10 mb-2 opacity-40" />
                      <p className="text-xs opacity-60">Camera off</p>
                    </div>
                  )}

                  {remoteAudioTrack && (
                    <audio
                      ref={el => {
                        if (el && remoteAudioTrack.audioTrack) {
                          el.srcObject = new MediaStream([remoteAudioTrack.audioTrack.mediaStreamTrack!])
                        }
                      }}
                      autoPlay
                    />
                  )}

                  {screenStream && localStream && cameraEnabled && (
                    <div className="absolute bottom-2 right-2 w-40 aspect-video rounded-lg overflow-hidden border-2 border-white/20">
                      <video
                        ref={localVideoRef}
                        autoPlay
                        playsInline
                        muted
                        className="w-full h-full object-cover"
                      />
                    </div>
                  )}

                  <div className="absolute inset-x-0 bottom-0 flex items-center justify-center gap-1.5 bg-gradient-to-t from-black/70 to-transparent p-3">
                    <ControlButton active={cameraEnabled} onClick={toggleCamera} label={cameraEnabled ? "Turn off camera" : "Turn on camera"}>
                      {cameraEnabled ? <Video className="size-4" /> : <VideoOff className="size-4" />}
                    </ControlButton>
                    <ControlButton active={micEnabled} onClick={toggleMic} label={micEnabled ? "Mute" : "Unmute"}>
                      {micEnabled ? <Mic className="size-4" /> : <MicOff className="size-4" />}
                    </ControlButton>
                    <ControlButton active={screenSharing} onClick={toggleScreenShare} label={screenSharing ? "Stop sharing" : "Share screen"}>
                      <MonitorUp className="size-4" />
                    </ControlButton>
                    <ControlButton active={handRaised} onClick={toggleHand} label={handRaised ? "Lower hand" : "Raise hand"}>
                      <Hand className="size-4" />
                    </ControlButton>
                    <button
                      type="button"
                      onClick={() => setShowIssueModal(true)}
                      aria-label="Report issue"
                      className="flex size-10 items-center justify-center rounded-full border border-white/20 bg-red-500/20 text-white hover:bg-red-500/30 transition-colors"
                      title="Report an issue"
                    >
                      <Flag className="size-4" />
                    </button>
                  </div>
                </>
              ) : liveClass.status === "COMPLETED" ? (
                <div className="text-center text-white">
                  <p className="text-sm opacity-75">This session has ended</p>
                </div>
              ) : (
                <div className="text-center text-white">
                  <Clock className="mx-auto size-10 mb-2 opacity-40" />
                  <p className="text-xs opacity-60">Starts at {liveClass.scheduledAt ? formatTime(liveClass.scheduledAt) : "TBD"}</p>
                </div>
              )}
            </div>
          </div>

          <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
            <h2 className="text-xs font-semibold text-foreground mb-2">Class Details</h2>
            <div className="space-y-1 text-xs text-muted-foreground">
              {liveClass.scheduledAt && <p>Scheduled: {new Date(liveClass.scheduledAt).toLocaleString()}</p>}
              {liveClass.durationMinutes && <p>Duration: {liveClass.durationMinutes} min</p>}
              {liveClass.maxParticipants && <p>Max participants: {liveClass.maxParticipants}</p>}
              {liveClass.description && <p className="whitespace-pre-line line-clamp-3">{liveClass.description}</p>}
            </div>
          </div>
        </div>

        <div className="flex flex-col gap-4">
          <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-xs font-semibold text-foreground">Participants ({participants.length})</h2>
            </div>
            <div className="mt-3 space-y-1.5 max-h-36 overflow-y-auto">
              {participants.length === 0 ? (
                <p className="text-[10px] text-muted-foreground">No participants yet</p>
              ) : (
                participants.map((p) => (
                  <div key={p.userId} className="flex items-center gap-2">
                    <span className={cn(
                      "flex size-6 items-center justify-center rounded-full text-[10px] font-semibold",
                      p.userId === myUserId ? "bg-primary text-primary-foreground" : "bg-accent text-primary"
                    )}>
                      {p.userName?.charAt(0) || "?"}
                    </span>
                    <div className="min-w-0">
                      <p className="text-[11px] font-medium text-foreground truncate">
                        {p.userName}
                        {p.userId === myUserId && <span className="text-muted-foreground"> (you)</span>}
                      </p>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          <div className="flex min-h-80 flex-col rounded-xl border border-border bg-card shadow-sm">
            <div className="flex items-center justify-between border-b border-border px-4 py-2.5">
              <h2 className="text-xs font-semibold text-foreground">Live Chat</h2>
              <span className="inline-flex items-center gap-1">
                {connected && <span className="size-1.5 rounded-full bg-teal animate-pulse" />}
                <span className="text-[10px] text-muted-foreground">
                  {connected ? "Live" : reconnecting ? "Reconnecting..." : "Offline"}
                </span>
              </span>
            </div>
            <div className="flex-1 space-y-2 overflow-y-auto p-4">
              {chat.length === 0 ? (
                <p className="text-center text-[11px] text-muted-foreground py-6">No messages yet</p>
              ) : (
                chat.map((c, i) => (
                  <div key={i} className={cn("flex gap-2", c.system && "justify-center")}>
                    {c.system ? (
                      <span className="text-[10px] text-muted-foreground italic">{c.message}</span>
                    ) : (
                      <>
                        <span className={cn(
                          "flex size-7 shrink-0 items-center justify-center rounded-full text-[10px] font-semibold",
                          c.userId === myUserId ? "bg-primary text-primary-foreground" : "bg-accent text-primary"
                        )}>
                          {c.userName?.charAt(0) || "?"}
                        </span>
                        <div className="min-w-0">
                          <div className="flex items-baseline gap-1.5">
                            <span className="text-[11px] font-semibold text-foreground">{c.userName}</span>
                            <span className="text-[9px] text-muted-foreground">{formatTime(c.timestamp)}</span>
                          </div>
                          <p className="mt-0.5 text-xs text-muted-foreground">{c.message}</p>
                        </div>
                      </>
                    )}
                  </div>
                ))
              )}
              <div ref={chatEndRef} />
            </div>
            <form onSubmit={sendChatMessage} className="flex items-center gap-1.5 border-t border-border p-2.5">
              <input
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder={isInProgress ? "Type a message..." : "Chat during live session"}
                disabled={!isInProgress || !connected}
                className="h-9 flex-1 rounded-lg border border-border bg-muted/60 px-3 text-xs outline-none placeholder:text-muted-foreground focus:border-ring focus:bg-background disabled:opacity-50"
              />
              <Button type="submit" size="icon" className="size-9 shrink-0" aria-label="Send" disabled={!isInProgress || !connected || !message.trim()}>
                <Send className="size-3.5" />
              </Button>
            </form>
          </div>
        </div>
      </div>
      {showIssueModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4" onClick={() => setShowIssueModal(false)}>
          <div className="w-full max-w-md rounded-xl bg-card p-5 shadow-xl border border-border" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-sm font-semibold text-foreground flex items-center gap-2">
                <Flag className="size-4 text-destructive" /> Report an Issue
              </h3>
              <button onClick={() => setShowIssueModal(false)} className="text-muted-foreground hover:text-foreground">
                <XCircle className="size-4" />
              </button>
            </div>
            {issueSent ? (
              <div className="py-4 text-center">
                <p className="text-sm text-teal font-medium">Issue reported successfully</p>
                <p className="text-xs text-muted-foreground mt-1">Thank you for your report. Our team will investigate.</p>
                <Button variant="outline" size="sm" className="mt-4" onClick={() => { setShowIssueModal(false); setIssueSent(false); setIssueDescription("") }}>
                  Close
                </Button>
              </div>
            ) : (
              <div className="space-y-3">
                <div>
                  <label className="text-xs font-medium text-foreground">Issue Type</label>
                  <select
                    value={issueType}
                    onChange={e => setIssueType(e.target.value)}
                    className="mt-1 w-full rounded-lg border border-border bg-muted/60 px-3 py-2 text-xs outline-none"
                  >
                    <option value="CONNECTION_PROBLEM">Connection Problem</option>
                    <option value="AUDIO_PROBLEM">Audio Problem</option>
                    <option value="VIDEO_PROBLEM">Video Problem</option>
                    <option value="CHAT_PROBLEM">Chat Problem</option>
                    <option value="PARTICIPANT_ISSUE">Participant Issue</option>
                    <option value="OTHER">Other</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs font-medium text-foreground">Description (optional)</label>
                  <textarea
                    value={issueDescription}
                    onChange={e => setIssueDescription(e.target.value)}
                    rows={3}
                    placeholder="Describe the issue..."
                    className="mt-1 w-full rounded-lg border border-border bg-muted/60 px-3 py-2 text-xs outline-none resize-none"
                  />
                </div>
                <div className="flex justify-end gap-2 pt-1">
                  <Button variant="outline" size="sm" onClick={() => setShowIssueModal(false)}>Cancel</Button>
                  <Button
                    size="sm"
                    disabled={issueSubmitting}
                    onClick={async () => {
                      setIssueSubmitting(true)
                      try {
                        const res = await fetch(`/v1/live-session/issues`, {
                          method: "POST",
                          headers: {
                            "Authorization": `Bearer ${token}`,
                            "X-Institution-Id": user?.institutionId || "",
                            "Content-Type": "application/json",
                          },
                          body: JSON.stringify({
                            classId: liveClass.id,
                            issueType,
                            description: issueDescription || undefined,
                          }),
                        })
                        if (res.ok) setIssueSent(true)
                      } catch {
                      } finally {
                        setIssueSubmitting(false)
                      }
                    }}
                  >
                    {issueSubmitting ? "Sending..." : "Submit Report"}
                  </Button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
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
        "flex size-10 items-center justify-center rounded-full border text-white transition-colors",
        active
          ? "border-white/30 bg-white/25 hover:bg-white/35"
          : "border-white/20 bg-white/10 hover:bg-white/20",
      )}
      title={label}
    >
      {children}
    </button>
  )
}
