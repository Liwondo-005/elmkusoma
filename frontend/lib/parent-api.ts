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

export interface AttentionItem {
  id: string
  type: string
  priority: string
  title: string
  description: string
  timestamp: string
  relatedEntityType: string
  relatedEntityId: string
  actionLabel: string
  actionUrl: string
}

export interface PositiveSignal {
  id: string
  type: string
  title: string
  description: string
  timestamp: string
  relatedEntityType: string
  relatedEntityId: string
}

export interface RecommendationItem {
  id: string
  type: string
  title: string
  description: string
  actionLabel: string
  actionUrl: string
  evidenceSource: string
}

export interface UpcomingItem {
  id: string
  type: string
  title: string
  description: string
  timestamp: string
  relatedEntityType: string
  relatedEntityId: string
  actionLabel: string
}

export interface WeeklyBrief {
  lessonsCompleted: number
  assignmentsCompleted: number
  assignmentsPending: number
  assignmentsOverdue: number
  liveClassesAttended: number
  assessmentsCompleted: number
  presentDays: number
  absentDays: number
  lateDays: number
  highlights: string[]
  focusNextWeek: string[]
  upcomingThisWeek: UpcomingItem[]
}

export interface ParentIntelligence {
  childId: string
  childName: string
  needsAttention: AttentionItem[]
  doingWell: PositiveSignal[]
  recommendations: RecommendationItem[]
  weeklyBrief: WeeklyBrief
  upcoming: UpcomingItem[]
}

export interface CalendarEvent {
  id: string
  type: string
  title: string
  description: string
  start: string
  end: string
  status: string
  relatedEntityType: string
  relatedEntityId: string
  isActionRequired: boolean
}

export interface ParentCalendar {
  events: CalendarEvent[]
}

export interface PaymentItem {
  id: string
  amount: number
  currency: string
  description: string
  serviceType: string
  status: string
  paidAt: string | null
  createdAt: string
  providerReference: string | null
}

export interface ParentPayments {
  outstanding: number
  paidThisTerm: number
  recentPayments: PaymentItem[]
  pendingPayments: PaymentItem[]
}

export interface AchievementItem {
  id: string
  title: string
  description: string
  achievementType: string
  icon: string | null
  color: string | null
  relatedEntityType: string
  relatedEntityId: string | null
  achievedAt: string
}

export interface ParentAchievements {
  achievements: AchievementItem[]
  totalAchievements: number
}

export interface GoalItem {
  id: string
  title: string
  description: string
  goalType: string
  status: string
  progressPercentage: number
  targetDate: string | null
  completedAt: string | null
  relatedEntityType: string
  relatedEntityId: string | null
}

export interface ParentGoals {
  goals: GoalItem[]
  activeCount: number
  completedCount: number
}

export interface LibraryCategory {
  name: string
  description: string
  items: LibraryItem[]
}

export interface LibraryItem {
  id: string
  title: string
  description: string
  resourceType: string
  fileUrl: string
  subject: string
  targetGroup: string
}

export interface ParentLibrary {
  categories: LibraryCategory[]
}

export interface SupportTicketItem {
  id: string
  subject: string
  description: string
  category: string
  priority: string
  status: string
  createdAt: string
  resolvedAt: string | null
  messageCount: number
}

export interface ParentSupport {
  tickets: SupportTicketItem[]
  openCount: number
  resolvedCount: number
}

export interface EntitlementItem {
  id: string
  serviceType: string
  serviceId: string
  status: string
  startsAt: string
  expiresAt: string | null
}

export interface LiveClassData {
  liveClasses: LiveClassItem[]
}

export interface LiveClassItem {
  id: string
  title: string
  description?: string
  status: string
  scheduledAt: string
  durationMinutes: number
  teacherName?: string
  subjectName?: string
  participantCount?: number
  maxParticipants?: number
}

export interface AssessmentItem {
  id: string
  title: string
  subject: string
  subjectId: string | null
  totalMarks: number | null
  passMarks: number | null
  score: number | null
  isPassed: boolean | null
  percentage: number | null
  createdAt: string
  remarks: string | null
  status: string
}

export interface ParentAssessments {
  studentName: string
  className: string
  upcoming: AssessmentItem[]
  completed: AssessmentItem[]
}

export interface ActivityItem {
  id: string
  type: string
  title: string
  description: string
  timestamp: string
  relatedEntityType: string
  relatedEntityId: string
  status: string
}

export interface ParentActivity {
  activities: ActivityItem[]
}

export interface TeacherItem {
  id: string
  userId: string
  fullName: string
  email: string
  phone: string
  subject: string
  subjectId: string | null
  specialization: string
}

export interface ParentTeachers {
  teachers: TeacherItem[]
}

export interface SubjectPerformanceItem {
  subjectId: string
  subjectName: string
  averageMark: number | null
  grade: string | null
  gpa: number | null
  totalAssessments: number
  completedAssessments: number
  trend: string | null
}

export interface ParentSubjectPerformance {
  subjects: SubjectPerformanceItem[]
}

export interface CourseProgressItem {
  courseId: string
  courseName: string
  progressPercentage: number
  totalLessons: number
  completedLessons: number
  pendingLessons: number
  currentLesson: string | null
  lastActivity: string | null
  lastActivityAt: string | null
  nextLesson: string | null
  status: string
}

