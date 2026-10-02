"use client"

import { useTranslations } from "next-intl";
import { useEffect, useState } from "react"
import { Users, Search, UserPlus, Shield, GraduationCap, UserCheck, Loader2, Mail, Phone, CheckCircle, XCircle, ChevronDown } from "lucide-react"
import { adminApi, academicApi, getInstitutionId, type PeopleMemberResponse, type InvitationResponse, type Subject, type ClassGroup, type AcademicYear } from "@/lib/api"
import { teacherApi, type TeacherAssignment } from "@/lib/teacher-api"

const roleColors: Record<string, string> = {
  OWNER: "bg-amber-100 text-amber-800 border-amber-200",
  ADMIN: "bg-purple-100 text-purple-800 border-purple-200",
  TEACHER: "bg-blue-100 text-blue-800 border-blue-200",
  STUDENT: "bg-emerald-100 text-emerald-800 border-emerald-200",
  PARENT: "bg-teal-100 text-teal-800 border-teal-200",
}

const roleIcons: Record<string, typeof Users> = {
  OWNER: Shield,
  ADMIN: Shield,
  TEACHER: GraduationCap,
  STUDENT: Users,
  PARENT: UserCheck,
}

export default function PeopleManagementPage() {
  const t = useTranslations("admin");
  const tc = useTranslations("common");
  const ts = useTranslations("status");
  const [people, setPeople] = useState<PeopleMemberResponse[]>([])
  const [invitations, setInvitations] = useState<InvitationResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [search, setSearch] = useState("")
  const [roleFilter, setRoleFilter] = useState<string>("ALL")
  const [showInviteModal, setShowInviteModal] = useState(false)
  const [inviteEmail, setInviteEmail] = useState("")
  const [inviteRole, setInviteRole] = useState("STUDENT")
  const [inviteLoading, setInviteLoading] = useState(false)
  const [activeTab, setActiveTab] = useState<"members" | "invitations">("members")

  // ── Teacher assignment panel (authorised admin assignment management) ──
  const [assignMember, setAssignMember] = useState<PeopleMemberResponse | null>(null)
  const [teacherId, setTeacherId] = useState<string | null>(null)
  const [assignments, setAssignments] = useState<TeacherAssignment[]>([])
  const [classGroups, setClassGroups] = useState<ClassGroup[]>([])
  const [subjects, setSubjects] = useState<Subject[]>([])
  const [years, setYears] = useState<AcademicYear[]>([])
  const [assignLoading, setAssignLoading] = useState(false)
  const [assignError, setAssignError] = useState<string | null>(null)
  const [assignNotice, setAssignNotice] = useState<string | null>(null)
  const [formClass, setFormClass] = useState("")
  const [formSubject, setFormSubject] = useState("")
  const [formYear, setFormYear] = useState("")
  const [creating, setCreating] = useState(false)
  const [endingId, setEndingId] = useState<string | null>(null)
  const [confirmEndId, setConfirmEndId] = useState<string | null>(null)

  useEffect(() => { loadData() }, [])

  async function loadData() {
    const institutionId = getInstitutionId()
    if (!institutionId) { setError(t("people.noInstitutionContextFound")); setLoading(false); return }
    try {
      const [peopleData, invitationsData] = await Promise.all([
        adminApi.listPeople(institutionId),
        adminApi.listInvitations(institutionId),
      ])
      setPeople(peopleData)
      setInvitations(invitationsData)
    } catch (err) {
      setError(err instanceof Error ? err.message : tc("error.load"))
    } finally { setLoading(false) }
  }

  async function handleInvite() {
    const institutionId = getInstitutionId()
    if (!institutionId || !inviteEmail) return
    setInviteLoading(true)
    try {
      await adminApi.inviteUser(institutionId, { email: inviteEmail, role: inviteRole })
      setShowInviteModal(false); setInviteEmail(""); setInviteRole("STUDENT"); loadData()
    } catch (err) { setError(err instanceof Error ? err.message : t("people.failedToSendInvitation")) }
    finally { setInviteLoading(false) }
  }

  async function handleToggleActive(member: PeopleMemberResponse) {
    const institutionId = getInstitutionId()
    if (!institutionId) return
    try {
      if (member.isActive) await adminApi.deactivateMember(institutionId, member.userId)
      else await adminApi.activateMember(institutionId, member.userId)
      loadData()
    } catch (err) { setError(err instanceof Error ? err.message : t("people.failedToUpdateMember")) }
  }

  async function handleRoleChange(member: PeopleMemberResponse, newRole: string) {
    const institutionId = getInstitutionId()
    if (!institutionId) return
    try { await adminApi.updateMemberRole(institutionId, member.userId, newRole); loadData() }
    catch (err) { setError(err instanceof Error ? err.message : t("people.failedToUpdateRole")) }
  }

  async function handleCancelInvitation(inv: InvitationResponse) {
    const institutionId = getInstitutionId()
    if (!institutionId) return
    try { await adminApi.cancelInvitation(institutionId, inv.id); loadData() }
    catch (err) { setError(err instanceof Error ? err.message : t("people.failedToCancelInvitation")) }
  }

  // ── Teacher assignment management (admin) ──
  async function resolveTeacherId(userId: string): Promise<string | null> {
    for (let page = 0; page < 10; page++) {
      const res = await teacherApi.listTeachers(page, 50)
      const match = (res.content ?? []).find((tch) => tch.userId === userId)
      if (match) return match.id
      if (!res.totalElements || (page + 1) * 50 >= res.totalElements) return null
    }
    return null
  }

  async function loadAssignments(tid: string) {
    setAssignments(await teacherApi.getAssignments(tid))
  }

  async function openAssignments(member: PeopleMemberResponse) {
    const institutionId = getInstitutionId()
    if (!institutionId) return
    setAssignMember(member)
    setTeacherId(null)
    setAssignments([])
    setAssignLoading(true)
    setAssignError(null)
    setAssignNotice(null)
    setFormClass("")
    setFormSubject("")
    setFormYear("")
    setConfirmEndId(null)
    try {
      const tid = await resolveTeacherId(member.userId)
      if (!tid) { setAssignError(t("teachers.profileNotFound")); return }
      setTeacherId(tid)
      const [assignmentData, classData, subjectData, yearData] = await Promise.all([
        teacherApi.getAssignments(tid),
        academicApi.getClassGroups(institutionId),
        academicApi.getSubjects(institutionId),
        academicApi.getAcademicYears(institutionId),
      ])
      setAssignments(assignmentData)
      setClassGroups(classData)
      setSubjects(subjectData)
      setYears(yearData)
      if (yearData.length > 0) {
        setFormYear(yearData.find((y) => y.isCurrent)?.yearLabel ?? yearData[0].yearLabel)
      }
    } catch (err) {
      setAssignError(err instanceof Error ? err.message : t("teachers.loadFailed"))
    } finally {
      setAssignLoading(false)
    }
  }

  async function handleCreateAssignment() {
    if (!teacherId || !formClass || !formSubject || !formYear.trim()) return
    setCreating(true)
    setAssignError(null)
    setAssignNotice(null)
    try {
      // Server re-validates every id against the caller's institution and level.
      await teacherApi.addAssignment(teacherId, {
        classGroupId: formClass,
        subjectId: formSubject,
        academicYear: formYear.trim(),
      })
      await loadAssignments(teacherId)
      setFormClass("")
      setFormSubject("")
      setAssignNotice(t("teachers.created"))
    } catch (err) {
      setAssignError(err instanceof Error ? err.message : t("teachers.loadFailed"))
    } finally {
      setCreating(false)
    }
  }

  async function handleEndAssignment(assignment: TeacherAssignment) {
    if (!teacherId) return
    setEndingId(assignment.id)
    setAssignError(null)
    setAssignNotice(null)
    try {
      await teacherApi.endAssignment(assignment.id)
      await loadAssignments(teacherId)
      setAssignNotice(t("teachers.endedSuccess"))
    } catch (err) {
      setAssignError(err instanceof Error ? err.message : t("teachers.loadFailed"))
    } finally {
      setEndingId(null)
      setConfirmEndId(null)
    }
  }

  const activeAssignments = assignments.filter((a) => a.status !== "ENDED" && a.status !== "CANCELLED")
  const endedAssignments = assignments.filter((a) => a.status === "ENDED" || a.status === "CANCELLED")

  const filteredPeople = people.filter((p) => {
    const matchesSearch = !search || p.fullName.toLowerCase().includes(search.toLowerCase()) || p.email.toLowerCase().includes(search.toLowerCase())
    const matchesRole = roleFilter === "ALL" || p.membershipRole === roleFilter
    return matchesSearch && matchesRole
  })

  const roleCounts = people.reduce((acc, p) => { acc[p.membershipRole] = (acc[p.membershipRole] || 0) + 1; return acc }, {} as Record<string, number>)

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-foreground">{t("people.peopleManagement")}</h1>
          <p className="mt-1 text-sm text-muted-foreground">{t("people.manageUsersRolesAnd")}</p>
        </div>
        <button onClick={() => setShowInviteModal(true)} className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground shadow-xs hover:bg-primary/90">
          <UserPlus className="size-4" /> {t("people.inviteUser")}</button>
      </div>

      <div className="flex gap-1 rounded-xl border border-border bg-muted p-1">
        <button onClick={() => setActiveTab("members")} className={`flex-1 rounded-lg px-4 py-2 text-sm font-medium transition-colors ${activeTab === "members" ? "bg-card text-foreground shadow-xs" : "text-muted-foreground hover:text-foreground"}`}>
          {t("people.members", { p0: people.length })}</button>
        <button onClick={() => setActiveTab("invitations")} className={`flex-1 rounded-lg px-4 py-2 text-sm font-medium transition-colors ${activeTab === "invitations" ? "bg-card text-foreground shadow-xs" : "text-muted-foreground hover:text-foreground"}`}>
          {t("people.pendingInvitations")}{invitations.filter((i) => i.status === "PENDING").length})
        </button>
      </div>

      {loading && <div className="flex items-center justify-center py-20"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>}
      {error && <div className="rounded-2xl border border-destructive/20 bg-destructive/5 px-6 py-4"><p className="text-sm font-medium text-destructive">{error}</p></div>}

      {!loading && activeTab === "members" && (
        <>
          <div className="grid gap-3 sm:grid-cols-5">
            {Object.entries(roleCounts).map(([role, count]) => {
              const Icon = roleIcons[role] || Users
              return (
                <button key={role} onClick={() => setRoleFilter(roleFilter === role ? "ALL" : role)} className={`flex items-center gap-3 rounded-xl border p-4 transition-all ${roleFilter === role ? "border-primary bg-primary/5 shadow-xs" : "border-border bg-card hover:border-primary/30"}`}>
                  <div className={`flex size-10 items-center justify-center rounded-lg ${roleColors[role] || "bg-gray-100"}`}><Icon className="size-5" /></div>
                  <div className="text-left"><p className="text-lg font-bold text-foreground">{count}</p><p className="text-xs text-muted-foreground">{role}</p></div>
                </button>
              )
            })}
          </div>

          <div className="relative">
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <input type="text" placeholder={t("people.searchByNameOr")} value={search} onChange={(e) => setSearch(e.target.value)} className="w-full rounded-xl border border-border bg-card py-2.5 pl-10 pr-4 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
          </div>

          <div className="rounded-2xl border border-border bg-card overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead><tr className="border-b border-border bg-muted/50">
                  <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("people.user")}</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("people.role")}</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("people.contact")}</th>
                  <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("people.status")}</th>
                  <th className="px-4 py-3 text-right text-xs font-semibold text-muted-foreground uppercase">{t("people.actions")}</th>
                </tr></thead>
                <tbody className="divide-y divide-border">
                  {filteredPeople.map((member) => (
                    <tr key={member.userId} className="hover:bg-muted/30 transition-colors">
                      <td className="px-4 py-3"><div className="flex items-center gap-3">
                        <div className="flex size-9 items-center justify-center rounded-full bg-primary/10 text-sm font-bold text-primary">{member.firstName?.[0]}{member.lastName?.[0]}</div>
                        <div><p className="text-sm font-medium text-foreground">{member.fullName}</p><p className="text-xs text-muted-foreground">{member.email}</p></div>
                      </div></td>
                      <td className="px-4 py-3"><div className="relative">
                        <select value={member.membershipRole} onChange={(e) => handleRoleChange(member, e.target.value)} className={`appearance-none rounded-lg border px-3 py-1.5 pr-8 text-xs font-medium cursor-pointer focus:outline-none focus:ring-1 focus:ring-primary ${roleColors[member.membershipRole] || "bg-gray-100"}`}>
                          <option value="OWNER">OWNER</option><option value="ADMIN">ADMIN</option><option value="TEACHER">TEACHER</option><option value="STUDENT">STUDENT</option><option value="PARENT">PARENT</option>
                        </select>
                        <ChevronDown className="pointer-events-none absolute right-2 top-1/2 size-3 -translate-y-1/2 opacity-50" />
                      </div></td>
                      <td className="px-4 py-3"><div className="flex items-center gap-2 text-xs text-muted-foreground">
                        {member.phone && <span className="flex items-center gap-1"><Phone className="size-3" />{member.phone}</span>}
                      </div></td>
                      <td className="px-4 py-3"><span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium ${member.isActive ? "bg-emerald-50 text-emerald-700 border border-emerald-200" : "bg-red-50 text-red-700 border border-red-200"}`}>
                        {member.isActive ? <CheckCircle className="size-3" /> : <XCircle className="size-3" />}{member.isActive ? ts("active") : ts("inactive")}
                      </span></td>
                      <td className="px-4 py-3 text-right"><div className="flex items-center justify-end gap-1">
                        {member.membershipRole === "TEACHER" && (
                          <button onClick={() => openAssignments(member)} className="inline-flex items-center gap-1 rounded-lg px-3 py-1.5 text-xs font-medium text-blue-600 transition-colors hover:bg-blue-50">
                            {t("teachers.assignments")}
                          </button>
                        )}
                        <button onClick={() => handleToggleActive(member)} className={`inline-flex items-center gap-1 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors ${member.isActive ? "text-red-600 hover:bg-red-50" : "text-emerald-600 hover:bg-emerald-50"}`}>
                          {member.isActive ? t("people.deactivate") : t("people.activate")}
                        </button>
                      </div></td>
                    </tr>
                  ))}
                  {filteredPeople.length === 0 && <tr><td colSpan={5} className="px-4 py-10 text-center text-sm text-muted-foreground">{t("people.noMembersFound")}</td></tr>}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {!loading && activeTab === "invitations" && (
        <div className="rounded-2xl border border-border bg-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead><tr className="border-b border-border bg-muted/50">
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("people.email")}</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("people.role2")}</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("people.status2")}</th>
                <th className="px-4 py-3 text-left text-xs font-semibold text-muted-foreground uppercase">{t("people.expires")}</th>
                <th className="px-4 py-3 text-right text-xs font-semibold text-muted-foreground uppercase">{t("people.actions2")}</th>
              </tr></thead>
              <tbody className="divide-y divide-border">
                {invitations.map((inv) => (
                  <tr key={inv.id} className="hover:bg-muted/30 transition-colors">
                    <td className="px-4 py-3"><div className="flex items-center gap-2"><Mail className="size-4 text-muted-foreground" /><span className="text-sm font-medium text-foreground">{inv.email}</span></div></td>
                    <td className="px-4 py-3"><span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium border ${roleColors[inv.role] || "bg-gray-100"}`}>{inv.role}</span></td>
                    <td className="px-4 py-3"><span className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium ${inv.status === "PENDING" ? "bg-yellow-50 text-yellow-700 border border-yellow-200" : inv.status === "ACCEPTED" ? "bg-emerald-50 text-emerald-700 border border-emerald-200" : "bg-red-50 text-red-700 border border-red-200"}`}>{inv.status}</span></td>
                    <td className="px-4 py-3"><span className="text-xs text-muted-foreground">{new Date(inv.expiresAt).toLocaleDateString()}</span></td>
                    <td className="px-4 py-3 text-right">{inv.status === "PENDING" && <button onClick={() => handleCancelInvitation(inv)} className="text-xs font-medium text-red-600 hover:text-red-700">{tc("cancel")}</button>}</td>
                  </tr>
                ))}
                {invitations.length === 0 && <tr><td colSpan={5} className="px-4 py-10 text-center text-sm text-muted-foreground">{t("people.noInvitationsYet")}</td></tr>}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {showInviteModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="mx-4 w-full max-w-md rounded-2xl border border-border bg-card p-6 shadow-xl">
            <h3 className="text-lg font-bold text-foreground">{t("people.inviteUser2")}</h3>
            <p className="mt-1 text-sm text-muted-foreground">{t("people.sendAnInvitationTo")}</p>
            <div className="mt-4 space-y-4">
              <div><label className="block text-sm font-medium text-foreground mb-1">{t("people.email2")}</label>
                <input type="email" value={inviteEmail} onChange={(e) => setInviteEmail(e.target.value)} placeholder={t("people.userExampleCom")} className="w-full rounded-xl border border-border bg-card px-4 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary" /></div>
              <div><label className="block text-sm font-medium text-foreground mb-1">{t("people.role3")}</label>
                <select value={inviteRole} onChange={(e) => setInviteRole(e.target.value)} className="w-full rounded-xl border border-border bg-card px-4 py-2.5 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary">
                  <option value="STUDENT">{t("people.student")}</option><option value="TEACHER">{t("people.teacher")}</option><option value="PARENT">{t("people.parent")}</option><option value="ADMIN">{t("people.admin")}</option>
                </select></div>
            </div>
            <div className="mt-6 flex gap-3">
              <button onClick={() => setShowInviteModal(false)} className="flex-1 rounded-xl border border-border px-4 py-2.5 text-sm font-medium text-muted-foreground hover:bg-muted">{tc("cancel")}</button>
              <button onClick={handleInvite} disabled={!inviteEmail || inviteLoading} className="flex-1 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground shadow-xs hover:bg-primary/90 disabled:opacity-50">{inviteLoading ? t("people.sending") : t("people.sendInvitation")}</button>
            </div>
          </div>
        </div>
      )}

      {assignMember && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
          <div className="mx-4 w-full max-w-2xl rounded-2xl border border-border bg-card p-6 shadow-xl">
            <div className="flex items-start justify-between gap-4">
              <div>
                <h3 className="text-lg font-bold text-foreground">{t("teachers.assignments")}</h3>
                <p className="mt-1 text-sm text-muted-foreground">{t("teachers.assignmentsFor", { name: assignMember.fullName })}</p>
              </div>
              <span className="flex size-9 shrink-0 items-center justify-center rounded-full bg-primary/10 text-primary"><GraduationCap className="size-5" /></span>
            </div>

            {assignLoading && (
              <div className="flex items-center justify-center py-12"><Loader2 className="size-6 animate-spin text-muted-foreground" /></div>
            )}

            {!assignLoading && assignError && (
              <div className="mt-4 rounded-xl border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive">{assignError}</div>
            )}

            {!assignLoading && !assignError && (
              <div className="mt-4 max-h-[65vh] space-y-5 overflow-y-auto pr-1">
                {assignNotice && (
                  <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{assignNotice}</div>
                )}

                {/* Create assignment: ids are re-validated server-side against
                    the caller's institution and education level. */}
                <div className="space-y-3 rounded-xl border border-border bg-muted/30 p-4">
                  <h4 className="text-sm font-semibold text-foreground">{t("teachers.createTitle")}</h4>
                  <div className="grid gap-3 sm:grid-cols-3">
                    <div>
                      <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("teachers.selectClass")}</label>
                      <select value={formClass} onChange={(e) => setFormClass(e.target.value)} className="w-full rounded-xl border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary">
                        <option value="">{t("teachers.selectClass")}</option>
                        {classGroups.map((cg) => <option key={cg.id} value={cg.id}>{cg.name}</option>)}
                      </select>
                      {classGroups.length === 0 && <p className="mt-1 text-xs text-muted-foreground">{t("teachers.noClasses")}</p>}
                    </div>
                    <div>
                      <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("teachers.selectSubject")}</label>
                      <select value={formSubject} onChange={(e) => setFormSubject(e.target.value)} className="w-full rounded-xl border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary">
                        <option value="">{t("teachers.selectSubject")}</option>
                        {subjects.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
                      </select>
                      {subjects.length === 0 && <p className="mt-1 text-xs text-muted-foreground">{t("teachers.noSubjects")}</p>}
                    </div>
                    <div>
                      <label className="mb-1 block text-xs font-medium text-muted-foreground">{t("teachers.selectYear")}</label>
                      {years.length > 0 ? (
                        <select value={formYear} onChange={(e) => setFormYear(e.target.value)} className="w-full rounded-xl border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary">
                          {years.map((y) => <option key={y.id} value={y.yearLabel}>{y.yearLabel}</option>)}
                        </select>
                      ) : (
                        <input value={formYear} onChange={(e) => setFormYear(e.target.value)} placeholder={t("teachers.noYears")} className="w-full rounded-xl border border-border bg-card px-3 py-2 text-sm outline-none focus:border-primary focus:ring-1 focus:ring-primary" />
                      )}
                    </div>
                  </div>
                  <button onClick={handleCreateAssignment} disabled={!formClass || !formSubject || !formYear.trim() || creating} className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2.5 text-sm font-medium text-primary-foreground shadow-xs hover:bg-primary/90 disabled:opacity-50">
                    {creating && <Loader2 className="size-4 animate-spin" />} {creating ? t("teachers.creating") : t("teachers.create")}
                  </button>
                </div>

                <div>
                  <h4 className="mb-2 text-sm font-semibold text-foreground">{ts("active")}</h4>
                  {activeAssignments.length === 0 ? (
                    <p className="rounded-xl border border-border bg-muted/30 px-4 py-3 text-sm text-muted-foreground">{t("teachers.noAssignments")}</p>
                  ) : (
                    <ul className="space-y-2">
                      {activeAssignments.map((a) => (
                        <li key={a.id} className="flex items-center justify-between gap-3 rounded-xl border border-border bg-card px-4 py-3">
                          <div className="min-w-0">
                            <p className="truncate text-sm font-medium text-foreground">{a.classGroupName || a.classGroupId} · {a.subjectName || a.subjectId}</p>
                            <p className="text-xs text-muted-foreground">{a.academicYear}{a.startDate ? ` · ${new Date(a.startDate).toLocaleDateString()}` : ""}</p>
                          </div>
                          <div className="flex shrink-0 items-center gap-2">
                            <span className="rounded-full border border-emerald-200 bg-emerald-50 px-2.5 py-1 text-xs font-medium text-emerald-700">{ts("active")}</span>
                            <button
                              onClick={() => { if (confirmEndId === a.id) handleEndAssignment(a); else setConfirmEndId(a.id) }}
                              disabled={endingId === a.id}
                              className={`inline-flex items-center gap-1 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors disabled:opacity-50 ${confirmEndId === a.id ? "bg-red-600 text-white hover:bg-red-700" : "text-red-600 hover:bg-red-50"}`}
                            >
                              {endingId === a.id ? <Loader2 className="size-3 animate-spin" /> : confirmEndId === a.id ? t("teachers.confirmEnd") : t("teachers.end")}
                            </button>
                          </div>
                        </li>
                      ))}
                    </ul>
                  )}
                </div>

                {endedAssignments.length > 0 && (
                  <div>
                    <h4 className="mb-2 text-sm font-semibold text-foreground">{t("teachers.ended")}</h4>
                    <ul className="space-y-2">
                      {endedAssignments.map((a) => (
                        <li key={a.id} className="flex items-center justify-between gap-3 rounded-xl border border-border bg-muted/20 px-4 py-3 opacity-80">
                          <div className="min-w-0">
                            <p className="truncate text-sm font-medium text-foreground">{a.classGroupName || a.classGroupId} · {a.subjectName || a.subjectId}</p>
                            <p className="text-xs text-muted-foreground">{a.academicYear}{a.endDate ? ` · ${new Date(a.endDate).toLocaleDateString()}` : ""}</p>
                          </div>
                          <span className="shrink-0 rounded-full border border-border bg-muted px-2.5 py-1 text-xs font-medium text-muted-foreground">{t("teachers.ended")}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}

            <div className="mt-6">
              <button onClick={() => { setAssignMember(null); setAssignError(null); setAssignNotice(null) }} className="w-full rounded-xl border border-border px-4 py-2.5 text-sm font-medium text-muted-foreground hover:bg-muted">{tc("cancel")}</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
