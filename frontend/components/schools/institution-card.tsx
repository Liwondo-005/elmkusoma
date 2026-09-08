"use client"

import Link from "next/link"
import Image from "next/image"
import { MapPin, Users, Star, CheckCircle, Building2 } from "lucide-react"
import { Institution } from "@/lib/data"
import { cn } from "@/lib/utils"
import { buttonVariants } from "@/components/ui/button"

type Props = {
  institution: Institution
  detailHref: string
}

export function InstitutionCard({ institution, detailHref }: Props) {
  return (
    <div className="group flex flex-col overflow-hidden rounded-2xl border border-border bg-card shadow-xs transition-all duration-300 hover:border-primary/30 hover:shadow-lg">
      {/* Image */}
      <div className="relative h-48 w-full overflow-hidden bg-muted">
        {institution.images.length > 0 ? (
          <Image
            src={institution.images[0]}
            alt={institution.name}
            fill
            className="object-cover transition-transform duration-500 group-hover:scale-105"
            sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <Building2 className="size-12 text-muted-foreground/30" />
          </div>
        )}

        {/* Top-left badges */}
        <div className="absolute top-3 left-3 flex gap-1.5">
          {institution.verified && (
            <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/90 px-2 py-0.5 text-xs font-semibold text-white shadow-sm">
              <CheckCircle className="size-3" />
              Verified
            </span>
          )}
          <span className="rounded-full bg-background/90 px-2 py-0.5 text-xs font-medium text-foreground backdrop-blur-sm">
            {institution.ownership}
          </span>
        </div>

        {/* Top-right rating */}
        <div className="absolute top-3 right-3 flex items-center gap-1 rounded-lg bg-background/90 px-2 py-1 backdrop-blur-sm">
          <Star className="size-3.5 fill-orange text-orange" />
          <span className="text-xs font-bold text-foreground">{institution.rating}</span>
        </div>
      </div>

      {/* Content */}
      <div className="flex flex-1 flex-col p-5">
        <h3 className="text-base font-bold text-foreground transition-colors group-hover:text-primary">
          {institution.name}
        </h3>
        <div className="mt-1.5 flex items-center gap-3 text-xs text-muted-foreground">
          <span className="flex items-center gap-1">
            <MapPin className="size-3 shrink-0" />
            {institution.location}
          </span>
          <span className="flex items-center gap-1">
            <Users className="size-3 shrink-0" />
            {institution.students.toLocaleString()} students
          </span>
        </div>

        <p className="mt-3 flex-1 text-sm leading-relaxed text-muted-foreground line-clamp-2">
          {institution.description}
        </p>

        <div className="mt-4 pt-1">
          <Link
            href={detailHref}
            className={cn(
              buttonVariants(),
              "h-10 w-full gap-2 transition-all duration-200 hover:bg-primary/90"
            )}
          >
            View Details
          </Link>
        </div>
      </div>
    </div>
  )
}
