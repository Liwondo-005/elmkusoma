"use client"

import { api } from "./api-client"
import type {
  Programme, Department,
  Competency, CompetencyRecord, CompetencySummary,
  Project, ProjectMilestone, ProjectSubmission,
  FieldworkPlacement, LogbookEntry,
} from "./types/college"

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
}
