package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.dto.DataQualityResponse;
import tz.elmkusoma.administration.service.RegionalAdminService;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.oversight.dto.*;

import java.util.List;
import java.util.UUID;

/**
 * Regional Administration & Regional Education Governance Command Center API.
 *
 * <p>All jurisdiction resolution and enforcement happens server-side inside
 * {@link RegionalAdminService}: every call is scoped to the caller's
 * region/district (REGIONAL_ADMIN / DISTRICT_ADMIN) before any data is read.
 * Never trust client-supplied region/district identifiers.</p>
 */
@RestController
@RequestMapping("/v1/regional-admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('REGIONAL_ADMIN', 'DISTRICT_ADMIN')")
@Tag(name = "Regional Administration", description = "Regional education governance, oversight and operations command center")
public class RegionalAdminController {

    private final RegionalAdminService regionalAdminService;

    // ── Command Center ──

    @GetMapping("/dashboard")
    @Operation(summary = "Regional command center dashboard (KPIs, jurisdiction, alerts, verification backlog)")
    public ResponseEntity<ApiResponse<RegionalDashboardResponse>> getDashboard(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getDashboard(userId)));
    }

    @GetMapping("/attention")
    @Operation(summary = "Attention center — real items requiring action inside the caller's jurisdiction")
    public ResponseEntity<ApiResponse<List<AttentionItemResponse>>> getAttentionItems(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getAttentionItems(userId)));
    }

    @GetMapping("/pulse")
    @Operation(summary = "Live learning pulse for the jurisdiction (real counters, no synthetic series)")
    public ResponseEntity<ApiResponse<RegionalPulseResponse>> getPulse(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getPulse(userId)));
    }

    // ── Geography drill-down: Region → District → Ward → Institution ──

    @GetMapping("/regions")
    @Operation(summary = "Region(s) the caller is authorized to govern")
    public ResponseEntity<ApiResponse<List<RegionResponse>>> getRegions(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getAccessibleRegions(userId)));
    }

    @GetMapping("/regions/{regionId}")
    @Operation(summary = "Region detail with district and institution statistics")
    public ResponseEntity<ApiResponse<RegionDetailResponse>> getRegionDetail(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID regionId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getRegionDetail(userId, regionId)));
    }

    @GetMapping("/regions/{regionId}/districts")
    @Operation(summary = "Districts inside an authorized region")
    public ResponseEntity<ApiResponse<List<DistrictResponse>>> getDistrictsByRegion(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID regionId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getDistrictsByRegion(userId, regionId)));
    }

    @GetMapping("/districts/{districtId}")
    @Operation(summary = "District detail with institutions and statistics")
    public ResponseEntity<ApiResponse<DistrictDetailResponse>> getDistrictDetail(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID districtId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getDistrictDetail(userId, districtId)));
    }

    // ── Institutions & schools ──

    @GetMapping("/institutions")
    @Operation(summary = "Institutions/schools in jurisdiction (paged, searchable, filterable by district and type)")
    public ResponseEntity<ApiResponse<PageResponse<InstitutionSummary>>> getInstitutions(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID districtId,
            @RequestParam(required = false) String type) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getInstitutions(userId, page, size, search, districtId, type)));
    }

    @GetMapping("/institutions/{institutionId}")
    @Operation(summary = "Institution governance detail (identity, people, activity, verification, data quality)")
    public ResponseEntity<ApiResponse<InstitutionGovernanceResponse>> getInstitutionGovernance(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID institutionId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getInstitutionGovernance(userId, institutionId)));
    }

    // ── People ──

    @GetMapping("/learners")
    @Operation(summary = "Learners in jurisdiction (paged)")
    public ResponseEntity<ApiResponse<PageResponse<LearnerSummary>>> getLearners(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) UUID districtId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getLearners(userId, page, size, search, institutionId, districtId)));
    }

    @GetMapping("/teachers")
    @Operation(summary = "Teachers/instructors in jurisdiction (paged)")
    public ResponseEntity<ApiResponse<PageResponse<TeacherSummary>>> getTeachers(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) UUID districtId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getTeachers(userId, page, size, search, institutionId, districtId)));
    }

    @GetMapping("/education-staff")
    @Operation(summary = "Education staff (institution administrators/staff) in jurisdiction (paged)")
    public ResponseEntity<ApiResponse<PageResponse<EducationStaffSummary>>> getEducationStaff(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) UUID districtId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getEducationStaff(userId, page, size, search, institutionId, districtId)));
    }

    // ── Learning ecosystem monitoring ──

    @GetMapping("/courses")
    @Operation(summary = "Courses in jurisdiction (paged)")
    public ResponseEntity<ApiResponse<PageResponse<CourseSummary>>> getCourses(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) UUID districtId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getCourses(userId, page, size, search, institutionId, districtId)));
    }

    @GetMapping("/live-classes")
    @Operation(summary = "Live classes in jurisdiction (paged, status filter)")
    public ResponseEntity<ApiResponse<PageResponse<LiveClassSummary>>> getLiveClasses(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) UUID districtId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getLiveClasses(userId, page, size, status, institutionId, districtId)));
    }

    @GetMapping("/live-classes/{liveClassId}/observe")
    @Operation(summary = "Observer join details for a live class inside the caller's jurisdiction")
    public ResponseEntity<ApiResponse<ObserverJoinResponse>> getObserverJoinUrl(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID liveClassId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getObserverJoinUrl(userId, liveClassId)));
    }

    @GetMapping("/resources")
    @Operation(summary = "Learning resources in jurisdiction (paged)")
    public ResponseEntity<ApiResponse<PageResponse<ResourceSummary>>> getResources(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) UUID districtId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getResources(userId, page, size, search, institutionId, districtId)));
    }

    @GetMapping("/video-tutorials")
    @Operation(summary = "Video tutorials in jurisdiction (paged)")
    public ResponseEntity<ApiResponse<PageResponse<VideoTutorialSummary>>> getVideoTutorials(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID institutionId,
            @RequestParam(required = false) UUID districtId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getVideoTutorials(userId, page, size, search, institutionId, districtId)));
    }

    // ── Analytics (delegates to the existing jurisdiction-aware oversight engine) ──

    @GetMapping("/performance")
    @Operation(summary = "Academic performance analytics for the jurisdiction")
    public ResponseEntity<ApiResponse<PerformanceResponse>> getPerformance(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getPerformance(userId)));
    }

    @GetMapping("/attendance")
    @Operation(summary = "Attendance analytics for the jurisdiction")
    public ResponseEntity<ApiResponse<AttendanceResponse>> getAttendance(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getAttendance(userId)));
    }

    @GetMapping("/assessments")
    @Operation(summary = "Assessment analytics for the jurisdiction")
    public ResponseEntity<ApiResponse<AssessmentsResponse>> getAssessments(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getAssessments(userId)));
    }

    @GetMapping("/curriculum")
    @Operation(summary = "Curriculum progress for the jurisdiction")
    public ResponseEntity<ApiResponse<CurriculumResponse>> getCurriculum(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getCurriculum(userId)));
    }

    @GetMapping("/reports")
    @Operation(summary = "Report catalogue (existing reporting infrastructure)")
    public ResponseEntity<ApiResponse<List<ReportSummary>>> getReports(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getReports(userId)));
    }

    @GetMapping("/alerts")
    @Operation(summary = "Attendance/performance alerts for the jurisdiction")
    public ResponseEntity<ApiResponse<AlertsResponse>> getAlerts(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getAlerts(userId)));
    }

    // ── Governance: verification, data quality, compliance, audit ──

    @GetMapping("/verifications")
    @Operation(summary = "Verification queue for entities inside the jurisdiction (paged)")
    public ResponseEntity<ApiResponse<PageResponse<VerificationSummary>>> getVerifications(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getVerifications(userId, page, size, status)));
    }

    @GetMapping("/verifications/{verificationId}")
    @Operation(summary = "Verification detail for an in-jurisdiction entity")
    public ResponseEntity<ApiResponse<VerificationDetailResponse>> getVerificationDetail(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID verificationId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getVerificationDetail(userId, verificationId)));
    }

    @PostMapping("/verifications/{verificationId}/review")
    @Operation(summary = "Review a verification (APPROVED | REJECTED | CHANGES_REQUIRED) with jurisdiction enforcement")
    public ResponseEntity<ApiResponse<VerificationDetailResponse>> reviewVerification(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID verificationId,
            @Valid @RequestBody VerificationReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Verification reviewed",
                regionalAdminService.reviewVerification(userId, verificationId, request)));
    }

    @GetMapping("/data-quality")
    @Operation(summary = "Real data-quality findings for the jurisdiction (missing links, duplicates, gaps)")
    public ResponseEntity<ApiResponse<DataQualityResponse>> getDataQuality(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getDataQuality(userId)));
    }

    @GetMapping("/compliance")
    @Operation(summary = "Governance checks derived from existing audit/security/verification data (not a policy engine)")
    public ResponseEntity<ApiResponse<ComplianceResponse>> getCompliance(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getCompliance(userId)));
    }

    @GetMapping("/audit")
    @Operation(summary = "Audit activity for institutions inside the jurisdiction (paged)")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogSummary>>> getAuditLogs(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String entityType) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getAuditLogs(userId, page, size, entityType)));
    }

    // ── Regional communication ──

    @GetMapping("/announcements")
    @Operation(summary = "Regional announcements sent from this jurisdiction (paged)")
    public ResponseEntity<ApiResponse<PageResponse<AnnouncementSummary>>> getAnnouncements(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getAnnouncements(userId, page, size)));
    }

    @PostMapping("/announcements")
    @Operation(summary = "Send a regional announcement — audience validated against jurisdiction server-side")
    public ResponseEntity<ApiResponse<AnnouncementDetailResponse>> createAnnouncement(
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody CreateAnnouncementRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Announcement sent",
                regionalAdminService.createAnnouncement(userId, request)));
    }

    @GetMapping("/notifications")
    @Operation(summary = "The caller's notifications (existing notification infrastructure)")
    public ResponseEntity<ApiResponse<PageResponse<NotificationSummary>>> getNotifications(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getNotifications(userId, page, size)));
    }

    @GetMapping("/notifications/unread-count")
    @Operation(summary = "Real unread notification count for the caller")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> getUnreadNotificationCount(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of("count", regionalAdminService.getUnreadNotificationCount(userId))));
    }

    @PutMapping("/notifications/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID notificationId) {
        regionalAdminService.markNotificationRead(userId, notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", null));
    }

    // ── Utilities ──

    @GetMapping("/quick-actions")
    @Operation(summary = "Quick actions available to the caller (all backed by real routes)")
    public ResponseEntity<ApiResponse<List<QuickActionResponse>>> getQuickActions(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.getQuickActions(userId)));
    }

    @GetMapping("/search")
    @Operation(summary = "Jurisdiction-aware search (districts, institutions, teachers, learners)")
    public ResponseEntity<ApiResponse<List<SearchResultResponse>>> search(
            @RequestAttribute("userId") UUID userId,
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(regionalAdminService.search(userId, q, limit)));
    }

    // ── Ward dimension (PROMPT §23) ──

    @GetMapping("/wards")
    @Operation(summary = "Wards inside the caller's jurisdiction (district-admin sees own district only)")
    public ResponseEntity<ApiResponse<PageResponse<WardSummary>>> getWards(
            @RequestAttribute("userId") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID districtId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getWards(userId, page, size, search, districtId)));
    }

    @GetMapping("/wards/{wardId}")
    @Operation(summary = "Ward detail with its institutions")
    public ResponseEntity<ApiResponse<WardDetailResponse>> getWardDetail(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID wardId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getWardDetail(userId, wardId)));
    }

    // ── Scheduled reports (PROMPT §45) ──

    @GetMapping("/scheduled-reports")
    @Operation(summary = "Scheduled reports owned by the caller inside their jurisdiction")
    public ResponseEntity<ApiResponse<List<ScheduledReportSummary>>> getScheduledReports(
            @RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getScheduledReports(userId)));
    }

    @PostMapping("/scheduled-reports")
    @Operation(summary = "Create a scheduled report (DAILY/WEEKLY/MONTHLY, bound to caller jurisdiction)")
    public ResponseEntity<ApiResponse<ScheduledReportSummary>> createScheduledReport(
            @RequestAttribute("userId") UUID userId,
            @Valid @RequestBody CreateScheduledReportRequest request) {
        ScheduledReportSummary created =
                regionalAdminService.createScheduledReport(userId, request);
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Scheduled report created", created));
    }

    @PutMapping("/scheduled-reports/{reportId}")
    @Operation(summary = "Update title/status/frequency/recipients of an owned scheduled report")
    public ResponseEntity<ApiResponse<ScheduledReportSummary>> updateScheduledReport(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID reportId,
            @Valid @RequestBody UpdateScheduledReportRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Scheduled report updated",
                regionalAdminService.updateScheduledReport(userId, reportId, request)));
    }

    @DeleteMapping("/scheduled-reports/{reportId}")
    @Operation(summary = "Delete (soft) an owned scheduled report")
    public ResponseEntity<ApiResponse<Void>> deleteScheduledReport(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID reportId) {
        regionalAdminService.deleteScheduledReport(userId, reportId);
        return ResponseEntity.ok(ApiResponse.success("Scheduled report deleted", null));
    }

    @PostMapping("/scheduled-reports/{reportId}/run")
    @Operation(summary = "Run an owned scheduled report immediately")
    public ResponseEntity<ApiResponse<ScheduledReportRunSummary>> runScheduledReportNow(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID reportId) {
        return ResponseEntity.ok(ApiResponse.success("Scheduled report executed",
                regionalAdminService.runScheduledReportNow(userId, reportId)));
    }

    @GetMapping("/scheduled-reports/{reportId}/runs")
    @Operation(summary = "Run history of an owned scheduled report")
    public ResponseEntity<ApiResponse<List<ScheduledReportRunSummary>>> getScheduledReportRuns(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID reportId) {
        return ResponseEntity.ok(ApiResponse.success(
                regionalAdminService.getScheduledReportRuns(userId, reportId)));
    }
}
