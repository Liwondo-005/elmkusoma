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

// ── Academic Module Types ────────────────────────────────────────

export type EducationLevel = "NURSERY" | "PRIMARY" | "SECONDARY" | "COLLEGE" | "VETA" | "UNIVERSITY"

export interface AcademicYear {
  id: string
  institutionId: string
  educationLevel: EducationLevel
  yearLabel: string
  startDate: string
  endDate: string
  isCurrent: boolean
  isActive: boolean
  createdAt: string
  updatedAt?: string
}

export interface AcademicYearRequest {
  institutionId: string
  educationLevel: EducationLevel
  yearLabel: string
  startDate: string
  endDate: string
  isCurrent?: boolean
}

export interface Term {
  id: string
  institutionId: string
  academicYearId: string
  name: string
  termNumber: number
  startDate: string
  endDate: string
  isActive: boolean
  createdAt: string
  updatedAt?: string
}

export interface TermRequest {
  institutionId: string
  name: string
  termNumber: number
  startDate: string
  endDate: string
}

export interface Grade {
  id: string
  institutionId: string
  educationLevel: EducationLevel
  name: string
  code?: string
  sortOrder: number
  isActive: boolean
  createdAt: string
  updatedAt?: string
}

export interface GradeRequest {
  institutionId: string
  educationLevel: EducationLevel
  name: string
  code?: string
  sortOrder?: number
}

export interface Subject {
  id: string
  institutionId: string
  educationLevel: EducationLevel
  name: string
  code?: string
  description?: string
  category?: string
  isActive: boolean
  createdAt: string
  updatedAt?: string
}

export interface SubjectRequest {
  institutionId: string
  educationLevel: EducationLevel
  name: string
  code?: string
  description?: string
}

export interface ClassGroup {
  id: string
  institutionId: string
  gradeId: string
  academicYearId: string
  termId: string
  name: string
  section?: string
  capacity?: number
  classTeacherId?: string
  isActive: boolean
  createdAt: string
  updatedAt?: string
}

export interface ClassGroupRequest {
  institutionId: string
  gradeId: string
  academicYearId: string
  termId: string
  name: string
  section?: string
  capacity?: number
  classTeacherId?: string
}

// ── Student Module Types ─────────────────────────────────────────

export type StudentStatus = "ACTIVE" | "GRADUATED" | "TRANSFERRED" | "EXPELLED"

export interface Student {
  id: string
  userId: string
  institutionId: string
  admissionNumber: string
  status: StudentStatus
  firstName: string
  middleName?: string
  lastName: string
  email: string
  phone?: string
  dateOfBirth?: string
  gender?: string
  address?: string
  city?: string
  region?: string
  guardianName?: string
  guardianPhone?: string
  guardianRelationship?: string
  enrollmentDate: string
  createdAt: string
  updatedAt?: string
}

export interface StudentRequest {
  institutionId: string
  userId: string
  firstName: string
  middleName?: string
  lastName: string
  email: string
  phone?: string
  dateOfBirth?: string
  gender?: string
  address?: string
  city?: string
  region?: string
  guardianName?: string
  guardianPhone?: string
  guardianRelationship?: string
  status?: StudentStatus
}

export interface StudentClassAssignment {
  id: string
  institutionId: string
  studentId: string
  classGroupId: string
  academicYearId: string
  termId: string
  assignedDate: string
  isActive: boolean
  createdAt: string
}

export interface AssignClassRequest {
  classGroupId: string
  academicYearId: string
  termId: string
}
