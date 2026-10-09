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
  Square,
  Loader2,
} from "lucide-react"
import type { LiveClass } from "@/lib/learner-api"
import { useTranslations } from "next-intl"
import { learnerApi } from "@/lib/learner-api"
import { Button } from "@/components/ui/button"
import { cn } from "@/lib/utils"
import { useAuth } from "@/lib/auth"
import { Room, RoomEvent, Track, Participant as LKParticipant, TrackPublication } from "livekit-client"
import { LiveVideoPlayer, type LivePlayerState } from "@/components/live/live-video-player"
import { useMediaDevices } from "@/hooks/use-media-devices"

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
  /** Q&A workflow: ISO timestamp when a teacher marked the question answered. */
  answeredAt?: string | null
}

/** Reactions the room offers. Every one is broadcast by the server as a REACTION event. */
const REACTION_EMOJI = ["👍", "❤️"] as const

/** Host-continuity snapshot returned by GET /v1/live-session/classes/{id}/host-state. */
interface HostState {
  classId: string
  status: string
  active: boolean
  startedAt: string | null
  scheduledAt: string | null
  durationMinutes: number | null
  recordingEnabled: boolean | null
  recordingActive: boolean
  recordingEgressId: string | null
  recordingAvailable: boolean
  recordingUrl: string | null
  liveKitAvailable: boolean
  liveKitUrl: string | null
  roomName: string
  presentParticipants: number
  canEndSession: boolean
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
  const t = useTranslations("live")
  const tc = useTranslations("common")
  const ts = useTranslations("status")
  const te = useTranslations("events")
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
  const [localStream, setLocalStream] = useState<MediaStream | null>(null)
  const [screenStream, setScreenStream] = useState<MediaStream | null>(null)
  const [serviceMode, setServiceMode] = useState<"full" | "chat-only" | "unknown">("unknown")
  const [liveKitToken, setLiveKitToken] = useState<string | null>(null)
  const [liveKitUrl, setLiveKitUrl] = useState<string | null>(null)
  const [roomName, setRoomName] = useState<string | null>(null)
  const [remoteParticipants, setRemoteParticipants] = useState<Map<string, LKParticipant>>(new Map())
  const [remoteVideoTrack, setRemoteVideoTrack] = useState<TrackPublication | null>(null)
  // Screen share is a separate publication from the camera so it can own the stage.
  const [remoteScreenTrack, setRemoteScreenTrack] = useState<TrackPublication | null>(null)
  const [remoteAudioTrack, setRemoteAudioTrack] = useState<TrackPublication | null>(null)
  const [sessionStatus, setSessionStatus] = useState(liveClass.status)
  // Broadcast source + real device selection: the laptop camera is ONE source
  // among phone/USB/professional-camera/OBS/encoder/studio sources. Devices come
  // from the browser's enumerateDevices — never hardcoded names.
  const [broadcastSource, setBroadcastSource] = useState<string>(liveClass.broadcastSource || "BROWSER")
  const { cameras, microphones, hasLabels, requestPermission } = useMediaDevices()
  const [selectedCameraId, setSelectedCameraId] = useState("")
  const [selectedMicId, setSelectedMicId] = useState("")
  const [sourceBusy, setSourceBusy] = useState(false)
  const [sourceError, setSourceError] = useState("")
  const [roomState, setRoomState] = useState<"idle" | "connecting" | "connected" | "reconnecting" | "error">("idle")
  const [showIssueModal, setShowIssueModal] = useState(false)
  const [issueType, setIssueType] = useState("CONNECTION_PROBLEM")
  const [issueDescription, setIssueDescription] = useState("")
  const [issueSubmitting, setIssueSubmitting] = useState(false)
  const [issueSent, setIssueSent] = useState(false)
  const [isRecording, setIsRecording] = useState(false)
  const [recordingEgressId, setRecordingEgressId] = useState<string | null>(null)
  const [recordingBusy, setRecordingBusy] = useState(false)
  const [showMaterialInput, setShowMaterialInput] = useState(false)
  const [materialName, setMaterialName] = useState("")
  const [materialUrl, setMaterialUrl] = useState("")
  const [attachedMaterials, setAttachedMaterials] = useState<Array<{name: string; url: string}>>([])
  const [handRaiseQueue, setHandRaiseQueue] = useState<HandRaiseEntry[]>([])
  
