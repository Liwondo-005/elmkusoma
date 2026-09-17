"use client"
import { useState } from "react"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"

export default function SettingsPage() {
  const [providerName, setProviderName] = useState("")
  const [contactEmail, setContactEmail] = useState("")
  const [contactPhone, setContactPhone] = useState("")
  const [address, setAddress] = useState("")

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Settings</h1>
        <p className="text-muted-foreground">Configure your NFE provider profile</p>
      </div>
      <Card>
        <CardHeader><CardTitle>Provider Information</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="text-sm font-medium text-foreground">Provider Name</label>
            <input
              value={providerName}
              onChange={(e) => setProviderName(e.target.value)}
              placeholder="Enter provider name"
              className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-foreground">Contact Email</label>
            <input
              value={contactEmail}
              onChange={(e) => setContactEmail(e.target.value)}
              placeholder="email@example.com"
              className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-foreground">Contact Phone</label>
            <input
              value={contactPhone}
              onChange={(e) => setContactPhone(e.target.value)}
              placeholder="+255..."
              className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label className="text-sm font-medium text-foreground">Address</label>
            <textarea
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              placeholder="Enter address"
              rows={3}
              className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <button className="inline-flex items-center rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90">
            Save Changes
          </button>
        </CardContent>
      </Card>
    </div>
  )
}
