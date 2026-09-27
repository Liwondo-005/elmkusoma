"use client"

import { useState, useEffect, useRef, useCallback } from "react"
import Link from "next/link"
import { useRouter } from "next/navigation"
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
  Wifi,
  WifiOff,
  Hand,
  XCircle,
  Flag,
  Circle,
  Paperclip,
  FileText,
  ListOrdered,
  PlayCircle,
  ClipboardList,
  MessageSquare,
  Zap,
  BarChart3,
} from "lucide-react"
import type { LiveClass } from "@/lib/learner-api"
import { learnerApi } from "@/lib/learner-api"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"
import { Room, RoomEvent, Track, Participant as LKParticipant, TrackPublication } from "livekit-client"
import { LiveVideoPlayer, type LivePlayerState } from "@/components/live/live-video-player"

interface Participant {
  userId: string
  userName: string
  role: string
  joinedAt: string | null
}

interface ChatMessage {
  id?: string | null
  userId: string
  userName: string
  message: string
  timestamp: string
  system?: boolean
  kind?: "CHAT" | "QA"
  reactions?: Record<string, number>
  deleted?: boolean
}

interface HandRaiseEntry {
  userId: string
  userName: string
  position: number
  raisedAt: string | null
}

interface Quiz {
  id: string
  title: string
  status: string
}

interface QuizQuestion {
  id: string
  questionText: string
  questionType: string
  options: string
  displayOrder: number
}

interface Poll {
  id: string
  question: string
  options: string
  status: string
}

