"use client"

import Link from "next/link"
import {
  CheckCircle,
  MapPin,
  Phone,
  Mail,
  Globe,
  Users,
  GraduationCap,
  Star,
  ArrowLeft,
  Building2,
  Shield,
  BookOpen,
  ExternalLink,
} from "lucide-react"
import { Institution } from "@/lib/data"
import { Carousel } from "@/components/ui/carousel"
import { Button, buttonVariants } from "@/components/ui/button"
import { cn } from "@/lib/utils"

const educationLevelRoutes: Record<Institution["educationLevel"], string> = {
  Nursery: "/schools/nursery",
  Primary: "/schools/primary",
  Secondary: "/schools/secondary",
  Colleges: "/schools/colleges",
  Vocational: "/schools/vocational",
  Universities: "/schools/universities",
}

const educationLevelColors: Record<Institution["educationLevel"], string> = {
  Nursery: "bg-pink-500/10 text-pink-600",
  Primary: "bg-blue-500/10 text-blue-600",
  Secondary: "bg-purple-500/10 text-purple-600",
  Colleges: "bg-orange/10 text-orange",
  Vocational: "bg-emerald-500/10 text-emerald-600",
  Universities: "bg-indigo-500/10 text-indigo-600",
}

interface InstitutionDetailProps {
  institution: Institution
}

