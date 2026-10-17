import { api } from "@/lib/api-client"

/**
 * Bundle catalog and administration.
 *
 * Reuses the shared `api` client rather than adding a second fetch path, so headers, error
 * handling and the ApiResponse envelope behave exactly as they do everywhere else.
 *
 * Purchase deliberately does NOT live here. A bundle is bought through the ordinary payment
 * lifecycle - price quote, initiate, mobile money, poll - so a bundle purchase is not a second
 * kind of checkout with its own rules.
 */

export type Bundle = {
  id: string
  institutionId: string | null
  name: string
  description: string | null
  serviceType: string
  accessDays: number | null
  status: "DRAFT" | "PUBLISHED" | "SUSPENDED"
}

export type BundleItem = {
  id: string
  bundleId: string
  serviceType: string
  serviceId: string
  position: number
}

export type BundleDetail = {
  bundle: Bundle
  items: BundleItem[]
}

export type BundleItemRequest = { serviceType: string; serviceId: string }
export type BundleRequest = {
  name: string
  description?: string
  serviceType: string
  accessDays?: number | null
  items: BundleItemRequest[]
}

/**
 * The catalog returns only published bundles, so a draft or suspended one is not merely hidden -
 * it does not exist as far as a learner is concerned. Do not add a client-side filter for status
 * on a list endpoint that already enforces it; that would only hide a bug rather than fix it.
 */
export async function listBundles(): Promise<Bundle[]> {
  return (await api.get<Bundle[]>("/v1/bundles")).data ?? []
}

export async function getBundle(bundleId: string): Promise<BundleDetail> {
  const response = await api.get<BundleDetail>(`/v1/bundles/${bundleId}`)
  if (!response.data) {
    throw new Error("Bundle not found")
  }
  return response.data
}

/** Administration. Every call is institution-scoped server-side; no institution id is sent. */
export async function listInstitutionBundles(): Promise<Bundle[]> {
  return (await api.get<Bundle[]>("/v1/institution/bundles")).data ?? []
}

export async function createBundle(body: BundleRequest): Promise<Bundle> {
  const response = await api.post<Bundle>("/v1/institution/bundles", body)
  if (!response.data) throw new Error("Bundle was not created")
  return response.data
}

export async function updateBundle(bundleId: string, body: BundleRequest): Promise<Bundle> {
  const response = await api.put<Bundle>(`/v1/institution/bundles/${bundleId}`, body)
  if (!response.data) throw new Error("Bundle was not saved")
  return response.data
}

export async function publishBundle(bundleId: string): Promise<Bundle> {
  const response = await api.post<Bundle>(`/v1/institution/bundles/${bundleId}/publish`)
  if (!response.data) throw new Error("Bundle was not published")
  return response.data
}

export async function suspendBundle(bundleId: string): Promise<Bundle> {
  const response = await api.post<Bundle>(`/v1/institution/bundles/${bundleId}/suspend`)
  if (!response.data) throw new Error("Bundle was not suspended")
  return response.data
}