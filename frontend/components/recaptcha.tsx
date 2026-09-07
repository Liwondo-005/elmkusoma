"use client"

import { useEffect, useRef, useState } from "react"

declare global {
  interface Window {
    grecaptcha: {
      ready: (callback: () => void) => void
      render: (container: HTMLElement, options: { sitekey: string; callback: (token: string) => void; "expired-callback": () => void }) => number
      reset: (widgetId?: number) => void
    }
  }
}

interface RecaptchaProps {
  siteKey: string
  onVerify: (token: string) => void
  onExpire: () => void
}

export function Recaptcha({ siteKey, onVerify, onExpire }: RecaptchaProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const widgetIdRef = useRef<number | null>(null)
  const [loaded, setLoaded] = useState(false)

  useEffect(() => {
    if (document.getElementById("recaptcha-script")) {
      setLoaded(true)
      return
    }

    const script = document.createElement("script")
    script.id = "recaptcha-script"
    script.src = "https://www.google.com/recaptcha/api.js"
    script.async = true
    script.defer = true
    script.onload = () => setLoaded(true)
    document.head.appendChild(script)
  }, [])

  useEffect(() => {
    if (!loaded || !containerRef.current || widgetIdRef.current !== null) return

    const checkReady = setInterval(() => {
      if (window.grecaptcha?.ready) {
        clearInterval(checkReady)
        window.grecaptcha.ready(() => {
          if (containerRef.current) {
            widgetIdRef.current = window.grecaptcha.render(containerRef.current, {
              sitekey,
              callback: onVerify,
              "expired-callback": onExpire,
            })
          }
        })
      }
    }, 100)

    return () => clearInterval(checkReady)
  }, [loaded, siteKey, onVerify, onExpire])

  return (
    <div className="flex items-center gap-3">
      <div ref={containerRef} />
      {!loaded && (
        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <div className="size-4 animate-spin rounded-full border-2 border-primary border-t-transparent" />
          Loading verification...
        </div>
      )}
    </div>
  )
}
