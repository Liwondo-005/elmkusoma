const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || ""

export async function teacherFetch<T>(path: string, options?: RequestInit): Promise<T> {
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
  if (!institutionId) institutionId = "a0000000-0000-0000-0000-000000000001"
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

export interface LearningOffering {
  id: string
  title: string
  description: string | null
  thumbnailUrl: string | null
  educationLevel: string | null
  visibility: string
  status: string
  subjectId: string | null
  subjectName: string | null
  courseId: string | null
  courseTitle: string | null
  ownerId: string
  ownerName: string | null
  teacherId: string | null
  institutionId: string | null
  createdAt: string
}

export interface LearningOfferingInput {
  title: string
  description?: string
  subjectId?: string | null
  courseId?: string | null
  educationLevel?: string | null
  visibility?: string
  status?: string
  independent?: boolean
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

export interface TeacherAnalytics {
  totalStudents: number
  totalAssignments: number
  totalLessons: number
  averageAttendance: number
  pendingGrading: number
  classesCount: number
  upcomingDeadlines: {
    title: string
    dueDate: string
    subjectName: string
    className: string
  }[]
  recentSubmissions: {
    studentName: string
    assignmentTitle: string
    submittedAt: string
    graded: boolean
  }[]
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

export interface PresignedUploadResponse {
  uploadUrl: string
  videoTutorialId: string
  fields?: Record<string, string>
}

export interface VideoTutorialCreate {
  title: string
  description?: string
  visibility: string
  lessonId?: string
  tags?: string[]
  sortOrder?: number
}

export interface VideoTutorialItem {
  id: string
  title: string
  description: string | null
  recordingUrl: string | null
  status: string
  visibility: string
  durationSeconds: number | null
  createdAt: string
}

export interface ResourceItem {
  id: string
  title: string
  description?: string | null
  resourceType: string
  visibility: string
  mimeType?: string | null
  fileSize?: number | null
  storageUrl?: string | null
  externalUrl?: string | null
  lessonId?: string | null
  sortOrder?: number | null
  isDownloadable?: boolean | null
  isPreviewable?: boolean | null
  processingStatus?: string | null
  processingError?: string | null
  pageCount?: number | null
  width?: number | null
  height?: number | null
  durationSeconds?: number | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface ResourcePayload {
  title: string
  description?: string | null
  resourceType: string
  visibility: string
  lessonId?: string | null
  courseId?: string | null
  moduleId?: string | null
  externalUrl?: string | null
  storageUrl?: string | null
  isDownloadable?: boolean
  isPreviewable?: boolean
  tagNames?: string[]
}

function authInstitutionHeaders(): Record<string, string> {
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
  if (!institutionId) institutionId = "a0000000-0000-0000-0000-000000000001"
  return {
    "X-Institution-Id": institutionId,
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  }
}

/**
 * Creates or replaces a resource file through the multipart endpoint: the
 * server reads the real bytes to extract metadata (MIME type, dimensions,
 * duration, pages) and stores them via the media service — the client never
 * reports fabricated metadata.
 */
export function saveResourceWithFile(
  payload: ResourcePayload,
  file: File,
  options?: { resourceId?: string; onProgress?: (progress: number) => void }
): Promise<ResourceItem> {
  return new Promise<ResourceItem>((resolve, reject) => {
    const form = new FormData()
    form.append("request", new Blob([JSON.stringify(payload)], { type: "application/json" }))
    form.append("file", file, file.name)

    const xhr = new XMLHttpRequest()
    const method = options?.resourceId ? "PUT" : "POST"
    const url = `${API_BASE_URL}${options?.resourceId ? `/v1/resources/${options.resourceId}` : "/v1/resources"}`
    xhr.open(method, url)
    const headers = authInstitutionHeaders()
    Object.entries(headers).forEach(([k, v]) => xhr.setRequestHeader(k, v))

    xhr.upload.addEventListener("progress", (event) => {
      if (event.lengthComputable && options?.onProgress) {
        options.onProgress(Math.round((event.loaded / event.total) * 100))
      }
    })
    xhr.addEventListener("load", () => {
      let body: Record<string, unknown> = {}
      try {
        body = JSON.parse(xhr.responseText || "{}")
      } catch {}
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve((body.data ?? body) as ResourceItem)
      } else {
        reject(new Error(
          (typeof body.error === "string" && body.error) ||
            (typeof body.message === "string" && body.message) ||
            `Upload failed: ${xhr.status}`
        ))
      }
    })
    xhr.addEventListener("error", () => reject(new Error("Network error during upload")))
    xhr.addEventListener("abort", () => reject(new Error("Upload cancelled")))
    xhr.send(form)
  })
}

export async function getPresignedUploadUrl(
  fileName: string,
  contentType: string
): Promise<PresignedUploadResponse> {
  return teacherFetch<PresignedUploadResponse>(
    `/v1/video-tutorials/presigned-upload?fileName=${encodeURIComponent(fileName)}&contentType=${encodeURIComponent(contentType)}`
  )
}

export async function uploadFileToPresignedUrl(
  uploadUrl: string,
  file: File,
  onProgress?: (progress: number) => void
): Promise<void> {
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
  if (!institutionId) institutionId = "a0000000-0000-0000-0000-000000000001"

  await new Promise<void>((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    xhr.upload.addEventListener("progress", (event) => {
      if (event.lengthComputable && onProgress) {
        onProgress(Math.round((event.loaded / event.total) * 100))
      }
    })
    xhr.addEventListener("load", () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve()
      } else {
        reject(new Error(`Upload failed: ${xhr.status}`))
      }
    })
    xhr.addEventListener("error", () => reject(new Error("Upload failed")))
    xhr.addEventListener("abort", () => reject(new Error("Upload aborted")))
    xhr.open("PUT", uploadUrl)
    xhr.setRequestHeader("Content-Type", file.type)
    if (token) {
      xhr.setRequestHeader("Authorization", `Bearer ${token}`)
    }
    xhr.setRequestHeader("X-Institution-Id", institutionId)
    xhr.send(file)
  })
}

export async function createVideoTutorial(data: VideoTutorialCreate): Promise<VideoTutorialItem> {
  return teacherFetch<VideoTutorialItem>("/v1/video-tutorials", {
    method: "POST",
    body: JSON.stringify(data),
  })
}

export const teacherApi = {
  getClasses: () => teacherFetch<{ classGroupId: string; className: string; classSection: string; subjectId: string; subjectName: string; academicYear: string; enrolledStudents: number; totalAssignments: number; totalLessons: number }[]>("/v1/teachers/me/classes"),
  getStudents: () => teacherFetch<StudentInClass[]>("/v1/teachers/me/students"),
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

  reorderResources: (items: { id: string; sortOrder: number }[]) =>
    teacherFetch<ResourceItem[]>("/v1/resources/reorder", { method: "PUT", body: JSON.stringify(items) }),

  getOfferings: () => teacherFetch<LearningOffering[]>("/v1/teachers/me/offerings"),
  createOffering: (data: LearningOfferingInput) =>
    teacherFetch<LearningOffering>("/v1/teachers/me/offerings", { method: "POST", body: JSON.stringify(data) }),
  updateOffering: (id: string, data: LearningOfferingInput) =>
    teacherFetch<LearningOffering>(`/v1/teachers/me/offerings/${id}`, { method: "PUT", body: JSON.stringify(data) }),
  deleteOffering: (id: string) =>
    teacherFetch<void>(`/v1/teachers/me/offerings/${id}`, { method: "DELETE" }),

  getPresignedUploadUrl,
  uploadFileToPresignedUrl,
  createVideoTutorial,
}
