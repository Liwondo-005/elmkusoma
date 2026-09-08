export interface ApiResponse<T> {
  success: boolean
  message?: string
  data?: T
  error?: string
  timestamp: string
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export interface TeacherResponse {
  id: string
  userId: string
  fullName: string
  email: string
  phone: string
  employeeNumber: string
  status: "ACTIVE" | "INACTIVE" | "ON_LEAVE"
  specialization: string
  hireDate: string
  bio: string
  createdAt: string
}

export interface TeacherRequest {
  userId: string
  employeeNumber?: string
  specialization?: string
  hireDate?: string
  bio?: string
}

export interface TeacherAssignmentResponse {
  id: string
  teacherId: string
  classGroupId: string
  subjectId: string
  academicYear: string
  createdAt: string
}

export interface TeacherAssignmentRequest {
  classGroupId: string
  subjectId: string
  academicYear?: string
}

export interface TeacherQualificationResponse {
  id: string
  teacherId: string
  qualificationName: string
  institutionName: string
  fieldOfStudy: string
  yearObtained: number
  certificateUrl: string
  createdAt: string
}

export interface TeacherQualificationRequest {
  qualificationName: string
  institutionName: string
  fieldOfStudy?: string
  yearObtained?: number
  certificateUrl?: string
}

export interface ParentResponse {
  id: string
  userId: string
  fullName: string
  email: string
  phone: string
  occupation: string
  relationshipType: "FATHER" | "MOTHER" | "GUARDIAN" | "OTHER"
  emergencyContact: string
  createdAt: string
}

export interface ParentRequest {
  userId: string
  occupation?: string
  relationshipType?: "FATHER" | "MOTHER" | "GUARDIAN" | "OTHER"
  emergencyContact?: string
}

export interface ParentStudentResponse {
  id: string
  parentId: string
  studentId: string
  studentName: string
  admissionNumber: string
  relationshipType: string
  isPrimary: boolean
  createdAt: string
}

export interface LinkStudentRequest {
  studentId: string
  relationshipType?: "FATHER" | "MOTHER" | "GUARDIAN" | "OTHER"
  isPrimary?: boolean
}

export interface ParentNotificationPreferenceResponse {
  id: string
  parentId: string
  attendanceAlerts: boolean
  gradeAlerts: boolean
  feeAlerts: boolean
  generalAnnouncements: boolean
  smsEnabled: boolean
  emailEnabled: boolean
  pushEnabled: boolean
}

export interface ParentNotificationPreferenceRequest {
  attendanceAlerts?: boolean
  gradeAlerts?: boolean
  feeAlerts?: boolean
  generalAnnouncements?: boolean
  smsEnabled?: boolean
  emailEnabled?: boolean
  pushEnabled?: boolean
}
