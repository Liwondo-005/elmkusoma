const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || ""

export class LearnerApiError extends Error {
  status: number
  code: string
  constructor(message: string, status: number, code: string) {
    super(message)
    this.name = "LearnerApiError"
    this.status = status
    this.code = code
  }
}

function statusMessage(status: number, body: Record<string, unknown>): string {
  const serverMsg =
    (typeof body.error === "string" && body.error) ||
    (typeof body.message === "string" && body.message) ||
    ""
  switch (status) {
    case 401:
      return serverMsg || "Your session has expired. Please sign in again."
    case 403:
      return serverMsg || "You do not have permission to perform this action."
    case 404:
      return serverMsg || "The requested item was not found."
    case 409:
      return serverMsg || "This conflicts with the current state. Refresh and try again."
    case 422:
      return serverMsg || "Some of the provided information is invalid."
    default:
      return serverMsg || `Request failed (${status})`
  }
}

async function learnerFetch<T>(path: string, options?: RequestInit): Promise<T> {
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
    throw new LearnerApiError(statusMessage(res.status, body), res.status, String(body.code || ""))
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
  contentType: string
  contentUrl: string | null
  durationMinutes: number | null
  sortOrder: number
  moduleId: string
  isFree: boolean
}

export interface LessonMaterial {
  id: string
  kind: "resource" | "video"
  title: string
  visibility: string | null
  url: string | null
  durationSeconds: number | null
}

export interface LiveLessonSummary {
  id: string
  title: string
  description: string | null
  status: string
  scheduledAt: string | null
  durationMinutes: number | null
  classGroupId: string | null
  sessionType: string
  recordingEnabled: boolean
  recordingUrl: string | null
  canJoin: boolean
  inPast: boolean
}

export interface CourseLessonDetail {
  id: string
  moduleId: string
  moduleTitle: string | null
  title: string
  contentType: string
  contentUrl: string | null
  durationMinutes: number | null
  sortOrder: number
  isFree: boolean
  completed: boolean
  progressPercentage: number
  previousLessonId: string | null
  previousLessonTitle: string | null
  nextLessonId: string | null
  nextLessonTitle: string | null
  courseId: string | null
  totalLessons: number
  currentIndex: number
  materials?: LessonMaterial[]
  /** Lesson ↔ Live Class: real live session linked to this lesson (null when none). */
  liveClass?: LiveLessonSummary | null
}

export interface CourseLessonFlat {
  id: string
  moduleId: string
  moduleTitle: string
  title: string
  contentType: string
  contentUrl: string | null
  durationMinutes: number | null
  sortOrder: number
  isFree: boolean
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
  lastAccessedAt: string | null
}

export interface Bookmark {
  id: string
  targetType: string
  targetId: string
  targetTitle: string
  targetAvailable: boolean
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
  fileUrl?: string
  storageUrl?: string
  thumbnailUrl?: string
  mimeType?: string
  fileSize?: number | null
  durationSeconds?: number | null
  pageCount?: number | null
  width?: number | null
  height?: number | null
  externalUrl?: string | null
  visibility?: string
  processingStatus?: string | null
  processingError?: string | null
  isDownloadable?: boolean | null
  isPreviewable?: boolean | null
  resourceType: string
  subjectId: string | null
  institutionId: string
  createdAt: string
}

export interface VideoTutorial {
  id: string
  institutionId: string
  lessonId: string | null
  title: string
  description: string | null
  durationSeconds: number | null
  recordingUrl: string | null
  thumbnailUrl: string | null
  status: string
  visibility: string
  isDownloadable: boolean | null
  tagNames: string[] | null
  createdAt: string
}

export interface VideoTutorialProgress {
  id: string
  videoTutorialId: string
  studentId: string
  positionSeconds: number
  completed: boolean
  completionPercentage: number
  lastWatchedAt: string | null
  watchCount: number
  totalWatchTimeSeconds: number
  lastPositionSeconds: number
}

