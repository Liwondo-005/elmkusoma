package tz.elmkusoma.administration.dto;

import java.util.Map;
import java.util.UUID;

public class DashboardResponse {

    private UUID institutionId;
    private Long totalStudents;
    private Long totalTeachers;
    private Long totalParents;
    private Long activeStudents;
    private Long certificatesIssued;
    private Long pendingImportJobs;
    private Long totalCourses;
    private Long publishedCourses;
    private Long draftCourses;
    private Long totalModules;
    private Long totalLessons;
    private Long liveClassesScheduled;
    private Map<String, Object> additionalStats;

    public UUID getInstitutionId() { return institutionId; }
    public void setInstitutionId(UUID institutionId) { this.institutionId = institutionId; }
    public Long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(Long totalStudents) { this.totalStudents = totalStudents; }
    public Long getTotalTeachers() { return totalTeachers; }
    public void setTotalTeachers(Long totalTeachers) { this.totalTeachers = totalTeachers; }
    public Long getTotalParents() { return totalParents; }
    public void setTotalParents(Long totalParents) { this.totalParents = totalParents; }
    public Long getActiveStudents() { return activeStudents; }
    public void setActiveStudents(Long activeStudents) { this.activeStudents = activeStudents; }
    public Long getCertificatesIssued() { return certificatesIssued; }
    public void setCertificatesIssued(Long certificatesIssued) { this.certificatesIssued = certificatesIssued; }
    public Long getPendingImportJobs() { return pendingImportJobs; }
    public void setPendingImportJobs(Long pendingImportJobs) { this.pendingImportJobs = pendingImportJobs; }
    public Long getTotalCourses() { return totalCourses; }
    public void setTotalCourses(Long totalCourses) { this.totalCourses = totalCourses; }
    public Long getPublishedCourses() { return publishedCourses; }
    public void setPublishedCourses(Long publishedCourses) { this.publishedCourses = publishedCourses; }
    public Long getDraftCourses() { return draftCourses; }
    public void setDraftCourses(Long draftCourses) { this.draftCourses = draftCourses; }
    public Long getTotalModules() { return totalModules; }
    public void setTotalModules(Long totalModules) { this.totalModules = totalModules; }
    public Long getTotalLessons() { return totalLessons; }
    public void setTotalLessons(Long totalLessons) { this.totalLessons = totalLessons; }
    public Long getLiveClassesScheduled() { return liveClassesScheduled; }
    public void setLiveClassesScheduled(Long liveClassesScheduled) { this.liveClassesScheduled = liveClassesScheduled; }
    public Map<String, Object> getAdditionalStats() { return additionalStats; }
    public void setAdditionalStats(Map<String, Object> additionalStats) { this.additionalStats = additionalStats; }

    public static DashboardResponse of(UUID institutionId, Long totalStudents,
                                       Long totalTeachers, Long totalParents,
                                       Long activeStudents, Long certificatesIssued,
                                       Long pendingImportJobs, Long totalCourses,
                                       Long publishedCourses, Long draftCourses,
                                       Long totalModules, Long totalLessons,
                                       Long liveClassesScheduled) {
        DashboardResponse resp = new DashboardResponse();
        resp.institutionId = institutionId;
        resp.totalStudents = totalStudents;
        resp.totalTeachers = totalTeachers;
        resp.totalParents = totalParents;
        resp.activeStudents = activeStudents;
        resp.certificatesIssued = certificatesIssued;
        resp.pendingImportJobs = pendingImportJobs;
        resp.totalCourses = totalCourses;
        resp.publishedCourses = publishedCourses;
        resp.draftCourses = draftCourses;
        resp.totalModules = totalModules;
        resp.totalLessons = totalLessons;
        resp.liveClassesScheduled = liveClassesScheduled;
        return resp;
    }
}