  // Reactions are server-broadcast ephemeral events (never persisted chat): counts here are
  // the real room-wide tally, not a local per-client guess.
  const [reactions, setReactions] = useState<Record<string, number>>({})
  const [lastReaction, setLastReaction] = useState<{ emoji: string; count: number; at: number; fromMe: boolean } | null>(null)
  // Server-authoritative snapshot of this class for host continuity (restored on mount).
  const [hostState, setHostState] = useState<HostState | null>(null)
  const issueDialogRef = useRef<HTMLDivElement | null>(null)
  // Move focus into the issue dialog when it opens.
  useEffect(() => {
    if (showIssueModal) {
      issueDialogRef.current?.focus()
    }
  }, [showIssueModal])
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
  // Authoritative live-start anchor (epoch ms), set only from server-recorded
  // timestamps via applySessionStart — never from page-load/mount time.
  const [sessionStartMs, setSessionStartMs] = useState<number | null>(null)
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null)
  const retryCountRef = useRef(0)
  // LiveKit's reconnect budget is separate from the classroom WS budget: a dead
  // signal server must not silently consume (or be reset by) chat retries.
  const lkRetryCountRef = useRef(0)
  const lkReconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null)
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
    const diff = Math.max(0, Math.floor((Date.now() - startTimeRef.current.getTime()) / 1000))
    const h = String(Math.floor(diff / 3600)).padStart(2, "0")
    const m = String(Math.floor((diff % 3600) / 60)).padStart(2, "0")
    const s = String(diff % 60).padStart(2, "0")
    return `${h}:${m}:${s}`
  }, [])

  // §009/§010: elapsed ALWAYS derives from the authoritative session-start
  // timestamp recorded by the server (earliest participant joinedAt — the WS
  // JOIN handler rejects unless the class is IN_PROGRESS/LIVE, so the first
  // join ≈ the moment the session went live). Monotonic-min keeps refresh,
  // reconnect and remount on the same anchor; it is never anchored to
  // page-load time, so a refresh continues instead of restarting at 00:00:00.
  const applySessionStart = useCallback((iso: string | null | undefined) => {
    if (!iso) return
    const ms = new Date(iso).getTime()
    if (Number.isNaN(ms)) return
    setSessionStartMs(prev => (prev == null || ms < prev ? ms : prev))
  }, [])

  useEffect(() => {
    startTimeRef.current = sessionStartMs != null ? new Date(sessionStartMs) : null
  }, [sessionStartMs])

  useEffect(() => {
    if (!isInProgress) {
      setElapsed("00:00:00")
      return
    }
    setElapsed(getElapsed())
    const interval = setInterval(() => setElapsed(getElapsed()), 1000)
    return () => clearInterval(interval)
  }, [isInProgress, sessionStartMs, getElapsed])

  useEffect(() => {
    // Scroll ONLY the chat panel, never the page: scrollIntoView walks every
    // scrollable ancestor (including the window), which pushed the player's
    // top-left/top-right broadcast overlays above the viewport.
    const end = chatEndRef.current
    const box = end?.parentElement
    if (box) box.scrollTo({ top: box.scrollHeight, behavior: "smooth" })
  }, [chat])

  useEffect(() => {
    if (!isInProgress || !liveKitToken || !liveKitUrl || serviceMode !== "full") return

    setRoomState("connecting")
    // Fresh room (initial join, breakout switch, or Retry) gets a fresh budget.
    lkRetryCountRef.current = 0
    // A token swap tears this room down and builds a fresh one (main room →
    // breakout → back). Remote publications belong to the previous room, so they
    // must not linger on the stage while the new room connects.
    setRemoteVideoTrack(null)
    setRemoteScreenTrack(null)
    setRemoteAudioTrack(null)
    setRemoteParticipants(new Map())
    const room = new Room({
      adaptiveStream: true,
      dynacast: true,
    })
    roomRef.current = room

    // Bounded LiveKit retry loop. Failed attempts used to be swallowed with an
    // empty catch, which left the player stuck on "Reconnecting to live session..."
    // forever; every failure now either schedules the next attempt or surfaces the
    // error state with the Retry action. `disposed` kills any in-flight schedule
    // when this effect tears down (token swap), so a stale timer can never
    // reconnect the new room with the old token.
    let disposed = false
    const scheduleLiveKitReconnect = () => {
      if (disposed) return
      if (lkReconnectTimeoutRef.current) clearTimeout(lkReconnectTimeoutRef.current)
      if (!isInProgress || lkRetryCountRef.current >= 10) {
        setReconnecting(false)
        setRoomState("error")
        return
      }
      setReconnecting(true)
      setRoomState("reconnecting")
      const delay = Math.min(3000 * Math.pow(1.5, lkRetryCountRef.current), 30000)
      lkRetryCountRef.current++
      lkReconnectTimeoutRef.current = setTimeout(() => {
        if (disposed || !roomRef.current || !liveKitUrl) return
        // Participant tokens are short-lived (15 min), while a live class can run far
        // longer. Reconnecting with the original token fails once it has expired, which
        // would end a long session for good — so every attempt obtains a fresh token
        // from the same join endpoint the room was opened with.
        const room = roomRef.current
        refreshLiveKitCredentials()
          .then(creds => {
            if (disposed || !creds) return
            if (creds.token) setLiveKitToken(creds.token)
            return room.connect(creds.url || liveKitUrl, creds.token || liveKitToken)
          })
          .catch(() => scheduleLiveKitReconnect())
      }, delay)
    }

    room.on(RoomEvent.Connected, () => {
      setConnected(true)
      setReconnecting(false)
      setRoomState("connected")
      lkRetryCountRef.current = 0
      if (lkReconnectTimeoutRef.current) clearTimeout(lkReconnectTimeoutRef.current)
      // Re-publish local camera/mic captured in a previous room (breakout join or
      // return switches rooms without touching the media stream).
      const stream = mediaStateRef.current
      if (stream) {
        stream.getTracks().forEach((track) => {
          const source = track.kind === "video" ? Track.Source.Camera : Track.Source.Microphone
          if (room.localParticipant.getTrackPublication(source)) return
          room.localParticipant
            .publishTrack(track, { name: track.kind === "video" ? "camera" : "microphone" })
            .catch(() => {})
        })
      }
    })

    room.on(RoomEvent.Disconnected, () => {
      scheduleLiveKitReconnect()
    })

    room.on(RoomEvent.ParticipantConnected, (participant: LKParticipant) => {
      setRemoteParticipants(prev => new Map(prev).set(participant.identity, participant))
      participant.on(RoomEvent.TrackSubscribed, (_track: any, pub: TrackPublication) => {
        if (!isClassroomSource(participant.identity)) return
        // Camera and screen share are tracked separately so a screen share actually takes
        // the stage instead of fighting the camera publication for one slot.
        if (pub.kind === Track.Kind.Video) {
          if (pub.source === Track.Source.ScreenShare) setRemoteScreenTrack(pub)
          else setRemoteVideoTrack(pub)
        }
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
      setRemoteScreenTrack(prev => {
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
      if (pub.kind === Track.Kind.Video) {
        if (pub.source === Track.Source.ScreenShare) setRemoteScreenTrack(pub)
        else setRemoteVideoTrack(pub)
      }
      if (pub.kind === Track.Kind.Audio) setRemoteAudioTrack(pub)
    })

    room.on(RoomEvent.TrackUnsubscribed, (_track: any, pub: TrackPublication) => {
      setRemoteVideoTrack(prev => (prev === pub ? null : prev))
      setRemoteScreenTrack(prev => (prev === pub ? null : prev))
      setRemoteAudioTrack(prev => (prev === pub ? null : prev))
    })

    room.connect(liveKitUrl, liveKitToken).catch(err => {
      console.error("LiveKit connection failed:", err)
      scheduleLiveKitReconnect()
    })

    return () => {
      disposed = true
      if (lkReconnectTimeoutRef.current) clearTimeout(lkReconnectTimeoutRef.current)
      room.disconnect()
      roomRef.current = null
    }
    // roomName: isClassroomSource reads it to relax source gating inside breakout rooms.
  }, [isInProgress, liveKitToken, liveKitUrl, serviceMode, roomName])

  // Status polling. Runs for SCHEDULED too (only ENDED stops it): a learner
  // already sitting in the classroom must see the session flip to LIVE when
  // the teacher starts it — the WS cannot (JOIN is rejected pre-start) and
  // there is no other push channel, so without this read the page would stay
  // "scheduled" until a manual refresh.
  useEffect(() => {
    if (sessionEnded) return
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
  }, [sessionEnded, liveClass.id, sessionStatus])

  // Host continuity: on mount, ask the server for the real state of this class. This is what
  // makes "host leaves and returns" resume the SAME session instead of guessing from local
  // component state (which always resets). Recording state in particular lives on the server,
  // so a host that reopens the room sees the true recording status.
  useEffect(() => {
    if (!token || !user || !liveClass?.id) return
    if (!isTeacherClient && user?.role !== "Admin") return
    let cancelled = false
    const load = async () => {
      try {
        const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE || ""}/v1/live-session/classes/${liveClass.id}/host-state`, {
          headers: {
            Authorization: `Bearer ${token}`,
            "X-Institution-Id": user?.institutionId || "",
          },
        })
        if (!res.ok || cancelled) return
        const body = await res.json()
        const state = body?.data as HostState | undefined
        if (!state || cancelled) return
        setHostState(state)
        setIsRecording(Boolean(state.recordingActive))
        setRecordingEgressId(state.recordingEgressId ?? null)
        if (state.startedAt) applySessionStart(state.startedAt)
        if (!state.active) setSessionStatus("COMPLETED")
      } catch {
        // Host state is an enhancement; a failure must not block entering the room.
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [token, user, liveClass?.id, isTeacherClient, applySessionStart])

  // Authoritative elapsed anchor: earliest server-recorded participant join,
  // read once per live session through the EXISTING participants endpoint
  // (full history — leavers keep their original joinedAt, rejoin preserves it).
  // Any earlier anchor already seen from the WS stays (monotonic min).
  useEffect(() => {
    if (!isInProgress) {
      setSessionStartMs(null)
      return
    }
    let cancelled = false
    learnerApi
      .getLiveParticipants(liveClass.id)
      .then(list => {
        if (cancelled || !Array.isArray(list)) return
        for (const p of list) applySessionStart(p.joinedAt)
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [isInProgress, liveClass.id, applySessionStart])

  // A host who closes the tab or navigates away mid-session must be warned: losing the room
  // silently drops the recording/egress control with it. Learners keep the normal behaviour,
  // because re-entering a live class is an everyday action for them.
  useEffect(() => {
    if (!isInProgress || !(isTeacherClient || user?.role === "Admin")) return
    const warn = (e: BeforeUnloadEvent) => {
      e.preventDefault()
      e.returnValue = ""
    }
    window.addEventListener("beforeunload", warn)
    return () => window.removeEventListener("beforeunload", warn)
  }, [isInProgress, isTeacherClient, user?.role])

/**
   * Fetches a fresh LiveKit participant token for this class from the existing join
   * endpoint. Shared by the initial join, the breakout switch, the Retry action and
   * every reconnect attempt, so no path can run on a stale (expired) token.
   */
  const refreshLiveKitCredentials = useCallback(async (): Promise<{ token: string | null; url: string | null } | null> => {
    if (!token || !user) return null
    try {
      const res = await fetch(`/v1/live-session/join/${liveClass.id}`, {
        method: "POST",
        headers: {
          "Authorization": `Bearer ${token}`,
          "X-Institution-Id": user?.institutionId || "",
          "Content-Type": "application/json",
        },
      })
      if (!res.ok) return null
      const data = await res.json()
      const payload = data?.data
      if (!payload?.liveKitAvailable) return null
      setServiceMode("full")
      setLiveKitToken(payload.liveKitToken ?? null)
      setLiveKitUrl(payload.liveKitUrl ?? null)
      setRoomName(payload.roomName ?? null)
      return { token: payload.liveKitToken ?? null, url: payload.liveKitUrl ?? null }
    } catch {
      return null
    }
  }, [token, user, liveClass.id])

  useEffect(() => {
    if (!isInProgress || !token || !user) return

    function fetchLiveKitToken() {
      refreshLiveKitCredentials().then(creds => {
        if (creds) return
        setServiceMode("chat-only")
        setRoomState("error")
        setConnected(true)
      })
    }

    function connect() {
      const protocol = window.location.protocol === "https:" ? "wss:" : "ws:"
      const wsHost = process.env.NEXT_PUBLIC_WS_HOST || window.location.hostname
      const wsPort = process.env.NEXT_PUBLIC_WS_PORT || "8080"
      const wsUrl = `${protocol}//${wsHost}:${wsPort}/ws/live-class/${liveClass.id}`

      // Authenticate the socket through the subprotocol list instead of the URL, so the JWT does
      // not end up in proxy/access logs or browser history. The server still accepts the
      // legacy ?token= form for older clients.
      const ws = new WebSocket(wsUrl, ["elmkusoma.jwt.v1", `bearer.${token ?? ""}`])
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
            applySessionStart(data.timestamp)
            setParticipants((prev) => {
              if (prev.some((p) => p.userId === data.userId)) return prev
              return [...prev, { userId: data.userId, userName: data.userName, role: data.role || "LEARNER", joinedAt: data.timestamp }]
            })
            if (data.userId !== myUserId) {
              setChat((prev) => [...prev, {
                userId: "system",
                userName: t("systemSender"),
                message: t("userJoinedMsg", { name: data.userName }),
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
                userName: t("systemSender"),
                message: t("userLeftMsg", { name: data.userName }),
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
              for (const p of data.participants) applySessionStart(p.joinedAt)
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
            setChat((prev) => [...prev, { id: data.id ?? data.messageId ?? null, userId: data.userId, userName: data.userName, message: isQa ? raw.slice(6).trim() : raw, timestamp: data.timestamp, kind: isQa ? "QA" : "CHAT", answeredAt: data.answeredAt ?? null }])
            break
          }
          case "REACTION": {
            // Real reaction broadcast from the server (REACTION message type). Reactions are
            // ephemeral and never enter the chat transcript; the badge count is authoritative
            // for everyone in the room because the server echoes the event to all clients.
            const emoji = String(data.message || "")
            if (!emoji) break
            const fromMe = String(data.userId || "") === myUserId
            setReactions((prev) => ({ ...prev, [emoji]: (prev[emoji] || 0) + 1 }))
            setLastReaction((prev) =>
              prev && Date.now() - prev.at < 2500 && prev.emoji === emoji
                ? { ...prev, count: prev.count + 1, at: Date.now(), fromMe: prev.fromMe || fromMe }
                : { emoji, count: 1, at: Date.now(), fromMe },
            )
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
                answeredAt: m.answeredAt ?? null,
              }))
              setChat((prev) => [...history, ...prev])
            }
            break
          case "QA_ANSWERED":
            if (data.messageId) {
              setChat((prev) =>
                prev.map((m) =>
                  m.id === data.messageId
                    ? { ...m, answeredAt: data.answered ? data.timestamp || new Date().toISOString() : null }
                    : m,
                ),
              )
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
              userName: t("systemSender"),
              message: t("handRaisedMsg", { name: data.userName }),
              timestamp: data.timestamp,
              system: true,
            }])
            break
          case "HAND_LOWERED":
            setChat((prev) => [...prev, {
              userId: "system",
              userName: t("systemSender"),
              message: t("handLoweredMsg", { name: data.userName }),
              timestamp: data.timestamp,
              system: true,
            }])
            break
          case "SCREEN_SHARE_STARTED":
            setChat((prev) => [...prev, {
              userId: "system",
              userName: t("systemSender"),
              message: t("screenShareStartedMsg", { name: data.userName }),
              timestamp: data.timestamp,
              system: true,
            }])
            break
          case "SCREEN_SHARE_STOPPED":
            setChat((prev) => [...prev, {
              userId: "system",
              userName: t("systemSender"),
              message: t("screenShareStoppedMsg", { name: data.userName }),
              timestamp: data.timestamp,
              system: true,
            }])
            break
          case "SOURCE_CHANGED": {
            // The teacher switched source/device mid-session (camera → USB camera,
            // camera → screen, browser → OBS ingest...). Update the badge and leave
            // a visible trail; the stage itself follows the republished track.
            const nextSource = typeof data.source === "string" && data.source ? data.source : null
            if (nextSource) setBroadcastSource(nextSource)
            const sourceLabel =
              typeof data.label === "string" && data.label
                ? data.label
                : (nextSource || "").replace(/_/g, " ")
            if (sourceLabel) {
              setChat((prev) => [...prev, {
                userId: "system",
                userName: "System",
                message: `Broadcast source switched: ${sourceLabel}`,
                timestamp: data.timestamp || new Date().toISOString(),
                system: true,
              }])
            }
            break
          }
          case "PARTICIPANT_MUTED":
            if (data.targetUserId === myUserId) {
              setMicEnabled(false)
              setCameraEnabled(false)
              setJoinError(t("mutedByTeacher"))
            } else {
              setChat((prev) => [...prev, {
                userId: "system",
                userName: t("systemSender"),
                message: t("participantMutedMsg"),
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
            setJoinError(data.message || t("removedFromClassFallback"))
            retryCountRef.current = 10
            if (wsRef.current) wsRef.current.close()
            break
          case "PARTICIPANT_KICKED":
            setParticipants((prev) => prev.filter((p) => p.userId !== data.targetUserId))
            setChat((prev) => [...prev, {
              userId: "system",
              userName: t("systemSender"),
              message: t("participantRemovedMsg"),
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
              userName: t("systemSender"),
              message: t("quizStartedMsg", { title: data.title }),
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
              userName: t("systemSender"),
              message: t("quizClosedMsg", { title: data.title || t("untitledQuiz") }),
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
              userName: t("systemSender"),
              message: t("pollStartedMsg", { question: data.question }),
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
              userName: t("systemSender"),
              message: t("pollClosedMsg", { question: data.question || "" }),
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
          case "RESOURCE_SHARED":
            if (data.url || data.title) {
              setSharedMediaList((prev) =>
                prev.some((m) => m.title === data.title && m.url === data.url)
                  ? prev
                  : [
                      ...prev,
                      {
                        title: data.title || t("sharedContentFallback"),
                        url: data.url ?? "",
                        mediaType: data.mediaType || "VIDEO",
                      },
                    ],
              )
            }
            setChat((prev) => [...prev, {
              userId: "system",
              userName: t("systemSender"),
              message: t("mediaSharedMsg", { title: data.title || data.url }),
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
        } else {
          // Budget exhausted (or session not live): drop the banner so the status
          // chip shows the honest "Offline" instead of "Reconnecting..." forever.
          setReconnecting(false)
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
        localStream.getVideoTracks().forEach((track) => {
          track.stop()
          localStream.removeTrack(track)
        })
        if (localStream.getTracks().length === 0) setLocalStream(null)
      }
      setCameraEnabled(false)
    } else {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          video: selectedCameraId ? { deviceId: { exact: selectedCameraId } } : true,
          audio: false,
        })
        // Merge into the existing stream instead of replacing it, so a microphone
        // track captured earlier (or later) is never dropped.
        setLocalStream((prev) => {
          if (!prev) return stream
          stream.getVideoTracks().forEach((t) => prev.addTrack(t))
          return prev
        })
        setCameraEnabled(true)
        const room = roomRef.current
        if (room?.localParticipant) {
          const videoTrack = stream.getVideoTracks()[0]
          if (videoTrack) {
            await room.localParticipant.publishTrack(videoTrack, { name: "camera" })
          }
        }
      } catch (err) {
        setJoinError(t("cameraPermissionError"))
      }
    }
  }

  async function toggleMic() {
    if (micEnabled) {
      if (localStream) {
        localStream.getAudioTracks().forEach((track) => { track.enabled = false })
      }
      setMicEnabled(false)
    } else {
      try {
        let stream = localStream
        const micConstraints: boolean | MediaTrackConstraints = selectedMicId ? { deviceId: { exact: selectedMicId } } : true
        if (!stream) {
          stream = await navigator.mediaDevices.getUserMedia({ video: false, audio: micConstraints })
          setLocalStream(stream)
        } else if (stream.getAudioTracks().length === 0) {
          // The camera was turned on first: capture the mic and merge it into the
          // existing stream so it can actually be published.
          const mic = await navigator.mediaDevices.getUserMedia({ video: false, audio: micConstraints })
          mic.getAudioTracks().forEach((track) => stream!.addTrack(track))
        }
        stream.getAudioTracks().forEach((track) => { track.enabled = true })
        setMicEnabled(true)
        const room = roomRef.current
        if (room?.localParticipant) {
          const audioTrack = stream.getAudioTracks()[0]
          if (audioTrack) {
            await room.localParticipant.publishTrack(audioTrack, { name: "microphone" })
          }
        }
      } catch (err) {
        setJoinError(t("micPermissionError"))
      }
    }
  }

  /** Tell participants what now feeds the stage (persisted server-side as SOURCE_CHANGED). */
  function announceSource(kind: "camera" | "microphone", deviceId: string) {
    if (wsRef.current?.readyState !== WebSocket.OPEN) return
    const list = kind === "camera" ? cameras : microphones
    const label = list.find((d) => d.deviceId === deviceId)?.label || ""
    wsRef.current.send(JSON.stringify({
      type: "SOURCE_UPDATE",
      source: broadcastSource,
      label: label ? `${kind === "camera" ? "Camera" : "Microphone"}: ${label}` : kind,
    }))
  }

  /**
   * Switch to another camera (built-in → USB → capture card...) WITHOUT ending
   * the session: acquire the new track, replace it in the local preview, and
   * republish it through the existing LiveKit room. Failure of the republish is
   * reported honestly instead of pretending viewers see the new device.
   */
  async function switchCameraDevice(deviceId: string) {
    if (!deviceId || deviceId === selectedCameraId || sourceBusy) return
    setSelectedCameraId(deviceId)
    if (!cameraEnabled) return // applies the next time the camera is turned on
    setSourceBusy(true)
    setSourceError("")
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { deviceId: { exact: deviceId } },
        audio: false,
      })
      const newTrack = stream.getVideoTracks()[0]
      if (!newTrack) throw new Error("The selected camera produced no video track")
      const oldTracks = localStream ? [...localStream.getVideoTracks()] : []

      setLocalStream((prev) => {
        if (!prev) return stream
        oldTracks.forEach((t) => {
          prev.removeTrack(t)
          t.stop()
        })
        prev.addTrack(newTrack)
        return prev
      })

      const room = roomRef.current
      if (room?.localParticipant) {
        for (const old of oldTracks) {
          try {
            room.localParticipant.unpublishTrack(old)
          } catch {
            // track was never published — nothing to remove
          }
        }
        try {
          await room.localParticipant.publishTrack(newTrack, { name: "camera" })
        } catch {
          setSourceError("Camera switched locally, but re-publishing to the live room failed — viewers may still see the previous camera.")
        }
      }
      announceSource("camera", deviceId)
    } catch (err) {
      const name = (err as { name?: string })?.name || ""
      setSourceError(
        name === "NotAllowedError"
          ? "Permission to use this camera was denied."
          : name === "NotReadableError"
            ? "This camera is already in use by another application."
            : name === "OverconstrainedError" || name === "NotFoundError"
              ? "This camera is no longer available — it may have been unplugged."
              : "Could not switch camera."
      )
    } finally {
      setSourceBusy(false)
    }
  }

  /** Switch to another microphone without ending the session (same republish flow). */
  async function switchMicDevice(deviceId: string) {
    if (!deviceId || deviceId === selectedMicId || sourceBusy) return
    setSelectedMicId(deviceId)
    if (!micEnabled) return // applies the next time the mic is turned on
    setSourceBusy(true)
    setSourceError("")
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: false,
        audio: { deviceId: { exact: deviceId } },
      })
      const newTrack = stream.getAudioTracks()[0]
      if (!newTrack) throw new Error("The selected microphone produced no audio track")
      newTrack.enabled = true
      const oldTracks = localStream ? [...localStream.getAudioTracks()] : []

      const merged = localStream || stream
      oldTracks.forEach((t) => {
        merged.removeTrack(t)
        t.stop()
      })
      if (localStream) {
        merged.addTrack(newTrack)
        setLocalStream(merged)
      } else {
        setLocalStream(stream)
      }

      const room = roomRef.current
      if (room?.localParticipant) {
        for (const old of oldTracks) {
          try {
            room.localParticipant.unpublishTrack(old)
          } catch {
            // track was never published — nothing to remove
          }
        }
        try {
          await room.localParticipant.publishTrack(newTrack, { name: "microphone" })
        } catch {
          setSourceError("Microphone switched locally, but re-publishing to the live room failed — viewers may still hear the previous microphone.")
        }
      }
      announceSource("microphone", deviceId)
    } catch (err) {
      const name = (err as { name?: string })?.name || ""
      setSourceError(
        name === "NotAllowedError"
          ? "Permission to use this microphone was denied."
          : name === "NotReadableError"
            ? "This microphone is already in use by another application."
            : name === "OverconstrainedError" || name === "NotFoundError"
              ? "This microphone is no longer available — it may have been unplugged."
              : "Could not switch microphone."
      )
    } finally {
      setSourceBusy(false)
    }
  }

  async function toggleScreenShare() {
    if (screenSharing) {
      if (screenStream) {
        screenStream.getTracks().forEach((track) => track.stop())
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
          if (wsRef.current?.readyState === WebSocket.OPEN) {
            wsRef.current.send(JSON.stringify({ type: "SCREEN_SHARE_STOP" }))
          }
        }
        if (wsRef.current?.readyState === WebSocket.OPEN) {
          wsRef.current.send(JSON.stringify({ type: "SCREEN_SHARE_START" }))
        }
      } catch (err) {
        setJoinError(t("screenPermissionError"))
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
    if (!confirm(t("removeParticipantConfirm"))) return
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
      setChat((prev) => [...prev, { userId: myUserId, userName: t("youSender"), message: trimmed, timestamp: new Date().toISOString(), kind: "QA" }])
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
    // Recording is teacher/admin-only on the server (403 for anyone else) —
    // never show/allow the action for learners.
    if (!isTeacherClient && user?.role !== "Admin") return
    // Guards against a double click firing two Start requests, which would otherwise open a
    // second LiveKit egress job for the same session.
    if (recordingBusy) return
    setRecordingBusy(true)
    setActionError("")
    try {
      const token = localStorage.getItem("elmkusoma_access_token")
      const base = process.env.NEXT_PUBLIC_API_BASE || "http://localhost:8080"
      const headers: Record<string, string> = { Authorization: `Bearer ${token}` }

      if (isRecording) {
        const res = await fetch(`${base}/v1/live-session/classes/${liveClass.id}/recording/stop`, {
          method: "POST",
          headers,
        })
        if (!res.ok) {
          setActionError(await describeRecordingError(res))
          return
        }
        setIsRecording(false)
        setRecordingEgressId(null)
        setChat((prev) => [...prev, { userId: "system", userName: t("systemSender"), message: t("recordingStoppedMsg"), timestamp: new Date().toISOString(), system: true }])
        return
      }

      const res = await fetch(`${base}/v1/live-session/classes/${liveClass.id}/recording/start`, {
        method: "POST",
        headers,
      })
      if (!res.ok) {
        // Never flip to "recording" on a failed or refused request. The server answers 503 when
        // LiveKit or its recording storage is unavailable and 403/404 when the caller is not
        // the host; previously these were swallowed silently.
        setActionError(await describeRecordingError(res))
        return
      }
      const data = await res.json().catch(() => null)
      const egressId = data?.data?.egressId
      if (!egressId) {
        // A 200 without an egress id means no capture is actually running. Trusting it would
        // show a false recording indicator.
        setActionError(t("recordingStartUnconfirmed"))
        return
      }
      setIsRecording(true)
      setRecordingEgressId(egressId)
      setChat((prev) => [...prev, { userId: "system", userName: t("systemSender"), message: data?.data?.alreadyRecording ? t("recordingAlreadyActiveMsg") : t("recordingStartedMsg"), timestamp: new Date().toISOString(), system: true }])
    } catch {
      setActionError(t("recordingToggleFailed"))
    } finally {
      setRecordingBusy(false)
    }
  }

  /** Turns a non-2xx recording response into a message the teacher can act on. */
  async function describeRecordingError(res: Response): Promise<string> {
    const body = await res.json().catch(() => null)
    const detail = body?.error || body?.message
    if (res.status === 503) return detail || t("recordingUnavailable")
    if (res.status === 403) return detail || t("recordingNotAllowed")
    if (res.status === 404) return detail || t("recordingSessionNotFound")
    return detail || `${t("recordingToggleFailed")} (${res.status})`
  }

  async function handleAttachMaterial() {
    const title = materialName.trim()
    const url = materialUrl.trim()
    if (!title || !url || !liveClass?.id) return
    setActionError("")
    // Persisted through the existing shared-media endpoint, which also broadcasts to the
    // room. The previous implementation only posted a chat line, so the shared-media
    // panel never received anything and stayed permanently empty.
    try {
      const res = await fetch(`${API_BASE}/v1/live-session/classes/${liveClass.id}/shared-media`, {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({ title, url, mediaType: "LINK" }),
      })
      if (!res.ok) {
        setActionError(t("shareMaterialFailed"))
        return
      }
      setAttachedMaterials((prev) => [...prev, { name: title, url }])
      setSharedMediaList((prev) =>
        prev.some((m) => m.title === title && m.url === url) ? prev : [...prev, { title, url, mediaType: "LINK" }],
      )
      setMaterialName("")
      setMaterialUrl("")
      setShowMaterialInput(false)
    } catch {
      setActionError(t("shareMaterialFailed"))
    }
  }

  function sendReaction(emoji: string) {
    if (wsRef.current?.readyState !== WebSocket.OPEN) return
    // REACTION is broadcast to the room and never persisted into the chat transcript.
    wsRef.current.send(JSON.stringify({ type: "REACTION", messageType: "REACTION", message: emoji }))
  }

  async function handleEndSession() {
    if (!liveClass?.id || !isInProgress) return
    if (!isTeacherClient && user?.role !== "Admin") return
    setActionBusy(true)
    setActionError("")
    try {
      // Teachers end through their own endpoint; an administrator has no teacher-scoped
      // route, so it uses the admin force-end (which is also institution-scoped).
      const isAdmin = user?.role === "Admin"
      const url = isAdmin
        ? `${API_BASE}/v1/admin/live-sessions/${liveClass.id}/force-end`
        : `${API_BASE}/v1/teachers/me/live-classes/${liveClass.id}/end`
      const res = await fetch(url, {
        method: "POST",
        headers: authHeaders(false),
      })
      if (res.ok) {
        setSessionStatus("COMPLETED")
        setIsRecording(false)
        setRecordingEgressId(null)
        setChat((prev) => [
          ...prev,
          {
            userId: "system",
            userName: t("systemSender"),
            message: t("sessionEndedByHostMsg"),
            timestamp: new Date().toISOString(),
            system: true,
          },
        ])
      } else {
        setActionError(t("endSessionFailed"))
      }
    } catch {
      setActionError(t("endSessionFailed"))
    } finally {
      setActionBusy(false)
    }
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
        const msg = await apiErrorMessage(qRes, t("couldNotLoadQuiz"))
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
          [quizId]: { questions: [], answers: {}, submitted: false, loading: false, error: t("couldNotLoadQuizConn") },
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
        setInteractiveError(t("activitiesAccessError"))
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
      setInteractiveError(t("activitiesConnError"))
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
        setActionError(await apiErrorMessage(res, t("quizLaunchError")))
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
      setActionError(t("quizLaunchNetError"))
    } finally {
      setActionBusy(false)
    }
  }

  async function createPoll() {
    if (!newPollQuestion.trim() || !liveClass?.id || !newPollOptions.trim() || actionBusy) return
    const options = newPollOptions.split(",").map(o => o.trim()).filter(Boolean)
    if (options.length < 2) {
      setActionError(t("pollOptionsError"))
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
        setActionError(await apiErrorMessage(res, t("pollLaunchError")))
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
      setActionError(t("pollLaunchNetError"))
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
        setActionError(await apiErrorMessage(res, t("breakoutCreateError")))
        return
      }
      await refreshBreakoutRooms()
      setNewBreakoutName("")
      setShowBreakoutModal(false)
    } catch {
      setActionError(t("breakoutCreateNetError"))
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
        setActionError(await apiErrorMessage(res, t("roomOpenError")))
        return
      }
      await refreshBreakoutRooms()
    } catch {
      setActionError(t("roomOpenNetError"))
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
        setActionError(await apiErrorMessage(res, t("roomCloseError")))
        return
      }
      await refreshBreakoutRooms()
    } catch {
      setActionError(t("roomCloseNetError"))
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
        setActionError(await apiErrorMessage(res, t("assignError")))
        return
      }
      await refreshBreakoutRooms()
    } catch {
      setActionError(t("assignNetError"))
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
        setActionError(await apiErrorMessage(res, t("quizCloseError")))
        return
      }
      const body = await res.json()
      const id = String(body?.data?.id || quizId)
      setActiveQuizzes((prev) => prev.map((q) => (q.id === id ? { ...q, status: "CLOSED" } : q)))
      void loadQuizResults(quizId)
    } catch {
      setActionError(t("quizCloseNetError"))
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
        setActionError(await apiErrorMessage(res, t("voteError")))
        return
      }
      // Selected only after the server accepted the vote — never optimistic.
      setSelectedPollOption(optionIndex)
    } catch {
      setActionError(t("voteNetError"))
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
        setActionError(await apiErrorMessage(res, t("answersSubmitError")))
      }
      // Reload server truth in both cases: submitted / already-submitted / closed
      // all render from what the server says, never from a local flag flip.
      await ensureQuizLoaded(quizId, true)
    } catch {
      setActionError(t("answersSubmitNetError"))
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
        setActionError(await apiErrorMessage(res, t("joinRoomError")))
        await refreshBreakoutRooms()
        return
      }
      const body = await res.json()
      const d = body?.data || {}
      setInBreakout({ roomId, roomName: String(d.roomName || t("breakoutFallbackName")) })
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
        userName: t("systemSender"),
        message: t("breakoutJoinedMsg", { name: d.roomName || "" }),
        timestamp: new Date().toISOString(),
        system: true,
      }])
      await refreshBreakoutRooms()
    } catch {
      setActionError(t("joinRoomNetError"))
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
      localStream.getTracks().forEach((track) => track.stop())
      setLocalStream(null)
    }
    if (screenStream) {
      screenStream.getTracks().forEach((track) => track.stop())
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
    // Same credential path as the initial join and the reconnect attempts, so Retry can
    // never re-apply an expired token either.
    refreshLiveKitCredentials().then(creds => {
      if (creds) return
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
            {tc("back")}
          </Link>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" size="sm" className="border-destructive/30 text-destructive hover:bg-destructive/10" onClick={handleLeave}>
            {t("leaveButton")}
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
            ? t("liveBadge")
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
              <><Wifi className="size-3 text-teal" /> <span className="text-teal">{t("connectedLabel")}</span></>
            ) : reconnecting ? (
              <><WifiOff className="size-3 text-amber-500 animate-pulse" /> <span className="text-amber-500">{t("reconnectingLabel")}</span></>
            ) : (
              <><WifiOff className="size-3 text-muted-foreground" /> <span className="text-muted-foreground">{t("offlineLabel")}</span></>
            )}
          </span>
        </div>
      </div>

      {joinError && (
        <div role="alert" className="mb-3 rounded-lg border border-red-200 bg-red-50 p-2 text-xs text-red-700 flex items-center justify-between">
          <span>{joinError}</span>
          <button onClick={() => setJoinError("")} aria-label={tc("close")} className="text-red-500 hover:text-red-700"><XCircle className="size-3.5" /></button>
        </div>
      )}

      {actionError && (
        <div role="alert" className="mb-3 rounded-lg border border-red-200 bg-red-50 p-2 text-xs text-red-700 flex items-center justify-between">
          <span>{actionError}</span>
          <button onClick={() => setActionError("")} aria-label={tc("close")} className="text-red-500 hover:text-red-700"><XCircle className="size-3.5" /></button>
        </div>
      )}

      {interactiveLoading && !interactiveReady && (
        <div className="mb-3 rounded-lg border border-border bg-card p-2 text-xs text-muted-foreground">
          {t("loadingActivities")}
        </div>
      )}

      {interactiveError && (
        <div role="status" className="mb-3 rounded-lg border border-amber-200 bg-amber-50 p-2 text-xs text-amber-700 flex items-center justify-between">
          <span>{interactiveError}</span>
          <button onClick={() => { interactiveLoadedRef.current = false; void refreshInteractiveState() }} className="font-medium text-amber-600 underline">{tc("retry")}</button>
        </div>
      )}

      {serviceMode === "chat-only" && isInProgress && (
        <div role="status" className="mb-3 rounded-lg border border-amber-200 bg-amber-50 p-2.5 text-xs text-amber-700">
          <p className="font-medium">{t("degradedTitle")}</p>
          <p className="mt-0.5 text-amber-600">{t("degradedDesc")}</p>
        </div>
      )}

      {isTeacherClient && isInProgress && hostState?.recordingActive && (
        <div role="status" className="mb-3 rounded-lg border border-red-200 bg-red-50 p-2.5 text-xs text-red-700 flex items-center gap-2">
          <Circle className="size-2.5 shrink-0 fill-red-500 text-red-500 animate-pulse motion-reduce:animate-none" aria-hidden />
          <span>{t("recordingInProgressMsg")}</span>
        </div>
      )}

      <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
        <div className="flex flex-col gap-4 order-2 lg:order-1">
          <LiveVideoPlayer
            state={playerState}
            sessionLive={isInProgress}
            liveElapsed={isInProgress ? elapsed : null}
            remoteVideoTrack={remoteVideoTrack}
            remoteScreenTrack={remoteScreenTrack}
            remoteAudioTrack={remoteAudioTrack}
            isTeacher={isTeacherClient}
            localStream={localStream}
            screenStream={screenStream}
            cameraEnabled={cameraEnabled}
            recordingUrl={sessionEnded ? liveClass.recordingUrl : null}
            scheduledLabel={liveClass.scheduledAt ? t("startsAtWithTime", { time: formatTime(liveClass.scheduledAt) }) : t("startsAtTbd")}
            onRetry={retryLiveKit}
            pipStream={
              localStream && cameraEnabled && (screenStream || (!isTeacherClient && !remoteVideoTrack))
                ? localStream
                : null
            }
          >
            {isInProgress && (
              <>
                <div className="absolute inset-x-0 bottom-0 z-20 flex flex-wrap items-center justify-center gap-1.5 bg-gradient-to-t from-black/70 to-transparent p-3">
                    <ControlButton active={cameraEnabled} onClick={toggleCamera} label={cameraEnabled ? t("cameraOffLabel") : t("cameraOnLabel")}>
                      {cameraEnabled ? <Video className="size-4" /> : <VideoOff className="size-4" />}
                    </ControlButton>
                    <ControlButton active={micEnabled} onClick={toggleMic} label={micEnabled ? t("muteLabel") : t("unmuteLabel")}>
                      {micEnabled ? <Mic className="size-4" /> : <MicOff className="size-4" />}
                    </ControlButton>
                    <ControlButton active={screenSharing} onClick={toggleScreenShare} label={screenSharing ? t("stopSharingLabel") : t("shareScreenLabel")}>
                      <MonitorUp className="size-4" />
                    </ControlButton>
                    {(isTeacherClient || user?.role === "Admin") && (
                      <ControlButton
                        active={isRecording}
                        onClick={toggleRecording}
                        disabled={recordingBusy}
                        label={
                          recordingBusy
                            ? t("recordingBusyLabel")
                            : isRecording
                              ? t("stopRecordingLabel")
                              : t("startRecordingLabel")
                        }
                      >
                        {recordingBusy ? (
                          <Loader2 className="size-4 animate-spin" />
                        ) : (
                          <Circle className={cn("size-4", isRecording && "fill-red-500 text-red-500 animate-pulse")} />
                        )}
                      </ControlButton>
                    )}
                    {(isTeacherClient || user?.role === "Admin") && (
                      <ControlButton
                        active={false}
                        onClick={handleEndSession}
                        label={t("endSessionLabel")}
                        disabled={!isInProgress || actionBusy}
                      >
                        <Square className="size-4" />
                      </ControlButton>
                    )}
                    <div className="flex items-center gap-0.5" role="group" aria-label={t("reactionsLabel")}>
                      {REACTION_EMOJI.map((emoji) => (
                        <button
                          key={emoji}
                          type="button"
                          aria-label={t("reactAria", { emoji })}
                          aria-pressed={reactions[emoji] !== undefined}
                          onClick={() => sendReaction(emoji)}
                          className="flex size-11 min-h-11 min-w-11 items-center justify-center rounded-full border border-white/20 bg-white/10 text-white transition-colors hover:bg-white/20"
                        >
                          <span aria-hidden>{emoji}</span>
                        </button>
                      ))}
                    </div>
                    <ControlButton active={showMaterialInput} onClick={() => setShowMaterialInput(!showMaterialInput)} label={t("attachMaterialLabel")}>
                      <Paperclip className="size-4" />
                    </ControlButton>
                    <ControlButton active={handRaised} onClick={toggleHand} label={handRaised ? t("lowerHandLabel") : t("raiseHandLabel")}>
                      <Hand className="size-4" />
                    </ControlButton>
                    <button
                      type="button"
                      onClick={() => setShowIssueModal(true)}
                      aria-label={t("reportIssueAria")}
                      className="flex size-11 min-h-11 min-w-11 items-center justify-center rounded-full border border-white/20 bg-red-500/20 text-white hover:bg-red-500/30 transition-colors"
                      title={t("reportIssueTitle")}
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
                        placeholder={t("materialNamePlaceholder")}
                        className="h-8 flex-1 rounded border border-white/20 bg-white/10 px-2 text-xs text-white placeholder:text-white/50 outline-none"
                      />
                      <input
                        type="url"
                        value={materialUrl}
                        onChange={(e) => setMaterialUrl(e.target.value)}
                        placeholder="https://..."
                        className="h-8 flex-1 rounded border border-white/20 bg-white/10 px-2 text-xs text-white placeholder:text-white/50 outline-none"
                      />
                      <Button size="sm" className="h-8 text-xs" onClick={handleAttachMaterial}>{t("shareButton")}</Button>
                    </div>
                  )}
                </>
              )}
            </LiveVideoPlayer>

          <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
            <h2 className="text-xs font-semibold text-foreground mb-2">{t("classDetailsTitle")}</h2>
            <div className="space-y-1 text-xs text-muted-foreground">
              <p>{t("sourceLine", { source: broadcastSource.replace(/_/g, " ") })}</p>
              {liveClass.scheduledAt && <p>{t("scheduledLine", { datetime: new Date(liveClass.scheduledAt).toLocaleString() })}</p>}
              {liveClass.durationMinutes && <p>{t("durationLine", { count: liveClass.durationMinutes })}</p>}
              {liveClass.maxParticipants && <p>{t("maxParticipantsLine", { count: liveClass.maxParticipants })}</p>}
              {liveClass.description && <p className="whitespace-pre-line line-clamp-3">{liveClass.description}</p>}
            </div>
          </div>
          {attachedMaterials.length > 0 && (
            <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
              <h2 className="text-xs font-semibold text-foreground mb-2">{t("attachedMaterialsTitle")}</h2>
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
          {(isTeacherClient || user?.role === "Admin") && isInProgress && (
            <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
              <div className="flex items-center justify-between gap-2">
                <h2 className="text-xs font-semibold text-foreground">Broadcast Source</h2>
                <span className="max-w-[140px] truncate rounded-full bg-primary/10 px-2 py-0.5 text-[10px] font-medium text-primary">
                  {broadcastSource.replace(/_/g, " ")}
                </span>
              </div>

              {!hasLabels && (
                <button
                  type="button"
                  onClick={requestPermission}
                  className="mt-2 w-full rounded-lg border border-border px-2 py-1.5 text-[11px] text-muted-foreground hover:bg-accent"
                >
                  Allow device access to list cameras & microphones
                </button>
              )}

              <div className="mt-2 space-y-1.5">
                <select
                  value={selectedCameraId}
                  onChange={(e) => switchCameraDevice(e.target.value)}
                  disabled={sourceBusy || cameras.length === 0}
                  aria-label="Camera source"
                  className="w-full rounded-lg border border-border bg-background px-2 py-1.5 text-[11px] text-foreground disabled:opacity-60"
                >
                  <option value="">{cameras.length > 0 ? "Default camera" : "No cameras found"}</option>
                  {cameras.map((c) => (
                    <option key={c.deviceId} value={c.deviceId}>{c.label}</option>
                  ))}
                </select>
                <select
                  value={selectedMicId}
                  onChange={(e) => switchMicDevice(e.target.value)}
                  disabled={sourceBusy || microphones.length === 0}
                  aria-label="Microphone source"
                  className="w-full rounded-lg border border-border bg-background px-2 py-1.5 text-[11px] text-foreground disabled:opacity-60"
                >
                  <option value="">{microphones.length > 0 ? "Default microphone" : "No microphones found"}</option>
                  {microphones.map((m) => (
                    <option key={m.deviceId} value={m.deviceId}>{m.label}</option>
                  ))}
                </select>
              </div>

              {sourceBusy && <p className="mt-1.5 text-[10px] text-muted-foreground">Switching source...</p>}
              {sourceError && <p className="mt-1.5 text-[10px] text-red-500">{sourceError}</p>}
            </div>
          )}

          <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-xs font-semibold text-foreground">{t("participantsTitle", { count: participants.length })}</h2>
            </div>
            <div className="mt-3 space-y-1.5 max-h-36 overflow-y-auto">
              {participants.length === 0 ? (
                <p className="text-[10px] text-muted-foreground">{t("noParticipants")}</p>
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
                        {p.userId === myUserId && <span className="text-muted-foreground">{t("youSuffix")}</span>}
                      </p>
                    </div>
                    {p.userId !== myUserId && (user?.role === "Teacher" || user?.role === "Admin") && (
                      // Always visible on coarse pointers (touch) and keyboard reachable;
                      // hover-reveal only on precise pointers. Previously these were
                      // hover-only, so they were unreachable by keyboard and invisible on mobile.
                      <div className="flex items-center gap-0.5 opacity-100 md:opacity-0 md:transition-opacity md:group-hover:opacity-100 md:group-focus-within:opacity-100">
                        <button
                          type="button"
                          onClick={() => muteParticipant(p.userId)}
                          className="rounded p-1 text-muted-foreground hover:text-foreground hover:bg-muted focus-visible:opacity-100"
                          aria-label={t("muteParticipantAria", { name: p.userName })}
                          title={t("muteLabel")}
                        >
                          <MicOff className="size-3" aria-hidden />
                        </button>
                        <button
                          type="button"
                          onClick={() => unmuteParticipant(p.userId)}
                          className="rounded p-1 text-muted-foreground hover:text-foreground hover:bg-muted focus-visible:opacity-100"
                          aria-label={t("unmuteParticipantAria", { name: p.userName })}
                          title={t("unmuteLabel")}
                        >
                          <Mic className="size-3" aria-hidden />
                        </button>
                        <button
                          type="button"
                          onClick={() => kickParticipant(p.userId)}
                          className="rounded p-1 text-muted-foreground hover:text-destructive hover:bg-destructive/10 focus-visible:opacity-100"
                          aria-label={t("removeParticipantAria", { name: p.userName })}
                          title={t("removeAction")}
                        >
                          <XCircle className="size-3" aria-hidden />
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
                  <ListOrdered className="size-3" /> {t("handQueueTitle", { count: handRaiseQueue.length })}
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
                  <ClipboardList className="size-3" /> {isActive ? t("activePollTitle") : t("pollClosedTitle")}
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
                    {tally ? t("pollFinalResult", { count: tally.totalVotes }) : t("pollClosedEnded")}
                  </p>
                )}
                {isActive && isTeacherClient && (
                  <p className="mt-2 text-[10px] text-blue-700">
                    {tally ? t("pollLiveResult", { count: tally.totalVotes }) : t("waitingVotes")}
                  </p>
                )}
                {isActive && !isTeacherClient && myVote === null && pollVoting && (
                  <p className="mt-2 text-[10px] text-blue-600">{t("recordingVote")}</p>
                )}
                {isActive && !isTeacherClient && myVote !== null && (
                  <p className="mt-2 text-[10px] text-blue-700">{t("voteSubmitted")}</p>
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
                  <ListOrdered className="size-3" /> {t("liveQuizTitle")} {isActive ? "" : t("quizClosedSuffix")}
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
                        {q.title || t("untitled")}
                      </button>
                    ))}
                  </div>
                )}
                <p className="mb-2 text-xs font-medium text-foreground">{quiz.title || t("untitledQuiz")}</p>
                {ui?.loading && <p className="text-[10px] text-muted-foreground">{t("loadingQuestions")}</p>}
                {ui?.error && <p className="text-[10px] text-red-600">{ui.error}</p>}
                {ui && !ui.loading && !ui.error && ui.questions.length === 0 && (
                  <p className="text-[10px] text-muted-foreground">{t("quizNoQuestions")}</p>
                )}
                {ui && !ui.loading && ui.questions.length > 0 && (
                  <div className="space-y-3">
                    {ui.questions.map((q, qi) => {
                      const opts = parseOptionsList(q.options)
                      const canAnswer = isActive && !ui.submitted && !quizSubmitting
                      return (
                        <div key={q.id} className="rounded-lg border border-teal-100 bg-white p-2">
                          <p className="text-[11px] font-medium text-foreground">{t("questionNumberLine", { index: qi + 1, text: q.questionText })}</p>
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
                              {t("yourAnswerLine", { answer: ui.answers[q.id] ?? q.myAnswer ?? "—" })}
                              {!isActive && typeof q.isCorrect === "boolean" && (q.isCorrect ? t("correctSuffix") : t("incorrectSuffix"))}
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
                          {quizSubmitting ? t("submittingLabel") : t("submitAnswersButton")}
                        </Button>
                        {!allAnswered && (
                          <p className="text-[10px] text-muted-foreground">{t("answerAllToSubmit")}</p>
                        )}
                      </>
                    )}
                    {ui.submitted && (
                      <p className="text-[10px] font-medium text-teal-700">{t("voteSubmitted")}</p>
                    )}
                    {!isActive && !ui.submitted && (
                      <p className="text-[10px] text-muted-foreground">{t("quizClosedNoSubmit")}</p>
                    )}
                  </div>
                )}
              </div>
            )
          })()}

          {sharedMediaList.length > 0 && (
            <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
                <h2 className="text-xs font-semibold text-foreground mb-2 flex items-center gap-1">
                <PlayCircle className="size-3" /> {t("sharedMediaTitle")}
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
                <Zap className="size-3" /> {t("launchQuizTitle")}
              </h2>
              <input
                type="text"
                value={newQuizTitle}
                onChange={e => setNewQuizTitle(e.target.value)}
                placeholder={t("quizTitlePlaceholder")}
                className="h-8 w-full rounded border border-border bg-muted/60 px-2 text-xs outline-none"
              />
              {quizQuestionList.map((q, i) => (
                <div key={i} className="rounded border border-border p-2 text-[10px] space-y-1">
                  <div className="font-medium text-foreground">{t("questionNumberLine", { index: i + 1, text: q.questionText })}</div>
                  <div className="text-muted-foreground">{t("questionOptionsLine", { options: q.options })}</div>
                  <div className="text-teal">{t("questionAnswerLine", { answer: q.correctAnswer })}</div>
                </div>
              ))}
              <div className="flex gap-1">
                <input
                  type="text"
                  placeholder={t("questionPlaceholder")}
                  className="h-7 flex-1 rounded border border-border bg-muted/60 px-2 text-[10px] outline-none"
                  id="quiz-q-text"
                />
                <input
                  type="text"
                  placeholder={t("optionsPlaceholder")}
                  className="h-7 flex-1 rounded border border-border bg-muted/60 px-2 text-[10px] outline-none"
                  id="quiz-q-opts"
                />
                <input
                  type="text"
                  placeholder={t("correctAnswerPlaceholder")}
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
                {t("launchQuizCount", { count: quizQuestionList.length })}
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
                  <Zap className="size-3" /> {t("liveQuizzesTitle")}
                </h2>
                {list.map(q => {
                  const r = quizResultsById[q.id]
                  const isActive = q.status === "ACTIVE"
                  const accuracy = r && r.totalResponses > 0 ? Math.round((r.totalCorrect / r.totalResponses) * 100) : 0
                  return (
                    <div key={q.id} className="rounded-lg border border-border p-2 space-y-1">
                      <p className="text-[11px] font-medium text-foreground">
                        {q.title || t("untitledQuiz")}
                        {!isActive && <span className="ml-1 text-muted-foreground">{t("quizClosedSuffix")}</span>}
                      </p>
                      <p className="text-[10px] text-muted-foreground">
                        {r
                          ? r.totalResponses > 0
                            ? t("quizStatsWithAccuracy", { answered: r.answeredCount, total: r.participantCount || r.answeredCount, correct: r.totalCorrect, responses: r.totalResponses, accuracy })
                            : t("quizStatsNoAccuracy", { answered: r.answeredCount, total: r.participantCount || r.answeredCount, correct: r.totalCorrect, responses: r.totalResponses })
                          : t("waitingResponses")}
                      </p>
                      {isActive ? (
                        <div className="flex gap-1">
                          <Button size="sm" variant="outline" className="h-5 flex-1 text-[10px]" disabled={actionBusy} onClick={() => void loadQuizResults(q.id)}>
                            {t("refreshButton")}
                          </Button>
                          <Button size="sm" variant="ghost" className="h-5 flex-1 text-[10px] text-destructive" disabled={actionBusy} onClick={() => void closeQuiz(q.id)}>
                            {t("closeQuizButton")}
                          </Button>
                        </div>
                      ) : (
                        <Button size="sm" variant="outline" className="h-5 w-full text-[10px]" disabled={actionBusy} onClick={() => void loadQuizResults(q.id)}>
                          {t("viewResultsButton")}
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
                <BarChart3 className="size-3" /> {t("createPollTitle")}
              </h2>
              <input
                type="text"
                value={newPollQuestion}
                onChange={e => setNewPollQuestion(e.target.value)}
                placeholder={t("pollQuestionPlaceholder")}
                className="h-8 w-full rounded border border-border bg-muted/60 px-2 text-xs outline-none"
              />
              <input
                type="text"
                value={newPollOptions}
                onChange={e => setNewPollOptions(e.target.value)}
                placeholder={t("optionsPlaceholder")}
                className="h-8 w-full rounded border border-border bg-muted/60 px-2 text-xs outline-none"
              />
              <Button
                size="sm"
                className="h-7 text-[10px] w-full"
                disabled={actionBusy || !newPollQuestion.trim() || !newPollOptions.trim()}
                onClick={createPoll}
              >
                {t("launchPollButton")}
              </Button>
            </div>
          )}

          {(breakoutRooms.length > 0 || (isInProgress && user?.role === "Teacher")) && (
            <div className="rounded-xl border border-border bg-card p-4 shadow-sm">
              <div className="flex items-center justify-between mb-2">
                <h2 className="text-xs font-semibold text-foreground flex items-center gap-1">
                  <Users className="size-3" /> {t("breakoutRoomsTitle")}
                </h2>
                {isInProgress && user?.role === "Teacher" && (
                  <Button size="sm" variant="outline" className="h-6 text-[10px]" onClick={() => setShowBreakoutModal(true)}>
                    {t("createBreakoutButton")}
                  </Button>
                )}
              </div>
              {inBreakout && (
                <div className="mb-2 space-y-1 rounded-lg border border-teal-200 bg-teal-50 p-2 text-[10px] text-teal-800">
                  <p className="font-medium">{t("inBreakoutLabel", { name: inBreakout.roomName })}</p>
                  <Button size="sm" variant="outline" className="h-5 text-[10px]" onClick={() => void returnToMainRoom()}>
                    {t("returnMainRoom")}
                  </Button>
                </div>
              )}
              {breakoutRooms.length === 0 ? (
                <p className="text-[10px] text-muted-foreground">{t("noBreakoutRooms")}</p>
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
                          <span className="text-[10px] text-muted-foreground">{t("assignedCountLine", { assigned: room.assignedCount ?? assigned.length, max: room.maxParticipants })}</span>
                          {user?.role === "Teacher" && (
                            <div className="flex gap-1">
                              {room.status === "WAITING" && (
                                <Button size="sm" variant="ghost" className="h-5 text-[10px] text-teal" disabled={actionBusy} onClick={() => void startBreakoutRoom(room.id)}>{t("startButton")}</Button>
                              )}
                              {room.status === "ACTIVE" && (
                                <Button size="sm" variant="ghost" className="h-5 text-[10px] text-destructive" disabled={actionBusy} onClick={() => void endBreakoutRoom(room.id)}>{t("endButton")}</Button>
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
                            >{room.assignedToMe ? t("joinRoomButton") : t("joinButton")}</Button>
                          )}
                        </div>
                        {!isTeacherClient && room.assignedToMe && room.status === "WAITING" && (
                          <p className="text-[10px] text-amber-600">{t("assignedWaiting")}</p>
                        )}
                        {!isTeacherClient && room.assignedToMe && room.status === "ENDED" && (
                          <p className="text-[10px] text-muted-foreground">{t("roomEndedMsg")}</p>
                        )}
                        {!isTeacherClient && iAmAssignedElsewhere && (
                          <p className="text-[10px] text-muted-foreground">{t("assignedElsewhere")}</p>
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
                            <option value="">{t("assignParticipantPlaceholder")}</option>
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
              <h3 className="text-xs font-semibold text-foreground mb-2">{t("newBreakoutTitle")}</h3>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={newBreakoutName}
                  onChange={e => setNewBreakoutName(e.target.value)}
                  placeholder={t("roomNamePlaceholder")}
                  className="h-8 flex-1 rounded border border-border bg-muted/60 px-2 text-xs outline-none"
                  onKeyDown={e => { if (e.key === "Enter") createBreakoutRoom() }}
                />
                <Button size="sm" className="h-8 text-xs" onClick={createBreakoutRoom} disabled={actionBusy || !newBreakoutName.trim()}>{tc("create")}</Button>
                <Button size="sm" variant="ghost" className="h-8 text-xs" onClick={() => { setShowBreakoutModal(false); setNewBreakoutName("") }}>{tc("cancel")}</Button>
              </div>
            </div>
          )}

          <div className="flex min-h-80 flex-col rounded-xl border border-border bg-card shadow-sm">
            <div className="flex items-center justify-between border-b border-border px-4 py-2.5">
              <div className="flex gap-1" role="tablist" aria-label={t("sidePanelAria")}>
                {(["chat", "qa", "people"] as const).map((tab) => (
                  <button
                    key={tab}
                    type="button"
                    role="tab"
                    id={`live-side-tab-${tab}`}
                    aria-selected={sideTab === tab}
                    aria-controls={`live-side-panel-${tab}`}
                    tabIndex={sideTab === tab ? 0 : -1}
                    onClick={() => setSideTab(tab)}
                    onKeyDown={e => {
                      const order = ["chat", "qa", "people"] as const
                      const idx = order.indexOf(tab)
                      if (e.key === "ArrowRight" || e.key === "ArrowLeft") {
                        e.preventDefault()
                        const next = order[(idx + (e.key === "ArrowRight" ? 1 : order.length - 1)) % order.length]
                        setSideTab(next)
                        document.getElementById(`live-side-tab-${next}`)?.focus()
                      }
                    }}
                    className={`rounded px-2 py-1 text-[10px] font-semibold uppercase ${sideTab === tab ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted"}`}
                  >
                    {tab === "chat" ? t("chatTab") : tab === "qa" ? t("qaTab") : t("peopleTab")}
                  </button>
                ))}
              </div>
              <span className="inline-flex items-center gap-1">
                {connected && <span className="size-1.5 rounded-full bg-teal animate-pulse" />}
                <span className="text-[10px] text-muted-foreground">
                  {connected ? t("liveSmallLabel") : reconnecting ? t("reconnectingLabel") : t("offlineLabel")}
                </span>
              </span>
            </div>
            {sideTab === "people" ? (
              <div id="live-side-panel-people" role="tabpanel" aria-labelledby="live-side-tab-people" className="flex-1 space-y-1.5 overflow-y-auto p-4">
                {participants.map((p) => (
                  <div key={p.userId} className="flex items-center justify-between gap-2 text-xs">
                    <span className="font-medium text-foreground">{p.userName}</span>
                    <span className="text-[10px] text-muted-foreground">{p.role}</span>
                    {(user?.role === "Teacher" || user?.role === "Admin") && p.userId !== myUserId && (
                      <span className="flex gap-1">
                        <button
                          type="button"
                          onClick={() => setParticipantRole(p.userId, p.role === "MODERATOR" ? "LEARNER" : "MODERATOR")}
                          aria-label={p.role === "MODERATOR" ? t("demoteAria", { name: p.userName }) : t("promoteAria", { name: p.userName })}
                          className="rounded p-0.5 text-[10px] text-muted-foreground hover:text-foreground"
                        >
                          {p.role === "MODERATOR" ? t("demoteLabel") : t("promoteLabel")}
                        </button>
                        <button onClick={() => muteParticipant(p.userId)} aria-label={t("muteUserAria", { name: p.userName })} className="rounded p-0.5 text-muted-foreground hover:text-foreground">
                          <MicOff className="size-3" />
                        </button>
                        <button onClick={() => kickParticipant(p.userId)} aria-label={t("removeUserAria", { name: p.userName })} className="rounded p-0.5 text-destructive">
                          <XCircle className="size-3" />
                        </button>
                      </span>
                    )}
                  </div>
                ))}
                {participants.length === 0 && (
                  <p className="text-center text-[11px] text-muted-foreground py-6">{t("noParticipants")}</p>
                )}
              </div>
            ) : (
            // Chat/Q&A log is a live region so new messages are announced instead of being
            // silent for screen-reader users.
            <div
              id={sideTab === "qa" ? "live-side-panel-qa" : "live-side-panel-chat"}
              role="tabpanel"
              aria-labelledby={sideTab === "qa" ? "live-side-tab-qa" : "live-side-tab-chat"}
              aria-live="polite"
              aria-relevant="additions"
              className="flex-1 space-y-2 overflow-y-auto p-4"
            >
              {(() => {
                const visible = chat.filter((c) =>
                  sideTab === "qa" ? c.kind === "QA" || (c.message || "").includes("[Q&A]") : c.kind !== "QA",
                )
                if (visible.length === 0) {
                  return (
                    <p className="text-center text-[11px] text-muted-foreground py-6">
                      {sideTab === "qa" ? t("noQuestions") : t("noMessages")}
                    </p>
                  )
                }
                return visible.map((c, i) => (
                  <div key={i} className={cn("flex gap-2", (c.system || c.deleted) && "justify-center")}>
                    {c.system || c.deleted ? (
                      <span className="text-[10px] text-muted-foreground italic">{c.deleted ? t("messageRemoved") : c.message}</span>
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
                            {sideTab === "qa" && c.answeredAt && (
                              <span className="rounded bg-accent/15 px-1 text-[9px] font-medium text-accent">Answered</span>
                            )}
                          </div>
                          <p className="mt-0.5 text-xs text-muted-foreground">{c.message}</p>
                          <div className="mt-0.5 flex gap-1">
                            {(Object.keys(reactions).length > 0) && (
                              <div className="mt-0.5 flex items-center gap-1">
                                {Object.entries(reactions).map(([emoji, count]) => (
                                  <span
                                    key={emoji}
                                    aria-label={t("reactCountAria", { emoji, count })}
                                    className="inline-flex items-center gap-0.5 rounded-full bg-muted px-1.5 py-0.5 text-[10px]"
                                  >
                                    <span aria-hidden>{emoji}</span>
                                    <span className="text-[9px] text-muted-foreground">{count}</span>
                                  </span>
                                ))}
                              </div>
                            )}
                            {(user?.role === "Teacher" || user?.role === "Admin") && c.userId !== myUserId && (
                              <button
                                type="button"
                                aria-label={t("removeMessageAria")}
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
                                {t("removeButton")}
                              </button>
                            )}
                            {sideTab === "qa" && (user?.role === "Teacher" || user?.role === "Admin") && c.id && (
                              <button
                                type="button"
                                aria-label={c.answeredAt ? "Reopen question" : "Mark question answered"}
                                onClick={() => {
                                  const nextAnswered = c.answeredAt ? null : new Date().toISOString()
                                  setChat((prev) => prev.map((m) => (m === c ? { ...m, answeredAt: nextAnswered } : m)))
                                  if (wsRef.current?.readyState === WebSocket.OPEN) {
                                    wsRef.current.send(JSON.stringify({ type: "QA_MARK_ANSWERED", messageId: c.id }))
                                  }
                                }}
                                className={cn("rounded px-1 text-[10px] hover:bg-primary/10", c.answeredAt ? "text-accent" : "text-primary")}
                              >
                                {c.answeredAt ? "Reopen" : "Mark answered"}
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
                placeholder={sideTab === "qa" ? (isInProgress ? t("askQuestionPlaceholder") : t("qaDisabledPlaceholder")) : isInProgress ? t("typeMessagePlaceholder") : t("chatDisabledPlaceholder")}
                disabled={!isInProgress || !connected}
                aria-label={sideTab === "qa" ? t("askQuestionAria") : t("chatMessageAria")}
                className="h-9 flex-1 rounded-lg border border-border bg-muted/60 px-3 text-xs outline-none placeholder:text-muted-foreground focus:border-ring focus:bg-background disabled:opacity-50"
              />
              <Button type="submit" size="icon" className="size-9 shrink-0" aria-label={tc("send")} disabled={!isInProgress || !connected || !message.trim()}>
                <Send className="size-3.5" />
              </Button>
            </form>
          </div>
        </div>
      </div>
      {showIssueModal && (
        // Real dialog semantics: labelled, modal, Escape closes, focus moves into the dialog.
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4"
          onClick={() => setShowIssueModal(false)}
          onKeyDown={e => { if (e.key === "Escape") setShowIssueModal(false) }}
        >
          <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="live-issue-dialog-title"
            ref={issueDialogRef}
            tabIndex={-1}
            className="w-full max-w-md rounded-xl bg-card p-5 shadow-xl border border-border"
            onClick={e => e.stopPropagation()}
          >
            <div className="flex items-center justify-between mb-4">
              <h3 id="live-issue-dialog-title" className="text-sm font-semibold text-foreground flex items-center gap-2">
                <Flag className="size-4 text-destructive" /> {t("reportIssueTitle")}
              </h3>
              <button
                onClick={() => setShowIssueModal(false)}
                aria-label={tc("close")}
                className="text-muted-foreground hover:text-foreground"
              >
                <XCircle className="size-4" />
              </button>
            </div>
            {issueSent ? (
              <div className="py-4 text-center">
                <p className="text-sm text-teal font-medium">{t("issueReportedTitle")}</p>
                <p className="text-xs text-muted-foreground mt-1">{t("issueReportedDesc")}</p>
                <Button variant="outline" size="sm" className="mt-4" onClick={() => { setShowIssueModal(false); setIssueSent(false); setIssueDescription("") }}>
                  {tc("close")}
                </Button>
              </div>
            ) : (
              <div className="space-y-3">
                <div>
                  <label className="text-xs font-medium text-foreground">{t("issueTypeLabel")}</label>
                  <select
                    value={issueType}
                    onChange={e => setIssueType(e.target.value)}
                    className="mt-1 w-full rounded-lg border border-border bg-muted/60 px-3 py-2 text-xs outline-none"
                  >
                    <option value="CONNECTION_PROBLEM">{t("connectionProblem")}</option>
                    <option value="AUDIO_PROBLEM">{t("audioProblem")}</option>
                    <option value="VIDEO_PROBLEM">{t("videoProblem")}</option>
                    <option value="CHAT_PROBLEM">{t("chatProblem")}</option>
                    <option value="PARTICIPANT_ISSUE">{t("participantIssue")}</option>
                    <option value="OTHER">{t("otherIssue")}</option>
                  </select>
                </div>
                <div>
                  <label className="text-xs font-medium text-foreground">{t("descriptionOptionalLabel")}</label>
                  <textarea
                    value={issueDescription}
                    onChange={e => setIssueDescription(e.target.value)}
                    rows={3}
                    placeholder={t("describeIssuePlaceholder")}
                    className="mt-1 w-full rounded-lg border border-border bg-muted/60 px-3 py-2 text-xs outline-none resize-none"
                  />
                </div>
                <div className="flex justify-end gap-2 pt-1">
                  <Button variant="outline" size="sm" onClick={() => setShowIssueModal(false)}>{tc("cancel")}</Button>
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
                        setJoinError(t("failedSubmitIssue"))
                      } finally {
                        setIssueSubmitting(false)
                      }
                    }}
                  >
                    {issueSubmitting ? t("sendingLabel") : t("submitReportButton")}
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
  disabled,
}: {
  children: React.ReactNode
  label: string
  active?: boolean
  onClick?: () => void
  disabled?: boolean
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      aria-pressed={active}
      className={cn(
        // 44px minimum touch target (was 40px, below the platform guideline).
        "flex size-11 min-h-11 min-w-11 items-center justify-center rounded-full border text-white transition-colors",
        "disabled:cursor-not-allowed disabled:opacity-50",
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
