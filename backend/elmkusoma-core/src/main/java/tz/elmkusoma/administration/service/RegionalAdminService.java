package tz.elmkusoma.administration.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.PlatformNotification;
import tz.elmkusoma.administration.domain.ScheduledReport;
import tz.elmkusoma.administration.domain.ScheduledReportRun;
import tz.elmkusoma.administration.domain.VerificationRecord;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.dto.DataQualityResponse;
import tz.elmkusoma.administration.repository.PlatformNotificationRepository;
import tz.elmkusoma.administration.repository.ScheduledReportRepository;
import tz.elmkusoma.administration.repository.ScheduledReportRunRepository;
import tz.elmkusoma.administration.repository.VerificationRecordRepository;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.audit.repository.SecurityEventRepository;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.course.domain.Course;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.CourseRepository;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.domain.VideoTutorial;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learning.repository.VideoTutorialRepository;
import tz.elmkusoma.learner.domain.LearnerNotification;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.oversight.domain.District;
import tz.elmkusoma.oversight.domain.Region;
import tz.elmkusoma.oversight.domain.Ward;
import tz.elmkusoma.oversight.repository.WardRepository;
import tz.elmkusoma.oversight.dto.*;
import tz.elmkusoma.oversight.service.OversightService;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Regional Administration & Regional Education Governance Command Center service.
 *
 * <p>Design rules (PROMPT §§1–5, 12, 17–23, 34–40):</p>
 * <ul>
 *   <li><b>Backend-authoritative jurisdiction</b> — every public method first
 *       resolves the caller's region/district from the database and rejects any
 *       target outside it. Client-supplied ids are validated, never trusted.</li>
 *   <li><b>Reuse over duplication</b> — analytics, alerting and observer joins
 *       delegate to the existing {@link OversightService}; verification review
 *       delegates to {@link PlatformAdminService}; notifications reuse
 *       {@link NotificationService}; announcements reuse {@link PlatformNotification}.</li>
 *   <li><b>Ward dimension &amp; scheduled reports</b> — the §23 ward geography
 *       and §45 scheduled-report scheduler are now first-class: wards are scoped
 *       through their district/region like every other geography, scheduled
 *       reports are owner- and jurisdiction-bound and materialised by the shared
 *       Spring scheduler from real oversight figures (never synthetic).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RegionalAdminService {

    /** Hard cap per announcement so a single request cannot fan out unbounded writes. */
    private static final int MAX_ANNOUNCEMENT_RECIPIENTS = 5000;

    private static final String ANNOUNCEMENT_TYPE = "REGIONAL_ANNOUNCEMENT";
    private static final Set<String> SCHOOL_TYPES = Set.of(
            "NURSERY", "PRIMARY", "SECONDARY", "SCHOOL", "COMMUNITY_SCHOOL");

    private final OversightService oversightService;
    private final PlatformAdminService platformAdminService;
    private final NotificationService notificationService;

    private final tz.elmkusoma.oversight.repository.RegionRepository regionRepository;
    private final tz.elmkusoma.oversight.repository.DistrictRepository districtRepository;
    private final WardRepository wardRepository;
    private final ScheduledReportRepository scheduledReportRepository;
    private final ScheduledReportRunRepository scheduledReportRunRepository;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final CourseRepository courseRepository;
    private final LiveClassRepository liveClassRepository;
    private final ResourceRepository resourceRepository;
    private final VideoTutorialRepository videoTutorialRepository;
    private final VerificationRecordRepository verificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final SecurityEventRepository securityEventRepository;
    private final PlatformNotificationRepository platformNotificationRepository;
    private final LearnerNotificationRepository learnerNotificationRepository;
    private final tz.elmkusoma.academic.repository.SubjectRepository subjectRepository;
    private final ObjectMapper objectMapper;

    // ────────────────────────────────────────────────────────────────────────
    // Jurisdiction resolution (server-side, every call)
    // ────────────────────────────────────────────────────────────────────────

    /** Immutable, pre-resolved jurisdiction of the calling user. */
    private record Scope(UUID userId, User user, String role, UUID regionId, UUID districtId,
                         Region region, District district,
                         List<UUID> institutionIds, List<Institution> institutions) {
        Set<UUID> institutionIdSet() {
            return new HashSet<>(institutionIds);
        }
    }

    private Scope resolveScope(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ForbiddenException("account", "use"));
        String role = user.getRole() != null ? user.getRole().name() : "";
        if (!"REGIONAL_ADMIN".equals(role) && !"DISTRICT_ADMIN".equals(role)) {
            throw new ForbiddenException("regional administration", "access");
        }
        UUID regionId = user.getRegionId();
        if (regionId == null) {
            throw new ForbiddenException("No regional jurisdiction is assigned to your account");
        }
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ForbiddenException("assigned region", "resolve"));

        UUID districtId = null;
        District district = null;
        List<Institution> institutions;
        if ("DISTRICT_ADMIN".equals(role)) {
            districtId = user.getDistrictId();
            if (districtId == null) {
                throw new ForbiddenException("No district jurisdiction is assigned to your account");
            }
            district = districtRepository.findById(districtId)
                    .orElseThrow(() -> new ForbiddenException("assigned district", "resolve"));
            if (!regionId.equals(district.getRegionId())) {
                throw new ForbiddenException("assigned district does not belong to your assigned region");
            }
            institutions = institutionRepository.findByDistrictIdAndIsDeletedFalse(districtId);
        } else {
            institutions = institutionRepository.findByRegionIdAndIsDeletedFalse(regionId);
        }
        List<UUID> ids = institutions.stream().map(Institution::getId).toList();
        return new Scope(userId, user, role, regionId, districtId, region, district, ids, institutions);
    }

    private void assertRegionInScope(Scope scope, UUID regionId) {
        if (!"REGIONAL_ADMIN".equals(scope.role())) {
            // District admins are district-scoped; sibling-district views would leak.
            throw new ForbiddenException("region", "view");
        }
        if (regionId == null || !regionId.equals(scope.regionId())) {
            throw new ForbiddenException("region", "view");
        }
    }

    private District assertDistrictInScope(Scope scope, UUID districtId) {
        District district = districtRepository.findById(districtId)
                .orElseThrow(() -> new ResourceNotFoundException("District", "id", districtId));
        if (scope.districtId() != null) {
            if (!scope.districtId().equals(districtId)) {
                throw new ForbiddenException("district", "view");
            }
        } else if (!scope.regionId().equals(district.getRegionId())) {
            throw new ForbiddenException("district", "view");
        }
        return district;
    }

    private Institution assertInstitutionInScope(Scope scope, UUID institutionId) {
        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", institutionId));
        if (scope.districtId() != null) {
            if (!scope.districtId().equals(institution.getDistrictId())) {
                throw new ForbiddenException("institution", "view");
            }
        } else if (!scope.regionId().equals(institution.getRegionId())) {
            throw new ForbiddenException("institution", "view");
        }
        return institution;
    }

    private List<UUID> targetInstitutionIds(Scope scope, UUID institutionId, UUID districtId) {
        if (institutionId != null) {
            return List.of(assertInstitutionInScope(scope, institutionId).getId());
        }
        if (districtId != null) {
            assertDistrictInScope(scope, districtId);
            return institutionRepository.findByDistrictIdAndIsDeletedFalse(districtId).stream()
                    .map(Institution::getId).toList();
        }
        return scope.institutionIds();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Command center
    // ────────────────────────────────────────────────────────────────────────

    public RegionalDashboardResponse getDashboard(UUID userId) {
        Scope scope = resolveScope(userId);
        OversightDashboardResponse d = oversightService.getDashboard(scope.regionId(), scope.districtId());

        DataQualityResponse dataQuality = computeDataQuality(scope);
        long pendingVerifications = inScopeVerifications(scope).stream()
                .filter(v -> "PENDING".equalsIgnoreCase(v.getStatus())).count();

        return RegionalDashboardResponse.builder()
                .jurisdictionSummary(d.getJurisdictionSummary())
                .totalInstitutions(nvl(d.getTotalInstitutions()))
                .totalSchools(scope.institutions().stream()
                        .filter(i -> SCHOOL_TYPES.contains(i.getType() != null ? i.getType().name() : "")).count())
                .totalDistricts(scope.districtId() != null ? 1
                        : districtRepository.countByRegionIdAndIsDeletedFalse(scope.regionId()))
                .totalTeachers(nvl(d.getTotalTeachers()))
                .totalLearners(nvl(d.getTotalStudents()))
                .totalClasses(nvl(d.getTotalClasses()))
                .totalLessons(nvl(d.getTotalLessons()))
                .totalCourses(countCourses(scope.institutionIds()))
                .activeLiveClasses(nvl(d.getActiveLiveClasses()))
                .attendanceRate(d.getAttendanceRate())
                .averagePerformance(d.getAveragePerformance())
                .curriculumProgress(d.getCurriculumProgress())
                .alertsCount(nvl(d.getAlertsCount()))
                .pendingVerifications(pendingVerifications)
                .dataQualityIssues(dataQuality.getTotalIssues())
                .topDistricts(districtSummaries(scope))
                .recentAlerts(d.getRecentAlerts())
                .quickActions(quickActions(scope.role()))
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    public List<AttentionItemResponse> getAttentionItems(UUID userId) {
        Scope scope = resolveScope(userId);
        List<AttentionItemResponse> items = new ArrayList<>();

        long pendingVerifications = inScopeVerifications(scope).stream()
                .filter(v -> "PENDING".equalsIgnoreCase(v.getStatus())).count();
        if (pendingVerifications > 0) {
            items.add(AttentionItemResponse.builder()
                    .severity(pendingVerifications > 10 ? "HIGH" : "MEDIUM")
                    .title("Pending Verifications")
                    .description(pendingVerifications + " verification request(s) in your jurisdiction await review")
                    .category("VERIFICATION")
                    .actionUrl("/dashboard/regional-admin/governance/verification")
                    .build());
        }

        DataQualityResponse dq = computeDataQuality(scope);
        if (dq.getCriticalIssues() > 0 || dq.getWarningIssues() > 0) {
            items.add(AttentionItemResponse.builder()
                    .severity(dq.getCriticalIssues() > 0 ? "HIGH" : "MEDIUM")
                    .title("Data Quality Findings")
                    .description(dq.getTotalIssues() + " finding(s): " + dq.getCriticalIssues()
                            + " critical, " + dq.getWarningIssues() + " warning")
                    .category("DATA_QUALITY")
                    .actionUrl("/dashboard/regional-admin/governance/data-quality")
                    .build());
        }

        AlertsResponse alerts = oversightService.getAlerts(scope.regionId(), scope.districtId());
        if (alerts.getAlerts() != null) {
            alerts.getAlerts().stream().limit(5).forEach(a -> items.add(AttentionItemResponse.builder()
                    .severity(a.getSeverity())
                    .title(a.getTitle())
                    .description(a.getMessage() + (a.getInstitutionName() != null
                            ? " — " + a.getInstitutionName() : ""))
                    .category("PERFORMANCE".equals(a.getType()) ? "PERFORMANCE" : "ATTENDANCE")
                    .actionUrl("/oversight/alerts")
                    .build()));
        }

        long inactive = scope.institutions().stream().filter(i -> !Boolean.TRUE.equals(i.getIsActive())).count();
        if (inactive > 0) {
            items.add(AttentionItemResponse.builder()
                    .severity("MEDIUM")
                    .title("Inactive Institutions")
                    .description(inactive + " institution(s) in your jurisdiction are inactive/suspended")
                    .category("INSTITUTION")
                    .actionUrl("/dashboard/regional-admin/institutions")
                    .build());
        }
        return items;
    }

    public RegionalPulseResponse getPulse(UUID userId) {
        Scope scope = resolveScope(userId);
        List<UUID> ids = scope.institutionIds();
        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd = weekStart.plusWeeks(1).minusNanos(1);

        boolean hasInstitutions = !ids.isEmpty();
        long liveNow = hasInstitutions
                ? liveClassRepository.countByInstitutionIdsAndStatusAndIsDeletedFalse(ids, "IN_PROGRESS") : 0;
        long scheduledToday = hasInstitutions
                ? liveClassRepository.countByInstitutionIdsAndStatusAndScheduledToday(ids, "SCHEDULED", today) : 0;
        long completedToday = hasInstitutions
                ? liveClassRepository.countByInstitutionIdsAndStatusAndCompletedToday(ids, "COMPLETED", today) : 0;
        long totalThisWeek = hasInstitutions
                ? liveClassRepository.countByInstitutionIdsAndScheduledThisWeek(ids, weekStart, weekEnd) : 0;
        long teachers = hasInstitutions
                ? membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(ids, InstitutionMembership.Role.TEACHER) : 0;
        long learners = hasInstitutions
                ? membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(ids, InstitutionMembership.Role.STUDENT)
                + membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(ids, InstitutionMembership.Role.OTHER_LEARNER) : 0;
        long totalLessons = hasInstitutions
                ? oversightService.jurisdictionInstitutionIds(scope.regionId(), scope.districtId()).size() > 0
                ? lessonCount(scope) : 0 : 0;
        long pendingVerifications = inScopeVerifications(scope).stream()
                .filter(v -> "PENDING".equalsIgnoreCase(v.getStatus())).count();
        long alerts = nvl(oversightService.getAlerts(scope.regionId(), scope.districtId()).getSummary().getTotal());

        return RegionalPulseResponse.builder()
                .liveNow(liveNow)
                .scheduledToday(scheduledToday)
                .completedToday(completedToday)
                .totalThisWeek(totalThisWeek)
                .teachers(teachers)
                .learners(learners)
                .totalLessons(totalLessons)
                .publishedLessons(hasInstitutions ? publishedLessonCount(scope) : 0)
                .pendingVerifications(pendingVerifications)
                .alertsCount(alerts)
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Geography drill-down
    // ────────────────────────────────────────────────────────────────────────

    public List<RegionResponse> getAccessibleRegions(UUID userId) {
        Scope scope = resolveScope(userId);
        if (!"REGIONAL_ADMIN".equals(scope.role())) {
            // District admins are district-scoped: no region-level list.
            return List.of();
        }
        return List.of(toRegionResponse(scope));
    }

    public RegionDetailResponse getRegionDetail(UUID userId, UUID regionId) {
        Scope scope = resolveScope(userId);
        assertRegionInScope(scope, regionId);
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new ResourceNotFoundException("Region", "id", regionId));
        List<Institution> institutions = institutionRepository.findByRegionIdAndIsDeletedFalse(regionId);
        List<UUID> ids = institutions.stream().map(Institution::getId).toList();

        return RegionDetailResponse.builder()
                .id(region.getId())
                .name(region.getName())
                .code(region.getCode())
                .isActive(region.getIsActive())
                .districtCount(districtRepository.countByRegionIdAndIsDeletedFalse(regionId))
                .institutionCount((long) ids.size())
                .schoolCount(institutions.stream()
                        .filter(i -> SCHOOL_TYPES.contains(i.getType() != null ? i.getType().name() : "")).count())
                .teacherCount(countMembers(ids, InstitutionMembership.Role.TEACHER))
                .learnerCount(countMembers(ids, InstitutionMembership.Role.STUDENT))
                .attendanceRate(oversightService.jurisdictionAttendanceRate(ids))
                .averagePerformance(oversightService.jurisdictionAveragePerformance(ids))
                .curriculumProgress(oversightService.jurisdictionCurriculumProgress(ids))
                .districts(districtSummaries(scope))
                .build();
    }

    public List<DistrictResponse> getDistrictsByRegion(UUID userId, UUID regionId) {
        Scope scope = resolveScope(userId);
        assertRegionInScope(scope, regionId);
        return oversightService.getDistrictsByRegion(regionId);
    }

    public DistrictDetailResponse getDistrictDetail(UUID userId, UUID districtId) {
        Scope scope = resolveScope(userId);
        District district = assertDistrictInScope(scope, districtId);
        Region region = regionRepository.findById(district.getRegionId()).orElse(null);
        List<Institution> institutions = institutionRepository.findByDistrictIdAndIsDeletedFalse(districtId);
        List<UUID> ids = institutions.stream().map(Institution::getId).toList();

        return DistrictDetailResponse.builder()
                .id(district.getId())
                .name(district.getName())
                .code(district.getCode())
                .isActive(district.getIsActive())
                .regionId(district.getRegionId())
                .regionName(region != null ? region.getName() : null)
                .regionCode(region != null ? region.getCode() : null)
                .institutionCount((long) ids.size())
                .schoolCount(institutions.stream()
                        .filter(i -> SCHOOL_TYPES.contains(i.getType() != null ? i.getType().name() : "")).count())
                .teacherCount(countMembers(ids, InstitutionMembership.Role.TEACHER))
                .learnerCount(countMembers(ids, InstitutionMembership.Role.STUDENT))
                .attendanceRate(oversightService.jurisdictionAttendanceRate(ids))
                .averagePerformance(oversightService.jurisdictionAveragePerformance(ids))
                .curriculumProgress(oversightService.jurisdictionCurriculumProgress(ids))
                .institutions(institutions.stream().map(this::toInstitutionSummary).toList())
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Institutions & schools
    // ────────────────────────────────────────────────────────────────────────

    public PageResponse<InstitutionSummary> getInstitutions(UUID userId, int page, int size,
                                                            String search, UUID districtId, String type) {
        Scope scope = resolveScope(userId);
        List<Institution> base = districtId != null
                ? institutionRepository.findByDistrictIdAndIsDeletedFalse(assertDistrictInScope(scope, districtId).getId())
                : scope.institutions();

        String q = normalize(search);
        if (!q.isEmpty()) {
            base = base.stream()
                    .filter(i -> contains(i.getName(), q) || contains(i.getCode(), q))
                    .toList();
        }
        if (type != null && !type.isBlank()) {
            Set<String> types = Arrays.stream(type.split(","))
                    .map(String::trim).map(t -> t.toUpperCase(Locale.ROOT))
                    .filter(t -> !t.isEmpty()).collect(Collectors.toSet());
            base = base.stream()
                    .filter(i -> i.getType() != null && types.contains(i.getType().name()))
                    .toList();
        }
        base = base.stream()
                .sorted(Comparator.comparing(Institution::getName, Comparator.nullsLast(String::compareTo)))
                .toList();

        int safeSize = clampSize(size);
        int from = clampPage(page, base.size(), safeSize) * safeSize;
        List<Institution> slice = base.size() == 0 ? List.of()
                : base.subList(from, Math.min(base.size(), from + safeSize));
        return toPageResponse(
                slice.stream().map(this::toInstitutionSummary).toList(),
                from / safeSize, safeSize, base.size());
    }

    public InstitutionGovernanceResponse getInstitutionGovernance(UUID userId, UUID institutionId) {
        Scope scope = resolveScope(userId);
        Institution institution = assertInstitutionInScope(scope, institutionId);
        tz.elmkusoma.oversight.dto.InstitutionDetailResponse detail =
                oversightService.getInstitutionDetail(institutionId);

        String verificationStatus = verificationRepository
                .findByEntityTypeAndEntityIdAndIsDeletedFalse("INSTITUTION", institutionId).stream()
                .sorted(Comparator.comparing(VerificationRecord::getSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .map(VerificationRecord::getStatus)
                .orElse("UNVERIFIED");

        List<String> qualityFlags = new ArrayList<>();
        if (institution.getRegionId() == null) qualityFlags.add("Missing region link");
        if (institution.getDistrictId() == null) qualityFlags.add("Missing district link");
        if (institution.getEmail() == null || institution.getEmail().isBlank()) qualityFlags.add("Missing contact email");
        if (institution.getPhone() == null || institution.getPhone().isBlank()) qualityFlags.add("Missing contact phone");
        if (!Boolean.TRUE.equals(institution.getIsActive())) qualityFlags.add("Institution is not active");

        return InstitutionGovernanceResponse.builder()
                .id(detail.getId())
                .name(detail.getName())
                .code(detail.getCode())
                .type(detail.getType())
                .isActive(detail.getIsActive())
                .status(institution.getStatus())
                .districtId(institution.getDistrictId())
                .districtName(detail.getDistrictName())
                .regionId(institution.getRegionId())
                .regionName(detail.getRegionName())
                .teacherCount(nvl(detail.getTeacherCount()))
                .learnerCount(nvl(detail.getStudentCount()))
                .classCount(nvl(detail.getClassCount()))
                .lessonCount(nvl(detail.getLessonCount()))
                .liveClassCount(nvl(detail.getActiveLiveClasses()))
                .attendanceRate(detail.getAttendanceRate())
                .averagePerformance(detail.getAveragePerformance())
                .curriculumProgress(detail.getCurriculumProgress())
                .verificationStatus(verificationStatus)
                .dataQualityIssues(qualityFlags)
                .contactEmail(institution.getEmail())
                .contactPhone(institution.getPhone())
                .address(institution.getAddress())
                .city(institution.getCity())
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // People
    // ────────────────────────────────────────────────────────────────────────

    public PageResponse<LearnerSummary> getLearners(UUID userId, int page, int size, String search,
                                                     UUID institutionId, UUID districtId) {
        Scope scope = resolveScope(userId);
        List<User.Role> roles = List.of(User.Role.STUDENT, User.Role.OTHER_LEARNER, User.Role.LEARNER);
        return pagedUsers(scope, roles, search, institutionId, districtId, page, size).stream()
                .map(u -> toLearnerSummary(scope, u))
                .collect(Collectors.collectingAndThen(Collectors.toList(),
                        list -> toPageResponse(list, page, clampSize(size), -1)));
    }

    public PageResponse<TeacherSummary> getTeachers(UUID userId, int page, int size, String search,
                                                    UUID institutionId, UUID districtId) {
        Scope scope = resolveScope(userId);
        List<User.Role> roles = List.of(User.Role.TEACHER, User.Role.INSTRUCTOR);
        List<User> users = pagedUsers(scope, roles, search, institutionId, districtId, page, size);
        List<TeacherSummary> mapped = users.stream().map(u -> toTeacherSummary(scope, u)).toList();
        return toPageResponse(mapped, page, clampSize(size), -1);
    }

    public PageResponse<EducationStaffSummary> getEducationStaff(UUID userId, int page, int size, String search,
                                                                 UUID institutionId, UUID districtId) {
        Scope scope = resolveScope(userId);
        List<User.Role> roles = List.of(User.Role.INSTITUTION_ADMIN);
        List<User> users = pagedUsers(scope, roles, search, institutionId, districtId, page, size);
        List<EducationStaffSummary> mapped = users.stream().map(u -> EducationStaffSummary.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .email(u.getEmail())
                .institutionId(u.getInstitutionId())
                .institutionName(institutionName(scope, u.getInstitutionId()))
                .role(u.getRole() != null ? u.getRole().name() : null)
                .isActive(u.getIsActive())
                .createdAt(u.getCreatedAt())
                .build()).toList();
        return toPageResponse(mapped, page, clampSize(size), -1);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Learning ecosystem
    // ────────────────────────────────────────────────────────────────────────

    public PageResponse<CourseSummary> getCourses(UUID userId, int page, int size, String search,
                                                  UUID institutionId, UUID districtId) {
        Scope scope = resolveScope(userId);
        List<UUID> ids = targetInstitutionIds(scope, institutionId, districtId);
        if (ids.isEmpty()) return emptyPage(page, size);
        String q = normalize(search);
        Page<Course> result = q.isEmpty()
                ? courseRepository.findByInstitutionIdsAndIsDeletedFalse(ids, PageRequest.of(page, clampSize(size)))
                : courseRepository.searchByInstitutionIds(ids, q, PageRequest.of(page, clampSize(size)));
        List<CourseSummary> mapped = result.getContent().stream()
                .map(c -> CourseSummary.builder()
                        .id(c.getId()).title(c.getTitle()).level(c.getLevel()).category(c.getCategory())
                        .subject(c.getSubjectId() != null
                                ? subjectRepository.findById(c.getSubjectId()).map(s -> s.getName()).orElse(null)
                                : null)
                        .institutionId(c.getInstitutionId())
                        .institutionName(institutionName(scope, c.getInstitutionId()))
                        .isPublished(c.getIsPublished())
                        .createdAt(c.getCreatedAt())
                        .build())
                .toList();
        return toPageResponse(mapped, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public PageResponse<LiveClassSummary> getLiveClasses(UUID userId, int page, int size, String status,
                                                         UUID institutionId, UUID districtId) {
        Scope scope = resolveScope(userId);
        List<UUID> ids = targetInstitutionIds(scope, institutionId, districtId);
        if (ids.isEmpty()) return emptyPage(page, size);
        List<LiveClass> all = liveClassRepository.findByInstitutionIdsAndIsDeletedFalseOrderByScheduledAt(ids);
        if (status != null && !status.isBlank()) {
            String wanted = status.trim().toUpperCase(Locale.ROOT);
            all = all.stream().filter(lc -> wanted.equals(lc.getStatus())).toList();
        }
        int safeSize = clampSize(size);
        int pageIdx = clampPage(page, all.size(), safeSize);
        List<LiveClass> slice = all.isEmpty() ? List.of()
                : all.subList(pageIdx * safeSize, Math.min(all.size(), pageIdx * safeSize + safeSize));
        List<LiveClassSummary> mapped = slice.stream().map(lc -> LiveClassSummary.builder()
                .id(lc.getId()).title(lc.getTitle()).status(lc.getStatus())
                .scheduledAt(lc.getScheduledAt()).durationMinutes(lc.getDurationMinutes())
                .maxParticipants(lc.getMaxParticipants())
                .subjectId(lc.getSubjectId())
                .subjectName(lc.getSubjectId() != null
                        ? subjectRepository.findById(lc.getSubjectId()).map(s -> s.getName()).orElse(null) : null)
                .teacherId(lc.getTeacherId())
                .teacherName(lc.getTeacherId() != null
                        ? userRepository.findById(lc.getTeacherId()).map(User::getFullName).orElse(null) : null)
                .institutionId(lc.getInstitutionId())
                .institutionName(institutionName(scope, lc.getInstitutionId()))
                .build()).toList();
        return toPageResponse(mapped, pageIdx, safeSize, all.size());
    }

    public ObserverJoinResponse getObserverJoinUrl(UUID userId, UUID liveClassId) {
        // OversightService re-validates authority role + jurisdiction before issuing the URL.
        return oversightService.getObserverJoinUrl(userId, liveClassId);
    }

    public PageResponse<ResourceSummary> getResources(UUID userId, int page, int size, String search,
                                                      UUID institutionId, UUID districtId) {
        Scope scope = resolveScope(userId);
        List<UUID> ids = targetInstitutionIds(scope, institutionId, districtId);
        if (ids.isEmpty()) return emptyPage(page, size);
        String q = normalize(search);
        Page<Resource> result = q.isEmpty()
                ? resourceRepository.findByInstitutionIdsAndIsDeletedFalse(ids, PageRequest.of(page, clampSize(size)))
                : resourceRepository.searchByInstitutionIds(ids, q, PageRequest.of(page, clampSize(size)));
        List<ResourceSummary> mapped = result.getContent().stream()
                .map(r -> ResourceSummary.builder()
                        .id(r.getId()).title(r.getTitle())
                        .type(r.getResourceType() != null ? r.getResourceType().name() : null)
                        .mimeType(r.getMimeType()).fileSize(r.getFileSize())
                        .visibility(r.getVisibility() != null ? r.getVisibility().name() : null)
                        .institutionId(r.getInstitutionId())
                        .institutionName(institutionName(scope, r.getInstitutionId()))
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();
        return toPageResponse(mapped, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    public PageResponse<VideoTutorialSummary> getVideoTutorials(UUID userId, int page, int size, String search,
                                                                UUID institutionId, UUID districtId) {
        Scope scope = resolveScope(userId);
        List<UUID> ids = targetInstitutionIds(scope, institutionId, districtId);
        if (ids.isEmpty()) return emptyPage(page, size);
        String q = normalize(search);
        Page<VideoTutorial> result = q.isEmpty()
                ? videoTutorialRepository.findByInstitutionIdsAndIsDeletedFalse(ids, PageRequest.of(page, clampSize(size)))
                : videoTutorialRepository.searchByInstitutionIds(ids, q, PageRequest.of(page, clampSize(size)));
        List<VideoTutorialSummary> mapped = result.getContent().stream()
                .map(v -> VideoTutorialSummary.builder()
                        .id(v.getId()).title(v.getTitle())
                        .status(v.getStatus() != null ? v.getStatus().name() : null)
                        .durationSeconds(v.getDurationSeconds())
                        .recordingUrl(v.getRecordingUrl())
                        .institutionId(v.getInstitutionId())
                        .institutionName(institutionName(scope, v.getInstitutionId()))
                        .createdAt(v.getCreatedAt())
                        .build())
                .toList();
        return toPageResponse(mapped, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Analytics (delegated to the existing oversight engine)
    // ────────────────────────────────────────────────────────────────────────

    public PerformanceResponse getPerformance(UUID userId) {
        Scope scope = resolveScope(userId);
        return oversightService.getPerformance(scope.regionId(), scope.districtId());
    }

    public AttendanceResponse getAttendance(UUID userId) {
        Scope scope = resolveScope(userId);
        return oversightService.getAttendance(scope.regionId(), scope.districtId());
    }

    public AssessmentsResponse getAssessments(UUID userId) {
        Scope scope = resolveScope(userId);
        return oversightService.getAssessments(scope.regionId(), scope.districtId());
    }

    public CurriculumResponse getCurriculum(UUID userId) {
        Scope scope = resolveScope(userId);
        return oversightService.getCurriculum(scope.regionId(), scope.districtId());
    }

    public List<ReportSummary> getReports(UUID userId) {
        resolveScope(userId);
        return oversightService.getReports();
    }

    public AlertsResponse getAlerts(UUID userId) {
        Scope scope = resolveScope(userId);
        return oversightService.getAlerts(scope.regionId(), scope.districtId());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Governance: verification, data quality, compliance, audit
    // ────────────────────────────────────────────────────────────────────────

    public PageResponse<VerificationSummary> getVerifications(UUID userId, int page, int size, String status) {
        Scope scope = resolveScope(userId);
        List<VerificationRecord> all = inScopeVerifications(scope);
        if (status != null && !status.isBlank()) {
            String wanted = status.trim().toUpperCase(Locale.ROOT);
            all = all.stream().filter(v -> wanted.equalsIgnoreCase(v.getStatus())).toList();
        }
        all = all.stream()
                .sorted(Comparator.comparing(VerificationRecord::getSubmittedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int safeSize = clampSize(size);
        int pageIdx = clampPage(page, all.size(), safeSize);
        List<VerificationRecord> slice = all.isEmpty() ? List.of()
                : all.subList(pageIdx * safeSize, Math.min(all.size(), pageIdx * safeSize + safeSize));
        Map<UUID, String> names = institutionNameMap(scope);
        List<VerificationSummary> mapped = slice.stream()
                .map(v -> VerificationSummary.builder()
                        .id(v.getId())
                        .entityType(v.getEntityType())
                        .entityId(v.getEntityId())
                        .entityName(resolveEntityName(v, names))
                        .verificationType(v.getVerificationType())
                        .status(v.getStatus())
                        .submittedBy(v.getSubmittedBy())
                        .submittedAt(v.getSubmittedAt())
                        .reviewedAt(v.getReviewedAt())
                        .build())
                .toList();
        return toPageResponse(mapped, pageIdx, safeSize, all.size());
    }

    public VerificationDetailResponse getVerificationDetail(UUID userId, UUID verificationId) {
        Scope scope = resolveScope(userId);
        VerificationRecord record = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("VerificationRecord", "id", verificationId));
        assertVerificationInScope(scope, record);
        Map<UUID, String> names = institutionNameMap(scope);

        List<String> documents = List.of();
        if (record.getDocuments() != null && !record.getDocuments().isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(record.getDocuments());
                if (node.isArray()) {
                    List<String> parsed = new ArrayList<>();
                    node.forEach(el -> parsed.add(el.isTextual() ? el.asText() : el.toString()));
                    documents = parsed;
                }
            } catch (Exception ignored) {
                documents = List.of();
            }
        }
        return VerificationDetailResponse.builder()
                .id(record.getId())
                .entityType(record.getEntityType())
                .entityId(record.getEntityId())
                .entityName(resolveEntityName(record, names))
                .verificationType(record.getVerificationType())
                .status(record.getStatus())
                .submittedBy(record.getSubmittedBy() != null
                        ? userRepository.findById(record.getSubmittedBy()).map(User::getFullName)
                        .orElse(record.getSubmittedBy().toString()) : null)
                .submittedAt(record.getSubmittedAt())
                .documents(documents)
                .reviewerNotes(record.getNotes())
                .reviewedAt(record.getReviewedAt())
                .reviewedBy(record.getReviewedBy() != null
                        ? userRepository.findById(record.getReviewedBy()).map(User::getFullName)
                        .orElse(record.getReviewedBy().toString()) : null)
                .build();
    }

    @Transactional
    public VerificationDetailResponse reviewVerification(UUID userId, UUID verificationId,
                                                         VerificationReviewRequest request) {
        Scope scope = resolveScope(userId);
        VerificationRecord record = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("VerificationRecord", "id", verificationId));
        assertVerificationInScope(scope, record);

        // Reuse the existing review workflow (status validation, audit trail,
        // delegation-aware logic). REGIONAL_ADMIN/DISTRICT_ADMIN are accepted as
        // platform-authority roles there; jurisdiction was enforced above.
        platformAdminService.reviewProviderVerification(
                verificationId, userId, request.getStatus(), request.getReviewerNotes());
        return getVerificationDetail(userId, verificationId);
    }

    public DataQualityResponse getDataQuality(UUID userId) {
        return computeDataQuality(resolveScope(userId));
    }

    private DataQualityResponse computeDataQuality(Scope scope) {
        List<DataQualityIssue> issues = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (Institution inst : scope.institutions()) {
            if (inst.getDistrictId() == null) {
                issues.add(issue("MISSING_GEOGRAPHY", "HIGH",
                        "Institution has no district link",
                        inst.getName() + " is linked to a region but has no district, so it cannot be governed at district level.",
                        "INSTITUTION", inst.getId(), inst.getName(),
                        "Assign a district to this institution (Platform Admin → Institutions → Edit).", now));
            }
            if (inst.getRegionId() == null) {
                issues.add(issue("MISSING_GEOGRAPHY", "HIGH",
                        "Institution has no region link",
                        inst.getName() + " has no region link and is invisible to regional oversight.",
                        "INSTITUTION", inst.getId(), inst.getName(),
                        "Assign a region to this institution (Platform Admin → Institutions → Edit).", now));
            }
            if (inst.getWardId() == null && inst.getDistrictId() != null) {
                issues.add(issue("MISSING_GEOGRAPHY", "MEDIUM",
                        "Institution has no ward link",
                        inst.getName() + " is linked to a district but not to a ward, so ward-level governance cannot see it.",
                        "INSTITUTION", inst.getId(), inst.getName(),
                        "Assign a ward to this institution once ward-level addresses are collected.", now));
            }
            if (inst.getEmail() == null || inst.getEmail().isBlank()) {
                issues.add(issue("MISSING_CONTACT", "MEDIUM",
                        "Institution has no contact email",
                        inst.getName() + " has no contact email on record.",
                        "INSTITUTION", inst.getId(), inst.getName(),
                        "Add a contact email to the institution profile.", now));
            }
            if (!Boolean.TRUE.equals(inst.getIsActive())) {
                issues.add(issue("INACTIVE_INSTITUTION", "MEDIUM",
                        "Institution is inactive",
                        inst.getName() + " is currently " + (inst.getStatus() != null ? inst.getStatus() : "inactive") + ".",
                        "INSTITUTION", inst.getId(), inst.getName(),
                        "Review the institution lifecycle status and reactivate if appropriate.", now));
            }
        }

        if (!scope.institutionIds().isEmpty()) {
            for (String email : userRepository.findDuplicateEmailsInScope(scope.institutionIds())) {
                issues.add(issue("DUPLICATE_RECORD", "HIGH",
                        "Duplicate user email: " + email,
                        "Multiple active user accounts share the email address " + email + " inside this jurisdiction.",
                        "USER", null, email,
                        "Merge or deactivate the duplicate accounts (Platform Admin → Users).", now));
            }
        }

        long orphans = userRepository.countLearnersWithoutInstitutionInRegion(scope.regionId());
        if (orphans > 0) {
            issues.add(issue("MISSING_RELATIONSHIP", "MEDIUM",
                    orphans + " learner(s) have no institution link",
                    "Learners assigned to this region have no institution membership.",
                    "USER", null, null,
                    "Link each learner to an institution (Platform Admin → Users → Edit).", now));
        }

        long pending = inScopeVerifications(scope).stream()
                .filter(v -> "PENDING".equalsIgnoreCase(v.getStatus())).count();
        if (pending > 0) {
            issues.add(issue("PENDING_WORKFLOW", "INFO",
                    pending + " pending verification(s)",
                    "Verification requests in this jurisdiction are awaiting review.",
                    "VERIFICATION", null, null,
                    "Open the verification queue and review pending requests.", now));
        }

        long critical = issues.stream().filter(i -> "HIGH".equals(i.getSeverity())).count();
        long warning = issues.stream().filter(i -> "MEDIUM".equals(i.getSeverity())).count();
        long info = issues.stream().filter(i -> "INFO".equals(i.getSeverity())).count();
        return DataQualityResponse.builder()
                .totalIssues(issues.size())
                .criticalIssues(critical)
                .warningIssues(warning)
                .infoIssues(info)
                .issues(issues)
                .build();
    }

    public ComplianceResponse getCompliance(UUID userId) {
        Scope scope = resolveScope(userId);
        List<ComplianceCheck> checks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        List<UUID> ids = scope.institutionIds();
        boolean hasInstitutions = !ids.isEmpty();

        long missingGeoLinks = scope.institutions().stream()
                .filter(i -> i.getRegionId() == null || i.getDistrictId() == null).count();
        checks.add(ComplianceCheck.builder()
                .id("geographic-linkage").name("Geographic linkage").category("JURISDICTION")
                .passed(missingGeoLinks == 0)
                .description("Every institution in the jurisdiction has region and district links")
                .remediation(missingGeoLinks == 0 ? null
                        : missingGeoLinks + " institution(s) missing region/district links — see Data Quality")
                .lastChecked(now).build());

        Set<UUID> verifiedInstitutions = inScopeVerifications(scope).stream()
                .filter(v -> "APPROVED".equalsIgnoreCase(v.getStatus()))
                .filter(v -> "INSTITUTION".equalsIgnoreCase(v.getEntityType()))
                .map(VerificationRecord::getEntityId)
                .collect(Collectors.toSet());
        long unverified = scope.institutionIds().stream().filter(id -> !verifiedInstitutions.contains(id)).count();
        checks.add(ComplianceCheck.builder()
                .id("verification-coverage").name("Institution verification coverage").category("VERIFICATION")
                .passed(hasInstitutions && unverified == 0)
                .description("All institutions in the jurisdiction hold an approved verification")
                .remediation(unverified == 0 ? null
                        : unverified + " institution(s) without approved verification")
                .lastChecked(now).build());

        long criticalSecurity = hasInstitutions
                ? securityEventRepository.countCriticalUnresolvedByInstitutionIds(ids) : 0;
        checks.add(ComplianceCheck.builder()
                .id("critical-security-events").name("No unresolved critical security events").category("SECURITY")
                .passed(criticalSecurity == 0)
                .description("Unresolved CRITICAL security events inside the jurisdiction")
                .remediation(criticalSecurity == 0 ? null
                        : criticalSecurity + " unresolved critical security event(s)")
                .lastChecked(now).build());

        long auditTrail = hasInstitutions
                ? auditLogRepository.findByInstitutionIdsAndEntitytype(ids, null, PageRequest.of(0, 1)).getTotalElements() : 0;
        checks.add(ComplianceCheck.builder()
                .id("audit-activity").name("Audit trail present").category("AUDIT")
                .passed(auditTrail > 0)
                .description("Administrative activity inside the jurisdiction is being audited")
                .remediation(auditTrail > 0 ? null : "No audit activity recorded for this jurisdiction yet")
                .lastChecked(now).build());

        long duplicateEmails = hasInstitutions ? userRepository.findDuplicateEmailsInScope(ids).size() : 0;
        checks.add(ComplianceCheck.builder()
                .id("duplicate-identities").name("No duplicate user emails").category("DATA_INTEGRITY")
                .passed(duplicateEmails == 0)
                .description("Active users in the jurisdiction do not share email addresses")
                .remediation(duplicateEmails == 0 ? null
                        : duplicateEmails + " duplicated email address(es) — see Data Quality")
                .lastChecked(now).build());

        long failed = checks.stream().filter(c -> !Boolean.TRUE.equals(c.getPassed())).count();
        return ComplianceResponse.builder()
                .isCompliant(failed == 0)
                .totalChecks(checks.size())
                .passedChecks(checks.size() - failed)
                .failedChecks(failed)
                .checks(checks)
                .note("Derived governance checks computed from existing verification, audit, security and "
                        + "data records — the platform has no regulatory compliance engine and none is invented here.")
                .build();
    }

    public PageResponse<AuditLogSummary> getAuditLogs(UUID userId, int page, int size, String entityType) {
        Scope scope = resolveScope(userId);
        if (scope.institutionIds().isEmpty()) return emptyPage(page, size);
        String type = (entityType == null || entityType.isBlank()) ? null : entityType.trim();
        Page<AuditLog> result = auditLogRepository.findByInstitutionIdsAndEntitytype(
                scope.institutionIds(), type, PageRequest.of(page, clampSize(size)));
        Map<UUID, String> names = institutionNameMap(scope);
        List<AuditLogSummary> mapped = result.getContent().stream()
                .map(a -> AuditLogSummary.builder()
                        .id(a.getId())
                        .entityType(a.getEntityType())
                        .entityId(a.getEntityId())
                        .entityName(a.getEntityName())
                        .action(a.getAction() != null ? a.getAction().name() : null)
                        .actorName(a.getUserEmail())
                        .actorRole(a.getUserRole())
                        .institutionName(names.get(a.getInstitutionId()))
                        .timestamp(a.getCreatedAt())
                        .build())
                .toList();
        return toPageResponse(mapped, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    // ────────────────────────────────────────────────────────────────────────
    // Regional communication
    // ────────────────────────────────────────────────────────────────────────

    public PageResponse<AnnouncementSummary> getAnnouncements(UUID userId, int page, int size) {
        Scope scope = resolveScope(userId);
        String needle = audienceNeedle(scope);
        Page<PlatformNotification> result = platformNotificationRepository
                .findByNotificationTypeAndTargetAudienceContainingAndIsDeletedFalseOrderBySentAtDesc(
                        ANNOUNCEMENT_TYPE, needle, PageRequest.of(page, clampSize(size)));
        List<AnnouncementSummary> mapped = result.getContent().stream()
                .map(n -> {
                    JsonNode meta = parseJson(n.getMetadata());
                    long recipients = meta != null && meta.has("recipientCount")
                            ? meta.get("recipientCount").asLong(0) : 0;
                    String audienceType = meta != null && meta.has("audienceType")
                            ? meta.get("audienceType").asText(n.getTargetAudience()) : n.getTargetAudience();
                    return AnnouncementSummary.builder()
                            .id(n.getId())
                            .title(n.getTitle())
                            .summary(n.getMessage() != null && n.getMessage().length() > 200
                                    ? n.getMessage().substring(0, 200) + "…" : n.getMessage())
                            .priority(n.getPriority())
                            .audienceType(audienceType)
                            .recipientCount(recipients)
                            .sentBy(n.getSentBy())
                            .sentAt(n.getSentAt())
                            .expiresAt(n.getExpiresAt())
                            .build();
                })
                .toList();
        return toPageResponse(mapped, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional
    public AnnouncementDetailResponse createAnnouncement(UUID userId, CreateAnnouncementRequest request) {
        Scope scope = resolveScope(userId);
        String audienceType = request.getAudienceType().trim().toUpperCase(Locale.ROOT);

        List<UUID> targetDistrictIds = request.getTargetDistrictIds() != null
                ? request.getTargetDistrictIds() : List.of();
        List<UUID> targetInstitutionIds = request.getTargetInstitutionIds() != null
                ? request.getTargetInstitutionIds() : List.of();

        List<UUID> recipientsInstitutions;
        switch (audienceType) {
            case "ALL" -> recipientsInstitutions = scope.institutionIds();
            case "DISTRICTS" -> {
                if (targetDistrictIds.isEmpty()) {
                    throw new IllegalArgumentException("targetDistrictIds is required for DISTRICTS audience");
                }
                List<UUID> collected = new ArrayList<>();
                for (UUID districtId : targetDistrictIds) {
                    assertDistrictInScope(scope, districtId);
                    collected.addAll(institutionRepository.findByDistrictIdAndIsDeletedFalse(districtId).stream()
                            .map(Institution::getId).toList());
                }
                recipientsInstitutions = collected;
            }
            case "INSTITUTIONS" -> {
                if (targetInstitutionIds.isEmpty()) {
                    throw new IllegalArgumentException("targetInstitutionIds is required for INSTITUTIONS audience");
                }
                List<UUID> collected = new ArrayList<>();
                for (UUID institutionId : targetInstitutionIds) {
                    collected.add(assertInstitutionInScope(scope, institutionId).getId());
                }
                recipientsInstitutions = collected;
            }
            default -> throw new IllegalArgumentException(
                    "Unsupported audience type: " + request.getAudienceType() + " (use ALL, DISTRICTS or INSTITUTIONS)");
        }
        if (recipientsInstitutions.isEmpty()) {
            throw new IllegalArgumentException("No institutions matched the requested audience inside your jurisdiction");
        }

        // Resolve recipients from real institution memberships inside the jurisdiction.
        Set<UUID> recipients = new LinkedHashSet<>();
        for (UUID institutionId : recipientsInstitutions) {
            for (InstitutionMembership membership : membershipRepository.findByInstitutionIdAndIsActiveTrue(institutionId)) {
                if (!Boolean.TRUE.equals(membership.getIsDeleted()) && membership.getUserId() != null) {
                    recipients.add(membership.getUserId());
                }
            }
        }
        if (recipients.isEmpty()) {
            throw new IllegalStateException("No recipients found for the selected audience");
        }
        if (recipients.size() > MAX_ANNOUNCEMENT_RECIPIENTS) {
            throw new IllegalStateException("Audience too large: " + recipients.size()
                    + " recipients (maximum " + MAX_ANNOUNCEMENT_RECIPIENTS
                    + "). Narrow the audience (e.g. target specific districts or institutions).");
        }

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("audienceType", audienceType);
        metadata.put("regionId", scope.regionId().toString());
        if (scope.districtId() != null) metadata.put("districtId", scope.districtId().toString());
        metadata.put("targetDistrictIds", targetDistrictIds.stream().map(UUID::toString).toList());
        metadata.put("targetInstitutionIds", recipientsInstitutions.stream().map(UUID::toString).toList());
        metadata.put("recipientCount", recipients.size());
        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            metadataJson = "{}";
        }

        PlatformNotification notification = PlatformNotification.builder()
                .title(request.getTitle())
                .message(request.getContent())
                .notificationType(ANNOUNCEMENT_TYPE)
                .priority(request.getPriority() != null && !request.getPriority().isBlank()
                        ? request.getPriority().trim().toUpperCase(Locale.ROOT) : "NORMAL")
                .targetAudience(audienceNeedle(scope))
                .targetRole(audienceType)
                .sentBy(scope.user().getEmail())
                .sentAt(LocalDateTime.now())
                .expiresAt(request.getExpiresAt())
                .metadata(metadataJson)
                .build();
        notification = platformNotificationRepository.save(notification);

        // Deliver through the existing notification infrastructure (real inbox rows + realtime event).
        for (UUID recipientId : recipients) {
            notificationService.notifyUser(recipientId, request.getTitle(), request.getContent(),
                    ANNOUNCEMENT_TYPE, "ANNOUNCEMENT", notification.getId());
        }
        log.info("Regional announcement '{}' sent by {} to {} recipient(s) in {}",
                notification.getTitle(), scope.user().getEmail(), recipients.size(),
                scope.districtId() != null ? "district " + scope.districtId() : "region " + scope.regionId());

        return AnnouncementDetailResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getMessage())
                .priority(notification.getPriority())
                .audienceType(audienceType)
                .targetDistrictIds(targetDistrictIds)
                .targetInstitutionIds(recipientsInstitutions)
                .recipientCount(recipients.size())
                .sentBy(notification.getSentBy())
                .sentAt(notification.getSentAt())
                .expiresAt(notification.getExpiresAt())
                .build();
    }

    public PageResponse<NotificationSummary> getNotifications(UUID userId, int page, int size) {
        resolveScope(userId);
        List<LearnerNotification> all = notificationService.getNotifications(userId);
        all = all.stream()
                .sorted(Comparator.comparing(LearnerNotification::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        int safeSize = clampSize(size);
        int pageIdx = clampPage(page, all.size(), safeSize);
        List<LearnerNotification> slice = all.isEmpty() ? List.of()
                : all.subList(pageIdx * safeSize, Math.min(all.size(), pageIdx * safeSize + safeSize));
        List<NotificationSummary> mapped = slice.stream()
                .map(n -> NotificationSummary.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .notificationType(n.getNotificationType())
                        .targetType(n.getTargetType())
                        .targetId(n.getTargetId())
                        .isRead(n.getIsRead())
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
        return toPageResponse(mapped, pageIdx, safeSize, all.size());
    }

    public long getUnreadNotificationCount(UUID userId) {
        resolveScope(userId);
        return notificationService.getNotifications(userId).stream()
                .filter(n -> !Boolean.TRUE.equals(n.getIsRead()))
                .count();
    }

    @Transactional
    public void markNotificationRead(UUID userId, UUID notificationId) {
        LearnerNotification notification = learnerNotificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        if (!userId.equals(notification.getUserId())) {
            throw new ForbiddenException("notification", "read");
        }
        notificationService.markAsRead(notificationId, userId);
    }

    // ────────────────────────────────────────────────────────────────────────
    // Utilities
    // ────────────────────────────────────────────────────────────────────────

    public List<QuickActionResponse> getQuickActions(UUID userId) {
        resolveScope(userId);
        return quickActions(null);
    }

    private List<QuickActionResponse> quickActions(String role) {
        return List.of(
                QuickActionResponse.builder().id("create-announcement")
                        .label("Create Regional Announcement")
                        .description("Send a notice to districts, institutions, teachers or learners in your jurisdiction")
                        .icon("Megaphone")
                        .route("/dashboard/regional-admin/communication")
                        .category("COMMUNICATION").build(),
                QuickActionResponse.builder().id("review-verification")
                        .label("Review Verifications")
                        .description("Approve, reject or request changes on verification requests in your jurisdiction")
                        .icon("BadgeCheck")
                        .route("/dashboard/regional-admin/governance/verification")
                        .category("GOVERNANCE").build(),
                QuickActionResponse.builder().id("open-data-quality")
                        .label("Open Data Quality")
                        .description("Inspect missing links, duplicates and incomplete records")
                        .icon("ShieldAlert")
                        .route("/dashboard/regional-admin/governance/data-quality")
                        .category("GOVERNANCE").build(),
                QuickActionResponse.builder().id("open-districts")
                        .label("Open Districts")
                        .description("Drill down into districts and their institutions")
                        .icon("MapPin")
                        .route("/dashboard/regional-admin/districts")
                        .category("GEOGRAPHY").build(),
                QuickActionResponse.builder().id("open-institutions")
                        .label("Open Institutions")
                        .description("Inspect institutions and schools in your jurisdiction")
                        .icon("Building2")
                        .route("/dashboard/regional-admin/institutions")
                        .category("GEOGRAPHY").build(),
                QuickActionResponse.builder().id("open-audit")
                        .label("Open Audit")
                        .description("Review audited administrative actions inside your jurisdiction")
                        .icon("ScrollText")
                        .route("/dashboard/regional-admin/governance/audit")
                        .category("GOVERNANCE").build(),
                QuickActionResponse.builder().id("open-reports")
                        .label("Open Reports")
                        .description("Generate jurisdiction-scoped education reports")
                        .icon("FileBarChart")
                        .route("/oversight/reports")
                        .category("ANALYTICS").build(),
                QuickActionResponse.builder().id("search")
                        .label("Search Records")
                        .description("Search districts, institutions, teachers and learners")
                        .icon("Search")
                        .route("/dashboard/regional-admin/institutions")
                        .category("UTILITY").build());
    }

    public List<SearchResultResponse> search(UUID userId, String query, int limit) {
        Scope scope = resolveScope(userId);
        String q = normalize(query);
        if (q.isEmpty()) return List.of();
        int max = Math.max(1, Math.min(limit, 50));
        List<SearchResultResponse> results = new ArrayList<>();

        if (scope.districtId() == null) {
            for (District district : districtRepository.findByRegionIdAndIsDeletedFalse(scope.regionId())) {
                if (results.size() >= max) break;
                if (contains(district.getName(), q) || contains(district.getCode(), q)) {
                    results.add(SearchResultResponse.builder()
                            .type("DISTRICT").id(district.getId())
                            .title(district.getName())
                            .subtitle(district.getCode())
                            .route("/dashboard/regional-admin/districts/" + district.getId())
                            .icon("MapPin").build());
                }
            }
        }
        for (Institution institution : scope.institutions()) {
            if (results.size() >= max) break;
            if (contains(institution.getName(), q) || contains(institution.getCode(), q)) {
                results.add(SearchResultResponse.builder()
                        .type("INSTITUTION").id(institution.getId())
                        .title(institution.getName())
                        .subtitle((institution.getType() != null ? institution.getType().name() : "")
                                + (institution.getDistrictId() != null ? " · " + districtName(institution.getDistrictId()) : ""))
                        .route("/dashboard/regional-admin/institutions/" + institution.getId())
                        .icon("Building2").build());
            }
        }
        if (!scope.institutionIds().isEmpty() && results.size() < max) {
            Page<User> users = userRepository.findScopedBySearch(q, scope.institutionIds(),
                    PageRequest.of(0, max));
            for (User user : users.getContent()) {
                if (results.size() >= max) break;
                User.Role role = user.getRole();
                if (role == null) continue;
                String route = switch (role) {
                    case STUDENT, OTHER_LEARNER, LEARNER -> "/dashboard/regional-admin/learners";
                    case TEACHER, INSTRUCTOR -> "/dashboard/regional-admin/teachers";
                    case INSTITUTION_ADMIN -> "/dashboard/regional-admin/education-staff";
                    default -> null;
                };
                if (route == null) continue;
                results.add(SearchResultResponse.builder()
                        .type(role.name()).id(user.getId())
                        .title(user.getFullName())
                        .subtitle(user.getEmail())
                        .route(route).icon("User").build());
            }
        }
        return results;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Internals
    // ────────────────────────────────────────────────────────────────────────

    private List<VerificationRecord> inScopeVerifications(Scope scope) {
        Set<UUID> scopeIds = scope.institutionIdSet();
        Set<UUID> cached = scopeIds;
        List<VerificationRecord> all = verificationRepository.findAll();
        List<VerificationRecord> result = new ArrayList<>();
        for (VerificationRecord record : all) {
            UUID institutionId = resolveEntityInstitutionId(record);
            if (institutionId != null && cached.contains(institutionId)) {
                result.add(record);
            }
        }
        return result;
    }

    private UUID resolveEntityInstitutionId(VerificationRecord record) {
        if (record.getInstitutionId() != null) return record.getInstitutionId();
        String type = record.getEntityType() != null ? record.getEntityType().toUpperCase(Locale.ROOT) : "";
        switch (type) {
            case "INSTITUTION":
                return record.getEntityId();
            case "PROVIDER":
                return membershipRepository.findByUserIdAndIsActiveTrue(record.getEntityId()).stream()
                        .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                        .map(InstitutionMembership::getInstitutionId)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse(null);
            default:
                return null;
        }
    }

    private void assertVerificationInScope(Scope scope, VerificationRecord record) {
        UUID institutionId = resolveEntityInstitutionId(record);
        if (institutionId == null || !scope.institutionIdSet().contains(institutionId)) {
            throw new ForbiddenException("verification", "review");
        }
    }

    private String resolveEntityName(VerificationRecord record, Map<UUID, String> names) {
        UUID institutionId = resolveEntityInstitutionId(record);
        if (institutionId == null) return null;
        return names.getOrDefault(institutionId,
                institutionRepository.findById(institutionId).map(Institution::getName).orElse(null));
    }

    private List<DistrictSummary> districtSummaries(Scope scope) {
        List<District> districts = scope.districtId() != null
                ? districtRepository.findById(scope.districtId()).map(List::of).orElse(List.of())
                : districtRepository.findByRegionIdAndIsDeletedFalse(scope.regionId());
        return districts.stream().map(d -> {
            List<UUID> ids = institutionRepository.findByDistrictIdAndIsDeletedFalse(d.getId()).stream()
                    .map(Institution::getId).toList();
            return DistrictSummary.builder()
                    .id(d.getId())
                    .name(d.getName())
                    .code(d.getCode())
                    .isActive(d.getIsActive())
                    .institutionCount((long) ids.size())
                    .schoolCount(institutionRepository.findByDistrictIdAndIsDeletedFalse(d.getId()).stream()
                            .filter(i -> SCHOOL_TYPES.contains(i.getType() != null ? i.getType().name() : "")).count())
                    .teacherCount(countMembers(ids, InstitutionMembership.Role.TEACHER))
                    .learnerCount(countMembers(ids, InstitutionMembership.Role.STUDENT))
                    .attendanceRate(ids.isEmpty() ? 0.0 : oversightService.jurisdictionAttendanceRate(ids))
                    .averagePerformance(ids.isEmpty() ? 0.0 : oversightService.jurisdictionAveragePerformance(ids))
                    .build();
        }).toList();
    }

    private RegionResponse toRegionResponse(Scope scope) {
        List<UUID> ids = scope.institutionIds();
        return RegionResponse.builder()
                .id(scope.region().getId())
                .name(scope.region().getName())
                .code(scope.region().getCode())
                .isActive(scope.region().getIsActive())
                .institutionCount((long) ids.size())
                .teacherCount(countMembers(ids, InstitutionMembership.Role.TEACHER))
                .studentCount(countMembers(ids, InstitutionMembership.Role.STUDENT))
                .build();
    }

    private InstitutionSummary toInstitutionSummary(Institution institution) {
        List<UUID> one = List.of(institution.getId());
        return InstitutionSummary.builder()
                .id(institution.getId())
                .name(institution.getName())
                .code(institution.getCode())
                .type(institution.getType() != null ? institution.getType().name() : null)
                .districtName(districtName(institution.getDistrictId()))
                .regionName(institution.getRegionId() != null
                        ? regionRepository.findById(institution.getRegionId()).map(Region::getName).orElse(null)
                        : null)
                .teacherCount(countMembers(one, InstitutionMembership.Role.TEACHER))
                .studentCount(countMembers(one, InstitutionMembership.Role.STUDENT))
                .isActive(institution.getIsActive())
                .build();
    }

    private List<User> pagedUsers(Scope scope, List<User.Role> roles, String search,
                                  UUID institutionId, UUID districtId, int page, int size) {
        List<UUID> ids = targetInstitutionIds(scope, institutionId, districtId);
        String q = normalize(search);
        Page<User> result = ids.isEmpty()
                ? userRepository.findScopedByRolesAndRegion(roles, q, scope.regionId(),
                PageRequest.of(page, clampSize(size)))
                : userRepository.findScopedByRoles(roles, q, ids, scope.regionId(),
                PageRequest.of(page, clampSize(size)));
        return result.getContent();
    }

    private LearnerSummary toLearnerSummary(Scope scope, User user) {
        return LearnerSummary.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .institutionId(user.getInstitutionId())
                .institutionName(institutionName(scope, user.getInstitutionId()))
                .role(user.getRole() != null ? user.getRole().name() : null)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private TeacherSummary toTeacherSummary(Scope scope, User user) {
        return TeacherSummary.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .institutionId(user.getInstitutionId())
                .institutionName(institutionName(scope, user.getInstitutionId()))
                .role(user.getRole() != null ? user.getRole().name() : null)
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private Map<UUID, String> institutionNameMap(Scope scope) {
        Map<UUID, String> names = new HashMap<>();
        for (Institution institution : scope.institutions()) {
            names.put(institution.getId(), institution.getName());
        }
        return names;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Ward dimension (PROMPT §23)
    // ────────────────────────────────────────────────────────────────────────

    public PageResponse<WardSummary> getWards(UUID userId, int page, int size,
                                              String search, UUID districtId) {
        Scope scope = resolveScope(userId);
        List<UUID> districtIds;
        if (districtId != null) {
            assertDistrictInScope(scope, districtId);
            districtIds = List.of(districtId);
        } else if (scope.districtId() != null) {
            districtIds = List.of(scope.districtId());
        } else {
            districtIds = districtRepository.findByRegionIdAndIsDeletedFalse(scope.regionId())
                    .stream().map(District::getId).toList();
        }
        List<Ward> wards = districtIds.isEmpty() ? List.of()
                : wardRepository.findByDistrictIdInAndIsDeletedFalse(districtIds);
        String q = normalize(search);
        if (!q.isEmpty()) {
            wards = wards.stream()
                    .filter(w -> contains(w.getName(), q) || contains(w.getCode(), q))
                    .toList();
        }
        wards = wards.stream()
                .sorted(Comparator.comparing(Ward::getName, Comparator.nullsLast(String::compareTo)))
                .toList();

        int safeSize = clampSize(size);
        int from = clampPage(page, wards.size(), safeSize) * safeSize;
        List<Ward> slice = wards.isEmpty() ? List.of()
                : wards.subList(from, Math.min(wards.size(), from + safeSize));
        return toPageResponse(
                slice.stream().map(this::toWardSummary).toList(),
                from / safeSize, safeSize, wards.size());
    }

    public WardDetailResponse getWardDetail(UUID userId, UUID wardId) {
        Scope scope = resolveScope(userId);
        Ward ward = wardRepository.findById(wardId)
                .orElseThrow(() -> new ResourceNotFoundException("Ward not found"));
        District district = districtRepository.findById(ward.getDistrictId())
                .orElseThrow(() -> new ResourceNotFoundException("District not found"));
        assertDistrictInScope(scope, district.getId());
        Region region = regionRepository.findById(district.getRegionId()).orElse(null);

        List<Institution> institutions =
                institutionRepository.findByWardIdAndIsDeletedFalse(wardId);
        List<UUID> ids = institutions.stream().map(Institution::getId).toList();

        return WardDetailResponse.builder()
                .id(ward.getId())
                .name(ward.getName())
                .code(ward.getCode())
                .isActive(ward.getIsActive())
                .districtId(district.getId())
                .districtName(district.getName())
                .districtCode(district.getCode())
                .regionId(district.getRegionId())
                .regionName(region != null ? region.getName() : null)
                .regionCode(region != null ? region.getCode() : null)
                .institutionCount((long) ids.size())
                .institutions(institutions.stream().map(this::toInstitutionSummary).toList())
                .build();
    }

    private WardSummary toWardSummary(Ward ward) {
        District district = districtRepository.findById(ward.getDistrictId()).orElse(null);
        Region region = district != null
                ? regionRepository.findById(district.getRegionId()).orElse(null)
                : null;
        long institutions = institutionRepository.countByWardIdAndIsDeletedFalse(ward.getId());
        return WardSummary.builder()
                .id(ward.getId())
                .name(ward.getName())
                .code(ward.getCode())
                .isActive(ward.getIsActive())
                .districtId(ward.getDistrictId())
                .districtName(district != null ? district.getName() : null)
                .districtCode(district != null ? district.getCode() : null)
                .regionId(district != null ? district.getRegionId() : null)
                .regionName(region != null ? region.getName() : null)
                .regionCode(region != null ? region.getCode() : null)
                .institutionCount(institutions)
                .build();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Scheduled reports (PROMPT §45)
    // ────────────────────────────────────────────────────────────────────────

    public List<ScheduledReportSummary> getScheduledReports(UUID userId) {
        Scope scope = resolveScope(userId);
        return scheduledReportRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .stream()
                .filter(r -> reportInScope(r, scope))
                .map(this::toScheduledReportSummary)
                .toList();
    }

    @Transactional
    public ScheduledReportSummary createScheduledReport(UUID userId, CreateScheduledReportRequest request) {
        Scope scope = resolveScope(userId);
        ScheduledReport report = ScheduledReport.builder()
                .userId(userId)
                .regionId(scope.regionId())
                .districtId(scope.districtId())
                .reportType(request.getReportType())
                .title(request.getTitle())
                .frequency(request.getFrequency())
                .recipients(request.getRecipients())
                .status("ACTIVE")
                .nextRunAt(nextRunBoundary(request.getFrequency(), LocalDateTime.now()))
                .runCount(0)
                .build();
        report = scheduledReportRepository.save(report);
        log.info("Scheduled report created: {} type={} freq={} owner={}",
                report.getId(), report.getReportType(), report.getFrequency(), userId);
        return toScheduledReportSummary(report);
    }

    @Transactional
    public ScheduledReportSummary updateScheduledReport(UUID userId, UUID reportId,
                                                        UpdateScheduledReportRequest request) {
        Scope scope = resolveScope(userId);
        ScheduledReport report = ownedReport(userId, reportId, scope);
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            report.setTitle(request.getTitle());
        }
        if (request.getStatus() != null) {
            report.setStatus(request.getStatus());
            if ("ACTIVE".equals(request.getStatus()) && report.getNextRunAt() == null) {
                report.setNextRunAt(nextRunBoundary(report.getFrequency(), LocalDateTime.now()));
            }
        }
        if (request.getFrequency() != null) {
            report.setFrequency(request.getFrequency());
            report.setNextRunAt(nextRunBoundary(request.getFrequency(), LocalDateTime.now()));
        }
        if (request.getRecipients() != null) {
            report.setRecipients(request.getRecipients());
        }
        return toScheduledReportSummary(scheduledReportRepository.save(report));
    }

    @Transactional
    public void deleteScheduledReport(UUID userId, UUID reportId) {
        Scope scope = resolveScope(userId);
        ScheduledReport report = ownedReport(userId, reportId, scope);
        report.setIsDeleted(true);
        report.setStatus("PAUSED");
        report.setNextRunAt(null);
        scheduledReportRepository.save(report);
        log.info("Scheduled report {} deleted by {}", reportId, userId);
    }

    @Transactional
    public ScheduledReportRunSummary runScheduledReportNow(UUID userId, UUID reportId) {
        Scope scope = resolveScope(userId);
        ScheduledReport report = ownedReport(userId, reportId, scope);
        return executeRun(report, scope, true);
    }

    public List<ScheduledReportRunSummary> getScheduledReportRuns(UUID userId, UUID reportId) {
        Scope scope = resolveScope(userId);
        ownedReport(userId, reportId, scope);
        return scheduledReportRunRepository.findByScheduledReportIdOrderByRunAtDesc(reportId)
                .stream()
                .map(r -> ScheduledReportRunSummary.builder()
                        .id(r.getId())
                        .scheduledReportId(r.getScheduledReportId())
                        .runAt(r.getRunAt())
                        .status(r.getStatus())
                        .summary(r.getSummary())
                        .error(r.getError())
                        .build())
                .toList();
    }

    /**
     * Materialise every due ACTIVE scheduled report. Invoked by the shared
     * Spring scheduler ({@code RegionalReportScheduler}); each report runs in
     * its owner's scope so a jurisdiction change can never leak data.
     */
    @Transactional
    public int runDueScheduledReports() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledReport> due = scheduledReportRepository.findDue("ACTIVE", now);
        int executed = 0;
        for (ScheduledReport report : due) {
            try {
                Scope scope = resolveScope(report.getUserId());
                executeRun(report, scope, false);
                executed++;
            } catch (Exception ex) {
                log.warn("Scheduled report {} failed: {}", report.getId(), ex.getMessage());
                failRun(report, ex.getMessage());
            }
        }
        return executed;
    }

    private ScheduledReportRunSummary executeRun(ScheduledReport report, Scope scope,
                                                 boolean ownerTriggered) {
        String summary;
        try {
            summary = buildReportSnapshot(report, scope);
        } catch (Exception ex) {
            failRun(report, ex.getMessage());
            if (ownerTriggered) {
                throw new IllegalStateException("Could not build report snapshot: " + ex.getMessage(), ex);
            }
            return ScheduledReportRunSummary.builder()
                    .scheduledReportId(report.getId())
                    .runAt(LocalDateTime.now())
                    .status("FAILED")
                    .error(ex.getMessage())
                    .build();
        }
        LocalDateTime now = LocalDateTime.now();
        ScheduledReportRun run = ScheduledReportRun.builder()
                .scheduledReportId(report.getId())
                .runAt(now)
                .status("SUCCESS")
                .summary(summary)
                .error(null)
                .build();
        scheduledReportRunRepository.save(run);
        report.setLastRunAt(now);
        report.setRunCount((report.getRunCount() == null ? 0 : report.getRunCount()) + 1);
        report.setNextRunAt(nextRunBoundary(report.getFrequency(), now));
        scheduledReportRepository.save(report);

        if (!ownerTriggered) {
            try {
                notificationService.notifyUser(report.getUserId(),
                        "Scheduled report ready",
                        report.getTitle() + " was generated (" + report.getFrequency().toLowerCase(Locale.ROOT)
                                + ", " + report.getReportType() + ").",
                        ANNOUNCEMENT_TYPE, "SCHEDULED_REPORT", report.getId());
            } catch (Exception ex) {
                log.warn("Could not notify owner of scheduled report {}: {}", report.getId(), ex.getMessage());
            }
        }
        log.info("Scheduled report {} executed by {}", report.getId(),
                ownerTriggered ? "owner" : "scheduler");
        return ScheduledReportRunSummary.builder()
                .id(run.getId())
                .scheduledReportId(run.getScheduledReportId())
                .runAt(run.getRunAt())
                .status(run.getStatus())
                .summary(run.getSummary())
                .build();
    }

    private void failRun(ScheduledReport report, String error) {
        LocalDateTime now = LocalDateTime.now();
        scheduledReportRunRepository.save(ScheduledReportRun.builder()
                .scheduledReportId(report.getId())
                .runAt(now)
                .status("FAILED")
                .summary(null)
                .error(error != null ? error : "unknown error")
                .build());
        report.setLastRunAt(now);
        report.setRunCount((report.getRunCount() == null ? 0 : report.getRunCount()) + 1);
        report.setStatus("PAUSED");
        report.setNextRunAt(null);
        scheduledReportRepository.save(report);
    }

    private String buildReportSnapshot(ScheduledReport report, Scope scope) throws Exception {
        OversightDashboardResponse dashboard =
                oversightService.getDashboard(scope.regionId(), scope.districtId());
        DataQualityResponse dq = computeDataQuality(scope);
        long pending = inScopeVerifications(scope).stream()
                .filter(v -> "PENDING".equalsIgnoreCase(v.getStatus())).count();

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("reportId", report.getId());
        snapshot.put("title", report.getTitle());
        snapshot.put("reportType", report.getReportType());
        snapshot.put("frequency", report.getFrequency());
        snapshot.put("jurisdiction", scope.districtId() != null
                ? (scope.district() != null ? scope.district().getName() : scope.districtId())
                : (scope.region() != null ? scope.region().getName() : scope.regionId()));
        snapshot.put("generatedAt", LocalDateTime.now().toString());
        snapshot.put("institutions", dashboard.getTotalInstitutions());
        snapshot.put("teachers", dashboard.getTotalTeachers());
        snapshot.put("learners", dashboard.getTotalStudents());
        snapshot.put("attendanceRate", dashboard.getAttendanceRate());
        snapshot.put("averagePerformance", dashboard.getAveragePerformance());
        snapshot.put("pendingVerifications", pending);
        snapshot.put("dataQualityIssues", dq.getTotalIssues());
        snapshot.put("dataQualityCritical", dq.getCriticalIssues());
        return objectMapper.writeValueAsString(snapshot);
    }

    private ScheduledReport ownedReport(UUID userId, UUID reportId, Scope scope) {
        ScheduledReport report = scheduledReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled report not found"));
        if (!userId.equals(report.getUserId())) {
            throw new ForbiddenException("You do not own this scheduled report");
        }
        if (!reportInScope(report, scope)) {
            throw new ForbiddenException("Scheduled report is outside your jurisdiction");
        }
        return report;
    }

    private boolean reportInScope(ScheduledReport report, Scope scope) {
        if (scope.districtId() != null) {
            return scope.districtId().equals(report.getDistrictId());
        }
        if (report.getDistrictId() != null) {
            return false;
        }
        return scope.regionId().equals(report.getRegionId());
    }

    private ScheduledReportSummary toScheduledReportSummary(ScheduledReport report) {
        String jurisdiction;
        if (report.getDistrictId() != null) {
            jurisdiction = districtName(report.getDistrictId());
        } else if (report.getRegionId() != null) {
            jurisdiction = regionRepository.findById(report.getRegionId())
                    .map(Region::getName).orElse(null);
        } else {
            jurisdiction = null;
        }
        return ScheduledReportSummary.builder()
                .id(report.getId())
                .title(report.getTitle())
                .reportType(report.getReportType())
                .frequency(report.getFrequency())
                .status(report.getStatus())
                .recipients(report.getRecipients())
                .regionId(report.getRegionId())
                .districtId(report.getDistrictId())
                .jurisdiction(jurisdiction)
                .nextRunAt(report.getNextRunAt())
                .lastRunAt(report.getLastRunAt())
                .runCount(report.getRunCount())
                .createdAt(report.getCreatedAt())
                .build();
    }

    /** Next 02:00 boundary for DAILY / next Monday for WEEKLY / 1st of month for MONTHLY. */
    private static LocalDateTime nextRunBoundary(String frequency, LocalDateTime from) {
        LocalDateTime tomorrow = from.toLocalDate().plusDays(1)
                .atTime(2, 0, 0, 0);
        if (frequency == null) return tomorrow;
        return switch (frequency) {
            case "WEEKLY" -> {
                LocalDateTime d = tomorrow;
                while (d.getDayOfWeek() != DayOfWeek.MONDAY) d = d.plusDays(1);
                yield d;
            }
            case "MONTHLY" -> from.toLocalDate().withDayOfMonth(1).plusMonths(1)
                    .atTime(2, 0, 0, 0);
            default -> tomorrow;
        };
    }

    private String institutionName(Scope scope, UUID institutionId) {
        if (institutionId == null) return null;
        for (Institution institution : scope.institutions()) {
            if (institutionId.equals(institution.getId())) return institution.getName();
        }
        return institutionRepository.findById(institutionId).map(Institution::getName).orElse(null);
    }

    private String districtName(UUID districtId) {
        if (districtId == null) return null;
        return districtRepository.findById(districtId).map(District::getName).orElse(null);
    }

    private long countMembers(List<UUID> institutionIds, InstitutionMembership.Role role) {
        if (institutionIds.isEmpty()) return 0;
        return membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(institutionIds, role);
    }

    private long countCourses(List<UUID> institutionIds) {
        long total = 0;
        for (UUID id : institutionIds) {
            total += courseRepository.countByInstitutionIdAndIsDeletedFalse(id);
        }
        return total;
    }

    private long lessonCount(Scope scope) {
        if (scope.institutionIds().isEmpty()) return 0;
        return oversightService.getCurriculum(scope.regionId(), scope.districtId()).getTotalLessons() != null
                ? oversightService.getCurriculum(scope.regionId(), scope.districtId()).getTotalLessons() : 0L;
    }

    private long publishedLessonCount(Scope scope) {
        if (scope.institutionIds().isEmpty()) return 0;
        CurriculumResponse curriculum = oversightService.getCurriculum(scope.regionId(), scope.districtId());
        return curriculum.getCompletedLessons() != null ? curriculum.getCompletedLessons() : 0L;
    }

    private String audienceNeedle(Scope scope) {
        return scope.districtId() != null
                ? "DISTRICT:" + scope.districtId()
                : "REGION:" + scope.regionId();
    }

    private DataQualityIssue issue(String type, String severity, String title, String description,
                                   String entityType, UUID entityId, String entityName,
                                   String suggestedAction, LocalDateTime detectedAt) {
        return DataQualityIssue.builder()
                .id(UUID.randomUUID().toString())
                .type(type).severity(severity)
                .title(title).description(description)
                .entityType(entityType).entityId(entityId).entityName(entityName)
                .suggestedAction(suggestedAction)
                .detectedAt(detectedAt)
                .build();
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean contains(String haystack, String needleLower) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needleLower);
    }

    private static int clampSize(int size) {
        return Math.max(1, Math.min(size, 100));
    }

    private static int clampPage(int page, int totalItems, int size) {
        int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / size));
        return Math.max(0, Math.min(page, totalPages - 1));
    }

    private static <T> PageResponse<T> emptyPage(int page, int size) {
        int safeSize = clampSize(size);
        return new PageResponse<>(List.of(), Math.max(0, page), safeSize, 0, 0, true, true);
    }

    private static <T> PageResponse<T> toPageResponse(List<T> content, int page, int size, long totalElements) {
        int safeSize = clampSize(size);
        long total = totalElements >= 0 ? totalElements : content.size();
        int totalPages = (int) Math.ceil((double) total / safeSize);
        return new PageResponse<>(content, page, safeSize, total, totalPages,
                page == 0, page >= totalPages - 1 || total == 0);
    }

    private static long nvl(Long value) {
        return value != null ? value : 0L;
    }

    private static long nvl(Long... values) {
        long sum = 0;
        if (values != null) {
            for (Long v : values) sum += v != null ? v : 0;
        }
        return sum;
    }
}