export function InstitutionDetail({ institution }: InstitutionDetailProps) {
  const backHref = educationLevelRoutes[institution.educationLevel]
  const hasHigherEdPrograms =
    (institution.educationLevel === "Colleges" ||
      institution.educationLevel === "Vocational" ||
      institution.educationLevel === "Universities") &&
    institution.programs &&
    institution.programs.length > 0
  const hasAdmissionUrl =
    (institution.educationLevel === "Colleges" ||
      institution.educationLevel === "Vocational" ||
      institution.educationLevel === "Universities") &&
    institution.admissionUrl

  return (
    <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <Link
        href={backHref}
        className="mb-6 inline-flex items-center gap-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
      >
        <ArrowLeft className="size-4" />
        Browse more {institution.educationLevel.toLowerCase()} schools
      </Link>

      <div className="grid gap-8 lg:grid-cols-[1fr_1.2fr]">
        <div className="space-y-6">
          <div className="overflow-hidden rounded-2xl border border-border bg-card shadow-xs">
            <Carousel images={institution.images} alt={institution.name} />
          </div>

          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-2xl font-bold tracking-tight text-foreground sm:text-3xl">
                {institution.name}
              </h1>
              {institution.verified && (
                <span className="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-xs font-semibold text-emerald-600">
                  <CheckCircle className="size-3.5" />
                  Verified
                </span>
              )}
            </div>

            <div className="mt-3 flex flex-wrap items-center gap-2">
              <span
                className={cn(
                  "inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold",
                  institution.ownership === "Government"
                    ? "bg-blue-500/10 text-blue-600"
                    : "bg-violet-500/10 text-violet-600"
                )}
              >
                <Shield className="size-3.5" />
                {institution.ownership}
              </span>
              <span
                className={cn(
                  "inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold",
                  educationLevelColors[institution.educationLevel]
                )}
              >
                <GraduationCap className="size-3.5" />
                {institution.educationLevel}
              </span>
              {institution.secondaryLevel && (
                <span
                  className={cn(
                    "inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-xs font-semibold",
                    institution.secondaryLevel === "CO-School"
                      ? "bg-primary/10 text-primary"
                      : institution.secondaryLevel === "A-Level"
                        ? "bg-teal/10 text-teal"
                        : "bg-orange/10 text-orange"
                  )}
                >
                  {institution.secondaryLevel === "CO-School"
                    ? "Form I–VI"
                    : institution.secondaryLevel === "A-Level"
                      ? "Form V–VI"
                      : "Form I–IV"}
                </span>
              )}
            </div>

            <div className="mt-5 grid grid-cols-3 gap-4">
              <div className="rounded-xl bg-muted/60 p-3 text-center">
                <Users className="mx-auto size-5 text-primary" />
                <p className="mt-1 text-lg font-bold text-foreground">
                  {institution.students.toLocaleString()}
                </p>
                <p className="text-xs text-muted-foreground">Students</p>
              </div>
              {institution.teachers !== undefined && (
                <div className="rounded-xl bg-muted/60 p-3 text-center">
                  <BookOpen className="mx-auto size-5 text-primary" />
                  <p className="mt-1 text-lg font-bold text-foreground">
                    {institution.teachers.toLocaleString()}
                  </p>
                  <p className="text-xs text-muted-foreground">Teachers</p>
                </div>
              )}
              <div className="rounded-xl bg-muted/60 p-3 text-center">
                <Star className="mx-auto size-5 fill-orange text-orange" />
                <p className="mt-1 text-lg font-bold text-foreground">{institution.rating}</p>
                <p className="text-xs text-muted-foreground">Rating</p>
              </div>
            </div>

            <div className="mt-5 flex items-center gap-2 text-sm text-muted-foreground">
              <MapPin className="size-4 shrink-0 text-primary" />
              {institution.location}
            </div>
          </div>
        </div>

        <div className="space-y-6">
          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <h2 className="text-lg font-bold text-foreground">About</h2>
            <p className="mt-3 text-sm leading-relaxed text-muted-foreground">
              {institution.description}
            </p>
          </div>

          {hasHigherEdPrograms && (
            <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
              <h2 className="text-lg font-bold text-foreground">Programs Offered</h2>
              <div className="mt-4 flex flex-wrap gap-2">
                {institution.programs!.map((program) => (
                  <span
                    key={program}
                    className="rounded-lg bg-primary/10 px-3 py-1.5 text-xs font-semibold text-primary"
                  >
                    {program}
                  </span>
                ))}
              </div>
            </div>
          )}

          {institution.facilities.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
              <h2 className="text-lg font-bold text-foreground">Facilities</h2>
              <div className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-3">
                {institution.facilities.map((facility) => (
                  <div
                    key={facility}
                    className="flex items-center gap-2 rounded-xl border border-border bg-muted/40 p-3"
                  >
                    <Building2 className="size-4 shrink-0 text-primary" />
                    <span className="text-xs font-medium text-foreground">{facility}</span>
                  </div>
                ))}
              </div>
            </div>
          )}

          {(institution.phone || institution.email || institution.website) && (
            <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
              <h2 className="text-lg font-bold text-foreground">Contact Information</h2>
              <ul className="mt-4 space-y-3">
                {institution.phone && (
                  <li className="flex items-center gap-3 text-sm">
                    <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                      <Phone className="size-4 text-primary" />
                    </span>
                    <span className="text-muted-foreground">{institution.phone}</span>
                  </li>
                )}
                {institution.email && (
                  <li className="flex items-center gap-3 text-sm">
                    <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                      <Mail className="size-4 text-primary" />
                    </span>
                    <span className="text-muted-foreground">{institution.email}</span>
                  </li>
                )}
                {institution.website && (
                  <li className="flex items-center gap-3 text-sm">
                    <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-primary/10">
                      <Globe className="size-4 text-primary" />
                    </span>
                    <a
                      href={institution.website}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-muted-foreground transition-colors hover:text-primary"
                    >
                      {institution.website}
                    </a>
                  </li>
                )}
              </ul>
            </div>
          )}

          {(institution.educationLevel === "Colleges" ||
            institution.educationLevel === "Vocational" ||
            institution.educationLevel === "Universities") && (
            <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
              {hasAdmissionUrl ? (
                <a
                  href={institution.admissionUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className={cn(
                    buttonVariants({ className: "h-11 w-full gap-2 text-sm" })
                  )}
                >
                  Apply Now
                  <ExternalLink className="size-4" />
                </a>
              ) : (
                <Button disabled className="h-11 w-full gap-2 text-sm">
                  Admission information coming soon
                </Button>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
