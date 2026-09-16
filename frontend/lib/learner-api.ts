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

export interface CourseDetail {
  course: CourseSummary
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
  courseTitle: string
  courseDescription: string | null
  courseThumbnailUrl: string | null
  courseLevel: string | null
  courseCategory: string | null
  enrolledAt: string
  completedAt: string | null
  progressPercentage: number
}

export interface Bookmark {
  id: string
  targetType: string
  targetId: string
  targetTitle: string
  targetAvailable?: boolean
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
  teacherName: string | null
  subjectName: string | null
  recordingUrl: string | null
  canJoin: boolean | null
  createdAt: string
}

export interface LiveSessionJoinResponse {
  liveKitToken: string | null
  liveKitUrl: string | null
  roomName: string
  liveKitAvailable: boolean
  classStatus: string
  message: string
}

export interface ParticipantInfo {
  userId: string
  userName: string
  role: string
  joinedAt: string | null
  leftAt: string | null
  durationSeconds: number | null
  online: boolean
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

export interface EventItem {
  id: string
  institutionId: string
  organizerId: string
  organizerName: string | null
  title: string
  description: string | null
  eventType: string
  category: string | null
  location: string | null
  meetingUrl: string | null
  startsAt: string
  endsAt: string | null
  durationMinutes: number | null
  maxParticipants: number | null
  registeredCount: number
  availableSpots: number | null
  status: string
  thumbnailUrl: string | null
  tags: string | null
  isFree: boolean
  requiresApproval: boolean
  isRegistered: boolean
  registrationStatus: string | null
  materialCount: number
  hasRecording: boolean
  createdAt: string
}

export interface EventRegistration {
  id: string
  eventId: string
  eventTitle: string
  eventStartsAt: string | null
  eventEndsAt: string | null
  eventLocation: string | null
  eventMeetingUrl: string | null
  eventType: string | null
  status: string
  registeredAt: string
  cancelledAt: string | null
  attended: boolean
}

export interface EventMaterial {
  id: string
  eventId: string
  title: string
  description: string | null
  materialType: string
  fileUrl: string
  fileSize: number | null
  durationMinutes: number | null
  sortOrder: number
  isPublic: boolean
}

export interface SearchResult {
  courses: CourseSummary[]
  resources: Resource[]
  liveClasses: LiveClass[]
  announcements?: Announcement[]
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

export interface SearchFilters {
  dateFrom?: string
  dateTo?: string
  resourceType?: string
  level?: string
  category?: string
  provider?: string
  sort?: string
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
  getLiveClass: (id: string) => learnerFetch<LiveClass>(`/v1/learner/live-classes/${id}`),
  joinLiveSession: (classId: string) =>
    learnerFetch<LiveSessionJoinResponse>(`/v1/live-session/join/${classId}`, { method: "POST" }),
  getLiveParticipants: (classId: string) =>
    learnerFetch<ParticipantInfo[]>(`/v1/live-session/participants/${classId}`),
  reportLiveIssue: (classId: string, issueType: string, description?: string) =>
    learnerFetch<void>(`/v1/live-session/report/${classId}`, {
      method: "POST",
      body: JSON.stringify({ issueType, description }),
    }),
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
  search: (q: string, type?: string, filters?: SearchFilters) => {
    const searchParams = new URLSearchParams()
    searchParams.set("q", q)
    if (type) searchParams.set("type", type)
    if (filters?.dateFrom) searchParams.set("dateFrom", filters.dateFrom)
    if (filters?.dateTo) searchParams.set("dateTo", filters.dateTo)
    if (filters?.resourceType) searchParams.set("resourceType", filters.resourceType)
    if (filters?.level) searchParams.set("level", filters.level)
    if (filters?.category) searchParams.set("category", filters.category)
    if (filters?.provider) searchParams.set("provider", filters.provider)
    if (filters?.sort) searchParams.set("sort", filters.sort)
    return learnerFetch<SearchResult>(`/v1/learner/search?${searchParams.toString()}`)
  },
  getEvents: (params?: { eventType?: string; category?: string; search?: string }) => {
    const searchParams = new URLSearchParams()
    if (params?.eventType) searchParams.set("eventType", params.eventType)
    if (params?.category) searchParams.set("category", params.category)
    if (params?.search) searchParams.set("search", params.search)
    const qs = searchParams.toString()
    return learnerFetch<EventItem[]>(`/v1/learner/events${qs ? `?${qs}` : ""}`)
  },
  getUpcomingEvents: () => learnerFetch<EventItem[]>("/v1/learner/events/upcoming"),
  getPastEvents: () => learnerFetch<EventItem[]>("/v1/learner/events/past"),
  getEvent: (id: string) => learnerFetch<EventItem>(`/v1/learner/events/${id}`),
  registerForEvent: (eventId: string) =>
    learnerFetch<EventRegistration>(`/v1/learner/events/${eventId}/register`, { method: "POST" }),
  cancelEventRegistration: (eventId: string, reason?: string) =>
    learnerFetch<void>(`/v1/learner/events/${eventId}/cancel`, {
      method: "POST",
      body: JSON.stringify({ reason: reason || "" }),
    }),
  getRegisteredEvents: () => learnerFetch<EventItem[]>("/v1/learner/events/registered"),
  getRegisteredPastEvents: () => learnerFetch<EventItem[]>("/v1/learner/events/registered/past"),
  getEventMaterials: (eventId: string) =>
    learnerFetch<EventMaterial[]>(`/v1/learner/events/${eventId}/materials`),
  getVideoLibrary: () => learnerFetch<Resource[]>("/v1/learner/resources"),
  getVideoResources: () =>
    learnerFetch<Resource[]>("/v1/learner/resources").then((resources) =>
      resources.filter((r) => r.resourceType === "VIDEO")
    ),
  getRelatedCourses: (courseId: string) =>
    learnerFetch<CourseSummary[]>(`/v1/learner/courses/${courseId}/related`),
  getResource: (id: string) => learnerFetch<Resource>(`/v1/learner/resources/${id}`),
  getRelatedResources: (resourceId: string) =>
    learnerFetch<Resource[]>(`/v1/learner/resources/${resourceId}/related`),
}
