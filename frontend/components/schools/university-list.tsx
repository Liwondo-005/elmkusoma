"use client"

import Link from "next/link"
import Image from "next/image"
import { MapPin, Users, Star, ArrowRight, CheckCircle } from "lucide-react"
import { getInstitutionsByLevel } from "@/lib/data"
import { cn } from "@/lib/utils"
import { buttonVariants } from "@/components/ui/button"

export function UniversityList() {
  const institutions = getInstitutionsByLevel("Universities")

  return (
    <section className="bg-muted/40 py-12 lg:py-16">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="max-w-2xl">
          <h2 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
            Partner Universities
          </h2>
          <p className="mt-2 text-muted-foreground">
            Browse Tanzania&apos;s top universities. Each institution offers programs across multiple degree levels.
          </p>
        </div>

        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {institutions.map((institution) => (
            <div
              key={institution.id}
              className="group flex flex-col rounded-2xl border border-border bg-card shadow-xs transition-all hover:border-primary/40 hover:shadow-md overflow-hidden"
            >
              <div className="relative h-44 w-full overflow-hidden bg-muted">
                <Image
                  src={institution.images[0]}
                  alt={institution.name}
                  fill
                  className="object-cover transition-transform group-hover:scale-105"
                  sizes="(max-width: 640px) 100vw, (max-width: 1024px) 50vw, 33vw"
                />
                <div className="absolute top-3 left-3 flex gap-1.5">
                  {institution.verified && (
                    <span className="inline-flex items-center gap-1 rounded-full bg-green-500/90 px-2 py-0.5 text-xs font-semibold text-white">
                      <CheckCircle className="size-3" />
                      Verified
                    </span>
                  )}
                  <span className="rounded-full bg-background/90 px-2 py-0.5 text-xs font-medium text-foreground">
                    {institution.ownership}
                  </span>
                </div>
                <div className="absolute top-3 right-3 flex items-center gap-1 rounded-md bg-background/90 px-2 py-0.5">
                  <Star className="size-3 fill-orange text-orange" />
                  <span className="text-xs font-semibold text-foreground">{institution.rating}</span>
                </div>
              </div>

              <div className="flex flex-1 flex-col p-5">
                <h3 className="text-base font-bold text-foreground">{institution.name}</h3>
                <div className="mt-1 flex items-center gap-3 text-xs text-muted-foreground">
                  <span className="flex items-center gap-1">
                    <MapPin className="size-3" />
                    {institution.location}
                  </span>
                  <span className="flex items-center gap-1">
                    <Users className="size-3" />
                    {institution.students.toLocaleString()} students
                  </span>
                </div>

                <p className="mt-3 flex-1 text-sm text-muted-foreground line-clamp-2">{institution.description}</p>

                <div className="mt-4 pt-1">
                  <Link
                    href={`/schools/universities/${institution.id}`}
                    className={cn(
                      buttonVariants({ variant: "outline" }),
                      "h-9 w-full gap-2 border-primary/30 text-primary hover:bg-accent",
                    )}
                  >
                    View Details
                    <ArrowRight className="size-3.5" />
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
