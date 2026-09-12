const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || ""

async function learnerFetch<T>(path: string, options?: RequestInit): Promise<T> {
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

export interface LearnerProfile {
  id: string
  userId: string
  interests: string | null
  bio: string | null
  avatarUrl: string | null
  learningGoal: string | null
}

export interface CourseSummary {
  id: string
  title: string
  description: string | null
  thumbnailUrl: string | null
  level: string
  category: string | null
  isPublished: boolean
  isFeatured: boolean
  institutionId: string
  subjectId: string | null
  createdAt: string
}

export interface CourseDetail extends CourseSummary {
  modules: CourseModuleSummary[]
}

export interface CourseModuleSummary {
  id: string
  title: string
  description: string | null
  sortOrder: number
  lessonCount: number
}

export interface CourseLesson {
  id: string
  title: string
  content: string | null
  sortOrder: number
  moduleId: string
}

export interface Enrollment {
  id: string
  courseId: string
  courseName: string
  enrolledAt: string
  completedAt: string | null
  progressPercentage: number
  thumbnailUrl: string | null
}

export interface Bookmark {
  id: string
  targetType: string
  targetId: string
  title: string
  description: string | null
  createdAt: string
}

export interface LearnerNotification {
  id: string
  title: string
  message: string
  notificationType: string
  targetType: string | null
  targetId: string | null
  isRead: boolean
  createdAt: string
}

export interface DashboardData {
  enrolledCourses: number
  completedCourses: number
  overallProgress: number
  recentEnrollments: Enrollment[]
  continueLearning: Enrollment[]
  recommended: CourseSummary[]
}

export interface Resource {
  id: string
  title: string
  description: string | null
  fileUrl: string
  resourceType: string
  subjectId: string | null
  institutionId: string
  createdAt: string
}

export interface LiveClass {
  id: string
  title: string
  description: string | null
  scheduledAt: string
  durationMinutes: number
  status: string
  meetingUrl: string | null
  maxParticipants: number | null
  teacherId: string
  subjectId: string | null
  createdAt: string
}

export interface Announcement {
  id: string
  title: string
  content: string
  priority: string
  authorName: string | null
  createdAt: string
}

export interface Certificate {
  id: string
  title: string
  description: string | null
  studentName: string
  completionDate: string
  issueDate: string
  status: string
  verificationCode: string
  courseOrProgramme: string | null
}

export interface SearchResult {
  courses: CourseSummary[]
  resources: Resource[]
  liveClasses: LiveClass[]
}

export interface CourseProgress {
  courseId: string
  completedLessons: number
  totalLessons: number
  completionPercentage: number
  lastLessonId: string | null
  startedAt: string | null
  completedAt: string | null
}

export interface ProfileUpdate {
  bio?: string
  interests?: string
  learningGoal?: string
  avatarUrl?: string
}

export const learnerApi = {
  getProfile: () => learnerFetch<LearnerProfile>("/v1/learner/me/profile"),
  updateProfile: (data: ProfileUpdate) =>
    learnerFetch<LearnerProfile>("/v1/learner/me/profile", {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  getDashboard: () => learnerFetch<DashboardData>("/v1/learner/me/dashboard"),
  getCourses: () => learnerFetch<CourseSummary[]>("/v1/learner/courses"),
  getCourse: (id: string) => learnerFetch<CourseDetail>(`/v1/learner/courses/${id}`),
  getCourseModules: (courseId: string) => learnerFetch<CourseModuleSummary[]>(`/v1/learner/courses/${courseId}/modules`),
  getModuleLessons: (moduleId: string) => learnerFetch<CourseLesson[]>(`/v1/learner/courses/modules/${moduleId}/lessons`),
  enroll: (courseId: string) =>
    learnerFetch<Enrollment>("/v1/learner/me/enrollments", {
      method: "POST",
      body: JSON.stringify({ courseId }),
    }),
  getEnrollments: () => learnerFetch<Enrollment[]>("/v1/learner/me/enrollments"),
  getProgress: (courseId: string) => learnerFetch<CourseProgress>(`/v1/learner/me/progress/${courseId}`),
  updateProgress: (lessonId: string, completionPercentage: number) =>
    learnerFetch<void>("/v1/learner/me/progress", {
      method: "POST",
      body: JSON.stringify({ lessonId, completionPercentage }),
    }),
  getResources: () => learnerFetch<Resource[]>("/v1/learner/resources"),
  getLiveClasses: () => learnerFetch<LiveClass[]>("/v1/learner/live-classes"),
  getAnnouncements: () => learnerFetch<Announcement[]>("/v1/learner/announcements"),
  getBookmarks: () => learnerFetch<Bookmark[]>("/v1/learner/me/bookmarks"),
  addBookmark: (targetType: string, targetId: string) =>
    learnerFetch<Bookmark>("/v1/learner/me/bookmarks", {
      method: "POST",
      body: JSON.stringify({ targetType, targetId }),
    }),
  removeBookmark: (id: string) =>
    learnerFetch<void>(`/v1/learner/me/bookmarks/${id}`, { method: "DELETE" }),
  checkBookmark: (targetType: string, targetId: string) =>
    learnerFetch<{ bookmarked: boolean }>(`/v1/learner/me/bookmarks/check?targetType=${targetType}&targetId=${targetId}`),
  getNotifications: () => learnerFetch<LearnerNotification[]>("/v1/learner/me/notifications"),
  getUnreadCount: () => learnerFetch<{ count: number }>("/v1/learner/me/notifications/unread-count"),
  markNotificationRead: (id: string) =>
    learnerFetch<void>(`/v1/learner/me/notifications/${id}/read`, { method: "PUT" }),
  markAllRead: () =>
    learnerFetch<void>("/v1/learner/me/notifications/read-all", { method: "PUT" }),
  getCertificates: () => learnerFetch<Certificate[]>("/v1/learner/me/certificates"),
  search: (q: string, type?: string) =>
    learnerFetch<SearchResult>(
      `/v1/learner/search?q=${encodeURIComponent(q)}${type ? `&type=${type}` : ""}`,
    ),
}
