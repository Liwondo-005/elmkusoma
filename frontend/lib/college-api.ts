"use client"

import { api } from "./api-client"
import type {
  Programme, Department,
  Competency, CompetencyRecord, CompetencySummary,
  Project, ProjectMilestone, ProjectSubmission,
  FieldworkPlacement, LogbookEntry,
  ResearchProject, ResearchMilestone, ResearchResource, Thesis, StudyTask,
  DeepContent, DeepContentDto,
} from "./types/college"
import type { HigherEducationDashboard, StudentCourseEnrollment, AcademicRecord, CareerProfile } from "./types/college"

export const collegeApi = {
  // Programmes
  listProgrammes: () => api.get<Programme[]>("/api/v1/education/programmes"),
  getProgramme: (id: string) => api.get<Programme>(`/api/v1/education/programmes/${id}`),
  createProgramme: (data: Partial<Programme>) => api.post<Programme>("/api/v1/education/programmes", data),
  updateProgramme: (id: string, data: Partial<Programme>) => api.put<Programme>(`/api/v1/education/programmes/${id}`, data),
  deleteProgramme: (id: string) => api.delete<void>(`/api/v1/education/programmes/${id}`),

  // Departments
  listDepartments: () => api.get<Department[]>("/api/v1/education/departments"),
  getDepartment: (id: string) => api.get<Department>(`/api/v1/education/departments/${id}`),
  createDepartment: (data: Partial<Department>) => api.post<Department>("/api/v1/education/departments", data),
  updateDepartment: (id: string, data: Partial<Department>) => api.put<Department>(`/api/v1/education/departments/${id}`, data),
  deleteDepartment: (id: string) => api.delete<void>(`/api/v1/education/departments/${id}`),

  // Competencies
  listCompetencies: () => api.get<Competency[]>("/api/v1/education/competencies"),
  getCompetency: (id: string) => api.get<Competency>(`/api/v1/education/competencies/${id}`),
  createCompetency: (data: Partial<Competency>) => api.post<Competency>("/api/v1/education/competencies", data),
  updateCompetency: (id: string, data: Partial<Competency>) => api.put<Competency>(`/api/v1/education/competencies/${id}`, data),
  deleteCompetency: (id: string) => api.delete<void>(`/api/v1/education/competencies/${id}`),
  getStudentCompetencies: (studentId: string) => api.get<CompetencyRecord[]>(`/api/v1/education/competencies/student/${studentId}`),
  updateStudentCompetency: (studentId: string, competencyId: string, data: { status: string; evidence?: string }) =>
    api.put<CompetencyRecord>(`/api/v1/education/competencies/student/${studentId}/competency/${competencyId}`, data),
  getCompetencySummary: (studentId: string) => api.get<CompetencySummary>(`/api/v1/education/competencies/student/${studentId}/summary`),
  linkAssessment: (competencyId: string, data: { assessmentId: string; weight?: number }) =>
    api.post<void>(`/api/v1/education/competencies/${competencyId}/assessments`, data),
  getCompetenciesForSubject: (subjectId: string) => api.get<Competency[]>(`/api/v1/education/competencies/subject/${subjectId}`),

  // Projects
  listProjects: () => api.get<Project[]>("/api/v1/education/projects"),
  getProject: (id: string) => api.get<Project>(`/api/v1/education/projects/${id}`),
  createProject: (data: Partial<Project>) => api.post<Project>("/api/v1/education/projects", data),
  updateProject: (id: string, data: Partial<Project>) => api.put<Project>(`/api/v1/education/projects/${id}`, data),
  deleteProject: (id: string) => api.delete<void>(`/api/v1/education/projects/${id}`),
  getStudentProjects: (studentId: string) => api.get<Project[]>(`/api/v1/education/projects/student/${studentId}`),
  addMilestone: (projectId: string, data: Partial<ProjectMilestone>) =>
    api.post<ProjectMilestone>(`/api/v1/education/projects/${projectId}/milestones`, data),
  addSubmission: (projectId: string, data: Partial<ProjectSubmission>) =>
    api.post<ProjectSubmission>(`/api/v1/education/projects/${projectId}/submissions`, data),

  // Fieldwork
  listFieldwork: () => api.get<FieldworkPlacement[]>("/api/v1/education/fieldwork"),
  getFieldwork: (id: string) => api.get<FieldworkPlacement>(`/api/v1/education/fieldwork/${id}`),
  createFieldwork: (data: Partial<FieldworkPlacement>) => api.post<FieldworkPlacement>("/api/v1/education/fieldwork", data),
  updateFieldwork: (id: string, data: Partial<FieldworkPlacement>) => api.put<FieldworkPlacement>(`/api/v1/education/fieldwork/${id}`, data),
  deleteFieldwork: (id: string) => api.delete<void>(`/api/v1/education/fieldwork/${id}`),
  getStudentFieldwork: (studentId: string) => api.get<FieldworkPlacement[]>(`/api/v1/education/fieldwork/student/${studentId}`),
  addLogbookEntry: (placementId: string, data: Partial<LogbookEntry>) =>
    api.post<LogbookEntry>(`/api/v1/education/fieldwork/${placementId}/logbook`, data),
  approveLogbookEntry: (entryId: string) =>
    api.put<LogbookEntry>(`/api/v1/education/fieldwork/logbook/${entryId}/approve`),

  // Portfolios
  listPortfolios: () => api.get<any[]>("/api/v1/education/portfolios"),
  getPortfolio: (id: string) => api.get<any>(`/api/v1/education/portfolios/${id}`),
  getStudentPortfolio: (studentId: string) => api.get<any>(`/api/v1/education/portfolios/student/${studentId}`),
  createPortfolio: (data: any) => api.post<any>("/api/v1/education/portfolios", data),
  updatePortfolio: (id: string, data: any) => api.put<any>(`/api/v1/education/portfolios/${id}`, data),
  deletePortfolio: (id: string) => api.delete<void>(`/api/v1/education/portfolios/${id}`),
  addPortfolioItem: (portfolioId: string, data: any) => api.post<any>(`/api/v1/education/portfolios/${portfolioId}/items`, data),
  updatePortfolioItem: (itemId: string, data: any) => api.put<any>(`/api/v1/education/portfolios/items/${itemId}`, data),
  deletePortfolioItem: (itemId: string) => api.delete<void>(`/api/v1/education/portfolios/items/${itemId}`),

  // Demonstrations
  listDemonstrations: () => api.get<any[]>("/api/v1/education/demonstrations"),
  getDemonstration: (id: string) => api.get<any>(`/api/v1/education/demonstrations/${id}`),
  createDemonstration: (data: any) => api.post<any>("/api/v1/education/demonstrations", data),
  updateDemonstration: (id: string, data: any) => api.put<any>(`/api/v1/education/demonstrations/${id}`, data),
  deleteDemonstration: (id: string) => api.delete<void>(`/api/v1/education/demonstrations/${id}`),
  submitDemonstration: (id: string) => api.post<any>(`/api/v1/education/demonstrations/${id}/submit`),
  reviewDemonstration: (id: string, data: any) => api.put<any>(`/api/v1/education/demonstrations/${id}/review`, data),
  getStudentDemonstrations: (studentId: string) => api.get<any[]>(`/api/v1/education/demonstrations/student/${studentId}`),

  // Research
  listResearch: () => api.get<ResearchProject[]>("/api/v1/education/research"),
  getResearch: (id: string) => api.get<ResearchProject>(`/api/v1/education/research/${id}`),
  createResearch: (data: Partial<ResearchProject>) => api.post<ResearchProject>("/api/v1/education/research", data),
  updateResearch: (id: string, data: Partial<ResearchProject>) => api.put<ResearchProject>(`/api/v1/education/research/${id}`, data),
  deleteResearch: (id: string) => api.delete<void>(`/api/v1/education/research/${id}`),
  getStudentResearch: (studentId: string) => api.get<ResearchProject[]>(`/api/v1/education/research/student/${studentId}`),
  addResearchMilestone: (researchId: string, data: Partial<ResearchMilestone>) => api.post<ResearchMilestone>(`/api/v1/education/research/${researchId}/milestones`, data),
  addResearchResource: (researchId: string, data: Partial<ResearchResource>) => api.post<ResearchResource>(`/api/v1/education/research/${researchId}/resources`, data),

  // Theses
  listTheses: () => api.get<Thesis[]>("/api/v1/education/theses"),
  getThesis: (id: string) => api.get<Thesis>(`/api/v1/education/theses/${id}`),
  createThesis: (data: Partial<Thesis>) => api.post<Thesis>("/api/v1/education/theses", data),
  updateThesis: (id: string, data: Partial<Thesis>) => api.put<Thesis>(`/api/v1/education/theses/${id}`, data),
  getStudentTheses: (studentId: string) => api.get<Thesis[]>(`/api/v1/education/theses/student/${studentId}`),

  // Study Tasks
  listStudyTasks: () => api.get<StudyTask[]>("/api/v1/education/study-tasks"),
  getStudyTask: (id: string) => api.get<StudyTask>(`/api/v1/education/study-tasks/${id}`),
  createStudyTask: (data: Partial<StudyTask>) => api.post<StudyTask>("/api/v1/education/study-tasks", data),
  updateStudyTask: (id: string, data: Partial<StudyTask>) => api.put<StudyTask>(`/api/v1/education/study-tasks/${id}`, data),
  deleteStudyTask: (id: string) => api.delete<void>(`/api/v1/education/study-tasks/${id}`),
  getStudentTasks: (studentId: string) => api.get<StudyTask[]>(`/api/v1/education/study-tasks/student/${studentId}`),
  getTodayTasks: (studentId: string) => api.get<StudyTask[]>(`/api/v1/education/study-tasks/student/${studentId}/today`),
  getWeekTasks: (studentId: string) => api.get<StudyTask[]>(`/api/v1/education/study-tasks/student/${studentId}/week`),
  completeStudyTask: (id: string) => api.put<StudyTask>(`/api/v1/education/study-tasks/${id}/complete`),

  // Higher Education Dashboard
  getHEDashboard: (studentId: string, learningLevel: string = "COLLEGE") =>
    api.get<HigherEducationDashboard>(`/api/v1/education/higher-education/dashboard/${studentId}?learningLevel=${learningLevel}`),

  // Enrollments
  getStudentEnrollments: (studentId: string) => api.get<StudentCourseEnrollment[]>(`/api/v1/education/higher-education/enrollments/${studentId}`),
  createEnrollment: (data: Partial<StudentCourseEnrollment>) => api.post<StudentCourseEnrollment>("/api/v1/education/higher-education/enrollments", data),
  updateEnrollment: (id: string, data: Partial<StudentCourseEnrollment>) => api.put<StudentCourseEnrollment>(`/api/v1/education/higher-education/enrollments/${id}`, data),

  // Academic Records
  getAcademicRecord: (studentId: string) => api.get<AcademicRecord>(`/api/v1/education/higher-education/academic-record/${studentId}`),
  getAcademicRecordHistory: (studentId: string) => api.get<AcademicRecord[]>(`/api/v1/education/higher-education/academic-record/${studentId}/history`),
  createAcademicRecord: (data: Partial<AcademicRecord>) => api.post<AcademicRecord>("/api/v1/education/higher-education/academic-record", data),
  updateAcademicRecord: (id: string, data: Partial<AcademicRecord>) => api.put<AcademicRecord>(`/api/v1/education/higher-education/academic-record/${id}`, data),

  // Career Profile
  getCareerProfile: (studentId: string) => api.get<CareerProfile>(`/api/v1/education/higher-education/career-profile/${studentId}`),
  upsertCareerProfile: (studentId: string, data: Partial<CareerProfile>) => api.post<CareerProfile>(`/api/v1/education/higher-education/career-profile/${studentId}`, data),

  // Professional Development
  getLearnerProfessionalDev: (studentId: string) => api.get<any[]>(`/api/v1/education/professional-dev/student/${studentId}`),
  createProfessionalDev: (data: any) => api.post<any>("/api/v1/education/professional-dev", data),
  updateProfessionalDev: (id: string, data: any) => api.put<any>(`/api/v1/education/professional-dev/${id}`, data),
  deleteProfessionalDev: (id: string) => api.delete<void>(`/api/v1/education/professional-dev/${id}`),

  // Deep Learning Content
  getLearnerDeepContent: (studentId: string) => api.get<DeepContent[]>(`/api/v1/education/deep-learning/student/${studentId}`),
  createDeepContent: (data: DeepContentDto) => api.post<DeepContent>("/api/v1/education/deep-learning", data),
  updateDeepContent: (id: string, data: Partial<DeepContentDto>) => api.put<DeepContent>(`/api/v1/education/deep-learning/${id}`, data),
  deleteDeepContent: (id: string) => api.delete<void>(`/api/v1/education/deep-learning/${id}`),

  // Learning Modules
  getLearnerModules: (studentId: string) => api.get<any[]>(`/api/v1/college/learner/modules/student/${studentId}`),
  createModule: (data: any) => api.post<any>("/api/v1/college/learner/modules", data),
  updateModule: (id: string, data: any) => api.put<any>(`/api/v1/college/learner/modules/${id}`, data),
  deleteModule: (id: string) => api.delete<void>(`/api/v1/college/learner/modules/${id}`),

  // Collaborations
  getLearnerCollaborations: (studentId: string) => api.get<any[]>(`/api/v1/college/learner/collaborations/student/${studentId}`),
  createCollaboration: (data: any) => api.post<any>("/api/v1/college/learner/collaborations", data),
  updateCollaboration: (id: string, data: any) => api.put<any>(`/api/v1/college/learner/collaborations/${id}`, data),
  deleteCollaboration: (id: string) => api.delete<void>(`/api/v1/college/learner/collaborations/${id}`),

  // Workshops
  getLearnerWorkshops: (studentId: string) => api.get<any[]>(`/api/v1/college/learner/workshops/student/${studentId}`),
  createWorkshop: (data: any) => api.post<any>("/api/v1/college/learner/workshops", data),
  updateWorkshop: (id: string, data: any) => api.put<any>(`/api/v1/college/learner/workshops/${id}`, data),
  deleteWorkshop: (id: string) => api.delete<void>(`/api/v1/college/learner/workshops/${id}`),

  // Notifications
  getNotifications: () => api.get<any[]>(`/api/v1/notifications`),
  getUnreadCount: () => api.get<{ count: number }>(`/api/v1/notifications/unread-count`),
  markNotificationRead: (notificationId: string) => api.put<void>(`/api/v1/notifications/${notificationId}/read`),
  markAllNotificationsRead: () => api.put<void>(`/api/v1/notifications/read-all`),

  // Student Events / Calendar
  getStudentEvents: () => api.get<any[]>("/api/v1/student/events"),
  getStudentEvent: (id: string) => api.get<any>(`/api/v1/student/events/${id}`),
  getRegisteredEvents: () => api.get<any[]>("/api/v1/student/events/registered"),
  registerForEvent: (id: string) => api.post<any>(`/api/v1/student/events/${id}/register`),
  cancelEventRegistration: (id: string) => api.post<void>(`/api/v1/student/events/${id}/cancel-registration`),
  getEventMaterials: (id: string) => api.get<any[]>(`/api/v1/student/events/${id}/materials`),

  // Student Live Classes
  getStudentLiveClasses: (page = 0, size = 20) => api.get<any[]>(`/api/v1/student/live-classes?page=${page}&size=${size}`),
  getLiveNow: () => api.get<any[]>("/api/v1/student/live-classes/live-now"),
  getUpcomingLiveClasses: () => api.get<any[]>("/api/v1/student/live-classes/upcoming"),
}
