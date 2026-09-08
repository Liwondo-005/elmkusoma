import { api } from "../api-client"
import type {
  ParentResponse,
  ParentRequest,
  ParentStudentResponse,
  LinkStudentRequest,
  ParentNotificationPreferenceResponse,
  ParentNotificationPreferenceRequest,
  PageResponse,
} from "../types/api"

const BASE = "/v1/parents"

export const parentService = {
  list: (page = 0, size = 20) =>
    api.get<PageResponse<ParentResponse>>(BASE, { page, size }),

  getById: (id: string) =>
    api.get<ParentResponse>(`${BASE}/${id}`),

  create: (data: ParentRequest) =>
    api.post<ParentResponse>(BASE, data),

  update: (id: string, data: ParentRequest) =>
    api.put<ParentResponse>(`${BASE}/${id}`, data),

  remove: (id: string) =>
    api.delete<void>(`${BASE}/${id}`),

  getChildren: (id: string) =>
    api.get<ParentStudentResponse[]>(`${BASE}/${id}/children`),

  linkStudent: (id: string, data: LinkStudentRequest) =>
    api.post<ParentStudentResponse>(`${BASE}/${id}/link-student`, data),

  unlinkStudent: (linkId: string) =>
    api.delete<void>(`${BASE}/links/${linkId}`),

  getNotificationPreferences: (id: string) =>
    api.get<ParentNotificationPreferenceResponse>(`${BASE}/${id}/notification-preferences`),

  updateNotificationPreferences: (id: string, data: ParentNotificationPreferenceRequest) =>
    api.put<ParentNotificationPreferenceResponse>(`${BASE}/${id}/notification-preferences`, data),
}
