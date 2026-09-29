"use client"

import { useState, useEffect, useCallback, useRef } from "react"

export interface CommandPaletteItem {
  id: string
  title: string
  description?: string
  shortcut?: string
  category: string
  action: () => void
  icon?: React.ReactNode
  disabled?: boolean
  keywords?: string[]
}

export interface CommandPaletteCategory {
  id: string
  label: string
  items: CommandPaletteItem[]
}

export function useCommandPalette() {
  const [isOpen, setIsOpen] = useState(false)
  const [selectedIndex, setSelectedIndex] = useState(0)
  const [query, setQuery] = useState("")
  const [categories, setCategories] = useState<CommandPaletteCategory[]>([])
  const [recentItems, setRecentItems] = useState<CommandPaletteItem[]>([])
  const inputRef = useRef<HTMLInputElement>(null)
  const itemsRef = useRef<HTMLDivElement[]>([])

  const filteredCategories = categories.map((category) => ({
    ...category,
    items: category.items.filter((item) => {
      if (!query) return true
      const searchText = `${item.title} ${item.description || ""} ${item.keywords?.join(" ") || ""} ${item.category}`.toLowerCase()
      return searchText.includes(query.toLowerCase())
    }),
  })).filter((cat) => cat.items.length > 0)

  const allFilteredItems = filteredCategories.flatMap((cat) => cat.items)

  useEffect(() => {
    setSelectedIndex(0)
  }, [query])

  const open = useCallback(() => {
    setIsOpen(true)
    setQuery("")
    setSelectedIndex(0)
    setTimeout(() => inputRef.current?.focus(), 0)
  }, [])

  const close = useCallback(() => {
    setIsOpen(false)
    setQuery("")
    setSelectedIndex(0)
  }, [])

  const selectItem = useCallback((index: number) => {
    const item = allFilteredItems[index]
    if (item && !item.disabled) {
      item.action()
      close()
    }
  }, [allFilteredItems, close])

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    switch (e.key) {
      case "ArrowDown":
        e.preventDefault()
        setSelectedIndex((prev) => Math.min(prev + 1, allFilteredItems.length - 1))
        break
      case "ArrowUp":
        e.preventDefault()
        setSelectedIndex((prev) => Math.max(prev - 1, 0))
        break
      case "Enter":
        e.preventDefault()
        selectItem(selectedIndex)
        break
      case "Escape":
        e.preventDefault()
        close()
        break
      default:
        break
    }
  }, [allFilteredItems.length, selectedIndex, selectItem, close])

  useEffect(() => {
    function handleGlobalKeyDown(e: KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault()
        if (isOpen) {
          close()
        } else {
          open()
        }
      }
    }

    document.addEventListener("keydown", handleGlobalKeyDown)
    return () => document.removeEventListener("keydown", handleGlobalKeyDown)
  }, [isOpen, open, close])

  const registerCategories = useCallback((newCategories: CommandPaletteCategory[]) => {
    setCategories(newCategories)
  }, [])

  const addRecentItem = useCallback((item: CommandPaletteItem) => {
    setRecentItems((prev) => {
      const filtered = prev.filter((i) => i.id !== item.id)
      return [item, ...filtered].slice(0, 5)
    })
  }, [])

  return {
    isOpen,
    open,
    close,
    query,
    setQuery,
    selectedIndex,
    setSelectedIndex,
    categories: filteredCategories,
    allFilteredItems,
    recentItems,
    registerCategories,
    addRecentItem,
    inputRef,
    handleKeyDown,
    selectItem,
  }
}

export function useCommandPaletteGlobal() {
  const palette = useCommandPalette()

  useEffect(() => {
    function handleGlobalKeyDown(e: KeyboardEvent) {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault()
        if (palette.isOpen) {
          palette.close()
        } else {
          palette.open()
        }
      }
    }

    document.addEventListener("keydown", handleGlobalKeyDown)
    return () => document.removeEventListener("keydown", handleGlobalKeyDown)
  }, [palette])

  return palette
}