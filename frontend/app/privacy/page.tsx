"use client"

import { useTranslations } from "next-intl"
import { LegalDocumentPage } from "@/components/legal-document-page"
import { useSiteSettings } from "@/hooks/use-site-settings"

export default function PrivacyPage() {
  const t = useTranslations("public")
  const { value } = useSiteSettings()
  const contactEmail = value("public.contact.email")

  // Fallback body, used only until a Platform Admin publishes a Privacy version.
  const fallback = (
    <>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("privacy.s1t")}</h2>
        <p className="mt-3">{t("privacy.s1b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("privacy.s2t")}</h2>
        <p className="mt-3">{t("privacy.s2b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("privacy.s3t")}</h2>
        <p className="mt-3">{t("privacy.s3b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("privacy.s4t")}</h2>
        <p className="mt-3">{t("privacy.s4b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("privacy.s5t")}</h2>
        <p className="mt-3">{t("privacy.s5b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("privacy.s6t")}</h2>
        <p className="mt-3">{t("privacy.s6b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("privacy.s7t")}</h2>
        <p className="mt-3">{t("privacy.s7b")}</p>
      </div>
      <div>
        <h2 className="text-base font-semibold text-foreground">{t("privacy.s8t")}</h2>
        <p className="mt-3">{t("privacy.s8b")}</p>
      </div>
      {contactEmail && (
        <div>
          <h2 className="text-base font-semibold text-foreground">{t("privacy.s9t")}</h2>
          <p className="mt-3">
            {t("privacy.s9pre")}{" "}
            <a href={`mailto:${contactEmail}`} className="text-primary hover:underline">
              {contactEmail}
            </a>
          </p>
        </div>
      )}
    </>
  )

  return <LegalDocumentPage type="PRIVACY" fallbackTitle={t("privacy.title")} fallback={fallback} />
}
