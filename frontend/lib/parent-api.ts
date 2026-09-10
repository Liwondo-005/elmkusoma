const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || ""

async function parentFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
  const res = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
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

export interface FamilyOverview {
  parentName: string
  totalChildren: number
  children: ChildSummary[]
  todayActions: ActionItem[]
}

export interface ChildSummary {
  studentId: string
  name: string
  admissionNumber: string
  className: string
  educationLevel: string
  relationshipType: string
  attendancePercentage: number | null
  latestGrade: string | null
  isPrimary: boolean
}

export interface ActionItem {
  type: string
  title: string
  detail: string
  childName: string
  date: string | null
  priority: string
}

export interface ChildOverview {
  studentId: string
  studentName: string
  admissionNumber: string
  relationshipType: string
  isPrimary: boolean
  className: string
  educationLevel: string
  totalDays: number
  daysPresent: number
  daysAbsent: number
  daysLate: number
  attendancePercentage: number | null
  totalAssignments: number
  completedAssignments: number
  overdueAssignments: number
  pendingAssignments: number
  latestGrade: string | null
  latestAverage: number | null
  classRank: number | null
  totalStudentsInClass: number | null
  learningProgress: number | null
}

export interface AttendanceData {
  studentName: string
  className: string
  attendancePercentage: number
  totalDays: number
  presentDays: number
  absentDays: number
  lateDays: number
  excusedDays: number
  recentDays: AttendanceDay[]
}

export interface AttendanceDay {
  date: string
  status: string
  remarks: string | null
}

export interface AssignmentData {
  studentName: string
  className: string
  pending: AssignmentItem[]
  completed: AssignmentItem[]
  overdue: AssignmentItem[]
}

export interface AssignmentItem {
  id: string
  title: string
  subject: string
  dueDate: string | null
  totalMarks: number
  obtainedMarks: number | null
  status: string
  remarks: string | null
}

export interface ResultData {
  studentName: string
  className: string
  reportCards: ReportCardItem[]
}

export interface ReportCardItem {
  id: string
  term: string
  academicYear: string
  overallGrade: string | null
  averageMark: number | null
  gpa: number | null
  classRank: number | null
  totalStudentsInClass: number | null
  remarks: string | null
  status: string
  publishedAt: string | null
}

export const parentApi = {
  getOverview: () => parentFetch<FamilyOverview>("/v1/my/overview"),
  getChildren: () => parentFetch<ChildOverview[]>("/v1/my/children"),
  getChild: (id: string) => parentFetch<ChildOverview>(`/v1/my/children/${id}`),
  getChildAttendance: (id: string) => parentFetch<AttendanceData>(`/v1/my/children/${id}/attendance`),
  getChildAssignments: (id: string) => parentFetch<AssignmentData>(`/v1/my/children/${id}/assignments`),
  getChildResults: (id: string) => parentFetch<ResultData>(`/v1/my/children/${id}/results`),
}
