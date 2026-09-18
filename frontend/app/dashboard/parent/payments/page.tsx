"use client"

import { useEffect, useState } from "react"
import { CreditCard, Loader2, CheckCircle, Clock, AlertCircle } from "lucide-react"
import { parentApi, type PaymentItem, type ParentPayments } from "@/lib/parent-api"

export default function ParentPaymentsPage() {
  const [payments, setPayments] = useState<ParentPayments | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    parentApi.getPayments().then(setPayments).catch(() => setPayments(null)).finally(() => setLoading(false))
  }, [])

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-foreground">Payments</h1>
        <p className="mt-1 text-sm text-muted-foreground">Manage payments and view transaction history</p>
      </div>

      {/* Summary */}
      <div className="grid gap-4 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Outstanding</p>
          <p className="mt-1 text-2xl font-extrabold text-red-600">
            TSh {(payments?.outstanding || 0).toLocaleString()}
          </p>
        </div>
        <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
          <p className="text-xs font-medium text-muted-foreground">Paid This Term</p>
          <p className="mt-1 text-2xl font-extrabold text-green-600">
            TSh {(payments?.paidThisTerm || 0).toLocaleString()}
          </p>
        </div>
      </div>

      {/* Pending */}
      {payments && payments.pendingPayments.length > 0 && (
        <section className="rounded-2xl border border-orange-200 bg-orange-50 p-5 shadow-xs dark:border-orange-900 dark:bg-orange-950/30">
          <h2 className="text-base font-semibold text-foreground">Pending Payments</h2>
          <div className="mt-3 space-y-2">
            {payments.pendingPayments.map((p) => (
              <PaymentRow key={p.id} payment={p} />
            ))}
          </div>
        </section>
      )}

      {/* History */}
      <section className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <h2 className="text-base font-semibold text-foreground">Payment History</h2>
        {!payments || payments.recentPayments.length === 0 ? (
          <div className="mt-6 py-8 text-center">
            <CreditCard className="mx-auto mb-3 size-8 text-muted-foreground" />
            <p className="text-sm text-muted-foreground">No payment history</p>
          </div>
        ) : (
          <div className="mt-3 space-y-2">
            {payments.recentPayments.map((p) => (
              <PaymentRow key={p.id} payment={p} />
            ))}
          </div>
        )}
      </section>
    </div>
  )
}

function PaymentRow({ payment }: { payment: PaymentItem }) {
  const statusConfig: Record<string, { icon: typeof CheckCircle; color: string; label: string }> = {
    COMPLETED: { icon: CheckCircle, color: "text-green-600", label: "Paid" },
    PENDING: { icon: Clock, color: "text-orange", label: "Pending" },
    PROCESSING: { icon: Clock, color: "text-blue-500", label: "Processing" },
    FAILED: { icon: AlertCircle, color: "text-red-500", label: "Failed" },
  }
  const cfg = statusConfig[payment.status] || statusConfig.PENDING
  const StatusIcon = cfg.icon

  return (
    <div className="flex items-center gap-3 rounded-xl border border-border bg-muted/30 p-3">
      <StatusIcon className={`size-4 shrink-0 ${cfg.color}`} />
      <div className="min-w-0 flex-1">
        <p className="text-sm font-medium text-foreground">{payment.description || payment.serviceType}</p>
        <p className="text-xs text-muted-foreground">
          {payment.paidAt ? new Date(payment.paidAt).toLocaleDateString("en-GB") : new Date(payment.createdAt).toLocaleDateString("en-GB")}
          {payment.providerReference && ` · ${payment.providerReference}`}
        </p>
      </div>
      <div className="text-right">
        <p className="text-sm font-bold text-foreground">TSh {payment.amount.toLocaleString()}</p>
        <p className={`text-xs ${cfg.color}`}>{cfg.label}</p>
      </div>
    </div>
  )
}
