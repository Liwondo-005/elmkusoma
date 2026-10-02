import { redirect } from "next/navigation"

// Nationaladmin.md §8 — single Command Center for every governance level.
export default function RegionalDashboardPage() {
  redirect("/oversight")
}
