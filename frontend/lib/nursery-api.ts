const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || ""

async function nurseryFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
  let institutionId = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_institution_id") : null
  if (!institutionId && typeof window !== "undefined") {
    try {
      const raw = localStorage.getItem("elmkusoma_current_user")
      if (raw) {
        const user = JSON.parse(raw)
        if (user?.institutionId) institutionId = user.institutionId
      }
    } catch {}
  }
  if (!institutionId) institutionId = "00000000-0000-0000-0000-000000000001"
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      "X-Institution-Id": institutionId,
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options?.headers,
    },
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error || body.message || `Request failed: ${res.status}`)
  }
  const json = await res.json()
  return json.data ?? json
}

export interface NurseryActivity {
  id: string
  classGroupId: string
  institutionId: string
  activityName: string
  activityType: "GAME" | "SONG" | "STORY" | "CRAFT" | "PHYSICAL" | "EDUCATIONAL"
  description: string | null
  instructions: string | null
  durationMinutes: number | null
  maxParticipants: number | null
  materialsNeeded: string | null
  learningObjectives: string | null
  ageGroup: string | null
  activityDate: string
  status: "PLANNED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED"
  conductedBy: string | null
}

export interface NurseryMilestone {
  id: string
  studentId: string
  institutionId: string
  category: "PHYSICAL" | "COGNITIVE" | "SOCIAL" | "EMOTIONAL" | "LANGUAGE" | "MOTOR"
  milestoneName: string
  description: string | null
  expectedAgeMonths: number | null
  achievedDate: string | null
  status: "PENDING" | "ACHIEVED" | "IN_PROGRESS" | "NOT_OBSERVED"
  observedBy: string | null
  evidenceNotes: string | null
}

export const nurseryApi = {
  getActivities: (classId: string) =>
    nurseryFetch<NurseryActivity[]>(`/v1/nursery/activities?classId=${classId}`),

  getActivitiesByType: (type: string) =>
    nurseryFetch<NurseryActivity[]>(`/v1/nursery/activities/type/${type}`),

  getActivitiesByClassAndDate: (classId: string, date: string) =>
    nurseryFetch<NurseryActivity[]>(`/v1/nursery/activities/class/${classId}/date/${date}`),

  getActivity: (id: string) =>
    nurseryFetch<NurseryActivity>(`/v1/nursery/activities/${id}`),

  getMilestones: (studentId: string) =>
    nurseryFetch<NurseryMilestone[]>(`/v1/nursery/milestones/student/${studentId}`),

  getMilestonesByCategory: (studentId: string, category: string) =>
    nurseryFetch<NurseryMilestone[]>(`/v1/nursery/milestones/student/${studentId}/category/${category}`),

  getMilestone: (id: string) =>
    nurseryFetch<NurseryMilestone>(`/v1/nursery/milestones/${id}`),
}
