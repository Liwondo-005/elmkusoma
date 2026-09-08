import { api } from "../api-client"
import type {
  TeacherResponse,
  TeacherRequest,
  TeacherAssignmentResponse,
  TeacherAssignmentRequest,
  TeacherQualificationResponse,
  TeacherQualificationRequest,
  PageResponse,
} from "../types/api"

const BASE = "/v1/teachers"

export const teacherService = {
  list: (page = 0, size = 20) =>
    api.get<PageResponse<TeacherResponse>>(BASE, { page, size }),

  getById: (id: string) =>
    api.get<TeacherResponse>(`${BASE}/${id}`),

  create: (data: TeacherRequest) =>
    api.post<TeacherResponse>(BASE, data),

  update: (id: string, data: TeacherRequest) =>
    api.put<TeacherResponse>(`${BASE}/${id}`, data),

  remove: (id: string) =>
    api.delete<void>(`${BASE}/${id}`),

  getAssignments: (id: string) =>
    api.get<TeacherAssignmentResponse[]>(`${BASE}/${id}/assignments`),

  addAssignment: (id: string, data: TeacherAssignmentRequest) =>
    api.post<TeacherAssignmentResponse>(`${BASE}/${id}/assignments`, data),

  removeAssignment: (assignmentId: string) =>
    api.delete<void>(`${BASE}/assignments/${assignmentId}`),

  getQualifications: (id: string) =>
    api.get<TeacherQualificationResponse[]>(`${BASE}/${id}/qualifications`),

  addQualification: (id: string, data: TeacherQualificationRequest) =>
    api.post<TeacherQualificationResponse>(`${BASE}/${id}/qualifications`, data),

  removeQualification: (qualificationId: string) =>
    api.delete<void>(`${BASE}/qualifications/${qualificationId}`),
}
