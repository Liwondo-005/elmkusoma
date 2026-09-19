"use client"

import { createContext, useContext, useEffect, useState, type ReactNode } from "react"

interface LowBandwidthContextType {
  isLowBandwidth: boolean
  reducedAnimations: boolean
  lazyLoadImages: boolean
}

const LowBandwidthContext = createContext<LowBandwidthContextValue>({ isLowBandwidth: false, reducedAnimations: false, lazyLoadImages: false })

export function useLowBandwidth() {
  return useContext(LowBandwidthContext)
}

export function LowBandwidthProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState({ isLowBandwidth: false, reducedAnimations: false, lazyLoadImages: false })

  useEffect(() => {
    const conn = (navigator as any).connection
    if (conn) {
      const isSlow = conn.effectiveType === "2g" || conn.effectiveType === "slow-2g" || conn.saveData
      setState({ isLowBandwidth: isSlow, reducedAnimations: isSlow, lazyLoadImages: isSlow || conn.effectiveType === "3g" })
    }
    const prefersReduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches
    if (prefersReduced) {
      setState(prev => ({ ...prev, reducedAnimations: true }))
    }
  }, [])

  return <LowBandwidthContext.Provider value={state}>{children}</LowBandwidthContext.Provider>
}
