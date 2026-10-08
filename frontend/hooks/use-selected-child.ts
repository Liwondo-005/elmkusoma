"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { parentApi, type ChildOverview } from "@/lib/parent-api"

export const SELECTED_CHILD_STORAGE_KEY = "elmkusoma_parent_selected_child"

const CACHE_TTL_MS = 30000
const CHANGE_EVENT = "elmkusoma:parent-selected-child-changed"

export type SelectedChildSource = "query" | "persisted" | "default" | null

type ChildrenCache = { children: ChildOverview[]; fetchedAt: number }

let cache: ChildrenCache | null = null
let inflight: Promise<ChildOverview[]> | null = null

function fetchChildren(): Promise<ChildOverview[]> {
  if (cache && Date.now() - cache.fetchedAt < CACHE_TTL_MS) {
    return Promise.resolve(cache.children)
  }
  if (!inflight) {
    inflight = parentApi
      .getChildren()
      .then((kids) => {
        cache = { children: Array.isArray(kids) ? kids : [], fetchedAt: Date.now() }
        return cache.children
      })
      .finally(() => {
        inflight = null
      })
  }
  return inflight
}

function readStoredId(): string | null {
  if (typeof window === "undefined") return null
  try {
    const value = window.localStorage.getItem(SELECTED_CHILD_STORAGE_KEY)
    return value ? value : null
  } catch {
    return null
  }
}

function writeStoredId(id: string) {
  if (typeof window === "undefined") return
  try {
    window.localStorage.setItem(SELECTED_CHILD_STORAGE_KEY, id)
  } catch {
    return
  }
}

function readQueryChildId(): string | null {
  if (typeof window === "undefined") return null
  try {
    const value = new URLSearchParams(window.location.search).get("child")
    return value ? value : null
  } catch {
    return null
  }
}

function isLinked(children: ChildOverview[], id: string | null): boolean {
  return !!id && children.some((child) => child.studentId === id)
}

export function useSelectedChild() {
  const [children, setChildren] = useState<ChildOverview[]>([])
  const [selectedChildId, setSelectedChildIdState] = useState<string | null>(null)
  const [selectionSource, setSelectionSource] = useState<SelectedChildSource>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const childrenRef = useRef<ChildOverview[]>([])
  const queryChildIdRef = useRef<string | null>(null)

  useEffect(() => {
    queryChildIdRef.current = readQueryChildId()
  }, [])

  const load = useCallback(async (force = false) => {
    if (force) cache = null
    setLoading(true)
    try {
      const kids = await fetchChildren()
      childrenRef.current = kids
      setChildren(kids)
      const queryId = queryChildIdRef.current
      if (kids.length === 0) {
        setSelectedChildIdState(null)
        setSelectionSource(null)
      } else if (isLinked(kids, queryId)) {
        setSelectedChildIdState(queryId)
        setSelectionSource("query")
        writeStoredId(queryId as string)
      } else if (isLinked(kids, readStoredId())) {
        setSelectedChildIdState(readStoredId())
        setSelectionSource("persisted")
      } else {
        const fallback = kids.find((child) => child.isPrimary) || kids[0]
        setSelectedChildIdState(fallback.studentId)
        setSelectionSource("default")
      }
      setError(null)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "Failed to load children")
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    function syncFromStored() {
      const stored = readStoredId()
      if (!isLinked(childrenRef.current, stored)) return
      setSelectedChildIdState((current) => (current === stored ? current : stored))
      setSelectionSource((current) => (current === "query" ? current : "persisted"))
    }
    function onStorage(event: StorageEvent) {
      if (event.key === SELECTED_CHILD_STORAGE_KEY) syncFromStored()
    }
    window.addEventListener("storage", onStorage)
    window.addEventListener(CHANGE_EVENT, syncFromStored)
    return () => {
      window.removeEventListener("storage", onStorage)
      window.removeEventListener(CHANGE_EVENT, syncFromStored)
    }
  }, [])

  const setSelectedChildId = useCallback((id: string | null) => {
    if (!id || !isLinked(childrenRef.current, id)) return
    setSelectedChildIdState(id)
    setSelectionSource("persisted")
    writeStoredId(id)
    window.dispatchEvent(new Event(CHANGE_EVENT))
  }, [])

  const reload = useCallback(() => {
    void load(true)
  }, [load])

  return { children, selectedChildId, setSelectedChildId, loading, error, reload, selectionSource }
}