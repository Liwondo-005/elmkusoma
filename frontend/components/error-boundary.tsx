"use client"

import { Component, type ReactNode, type ErrorInfo } from "react"
import { AlertTriangle, RefreshCw } from "lucide-react"
import { NextIntlClientProvider, useTranslations } from "next-intl"
import { Button } from "@/components/ui/button"
import { messagesForLocale, type AppLocale } from "@/components/locale-provider"

interface Props {
  children: ReactNode
  fallback?: ReactNode
}

interface State {
  hasError: boolean
  error: Error | null
}

// This boundary wraps every provider (see app/providers.tsx), so the fallback
// cannot rely on an ancestor NextIntlClientProvider — it resolves its own
// locale + messages here (NEXT_LOCALE cookie written by LocaleProvider).
function readLocale(): AppLocale {
  try {
    const match = document.cookie.match(/(?:^|;\s*)NEXT_LOCALE=([^;]*)/)
    if (match?.[1] === "sw") return "sw"
  } catch {}
  return "en"
}

function ErrorFallback({ message, onReset }: { message: string | null; onReset: () => void }) {
  const t = useTranslations("common")
  return (
    <div className="flex min-h-[30vh] flex-col items-center justify-center rounded-2xl border border-destructive/20 bg-destructive/5 p-8 text-center">
      <AlertTriangle className="size-10 text-destructive" />
      <h2 className="mt-4 text-lg font-semibold text-foreground">{t("error.generic")}</h2>
      <p className="mt-2 max-w-md text-sm text-muted-foreground">
        {message || t("error.unexpected")}
      </p>
      <Button onClick={onReset} variant="outline" className="mt-4 gap-2">
        <RefreshCw className="size-4" /> {t("retry")}
      </Button>
    </div>
  )
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("[ErrorBoundary]", error, errorInfo)
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null })
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback
      const locale = readLocale()
      return (
        <NextIntlClientProvider locale={locale} messages={messagesForLocale(locale)}>
          <ErrorFallback message={this.state.error?.message ?? null} onReset={this.handleReset} />
        </NextIntlClientProvider>
      )
    }
    return this.props.children
  }
}
