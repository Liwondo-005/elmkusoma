"use client"

import { useEffect } from "react"

export const REALTIME_EVENT_NAME = "elmkusoma:realtime"

export interface RealtimePayload {
  type?: string
  [key: string]: unknown
}

const NULL_BYTE = "\u0000"
const MAX_RECONNECT_ATTEMPTS = 5
const RECONNECT_BACKOFF_MS = [1000, 2000, 4000, 8000, 16000]
const HEARTBEAT_INTERVAL_MS = 10000
const CONNECT_TIMEOUT_MS = 10000

let socket: WebSocket | null = null
let stopped = false
let connected = false
let reconnectAttempts = 0
let subscriptionSeq = 0
let receiveBuffer = ""
let heartbeatTimer: ReturnType<typeof setInterval> | null = null
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let connectTimeoutTimer: ReturnType<typeof setTimeout> | null = null
let claims: { userId?: string; institutionId?: string } = {}

function readToken(): string | null {
  try {
    return localStorage.getItem("elmkusoma_access_token")
  } catch {
    return null
  }
}

function decodeClaims(token: string): { userId?: string; institutionId?: string } {
  try {
    const payload = token.split(".")[1]
    if (!payload) return {}
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/")
    const padded = base64 + "=".repeat((4 - (base64.length % 4)) % 4)
    const bytes = atob(padded)
    const percentEncoded = Array.from(bytes, (c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0")).join("")
    const json = JSON.parse(decodeURIComponent(percentEncoded))
    return {
      userId: typeof json.userId === "string" ? json.userId : undefined,
      institutionId: typeof json.institutionId === "string" ? json.institutionId : undefined,
    }
  } catch {
    return {}
  }
}

function buildUrl(): string {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:"
  const host = process.env.NEXT_PUBLIC_REALTIME_HOST || window.location.host
  return `${protocol}//${host}/ws/websocket`
}

function buildFrame(command: string, headers: Record<string, string> = {}, body = ""): string {
  let frame = command + "\n"
  for (const [name, value] of Object.entries(headers)) {
    frame += `${name}:${value}\n`
  }
  return frame + "\n" + body + NULL_BYTE
}

function sendFrame(command: string, headers: Record<string, string> = {}, body = ""): boolean {
  if (!socket || socket.readyState !== WebSocket.OPEN) return false
  try {
    socket.send(buildFrame(command, headers, body))
    return true
  } catch {
    return false
  }
}

function dispatch(payload: RealtimePayload) {
  window.dispatchEvent(new CustomEvent(REALTIME_EVENT_NAME, { detail: payload }))
}

function startHeartbeat() {
  stopHeartbeat()
  heartbeatTimer = setInterval(() => {
    if (!socket || socket.readyState !== WebSocket.OPEN) return
    try {
      socket.send("\n")
    } catch {}
  }, HEARTBEAT_INTERVAL_MS)
}

function stopHeartbeat() {
  if (heartbeatTimer) {
    clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

function clearConnectTimeout() {
  if (connectTimeoutTimer) {
    clearTimeout(connectTimeoutTimer)
    connectTimeoutTimer = null
  }
}

function clearReconnectTimer() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

function sendSubscriptions() {
  if (claims.userId) {
    subscriptionSeq += 1
    sendFrame("SUBSCRIBE", {
      id: `sub-${subscriptionSeq}`,
      destination: `/queue/notifications/${claims.userId}`,
    })
  }
  if (claims.institutionId) {
    subscriptionSeq += 1
    sendFrame("SUBSCRIBE", {
      id: `sub-${subscriptionSeq}`,
      destination: `/topic/institution/${claims.institutionId}/notifications`,
    })
    subscriptionSeq += 1
    sendFrame("SUBSCRIBE", {
      id: `sub-${subscriptionSeq}`,
      destination: `/topic/institution/${claims.institutionId}/presence`,
    })
  }
}

function handleFrame(raw: string) {
  if (!raw) return
  const headerBodySplit = raw.indexOf("\n\n")
  const headerBlock = headerBodySplit === -1 ? raw : raw.slice(0, headerBodySplit)
  const body = headerBodySplit === -1 ? "" : raw.slice(headerBodySplit + 2)
  const lines = headerBlock.split("\n")
  const command = lines[0]
  const headers: Record<string, string> = {}
  for (let i = 1; i < lines.length; i++) {
    const separator = lines[i].indexOf(":")
    if (separator === -1) continue
    headers[lines[i].slice(0, separator)] = lines[i].slice(separator + 1)
  }

  if (command === "CONNECTED") {
    clearConnectTimeout()
    connected = true
    reconnectAttempts = 0
    startHeartbeat()
    sendSubscriptions()
    return
  }

  if (command === "ERROR") {
    stopped = true
    if (headers.message) {
      try {
        console.warn("Realtime STOMP error:", headers.message)
      } catch {}
    }
    if (socket) socket.close()
    return
  }

  if (command === "MESSAGE") {
    if (!body) return
    try {
      const payload = JSON.parse(body) as RealtimePayload
      if (payload && typeof payload === "object") dispatch(payload)
    } catch {}
  }
}

function onChunk(chunk: string) {
  receiveBuffer += chunk
  for (;;) {
    if (!receiveBuffer) return
    if (receiveBuffer[0] === "\n" || receiveBuffer[0] === "\r") {
      receiveBuffer = receiveBuffer.slice(1)
      continue
    }
    const end = receiveBuffer.indexOf(NULL_BYTE)
    if (end === -1) return
    const raw = receiveBuffer.slice(0, end)
    receiveBuffer = receiveBuffer.slice(end + 1)
    handleFrame(raw)
  }
}

function scheduleReconnect() {
  if (stopped || reconnectTimer) return
  if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) return
  const delay = RECONNECT_BACKOFF_MS[Math.min(reconnectAttempts, RECONNECT_BACKOFF_MS.length - 1)]
  reconnectAttempts += 1
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    connect()
  }, delay)
}

function connect() {
  if (stopped || typeof window === "undefined") return
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return

  const token = readToken()
  if (!token) {
    stopped = true
    return
  }
  claims = decodeClaims(token)
  if (!claims.userId) {
    stopped = true
    return
  }

  connected = false
  receiveBuffer = ""

  let nextSocket: WebSocket
  try {
    nextSocket = new WebSocket(buildUrl())
  } catch {
    scheduleReconnect()
    return
  }
  socket = nextSocket

  clearConnectTimeout()
  connectTimeoutTimer = setTimeout(() => {
    if (!connected && nextSocket.readyState === WebSocket.OPEN) {
      nextSocket.close()
    }
  }, CONNECT_TIMEOUT_MS)

  nextSocket.onopen = () => {
    if (socket !== nextSocket) return
    if (stopped) {
      nextSocket.close()
      return
    }
    sendFrame("CONNECT", {
      "accept-version": "1.2",
      host: window.location.host,
      "heart-beat": "10000,10000",
      token,
    })
  }

  nextSocket.onmessage = (event) => {
    if (socket !== nextSocket) return
    if (typeof event.data === "string") onChunk(event.data)
  }

  nextSocket.onerror = () => {
    try {
      nextSocket.close()
    } catch {}
  }

  nextSocket.onclose = () => {
    if (socket !== nextSocket) return
    socket = null
    connected = false
    stopHeartbeat()
    clearConnectTimeout()
    scheduleReconnect()
  }
}

export function startRealtime() {
  if (typeof window === "undefined") return
  stopped = false
  reconnectAttempts = 0
  if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) return
  if (connected) return
  clearReconnectTimer()
  connect()
}

export function stopRealtime() {
  stopped = true
  connected = false
  stopHeartbeat()
  clearConnectTimeout()
  clearReconnectTimer()
  if (socket) {
    try {
      socket.close()
    } catch {}
    socket = null
  }
}

export function subscribeToRealtime(
  handler: (payload: RealtimePayload) => void,
  types?: string[]
): () => void {
  const listener = (event: Event) => {
    const detail = (event as CustomEvent<RealtimePayload>).detail
    if (!detail || typeof detail !== "object") return
    if (types && types.length > 0) {
      if (!detail.type || !types.includes(detail.type)) return
    }
    handler(detail)
  }
  window.addEventListener(REALTIME_EVENT_NAME, listener)
  return () => window.removeEventListener(REALTIME_EVENT_NAME, listener)
}

export function useRealtimeConnection() {
  useEffect(() => {
    startRealtime()
    return () => stopRealtime()
  }, [])
}
