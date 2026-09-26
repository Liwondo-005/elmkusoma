"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/lib/auth"
import { useTranslations } from "next-intl";
import { collegeApi } from "@/lib/college-api";
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared";

interface LearningModule {
  id: string;
  moduleTitle: string;
  moduleCode: string;
  description: string;
  creditHours: number;
  status: string;
  progressPercent: number;
  grade: string;
  totalLessons: number;
  completedLessons: number;
  totalAssignments: number;
  completedAssignments: number;
  totalAssessments: number;
  completedAssessments: number;
  courseId: string;
  semester: string;
  academicYear: string;
}

export default function ModuleWorkspacePage() {
  const t = useTranslations("learner")
  const tc = useTranslations("common")
  const { user } = useAuth();
  const [modules, setModules] = useState<LearningModule[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedModule, setSelectedModule] = useState<LearningModule | null>(null);

  useEffect(() => {
    if (!user) return;
    loadModules();
  }, [user]);

  async function loadModules() {
    if (!user) return;
    try {
      setLoading(true);
      setError(null);
      const res = await collegeApi.getLearnerModules(user.id);
      setModules(res.data || []);
    } catch (err: any) {
      console.error("Failed to load modules:", err);
      setError(t("mods.loadError"));
    } finally {
      setLoading(false);
    }
  }

  function statusColor(status: string) {
    switch (status) {
      case "COMPLETED": return "text-emerald-700 bg-emerald-50";
      case "IN_PROGRESS": return "text-blue-700 bg-blue-50";
      case "DROPPED": return "text-red-700 bg-red-50";
      default: return "text-muted-foreground bg-muted";
    }
  }

  if (loading) return <LoadingState />;

  return (
    <div className="space-y-6">
      <LearnerHeader
        firstName={user?.name || t("mods.learnerFallback")}
        subtitle={t("mods.subtitle")}
      />

      {error && (
        <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {error}
          <button onClick={loadModules} className="ml-2 underline">{tc("retry")}</button>
        </div>
      )}

      {modules.length === 0 && !error ? (
        <EmptyState
          title={t("mods.emptyTitle")}
          description={t("mods.emptyDesc")}
          icon="📚"
        />
      ) : !selectedModule ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {modules.map((m) => (
            <button
              key={m.id}
              onClick={() => setSelectedModule(m)}
              className="rounded-2xl border border-border bg-card p-5 shadow-xs text-left hover:shadow-md transition-shadow"
            >
              <div className="flex items-start justify-between mb-3">
                <div>
                  <h3 className="font-semibold text-foreground text-sm">{m.moduleTitle}</h3>
                  <p className="text-xs text-muted-foreground mt-0.5">{m.moduleCode} · {t("mods.creditsCount", { count: m.creditHours })}</p>
                </div>
                <span className={`text-[10px] px-2 py-0.5 rounded-full font-medium ${statusColor(m.status)}`}>
                  {m.status.replace(/_/g, " ")}
                </span>
              </div>

              <div className="space-y-2">
                <div>
                  <div className="flex justify-between text-xs text-muted-foreground mb-1">
                    <span>{t("mods.progressLabel")}</span><span>{m.progressPercent}%</span>
                  </div>
                  <div className="h-1.5 bg-muted rounded-full overflow-hidden">
                    <div className="h-full bg-primary rounded-full" style={{ width: `${m.progressPercent}%` }} />
                  </div>
                </div>

                {m.grade && (
                  <div className="flex items-center gap-2 text-xs">
                    <span className="text-muted-foreground">{t("mods.gradeLabel")}</span>
                    <span className="font-bold text-foreground">{m.grade}</span>
                  </div>
                )}

                <div className="grid grid-cols-3 gap-2 text-center text-[10px] text-muted-foreground">
                  <div>
                    <div className="font-semibold text-foreground">{m.completedLessons}/{m.totalLessons || 0}</div>
                    {t("mods.lessonsLabel")}
                  </div>
                  <div>
                    <div className="font-semibold text-foreground">{m.completedAssignments}/{m.totalAssignments || 0}</div>
                    {t("mods.assignmentsLabel")}
                  </div>
                  <div>
                    <div className="font-semibold text-foreground">{m.completedAssessments}/{m.totalAssessments || 0}</div>
                    {t("mods.assessmentsLabel")}
                  </div>
                </div>
              </div>
            </button>
          ))}
        </div>
      ) : (
        <div className="space-y-6">
          <button onClick={() => setSelectedModule(null)} className="text-sm text-primary hover:underline">{t("mods.backLink")}</button>

          <div className="rounded-2xl border border-border bg-card p-6 shadow-xs">
            <div className="flex items-start justify-between mb-4">
              <div>
                <h2 className="text-xl font-bold text-foreground">{selectedModule.moduleTitle}</h2>
                <p className="text-sm text-muted-foreground mt-1">{selectedModule.moduleCode} · {t("mods.creditsCount", { count: selectedModule.creditHours })} · {selectedModule.semester} {selectedModule.academicYear}</p>
              </div>
              <span className={`text-xs px-3 py-1 rounded-full font-medium ${statusColor(selectedModule.status)}`}>
                {selectedModule.status.replace(/_/g, " ")}
              </span>
            </div>

            {selectedModule.description && (
              <p className="text-sm text-muted-foreground mb-6">{selectedModule.description}</p>
            )}

            <div className="mb-6">
              <div className="flex justify-between text-sm text-muted-foreground mb-2">
                <span>{t("mods.overallProgress")}</span><span className="font-semibold text-foreground">{selectedModule.progressPercent}%</span>
              </div>
              <div className="h-3 bg-muted rounded-full overflow-hidden">
                <div className="h-full bg-primary rounded-full transition-all" style={{ width: `${selectedModule.progressPercent}%` }} />
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              {[
                { label: t("mods.lessonsLabel"), done: selectedModule.completedLessons, total: selectedModule.totalLessons, icon: "📖" },
                { label: t("mods.assignmentsLabel"), done: selectedModule.completedAssignments, total: selectedModule.totalAssignments, icon: "📝" },
                { label: t("mods.assessmentsLabel"), done: selectedModule.completedAssessments, total: selectedModule.totalAssessments, icon: "📊" },
              ].map((item) => (
                <div key={item.label} className="rounded-xl border border-border bg-muted/50 p-4 text-center">
                  <div className="text-2xl mb-1">{item.icon}</div>
                  <div className="text-lg font-bold text-foreground">{item.done}/{item.total || 0}</div>
                  <div className="text-xs text-muted-foreground">{item.label}</div>
                  <div className="h-1.5 bg-muted rounded-full overflow-hidden mt-2">
                    <div
                      className="h-full bg-primary rounded-full"
                      style={{ width: `${item.total ? (item.done / item.total) * 100 : 0}%` }}
                    />
                  </div>
                </div>
              ))}
            </div>

            {selectedModule.grade && (
              <div className="mt-6 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-center">
                <div className="text-xs text-emerald-600 mb-1">{t("mods.finalGrade")}</div>
                <div className="text-3xl font-bold text-emerald-800">{selectedModule.grade}</div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