export function LiveClassroom({ liveClass }: { liveClass: LiveClass }) {
  const { user, token } = useAuth()
  const router = useRouter()
  const [participants, setParticipants] = useState<Participant[]>([])
  // LiveKit participant identity = JWT users.id, but LiveClass.teacherId is the
  // teachers-profile PK (different table/UUID space). The WS USER_JOINED /
  // PARTICIPANTS payloads carry role:"TEACHER" with the users.id, which is the
  // only existing source that matches participant.identity — used to gate which
  // remote track may occupy the primary classroom stage.
  const teacherUserIdRef = useRef<string | null>(null)
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
  const [sessionStatus, setSessionStatus] = useState(liveClass.status)
  const [roomState, setRoomState] = useState<"idle" | "connecting" | "connected" | "reconnecting" | "error">("idle")
  const [showIssueModal, setShowIssueModal] = useState(false)
  const [issueType, setIssueType] = useState("CONNECTION_PROBLEM")
  const [issueDescription, setIssueDescription] = useState("")
  const [issueSubmitting, setIssueSubmitting] = useState(false)
  const [issueSent, setIssueSent] = useState(false)
  const [isRecording, setIsRecording] = useState(false)
  const [recordingEgressId, setRecordingEgressId] = useState<string | null>(null)
  const [showMaterialInput, setShowMaterialInput] = useState(false)
  const [materialName, setMaterialName] = useState("")
  const [materialUrl, setMaterialUrl] = useState("")
  const [attachedMaterials, setAttachedMaterials] = useState<Array<{name: string; url: string}>>([])
  const [handRaiseQueue, setHandRaiseQueue] = useState<HandRaiseEntry[]>([])
  const [showHandQueue, setShowHandQueue] = useState(false)
  const [activePolls, setActivePolls] = useState<Poll[]>([])
  const [showPollModal, setShowPollModal] = useState(false)
  const [selectedPollOption, setSelectedPollOption] = useState<number | null>(null)
  const [activeQuizzes, setActiveQuizzes] = useState<Quiz[]>([])
  const [showQuizModal, setShowQuizModal] = useState(false)
  const [quizQuestions, setQuizQuestions] = useState<QuizQuestion[]>([])
  const [quizAnswers, setQuizAnswers] = useState<Record<string, string>>({})
  const [quizSubmitted, setQuizSubmitted] = useState(false)
  const [sharedMediaList, setSharedMediaList] = useState<Array<{title: string; url: string; mediaType: string}>>([])
  const [breakoutRooms, setBreakoutRooms] = useState<Array<{id: string; name: string; maxParticipants: number; status: string}>>([])
  const [showBreakoutModal, setShowBreakoutModal] = useState(false)
  const [newBreakoutName, setNewBreakoutName] = useState("")
  const [sideTab, setSideTab] = useState<"chat" | "qa" | "people">("chat")
  const roomRef = useRef<Room | null>(null)
  const remoteVideoRef = useRef<HTMLVideoElement>(null)
  const remoteAudioRef = useRef<HTMLAudioElement>(null)
  const wsRef = useRef<WebSocket | null>(null)
  const chatEndRef = useRef<HTMLDivElement>(null)
  const startTimeRef = useRef<Date | null>(null)
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null)
  const retryCountRef = useRef(0)
  const localVideoRef = useRef<HTMLVideoElement>(null)
  const screenVideoRef = useRef<HTMLVideoElement>(null)

  const isInProgress = sessionStatus === "IN_PROGRESS" || sessionStatus === "LIVE"
  const sessionEnded = sessionStatus === "COMPLETED" || sessionStatus === "ENDED" || sessionStatus === "CANCELLED"
  const myUserId = user?.id || ""
  const isTeacherClient = user?.role === "Teacher"

  // Existing authenticated Live Class list/details page for this account —
  // the Leave/Back destination (never the public Home page). Every href is an
  // existing route that shows the account's own Join action for live classes.
  const listHref = isTeacherClient
    ? `/dashboard/teacher/live-classes/${liveClass.id}`
    : user?.role === "Admin"
      ? "/dashboard/platform-admin/live-classes"
      : user?.role === "Other Learner" ||
          (user?.role === "Student" &&
            ["COLLEGE", "UNIVERSITY", "VETA"].includes((user?.learningLevel || "").toUpperCase()))
        ? "/dashboard/learner/live-classes"
        : user?.role === "Parent"
          ? "/dashboard/parent/live-classes"
          : "/dashboard/live-classes"

  // Which remote tracks may become the PRIMARY classroom stream for this
  // client. Viewers (students) may receive the TEACHER's tracks only. The
  // backend derives the LiveKit token identity from JWT users.id, while
  // LiveClass.teacherId holds the teachers-profile PK — different UUID
  // spaces — so the matching id comes from the existing WS role
  // announcements (USER_JOINED / PARTICIPANTS, role:"TEACHER"), never
  // hardcoded participant IDs. Until that announcement arrives the previous
  // accept-any behavior is kept so no track published in the first instants
  // is lost. The teacher client keeps its existing behavior (remote
  // participants → own screen → own camera).
  function isClassroomSource(identity?: string) {
    if (isTeacherClient || !teacherUserIdRef.current) return true
    return identity === teacherUserIdRef.current
  }

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

    setRoomState("connecting")
    const room = new Room({
      adaptiveStream: true,
      dynacast: true,
    })
    roomRef.current = room

    room.on(RoomEvent.Connected, () => {
      setConnected(true)
      setReconnecting(false)
      setRoomState("connected")
      retryCountRef.current = 0
    })

    room.on(RoomEvent.Disconnected, () => {
      if (isInProgress && retryCountRef.current < 10) {
        setReconnecting(true)
        setRoomState("reconnecting")
        const delay = Math.min(3000 * Math.pow(1.5, retryCountRef.current), 30000)
        retryCountRef.current++
        setTimeout(() => {
          if (roomRef.current && liveKitToken && liveKitUrl) {
            roomRef.current.connect(liveKitUrl, liveKitToken).catch(() => {})
          }
        }, delay)
      } else {
        setRoomState("error")
      }
    })

    room.on(RoomEvent.ParticipantConnected, (participant: LKParticipant) => {
      setRemoteParticipants(prev => new Map(prev).set(participant.identity, participant))
      participant.on(RoomEvent.TrackSubscribed, (_track: any, pub: TrackPublication) => {
        if (!isClassroomSource(participant.identity)) return
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
      setRemoteVideoTrack(prev => {
        const pub = prev as (TrackPublication & { participant?: LKParticipant }) | null
        return pub?.participant?.identity === participant.identity ? null : prev
      })
      setRemoteAudioTrack(prev => {
        const pub = prev as (TrackPublication & { participant?: LKParticipant }) | null
        return pub?.participant?.identity === participant.identity ? null : prev
      })
    })

    room.on(RoomEvent.TrackSubscribed, (_track: any, pub: TrackPublication, participant?: LKParticipant) => {
      if (!isClassroomSource(participant?.identity)) return
      if (pub.kind === Track.Kind.Video) setRemoteVideoTrack(pub)
      if (pub.kind === Track.Kind.Audio) setRemoteAudioTrack(pub)
    })

    room.on(RoomEvent.TrackUnsubscribed, (_track: any, pub: TrackPublication) => {
      setRemoteVideoTrack(prev => (prev === pub ? null : prev))
      setRemoteAudioTrack(prev => (prev === pub ? null : prev))
    })

    room.connect(liveKitUrl, liveKitToken).catch(err => {
      console.error("LiveKit connection failed:", err)
      setRoomState("error")
    })

    return () => {
      room.disconnect()
      roomRef.current = null
    }
  }, [isInProgress, liveKitToken, liveKitUrl, serviceMode])

  useEffect(() => {
    if (!isInProgress) return
    let cancelled = false
    const iv = setInterval(async () => {
      try {
        const data = await learnerApi.getLiveClass(liveClass.id)
        if (!cancelled && data?.status && data.status !== sessionStatus) {
          setSessionStatus(data.status)
        }
      } catch {}
    }, 10000)
    return () => {
      cancelled = true
      clearInterval(iv)
    }
  }, [isInProgress, liveClass.id, sessionStatus])

  useEffect(() => {
    if (!isInProgress || !token || !user) return

    function fetchLiveKitToken() {
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
          setRoomState("error")
          setConnected(true)
        }
      }).catch(() => {
        setServiceMode("chat-only")
        setRoomState("error")
        setConnected(true)
      })
    }

    function connect() {
      const protocol = window.location.protocol === "https:" ? "wss:" : "ws:"
      const wsHost = process.env.NEXT_PUBLIC_WS_HOST || window.location.hostname
      const wsPort = process.env.NEXT_PUBLIC_WS_PORT || "8080"
      const wsUrl = `${protocol}//${wsHost}:${wsPort}/ws/live-class/${liveClass.id}?token=${encodeURIComponent(token ?? "")}`

      const ws = new WebSocket(wsUrl)
      wsRef.current = ws

      ws.onopen = () => {
        setReconnecting(false)
        setJoinError("")
        retryCountRef.current = 0
        ws.send(JSON.stringify({ type: "JOIN" }))
        fetchLiveKitToken()
      }

      ws.onmessage = (event) => {
        const data = JSON.parse(event.data)
        switch (data.type) {
          case "USER_JOINED":
            if (data.role === "TEACHER") teacherUserIdRef.current = data.userId
            setParticipants((prev) => {
              if (prev.some((p) => p.userId === data.userId)) return prev
              return [...prev, { userId: data.userId, userName: data.userName, role: data.role || "LEARNER", joinedAt: data.timestamp }]
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
            if (data.participants) {
              const teacher = data.participants.find((p: Participant) => p.role === "TEACHER")
              if (teacher) teacherUserIdRef.current = teacher.userId
              setParticipants(data.participants)
            }
            break
          case "CHAT_MESSAGE": {
            const raw = String(data.message || "")
            const isQa = raw.startsWith("[Q&A]")
            setChat((prev) => [...prev, { id: data.id ?? data.messageId ?? null, userId: data.userId, userName: data.userName, message: isQa ? raw.slice(6).trim() : raw, timestamp: data.timestamp, kind: isQa ? "QA" : "CHAT" }])
            break
          }
          case "CHAT_HISTORY":
            if (data.messages) {
              const history: ChatMessage[] = data.messages.map((m: any) => ({
                id: m.id ?? m.messageId ?? null,
                userId: m.userId,
                userName: m.userName,
                message: m.message,
                timestamp: m.timestamp,
                kind: String(m.message || "").startsWith("[Q&A]") ? "QA" : "CHAT",
                deleted: Boolean(m.deleted || m.isDeleted),
              }))
              setChat((prev) => [...history, ...prev])
            }
            break
          case "MESSAGE_DELETED":
            if (data.messageId) {
              setChat((prev) =>
                prev.map((m) =>
                  m.id === data.messageId || (m.timestamp === data.timestamp && m.message === data.message)
                    ? { ...m, deleted: true }
                    : m,
                ),
              )
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
          case "PARTICIPANT_MUTED":
            if (data.targetUserId === myUserId) {
              setMicEnabled(false)
              setCameraEnabled(false)
              setJoinError("You have been muted by the teacher.")
            } else {
              setChat((prev) => [...prev, {
                userId: "system",
                userName: "System",
                message: `A participant was muted by the teacher`,
                timestamp: data.timestamp,
                system: true,
              }])
            }
            break
          case "PARTICIPANT_UNMUTED":
            if (data.targetUserId === myUserId) {
              setJoinError("")
            }
            break
          case "KICKED":
            setJoinError(data.message || "You have been removed from the class.")
            retryCountRef.current = 10
            if (wsRef.current) wsRef.current.close()
            break
          case "PARTICIPANT_KICKED":
            setParticipants((prev) => prev.filter((p) => p.userId !== data.targetUserId))
            setChat((prev) => [...prev, {
              userId: "system",
              userName: "System",
              message: `A participant was removed from the class`,
              timestamp: data.timestamp,
              system: true,
            }])
            break
          case "PARTICIPANT_ROLE_CHANGED":
            setParticipants((prev) => prev.map((p) => (p.userId === data.userId ? { ...p, role: data.role } : p)))
            break
          case "HAND_RAISE_QUEUE":
            if (data.queue) setHandRaiseQueue(data.queue)
            break
          case "QUIZ_STARTED":
            setActiveQuizzes((prev) => [...prev, { id: data.quizId, title: data.title, status: "ACTIVE" }])
            setChat((prev) => [...prev, {
              userId: "system",
              userName: "System",
              message: `Quiz started: ${data.title}`,
              timestamp: data.timestamp || new Date().toISOString(),
              system: true,
            }])
            break
          case "POLL_STARTED":
            setActivePolls((prev) => [...prev, { id: data.pollId, question: data.question, options: data.options, status: "ACTIVE" }])
            setChat((prev) => [...prev, {
              userId: "system",
              userName: "System",
              message: `Poll: ${data.question}`,
              timestamp: data.timestamp || new Date().toISOString(),
              system: true,
            }])
            break
          case "SHARED_MEDIA":
            setSharedMediaList((prev) => [...prev, { title: data.title || "Shared content", url: data.url, mediaType: data.mediaType || "VIDEO" }])
            setChat((prev) => [...prev, {
              userId: "system",
              userName: "System",
              message: `Media shared: ${data.title || data.url}`,
              timestamp: data.timestamp || new Date().toISOString(),
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
      if (roomRef.current) {
        roomRef.current.disconnect()
        roomRef.current = null
      }
      if (wsRef.current) {
        if (wsRef.current.readyState === WebSocket.OPEN) {
          wsRef.current.send(JSON.stringify({ type: "LEAVE" }))
        }
        if (wsRef.current.readyState === WebSocket.OPEN || wsRef.current.readyState === WebSocket.CONNECTING) {
          wsRef.current.close()
        }
        wsRef.current = null
      }
    }
  }, [isInProgress, liveClass.id, token, user, myUserId])

  useEffect(() => {
    if (localStream && cameraEnabled && localVideoRef.current) {
      localVideoRef.current.srcObject = localStream
    }
  }, [localStream, cameraEnabled])

  useEffect(() => {
    if (screenStream && screenVideoRef.current) {
      screenVideoRef.current.srcObject = screenStream
    }
  }, [screenStream])

  async function toggleCamera() {
    if (cameraEnabled) {
      if (localStream) {
        // Only stop video tracks - a microphone track must stay alive when the
        // camera is turned off while the mic remains on.
        localStream.getVideoTracks().forEach((t) => {
          t.stop()
          localStream.removeTrack(t)
        })
        if (localStream.getTracks().length === 0) setLocalStream(null)
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
        // Merge into the existing stream instead of replacing it, so a microphone
        // track captured earlier (or later) is never dropped.
        setLocalStream((prev) => {
          if (!prev) return stream
          stream.getVideoTracks().forEach((t) => prev.addTrack(t))
          return prev
        })
        setCameraEnabled(true)
        setVideoTracks((prev) => new Map(prev).set("local-camera", stream))
        const room = roomRef.current
        if (room?.localParticipant) {
          const videoTrack = stream.getVideoTracks()[0]
          if (videoTrack) {
            await room.localParticipant.publishTrack(videoTrack, { name: "camera" })
          }
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
        let stream = localStream
        if (!stream) {
          stream = await navigator.mediaDevices.getUserMedia({ video: false, audio: true })
          setLocalStream(stream)
        } else if (stream.getAudioTracks().length === 0) {
          // The camera was turned on first: capture the mic and merge it into the
          // existing stream so it can actually be published.
          const mic = await navigator.mediaDevices.getUserMedia({ video: false, audio: true })
          mic.getAudioTracks().forEach((t) => stream!.addTrack(t))
        }
        stream.getAudioTracks().forEach((t) => { t.enabled = true })
        setMicEnabled(true)
        const room = roomRef.current
        if (room?.localParticipant) {
          const audioTrack = stream.getAudioTracks()[0]
          if (audioTrack) {
            await room.localParticipant.publishTrack(audioTrack, { name: "microphone" })
          }
        }
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
        const room = roomRef.current
        if (room?.localParticipant) {
          const screenTrack = stream.getVideoTracks()[0]
          if (screenTrack) {
            await room.localParticipant.publishTrack(screenTrack, { name: "screen-share" })
          }
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

  function muteParticipant(targetUserId: string) {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return
    wsRef.current.send(JSON.stringify({ type: "MUTE_PARTICIPANT", userId: targetUserId }))
  }

  function unmuteParticipant(targetUserId: string) {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return
    wsRef.current.send(JSON.stringify({ type: "UNMUTE_PARTICIPANT", userId: targetUserId }))
  }

  function kickParticipant(targetUserId: string) {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return
    if (!confirm("Remove this participant from the class?")) return
    wsRef.current.send(JSON.stringify({ type: "KICK_PARTICIPANT", userId: targetUserId }))
  }

  function setParticipantRole(targetUserId: string, role: "MODERATOR" | "LEARNER") {
    if (!wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return
    wsRef.current.send(JSON.stringify({ type: "SET_PARTICIPANT_ROLE", userId: targetUserId, role }))
    setParticipants((prev) => prev.map((p) => (p.userId === targetUserId ? { ...p, role } : p)))
  }

  function sendChatMessage(e: React.FormEvent) {
    e.preventDefault()
    const trimmed = message.trim()
    if (!trimmed || !wsRef.current || wsRef.current.readyState !== WebSocket.OPEN) return
    const isQa = sideTab === "qa"
    wsRef.current.send(JSON.stringify({ type: "CHAT", message: isQa ? `[Q&A] ${trimmed}` : trimmed, messageType: isQa ? "Q&A" : "CHAT" }))
    if (isQa) {
      setChat((prev) => [...prev, { userId: myUserId, userName: "You", message: trimmed, timestamp: new Date().toISOString(), kind: "QA" }])
    }
    setMessage("")
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault()
      sendChatMessage(e)
    }
  }

  async function toggleRecording() {
    if (!liveClass?.id) return
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const base = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080"
      if (isRecording && recordingEgressId) {
        const res = await fetch(`${base}/v1/live-session/classes/${liveClass.id}/recording/stop`, {
          method: "POST",
          headers: { Authorization: `Bearer ${token}` },
        })
        if (res.ok) {
          setIsRecording(false)
          setRecordingEgressId(null)
          setChat((prev) => [...prev, { userId: "system", userName: "System", message: "Recording stopped", timestamp: new Date().toISOString(), system: true }])
        }
      } else {
        const res = await fetch(`${base}/v1/live-session/classes/${liveClass.id}/recording/start`, {
          method: "POST",
          headers: { Authorization: `Bearer ${token}` },
        })
        if (res.ok) {
          const data = await res.json()
          const egressId = data?.data?.egressId
          setIsRecording(true)
          setRecordingEgressId(egressId || null)
          setChat((prev) => [...prev, { userId: "system", userName: "System", message: "Recording started", timestamp: new Date().toISOString(), system: true }])
        }
      }
    } catch {
      setChat((prev) => [...prev, { userId: "system", userName: "System", message: "Failed to toggle recording", timestamp: new Date().toISOString(), system: true }])
    }
  }

  function handleAttachMaterial() {
    if (!materialName.trim() || !materialUrl.trim()) return
    setAttachedMaterials((prev) => [...prev, { name: materialName.trim(), url: materialUrl.trim() }])
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify({ type: "CHAT", message: `📎 Material shared: ${materialName.trim()} - ${materialUrl.trim()}` }))
    }
    setMaterialName("")
    setMaterialUrl("")
    setShowMaterialInput(false)
  }

  const [newQuizTitle, setNewQuizTitle] = useState("")
  const [quizQuestionList, setQuizQuestionList] = useState<Array<{questionText: string; options: string; correctAnswer: string}>>([])
  const [newPollQuestion, setNewPollQuestion] = useState("")
  const [newPollOptions, setNewPollOptions] = useState("")

  async function createQuiz() {
    if (!newQuizTitle.trim() || !liveClass?.id || quizQuestionList.length === 0) return
    try {
      const apiBase = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080"
      const res = await fetch(`${apiBase}/v1/live-session/classes/${liveClass.id}/quizzes`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "X-Institution-Id": user?.institutionId || "",
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ title: newQuizTitle.trim(), questions: quizQuestionList }),
      })
      if (res.ok) {
        setChat(prev => [...prev, { userId: "system", userName: "System", message: `Quiz "${newQuizTitle.trim()}" launched!`, timestamp: new Date().toISOString(), system: true }])
        setNewQuizTitle("")
        setQuizQuestionList([])
      }
    } catch {}
  }

  async function createPoll() {
    if (!newPollQuestion.trim() || !liveClass?.id || !newPollOptions.trim()) return
    const options = newPollOptions.split(",").map(o => o.trim()).filter(Boolean)
    try {
      const apiBase = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080"
      const res = await fetch(`${apiBase}/v1/live-session/classes/${liveClass.id}/polls`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "X-Institution-Id": user?.institutionId || "",
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ question: newPollQuestion.trim(), options: JSON.stringify(options) }),
      })
      if (res.ok) {
        setChat(prev => [...prev, { userId: "system", userName: "System", message: `Poll: "${newPollQuestion.trim()}"`, timestamp: new Date().toISOString(), system: true }])
        setNewPollQuestion("")
        setNewPollOptions("")
      }
    } catch {}
  }

  async function createBreakoutRoom() {
    if (!newBreakoutName.trim() || !liveClass?.id) return
    try {
      const apiBase = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080"
      const res = await fetch(`${apiBase}/v1/live-session/classes/${liveClass.id}/breakout-rooms`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "X-Institution-Id": user?.institutionId || "",
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ name: newBreakoutName.trim(), maxParticipants: 10 }),
      })
      if (res.ok) {
        const data = await res.json()
        setBreakoutRooms(prev => [...prev, data.data])
        setNewBreakoutName("")
        setShowBreakoutModal(false)
        setChat(prev => [...prev, { userId: "system", userName: "System", message: `Breakout room "${newBreakoutName.trim()}" created`, timestamp: new Date().toISOString(), system: true }])
      }
    } catch {}
  }

  async function startBreakoutRoom(roomId: string) {
    try {
      const apiBase = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080"
      await fetch(`${apiBase}/v1/live-session/breakout-rooms/${roomId}/start`, {
        method: "POST",
        headers: { "Authorization": `Bearer ${token}` },
      })
      setBreakoutRooms(prev => prev.map(r => r.id === roomId ? { ...r, status: "ACTIVE" } : r))
    } catch {}
  }

  async function endBreakoutRoom(roomId: string) {
    try {
      const apiBase = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080"
      await fetch(`${apiBase}/v1/live-session/breakout-rooms/${roomId}/end`, {
        method: "POST",
        headers: { "Authorization": `Bearer ${token}` },
      })
      setBreakoutRooms(prev => prev.map(r => r.id === roomId ? { ...r, status: "ENDED" } : r))
    } catch {}
  }

  async function assignToBreakout(roomId: string, userId: string) {
    try {
      const apiBase = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080"
      const endpoint = user?.role === "Teacher"
        ? `${apiBase}/v1/live-session/breakout-rooms/${roomId}/assign`
        : `${apiBase}/v1/live-session/breakout-rooms/${roomId}/join`
      await fetch(endpoint, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ userId }),
      })
      setChat(prev => [...prev, { userId: "system", userName: "System", message: user?.role === "Teacher" ? `A participant was assigned to a breakout room` : `You joined a breakout room`, timestamp: new Date().toISOString(), system: true }])
    } catch {}
  }

  function handleLeave() {
    retryCountRef.current = 10
    if (reconnectTimeoutRef.current) clearTimeout(reconnectTimeoutRef.current)
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
    // Return to the existing authenticated Live Class list/details page (with
    // its Join action) — never the public Home page. Unmount then runs the
    // effect cleanup, which is idempotent (sockets already closed above).
    router.push(listHref)
  }

  const formatTime = (d: string) => {
    return new Date(d).toLocaleTimeString("en-US", { hour: "2-digit", minute: "2-digit" })
  }

  const playerState: LivePlayerState = sessionEnded
    ? "ended"
    : !isInProgress
      ? sessionStatus === "SCHEDULED" || sessionStatus === "UPCOMING"
        ? "scheduled"
        : "ended"
      : serviceMode === "chat-only"
        ? "error"
        : roomState === "error"
          ? "error"
          : roomState === "reconnecting"
            ? "reconnecting"
            : roomState === "connected"
              ? remoteVideoTrack?.videoTrack
                ? "live"
                : "waiting"
              : "connecting"

  function retryLiveKit() {
    if (!token || !user) return
    setRoomState("connecting")
    setServiceMode("unknown")
    setLiveKitToken(null)
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
        setRoomState("error")
        setConnected(true)
      }
    }).catch(() => {
      setServiceMode("chat-only")
      setRoomState("error")
      setConnected(true)
    })
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-4 sm:px-6 lg:px-8">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <Link
            href={listHref}
            className="inline-flex items-center gap-1 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
          >
            <ChevronLeft className="size-4" />
            Back
          </Link>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" className="border-destructive/30 text-destructive hover:bg-destructive/10" onClick={handleLeave}>
            Leave
          </Button>
        </div>
      </div>

      <div className="mb-3 flex flex-wrap items-center gap-3">
        <span className={cn(
          "inline-flex items-center gap-1.5 rounded-md px-2.5 py-1 text-xs font-semibold",
          (playerState === "live" || playerState === "waiting") ? "bg-teal text-teal-foreground" :
          sessionStatus === "SERVICE_DEGRADED" || sessionStatus === "SERVICE_UNAVAILABLE" ? "bg-amber-100 text-amber-700" :
          sessionStatus === "RECOVERING" ? "bg-blue-100 text-blue-700" :
          sessionEnded ? "bg-gray-100 text-gray-600" :
          "bg-blue-100 text-blue-700"
        )}>
          {(playerState === "live" || playerState === "waiting") && <span className="size-1.5 animate-pulse rounded-full bg-white" />}
          {(playerState === "live" || playerState === "waiting")
            ? "LIVE"
            : sessionStatus.replace(/_/g, " ")}
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
          <p className="font-medium">Live service degraded</p>
          <p className="mt-0.5 text-amber-600">ELMKUSOMA Live is experiencing a temporary technical issue with the real-time media service. You can still participate via chat. We are working to restore full service.</p>
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <div className="flex flex-col gap-4 order-2 lg:order-1">
          <LiveVideoPlayer
            state={playerState}
            remoteVideoTrack={remoteVideoTrack}
            remoteAudioTrack={remoteAudioTrack}
            isTeacher={isTeacherClient}
            localStream={localStream}
            screenStream={screenStream}
            cameraEnabled={cameraEnabled}
            recordingUrl={sessionEnded ? liveClass.recordingUrl : null}
            scheduledLabel={liveClass.scheduledAt ? `Starts at ${formatTime(liveClass.scheduledAt)}` : "Starts at TBD"}
            onRetry={retryLiveKit}
            pipStream={
              localStream && cameraEnabled && (screenStream || (!isTeacherClient && !remoteVideoTrack))
                ? localStream
                : null
            }
          >
            {isInProgress && (
              <>
                <div className="absolute inset-x-0 bottom-0 z-20 flex items-center justify-center gap-1.5 bg-gradient-to-t from-black/70 to-transparent p-3">
                    <ControlButton active={cameraEnabled} onClick={toggleCamera} label={cameraEnabled ? "Turn off camera" : "Turn on camera"}>
                      {cameraEnabled ? <Video className="size-4" /> : <VideoOff className="size-4" />}
                    </ControlButton>
                    <ControlButton active={micEnabled} onClick={toggleMic} label={micEnabled ? "Mute" : "Unmute"}>
                      {micEnabled ? <Mic className="size-4" /> : <MicOff className="size-4" />}
                    </ControlButton>
                    <ControlButton active={screenSharing} onClick={toggleScreenShare} label={screenSharing ? "Stop sharing" : "Share screen"}>
                      <MonitorUp className="size-4" />
                    </ControlButton>
                    <ControlButton active={isRecording} onClick={toggleRecording} label={isRecording ? "Stop recording" : "Start recording"}>
                      <Circle className={cn("size-4", isRecording && "fill-red-500 text-red-500 animate-pulse")} />
                    </ControlButton>
                    <ControlButton active={showMaterialInput} onClick={() => setShowMaterialInput(!showMaterialInput)} label="Attach material">
                      <Paperclip className="size-4" />
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
                  {showMaterialInput && (
                    <div className="absolute inset-x-0 bottom-16 z-20 flex items-center gap-2 bg-black/80 p-3 rounded-lg mx-3">
                      <input
                        type="text"
                        value={materialName}
                        onChange={(e) => setMaterialName(e.target.value)}
                        placeholder="Material name"
                        className="h-8 flex-1 rounded border border-white/20 bg-white/10 px-2 text-xs text-white placeholder:text-white/50 outline-none"
                      />
                      <input
                        type="url"
                        value={materialUrl}
                        onChange={(e) => setMaterialUrl(e.target.value)}
                        placeholder="https://..."
                        className="h-8 flex-1 rounded border border-white/20 bg-white/10 px-2 text-xs text-white placeholder:text-white/50 outline-none"
                      />
                      <Button size="sm" className="h-8 text-xs" onClick={handleAttachMaterial}>Share</Button>
                    </div>
                  )}
                </>
              )}
            </LiveVideoPlayer>

          <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
            <h2 className="text-xs font-semibold text-foreground mb-2">Class Details</h2>
            <div className="space-y-1 text-xs text-muted-foreground">
              {liveClass.scheduledAt && <p>Scheduled: {new Date(liveClass.scheduledAt).toLocaleString()}</p>}
              {liveClass.durationMinutes && <p>Duration: {liveClass.durationMinutes} min</p>}
              {liveClass.maxParticipants && <p>Max participants: {liveClass.maxParticipants}</p>}
              {liveClass.description && <p className="whitespace-pre-line line-clamp-3">{liveClass.description}</p>}
            </div>
          </div>
          {attachedMaterials.length > 0 && (
            <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
              <h2 className="text-xs font-semibold text-foreground mb-2">Attached Materials</h2>
              <div className="space-y-1.5">
                {attachedMaterials.map((m, i) => (
                  <a key={i} href={m.url} target="_blank" rel="noopener noreferrer" className="flex items-center gap-2 text-xs text-primary hover:underline">
                    <FileText className="size-3 shrink-0" />
                    {m.name}
                  </a>
                ))}
              </div>
            </div>
          )}
        </div>

        <div className="flex flex-col gap-4 order-1 lg:order-2">
          <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-xs font-semibold text-foreground">Participants ({participants.length})</h2>
            </div>
            <div className="mt-3 space-y-1.5 max-h-36 overflow-y-auto">
              {participants.length === 0 ? (
                <p className="text-[10px] text-muted-foreground">No participants yet</p>
              ) : (
                participants.map((p) => (
                  <div key={p.userId} className="flex items-center gap-2 group">
                    <span className={cn(
                      "flex size-6 items-center justify-center rounded-full text-[10px] font-semibold",
                      p.userId === myUserId ? "bg-primary text-primary-foreground" : "bg-accent text-primary"
                    )}>
                      {p.userName?.charAt(0) || "?"}
                    </span>
                    <div className="min-w-0 flex-1">
                      <p className="text-[11px] font-medium text-foreground truncate">
                        {p.userName}
                        {p.userId === myUserId && <span className="text-muted-foreground"> (you)</span>}
                      </p>
                    </div>
                    {p.userId !== myUserId && (user?.role === "Teacher" || user?.role === "Admin") && (
                      <div className="hidden group-hover:flex items-center gap-0.5">
                        <button onClick={() => muteParticipant(p.userId)} className="rounded p-0.5 text-muted-foreground hover:text-foreground hover:bg-muted" title="Mute">
                          <MicOff className="size-3" />
                        </button>
                        <button onClick={() => unmuteParticipant(p.userId)} className="rounded p-0.5 text-muted-foreground hover:text-foreground hover:bg-muted" title="Unmute">
                          <Mic className="size-3" />
                        </button>
                        <button onClick={() => kickParticipant(p.userId)} className="rounded p-0.5 text-muted-foreground hover:text-destructive hover:bg-destructive/10" title="Remove">
                          <XCircle className="size-3" />
                        </button>
                      </div>
                    )}
                  </div>
                ))
              )}
            </div>
          </div>

          {handRaiseQueue.length > 0 && (
            <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 shadow-sm">
              <div className="flex items-center justify-between mb-2">
                <h2 className="text-xs font-semibold text-amber-700 flex items-center gap-1">
                  <ListOrdered className="size-3" /> Hand Raise Queue ({handRaiseQueue.length})
                </h2>
              </div>
              <div className="space-y-1.5">
                {handRaiseQueue.map((entry, i) => (
                  <div key={entry.userId} className="flex items-center gap-2 text-xs">
                    <span className="flex size-5 items-center justify-center rounded-full bg-amber-200 text-[10px] font-bold text-amber-800">
                      {entry.position}
                    </span>
                    <span className="text-amber-900 font-medium">{entry.userName}</span>
                    {entry.raisedAt && (
                      <span className="text-amber-600 text-[9px] ml-auto">
                        {new Date(entry.raisedAt).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}
                      </span>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {activePolls.filter(p => p.status === "ACTIVE").length > 0 && (
            <div className="rounded-xl border border-blue-200 bg-blue-50 p-4 shadow-sm">
              <h2 className="text-xs font-semibold text-blue-700 flex items-center gap-1 mb-2">
                <ClipboardList className="size-3" /> Active Poll
              </h2>
              {activePolls.filter(p => p.status === "ACTIVE").slice(-1).map(poll => (
                <div key={poll.id}>
                  <p className="text-xs font-medium text-blue-900 mb-2">{poll.question}</p>
                  <div className="space-y-1.5">
                    {(() => {
                      try {
                        const options = JSON.parse(poll.options)
                        return Array.isArray(options) ? options.map((opt: string, i: number) => (
                          <button
                            key={i}
                            onClick={() => {
                              if (selectedPollOption === null) {
                                setSelectedPollOption(i)
                                fetch(`/v1/live-session/polls/${poll.id}/vote`, {
                                  method: "POST",
                                  headers: {
                                    "Authorization": `Bearer ${token}`,
                                    "Content-Type": "application/json",
                                  },
                                  body: JSON.stringify({ optionIndex: i }),
                                })
                              }
                            }}
                            className={cn(
                              "w-full text-left rounded-lg border px-3 py-1.5 text-xs transition-colors",
                              selectedPollOption === i
                                ? "border-blue-400 bg-blue-100 text-blue-800 font-medium"
                                : "border-blue-200 bg-white text-blue-700 hover:bg-blue-100"
                            )}
                          >
                            {opt}
                          </button>
                        )) : null
                      } catch { return null }
                    })()}
                  </div>
                </div>
              ))}
            </div>
          )}

          {sharedMediaList.length > 0 && (
            <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
              <h2 className="text-xs font-semibold text-foreground mb-2 flex items-center gap-1">
                <PlayCircle className="size-3" /> Shared Media
              </h2>
              <div className="space-y-1.5">
                {sharedMediaList.map((m, i) => (
                  <a key={i} href={m.url} target="_blank" rel="noopener noreferrer" className="flex items-center gap-2 text-xs text-primary hover:underline">
                    <PlayCircle className="size-3 shrink-0" />
                    {m.title || m.url}
                  </a>
                ))}
              </div>
            </div>
          )}

          {isInProgress && user?.role === "Teacher" && (
            <div className="rounded-xl border border-border bg-card p-4 shadow-sm space-y-3">
              <h2 className="text-xs font-semibold text-foreground flex items-center gap-1">
                <Zap className="size-3" /> Launch Quiz
              </h2>
              <input
                type="text"
                value={newQuizTitle}
                onChange={e => setNewQuizTitle(e.target.value)}
                placeholder="Quiz title"
                className="h-8 w-full rounded border border-border bg-muted/60 px-2 text-xs outline-none"
              />
              {quizQuestionList.map((q, i) => (
                <div key={i} className="rounded border border-border p-2 text-[10px] space-y-1">
                  <div className="font-medium text-foreground">Q{i + 1}: {q.questionText}</div>
                  <div className="text-muted-foreground">Options: {q.options}</div>
                  <div className="text-teal">Answer: {q.correctAnswer}</div>
                </div>
              ))}
              <div className="flex gap-1">
                <input
                  type="text"
                  placeholder="Question"
                  className="h-7 flex-1 rounded border border-border bg-muted/60 px-2 text-[10px] outline-none"
                  id="quiz-q-text"
                />
                <input
                  type="text"
                  placeholder="Options (comma-separated)"
                  className="h-7 flex-1 rounded border border-border bg-muted/60 px-2 text-[10px] outline-none"
                  id="quiz-q-opts"
                />
                <input
                  type="text"
                  placeholder="Correct answer"
                  className="h-7 flex-1 rounded border border-border bg-muted/60 px-2 text-[10px] outline-none"
                  id="quiz-q-ans"
                />
                <Button
                  size="sm"
                  variant="outline"
                  className="h-7 text-[10px]"
                  onClick={() => {
                    const text = (document.getElementById("quiz-q-text") as HTMLInputElement)?.value
                    const opts = (document.getElementById("quiz-q-opts") as HTMLInputElement)?.value
                    const ans = (document.getElementById("quiz-q-ans") as HTMLInputElement)?.value
                    if (text && opts && ans) {
                      setQuizQuestionList(prev => [...prev, { questionText: text, options: opts, correctAnswer: ans }])
                      ;(document.getElementById("quiz-q-text") as HTMLInputElement).value = ""
                      ;(document.getElementById("quiz-q-opts") as HTMLInputElement).value = ""
                      ;(document.getElementById("quiz-q-ans") as HTMLInputElement).value = ""
                    }
                  }}
                >+</Button>
              </div>
              <Button
                size="sm"
                className="h-7 text-[10px] w-full"
                disabled={!newQuizTitle.trim() || quizQuestionList.length === 0}
                onClick={createQuiz}
              >
                Launch Quiz ({quizQuestionList.length} questions)
              </Button>
            </div>
          )}

          {isInProgress && user?.role === "Teacher" && (
            <div className="rounded-xl border border-border bg-card p-4 shadow-sm space-y-3">
              <h2 className="text-xs font-semibold text-foreground flex items-center gap-1">
                <BarChart3 className="size-3" /> Create Poll
              </h2>
              <input
                type="text"
                value={newPollQuestion}
                onChange={e => setNewPollQuestion(e.target.value)}
                placeholder="Poll question"
                className="h-8 w-full rounded border border-border bg-muted/60 px-2 text-xs outline-none"
              />
              <input
                type="text"
                value={newPollOptions}
                onChange={e => setNewPollOptions(e.target.value)}
                placeholder="Options (comma-separated)"
                className="h-8 w-full rounded border border-border bg-muted/60 px-2 text-xs outline-none"
              />
              <Button
                size="sm"
                className="h-7 text-[10px] w-full"
                disabled={!newPollQuestion.trim() || !newPollOptions.trim()}
                onClick={createPoll}
              >
                Launch Poll
              </Button>
            </div>
          )}

          {(breakoutRooms.length > 0 || (isInProgress && user?.role === "Teacher")) && (
            <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
              <div className="flex items-center justify-between mb-2">
                <h2 className="text-xs font-semibold text-foreground flex items-center gap-1">
                  <Users className="size-3" /> Breakout Rooms
                </h2>
                {isInProgress && user?.role === "Teacher" && (
                  <Button size="sm" variant="outline" className="h-6 text-[10px]" onClick={() => setShowBreakoutModal(true)}>
                    + Create
                  </Button>
                )}
              </div>
              {breakoutRooms.length === 0 ? (
                <p className="text-[10px] text-muted-foreground">No breakout rooms yet</p>
              ) : (
                <div className="space-y-1.5">
                  {breakoutRooms.map(room => (
                    <div key={room.id} className="flex items-center gap-2 rounded-lg border border-border px-3 py-2 text-xs">
                      <div className={cn(
                        "size-2 rounded-full",
                        room.status === "ACTIVE" ? "bg-teal animate-pulse" : room.status === "ENDED" ? "bg-gray-400" : "bg-amber-400"
                      )} />
                      <span className="font-medium text-foreground flex-1">{room.name}</span>
                      {user?.role === "Teacher" && (
                        <div className="flex gap-1">
                          {room.status === "WAITING" && (
                            <Button size="sm" variant="ghost" className="h-5 text-[10px] text-teal" onClick={() => startBreakoutRoom(room.id)}>Start</Button>
                          )}
                          {room.status === "ACTIVE" && (
                            <Button size="sm" variant="ghost" className="h-5 text-[10px] text-destructive" onClick={() => endBreakoutRoom(room.id)}>End</Button>
                          )}
                        </div>
                      )}
                      {user?.role !== "Teacher" && room.status === "ACTIVE" && (
                        <Button
                          size="sm"
                          variant="ghost"
                          className="h-5 text-[10px] text-teal"
                          onClick={() => assignToBreakout(room.id, user?.id || "")}
                        >Join</Button>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {showBreakoutModal && (
            <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
              <h3 className="text-xs font-semibold text-foreground mb-2">New Breakout Room</h3>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={newBreakoutName}
                  onChange={e => setNewBreakoutName(e.target.value)}
                  placeholder="Room name"
                  className="h-8 flex-1 rounded border border-border bg-muted/60 px-2 text-xs outline-none"
                  onKeyDown={e => { if (e.key === "Enter") createBreakoutRoom() }}
                />
                <Button size="sm" className="h-8 text-xs" onClick={createBreakoutRoom} disabled={!newBreakoutName.trim()}>Create</Button>
                <Button size="sm" variant="ghost" className="h-8 text-xs" onClick={() => { setShowBreakoutModal(false); setNewBreakoutName("") }}>Cancel</Button>
              </div>
            </div>
          )}

          <div className="flex min-h-80 flex-col rounded-xl border border-border bg-card shadow-sm">
            <div className="flex items-center justify-between border-b border-border px-4 py-2.5">
              <div className="flex gap-1" role="tablist" aria-label="Class side panel">
                {(["chat", "qa", "people"] as const).map((tab) => (
                  <button
                    key={tab}
                    type="button"
                    role="tab"
                    aria-selected={sideTab === tab}
                    onClick={() => setSideTab(tab)}
                    className={`rounded px-2 py-1 text-[10px] font-semibold uppercase ${sideTab === tab ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted"}`}
                  >
                    {tab === "chat" ? "Chat" : tab === "qa" ? "Q&A" : "People"}
                  </button>
                ))}
              </div>
              <span className="inline-flex items-center gap-1">
                {connected && <span className="size-1.5 rounded-full bg-teal animate-pulse" />}
                <span className="text-[10px] text-muted-foreground">
                  {connected ? "Live" : reconnecting ? "Reconnecting..." : "Offline"}
                </span>
              </span>
            </div>
            {sideTab === "people" ? (
              <div className="flex-1 space-y-1.5 overflow-y-auto p-4">
                {participants.map((p) => (
                  <div key={p.userId} className="flex items-center justify-between gap-2 text-xs">
                    <span className="font-medium text-foreground">{p.userName}</span>
                    <span className="text-[10px] text-muted-foreground">{p.role}</span>
                    {(user?.role === "Teacher" || user?.role === "Admin") && p.userId !== myUserId && (
                      <span className="flex gap-1">
                        <button
                          type="button"
                          onClick={() => setParticipantRole(p.userId, p.role === "MODERATOR" ? "LEARNER" : "MODERATOR")}
                          aria-label={p.role === "MODERATOR" ? `Demote ${p.userName}` : `Promote ${p.userName} to moderator`}
                          className="rounded p-0.5 text-[10px] text-muted-foreground hover:text-foreground"
                        >
                          {p.role === "MODERATOR" ? "Demote" : "Promote"}
                        </button>
                        <button onClick={() => muteParticipant(p.userId)} aria-label={`Mute ${p.userName}`} className="rounded p-0.5 text-muted-foreground hover:text-foreground">
                          <MicOff className="size-3" />
                        </button>
                        <button onClick={() => kickParticipant(p.userId)} aria-label={`Remove ${p.userName}`} className="rounded p-0.5 text-destructive">
                          <XCircle className="size-3" />
                        </button>
                      </span>
                    )}
                  </div>
                ))}
                {participants.length === 0 && (
                  <p className="text-center text-[11px] text-muted-foreground py-6">No participants yet</p>
                )}
              </div>
            ) : (
            <div className="flex-1 space-y-2 overflow-y-auto p-4">
              {(() => {
                const visible = chat.filter((c) =>
                  sideTab === "qa" ? c.kind === "QA" || (c.message || "").includes("[Q&A]") : c.kind !== "QA",
                )
                if (visible.length === 0) {
                  return (
                    <p className="text-center text-[11px] text-muted-foreground py-6">
                      {sideTab === "qa" ? "No questions yet" : "No messages yet"}
                    </p>
                  )
                }
                return visible.map((c, i) => (
                  <div key={i} className={cn("flex gap-2", (c.system || c.deleted) && "justify-center")}>
                    {c.system || c.deleted ? (
                      <span className="text-[10px] text-muted-foreground italic">{c.deleted ? "Message removed" : c.message}</span>
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
                          <div className="mt-0.5 flex gap-1">
                            {["👍", "❤️"].map((r) => (
                              <button
                                key={r}
                                type="button"
                                aria-label={`React ${r}`}
                                onClick={() => {
                                  setChat((prev) => prev.map((m) => (m === c ? { ...m, reactions: { ...(m.reactions || {}), [r]: ((m.reactions || {})[r] || 0) + 1 } } : m)))
                                  if (wsRef.current?.readyState === WebSocket.OPEN) {
                                    wsRef.current.send(JSON.stringify({ type: "CHAT", message: r }))
                                  }
                                }}
                                className="rounded px-1 text-[10px] hover:bg-muted"
                              >
                                {r}{c.reactions?.[r] ? ` ${c.reactions[r]}` : ""}
                              </button>
                            ))}
                            {(user?.role === "Teacher" || user?.role === "Admin") && c.userId !== myUserId && (
                              <button
                                type="button"
                                aria-label="Remove message"
                                onClick={() => {
                                  setChat((prev) => prev.map((m) => (m === c ? { ...m, deleted: true } : m)))
                                  if (c.id && wsRef.current?.readyState === WebSocket.OPEN) {
                                    wsRef.current.send(
                                      JSON.stringify({
                                        type: "DELETE_MESSAGE",
                                        messageType: "DELETE_MESSAGE",
                                        messageId: c.id,
                                      }),
                                    )
                                  }
                                }}
                                className="rounded px-1 text-[10px] text-destructive hover:bg-destructive/10"
                              >
                                Remove
                              </button>
                            )}
                          </div>
                        </div>
                      </>
                    )}
                  </div>
                ))
              })()}
              <div ref={chatEndRef} />
            </div>
            )}
            <form onSubmit={sendChatMessage} className="flex items-center gap-1.5 border-t border-border p-2.5">
              <input
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder={sideTab === "qa" ? (isInProgress ? "Ask a question..." : "Q&A during live session") : isInProgress ? "Type a message..." : "Chat during live session"}
                disabled={!isInProgress || !connected}
                aria-label={sideTab === "qa" ? "Ask a question" : "Chat message"}
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
                        const apiBase = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080"
                        const res = await fetch(`${apiBase}/v1/live-session/report/${liveClass.id}`, {
                          method: "POST",
                          headers: {
                            "Authorization": `Bearer ${token}`,
                            "X-Institution-Id": user?.institutionId || "",
                            "Content-Type": "application/json",
                          },
                          body: JSON.stringify({
                            issueType,
                            description: issueDescription || undefined,
                          }),
                        })
                        if (res.ok) setIssueSent(true)
                      } catch {
                        setJoinError("Failed to submit issue report")
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