export interface ParentLearningProgress {
  courses: CourseProgressItem[]
}

export interface LearningProgressData {
  overallProgress: number
  completedCourses: number
  totalCourses: number
  courses: CourseProgressItem[]
}

export interface ParentNotificationItem {
  id: string
  title: string
  message: string
  category: string
  priority: string
  targetType: string
  targetId: string | null
  isRead: boolean
  createdAt: string
}

export interface ParentNotifications {
  notifications: ParentNotificationItem[]
  unreadCount: number
}

export interface MessageData {
  id: string
  senderId: string
  senderName?: string
  recipientId: string
  recipientName?: string
  subject: string
  body: string
  messageType: string
  isRead: boolean
  createdAt: string
}

export const parentApi = {
  getOverview: () => parentFetch<FamilyOverview>("/v1/my/overview"),
  getChildren: () => parentFetch<ChildOverview[]>("/v1/my/children"),
  getChild: (id: string) => parentFetch<ChildOverview>(`/v1/my/children/${id}`),
  getChildAttendance: (id: string) => parentFetch<AttendanceData>(`/v1/my/children/${id}/attendance`),
  getChildAssignments: (id: string) => parentFetch<AssignmentData>(`/v1/my/children/${id}/assignments`),
  getChildResults: (id: string) => parentFetch<ResultData>(`/v1/my/children/${id}/results`),

  getChildIntelligence: (id: string) => parentFetch<ParentIntelligence>(`/v1/my/children/${id}/intelligence`),
  getChildCalendar: (id: string, days?: number) => parentFetch<ParentCalendar>(`/v1/my/children/${id}/calendar?daysAhead=${days || 30}`),
  getChildAchievements: (id: string) => parentFetch<ParentAchievements>(`/v1/my/children/${id}/achievements`),
  getChildGoals: (id: string) => parentFetch<ParentGoals>(`/v1/my/children/${id}/goals`),
  getChildEntitlements: (id: string) => parentFetch<EntitlementItem[]>(`/v1/my/children/${id}/entitlements`),
  getChildLiveClasses: (id: string) => 
parentFetch<LiveClassData>(`/v1/my/children/${id}/live-classes`),
  getChildAssessments: (id: string) => parentFetch<ParentAssessments>(`/v1/my/children/${id}/assessments`),
  getChildActivity: (id: string) => parentFetch<ParentActivity>(`/v1/my/children/${id}/activity`),
  getChildTeachers: (id: string) => parentFetch<ParentTeachers>(`/v1/my/children/${id}/teachers`),
  getChildSubjectPerformance: (id: string) => parentFetch<ParentSubjectPerformance>(`/v1/my/children/${id}/subject-performance`),
  getChildLearningProgress: (id: string) => parentFetch<LearningProgressData>(`/v1/my/children/${id}/learning-progress`),

  getPayments: () => parentFetch<ParentPayments>("/v1/my/payments"),
  getChildPayments: (id: string) => parentFetch<PaymentItem[]>(`/v1/my/children/${id}/payments`),
  initiatePayment: (data: { studentId: string; amount: number; serviceType: string; serviceId?: string; description?: string }) =>
    parentFetch<{ paymentId: string; status: string; amount: number }>("/v1/my/payments/initiate", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  getMessages: () => parentFetch<MessageData[]>("/v1/my/messages"),
  getSentMessages: () => parentFetch<MessageData[]>("/v1/my/messages/sent"),
  sendMessage: (data: { recipientId: string; subject: string; body: string; messageType?: string }) =>
    parentFetch<MessageData>("/v1/my/messages", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  markMessageRead: (id: string) =>
    parentFetch<void>(`/v1/my/messages/${id}/read`, { method: "PUT" }),
  deleteMessage: (id: string) =>
    parentFetch<void>(`/v1/my/messages/${id}`, { method: "DELETE" }),

  getTickets: () => parentFetch<ParentSupport>("/v1/my/support/tickets"),

  getLibrary: () => parentFetch<ParentLibrary>("/v1/my/library"),

  getSupportTickets: () => parentFetch<ParentSupport>("/v1/my/support/tickets"),
  createSupportTicket: (data: { subject: string; description: string; category: string; priority?: string }) =>
    parentFetch<SupportTicketItem>("/v1/my/support/tickets", {
      method: "POST",
      body: JSON.stringify(data),
    }),
  getTicketMessages: (ticketId: string) => parentFetch<Array<{ id: string; senderId: string; message: string; createdAt: string }>>(`/v1/my/support/tickets/${ticketId}/messages`),
  addTicketMessage: (ticketId: string, message: string) =>
    parentFetch<{ id: string }>(`/v1/my/support/tickets/${ticketId}/messages`, {
      method: "POST",
      body: JSON.stringify({ message }),
    }),

  getNotifications: (page?: number, size?: number) =>
    parentFetch<ParentNotifications>(`/v1/my/notifications?page=${page || 0}&size=${size || 20}`),
  markNotificationRead: (id: string) =>
    parentFetch<string>(`/v1/my/notifications/${id}/read`, { method: "PUT" }),

  updateProfile: (data: { firstName?: string; lastName?: string; phone?: string }) =>
    parentFetch<{ status: string; fullName: string }>("/v1/my/profile", {
      method: "PUT",
      body: JSON.stringify(data),
    }),
}
