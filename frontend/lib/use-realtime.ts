"use client"

import { useEffect, useRef, useCallback, useState } from "react"

interface RealtimeOptions {
  institutionId: string
  userId: string
  token: string
  onNotification?: (notification: any) => void
  onPresenceUpdate?: (data: any) => void
}

export function useRealtime({ institutionId, userId, token, onNotification, onPresenceUpdate }: RealtimeOptions) {
  const wsRef = useRef<WebSocket | null>(null)
  const [connected, setConnected] = useState(false)
  const reconnectTimeoutRef = useRef<NodeJS.Timeout>(undefined)

  const connect = useCallback(() => {
    if (wsRef.current?.readyState === WebSocket.OPEN) return

    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:"
    const wsHost = process.env.NEXT_PUBLIC_REALTIME_HOST || window.location.hostname
    const wsPort = process.env.NEXT_PUBLIC_REALTIME_PORT || "8081"
    const wsUrl = `${protocol}//${wsHost}:${wsPort}/ws?token=${encodeURIComponent(token)}&institutionId=${institutionId}&userId=${userId}`

    try {
      const ws = new WebSocket(wsUrl)
      wsRef.current = ws

      ws.onopen = () => {
        setConnected(true)
        ws.send(JSON.stringify({ type: "SUBSCRIBE", destination: `/topic/institution/${institutionId}/notifications` }))
        ws.send(JSON.stringify({ type: "SUBSCRIBE", destination: `/topic/institution/${institutionId}/presence` }))
        ws.send(JSON.stringify({ type: "SUBSCRIBE", destination: `/queue/notifications/${userId}` }))
      }

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (data.type === "NOTIFICATION") onNotification?.(data.payload)
          if (data.type === "PRESENCE_UPDATE") onPresenceUpdate?.(data.payload)
        } catch {}
      }

      ws.onclose = () => {
        setConnected(false)
        reconnectTimeoutRef.current = setTimeout(connect, 5000)
      }

      ws.onerror = () => ws.close()
    } catch {}
  }, [institutionId, userId, token, onNotification, onPresenceUpdate])

  useEffect(() => {
    connect()
    return () => {
      clearTimeout(reconnectTimeoutRef.current)
      wsRef.current?.close()
    }
  }, [connect])

  const send = useCallback((data: any) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(JSON.stringify(data))
    }
  }, [])

  return { connected, send }
}
