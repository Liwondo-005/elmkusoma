"use client"

import { useCallback, useEffect, useState } from "react"

/**
 * Real camera/microphone device enumeration for the Live control room and
 * classroom. No device names are hardcoded: labels come from the browser's
 * enumerateDevices() API and only appear once the user has granted permission.
 * Every failure path (denied / missing / in use / unsupported) is surfaced
 * honestly instead of pretending a device is available.
 */

export interface MediaDeviceOption {
  deviceId: string
  label: string
}

export type MediaErrorReason =
  | "denied"
  | "not-found"
  | "in-use"
  | "unsupported"
  | "over-constrained"
  | "unknown"

export interface MediaErrorInfo {
  reason: MediaErrorReason
  message: string
}

export function describeMediaError(err: unknown, device: "camera" | "microphone" = "camera"): MediaErrorInfo {
  const name = (err as { name?: string })?.name || ""
  switch (name) {
    case "NotAllowedError":
    case "PermissionDeniedError":
    case "SecurityError":
      return {
        reason: "denied",
        message: `Permission to use the ${device} was denied. Allow access in your browser's site settings and try again.`,
      }
    case "NotFoundError":
    case "DevicesNotFoundError":
      return {
        reason: "not-found",
        message: `No ${device} was found on this device.`,
      }
    case "NotReadableError":
    case "TrackStartError":
      return {
        reason: "in-use",
        message: `The ${device} is already in use by another application.`,
      }
    case "OverconstrainedError":
      return {
        reason: "over-constrained",
        message: `The selected ${device} is no longer available. It may have been unplugged.`,
      }
    case "NotSupportedError":
      return {
        reason: "unsupported",
        message: `Media capture is not supported in this browser context (HTTPS or localhost is required).`,
      }
    default:
      return {
        reason: "unknown",
        message: err instanceof Error && err.message ? err.message : `Could not access the ${device}.`,
      }
  }
}

function labelDevices(devices: MediaDeviceInfo[], fallbackKind: string): MediaDeviceOption[] {
  return devices
    .filter((d) => d.deviceId && d.deviceId !== "default" && d.deviceId !== "communications")
    .map((d, i) => ({
      deviceId: d.deviceId,
      // Before permission the label is an empty string — never invent a name.
      label: d.label?.trim() || `${fallbackKind} ${i + 1}`,
    }))
}

export function useMediaDevices() {
  const [cameras, setCameras] = useState<MediaDeviceOption[]>([])
  const [microphones, setMicrophones] = useState<MediaDeviceOption[]>([])
  const [hasLabels, setHasLabels] = useState(false)
  const [permission, setPermission] = useState<"unknown" | "granted" | "prompt" | "denied">("unknown")
  const [error, setError] = useState<MediaErrorInfo | null>(null)
  const [supported, setSupported] = useState(true)

  const refresh = useCallback(async () => {
    if (typeof navigator === "undefined" || !navigator.mediaDevices?.enumerateDevices) {
      setSupported(false)
      setError({ reason: "unsupported", message: "Device selection is not supported in this browser." })
      return
    }
    try {
      const devices = await navigator.mediaDevices.enumerateDevices()
      setCameras(labelDevices(devices.filter((d) => d.kind === "videoinput"), "Camera"))
      setMicrophones(labelDevices(devices.filter((d) => d.kind === "audioinput"), "Microphone"))
      const labeled = devices.some((d) => (d.label || "").trim().length > 0)
      setHasLabels(labeled)
      setPermission((prev) => {
        if (labeled) return "granted"
        if (prev === "denied") return "denied"
        return "prompt"
      })
    } catch (err) {
      setError(describeMediaError(err))
    }
  }, [])

  useEffect(() => {
    refresh()
    const md = typeof navigator !== "undefined" ? navigator.mediaDevices : undefined
    if (!md?.addEventListener) return
    // Hot-plug: a USB camera plugged in or unplugged shows up without a reload.
    const onChange = () => {
      refresh()
    }
    md.addEventListener("devicechange", onChange)
    return () => md.removeEventListener("devicechange", onChange)
  }, [refresh])

  /** Ask for permission once — device labels only exist after a grant. */
  const requestPermission = useCallback(async () => {
    if (!navigator.mediaDevices?.getUserMedia) {
      setSupported(false)
      return false
    }
    try {
      await navigator.mediaDevices.getUserMedia({ video: true, audio: true })
      setPermission("granted")
      setError(null)
      return true
    } catch (err) {
      const info = describeMediaError(err)
      setError(info)
      if (info.reason === "denied") setPermission("denied")
      return false
    } finally {
      refresh()
    }
  }, [refresh])

  /** Acquire a camera stream from a specific device (or the default one). */
  const getCameraStream = useCallback(async (deviceId?: string): Promise<MediaStream> => {
    const video: boolean | MediaTrackConstraints = deviceId ? { deviceId: { exact: deviceId } } : true
    return navigator.mediaDevices.getUserMedia({ video, audio: false })
  }, [])

  /** Acquire a microphone stream from a specific device (or the default one). */
  const getMicrophoneStream = useCallback(async (deviceId?: string): Promise<MediaStream> => {
    const audio: boolean | MediaTrackConstraints = deviceId ? { deviceId: { exact: deviceId } } : true
    return navigator.mediaDevices.getUserMedia({ video: false, audio })
  }, [])

  return {
    cameras,
    microphones,
    hasLabels,
    permission,
    error,
    supported,
    refresh,
    requestPermission,
    getCameraStream,
    getMicrophoneStream,
    setError,
  }
}
