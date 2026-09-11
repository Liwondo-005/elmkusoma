const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || ""

async function teacherFetch<T>(path: string, options?: RequestInit): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_access_token") : null
  const institutionId = typeof window !== "undefined" ? localStorage.getItem("elmkusoma_institution_id") || "00000000-0000-0000-0000-000000000001" : "00000000-0000-0000-0000-000000000001"
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

export interface TeacherProfile {
  id: string
  userId: string
  firstName: string
  lastName: string
  email: string
  phone: string | null
  department: string | null
  qualification: string | null
  employeeNumber: string | null
  joiningDate: string | null
  isActive: boolean
}

export interface TeacherAssignment {
  id: string
  classGroupId: string
  classGroupName: string
  subjectId: string
  subjectName: string
  academicYearId: string | null
  academicYearName: string | null
}

export interface TeacherQualification {
  id: string
  qualificationName: string
  institution: string | null
  yearObtained: number | null
  certificateUrl: string | null
}

export interface ClassGroupInfo {
  id: string
  name: string
  gradeName: string | null
  studentCount: number
  educationLevel: string | null
}

export interface StudentInClass {
  id: string
  firstName: string
  lastName: string
  admissionNumber: string
  email: string | null
}

export interface AttendanceRecord {
  id: string
  studentId: string
  studentName: string
  date: string
  status: string
  remarks: string | null
}

export interface AttendanceSummary {
  studentId: string
  studentName: string
  admissionNumber: string
  totalDays: number
  presentDays: number
  absentDays: number
  lateDays: number
  excusedDays: number
  attendancePercentage: number
}

export interface AssignmentInfo {
  id: string
  title: string
  description: string | null
  subjectId: string | null
  subjectName: string | null
  classGroupId: string
  classGroupName: string
  dueDate: string | null
  totalMarks: number
  status: string
  createdAt: string
  submissionCount: number
  totalStudents: number
}

export interface AssignmentSubmission {
  id: string
  studentId: string
  studentName: string
  admissionNumber: string
  submittedAt: string | null
  obtainedMarks: number | null
  grade: string | null
  feedback: string | null
  status: string
}

export interface GradingScale {
  id: string
  name: string
  description: string | null
  minMark: number
  maxMark: number
  gradeBoundaries: GradeBoundary[]
}

export interface GradeBoundary {
  id: string
  grade: string
  minMark: number
  maxMark: number
  gpaPoints: number | null
  remarks: string | null
}

export interface ReportCard {
  id: string
  studentId: string
  studentName: string
  admissionNumber: string
  className: string
  term: string
  academicYear: string
  overallGrade: string | null
  averageMark: number | null
  classRank: number | null
  totalStudentsInClass: number | null
  remarks: string | null
  status: string
  publishedAt: string | null
}

export interface TeacherDashboardStats {
  totalStudents: number
  totalClasses: number
  totalSubjects: number
  pendingGrading: number
  todayAttendance: number
  attendanceRate: number
  upcomingAssignments: number
  averageClassPerformance: number
}

export interface TeacherScheduleItem {
  id: string
  className: string
  subject: string
  dayOfWeek: string
  startTime: string
  endTime: string
  room: string | null
}

export const teacherApi = {
  getProfile: (id: string) => teacherFetch<TeacherProfile>(`/v1/teachers/${id}`),
  listTeachers: (page = 0, size = 50) => teacherFetch<{ content: TeacherProfile[]; totalElements: number }>(`/v1/teachers?page=${page}&size=${size}`),

  getAssignments: (id: string) => teacherFetch<TeacherAssignment[]>(`/v1/teachers/${id}/assignments`),
  addAssignment: (id: string, data: { classGroupId: string; subjectId: string; academicYearId?: string }) =>
    teacherFetch<TeacherAssignment>(`/v1/teachers/${id}/assignments`, { method: "POST", body: JSON.stringify(data) }),
  removeAssignment: (assignmentId: string) =>
    teacherFetch<void>(`/v1/teachers/assignments/${assignmentId}`, { method: "DELETE" }),

  getQualifications: (id: string) => teacherFetch<TeacherQualification[]>(`/v1/teachers/${id}/qualifications`),
  addQualification: (id: string, data: { qualificationName: string; institution?: string; yearObtained?: number }) =>
    teacherFetch<TeacherQualification>(`/v1/teachers/${id}/qualifications`, { method: "POST", body: JSON.stringify(data) }),

  getClassGroups: () => teacherFetch<ClassGroupInfo[]>(`/v1/academic/class-groups`),
  getStudentsByClass: (classId: string) => teacherFetch<StudentInClass[]>(`/v1/students?classId=${classId}`),

  getAttendanceByClass: (classId: string, date: string) =>
    teacherFetch<AttendanceRecord[]>(`/v1/attendance?classId=${classId}&date=${date}`),
  markAttendance: (data: { studentId: string; classGroupId: string; date: string; status: string; remarks?: string }) =>
    teacherFetch<AttendanceRecord>("/v1/attendance/mark", { method: "POST", body: JSON.stringify(data) }),
  bulkMarkAttendance: (data: { classGroupId: string; date: string; records: { studentId: string; status: string; remarks?: string }[] }) =>
    teacherFetch<void>("/v1/attendance/bulk", { method: "POST", body: JSON.stringify(data) }),
  getAttendanceSummary: (classId: string) =>
    teacherFetch<AttendanceSummary[]>(`/v1/attendance/summary/class/${classId}`),

  getAssignmentsByClass: (classId: string) =>
    teacherFetch<AssignmentInfo[]>(`/v1/learning/assignments/class/${classId}`),
  createAssignment: (data: { title: string; description?: string; subjectId?: string; classGroupId: string; dueDate?: string; totalMarks: number }) =>
    teacherFetch<AssignmentInfo>("/v1/learning/assignments", { method: "POST", body: JSON.stringify(data) }),
  getSubmissions: (assignmentId: string) =>
    teacherFetch<AssignmentSubmission[]>(`/v1/learning/assignments/${assignmentId}/submissions`),
  gradeSubmission: (submissionId: string, grade: number, feedback?: string) =>
    teacherFetch<void>(`/v1/learning/submissions/${submissionId}/grade?grade=${grade}${feedback ? `&feedback=${encodeURIComponent(feedback)}` : ""}`, { method: "PUT" }),

  getGradingScales: () => teacherFetch<GradingScale[]>("/v1/grading/scales"),
  getReportCardsByTerm: (termId: string) =>
    teacherFetch<ReportCard[]>(`/v1/grading/report-cards/term/${termId}`),
  getReportCardsByStudent: (studentId: string) =>
    teacherFetch<ReportCard[]>(`/v1/grading/report-cards/student/${studentId}`),
}
