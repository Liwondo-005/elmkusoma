"use client"

import { useEffect, useState } from "react"
import { School, Loader2, Save, MapPin, Phone, Mail, Globe, Calendar, Users, Activity, Eye, Settings } from "lucide-react"
import { adminApi, getInstitutionId, type OrgProfileResponse } from "@/lib/api"

export default function OrgProfilePage() {
  const [profile, setProfile] = useState<OrgProfileResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form, setForm] = useState<Record<string, string>>({})

  useEffect(() => { loadProfile() }, [])

  async function loadProfile() {
    const institutionId = getInstitutionId()
    if (!institutionId) { setError("No institution context"); setLoading(false); return }
    try {
      const data = await adminApi.getOrgProfile(institutionId)
      setProfile(data)
      setForm({
        name: data.name || "", description: data.description || "", address: data.address || "",
        city: data.city || "", region: data.region || "", phone: data.phone || "",
        email: data.email || "", website: data.website || "", logoUrl: data.logoUrl || "",
        bannerUrl: data.bannerUrl || "", motto: data.motto || "",
        foundedYear: data.foundedYear?.toString() || "", totalCapacity: data.totalCapacity?.toString() || "",
      })
    } catch (err) { setError(err instanceof Error ? err.message : "Failed to load profile") }
    finally { setLoading(false) }
  }

  async function handleSave() {
    const institutionId = getInstitutionId()
    if (!institutionId) return
    setSaving(true)
    try {
      await adminApi.updateOrgProfile(institutionId, {
        name: form.name, description: form.description, address: form.address,
        city: form.city, region: form.region, phone: form.phone, email: form.email,
        website: form.website, logoUrl: form.logoUrl, bannerUrl: form.bannerUrl,
        motto: form.motto, foundedYear: form.foundedYear ? parseInt(form.foundedYear) : undefined,
        totalCapacity: form.totalCapacity ? parseInt(form.totalCapacity) : undefined,
      })
      setEditing(false); loadProfile()
    } catch (err) { setError(err instanceof Error ? err.message : "Failed to save") }
    finally { setSaving(false) }
  }

  if (loading) return <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center justify-between">
        <div><h1 className="text-2xl font-bold tracking-tight text-foreground">Organization Profile</h1>
          <p className="mt-1 text-sm text-muted-foreground">View and manage your institution profile</p></div>
        {!editing ? (
          <button onClick={() => setEditing(true)} className="inline-flex items-center gap-2 rounded-xl border border-border px-4 py-2.5 text-sm font-medium hover:bg-muted">Edit Profile</button>
        ) : (
          <div className="flex gap-2">
            <button onClick={() => setEditing(false)} className="inline-flex items-center gap-2 rounded-xl border border-border px-4 py-2.5 text-sm font-medium hover:bg-muted">Cancel</button>
            <button onClick={handleSave} disabled={saving} className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground shadow-xs hover:bg-primary/90 disabled:opacity-50">
              <Save className="size-4" />{saving ? "Saving..." : "Save"}
            </button>
          </div>
        )}
      </div>

      {error && <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4"><p className="text-sm font-medium text-destructive">{error}</p></div>}

      {profile && (
        <>
          {/* Profile Header */}
          <div className="rounded-2xl border border-border bg-card overflow-hidden">
            {profile.bannerUrl && <div className="h-32 bg-gradient-to-r from-primary/20 to-primary/5" />}
            <div className="p-6">
              <div className="flex items-start gap-4">
                <div className="flex size-16 items-center justify-center rounded-2xl bg-primary/10 text-2xl font-bold text-primary">
                  {profile.logoUrl ? <img src={profile.logoUrl} alt="" className="size-16 rounded-2xl object-cover" /> : profile.name?.[0]}
                </div>
                <div className="flex-1">
                  {editing ? (
                    <input value={form.name} onChange={(e) => setForm({...form, name: e.target.value})} className="w-full rounded-lg border border-border bg-card px-3 py-2 text-xl font-bold outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
                  ) : (
                    <h2 className="text-xl font-bold text-foreground">{profile.name}</h2>
                  )}
                  <div className="mt-1 flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
                    <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2.5 py-0.5 text-xs font-medium text-primary">{profile.type}</span>
                    <span className="inline-flex items-center gap-1"><School className="size-3" />{profile.code}</span>
                    {profile.motto && <span className="italic">&ldquo;{profile.motto}&rdquo;</span>}
                  </div>
                </div>
              </div>
            </div>
          </div>

          {/* Profile Details */}
          <div className="grid gap-6 sm:grid-cols-2">
            <div className="rounded-2xl border border-border bg-card p-6 space-y-4">
              <h3 className="text-sm font-semibold text-foreground">Contact Information</h3>
              {editing ? (
                <div className="space-y-3">
                  <div><label className="block text-xs text-muted-foreground mb-1">Address</label><input value={form.address} onChange={(e) => setForm({...form, address: e.target.value})} className="w-full rounded-lg border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary" /></div>
                  <div><label className="block text-xs text-muted-foreground mb-1">City</label><input value={form.city} onChange={(e) => setForm({...form, city: e.target.value})} className="w-full rounded-lg border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary" /></div>
                  <div><label className="block text-xs text-muted-foreground mb-1">Region</label><input value={form.region} onChange={(e) => setForm({...form, region: e.target.value})} className="w-full rounded-lg border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary" /></div>
                  <div><label className="block text-xs text-muted-foreground mb-1">Phone</label><input value={form.phone} onChange={(e) => setForm({...form, phone: e.target.value})} className="w-full rounded-lg border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary" /></div>
                  <div><label className="block text-xs text-muted-foreground mb-1">Email</label><input value={form.email} onChange={(e) => setForm({...form, email: e.target.value})} className="w-full rounded-lg border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary" /></div>
                  <div><label className="block text-xs text-muted-foreground mb-1">Website</label><input value={form.website} onChange={(e) => setForm({...form, website: e.target.value})} className="w-full rounded-lg border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary" /></div>
                </div>
              ) : (
                <div className="space-y-3 text-sm">
                  <div className="flex items-center gap-2 text-muted-foreground"><MapPin className="size-4" />{profile.address || "No address"}{profile.city ? `, ${profile.city}` : ""}</div>
                  <div className="flex items-center gap-2 text-muted-foreground"><MapPin className="size-4" />{profile.region || "No region"}</div>
                  {profile.phone && <div className="flex items-center gap-2 text-muted-foreground"><Phone className="size-4" />{profile.phone}</div>}
                  {profile.email && <div className="flex items-center gap-2 text-muted-foreground"><Mail className="size-4" />{profile.email}</div>}
                  {profile.website && <div className="flex items-center gap-2 text-muted-foreground"><Globe className="size-4" />{profile.website}</div>}
                </div>
              )}
            </div>

            <div className="rounded-2xl border border-border bg-card p-6 space-y-4">
              <h3 className="text-sm font-semibold text-foreground">Details</h3>
              {editing ? (
                <div className="space-y-3">
                  <div><label className="block text-xs text-muted-foreground mb-1">Description</label><textarea value={form.description} onChange={(e) => setForm({...form, description: e.target.value})} rows={3} className="w-full rounded-lg border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary resize-none" /></div>
                  <div><label className="block text-xs text-muted-foreground mb-1">Motto</label><input value={form.motto} onChange={(e) => setForm({...form, motto: e.target.value})} className="w-full rounded-lg border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary" /></div>
                  <div className="grid grid-cols-2 gap-3">
                    <div><label className="block text-xs text-muted-foreground mb-1">Founded Year</label><input type="number" value={form.foundedYear} onChange={(e) => setForm({...form, foundedYear: e.target.value})} className="w-full rounded-lg border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary" /></div>
                    <div><label className="block text-xs text-muted-foreground mb-1">Total Capacity</label><input type="number" value={form.totalCapacity} onChange={(e) => setForm({...form, totalCapacity: e.target.value})} className="w-full rounded-lg border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary" /></div>
                  </div>
                </div>
              ) : (
                <div className="space-y-3 text-sm">
                  <p className="text-muted-foreground">{profile.description || "No description"}</p>
                  {profile.motto && <p className="italic text-muted-foreground">&ldquo;{profile.motto}&rdquo;</p>}
                  {profile.foundedYear && <div className="flex items-center gap-2 text-muted-foreground"><Calendar className="size-4" />Founded {profile.foundedYear}</div>}
                  {profile.totalCapacity && <div className="text-muted-foreground">Capacity: {profile.totalCapacity}</div>}
                  <div className="text-xs text-muted-foreground">Status: <span className={profile.isActive ? "text-emerald-600" : "text-red-600"}>{profile.isActive ? "Active" : "Inactive"}</span></div>
                </div>
              )}
            </div>
          </div>

          {/* People Summary */}
          {profile.peopleSummary && (
            <div className="rounded-2xl border border-border bg-card p-6">
              <div className="mb-4 flex items-center gap-2"><Users className="size-4 text-muted-foreground" /><h3 className="text-sm font-semibold text-foreground">People Summary</h3></div>
              <div className="grid gap-4 sm:grid-cols-5">
                {[
                  { label: "Total Users", value: profile.peopleSummary.totalUsers },
                  { label: "Teachers", value: profile.peopleSummary.totalTeachers },
                  { label: "Students", value: profile.peopleSummary.totalStudents },
                  { label: "Parents", value: profile.peopleSummary.totalParents },
                  { label: "Pending Invites", value: profile.peopleSummary.pendingInvitations },
                ].map((stat) => (
                  <div key={stat.label} className="text-center"><p className="text-2xl font-bold text-foreground">{stat.value}</p><p className="text-xs text-muted-foreground">{stat.label}</p></div>
                ))}
              </div>
            </div>
          )}

          {/* Enabled Services */}
          {profile.enabledServices && profile.enabledServices.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-6">
              <div className="mb-4 flex items-center gap-2"><Settings className="size-4 text-muted-foreground" /><h3 className="text-sm font-semibold text-foreground">Enabled Services</h3></div>
              <div className="flex flex-wrap gap-2">
                {profile.enabledServices.map((service) => (
                  <span key={service} className="inline-flex items-center rounded-full bg-primary/10 px-3 py-1 text-xs font-medium text-primary">{service}</span>
                ))}
              </div>
            </div>
          )}

          {/* Recent Activity */}
          {profile.activitySummary && profile.activitySummary.recentActivity.length > 0 && (
            <div className="rounded-2xl border border-border bg-card p-6">
              <div className="mb-4 flex items-center gap-2"><Activity className="size-4 text-muted-foreground" /><h3 className="text-sm font-semibold text-foreground">Recent Activity</h3></div>
              <div className="space-y-2">
                {profile.activitySummary.recentActivity.map((a) => (
                  <div key={a.id} className="flex items-center gap-3 rounded-lg border border-border px-4 py-2">
                    <div className="size-2 shrink-0 rounded-full bg-primary" />
                    <div className="min-w-0 flex-1"><p className="text-sm font-medium text-foreground truncate">{a.title}</p></div>
                    <span className="shrink-0 text-xs text-muted-foreground">{new Date(a.createdAt).toLocaleDateString()}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
