import ResourceDetailPage from "../../../learner/resources/[id]/page"

/**
 * Secondary-stage route mirroring the learner resource detail page so that
 * back-links and related-resource links stay inside /dashboard/secondary.
 * Backend authorization remains the only authority for access.
 */
export default function SecondaryResourceDetailPage() {
  return <ResourceDetailPage basePath="/dashboard/secondary/resources" />
}
