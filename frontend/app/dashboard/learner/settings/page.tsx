"use client"

import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl"
import { LoadingState } from "@/components/learner/shared"
import { Settings, User, Mail, Shield, GraduationCap, Key, ExternalLink } from "lucide-react"
import Link from "next/link"

export default function LearnerSettingsPage() {
  const { user, loading: authLoading } = useAuth()
  const t = useTranslations("learner")
  const tc = useTranslations("common")

  if (authLoading || (user?.role !== "Other Learner" && user?.role !== "Student")) {
    return <LoadingState />
  }

  return (
    <div className="mx-auto max-w-6xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("lsettings.title")}</h1>
        <p className="mt-1 text-sm text-muted-foreground">{t("lsettings.subtitle")}
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-semibold text-foreground mb-4">{t("lsettings.account")}
        <div className="space-y-4">
          <div className="flex items-center gap-4 rounded-xl border border-border p-4">
            <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10">
              <User className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-sm font-medium text-foreground">{t("lsettings.fullName")}</p>
              <p className="text-sm text-muted-foreground">{user?.firstName} {user?.lastName}</p>
            </div>
          </div>
          <div className="flex items-center gap-4 rounded-xl border border-border p-4">
            <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10">
              <Mail className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-sm font-medium text-foreground">{t("lsettings.emailLabel")}</p>
              <p className="text-sm text-muted-foreground">{user?.email}</p>
            </div>
          </div>
          <div className="flex items-center gap-4 rounded-xl border border-border p-4">
            <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10">
              <Shield className="size-5 text-primary" />
            </div>
            <div>
              <p className="text-sm font-medium text-foreground">{t("lsettings.roleLabel")}</p>
              <p className="text-sm text-muted-foreground">{user?.role}</p>
            </div>
          </div>
          {user?.learningLevel && (
            <div className="flex items-center gap-4 rounded-xl border border-border p-4">
              <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10">
                <GraduationCap className="size-5 text-primary" />
              </div>
              <div>
                <p className="text-sm font-medium text-foreground">{t("lsettings.levelLabel")}</p>
                <p className="text-sm text-muted-foreground">{user.learningLevel}</p>
              </div>
            </div>
          )}
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-semibold text-foreground mb-4">{t("lsettings.security")}
        <div className="space-y-4">
          <div className="flex items-center justify-between rounded-xl border border-border p-4">
            <div className="flex items-center gap-4">
              <div className="flex size-10 items-center justify-center rounded-lg bg-primary/10">
                <Key className="size-5 text-primary" />
              </div>
              <div>
                <p className="text-sm font-medium text-foreground">{t("lsettings.passwordLabel")}</p>
                <p className="text-xs text-muted-foreground">{t("lsettings.passwordDesc")}</p>
              </div>
            </div>
            <Link
              href="/forgot-password"
              className="inline-flex items-center gap-1.5 rounded-lg border border-border bg-background px-4 py-2 text-xs font-medium text-foreground hover:bg-muted"
            >
              {t("lsettings.change")} <ExternalLink className="size-3" />
            </Link>
          </div>
        </div>
      </div>

      <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
        <h2 className="text-lg font-semibold text-foreground mb-4">{t("lsettings.about")}
        <p className="text-sm text-muted-foreground">
          {t("lsettings.aboutBody")}
        </p>
        <p className="mt-2 text-xs text-muted-foreground">
          {t("lsettings.aboutContact")}
        </p>
      </div>
    </div>
  )
}
