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

/**
 * Service type names an institution admin may name in a bundle.
 *
 * Names only, and that is deliberate: this endpoint exists so the bundle editor can offer a
 * choice instead of a blank line, and reading the pricing policy itself - amounts, margins,
 * versions - is a platform-admin concern. Nothing here describes how a type is priced, only that
 * it currently can be.
 *
 * An empty array is not an error condition to hide. If the platform sells nothing yet, the picker
 * has no options and says so; inventing one would send the author into a server refusal.
 */
export async function listServiceTypeCatalogue(): Promise<string[]> {
  return (await api.get<string[]>("/v1/institution/bundles/catalogue/service-types")).data ?? []
}

/**
 * Administration detail for one bundle, including unpublished ones.
 *
 * The public `getBundle` deliberately 404s a draft, so the admin editor cannot use it: it would
 * have to load a bundle whose contents it cannot see. This is the institution-scoped equivalent.
 */
export async function getInstitutionBundle(bundleId: string): Promise<BundleDetail> {
  const response = await api.get<BundleDetail>(`/v1/institution/bundles/${bundleId}`)
  if (!response.data) {
    throw new Error("Bundle not found")
  }
  return response.data
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
export type TopUpLine = {
  item: BundleItem
  covered: boolean
  price: number | null
  currency: string | null
  policyKey: string | null
  policyVersion: number | null
}

/**
 * BR-03 quote for one learner.
 *
 * `covered` items are already granted and are not charged. The total is therefore what this
 * learner actually owes, which may be less than the bundle's headline price - or zero.
 */
export type TopUpQuote = {
  lines: TopUpLine[]
  total: number
  currency: string | null
  fullyCovered: boolean
  chargeableCount: number
  coveredCount: number
}

/**
 * Prices a bundle for a specific child.
 *
 * The catalog's headline price is deliberately not used for checkout. A learner who already holds
 * part of a bundle must not be charged for it again, and only the server knows which parts they
 * hold.
 */
export async function quoteBundle(bundleId: string, studentId: string): Promise<TopUpQuote> {
  const response = await api.get<TopUpQuote>(
    `/v1/bundles/${bundleId}/quote`,
    { studentId }
  )
  if (!response.data) {
    throw new Error("No price is available for this bundle yet")
  }
  return response.data
}