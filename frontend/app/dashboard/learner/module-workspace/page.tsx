"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import { useSearchParams } from "next/navigation";
import { useTranslations } from "next-intl";
import { useAuth } from "@/lib/auth";
import { collegeApi } from "@/lib/college-api";
import { LearnerHeader, LoadingState, EmptyState } from "@/components/learner/shared";
import { AlertCircle, BookOpen, Clock, Award, CheckCircle, TrendingUp, Save } from "lucide-react";

interface ModuleWithProgress {
  id: string;
  studentId?: string;
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
  const tw = useTranslations("moduleWorkspace");
  const { user, loading: authLoading } = useAuth();
  const searchParams = useSearchParams();
  const deepLinkedModuleId = searchParams.get("moduleId");
  const deepLinkHandled = useRef(false);
  const [modules, setModules] = useState<ModuleWithProgress[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [drafts, setDrafts] = useState<Record<string, string>>({});
  const [savingId, setSavingId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [savedId, setSavedId] = useState<string | null>(null);

  const loadModules = useCallback(async () => {
    if (!user?.id) return;
    try {
      setLoading(true);
      setError(null);
      const res = await collegeApi.getLearnerModules(user.id);
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

  useEffect(() => { if (user?.id) loadModules(); }, [user?.id, loadModules]);

  useEffect(() => {
    if (deepLinkHandled.current || !deepLinkedModuleId || modules.length === 0) return;
    const target = modules.find(m => m.id === deepLinkedModuleId);
    if (!target) return;
    deepLinkHandled.current = true;
    setSelectedId(target.id);
    document.getElementById(`module-card-${target.id}`)?.scrollIntoView({ behavior: "smooth", block: "center" });
  }, [deepLinkedModuleId, modules]);

  function applyUpdate(moduleId: string, updated: any) {
    setModules(prev => prev.map(m => (m.id === moduleId ? { ...m, ...(updated || {}) } : m)));
    setDrafts(prev => {
      const next = { ...prev };
      delete next[moduleId];
      return next;
    });
    setSavedId(moduleId);
    window.setTimeout(() => setSavedId(prev => (prev === moduleId ? null : prev)), 2500);
  }

  async function handleSaveProgress(m: ModuleWithProgress) {
    if (!user?.id || savingId) return;
    const raw = drafts[m.id] ?? String(m.progressPercent);
    const value = raw.trim() === "" ? NaN : Number(raw);
    if (!Number.isFinite(value) || value < 0 || value > 100) {
      setActionError(tw("invalidPercent"));
      return;
    }
    setSavingId(m.id);
    setActionError(null);
    try {
      const res = await collegeApi.updateModuleProgress(m.id, { progressPercent: Math.round(value) });
      applyUpdate(m.id, res.data);
    } catch {
      setActionError(tc("error.update"));
    } finally {
      setSavingId(null);
    }
  }

  async function handleMarkComplete(m: ModuleWithProgress) {
    if (!user?.id || savingId) return;
    setSavingId(m.id);
    setActionError(null);
    try {
      const res = await collegeApi.updateModuleProgress(m.id, { progressPercent: 100, status: "COMPLETED" });
      applyUpdate(m.id, res.data);
    } catch {
      setActionError(tc("error.update"));
    } finally {
      setSavingId(null);
    }
  }

  function renderActions(m: ModuleWithProgress) {
    if (!user?.id || !m.studentId) return null;
    const saving = savingId === m.id;
    return (
      <div className="mt-3 flex flex-wrap items-center gap-2 border-t border-border pt-3" onClick={e => e.stopPropagation()}>
        <label className="sr-only" htmlFor={`progress-input-${m.id}`}>{tw("progressLabel")}</label>
        <input
          id={`progress-input-${m.id}`}
          type="number"
          min={0}
          max={100}
          value={drafts[m.id] ?? String(m.progressPercent)}
          onChange={e => setDrafts(prev => ({ ...prev, [m.id]: e.target.value }))}
          disabled={saving}
          aria-label={tw("progressLabel")}
          className="w-20 rounded-lg border border-border bg-background px-2 py-1.5 text-sm outline-none focus:ring-2 focus:ring-primary/20 disabled:opacity-50"
        />
        <button
          onClick={() => handleSaveProgress(m)}
          disabled={saving}
          aria-label={tw("saveProgress")}
          className="inline-flex items-center gap-1.5 rounded-lg bg-primary px-3 py-1.5 text-xs font-medium text-primary-foreground hover:bg-primary/90 transition disabled:opacity-50"
        >
          <Save className="h-3.5 w-3.5" />
          {saving ? tc("saving") : tw("saveProgress")}
        </button>
        {m.status !== "COMPLETED" && (
          <button
            onClick={() => handleMarkComplete(m)}
            disabled={saving}
            aria-label={tw("markComplete")}
            className="inline-flex items-center gap-1.5 rounded-lg border border-border px-3 py-1.5 text-xs font-medium hover:bg-muted transition disabled:opacity-50"
          >
            <CheckCircle className="h-3.5 w-3.5" />
            {tw("markComplete")}
          </button>
        )}
        {savedId === m.id && <span className="text-xs font-medium text-green-600">{tw("saved")}</span>}
      </div>
    );
  }

  if (authLoading || !user?.id || loading) return <div role="main" aria-busy="true"><span className="sr-only">{tc("loading")}</span><LoadingState /></div>;
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

      {actionError && (
        <div className="rounded-2xl border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive flex items-center gap-2">
          <AlertCircle className="h-4 w-4 shrink-0" />
          <span>{actionError}</span>
          <button onClick={() => setActionError(null)} aria-label={tc("close")} className="ml-auto text-xs underline">{tc("close")}</button>
        </div>
      )}

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
              <div key={m.id} id={`module-card-${m.id}`} className="rounded-2xl border border-border bg-card p-5 shadow-xs cursor-pointer hover:border-primary/30 transition-colors" onClick={() => setSelectedId(selectedId === m.id ? null : m.id)}>
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

                {renderActions(m)}

                {selectedId === m.id && (
                  <div className="mt-4 grid grid-cols-3 gap-3 text-sm">
                    <div className="rounded-lg bg-muted/50 p-3">
                      <p className="text-muted-foreground">{t("lessons")}</p>
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
                <div key={m.id} id={`module-card-${m.id}`} className="rounded-2xl border border-border bg-card p-4 shadow-xs opacity-80">
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
                  {renderActions(m)}
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
