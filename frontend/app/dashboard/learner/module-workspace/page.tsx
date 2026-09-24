"use client";

import { useState, useEffect, useCallback } from "react";
import { useTranslations } from "next-intl";
import { useAuth } from "@/lib/auth";
import { collegeApi } from "@/lib/college-api";
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared";
import { AlertCircle, BookOpen, Clock, Award, CheckCircle, TrendingUp } from "lucide-react";

interface ModuleWithProgress {
  id: string;
  moduleTitle?: string;
  moduleCode?: string;
  description?: string;
  creditHours?: number;
  semester?: string;
  status?: string;
  grade?: string;
  progressPercent: number;
  completedLessons: number;
  totalLessons: number;
  completedAssignments: number;
  totalAssignments: number;
  completedAssessments: number;
  totalAssessments: number;
  [key: string]: any;
}

export default function ModuleWorkspacePage() {
  const t = useTranslations("highered");
  const tc = useTranslations("common");
  const { user } = useAuth();
  const [modules, setModules] = useState<ModuleWithProgress[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedModule, setSelectedModule] = useState<ModuleWithProgress | null>(null);

  const loadModules = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await collegeApi.getLearnerModules(user?.id || "");
      const data = res.data || [];
      const mapped = (data || []).map((m: any) => ({
        ...m,
        progressPercent: m.progressPercent || 0,
        completedLessons: m.completedLessons || 0,
        totalLessons: m.totalLessons || 0,
        completedAssignments: m.completedAssignments || 0,
        totalAssignments: m.totalAssignments || 0,
        completedAssessments: m.completedAssessments || 0,
        totalAssessments: m.totalAssessments || 0,
      }));
      setModules(mapped);
    } catch (err: any) {
      setError(err?.message || tc("error.generic"));
    } finally {
      setLoading(false);
    }
  }, [user?.id]);

  useEffect(() => { loadModules(); }, [loadModules]);

  if (loading) return <div role="main" aria-busy="true"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>;
  if (error) return (
    <div role="main" className="space-y-6">
      <LearnerHeader firstName={user?.firstName || "Learner"} subtitle={t("subtitle.moduleWorkspace")} />
      <div className="rounded-2xl border border-border bg-card p-5 shadow-xs">
        <div className="flex items-center gap-3 text-destructive">
          <AlertCircle className="h-5 w-5" />
          <p>{error}</p>
          <button onClick={loadModules} aria-label={tc("retry")} className="ml-auto text-sm underline">{tc("retry")}</button>
        </div>
      </div>
    </div>
  );

  const firstName = user?.firstName || user?.name?.split(" ")[0] || "Learner";
  const activeModules = modules.filter(m => m.status === "IN_PROGRESS" || m.status === "NOT_STARTED");
  const completedModules = modules.filter(m => m.status === "COMPLETED");

  return (
    <div role="main" className="space-y-6">
      <LearnerHeader firstName={firstName} subtitle={t("subtitle.moduleWorkspace")} />

      {modules.length === 0 ? (
        <EmptyState title={t("empty.noModules")} description={t("empty.noModules")} />
      ) : (
        <>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
              <div className="flex items-center gap-2 text-muted-foreground text-sm"><BookOpen className="h-4 w-4" /> {t("stats.total")}</div>
              <p className="text-2xl font-bold mt-1">{modules.length}</p>
            </div>
            <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
              <div className="flex items-center gap-2 text-muted-foreground text-sm"><Clock className="h-4 w-4" /> {t("stats.inProgress")}</div>
              <p className="text-2xl font-bold mt-1 text-blue-600">{activeModules.length}</p>
            </div>
            <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
              <div className="flex items-center gap-2 text-muted-foreground text-sm"><CheckCircle className="h-4 w-4" /> {t("stats.completed")}</div>
              <p className="text-2xl font-bold mt-1 text-green-600">{completedModules.length}</p>
            </div>
            <div className="rounded-2xl border border-border bg-card p-4 shadow-xs">
              <div className="flex items-center gap-2 text-muted-foreground text-sm"><Award className="h-4 w-4" /> {t("progress.overall")}</div>
              <p className="text-2xl font-bold mt-1">{modules.length > 0 ? Math.round(modules.reduce((a, m) => a + m.progressPercent, 0) / modules.length) : 0}%</p>
            </div>
          </div>

          <div className="space-y-3">
            <h3 className="text-lg font-semibold">{t("stats.inProgress")}</h3>
            {activeModules.length === 0 ? (
              <p className="text-muted-foreground text-sm">{t("filters.inProgress")}</p>
            ) : activeModules.map(m => (
              <div key={m.id} className="rounded-2xl border border-border bg-card p-5 shadow-xs cursor-pointer hover:border-primary/30 transition-colors" onClick={() => setSelectedModule(selectedModule?.id === m.id ? null : m)}>
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <h4 className="font-medium">{m.moduleTitle}</h4>
                    <p className="text-sm text-muted-foreground mt-1">{m.moduleCode} · {m.creditHours || 0} {t("credit").toLowerCase()} · {m.semester || "N/A"}</p>
                    {m.description && <p className="text-sm text-muted-foreground mt-1 line-clamp-2">{m.description}</p>}
                  </div>
                  <span className={`text-xs px-2 py-1 rounded-full ${m.status === "COMPLETED" ? "bg-green-100 text-green-700" : m.status === "IN_PROGRESS" ? "bg-blue-100 text-blue-700" : "bg-gray-100 text-gray-700"}`}>
                    {m.status?.replace("_", " ")}
                  </span>
                </div>

                <div className="mt-3">
                  <div className="flex items-center justify-between text-sm text-muted-foreground mb-1">
                    <span>{t("progress.overall")}</span>
                    <span>{m.progressPercent}%</span>
                  </div>
                  <div className="h-2 rounded-full bg-muted overflow-hidden" role="progressbar" aria-valuenow={m.progressPercent} aria-valuemin={0} aria-valuemax={100}>
                    <div className="h-full rounded-full bg-primary transition-all" style={{ width: `${m.progressPercent}%` }} />
                  </div>
                </div>

                {selectedModule?.id === m.id && (
                  <div className="mt-4 grid grid-cols-3 gap-3 text-sm">
                    <div className="rounded-lg bg-muted/50 p-3">
                      <p className="text-muted-foreground">{t("courses")}</p>
                      <p className="font-medium">{m.completedLessons}/{m.totalLessons}</p>
                    </div>
                    <div className="rounded-lg bg-muted/50 p-3">
                      <p className="text-muted-foreground">{t("assessment")}</p>
                      <p className="font-medium">{m.completedAssignments}/{m.totalAssignments}</p>
                    </div>
                    <div className="rounded-lg bg-muted/50 p-3">
                      <p className="text-muted-foreground">{t("assessment")}</p>
                      <p className="font-medium">{m.completedAssessments}/{m.totalAssessments}</p>
                    </div>
                    {m.grade && (
                      <div className="rounded-lg bg-primary/10 p-3">
                        <p className="text-muted-foreground">{t("grade")}</p>
                        <p className="font-bold text-primary">{m.grade}</p>
                      </div>
                    )}
                  </div>
                )}
              </div>
            ))}
          </div>

          {completedModules.length > 0 && (
            <div className="space-y-3">
              <h3 className="text-lg font-semibold">{t("stats.completed")}</h3>
              {completedModules.map(m => (
                <div key={m.id} className="rounded-2xl border border-border bg-card p-4 shadow-xs opacity-80">
                  <div className="flex items-center justify-between">
                    <div>
                      <h4 className="font-medium">{m.moduleTitle}</h4>
                      <p className="text-sm text-muted-foreground">{m.moduleCode} · {m.creditHours || 0} {t("credit").toLowerCase()}</p>
                    </div>
                    <div className="text-right">
                      <span className="text-xs px-2 py-1 rounded-full bg-green-100 text-green-700">{t("stats.completed")}</span>
                      {m.grade && <p className="text-lg font-bold mt-1">{m.grade}</p>}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
