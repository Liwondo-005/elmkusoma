"use client"

import { useTranslations } from "next-intl"
import { Award, BadgeCheck } from "lucide-react"

interface Certificate {
  id: string
  studentName: string
  courseTitle: string
  instructor: string
  completionDate: string
  grade: string
  skills: string[]
}

export function CertificateCard({ certificate }: { certificate: Certificate }) {
  const t = useTranslations("ui")
  return (
    <div className="overflow-hidden rounded-2xl border border-border bg-card shadow-xs">
      <div className="border-b border-border bg-accent/50 px-6 py-4">
        <div className="flex items-center gap-3">
          <div className="flex size-11 items-center justify-center rounded-xl bg-orange/10">
            <Award className="size-5 text-orange" />
          </div>
          <div>
            <p className="text-xs font-medium text-muted-foreground">{t("certificateCard.completionTitle")}</p>
            <p className="text-sm font-semibold text-foreground">ELMKUSOMA</p>
          </div>
        </div>
      </div>

      <div className="px-6 py-6">
        <p className="text-center text-xs font-medium uppercase tracking-widest text-muted-foreground">
          {t("certificateCard.certifyLine1")}
        </p>
        <p className="mt-2 text-center text-2xl font-bold text-foreground">{certificate.studentName}</p>
        <p className="mt-2 text-center text-xs text-muted-foreground">
          {t("certificateCard.certifyLine2")}
        </p>
        <p className="mt-2 text-center text-lg font-semibold text-primary">{certificate.courseTitle}</p>

        <div className="mt-6 space-y-3 rounded-xl border border-border bg-muted/40 p-4">
          <div className="flex items-center gap-3 text-sm">
            <span className="text-muted-foreground">{t("certificateCard.instructor")}</span>
            <span className="font-medium text-foreground">{certificate.instructor}</span>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="text-muted-foreground">{t("certificateCard.completed")}</span>
            <span className="font-medium text-foreground">{certificate.completionDate}</span>
          </div>
          <div className="flex items-center gap-3 text-sm">
            <span className="text-muted-foreground">{t("certificateCard.grade")}</span>
            <span className="font-medium text-foreground">{certificate.grade}</span>
          </div>
        </div>

        {certificate.skills.length > 0 && (
          <div className="mt-5">
            <p className="text-xs font-medium text-muted-foreground">{t("certificateCard.skills")}</p>
            <div className="mt-2 flex flex-wrap gap-2">
              {certificate.skills.map((skill) => (
                <span
                  key={skill}
                  className="inline-flex items-center rounded-md bg-accent px-2.5 py-1 text-xs font-medium text-accent-foreground"
                >
                  {skill}
                </span>
              ))}
            </div>
          </div>
        )}

        <div className="mt-6 flex items-center justify-center gap-2 rounded-xl border border-teal/20 bg-teal/5 px-4 py-3">
          <BadgeCheck className="size-5 text-teal" />
          <span className="text-sm font-semibold text-teal">{t("certificateCard.verified")}</span>
        </div>

        <p className="mt-4 text-center text-xs text-muted-foreground">
          {t("certificateCard.certId")} <span className="font-mono font-medium text-foreground">{certificate.id}</span>
        </p>
      </div>
    </div>
  )
}
