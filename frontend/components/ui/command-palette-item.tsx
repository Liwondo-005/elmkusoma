"use client"

import { cn } from "@/lib/utils"
import type { CommandPaletteItem } from "@/hooks/use-command-palette"

interface CommandPaletteItemProps {
  item: CommandPaletteItem
  isSelected: boolean
  onSelect: () => void
}

export function CommandPaletteItem({ item, isSelected, onSelect }: CommandPaletteItemProps) {
  return (
    <button
      type="button"
      onClick={onSelect}
      disabled={item.disabled}
      className={cn(
        "flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-sm transition-colors",
        isSelected
          ? "bg-primary text-primary-foreground"
          : "text-foreground hover:bg-muted",
        item.disabled && "opacity-50 pointer-events-none"
      )}
      role="option"
      aria-selected={isSelected}
      aria-disabled={item.disabled}
    >
      {item.icon && (
        <span className="flex size-4 shrink-0 items-center justify-center" aria-hidden="true">
          {item.icon}
        </span>
      )}
      <div className="min-w-0 flex-1">
        <p className="font-medium truncate">{item.title}</p>
        {item.description && (
          <p className="text-xs truncate opacity-70">{item.description}</p>
        )}
      </div>
      {item.shortcut && (
        <kbd className="hidden shrink-0 rounded border border-border px-1.5 py-0.5 text-[10px] font-mono opacity-60 sm:inline-flex">
          {item.shortcut}
        </kbd>
      )}
    </button>
  )
}

interface CommandPaletteCategoryProps {
  label: string
  children: React.ReactNode
  itemCount: number
}

export function CommandPaletteCategory({ label, children, itemCount }: CommandPaletteCategoryProps) {
  return (
    <div>
      <p className="px-3 py-1.5 text-[10px] font-semibold uppercase tracking-wider text-muted-foreground">
        {label} <span className="text-[9px] font-normal text-muted-foreground/60">({itemCount})</span>
      </p>
      <div className="space-y-0.5">{children}</div>
    </div>
  )
}

interface CommandPaletteEmptyProps {
  query: string
}

export function CommandPaletteEmpty({ query }: CommandPaletteEmptyProps) {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
      <svg className="size-12 text-muted-foreground/50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
      </svg>
      <p className="mt-3 text-sm text-muted-foreground">
        {query
          ? "No commands match your search"
          : "Type to search commands..."}
      </p>
    </div>
  )
}

interface CommandPaletteLoadingProps {}

export function CommandPaletteLoading() {
  return (
    <div className="space-y-0.5 px-3 py-2">
      {[...Array(5)].map((_, i) => (
        <div key={i} className="h-10 rounded-lg bg-muted/50 animate-pulse" />
      ))}
    </div>
  )
}