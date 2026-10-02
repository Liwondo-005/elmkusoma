import LearnerResourcesPage from "../../learner/resources/page"

/**
 * Secondary-stage learners (STUDENT + learning_level SECONDARY) are denied the
 * /dashboard/learner/* layout, so this route reuses the exact same learner
 * resources implementation with a secondary basePath (no duplicated logic).
 */
export default function SecondaryResourcesPage() {
  return <LearnerResourcesPage basePath="/dashboard/secondary/resources" />
}
