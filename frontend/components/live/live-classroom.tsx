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
  // Teacher-only payload field (the backend strips it for students).
  correctAnswer?: string
  // Student's own submission — myAnswer always, isCorrect only after evaluation.
  myAnswer?: string | null
  isCorrect?: boolean
}

interface QuizUI {
  questions: QuizQuestion[]
  answers: Record<string, string>
  submitted: boolean
  loading: boolean
  error: string
}

interface Poll {
  id: string
  question: string
  options: string
  status: string
  myVote?: number | null
  totalVotes?: number
  closedAt?: string | null
}

// Server-side view returned by GET /classes/{id}/breakout-rooms (and refreshed on
// every BREAKOUT_ROOMS_UPDATED event — assignedToMe is per-viewer server truth).
interface BreakoutRoomView {
  id: string
  name: string
  maxParticipants: number
  status: string
  assignedCount?: number
  assignedUsers?: Array<{ userId: string; userName: string }>
  assignedToMe?: boolean
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
  const [selectedPollOption, setSelectedPollOption] = useState<number | null>(null)
  const [pollResultsById, setPollResultsById] = useState<Record<string, { results: Record<string, number>; totalVotes: number }>>({})
  const [activeQuizzes, setActiveQuizzes] = useState<Quiz[]>([])
  const [focusedQuizId, setFocusedQuizId] = useState<string | null>(null)
  const [quizUI, setQuizUI] = useState<Record<string, QuizUI>>({})
  const [quizResultsById, setQuizResultsById] = useState<Record<string, { answeredCount: number; participantCount: number; totalResponses: number; totalCorrect: number }>>({})
  const [sharedMediaList, setSharedMediaList] = useState<Array<{title: string; url: string; mediaType: string}>>([])
  const [breakoutRooms, setBreakoutRooms] = useState<BreakoutRoomView[]>([])
  const [showBreakoutModal, setShowBreakoutModal] = useState(false)
  const [newBreakoutName, setNewBreakoutName] = useState("")
  // Live-interaction lifecycle flags (quiz/poll/breakout): every mutation reports
  // its real outcome — errors surface inline, success only ever follows a server 2xx.
  const [interactiveReady, setInteractiveReady] = useState(false)
  const [interactiveLoading, setInteractiveLoading] = useState(false)
  const [interactiveError, setInteractiveError] = useState("")
  const [actionError, setActionError] = useState("")
  const [actionBusy, setActionBusy] = useState(false)
  const [pollVoting, setPollVoting] = useState(false)
  const [quizSubmitting, setQuizSubmitting] = useState(false)
  const [inBreakout, setInBreakout] = useState<{ roomId: string; roomName: string } | null>(null)
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
  // Restore flag re-armed on every WS (re)connect so refresh/reconnect always
  // re-reads quiz/poll/breakout state from the server after the JOIN ack.
  const interactiveLoadedRef = useRef(false)
  const quizLoadingRef = useRef<Set<string>>(new Set())
  const quizLoadedRef = useRef<Set<string>>(new Set())
  const inBreakoutRef = useRef<string | null>(null)
  // Mirror of localStream for the LiveKit room effect (room switches happen when
  // liveKitToken changes, which does not re-read state declared in the closure).
  const mediaStateRef = useRef<MediaStream | null>(null)

