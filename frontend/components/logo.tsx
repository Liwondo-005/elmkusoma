import Link from "next/link"
import { cn } from "@/lib/utils"

export function Logo({
  className,
  href = "/",
  showText = true,
}: {
  className?: string
  href?: string
  showText?: boolean
}) {
  return (
    <Link href={href} className={cn("flex items-center gap-2", className)} aria-label="ELMKUSOMA home">
      <span className="relative inline-flex h-9 w-9 items-center justify-center" aria-hidden="true">
        <svg viewBox="0 0 40 40" className="h-9 w-9" fill="none" xmlns="http://www.w3.org/2000/svg">
          {/* sprout leaves */}
          <path
            d="M20 17c-1.2-4.2-5-6.5-9.2-6.2 0 4.3 2.9 7.9 7 8.6"
            fill="#0d9488"
          />
          <path
            d="M20 17c1.2-4.6 5-7.2 9.4-6.9 0 4.6-3 8.4-7.2 9.1"
            fill="#f59e0b"
          />
          {/* open book */}
          <path
            d="M6 20c4.2-2 9.2-2 14 1 4.8-3 9.8-3 14-1v11c-4.2-2-9.2-2-14 1-4.8-3-9.8-3-14-1V20Z"
            fill="#2563eb"
          />
          <path d="M20 22v10" stroke="#ffffff" strokeWidth="1.4" strokeLinecap="round" opacity="0.7" />
        </svg>
      </span>
      {showText && (
        <span className="text-lg font-extrabold tracking-tight text-foreground">
          ELM<span className="text-primary">KUSOMA</span>
        </span>
      )}
    </Link>
  )
}