export interface ResourceAnnotation {
  id: string
  resourceId: string
  studentId: string
  studentName: string
  content: string
  positionData: string | null
  isPrivate: boolean | null
  parentAnnotationId: string | null
  createdAt: string
  updatedAt: string | null
}

export interface LiveClass {
  id: string
  title: string
  description: string | null
  scheduledAt: string
  durationMinutes: number
  status: string
  maxParticipants: number | null
  teacherId: string
  subjectId: string | null
  teacherName: string | null
  subjectName: string | null
  recordingUrl: string | null
  canJoin: boolean | null
  createdAt: string
  /** Declared broadcast source: BROWSER, MOBILE, USB_CAMERA, PROFESSIONAL_CAMERA, OBS, ENCODER, STUDIO, OTHER */
  broadcastSource?: string
}

export interface LiveSessionJoinResponse {
  liveKitToken: string | null
  liveKitUrl: string | null
  roomName: string
  liveKitAvailable: boolean
  classStatus: string
  message: string
}

export interface EventJoinResponse {
  token: string | null
  serverUrl: string | null
  roomName: string
  meetingUrl: string | null
  eventStatus: string
  liveKitAvailable: boolean
  waitingRoom?: boolean
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
  eventStatus?: string | null
  accessLevel?: string | null
  timezone?: string | null
  presenterName?: string | null
  providerId?: string | null
  cancelledAt?: string | null
  cancellationReason?: string | null
  rescheduledFrom?: string | null
  recordingStatus?: string | null
  recordingUrl?: string | null
  relatedCourseId?: string | null
  relatedCourseTitle?: string | null
  relatedLessonId?: string | null
  relatedModuleId?: string | null
  almostFull?: boolean
  attended?: boolean
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

export interface SearchFilters {
  category?: string
  level?: string
  provider?: string
  resourceType?: string
  dateFrom?: string
  dateTo?: string
  sort?: string
  sortBy?: string
}

export interface SearchResult {
  courses: CourseSummary[]
  resources: Resource[]
  liveClasses: LiveClass[]
  announcements: any[]
  events?: EventItem[]
  replays?: ReplayItem[]
}

export interface ReplayItem {
  id: string
  eventId: string
  eventTitle: string
  title: string
  presenterName: string | null
  thumbnailUrl: string | null
  videoUrl: string
  durationSeconds: number
  viewCount: number
  recordedAt: string
  eventType: string
  positionSeconds: number
  completed: boolean
  relatedCourseId: string | null
  relatedCourseTitle: string | null
  relatedLessonId: string | null
  relatedLessonTitle: string | null
  recordingStatus?: string | null
  status?: string | null
  captionUrl?: string | null
}

export interface ReplayDetail {
  replay: ReplayItem
  relatedResources: Resource[]
  upcomingEvents: EventItem[]
}

export interface ReplayProgress {
  positionSeconds: number
  completed: boolean
  lastWatchedAt: string
}

export interface LearningGoal {
  id: string
  userId?: string
  title: string
  description: string | null
  goalType: string
  targetDate: string | null
  progressPercentage: number
  status: string
  completedAt?: string | null
  createdAt?: string
}

export interface GoalInput {
  title: string
  description?: string
  goalType?: string
  targetDate?: string | null
  progressPercentage?: number
  status?: string
}

export interface LearningPathItem {
  id: string
  courseId: string
  title: string
  progress: number
  status: string
}

function normalizeReplay(raw: Record<string, unknown>): ReplayItem {
  const rec = raw as Record<string, any>
  const durationSeconds = Number(rec.durationSeconds ?? 0)
  const positionSeconds = Number(rec.positionSeconds ?? rec.lastPositionSeconds ?? rec.position ?? 0)
  const recordedAt = String(rec.recordedAt ?? rec.createdAt ?? "")
  const title = String(rec.title ?? rec.eventTitle ?? "")
  const completed =
    typeof rec.completed === "boolean"
      ? rec.completed
      : durationSeconds > 0 && positionSeconds > 0 && positionSeconds >= durationSeconds - 10
  return {
    id: String(rec.id ?? ""),
    eventId: String(rec.eventId ?? ""),
    eventTitle: String(rec.eventTitle ?? title),
    title,
    presenterName: rec.presenterName ?? null,
    thumbnailUrl: rec.thumbnailUrl ?? null,
    videoUrl: String(rec.videoUrl ?? rec.recordingUrl ?? ""),
    durationSeconds,
    viewCount: Number(rec.viewCount ?? 0),
    recordedAt,
    eventType: String(rec.eventType ?? ""),
    positionSeconds,
    completed,
    relatedCourseId: rec.relatedCourseId ?? null,
    relatedCourseTitle: rec.relatedCourseTitle ?? null,
    relatedLessonId: rec.relatedLessonId ?? null,
    relatedLessonTitle: rec.relatedLessonTitle ?? null,
    captionUrl: rec.captionUrl ?? rec.captionsUrl ?? null,
    recordingStatus: rec.recordingStatus ?? null,
    status: rec.status ?? null,
  }
}

export function isAlmostFull(event: Pick<EventItem, "almostFull" | "availableSpots" | "maxParticipants" | "registeredCount">): boolean {
  if (typeof event.almostFull === "boolean") return event.almostFull
  const max = event.maxParticipants ?? 0
  if (max <= 0) return false
  const left = event.availableSpots != null ? event.availableSpots : max - event.registeredCount
  const threshold = Math.max(2, Math.floor(max * 0.1))
  return left > 0 && left <= threshold
}

export function isReplayFailed(replay: Pick<ReplayItem, "recordingStatus" | "status">): boolean {
  const s = (replay.status || replay.recordingStatus || "").toUpperCase()
  return s === "FAILED" || s === "RECORDING_FAILED"
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

export interface ContinueLearningState {
  courseId: string
  courseTitle: string
  moduleId: string
  moduleTitle: string
  lessonId: string
  lessonTitle: string
  lastAccessedAt: string
}

export function getLastAccessedLesson(): ContinueLearningState | null {
  if (typeof window === "undefined") return null
  try {
    const raw = localStorage.getItem("elmkusoma_continue_learning")
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function setLastAccessedLesson(state: ContinueLearningState): void {
  if (typeof window === "undefined") return
  localStorage.setItem("elmkusoma_continue_learning", JSON.stringify(state))
}

export const learnerApi = {
  getProfile: () => learnerFetch<LearnerProfile>("/v1/learner/me/profile"),
  updateProfile: (data: ProfileUpdate) =>
    learnerFetch<LearnerProfile>("/v1/learner/me/profile", {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  getDashboard: () => learnerFetch<DashboardData>("/v1/learner/me/dashboard"),
  getCourses: async (params?: { page?: number; size?: number }) => {
    const searchParams = new URLSearchParams()
    if (params?.page != null) searchParams.set("page", String(Math.max(0, params.page - 1)))
    if (params?.size != null) searchParams.set("size", String(params.size))
    const qs = searchParams.toString()
    // Backend returns a Spring Page ({content: [...]}) since pagination;
    // accept a plain array too for backward compatibility.
    const data = await learnerFetch<{ content?: CourseSummary[] } | CourseSummary[]>(`/v1/learner/courses${qs ? `?${qs}` : ""}`)
    if (Array.isArray(data)) return data
    return data.content ?? []
  },
  getCourse: (id: string) => learnerFetch<CourseDetail>(`/v1/learner/courses/${id}`),
  getCourseModules: (courseId: string) => learnerFetch<CourseModuleSummary[]>(`/v1/learner/courses/${courseId}/modules`),
  getModuleLessons: (moduleId: string) => learnerFetch<CourseLesson[]>(`/v1/learner/courses/modules/${moduleId}/lessons`),
  getCourseLessons: (courseId: string) => learnerFetch<CourseLessonFlat[]>(`/v1/learner/courses/${courseId}/lessons`),
  getLessonDetail: (lessonId: string) => learnerFetch<CourseLessonDetail>(`/v1/learner/courses/lessons/${lessonId}`),
  completeLesson: (lessonId: string) =>
    learnerFetch<{ lessonId: string; completed: boolean; courseProgressPercentage: number }>(
      `/v1/learner/me/lessons/${lessonId}/complete`,
      { method: "POST" },
    ),
  enroll: (courseId: string) =>
    learnerFetch<Enrollment>("/v1/learner/me/enrollments", {
      method: "POST",
      body: JSON.stringify({ courseId }),
    }),
  getEnrollments: () => learnerFetch<Enrollment[]>("/v1/learner/me/enrollments"),
  getOfferings: async (params?: { page?: number; size?: number; q?: string; educationLevel?: string }) => {
    const searchParams = new URLSearchParams()
    if (params?.page != null) searchParams.set("page", String(Math.max(0, params.page)))
    if (params?.size != null) searchParams.set("size", String(params.size))
    if (params?.q) searchParams.set("q", params.q)
    if (params?.educationLevel) searchParams.set("educationLevel", params.educationLevel)
    const qs = searchParams.toString()
    const data = await learnerFetch<{ content?: LearningOffering[] } | LearningOffering[]>(`/v1/learner/offerings${qs ? `?${qs}` : ""}`)
    if (Array.isArray(data)) return data
    return data.content ?? []
  },
  getOffering: (id: string) => learnerFetch<LearningOffering>(`/v1/learner/offerings/${id}`),
  getProgress: (courseId: string) => learnerFetch<CourseProgress>(`/v1/learner/me/progress/${courseId}`),
  updateProgress: (lessonId: string, completionPercentage: number) =>
    learnerFetch<void>("/v1/learner/me/progress", {
      method: "POST",
      body: JSON.stringify({ lessonId, completionPercentage }),
    }),
  getResources: async (params?: { page?: number; size?: number }) => {
    const searchParams = new URLSearchParams()
    if (params?.page != null) searchParams.set("page", String(Math.max(0, params.page - 1)))
    if (params?.size != null) searchParams.set("size", String(params.size))
    const qs = searchParams.toString()
    const data = await learnerFetch<{ content?: Resource[] } | Resource[]>(`/v1/learner/resources${qs ? `?${qs}` : ""}`)
    if (Array.isArray(data)) return data
    return data.content ?? []
  },
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
    learnerFetch<boolean>(`/v1/learner/me/bookmarks/check?targetType=${targetType}&targetId=${targetId}`),
  getNotifications: () => learnerFetch<LearnerNotification[]>("/v1/learner/me/notifications"),
  getUnreadCount: () => learnerFetch<{ count: number }>("/v1/learner/me/notifications/unread-count"),
  markNotificationRead: (id: string) =>
    learnerFetch<void>(`/v1/learner/me/notifications/${id}/read`, { method: "PUT" }),
  markAllRead: () =>
    learnerFetch<void>("/v1/learner/me/notifications/read-all", { method: "PUT" }),
  getCertificates: () => learnerFetch<Certificate[]>("/v1/learner/me/certificates"),
  search: async (q: string, type?: string, filters?: SearchFilters, page?: number, size?: number): Promise<SearchResult> => {
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
    if (page != null) searchParams.set("page", String(page))
    if (size != null) searchParams.set("size", String(size))
    const data = await learnerFetch<Partial<SearchResult>>(`/v1/learner/search?${searchParams.toString()}`)
    const base: SearchResult = {
      courses: data.courses || [],
      resources: data.resources || [],
      liveClasses: data.liveClasses || [],
      announcements: data.announcements || [],
      events: data.events || [],
      replays: data.replays || [],
    }
    if (type === "EVENT") {
      const events = await learnerApi.getEvents({ search: q }).catch(() => [] as EventItem[])
      base.events = events
      base.courses = []
      base.resources = []
      base.liveClasses = []
      base.announcements = []
    }
    if (type === "REPLAY") {
      const replays = await learnerApi.getReplays({ search: q }).catch(() => [] as ReplayItem[])
      base.replays = replays
      base.courses = []
      base.resources = []
      base.liveClasses = []
      base.announcements = []
    }
    return base
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
  joinEvent: (eventId: string) =>
    learnerFetch<EventJoinResponse>(`/v1/learner/events/${eventId}/join`, { method: "POST" }),
  cancelEventRegistration: (eventId: string, reason?: string) =>
    learnerFetch<void>(`/v1/learner/events/${eventId}/cancel`, {
      method: "POST",
      body: JSON.stringify({ reason: reason || "" }),
    }),
  getRegisteredEvents: () => learnerFetch<EventItem[]>("/v1/learner/events/registered"),
  getRegisteredPastEvents: () => learnerFetch<EventItem[]>("/v1/learner/events/registered/past"),
  getEventMaterials: (eventId: string) =>
    learnerFetch<EventMaterial[]>(`/v1/learner/events/${eventId}/materials`),
  getVideoLibrary: () => learnerApi.getResources(),
  getVideoResources: () =>
    learnerApi.getResources().then((resources) =>
      resources.filter((r) => r.resourceType === "VIDEO")
    ),
  getRelatedCourses: (courseId: string) =>
    learnerFetch<CourseSummary[]>(`/v1/learner/courses/${courseId}/related`),
  getResource: (id: string) => learnerFetch<Resource>(`/v1/learner/resources/${id}`),
  getResourceUrl: (r: Resource) => r.storageUrl || r.fileUrl || r.thumbnailUrl || "",
  getVideoTutorials: () => learnerFetch<VideoTutorial[]>("/v1/video-tutorials"),
  getVideoTutorial: (id: string) => learnerFetch<VideoTutorial>(`/v1/video-tutorials/${id}`),
  getVideoTutorialProgressList: () => learnerFetch<VideoTutorialProgress[]>("/v1/video-tutorials/progress"),
  updateVideoProgress: (id: string, data: { positionSeconds: number; durationSeconds?: number; completed?: boolean }) => {
    const params = new URLSearchParams()
    params.set("positionSeconds", String(Math.floor(data.positionSeconds)))
    if (data.durationSeconds != null) params.set("durationSeconds", String(Math.floor(data.durationSeconds)))
    if (data.completed != null) params.set("completed", String(data.completed))
    return learnerFetch<VideoTutorialProgress>(`/v1/video-tutorials/${id}/progress?${params.toString()}`, {
      method: "PUT",
    })
  },
  getResourceAnnotations: (resourceId: string) =>
    learnerFetch<ResourceAnnotation[]>(`/v1/resources/${resourceId}/annotations`),
  createResourceAnnotation: (resourceId: string, data: { content: string; isPrivate?: boolean; parentAnnotationId?: string }) =>
    learnerFetch<ResourceAnnotation>(`/v1/resources/${resourceId}/annotations`, {
      method: "POST",
      body: JSON.stringify(data),
    }),
  deleteResourceAnnotation: (resourceId: string, annotationId: string) =>
    learnerFetch<void>(`/v1/resources/${resourceId}/annotations/${annotationId}`, { method: "DELETE" }),
  getRelatedResources: (resourceId: string) =>
    learnerFetch<Resource[]>(`/v1/learner/resources/${resourceId}/related`),

  // ── Authorized content access ──────────────────────────────────────────
  /** Backend-authorized, short-lived URL for viewing the real content. */
  getResourceContentUrl: (resourceId: string) =>
    learnerFetch<string>(`/v1/resources/${resourceId}/content-url`),
  /** Records a view after the backend has re-checked access. */
  recordResourceView: (resourceId: string) =>
    learnerFetch<boolean>(`/v1/resources/${resourceId}/analytics/view`, { method: "POST" }),
  /**
   * Backend-authorized download: bytes are fetched with credentials and saved
   * through a temporary object URL — the storage URL is never handed out.
   */
  downloadResource: async (resourceId: string, fallbackName: string) => {
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
    const res = await fetch(`${API_BASE_URL}/v1/resources/${resourceId}/download`, {
      headers: {
        "X-Institution-Id": institutionId,
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    })
    if (!res.ok) {
      const body = await res.json().catch(() => ({}))
      throw new LearnerApiError(statusMessage(res.status, body), res.status, String(body.code || ""))
    }
    const disposition = res.headers.get("Content-Disposition") || ""
    const match = disposition.match(/filename="?([^";]+)"?/)
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = match ? match[1] : fallbackName
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  },

  getGoals: () => learnerFetch<LearningGoal[]>("/v1/learner/me/goals"),
  createGoal: (data: GoalInput) =>
    learnerFetch<LearningGoal>("/v1/learner/me/goals", {
      method: "POST",
      body: JSON.stringify({ goalType: "PERSONAL", status: "ACTIVE", progressPercentage: 0, ...data }),
    }),
  updateGoal: (id: string, data: Partial<GoalInput>) =>
    learnerFetch<LearningGoal>(`/v1/learner/me/goals/${id}`, {
      method: "PUT",
      body: JSON.stringify(data),
    }),
  deleteGoal: (id: string) =>
    learnerFetch<void>(`/v1/learner/me/goals/${id}`, { method: "DELETE" }),
  getLearningPaths: () => learnerFetch<LearningPathItem[]>("/v1/learner/me/learning-paths"),

  getReplays: async (params?: { eventType?: string; search?: string; dateFrom?: string; dateTo?: string }) => {
    const data = await learnerFetch<unknown[]>("/v1/learner/replays")
    let items = (Array.isArray(data) ? data : []).map((raw) =>
      normalizeReplay(raw as Record<string, unknown>)
    )
    if (params?.search) {
      const q = params.search.toLowerCase()
      items = items.filter(
        (r) =>
          r.title.toLowerCase().includes(q) ||
          r.eventTitle.toLowerCase().includes(q)
      )
    }
    if (params?.eventType) {
      items = items.filter((r) => r.eventType === params.eventType)
    }
    if (params?.dateFrom) {
      items = items.filter((r) => r.recordedAt >= params.dateFrom!)
    }
    if (params?.dateTo) {
      items = items.filter((r) => r.recordedAt <= params.dateTo!)
    }
    return items
  },
  getReplay: async (id: string): Promise<ReplayDetail> => {
    const data = await learnerFetch<Record<string, unknown>>(`/v1/learner/replays/${id}`)
    if (data && typeof data === "object" && "replay" in data) {
      return data as unknown as ReplayDetail
    }
    return { replay: normalizeReplay(data), relatedResources: [], upcomingEvents: [] }
  },
  getReplayProgress: async (id: string): Promise<ReplayProgress> => {
    const data = await learnerFetch<Record<string, unknown>>(`/v1/learner/replays/${id}/progress`)
    return {
      positionSeconds: Number(data.positionSeconds ?? data.position ?? 0),
      completed: Boolean(data.completed),
      lastWatchedAt: String(data.lastWatchedAt ?? ""),
    }
  },
  updateReplayProgress: (id: string, positionSeconds: number, completed?: boolean) =>
    learnerFetch<void>(`/v1/learner/replays/${id}/progress`, {
      method: "PUT",
      body: JSON.stringify({ positionSeconds, position: positionSeconds, completed }),
    }),
  getLiveSessionHealth: () => learnerFetch<{ status: string }>("/v1/live-session/health"),
}