  useEffect(() => { mediaStateRef.current = localStream }, [localStream])
  useEffect(() => { inBreakoutRef.current = inBreakout?.roomId ?? null }, [inBreakout])

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
    // Inside a breakout room every assigned participant is a legitimate source —
    // the main-room "teacher's tracks only" rule does not apply there.
    if (typeof roomName === "string" && roomName.startsWith("breakout-")) return true
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
    // A token swap tears this room down and builds a fresh one (main room →
    // breakout → back). Remote publications belong to the previous room, so they
    // must not linger on the stage while the new room connects.
    setRemoteVideoTrack(null)
    setRemoteAudioTrack(null)
    setRemoteParticipants(new Map())
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
      // Re-publish local camera/mic captured in a previous room (breakout join or
      // return switches rooms without touching the media stream).
      const stream = mediaStateRef.current
      if (stream) {
        stream.getTracks().forEach((t) => {
          const source = t.kind === "video" ? Track.Source.Camera : Track.Source.Microphone
          if (room.localParticipant.getTrackPublication(source)) return
          room.localParticipant
            .publishTrack(t, { name: t.kind === "video" ? "camera" : "microphone" })
            .catch(() => {})
        })
      }
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
    // roomName: isClassroomSource reads it to relax source gating inside breakout rooms.
  }, [isInProgress, liveKitToken, liveKitUrl, serviceMode, roomName])

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
      // Each (re)connect performs its own server-state restore after the JOIN ack.
      interactiveLoadedRef.current = false

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
            // JOIN ack ⇒ this client's participant row exists server-side, so the
            // session-scoped reads below are authorized. Once per connection:
            // restores quizzes/polls/breakouts after mount and after every reconnect.
            if (!interactiveLoadedRef.current) {
              interactiveLoadedRef.current = true
              void refreshInteractiveState()
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
          case "QUIZ_STARTED": {
            const quizId = String(data.quizId || "")
            setActiveQuizzes((prev) =>
              prev.some((q) => q.id === quizId)
                ? prev
                : [...prev, { id: quizId, title: data.title, status: "ACTIVE" }],
            )
            setChat((prev) => [...prev, {
              userId: "system",
              userName: "System",
              message: `Quiz started: ${data.title}`,
              timestamp: data.timestamp || new Date().toISOString(),
              system: true,
            }])
            break
          }
          case "QUIZ_CLOSED": {
            const quizId = String(data.quizId || "")
            setActiveQuizzes((prev) => prev.map((q) => (q.id === quizId ? { ...q, status: "CLOSED" } : q)))
            if (!isTeacherClient) void ensureQuizLoaded(quizId, true) // evaluation done → refresh my score view
            setChat((prev) => [...prev, {
              userId: "system",
              userName: "System",
              message: `Quiz closed: ${data.title || "Untitled quiz"}`,
              timestamp: data.timestamp || new Date().toISOString(),
              system: true,
            }])
            break
          }
          case "QUIZ_RESULT": {
            // Aggregate-only progress event (no identities, no answers).
            const quizId = String(data.quizId || "")
            setQuizResultsById((prev) => ({
              ...prev,
              [quizId]: {
                answeredCount: Number(data.answeredCount ?? 0),
                participantCount: Number(data.participantCount ?? 0),
                totalResponses: Number(data.totalResponses ?? 0),
                totalCorrect: Number(data.totalCorrect ?? 0),
              },
            }))
            break
          }
          case "POLL_STARTED": {
            const pollId = String(data.pollId || "")
            setActivePolls((prev) =>
              prev.some((p) => p.id === pollId)
                ? prev
                : [...prev, { id: pollId, question: data.question, options: data.options, status: "ACTIVE", myVote: null, totalVotes: 0 }],
            )
            setChat((prev) => [...prev, {
              userId: "system",
              userName: "System",
              message: `Poll: ${data.question}`,
              timestamp: data.timestamp || new Date().toISOString(),
              system: true,
            }])
            break
          }
          case "POLL_CLOSED": {
            const pollId = String(data.pollId || "")
            setActivePolls((prev) => prev.map((p) => (p.id === pollId ? { ...p, status: "CLOSED" } : p)))
            if (data.results) {
              const results: Record<string, number> = data.results
              setPollResultsById((prev) => ({
                ...prev,
                [pollId]: { results, totalVotes: Object.values(results).reduce((a, b) => a + Number(b || 0), 0) },
              }))
            }
            setChat((prev) => [...prev, {
              userId: "system",
              userName: "System",
              message: `Poll closed: ${data.question || ""}`.trim(),
              timestamp: data.timestamp || new Date().toISOString(),
              system: true,
            }])
            break
          }
          case "POLL_RESULT": {
            const pollId = String(data.pollId || "")
            const results: Record<string, number> = data.results || {}
            setPollResultsById((prev) => ({
              ...prev,
              [pollId]: { results, totalVotes: Number(data.totalVotes ?? 0) },
            }))
            break
          }
          case "BREAKOUT_ROOMS_UPDATED":
            // assignedToMe inside the event is computed for the acting user, so each
            // client re-reads its own per-viewer view over REST — UI follows server state.
            void refreshBreakoutRooms()
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

  const API_BASE = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080"

  function authHeaders(json = true): Record<string, string> {
    const h: Record<string, string> = {
      "Authorization": `Bearer ${token}`,
      "X-Institution-Id": user?.institutionId || "",
    }
    if (json) h["Content-Type"] = "application/json"
    return h
  }

  /** Server-reported failure reason (ApiResponse.error/message) or a safe fallback. */
  async function apiErrorMessage(res: Response, fallback: string): Promise<string> {
    try {
      const body = await res.json()
      return body?.error || body?.message || fallback
    } catch {
      return fallback
    }
  }

  /** Poll/quiz options may arrive as canonical JSON text or legacy "A,B,C" text. */
  function parseOptionsList(raw: string): string[] {
    if (!raw) return []
    try {
      const parsed = JSON.parse(raw)
      if (Array.isArray(parsed)) return parsed.map((v) => String(v))
    } catch {
      // legacy comma storage — fall through
    }
    return raw.split(",").map((s) => s.trim()).filter(Boolean)
  }

  // ==================== live-activity restore (server state) ====================

  async function refreshBreakoutRooms(): Promise<void> {
    if (!liveClass?.id || !token) return
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/classes/${liveClass.id}/breakout-rooms`, { headers: authHeaders(false) })
      if (!res.ok) return // keep last known state; refreshInteractiveState surfaces persistent failures
      const body = await res.json()
      const rooms: BreakoutRoomView[] = Array.isArray(body?.data) ? body.data : []
      setBreakoutRooms(rooms)
      // The teacher ended my room (or my assignment moved) while I was in it —
      // media must return to the main room rather than sit on a dead stream.
      const mine = rooms.find((r) => r.id === inBreakoutRef.current)
      if (inBreakoutRef.current && (!mine || mine.status !== "ACTIVE")) {
        void returnToMainRoom()
      }
    } catch {
      // transient — retried on the next event/reconnect
    }
  }

  /** Loads one quiz's question payload (+ my submitted state for students) from the server. */
  async function ensureQuizLoaded(quizId: string, force = false): Promise<void> {
    if (!token || !quizId) return
    if (!force && (quizLoadingRef.current.has(quizId) || quizLoadedRef.current.has(quizId))) return
    quizLoadingRef.current.add(quizId)
    setQuizUI((prev) => ({
      ...prev,
      [quizId]: {
        questions: prev[quizId]?.questions ?? [],
        answers: prev[quizId]?.answers ?? {},
        submitted: prev[quizId]?.submitted ?? false,
        loading: true,
        error: "",
      },
    }))
    try {
      const qRes = await fetch(`${API_BASE}/v1/live-session/quizzes/${quizId}/questions`, { headers: authHeaders(false) })
      if (!qRes.ok) {
        const msg = await apiErrorMessage(qRes, "Could not load quiz questions")
        setQuizUI((prev) => ({
          ...prev,
          [quizId]: { questions: [], answers: {}, submitted: false, loading: false, error: msg },
        }))
        return
      }
      const qBody = await qRes.json()
      const questions: QuizQuestion[] = Array.isArray(qBody?.data) ? qBody.data : []
      let submitted = false
      const answers: Record<string, string> = {}
      if (!isTeacherClient) {
        const mRes = await fetch(`${API_BASE}/v1/live-session/quizzes/${quizId}/my-responses`, { headers: authHeaders(false) })
        if (mRes.ok) {
          const mBody = await mRes.json()
          submitted = Boolean(mBody?.data?.submitted)
          for (const q of questions) if (q.myAnswer) answers[q.id] = String(q.myAnswer)
        }
      }
      setQuizUI((prev) => ({ ...prev, [quizId]: { questions, answers, submitted, loading: false, error: "" } }))
      quizLoadedRef.current.add(quizId)
    } catch {
      setQuizUI((prev) => ({
        ...prev,
        [quizId]: { questions: [], answers: {}, submitted: false, loading: false, error: "Could not load quiz questions — check your connection." },
      }))
    } finally {
      quizLoadingRef.current.delete(quizId)
    }
  }

  /** Teacher aggregates: answered/not-answered/correct for one quiz. */
  async function loadQuizResults(quizId: string): Promise<void> {
    if (!token || !quizId) return
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/quizzes/${quizId}/results`, { headers: authHeaders(false) })
      if (!res.ok) return // unauthorized/failed → counters stay unset, UI shows the honest waiting state
      const body = await res.json()
      const d = body?.data
      if (!d) return
      setQuizResultsById((prev) => ({
        ...prev,
        [quizId]: {
          answeredCount: Number(d.answeredCount ?? 0),
          participantCount: Number(d.participantCount ?? 0),
          totalResponses: Number(d.totalResponses ?? 0),
          totalCorrect: Number(d.totalCorrect ?? 0),
        },
      }))
    } catch {
      // transient — QUIZ_RESULT events keep the counters fresh
    }
  }

  /** Per-option poll tally (aggregate counts only). */
  async function loadPollResults(pollId: string): Promise<void> {
    if (!token || !pollId) return
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/polls/${pollId}/results`, { headers: authHeaders(false) })
      if (!res.ok) return
      const body = await res.json()
      const d = body?.data
      if (!d?.results) return
      setPollResultsById((prev) => ({ ...prev, [pollId]: { results: d.results, totalVotes: Number(d.totalVotes ?? 0) } }))
    } catch {
      // transient — POLL_RESULT events keep the tally fresh
    }
  }

  /** One restore pass per WS connection: quizzes, polls, rooms + role aggregates. */
  async function refreshInteractiveState(): Promise<void> {
    if (!liveClass?.id || !token) return
    setInteractiveLoading(true)
    setInteractiveError("")
    try {
      const [qRes, pRes, bRes] = await Promise.all([
        fetch(`${API_BASE}/v1/live-session/classes/${liveClass.id}/quizzes`, { headers: authHeaders(false) }),
        fetch(`${API_BASE}/v1/live-session/classes/${liveClass.id}/polls`, { headers: authHeaders(false) }),
        fetch(`${API_BASE}/v1/live-session/classes/${liveClass.id}/breakout-rooms`, { headers: authHeaders(false) }),
      ])
      if (!qRes.ok || !pRes.ok || !bRes.ok) {
        setInteractiveError("Couldn't load live activities — the server refused access. Rejoin the class and try again.")
        return
      }
      const [qBody, pBody, bBody] = await Promise.all([qRes.json(), pRes.json(), bRes.json()])
      const quizzes: Quiz[] = Array.isArray(qBody?.data) ? qBody.data : []
      const polls: Poll[] = Array.isArray(pBody?.data) ? pBody.data : []
      const rooms: BreakoutRoomView[] = Array.isArray(bBody?.data) ? bBody.data : []
      setActiveQuizzes(quizzes)
      setActivePolls(polls)
      setBreakoutRooms(rooms)

      const latestActiveQuiz = [...quizzes].reverse().find((q) => q.status === "ACTIVE")
      const latestAnyQuiz = quizzes.length ? quizzes[quizzes.length - 1] : null
      setFocusedQuizId((prev) =>
        prev && quizzes.some((q) => q.id === prev) ? prev : (latestActiveQuiz || latestAnyQuiz)?.id ?? null)

      // Restore my own choice for the poll currently on screen (refresh/reconnect).
      const openPoll = [...polls].reverse().find((p) => p.status === "ACTIVE")
      setSelectedPollOption(openPoll && typeof openPoll.myVote === "number" ? openPoll.myVote : null)

      const latestClosedPoll = [...polls].reverse().find((p) => p.status === "CLOSED")
      const latestClosedQuiz = [...quizzes].reverse().find((q) => q.status === "CLOSED")

      if (isTeacherClient) {
        await Promise.all([
          ...quizzes.filter((q) => q.status === "ACTIVE").map((q) => loadQuizResults(q.id)),
          ...polls.filter((p) => p.status === "ACTIVE").map((p) => loadPollResults(p.id)),
          ...(latestClosedPoll ? [loadPollResults(latestClosedPoll.id)] : []),
        ])
      } else {
        await Promise.all([
          ...quizzes
            .filter((q) => q.status === "ACTIVE" || q.id === latestClosedQuiz?.id)
            .map((q) => ensureQuizLoaded(q.id)),
          ...(latestClosedPoll ? [loadPollResults(latestClosedPoll.id)] : []),
        ])
      }
      setInteractiveReady(true)
    } catch {
      setInteractiveError("Couldn't load live activities — check your connection; they reload on every reconnect.")
    } finally {
      setInteractiveLoading(false)
    }
  }

  // ==================== teacher mutations ====================

  async function createQuiz() {
    if (!newQuizTitle.trim() || !liveClass?.id || quizQuestionList.length === 0 || actionBusy) return
    setActionBusy(true)
    setActionError("")
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/classes/${liveClass.id}/quizzes`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ title: newQuizTitle.trim(), questions: quizQuestionList }),
      })
      if (!res.ok) {
        setActionError(await apiErrorMessage(res, "Quiz could not be launched"))
        return
      }
      const body = await res.json()
      const created = body?.data
      if (created?.id) {
        // Server state in, no optimistic chat line: the QUIZ_STARTED event (which
        // reaches this client too) announces it, and the id keeps duplicates out.
        setActiveQuizzes((prev) =>
          prev.some((q) => q.id === created.id)
            ? prev
            : [...prev, { id: created.id, title: created.title, status: created.status || "ACTIVE" }])
        setFocusedQuizId(created.id)
        if (isTeacherClient) void ensureQuizLoaded(created.id)
      } else {
        await refreshInteractiveState()
      }
      setNewQuizTitle("")
      setQuizQuestionList([])
    } catch {
      setActionError("Network error — the quiz was not launched")
    } finally {
      setActionBusy(false)
    }
  }

  async function createPoll() {
    if (!newPollQuestion.trim() || !liveClass?.id || !newPollOptions.trim() || actionBusy) return
    const options = newPollOptions.split(",").map(o => o.trim()).filter(Boolean)
    if (options.length < 2) {
      setActionError("A poll needs at least two options")
      return
    }
    setActionBusy(true)
    setActionError("")
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/classes/${liveClass.id}/polls`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ question: newPollQuestion.trim(), options: JSON.stringify(options) }),
      })
      if (!res.ok) {
        setActionError(await apiErrorMessage(res, "Poll could not be launched"))
        return
      }
      const body = await res.json()
      const created = body?.data
      if (created?.id) {
        setActivePolls((prev) =>
          prev.some((p) => p.id === created.id)
            ? prev
            : [...prev, { id: created.id, question: created.question, options: created.options, status: created.status || "ACTIVE", myVote: null, totalVotes: 0 }])
        setSelectedPollOption(null)
      } else {
        await refreshInteractiveState()
      }
      setNewPollQuestion("")
      setNewPollOptions("")
    } catch {
      setActionError("Network error — the poll was not launched")
    } finally {
      setActionBusy(false)
    }
  }

  async function createBreakoutRoom() {
    if (!newBreakoutName.trim() || !liveClass?.id || actionBusy) return
    setActionBusy(true)
    setActionError("")
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/classes/${liveClass.id}/breakout-rooms`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ name: newBreakoutName.trim(), maxParticipants: 10 }),
      })
      if (!res.ok) {
        setActionError(await apiErrorMessage(res, "Breakout room could not be created"))
        return
      }
      await refreshBreakoutRooms()
      setNewBreakoutName("")
      setShowBreakoutModal(false)
    } catch {
      setActionError("Network error — the breakout room was not created")
    } finally {
      setActionBusy(false)
    }
  }

  async function startBreakoutRoom(roomId: string) {
    if (actionBusy) return
    setActionBusy(true)
    setActionError("")
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/breakout-rooms/${roomId}/start`, {
        method: "POST",
        headers: authHeaders(false),
      })
      if (!res.ok) {
        setActionError(await apiErrorMessage(res, "The room could not be opened"))
        return
      }
      await refreshBreakoutRooms()
    } catch {
      setActionError("Network error — the room was not opened")
    } finally {
      setActionBusy(false)
    }
  }

  async function endBreakoutRoom(roomId: string) {
    if (actionBusy) return
    setActionBusy(true)
    setActionError("")
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/breakout-rooms/${roomId}/end`, {
        method: "POST",
        headers: authHeaders(false),
      })
      if (!res.ok) {
        setActionError(await apiErrorMessage(res, "The room could not be closed"))
        return
      }
      await refreshBreakoutRooms()
    } catch {
      setActionError("Network error — the room was not closed")
    } finally {
      setActionBusy(false)
    }
  }

  async function assignParticipantToRoom(roomId: string, userId: string) {
    if (actionBusy || !userId) return
    setActionBusy(true)
    setActionError("")
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/breakout-rooms/${roomId}/assign`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ userId }),
      })
      if (!res.ok) {
        setActionError(await apiErrorMessage(res, "The participant could not be assigned"))
        return
      }
      await refreshBreakoutRooms()
    } catch {
      setActionError("Network error — the participant was not assigned")
    } finally {
      setActionBusy(false)
    }
  }

  async function closeQuiz(quizId: string) {
    if (actionBusy) return
    setActionBusy(true)
    setActionError("")
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/quizzes/${quizId}/close`, {
        method: "POST",
        headers: authHeaders(false),
      })
      if (!res.ok) {
        setActionError(await apiErrorMessage(res, "The quiz could not be closed"))
        return
      }
      const body = await res.json()
      const id = String(body?.data?.id || quizId)
      setActiveQuizzes((prev) => prev.map((q) => (q.id === id ? { ...q, status: "CLOSED" } : q)))
      void loadQuizResults(quizId)
    } catch {
      setActionError("Network error — the quiz was not closed")
    } finally {
      setActionBusy(false)
    }
  }

  // ==================== student actions ====================

  async function votePollOption(pollId: string, optionIndex: number) {
    if (pollVoting || selectedPollOption !== null) return
    setPollVoting(true)
    setActionError("")
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/polls/${pollId}/vote`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ optionIndex }),
      })
      if (!res.ok) {
        setActionError(await apiErrorMessage(res, "Your vote could not be recorded"))
        return
      }
      // Selected only after the server accepted the vote — never optimistic.
      setSelectedPollOption(optionIndex)
    } catch {
      setActionError("Network error — your vote was not recorded")
    } finally {
      setPollVoting(false)
    }
  }

  async function submitQuiz(quizId: string) {
    const ui = quizUI[quizId]
    if (!ui || quizSubmitting || ui.submitted) return
    const responses = ui.questions.map((q) => ({ questionId: q.id, answer: ui.answers[q.id] ?? null }))
    if (responses.length === 0) return
    setQuizSubmitting(true)
    setActionError("")
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/quizzes/${quizId}/respond`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify(responses),
      })
      if (!res.ok) {
        setActionError(await apiErrorMessage(res, "Your answers could not be submitted"))
      }
      // Reload server truth in both cases: submitted / already-submitted / closed
      // all render from what the server says, never from a local flag flip.
      await ensureQuizLoaded(quizId, true)
    } catch {
      setActionError("Network error — your answers were not submitted")
    } finally {
      setQuizSubmitting(false)
    }
  }

  async function joinBreakoutRoom(roomId: string) {
    if (actionBusy) return
    setActionBusy(true)
    setActionError("")
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/breakout-rooms/${roomId}/join`, {
        method: "POST",
        headers: authHeaders(false),
        body: JSON.stringify({}),
      })
      if (!res.ok) {
        setActionError(await apiErrorMessage(res, "You could not join this room"))
        await refreshBreakoutRooms()
        return
      }
      const body = await res.json()
      const d = body?.data || {}
      setInBreakout({ roomId, roomName: String(d.roomName || "breakout room") })
      if (d.liveKitAvailable && d.liveKitToken && d.liveKitUrl) {
        // Room switch: swapping the token tears down the main room and connects
        // the breakout room through the existing LiveKit effect (no new socket).
        setServiceMode("full")
        setRoomName(String(d.breakoutLiveKitRoom || d.roomName || ""))
        setLiveKitToken(d.liveKitToken)
        setLiveKitUrl(d.liveKitUrl)
      }
      setChat(prev => [...prev, {
        userId: "system",
        userName: "System",
        message: `You joined breakout room "${d.roomName || ""}"`,
        timestamp: new Date().toISOString(),
        system: true,
      }])
      await refreshBreakoutRooms()
    } catch {
      setActionError("Network error — you did not join the room")
    } finally {
      setActionBusy(false)
    }
  }

  /** Back to the main room: re-fetches a fresh main-room token (cached one may have expired). */
  async function returnToMainRoom() {
    setInBreakout(null)
    setActionError("")
    setRoomState("connecting")
    setServiceMode("unknown")
    setLiveKitToken(null)
    try {
      const res = await fetch(`/v1/live-session/join/${liveClass.id}`, {
        method: "POST",
        headers: authHeaders(),
      })
      const data = await res.json()
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
    } catch {
      setServiceMode("chat-only")
      setRoomState("error")
      setConnected(true)
    }
  }

  // Keep student question payloads and teacher result counters in sync with the
  // server-reported quiz list (covers QUIZ_STARTED events and restores).
  useEffect(() => {
    if (!isInProgress || !token) return
    if (isTeacherClient) {
      for (const q of activeQuizzes) {
        if (q.status === "ACTIVE" && !quizResultsById[q.id]) void loadQuizResults(q.id)
      }
    } else {
      for (const q of activeQuizzes) {
        if (q.status === "ACTIVE" || q.status === "CLOSED") void ensureQuizLoaded(q.id)
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeQuizzes, isInProgress, token, isTeacherClient, quizResultsById])

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

      {actionError && (
        <div className="mb-3 rounded-lg border border-red-200 bg-red-50 p-2 text-xs text-red-700 flex items-center justify-between">
          <span>{actionError}</span>
          <button onClick={() => setActionError("")} className="text-red-500 hover:text-red-700"><XCircle className="size-3.5" /></button>
        </div>
      )}

      {interactiveLoading && !interactiveReady && (
        <div className="mb-3 rounded-lg border border-border bg-card p-2 text-xs text-muted-foreground">
          Loading live activities…
        </div>
      )}

      {interactiveError && (
        <div className="mb-3 rounded-lg border border-amber-200 bg-amber-50 p-2 text-xs text-amber-700 flex items-center justify-between">
          <span>{interactiveError}</span>
          <button onClick={() => { interactiveLoadedRef.current = false; void refreshInteractiveState() }} className="font-medium text-amber-600 underline">Retry</button>
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

          {(() => {
            if (!isInProgress || activePolls.length === 0) return null
            const active = activePolls.filter(p => p.status === "ACTIVE")
            const poll = active.length > 0 ? active[active.length - 1] : activePolls[activePolls.length - 1]
            const isActive = poll.status === "ACTIVE"
            const options = parseOptionsList(poll.options)
            const tally = pollResultsById[poll.id]
            const myVote = typeof poll.myVote === "number" ? poll.myVote : selectedPollOption
            const showCounts = Boolean(tally) && (!isActive || isTeacherClient || myVote !== null)
            return (
              <div className="rounded-xl border border-blue-200 bg-blue-50 p-4 shadow-sm">
                <h2 className="text-xs font-semibold text-blue-700 flex items-center gap-1 mb-2">
                  <ClipboardList className="size-3" /> {isActive ? "Active Poll" : "Poll closed"}
                </h2>
                <p className="text-xs font-medium text-blue-900 mb-2">{poll.question}</p>
                <div className="space-y-1.5">
                  {options.map((opt, i) => {
                    const count = tally?.results?.[String(i)]
                    const total = tally?.totalVotes ?? 0
                    const pct = tally && total > 0 && typeof count === "number" ? Math.round((count / total) * 100) : null
                    const canVote = isActive && !isTeacherClient && myVote === null && !pollVoting
                    const chosen = myVote === i
                    return (
                      <button
                        key={i}
                        onClick={canVote ? () => void votePollOption(poll.id, i) : undefined}
                        disabled={!canVote}
                        className={cn(
                          "relative w-full overflow-hidden rounded-lg border px-3 py-1.5 text-left text-xs transition-colors",
                          canVote ? "border-blue-200 bg-white text-blue-700 hover:bg-blue-100" : "border-blue-200 bg-white/70",
                          chosen && "border-blue-400 bg-blue-100 text-blue-800 font-medium",
                        )}
                      >
                        {showCounts && pct !== null && (
                          <span className="absolute inset-y-0 left-0 bg-blue-200/60" style={{ width: `${pct}%` }} aria-hidden />
                        )}
                        <span className="relative flex items-center justify-between gap-2">
                          <span>{opt}</span>
                          <span className="text-[10px] text-blue-600">
                            {chosen ? "✓ " : ""}
                            {showCounts && typeof count === "number" ? `${count} (${pct ?? 0}%)` : ""}
                          </span>
                        </span>
                      </button>
                    )
                  })}
                </div>
                {!isActive && (
                  <p className="mt-2 text-[10px] text-blue-700">
                    {tally ? `Final result · ${tally.totalVotes} vote${tally.totalVotes === 1 ? "" : "s"}` : "Poll closed — voting has ended."}
                  </p>
                )}
                {isActive && isTeacherClient && (
                  <p className="mt-2 text-[10px] text-blue-700">
                    {tally ? `Live results · ${tally.totalVotes} vote${tally.totalVotes === 1 ? "" : "s"}` : "Waiting for votes…"}
                  </p>
                )}
                {isActive && !isTeacherClient && myVote === null && pollVoting && (
                  <p className="mt-2 text-[10px] text-blue-600">Recording your vote…</p>
                )}
                {isActive && !isTeacherClient && myVote !== null && (
                  <p className="mt-2 text-[10px] text-blue-700">✓ Your response has been submitted.</p>
                )}
              </div>
            )
          })()}

          {isInProgress && !isTeacherClient && (() => {
            const relevant = activeQuizzes.filter(q => q.status === "ACTIVE" || q.status === "CLOSED")
            if (relevant.length === 0) return null
            const focus = focusedQuizId && relevant.some(q => q.id === focusedQuizId)
              ? focusedQuizId
              : (relevant[relevant.length - 1]?.id ?? null)
            const quiz = relevant.find(q => q.id === focus)
            if (!quiz) return null
            const ui = quizUI[quiz.id]
            const isActive = quiz.status === "ACTIVE"
            const allAnswered = Boolean(ui && ui.questions.length > 0 && ui.questions.every(q => typeof ui.answers[q.id] === "string"))
            return (
              <div className={cn("rounded-xl border p-4 shadow-sm", isActive ? "border-teal-200 bg-teal-50" : "border-border bg-card")}>
                <h2 className={cn("mb-1 flex items-center gap-1 text-xs font-semibold", isActive ? "text-teal-700" : "text-foreground")}>
                  <ListOrdered className="size-3" /> Live Quiz {isActive ? "" : "· closed"}
                </h2>
                {relevant.length > 1 && (
                  <div className="mb-2 flex flex-wrap gap-1">
                    {relevant.map(q => (
                      <button
                        key={q.id}
                        onClick={() => setFocusedQuizId(q.id)}
                        className={cn(
                          "rounded border px-1.5 py-0.5 text-[10px]",
                          q.id === quiz.id ? "border-teal-400 bg-teal-100 text-teal-800" : "border-border text-muted-foreground",
                        )}
                      >
                        {q.title || "Untitled"}
                      </button>
                    ))}
                  </div>
                )}
                <p className="mb-2 text-xs font-medium text-foreground">{quiz.title || "Untitled quiz"}</p>
                {ui?.loading && <p className="text-[10px] text-muted-foreground">Loading questions…</p>}
                {ui?.error && <p className="text-[10px] text-red-600">{ui.error}</p>}
                {ui && !ui.loading && !ui.error && ui.questions.length === 0 && (
                  <p className="text-[10px] text-muted-foreground">This quiz has no questions yet.</p>
                )}
                {ui && !ui.loading && ui.questions.length > 0 && (
                  <div className="space-y-3">
                    {ui.questions.map((q, qi) => {
                      const opts = parseOptionsList(q.options)
                      const canAnswer = isActive && !ui.submitted && !quizSubmitting
                      return (
                        <div key={q.id} className="rounded-lg border border-teal-100 bg-white p-2">
                          <p className="text-[11px] font-medium text-foreground">Q{qi + 1}. {q.questionText}</p>
                          <div className="mt-1.5 space-y-1">
                            {opts.map((opt) => {
                              const chosen = ui.answers[q.id] === opt
                              return (
                                <button
                                  key={opt}
                                  disabled={!canAnswer}
                                  onClick={() => setQuizUI(prev => ({
                                    ...prev,
                                    [quiz.id]: { ...prev[quiz.id], answers: { ...prev[quiz.id].answers, [q.id]: opt } },
                                  }))}
                                  className={cn(
                                    "w-full rounded border px-2 py-1 text-left text-[11px] transition-colors",
                                    chosen ? "border-teal-400 bg-teal-100 text-teal-800 font-medium" : "border-teal-100 bg-white text-foreground hover:bg-teal-50",
                                  )}
                                >
                                  {opt}
                                </button>
                              )
                            })}
                          </div>
                          {ui.submitted && (
                            <p className="mt-1 text-[10px] text-teal-700">
                              Your answer: {ui.answers[q.id] ?? q.myAnswer ?? "—"}
                              {!isActive && typeof q.isCorrect === "boolean" && (q.isCorrect ? " · ✓ Correct" : " · ✗ Incorrect")}
                            </p>
                          )}
                        </div>
                      )
                    })}
                    {isActive && !ui.submitted && (
                      <>
                        <Button
                          size="sm"
                          className="h-7 w-full text-[10px]"
                          disabled={quizSubmitting || !allAnswered}
                          onClick={() => void submitQuiz(quiz.id)}
                        >
                          {quizSubmitting ? "Submitting…" : "Submit answers"}
                        </Button>
                        {!allAnswered && (
                          <p className="text-[10px] text-muted-foreground">Answer every question to submit.</p>
                        )}
                      </>
                    )}
                    {ui.submitted && (
                      <p className="text-[10px] font-medium text-teal-700">✓ Your response has been submitted.</p>
                    )}
                    {!isActive && !ui.submitted && (
                      <p className="text-[10px] text-muted-foreground">This quiz is closed — submissions are no longer accepted.</p>
                    )}
                  </div>
                )}
              </div>
            )
          })()}

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
                disabled={actionBusy || !newQuizTitle.trim() || quizQuestionList.length === 0}
                onClick={createQuiz}
              >
                Launch Quiz ({quizQuestionList.length} questions)
              </Button>
            </div>
          )}

          {isInProgress && user?.role === "Teacher" && (() => {
            const actives = activeQuizzes.filter(q => q.status === "ACTIVE")
            const lastClosed = [...activeQuizzes].reverse().find(q => q.status === "CLOSED")
            const list = lastClosed && !actives.some(q => q.id === lastClosed.id) ? [...actives, lastClosed] : actives
            if (list.length === 0) return null
            return (
              <div className="rounded-xl border border-border bg-card p-4 shadow-sm space-y-2">
                <h2 className="text-xs font-semibold text-foreground flex items-center gap-1">
                  <Zap className="size-3" /> Live Quizzes
                </h2>
                {list.map(q => {
                  const r = quizResultsById[q.id]
                  const isActive = q.status === "ACTIVE"
                  const accuracy = r && r.totalResponses > 0 ? Math.round((r.totalCorrect / r.totalResponses) * 100) : 0
                  return (
                    <div key={q.id} className="rounded-lg border border-border p-2 space-y-1">
                      <p className="text-[11px] font-medium text-foreground">
                        {q.title || "Untitled quiz"}
                        {!isActive && <span className="ml-1 text-muted-foreground">· closed</span>}
                      </p>
                      <p className="text-[10px] text-muted-foreground">
                        {r
                          ? `Answered ${r.answeredCount}/${r.participantCount || r.answeredCount} · Correct ${r.totalCorrect}/${r.totalResponses}${r.totalResponses > 0 ? ` (${accuracy}%)` : ""}`
                          : "Waiting for responses…"}
                      </p>
                      {isActive ? (
                        <div className="flex gap-1">
                          <Button size="sm" variant="outline" className="h-5 flex-1 text-[10px]" disabled={actionBusy} onClick={() => void loadQuizResults(q.id)}>
                            Refresh
                          </Button>
                          <Button size="sm" variant="ghost" className="h-5 flex-1 text-[10px] text-destructive" disabled={actionBusy} onClick={() => void closeQuiz(q.id)}>
                            Close quiz
                          </Button>
                        </div>
                      ) : (
                        <Button size="sm" variant="outline" className="h-5 w-full text-[10px]" disabled={actionBusy} onClick={() => void loadQuizResults(q.id)}>
                          View results
                        </Button>
                      )}
                    </div>
                  )
                })}
              </div>
            )
          })()}

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
                disabled={actionBusy || !newPollQuestion.trim() || !newPollOptions.trim()}
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
              {inBreakout && (
                <div className="mb-2 space-y-1 rounded-lg border border-teal-200 bg-teal-50 p-2 text-[10px] text-teal-800">
                  <p className="font-medium">In breakout room: {inBreakout.roomName}</p>
                  <Button size="sm" variant="outline" className="h-5 text-[10px]" onClick={() => void returnToMainRoom()}>
                    Return to main room
                  </Button>
                </div>
              )}
              {breakoutRooms.length === 0 ? (
                <p className="text-[10px] text-muted-foreground">No breakout rooms yet</p>
              ) : (
                <div className="space-y-1.5">
                  {breakoutRooms.map(room => {
                    const assigned = room.assignedUsers || []
                    // §33: a student assigned elsewhere may only join their own room.
                    const iAmAssignedElsewhere = room.assignedToMe ? false : breakoutRooms.some(r => r.assignedToMe)
                    const canJoin = !isTeacherClient && room.status === "ACTIVE" && !iAmAssignedElsewhere
                    return (
                      <div key={room.id} className="space-y-1.5 rounded-lg border border-border px-3 py-2 text-xs">
                        <div className="flex items-center gap-2">
                          <div className={cn(
                            "size-2 rounded-full",
                            room.status === "ACTIVE" ? "bg-teal animate-pulse" : room.status === "ENDED" ? "bg-gray-400" : "bg-amber-400"
                          )} />
                          <span className="flex-1 font-medium text-foreground">{room.name}</span>
                          <span className="text-[10px] text-muted-foreground">{room.assignedCount ?? assigned.length}/{room.maxParticipants} assigned</span>
                          {user?.role === "Teacher" && (
                            <div className="flex gap-1">
                              {room.status === "WAITING" && (
                                <Button size="sm" variant="ghost" className="h-5 text-[10px] text-teal" disabled={actionBusy} onClick={() => void startBreakoutRoom(room.id)}>Start</Button>
                              )}
                              {room.status === "ACTIVE" && (
                                <Button size="sm" variant="ghost" className="h-5 text-[10px] text-destructive" disabled={actionBusy} onClick={() => void endBreakoutRoom(room.id)}>End</Button>
                              )}
                            </div>
                          )}
                          {canJoin && (
                            <Button
                              size="sm"
                              variant="ghost"
                              className="h-5 text-[10px] text-teal"
                              disabled={actionBusy}
                              onClick={() => void joinBreakoutRoom(room.id)}
                            >{room.assignedToMe ? "Join room" : "Join"}</Button>
                          )}
                        </div>
                        {!isTeacherClient && room.assignedToMe && room.status === "WAITING" && (
                          <p className="text-[10px] text-amber-600">You are assigned here — waiting for the teacher to open it.</p>
                        )}
                        {!isTeacherClient && room.assignedToMe && room.status === "ENDED" && (
                          <p className="text-[10px] text-muted-foreground">This room has ended.</p>
                        )}
                        {!isTeacherClient && iAmAssignedElsewhere && (
                          <p className="text-[10px] text-muted-foreground">You are assigned to another room.</p>
                        )}
                        {assigned.length > 0 && (
                          <div className="flex flex-wrap gap-1">
                            {assigned.map(a => (
                              <span key={a.userId} className="rounded bg-muted px-1.5 py-0.5 text-[10px] text-foreground">{a.userName}</span>
                            ))}
                          </div>
                        )}
                        {user?.role === "Teacher" && room.status !== "ENDED" && (
                          <select
                            className="h-6 w-full rounded border border-border bg-muted/60 px-1 text-[10px] outline-none"
                            value=""
                            disabled={actionBusy}
                            onChange={e => {
                              const targetId = e.target.value
                              if (targetId) void assignParticipantToRoom(room.id, targetId)
                            }}
                          >
                            <option value="">Assign participant…</option>
                            {participants
                              .filter(p => p.role !== "TEACHER" && !assigned.some(a => a.userId === p.userId))
                              .map(p => (
                                <option key={p.userId} value={p.userId}>{p.userName}</option>
                              ))}
                          </select>
                        )}
                      </div>
                    )
                  })}
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
                <Button size="sm" className="h-8 text-xs" onClick={createBreakoutRoom} disabled={actionBusy || !newBreakoutName.trim()}>Create</Button>
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
