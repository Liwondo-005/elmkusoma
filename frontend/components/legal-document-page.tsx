"use client"

import { useEffect, useState } from "react"
import { useTranslations } from "next-intl"
import { SiteHeader } from "@/components/site-header"
import { SiteFooter } from "@/components/site-footer"
import { Loader2, FileWarning } from "lucide-react"
import { publicSiteApi, type LegalDocumentView } from "@/lib/public-site-api"

/**
 * Renders the published version of a legal document.
 *
 * <p>Content comes from the platform's own published copy so Platform Admin can revise it
 * without a code deploy. If nothing has been published, `children` is rendered instead - the
 * caller supplies its existing translated text. That fallback is deliberately visible as
 * "unpublished" rather than silently swapping in different wording, because a visitor reading
 * a legal page should not be quietly shown text that is not the published version.</p>
 *
 * <p>The document body is rendered as React text nodes. `content` is plain text by server
 * contract; there is no `dangerouslySetInnerHTML` anywhere in this path, so legal content can
 * never execute.</p>
 */
export function LegalDocumentPage({
  type,
  fallbackTitle,
  fallback,
}: {
  type: "TERMS" | "PRIVACY" | "COOKIE" | "SUPPORT_POLICY"
  fallbackTitle: string
  fallback: React.ReactNode
}) {
  const tLegal = useTranslations("legal")
  const [document_, setDocument] = useState<LegalDocumentView | null>(null)
  const [state, setState] = useState<"loading" | "published" | "unpublished">("loading")

  useEffect(() => {
    let active = true
    setState("loading")
    publicSiteApi
      .getLegalDocument(type)
      .then((doc) => {
        if (!active) return
        setDocument(doc)
        setState("published")
      })
      .catch(() => {
        // 404 means no published version yet. That is a state, not an error worth shouting about.
        if (active) setState("unpublished")
      })
    return () => {
      active = false
    }
  }, [type])

  // Paragraphs are separated by blank lines. Splitting on that keeps the stored format simple
  // and means a blank line is a real, predictable break.
  const paragraphs = (document_?.content ?? "")
    .split(/\n\s*\n/)
    .map((block) => block.trim())
    .filter((block) => block.length > 0)

  return (
    <div className="flex min-h-dvh flex-col">
      <SiteHeader />
      <main className="flex-1">
        <section className="mx-auto max-w-3xl px-4 py-16 sm:px-6 lg:px-8">
          <h1 className="text-3xl font-bold tracking-tight text-foreground">
            {document_?.title || fallbackTitle}
          </h1>

          {state === "loading" && (
            <p className="mt-6 flex items-center gap-2 text-sm text-muted-foreground">
              <Loader2 className="size-4 animate-spin" aria-hidden="true" />
              {tLegal("loading")}
            </p>
          )}

          {state === "published" && document_ && (
            <>
              <p className="mt-2 text-sm text-muted-foreground">
                {tLegal("versionMeta", {
                  version: document_.version,
                  effectiveDate: document_.effectiveDate ?? document_.publishedAt ?? "",
                })}
              </p>
              <div className="mt-10 space-y-6 text-sm leading-relaxed text-muted-foreground">
                {paragraphs.map((paragraph, index) => (
                  // Paragraph text has no stable identity of its own; the index is the correct
                  // key here because the list is static and never reordered.
                  <p key={index}>{paragraph}</p>
                ))}
              </div>
            </>
          )}

          {state === "unpublished" && (
            <>
              <p
                className="mt-6 flex items-start gap-2 rounded-lg border border-amber-500/30 bg-amber-500/10 px-3 py-2 text-xs text-amber-700 dark:text-amber-300"
                role="status"
              >
                <FileWarning className="mt-0.5 size-3.5 shrink-0" aria-hidden="true" />
                <span>{tLegal("noPublishedVersion")}</span>
              </p>
              <div className="mt-8 space-y-8 text-sm leading-relaxed text-muted-foreground">
                {fallback}
              </div>
            </>
          )}
        </section>
      </main>
      <SiteFooter />
    </div>
  )
}