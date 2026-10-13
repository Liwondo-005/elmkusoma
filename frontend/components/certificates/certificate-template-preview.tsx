"use client"

import { useTranslations } from "next-intl"

/**
 * Renders a certificate template's stored markup as a design preview.
 *
 * <p>The content is HTML an institution administrator typed. It is sanitised on the way in
 * (CertificateTemplateContentSanitizer), but a renderer should not depend on that having happened
 * for every row already in the table - a template written before the rules existed is still a
 * row in the table. So the preview is a document with the most restrictive sandbox the browser
 * offers: an empty {@code sandbox} attribute denies scripts, forms, same-origin access and
 * top-level navigation, which is what turns markup into a picture rather than an actor.</p>
 *
 * <p>It is also deliberately inert to the pointer. The operator is looking at the result, not
 * operating it, and a preview that could be clicked would be indistinguishable from the real
 * certificate.</p>
 */
export function CertificateTemplatePreview({
  htmlContent,
  cssContent,
  height = 260,
  title,
}: {
  htmlContent?: string | null
  cssContent?: string | null
  height?: number
  title?: string
}) {
  const t = useTranslations("learner")

  const html = (htmlContent || "").trim()
  const css = (cssContent || "").trim()

  if (!html && !css) {
    return (
      <div
        className="flex items-center justify-center rounded-xl border border-dashed border-border bg-muted/40 text-xs text-muted-foreground"
        style={{ height }}
      >
        {t("certTemplates.previewEmpty")}
      </div>
    )
  }

  // Assembled here rather than as a bare srcDoc string so the document is a complete,
  // parseable document - quirks mode would silently change the very layout being previewed.
  const srcDoc = [
    "<!doctype html>",
    '<html lang="en"><head><meta charset="utf-8">',
    '<meta name="referrer" content="no-referrer">',
    "<style>",
    // A neutral page so the preview reads like paper rather than the dashboard behind it.
    "html,body{margin:0;padding:16px;background:#fff;color:#111;",
    "font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;}",
    css,
    "</style></head><body>",
    html,
    "</body></html>",
  ].join("\n")

  return (
    <div className="overflow-hidden rounded-xl border border-border bg-white">
      <iframe
        srcDoc={srcDoc}
        title={title || t("certTemplates.previewTitle")}
        // Empty sandbox: every capability is off. Combined with srcDoc there is no network
        // navigation, and referrerPolicy keeps the no-referrer meta honest at the frame level.
        sandbox=""
        referrerPolicy="no-referrer"
        loading="lazy"
        style={{ height, width: "100%", border: 0, pointerEvents: "none" }}
        className="block"
      />
    </div>
  )
}
