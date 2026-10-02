import { redirect } from "next/navigation"

// Nationaladmin.md §8 — single Command Center. The legacy thin landing for
// NATIONAL_ADMIN now resolves to /oversight, where the jurisdiction (derived
// from the caller's role, never the URL) decides the view.
export default function NationalDashboardPage() {
  redirect("/oversight")
}
