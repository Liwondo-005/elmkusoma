"use client"

import Link from "next/link"
import { Shield, ArrowLeft } from "lucide-react"

interface PermissionDeniedProps {
  title?: string
  message?: string
  backHref?: string
}

export function PermissionDenied({ title = "Access Restricted", message = "You don't have permission to view this page. Ask your teacher for help.", backHref = "/dashboard" }: PermissionDeniedProps) {
  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="text-center">
        <div className="mx-auto flex size-20 items-center justify-center rounded-full bg-red-50">
          <Shield className="size-10 text-red-500" />
        </div>
        <h2 className="mt-6 text-xl font-bold text-foreground">{title}</h2>
        <p className="mt-2 text-sm text-muted-foreground max-w-md mx-auto">{message}</p>
        <Link href={backHref} className="mt-6 inline-flex items-center gap-2 rounded-xl bg-primary px-5 py-2.5 text-sm font-medium text-primary-foreground hover:bg-primary/90">
          <ArrowLeft className="size-4" /> Back to Dashboard
        </Link>
      </div>
    </div>
  )
}
