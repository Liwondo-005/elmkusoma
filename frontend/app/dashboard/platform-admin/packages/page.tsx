"use client"

import { Package } from "lucide-react"

export default function PlatformPackagesPage() {
  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <h1 className="flex items-center gap-2 text-xl font-bold tracking-tight text-foreground"><span className="flex size-8 items-center justify-center rounded-lg bg-teal-600 text-white"><Package className="size-4" /></span> Packages</h1>
        <p className="mt-1 text-sm text-muted-foreground">Service packages and bundles — pricing, entitlements, and lifecycle. Spec ref: platform_admin.md § packages. Placeholder pending package catalogue.</p>
      </div>
      <div className="rounded-2xl border border-border bg-card p-5 shadow-sm">
        <div className="flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-muted/30 py-12 text-center">
          <Package className="size-10 text-muted-foreground/50" />
          <p className="mt-3 text-sm font-semibold text-foreground">No packages yet</p>
          <p className="mt-1 max-w-md text-xs leading-relaxed text-muted-foreground">Package definitions require catalogue service. No synthetic packages are shown — empty state per spec §13. When available, bundles and pricing will be governed here.</p>
        </div>
      </div>
    </div>
  )
}
