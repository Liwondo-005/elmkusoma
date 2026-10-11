"use client"

import { useTranslations } from "next-intl"
import { MapPin, ExternalLink, Navigation, Clock, Building2 } from "lucide-react"
import { useSiteSettings } from "@/hooks/use-site-settings"

/**
 * ELMKUSOMA Headquarters location section.
 *
 * <p>Reads the same `public.location.*` settings the Platform Admin screen edits, so the location
 * published here is always the location that was saved rather than a second copy in the code.</p>
 *
 * <p>Honesty rules this component deliberately follows:</p>
 * <ul>
 *   <li>The campus, city and country come from configuration. Nothing about the location is
 *       hardcoded here, so the page cannot claim an office that the administrator has not
 *       confirmed.</li>
 *   <li>The "View on Map" and "Get Directions" actions render <em>only</em> when a destination URL
 *       has actually been configured. An unconfigured map button is a dead control that implies a
 *       precision the platform does not have, so it is omitted instead of rendered disabled.</li>
 *   <li>Building, street address and opening hours appear only when set. Until the owner verifies
 *       them the page states the campus-level location and stops there, rather than padding it out
 *       with a plausible-looking building name.</li>
 * </ul>
 */
export function HeadquartersSection() {
  const t = useTranslations("siteLocation")
  const { value } = useSiteSettings()

  const name = value("public.location.name")
  const campus = value("public.location.campus")
  const city = value("public.location.city")
  const country = value("public.location.country")
  const building = value("public.location.building")
  const address = value("public.location.address")
  const mapUrl = value("public.location.mapUrl")
  const directionsUrl = value("public.location.directionsUrl")
  const description = value("public.location.description")
  const hours = value("public.location.hours")

  // The location line is assembled from whichever parts are configured. With the campus, city and
  // country seeded it always renders; a section with no location at all is not worth showing.
  const locationLine = [campus, city, country].filter(Boolean).join(", ")
  if (!name && !locationLine) return null

  return (
    <section className="border-t border-border bg-card py-16 lg:py-20" aria-labelledby="headquarters-heading">
      <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="grid items-start gap-8 lg:grid-cols-[1.2fr_1fr] lg:gap-12">
          <div>
            <p className="inline-flex items-center gap-2 rounded-full bg-primary/10 px-3 py-1 text-xs font-semibold text-primary">
              <MapPin className="size-3.5" aria-hidden="true" />
              {t("eyebrow")}
            </p>
            <h2
              id="headquarters-heading"
              className="mt-4 text-balance text-3xl font-bold tracking-tight text-foreground sm:text-4xl"
            >
              {name || t("fallbackName")}
            </h2>

            {locationLine && (
              <p className="mt-4 flex items-start gap-2.5 text-base font-medium text-foreground">
                <MapPin className="mt-0.5 size-5 shrink-0 text-primary" aria-hidden="true" />
                <span>{locationLine}</span>
              </p>
            )}

            {description && (
              <p className="mt-4 max-w-prose text-pretty text-muted-foreground">{description}</p>
            )}

            {/* Optional detail. Each line appears only when it has been verified and saved. */}
            {building && (
              <p className="mt-3 flex items-start gap-2.5 text-sm text-muted-foreground">
                <Building2 className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                <span>{building}</span>
              </p>
            )}
            {address && (
              <p className="mt-2 flex items-start gap-2.5 text-sm text-muted-foreground">
                <MapPin className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                <span>{address}</span>
              </p>
            )}
            {hours && (
              <p className="mt-2 flex items-start gap-2.5 text-sm text-muted-foreground">
                <Clock className="mt-0.5 size-4 shrink-0" aria-hidden="true" />
                <span>{hours}</span>
              </p>
            )}

            <div className="mt-7 flex flex-wrap gap-3">
              {mapUrl && (
                <a
                  href={mapUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex h-11 items-center gap-2 rounded-lg bg-primary px-5 text-sm font-semibold text-primary-foreground transition-colors hover:bg-primary/90"
                >
                  <MapPin className="size-4" aria-hidden="true" />
                  {t("viewOnMap")}
                </a>
              )}
              {directionsUrl && (
                <a
                  href={directionsUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex h-11 items-center gap-2 rounded-lg border border-border bg-background px-5 text-sm font-semibold text-foreground transition-colors hover:bg-muted/60"
                >
                  <Navigation className="size-4" aria-hidden="true" />
                  {t("getDirections")}
                </a>
              )}
            </div>
          </div>

          {/*
            Map panel. Deliberately not an embedded third-party iframe: an embed would load a
            tracker on every public page view and would need an API key, and it would pin the
            page to one provider's coverage. When no verified destination is configured this
            renders an honest, accessible text panel instead of a broken or empty iframe.
          */}
          <div className="rounded-2xl border border-border bg-muted/40 p-6 sm:p-8">
            <div className="flex size-12 items-center justify-center rounded-xl bg-primary/10">
              <MapPin className="size-6 text-primary" aria-hidden="true" />
            </div>
            <h3 className="mt-4 text-lg font-bold text-foreground">{name || t("fallbackName")}</h3>
            {locationLine && <p className="mt-1.5 text-sm font-medium text-foreground">{locationLine}</p>}

            {mapUrl ? (
              <a
                href={mapUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-5 inline-flex items-center gap-2 text-sm font-semibold text-primary hover:underline"
              >
                {t("viewOnMap")}
                <ExternalLink className="size-3.5" aria-hidden="true" />
              </a>
            ) : (
              <p className="mt-5 rounded-lg border border-dashed border-border bg-background px-4 py-3 text-sm text-muted-foreground">
                {t("mapNotAvailable")}
              </p>
            )}
          </div>
        </div>
      </div>
    </section>
  )
}
