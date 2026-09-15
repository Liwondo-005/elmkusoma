"use client"

import { useState, useEffect } from "react"
import { Settings, Save, Loader2, AlertCircle, GraduationCap, Plus, Trash2, Award } from "lucide-react"
import { useAuth } from "@/lib/auth"
import { teacherFetch, type TeacherQualification } from "@/lib/teacher-api"

export default function TeacherSettingsPage() {
  const { user } = useAuth()
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(false)
  const [error, setError] = useState("")
  const [firstName, setFirstName] = useState(user?.firstName || "")
  const [lastName, setLastName] = useState(user?.lastName || "")
  const [phone, setPhone] = useState("")
  const [notifAttendance, setNotifAttendance] = useState(true)
  const [notifAssignments, setNotifAssignments] = useState(true)
  const [notifMessages, setNotifMessages] = useState(false)
  const [notifResults, setNotifResults] = useState(true)

  const [activeTab, setActiveTab] = useState<"profile" | "qualifications">("profile")
  const [qualifications, setQualifications] = useState<TeacherQualification[]>([])
  const [loadingQuals, setLoadingQuals] = useState(false)
  const [showQualForm, setShowQualForm] = useState(false)
  const [qualForm, setQualForm] = useState({ qualificationName: "", institution: "", yearObtained: "" })
  const [savingQual, setSavingQual] = useState(false)
  const [teacherId, setTeacherId] = useState<string | null>(null)

  useEffect(() => {
    if (activeTab === "qualifications") loadQualifications()
  }, [activeTab])

  async function loadQualifications() {
    try {
      setLoadingQuals(true)
      const profileRes = await teacherFetch<{ content: Array<{ id: string; email: string }> }>("/v1/teachers?page=0&size=50")
      const teacher = profileRes.content?.find((t) => t.email === user?.email)
      if (teacher) {
        setTeacherId(teacher.id)
        const quals = await teacherFetch<TeacherQualification[]>(`/v1/teachers/${teacher.id}/qualifications`)
        setQualifications(quals)
      }
    } catch { /* empty */ }
    finally { setLoadingQuals(false) }
  }

  async function handleSave() {
    setSaving(true)
    setError("")
    try {
      const profileRes = await teacherFetch<{ content: Array<{ id: string; email: string }> }>("/v1/teachers?page=0&size=50")
      const teacher = profileRes.content?.find((t) => t.email === user?.email)
      if (!teacher) {
        setError("Teacher profile not found.")
        return
      }
      await teacherFetch(`/v1/teachers/${teacher.id}`, {
        method: "PUT",
        body: JSON.stringify({ userId: teacher.id, firstName, lastName, phone: phone || undefined }),
      })
      setSaved(true)
      setTimeout(() => setSaved(false), 3000)
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save.")
    } finally {
      setSaving(false)
    }
  }

  async function handleAddQualification() {
    if (!teacherId || !qualForm.qualificationName.trim()) return
    setSavingQual(true)
    try {
      await teacherFetch(`/v1/teachers/${teacherId}/qualifications`, {
        method: "POST",
        body: JSON.stringify({
          qualificationName: qualForm.qualificationName.trim(),
          institutionName: qualForm.institution.trim() || undefined,
          yearObtained: qualForm.yearObtained ? Number(qualForm.yearObtained) : undefined,
        }),
      })
      setQualForm({ qualificationName: "", institution: "", yearObtained: "" })
      setShowQualForm(false)
      loadQualifications()
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to add qualification")
    } finally {
      setSavingQual(false)
    }
  }

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">Settings</h1>
          <p className="mt-1 text-sm text-muted-foreground">Manage your profile and preferences.</p>
        </div>
        {activeTab === "profile" && (
          <button
            onClick={handleSave}
            disabled={saving}
            className="inline-flex items-center gap-2 rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-xs hover:bg-primary/90 disabled:opacity-50"
          >
            {saving ? <Loader2 className="size-4 animate-spin" /> : <Save className="size-4" />}
            {saved ? "Saved!" : "Save Changes"}
          </button>
        )}
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">
          <AlertCircle className="size-4 shrink-0" />
          {error}
        </div>
      )}

      <div className="flex gap-2 border-b border-border">
        <button
          onClick={() => setActiveTab("profile")}
          className={`border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
            activeTab === "profile" ? "border-primary text-primary" : "text-muted-foreground hover:text-foreground"
          }`}
        >
          <Settings className="mr-1.5 inline size-4" />
          Profile
        </button>
        <button
          onClick={() => setActiveTab("qualifications")}
          className={`border-b-2 px-4 py-2.5 text-sm font-medium transition-colors ${
            activeTab === "qualifications" ? "border-primary text-primary" : "text-muted-foreground hover:text-foreground"
          }`}
        >
          <GraduationCap className="mr-1.5 inline size-4" />
          Qualifications ({qualifications.length})
        </button>
      </div>

      {activeTab === "profile" && (
        <div className="space-y-6">
          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
            <h3 className="text-sm font-semibold text-foreground">Profile</h3>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label className="text-xs font-medium text-muted-foreground">First Name</label>
                <input value={firstName} onChange={(e) => setFirstName(e.target.value)} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground">Last Name</label>
                <input value={lastName} onChange={(e) => setLastName(e.target.value)} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground">Email</label>
                <input value={user?.email || ""} disabled className="mt-1 w-full rounded-lg border border-border bg-muted px-3 py-2 text-sm text-muted-foreground" />
              </div>
              <div>
                <label className="text-xs font-medium text-muted-foreground">Phone</label>
                <input value={phone} onChange={(e) => setPhone(e.target.value)} className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none" placeholder="+255..." />
              </div>
            </div>
          </div>

          <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
            <h3 className="text-sm font-semibold text-foreground">Notification Preferences</h3>
            <div className="space-y-3">
              {[
                { label: "Attendance alerts", value: notifAttendance, onChange: setNotifAttendance },
                { label: "Assignment deadlines", value: notifAssignments, onChange: setNotifAssignments },
                { label: "Messages from parents/students", value: notifMessages, onChange: setNotifMessages },
                { label: "Grade publishing", value: notifResults, onChange: setNotifResults },
              ].map((item) => (
                <label key={item.label} className="flex items-center justify-between">
                  <span className="text-sm text-foreground">{item.label}</span>
                  <button
                    type="button"
                    onClick={() => item.onChange(!item.value)}
                    className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors ${
                      item.value ? "bg-primary" : "bg-muted"
                    }`}
                  >
                    <span className={`pointer-events-none inline-block size-5 rounded-full bg-white shadow-xs transition-transform ${
                      item.value ? "translate-x-5" : "translate-x-0"
                    }`} />
                  </button>
                </label>
              ))}
            </div>
          </div>
        </div>
      )}

      {activeTab === "qualifications" && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              {qualifications.length} qualification{qualifications.length !== 1 ? "s" : ""} on file
            </p>
            <button
              onClick={() => setShowQualForm(!showQualForm)}
              className="inline-flex items-center gap-2 rounded-lg bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
            >
              <Plus className="size-4" />
              Add Qualification
            </button>
          </div>

          {showQualForm && (
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs space-y-4">
              <h3 className="text-sm font-semibold text-foreground">New Qualification</h3>
              <div className="grid gap-4 sm:grid-cols-2">
                <div className="sm:col-span-2">
                  <label className="text-xs font-medium text-muted-foreground">Qualification Name *</label>
                  <input
                    value={qualForm.qualificationName}
                    onChange={(e) => setQualForm({ ...qualForm, qualificationName: e.target.value })}
                    placeholder="e.g. B.Ed Mathematics"
                    className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                  />
                </div>
                <div>
                  <label className="text-xs font-medium text-muted-foreground">Institution</label>
                  <input
                    value={qualForm.institution}
                    onChange={(e) => setQualForm({ ...qualForm, institution: e.target.value })}
                    placeholder="University name"
                    className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                  />
                </div>
                <div>
                  <label className="text-xs font-medium text-muted-foreground">Year Obtained</label>
                  <input
                    type="number"
                    value={qualForm.yearObtained}
                    onChange={(e) => setQualForm({ ...qualForm, yearObtained: e.target.value })}
                    placeholder="2020"
                    min={1950}
                    max={2099}
                    className="mt-1 w-full rounded-lg border border-border bg-background px-3 py-2 text-sm text-foreground focus:border-primary focus:outline-none"
                  />
                </div>
              </div>
              <div className="flex justify-end gap-2">
                <button onClick={() => setShowQualForm(false)} className="rounded-lg border border-border px-3 py-2 text-sm font-medium text-foreground hover:bg-muted">
                  Cancel
                </button>
                <button
                  onClick={handleAddQualification}
                  disabled={savingQual || !qualForm.qualificationName.trim()}
                  className="inline-flex items-center gap-2 rounded-lg bg-primary px-3 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 disabled:opacity-50"
                >
                  {savingQual ? <Loader2 className="size-4 animate-spin" /> : <Plus className="size-4" />}
                  Add
                </button>
              </div>
            </div>
          )}

          {loadingQuals ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="size-6 animate-spin text-muted-foreground" />
            </div>
          ) : qualifications.length === 0 ? (
            <div className="rounded-2xl border border-dashed border-border py-12 text-center">
              <Award className="mx-auto mb-3 size-8 text-muted-foreground" />
              <p className="text-sm font-medium text-foreground">No qualifications on file</p>
              <p className="mt-1 text-xs text-muted-foreground">Add your qualifications to keep your profile complete.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {qualifications.map((q) => (
                <div key={q.id} className="flex items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-xs">
                  <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-primary/10">
                    <GraduationCap className="size-5 text-primary" />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-foreground">{q.qualificationName}</p>
                    <div className="flex items-center gap-2 text-xs text-muted-foreground">
                      {q.institution && <span>{q.institution}</span>}
                      {q.institution && q.yearObtained && <span className="text-border">&middot;</span>}
                      {q.yearObtained && <span>{q.yearObtained}</span>}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
