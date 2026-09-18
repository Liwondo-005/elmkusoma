export type ProgrammeType = "DIPLOMA" | "CERTIFICATE" | "DEGREE" | "HIGHER_DIPLOMA" | "PROFESSIONAL"
export type EducationLevelType = "COLLEGE" | "VETA" | "UNIVERSITY"

export interface Programme {
  id: string
  institutionId: string
  name: string
  code: string
  description?: string
  programmeType: ProgrammeType
  educationLevel: EducationLevelType
  durationMonths?: number
  creditHours?: number
  isActive: boolean
  createdAt: string
  updatedAt?: string
}

export interface Department {
  id: string
  institutionId: string
  name: string
  code: string
  description?: string
  programmeIds?: string[]
  headOfDepartmentId?: string
  isActive: boolean
  createdAt: string
  updatedAt?: string
}

export type CompetencyStatus = "NOT_STARTED" | "LEARNING" | "PRACTICING" | "ASSESSED" | "COMPETENT" | "NEEDS_PRACTICE" | "COMPLETED"
export type CompetencyType = "SKILL" | "KNOWLEDGE" | "PRACTICAL" | "PROFESSIONAL"

export interface Competency {
  id: string
  institutionId: string
  name: string
  code?: string
  description?: string
  competencyType: CompetencyType
  programmeId?: string
  subjectId?: string
  sortOrder?: number
  isActive: boolean
  createdAt: string
}

export interface CompetencyRecord {
  id: string
  institutionId: string
  studentId: string
  competencyId: string
  competencyName?: string
  competencyType?: CompetencyType
  status: CompetencyStatus
  evidence?: string
  assessedBy?: string
  assessmentDate?: string
  lastPracticeDate?: string
  notes?: string
  createdAt: string
  updatedAt?: string
}

export interface CompetencySummary {
  total: number
  notStarted: number
  learning: number
  practicing: number
  assessed: number
  competent: number
  needsPractice: number
  completed: number
}

export type ProjectStatus = "IDEATION" | "PLANNING" | "IN_PROGRESS" | "REVIEW" | "COMPLETED" | "ARCHIVED"
export type SubmissionType = "DOCUMENT" | "IMAGE" | "VIDEO" | "CODE" | "PRESENTATION" | "FILE" | "OTHER"

export interface Project {
  id: string
  institutionId: string
  studentId: string
  subjectId?: string
  title: string
  objective?: string
  description?: string
  status: ProjectStatus
  instructorId?: string
  startDate?: string
  dueDate?: string
  completedDate?: string
  createdAt: string
  updatedAt?: string
}

export interface ProjectMilestone {
  id: string
  projectId: string
  title: string
  description?: string
  dueDate?: string
  isCompleted: boolean
  completedDate?: string
  sortOrder?: number
}

export interface ProjectSubmission {
  id: string
  projectId: string
  milestoneId?: string
  submissionType: SubmissionType
  title?: string
  fileUrl?: string
  description?: string
  feedback?: string
  grade?: string
  submittedAt?: string
}

export type PlacementStatus = "PLANNING" | "ACTIVE" | "COMPLETED" | "TERMINATED"

export interface FieldworkPlacement {
  id: string
  institutionId: string
  studentId: string
  programmeId?: string
  organisationName: string
  placementTitle: string
  supervisorName?: string
  supervisorEmail?: string
  supervisorPhone?: string
  institutionSupervisorId?: string
  startDate?: string
  endDate?: string
  status: PlacementStatus
  totalHoursRequired?: number
  totalHoursCompleted?: number
  objectives?: string
  remarks?: string
  createdAt: string
  updatedAt?: string
}

export interface LogbookEntry {
  id: string
  placementId: string
  entryDate: string
  activities: string
  hoursWorked?: number
  skillsUsed?: string
  challenges?: string
  learningOutcomes?: string
  supervisorComments?: string
  isApproved: boolean
  approvedBy?: string
  approvedAt?: string
  createdAt: string
}
