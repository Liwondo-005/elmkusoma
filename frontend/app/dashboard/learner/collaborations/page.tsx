"use client";

import { useState, useEffect, useCallback } from "react";
import { useTranslations } from "next-intl";
import { useAuth } from "@/lib/auth";
import { collegeApi } from "@/lib/college-api";
import {
  LearnerHeader,
  LoadingState,
  EmptyState,
} from "@/components/learner/shared";
import {
  AlertCircle,
  Users,
  Plus,
  BookOpen,
  Target,
  UserPlus,
  MessageSquare,
  Trash2,
} from "lucide-react";

type Collaboration = {
  id: string;
  studentId: string;
  peerStudentId?: string;
  collaborationType: string;
  title: string;
  description?: string;
  courseId?: string;
  status: string;
};

const COLLAB_TYPE_META: Record<
  string,
  { label: string; icon: React.ReactNode; color: string }
> = {
  "study-group": {
    label: "Study Group",
    icon: <Users className="h-4 w-4" />,
    color: "bg-blue-100 text-blue-700",
  },
  "peer-tutoring": {
    label: "Peer Tutoring",
    icon: <BookOpen className="h-4 w-4" />,
    color: "bg-green-100 text-green-700",
  },
  "research-team": {
    label: "Research Team",
    icon: <Target className="h-4 w-4" />,
    color: "bg-purple-100 text-purple-700",
  },
  "project-partner": {
    label: "Project Partner",
    icon: <UserPlus className="h-4 w-4" />,
    color: "bg-amber-100 text-amber-700",
  },
  "discussion-group": {
    label: "Discussion Group",
    icon: <MessageSquare className="h-4 w-4" />,
    color: "bg-pink-100 text-pink-700",
  },
  mentorship: {
    label: "Mentorship",
    icon: <Users className="h-4 w-4" />,
    color: "bg-indigo-100 text-indigo-700",
  },
};

const STATUS_STYLES: Record<string, string> = {
  active: "bg-green-100 text-green-700",
  pending: "bg-yellow-100 text-yellow-700",
  completed: "bg-gray-100 text-gray-600",
  inactive: "bg-red-100 text-red-600",
};

const NEW_COLLAB_TYPES = [
  { value: "study-group", label: "Study Group" },
  { value: "peer-tutoring", label: "Peer Tutoring" },
  { value: "research-team", label: "Research Team" },
  { value: "project-partner", label: "Project Partner" },
  { value: "discussion-group", label: "Discussion Group" },
  { value: "mentorship", label: "Mentorship" },
];

