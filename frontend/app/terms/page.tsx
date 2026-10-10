"use client"

import { useTranslations } from "next-intl"
import { LegalDocumentPage } from "@/components/legal-document-page"
import { useSiteSettings } from "@/hooks/use-site-settings"

export default function TermsPage() {
  const t = useTranslations("public")
  const { value } = useSiteSettings()
  const contactEmail = value("public.contact.email")

  // Fallback body, used only until a Platform Admin publishes a Terms version.
  const fallback = (
    <>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("terms.s1t")}</h2>
        <p className="mt-3">{t("terms.s1b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("terms.s2t")}</h2>
        <p className="mt-3">{t("terms.s2b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("terms.s3t")}</h2>
        <p className="mt-3">{t("terms.s3b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("terms.s4t")}</h2>
        <p className="mt-3">{t("terms.s4b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("terms.s5t")}</h2>
        <p className="mt-3">{t("terms.s5b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("terms.s6t")}</h2>
        <p className="mt-3">{t("terms.s6b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("terms.s7t")}</h2>
        <p className="mt-3">{t("terms.s7b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("terms.s8t")}</h2>
        <p className="mt-3">{t("terms.s8b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("terms.s9t")}</h2>
        <p className="mt-3">{t("terms.s9b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("terms.s10t")}</h2>
        <p className="mt-3">{t("terms.s10b")}</p>
      </div>
      {contactEmail && (
        <div>
          <h2 className="text-base font-semibold text-foreground">{t("terms.s11t")}</h2>
          <p className="mt-3">
            {t("terms.s11pre")}{" "}
            <a href={`mailto:${contactEmail}`} className="text-primary hover:underline">
              {contactEmail}
            </a>
          </p>
        </div>
      )}
    </>
  )

  return <LegalDocumentPage type="TERMS" fallbackTitle={t("terms.title")} fallback={fallback} />
}