export default function CollaborationsPage() {
  const t = useTranslations("highered");
  const tc = useTranslations("common");
  const { user } = useAuth();
  const [collaborations, setCollaborations] = useState<Collaboration[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [formTitle, setFormTitle] = useState("");
  const [formType, setFormType] = useState("study-group");
  const [formDescription, setFormDescription] = useState("");

  const fetchCollaborations = useCallback(async () => {
    if (!user?.id) return;
    try {
      setLoading(true);
      setError(null);
      const res = await collegeApi.getLearnerCollaborations(user.id);
      setCollaborations(res.data || []);
    } catch (err: any) {
      setError(err?.message || tc("error.generic"));
    } finally {
      setLoading(false);
    }
  }, [user?.id]);

  useEffect(() => {
    fetchCollaborations();
  }, [fetchCollaborations]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formTitle.trim() || !user?.id) return;
    setSubmitting(true);
    try {
      await collegeApi.createCollaboration({
        studentId: user.id,
        collaborationType: formType,
        title: formTitle.trim(),
        description: formDescription.trim() || undefined,
        status: "active",
      });
      setFormTitle("");
      setFormType("study-group");
      setFormDescription("");
      setShowForm(false);
      await fetchCollaborations();
    } catch (err: any) {
      setError(err?.message || tc("error.generic"));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm(tc("confirm"))) return;
    try {
      await collegeApi.deleteCollaboration(id);
      await fetchCollaborations();
    } catch (err: any) {
      setError(err?.message || tc("error.generic"));
    }
  };

  const activeCollabs = collaborations.filter(
    (c) => c.status === "active"
  ).length;
  const studyGroups = collaborations.filter(
    (c) => c.collaborationType === "study-group"
  ).length;
  const researchTeams = collaborations.filter(
    (c) => c.collaborationType === "research-team"
  ).length;

  const grouped = collaborations.reduce<Record<string, Collaboration[]>>(
    (acc, collab) => {
      const key = collab.collaborationType || "other";
      if (!acc[key]) acc[key] = [];
      acc[key].push(collab);
      return acc;
    },
    {}
  );

  return (
    <div role="main" className="space-y-6">
      <div className="flex items-center justify-between">
        <LearnerHeader
          firstName={user?.firstName || user?.name?.split(" ")[0] || "Learner"}
          subtitle={t("subtitle.collaborations")}
        />
        <button
          onClick={() => setShowForm(!showForm)}
          aria-label={t("collaborate")}
          aria-pressed={showForm}
          className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-sm transition hover:bg-primary/90"
        >
          <Plus className="h-4 w-4" />
          {t("collaborate")}
        </button>
      </div>

      {error && (
        <div className="flex items-center gap-2 rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{error}</span>
          <button
            onClick={() => setError(null)}
            aria-label={tc("retry")}
            className="ml-auto text-xs underline"
          >
            {tc("retry")}
          </button>
        </div>
      )}

      {loading ? (
        <div aria-busy="true"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
                  <Users className="h-5 w-5" />
                </div>
                <div>
                  <p className="text-2xl font-semibold text-foreground">
                    {activeCollabs}
                  </p>
                  <p className="text-sm text-muted-foreground">{t("filters.active")}</p>
                </div>
              </div>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-100 text-blue-700">
                  <BookOpen className="h-5 w-5" />
                </div>
                <div>
                  <p className="text-2xl font-semibold text-foreground">
                    {studyGroups}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    {t("courses")}
                  </p>
                </div>
              </div>
            </div>
            <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
              <div className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-purple-100 text-purple-700">
                  <Target className="h-5 w-5" />
                </div>
                <div>
                  <p className="text-2xl font-semibold text-foreground">
                    {researchTeams}
                  </p>
                  <p className="text-sm text-muted-foreground">
                    Research Teams
                  </p>
                </div>
              </div>
            </div>
          </div>

          {showForm && (
            <form
              onSubmit={handleCreate}
              className="rounded-2xl border border-border bg-card p-5 shadow-xs"
            >
              <h3 className="mb-4 text-lg font-semibold text-foreground">
                {t("collaborate")}
              </h3>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div className="space-y-1.5">
                  <label className="text-sm font-medium text-foreground">
                    {t("courses")}
                  </label>
                  <input
                    type="text"
                    value={formTitle}
                    onChange={(e) => setFormTitle(e.target.value)}
                    required
                    placeholder="e.g. Biology Study Group"
                    aria-label={t("courses")}
                    className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
                  />
                </div>
                <div className="space-y-1.5">
                  <label className="text-sm font-medium text-foreground">
                    {t("department")}
                  </label>
                  <select
                    value={formType}
                    onChange={(e) => setFormType(e.target.value)}
                    aria-label={t("department")}
                    className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
                  >
                    {NEW_COLLAB_TYPES.map((ct) => (
                      <option key={ct.value} value={ct.value}>
                        {ct.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
              <div className="mt-4 space-y-1.5">
                <label className="text-sm font-medium text-foreground">
                  {t("description")}
                </label>
                <textarea
                  value={formDescription}
                  onChange={(e) => setFormDescription(e.target.value)}
                  rows={3}
                  placeholder="What is the goal of this collaboration?"
                  aria-label={t("description")}
                  className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-primary/30"
                />
              </div>
              <div className="mt-4 flex items-center gap-3">
                <button
                  type="submit"
                  disabled={submitting || !formTitle.trim()}
                  aria-label={tc("submit")}
                  className="inline-flex items-center gap-2 rounded-xl bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-sm transition hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50"
                >
                  {submitting ? tc("loading") : tc("submit")}
                </button>
                <button
                  type="button"
                  onClick={() => setShowForm(false)}
                  aria-label={tc("cancel")}
                  className="rounded-xl border border-border px-4 py-2 text-sm font-medium text-muted-foreground transition hover:bg-muted"
                >
                  {tc("cancel")}
                </button>
              </div>
            </form>
          )}

          {collaborations.length === 0 ? (
            <EmptyState
              icon={<Users className="h-12 w-12" />}
              title={t("empty.noCollaborations")}
              description={t("empty.noCollaborations")}
            />
          ) : (
            <div className="space-y-6">
              {(
                Object.keys(COLLAB_TYPE_META) as Array<
                  keyof typeof COLLAB_TYPE_META
                >
              )
                .filter((key) => grouped[key]?.length > 0)
                .map((key) => {
                  const meta = COLLAB_TYPE_META[key];
                  return (
                    <div key={key} className="space-y-3">
                      <div className="flex items-center gap-2">
                        <span className={`rounded-lg p-1.5 ${meta.color}`}>
                          {meta.icon}
                        </span>
                        <h3 className="text-base font-semibold text-foreground">
                          {meta.label}
                        </h3>
                        <span className="rounded-full bg-muted px-2 py-0.5 text-xs text-muted-foreground">
                          {grouped[key].length}
                        </span>
                      </div>
                      <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                        {grouped[key].map((collab) => (
                          <div
                            key={collab.id}
                            className="rounded-2xl border border-border bg-card p-5 shadow-xs"
                          >
                            <div className="flex items-start justify-between gap-2">
                              <div className="min-w-0 flex-1">
                                <h4 className="truncate font-semibold text-foreground">
                                  {collab.title}
                                </h4>
                                <div className="mt-1 flex flex-wrap items-center gap-2">
                                  <span
                                    className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${meta.color}`}
                                  >
                                    {meta.label}
                                  </span>
                                  <span
                                    className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${
                                      STATUS_STYLES[collab.status] || "bg-gray-100 text-gray-600"
                                    }`}
                                  >
                                    {collab.status.charAt(0).toUpperCase() +
                                      collab.status.slice(1)}
                                  </span>
                                </div>
                                {collab.description && (
                                  <p className="mt-2 line-clamp-2 text-sm text-muted-foreground">
                                    {collab.description}
                                  </p>
                                )}
                                {collab.peerStudentId && (
                                  <p className="mt-2 text-xs text-muted-foreground">
                                    {t("supervisor")}: {collab.peerStudentId}
                                  </p>
                                )}
                              </div>
                              <button
                                onClick={() => handleDelete(collab.id)}
                                aria-label={tc("delete")}
                                className="shrink-0 rounded-lg p-1.5 text-muted-foreground transition hover:bg-destructive/10 hover:text-destructive"
                              >
                                <Trash2 className="h-4 w-4" />
                              </button>
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  );
                })}
            </div>
          )}
        </>
      )}
    </div>
  );
}
