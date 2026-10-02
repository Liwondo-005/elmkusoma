package tz.elmkusoma.oversight.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.administration.domain.ContentReport;
import tz.elmkusoma.administration.domain.VerificationRecord;
import tz.elmkusoma.administration.dto.GlobalSearchResult;
import tz.elmkusoma.administration.repository.ContentReportRepository;
import tz.elmkusoma.administration.repository.VerificationRecordRepository;
import tz.elmkusoma.administration.service.PlatformAdminService;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learning.repository.LessonRepository;
import tz.elmkusoma.oversight.domain.District;
import tz.elmkusoma.oversight.domain.Region;
import tz.elmkusoma.oversight.dto.*;
import tz.elmkusoma.oversight.repository.DistrictRepository;
import tz.elmkusoma.oversight.repository.RegionRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * National/Regional/District governance layer (§Nationaladmin.md).
 *
 * <p>Every method reuses the aggregation logic already proven in
 * {@link OversightService}, the platform search engine
 * ({@link PlatformAdminService#globalSearch}), the verification and content-report
 * repositories, and the existing CSV export pattern — no parallel dashboards,
 * no second search engine, no duplicate notification stack.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OversightGovernanceService {

    private final OversightService oversightService;
    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final ClassGroupRepository classGroupRepository;
    private final LessonRepository lessonRepository;
    private final LiveClassRepository liveClassRepository;
    private final VerificationRecordRepository verificationRecordRepository;
    private final ContentReportRepository contentReportRepository;
    private final PlatformAdminService platformAdminService;

    // ---------------------------------------------------------------- attention

    public AttentionResponse getAttention(UUID regionId, UUID districtId) {
        Set<UUID> scopeInstitutionIds =
                new HashSet<>(oversightService.getInstitutionIdsInJurisdiction(regionId, districtId));
        boolean national = regionId == null && districtId == null;

        List<AttentionResponse.AttentionItem> items = new ArrayList<>();

        // 1. Operational alerts — reuse the existing alert engine.
        AlertsResponse alerts = oversightService.getAlerts(regionId, districtId);
        if (alerts.getAlerts() != null) {
            for (AlertsResponse.Alert alert : alerts.getAlerts()) {
                String category = switch (alert.getType() == null ? "" : alert.getType()) {
                    case "LOW_ATTENDANCE" -> "ATTENDANCE";
                    case "LOW_PERFORMANCE" -> "PERFORMANCE";
                    default -> "OPERATIONAL";
                };
                items.add(AttentionResponse.AttentionItem.builder()
                        .id(alert.getId())
                        .category(category)
                        .type(alert.getType())
                        .title(alert.getTitle() != null ? alert.getTitle() : alert.getMessage())
                        .message(alert.getMessage())
                        .severity(alert.getSeverity())
                        .jurisdiction(alert.getInstitutionName())
                        .jurisdictionId(alert.getInstitutionId())
                        .timestamp(alert.getTimestamp())
                        .build());
            }
        }

        // 2. Pending verifications — real rows from verification_records.
        for (VerificationRecord record : verificationRecordRepository.findByStatusAndIsDeletedFalse("PENDING")) {
            String entityName;
            if ("INSTITUTION".equalsIgnoreCase(record.getEntityType())) {
                if (!scopeInstitutionIds.contains(record.getEntityId())) continue;
                entityName = institutionRepository.findById(record.getEntityId())
                        .map(Institution::getName).orElse("Institution");
            } else if (national) {
                entityName = record.getEntityType();
            } else {
                continue;
            }
            items.add(AttentionResponse.AttentionItem.builder()
                    .id(record.getId().toString())
                    .category("VERIFICATION")
                    .type(record.getVerificationType())
                    .title("Pending verification")
                    .message(entityName + " is awaiting " + (record.getVerificationType() != null
                            ? record.getVerificationType().toLowerCase() : "document") + " review")
                    .severity("HIGH")
                    .jurisdiction(entityName)
                    .jurisdictionId(record.getEntityId() != null ? record.getEntityId().toString() : null)
                    .timestamp(record.getSubmittedAt())
                    .build());
        }

        // 3. Open content reports — real rows from content_reports.
        Page<ContentReport> openReports =
                contentReportRepository.findByStatusAndIsDeletedFalse("OPEN", PageRequest.of(0, 50));
        for (ContentReport report : openReports.getContent()) {
            if (!national && !reporterInScope(report.getReporterId(), regionId, districtId, scopeInstitutionIds)) {
                continue;
            }
            items.add(AttentionResponse.AttentionItem.builder()
                    .id(report.getId().toString())
                    .category("CONTENT_REPORT")
                    .type(report.getEntityType())
                    .title("Content report: " + report.getReason())
                    .message(report.getEntityTitle() != null ? report.getEntityTitle()
                            : (report.getDescription() != null ? report.getDescription() : ""))
                    .severity("MEDIUM")
                    .jurisdiction(report.getEntityType())
                    .jurisdictionId(report.getEntityId() != null ? report.getEntityId().toString() : null)
                    .timestamp(report.getCreatedAt())
                    .build());
        }

        // 4. Data-quality findings — derived, read-only.
        DataQualityResponse quality = getDataQuality(regionId, districtId);
        for (DataQualityResponse.Check check : quality.getChecks()) {
            if (check.getAffectedCount() == null || check.getAffectedCount() == 0) continue;
            items.add(AttentionResponse.AttentionItem.builder()
                    .id(check.getId())
                    .category("DATA_QUALITY")
                    .type(check.getId())
                    .title(check.getTitle())
                    .message(check.getAffectedCount() + " affected record(s) — " + check.getDescription())
                    .severity(check.getSeverity())
                    .jurisdiction(jurisdictionLabel(regionId, districtId))
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        AttentionResponse.AttentionSummary summary = AttentionResponse.AttentionSummary.builder()
                .total((long) items.size())
                .high(countSeverity(items, "HIGH"))
                .medium(countSeverity(items, "MEDIUM"))
                .low(countSeverity(items, "LOW"))
                .attendance(countCategory(items, "ATTENDANCE"))
                .performance(countCategory(items, "PERFORMANCE"))
                .verification(countCategory(items, "VERIFICATION"))
                .contentReport(countCategory(items, "CONTENT_REPORT"))
                .dataQuality(countCategory(items, "DATA_QUALITY"))
                .build();

        return AttentionResponse.builder().items(items).summary(summary).build();
    }

    private long countSeverity(List<AttentionResponse.AttentionItem> items, String severity) {
        return items.stream().filter(i -> severity.equals(i.getSeverity())).count();
    }

    private long countCategory(List<AttentionResponse.AttentionItem> items, String category) {
        return items.stream().filter(i -> category.equals(i.getCategory())).count();
    }

    private String jurisdictionLabel(UUID regionId, UUID districtId) {
        if (districtId != null) {
            return districtRepository.findById(districtId).map(District::getName).orElse("District");
        }
        if (regionId != null) {
            return regionRepository.findById(regionId).map(Region::getName).orElse("Region");
        }
        return "Tanzania";
    }

    private boolean reporterInScope(UUID reporterId, UUID regionId, UUID districtId, Set<UUID> scopeInstitutionIds) {
        if (reporterId == null) return false;
        User user = userRepository.findById(reporterId).orElse(null);
        if (user == null) return false;
        if (districtId != null) {
            if (districtId.equals(user.getDistrictId())) return true;
            return user.getInstitutionId() != null && scopeInstitutionIds.contains(user.getInstitutionId());
        }
        if (regionId != null) {
            if (regionId.equals(user.getRegionId())) return true;
            return user.getInstitutionId() != null && scopeInstitutionIds.contains(user.getInstitutionId());
        }
        return true;
    }

    // ------------------------------------------------------------ data quality

    public DataQualityResponse getDataQuality(UUID regionId, UUID districtId) {
        List<UUID> institutionIds = oversightService.getInstitutionIdsInJurisdiction(regionId, districtId);
        List<Institution> institutions = institutionRepository.findAllById(institutionIds);
        List<UUID> districtScope = districtIdsInScope(regionId, districtId);

        List<DataQualityResponse.Check> checks = new ArrayList<>();

        // 1. Institutions without region/district linkage.
        List<String> missingGeo = institutions.stream()
                .filter(i -> i.getRegionId() == null || i.getDistrictId() == null)
                .map(Institution::getName)
                .collect(Collectors.toList());
        checks.add(check("institutions-missing-geo",
                "Institutions without region/district linkage",
                "Institution records missing region or district assignment cannot be governed jurisdictionally",
                "HIGH", missingGeo));

        // 2. Institutions without any active member.
        List<String> withoutMembers = new ArrayList<>();
        for (UUID id : institutionIds) {
            if (membershipRepository.countByInstitutionIdsAndIsDeletedFalse(List.of(id)) == 0) {
                institutionRepository.findById(id).map(Institution::getName).ifPresent(withoutMembers::add);
            }
        }
        checks.add(check("institutions-without-members",
                "Institutions without any member",
                "Institutions with zero active staff or learner memberships",
                "MEDIUM", withoutMembers));

        // 3. Duplicate institution codes.
        List<String> duplicateCodes = institutions.stream()
                .filter(i -> i.getCode() != null)
                .collect(Collectors.groupingBy(Institution::getCode, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(e -> "Code " + e.getKey() + " used " + e.getValue() + " times")
                .sorted()
                .collect(Collectors.toList());
        checks.add(check("duplicate-institution-codes",
                "Duplicate institution codes",
                "Institution codes must be unique for reporting integrity",
                "HIGH", duplicateCodes));

        // 4. Districts without institutions (coverage gaps).
        List<String> uncoveredDistricts = new ArrayList<>();
        for (UUID scopeDistrictId : districtScope) {
            if (institutionRepository.countByDistrictIdAndIsDeletedFalse(scopeDistrictId) == 0) {
                districtRepository.findById(scopeDistrictId).map(District::getName).ifPresent(uncoveredDistricts::add);
            }
        }
        checks.add(check("districts-without-institutions",
                "Districts without institutions",
                "Districts in scope that have no institution records",
                "MEDIUM", uncoveredDistricts));

        // 5. Authority users missing their own jurisdiction assignment.
        List<String> authorityWithoutJurisdiction = new ArrayList<>();
        for (User user : userRepository.findByRoleAndIsDeletedFalse(User.Role.REGIONAL_ADMIN, PageRequest.of(0, 1000))) {
            if (user.getRegionId() == null) authorityWithoutJurisdiction.add(user.getEmail());
        }
        for (User user : userRepository.findByRoleAndIsDeletedFalse(User.Role.DISTRICT_ADMIN, PageRequest.of(0, 1000))) {
            if (user.getDistrictId() == null) authorityWithoutJurisdiction.add(user.getEmail());
        }
        checks.add(check("authority-without-jurisdiction",
                "Authority users without jurisdiction",
                "Regional/district admin accounts missing their region or district assignment",
                "HIGH", authorityWithoutJurisdiction));

        // 6. Teacher accounts without an institution.
        List<String> teachersWithoutInstitution = userRepository
                .findByRoleAndIsDeletedFalse(User.Role.TEACHER, PageRequest.of(0, 5000))
                .stream()
                .filter(u -> u.getInstitutionId() == null)
                .map(User::getEmail)
                .collect(Collectors.toList());
        checks.add(check("teachers-without-institution",
                "Teacher accounts without institution",
                "Teacher accounts not linked to any institution",
                "LOW", teachersWithoutInstitution));

        long totalIssues = checks.stream().mapToLong(c -> c.getAffectedCount()).sum();
        long passed = checks.stream().filter(c -> c.getAffectedCount() == 0).count();
        double score = checks.isEmpty() ? 100.0
                : Math.round((double) passed / checks.size() * 1000.0) / 10.0;

        return DataQualityResponse.builder()
                .score(score)
                .totalIssues(totalIssues)
                .checks(checks)
                .build();
    }

    private DataQualityResponse.Check check(String id, String title, String description,
                                            String severity, List<String> affected) {
        return DataQualityResponse.Check.builder()
                .id(id)
                .title(title)
                .description(description)
                .severity(severity)
                .affectedCount((long) affected.size())
                .sample(affected.stream().limit(5).collect(Collectors.toList()))
                .build();
    }

    private List<UUID> districtIdsInScope(UUID regionId, UUID districtId) {
        if (districtId != null) return List.of(districtId);
        if (regionId != null) {
            return districtRepository.findByRegionIdAndIsDeletedFalse(regionId)
                    .stream().map(District::getId).collect(Collectors.toList());
        }
        return districtRepository.findByIsDeletedFalseOrderByCreatedAtDesc().stream()
                .filter(d -> regionId == null || regionId.equals(d.getRegionId()))
                .map(District::getId)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------- search

    /**
     * Jurisdiction-filtered search that delegates to the existing platform
     * search engine — authority roles never get their own second engine.
     */
    public List<GlobalSearchResult> search(String query, String type, UUID regionId, UUID districtId) {
        List<GlobalSearchResult> results = platformAdminService.globalSearch(query, type, 30);
        if (regionId == null && districtId == null) {
            return results;
        }
        Set<UUID> scopeInstitutionIds =
                new HashSet<>(oversightService.getInstitutionIdsInJurisdiction(regionId, districtId));
        return results.stream()
                .filter(r -> resultInScope(r, regionId, districtId, scopeInstitutionIds))
                .collect(Collectors.toList());
    }

    private boolean resultInScope(GlobalSearchResult result, UUID regionId, UUID districtId,
                                  Set<UUID> scopeInstitutionIds) {
        if (result.getType() == null || result.getId() == null) return false;
        switch (result.getType()) {
            case "INSTITUTION":
                return scopeInstitutionIds.contains(result.getId());
            case "USER":
                User user = userRepository.findById(result.getId()).orElse(null);
                if (user == null) return false;
                if (user.getInstitutionId() != null && scopeInstitutionIds.contains(user.getInstitutionId())) return true;
                if (districtId != null) return districtId.equals(user.getDistrictId());
                return regionId.equals(user.getRegionId());
            case "LIVE_CLASS":
                return liveClassRepository.findById(result.getId())
                        .map(lc -> lc.getInstitutionId() != null
                                && scopeInstitutionIds.contains(lc.getInstitutionId()))
                        .orElse(false);
            default:
                // Certificates and other platform-level records carry no
                // jurisdiction — hide them from scoped authority queries.
                return false;
        }
    }

    // ------------------------------------------------------------------ reports

    public static final Set<String> REPORT_IDS = Set.of(
            "school-performance", "attendance-report", "teacher-activity", "student-statistics",
            "assessment-report", "curriculum-progress", "live-class-activity", "school-comparison");

    /**
     * Real CSV export for a governance report. Rows are produced from the same
     * aggregations the oversight pages render — no placeholder data.
     */
    public String exportReport(String reportId, UUID regionId, UUID districtId) {
        if (!REPORT_IDS.contains(reportId)) {
            throw new ResourceNotFoundException("Report", "id", reportId);
        }
        List<UUID> institutionIds = oversightService.getInstitutionIdsInJurisdiction(regionId, districtId);

        return switch (reportId) {
            case "school-performance" -> csv(reportId,
                    performanceCsv(oversightService.getPerformance(regionId, districtId)));
            case "attendance-report" -> csv(reportId,
                    attendanceCsv(oversightService.getAttendance(regionId, districtId)));
            case "assessment-report" -> csv(reportId,
                    assessmentCsv(oversightService.getAssessments(regionId, districtId)));
            case "curriculum-progress" -> csv(reportId,
                    curriculumCsv(oversightService.getCurriculum(regionId, districtId)));
            case "live-class-activity" -> csv(reportId,
                    liveClassCsv(oversightService.getLiveClasses(regionId, districtId)));
            case "teacher-activity" -> csv(reportId, teacherActivityCsv(institutionIds));
            case "student-statistics" -> csv(reportId, studentStatisticsCsv(institutionIds));
            case "school-comparison" -> csv(reportId, schoolComparisonCsv(
                    oversightService.getPerformance(regionId, districtId),
                    oversightService.getAttendance(regionId, districtId),
                    oversightService.getCurriculum(regionId, districtId)));
            default -> throw new ResourceNotFoundException("Report", "id", reportId);
        };
    }

    private List<String[]> performanceCsv(PerformanceResponse performance) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Institution", "Code", "Students", "Average Score", "Pass Rate %", "Assessments"});
        performance.getSchoolPerformance().forEach(s -> rows.add(new String[]{
                s.getInstitutionName(), s.getInstitutionCode(),
                String.valueOf(s.getStudentCount()),
                String.valueOf(s.getAverageScore()),
                String.valueOf(s.getPassRate()),
                String.valueOf(s.getAssessmentCount())}));
        return rows;
    }

    private List<String[]> attendanceCsv(AttendanceResponse attendance) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Institution", "Code", "Students", "Attendance Rate %", "Students Below 75%", "Late Count"});
        attendance.getSchoolAttendance().forEach(s -> rows.add(new String[]{
                s.getInstitutionName(), s.getInstitutionCode(),
                String.valueOf(s.getStudentCount()),
                String.valueOf(s.getAttendanceRate()),
                String.valueOf(s.getAbsentCount()),
                String.valueOf(s.getLateCount())}));
        return rows;
    }

    private List<String[]> assessmentCsv(AssessmentsResponse assessments) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Institution", "Code", "Assessments", "Completed", "Average Score", "Pass Rate %"});
        assessments.getSchoolAssessments().forEach(s -> rows.add(new String[]{
                s.getInstitutionName(), s.getInstitutionCode(),
                String.valueOf(s.getAssessmentCount()),
                String.valueOf(s.getCompletedCount()),
                String.valueOf(s.getAverageScore()),
                String.valueOf(s.getPassRate())}));
        return rows;
    }

    private List<String[]> curriculumCsv(CurriculumResponse curriculum) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Institution", "Code", "Lessons", "Published", "Progress %", "Status"});
        curriculum.getSchoolProgress().forEach(s -> rows.add(new String[]{
                s.getInstitutionName(), s.getInstitutionCode(),
                String.valueOf(s.getTotalLessons()),
                String.valueOf(s.getCompletedLessons()),
                String.valueOf(s.getProgressRate()),
                s.getStatus()}));
        return rows;
    }

    private List<String[]> liveClassCsv(LiveClassesResponse liveClasses) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Title", "Institution", "Subject", "Teacher", "Scheduled At", "Duration (min)", "Status"});
        liveClasses.getLiveClasses().forEach(l -> rows.add(new String[]{
                l.getTitle(), l.getInstitutionName(), l.getSubjectName(), l.getTeacherName(),
                l.getScheduledAt(), String.valueOf(l.getDurationMinutes()), l.getStatus()}));
        return rows;
    }

    private List<String[]> teacherActivityCsv(List<UUID> institutionIds) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Institution", "Teachers", "Lessons", "Live Classes"});
        for (UUID id : institutionIds) {
            List<UUID> single = List.of(id);
            String name = institutionRepository.findById(id).map(Institution::getName).orElse("Unknown");
            long teachers = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(
                    single, InstitutionMembership.Role.TEACHER);
            long lessons = lessonRepository.countByInstitutionIdsAndIsDeletedFalse(single);
            long live = liveClassRepository.findByInstitutionIdsAndIsDeletedFalseOrderByScheduledAt(single).size();
            rows.add(new String[]{name, String.valueOf(teachers), String.valueOf(lessons), String.valueOf(live)});
        }
        return rows;
    }

    private List<String[]> studentStatisticsCsv(List<UUID> institutionIds) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Institution", "Teachers", "Students", "Total Members", "Classes"});
        for (UUID id : institutionIds) {
            List<UUID> single = List.of(id);
            String name = institutionRepository.findById(id).map(Institution::getName).orElse("Unknown");
            rows.add(new String[]{
                    name,
                    String.valueOf(membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(
                            single, InstitutionMembership.Role.TEACHER)),
                    String.valueOf(membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(
                            single, InstitutionMembership.Role.STUDENT)),
                    String.valueOf(membershipRepository.countByInstitutionIdsAndIsDeletedFalse(single)),
                    String.valueOf(classGroupRepository.countByInstitutionIdsAndIsDeletedFalse(single))});
        }
        return rows;
    }

    private List<String[]> schoolComparisonCsv(PerformanceResponse performance,
                                               AttendanceResponse attendance,
                                               CurriculumResponse curriculum) {
        Map<String, AttendanceResponse.SchoolAttendance> attendanceByInstitution =
                attendance.getSchoolAttendance().stream()
                        .collect(Collectors.toMap(
                                AttendanceResponse.SchoolAttendance::getInstitutionId, s -> s, (a, b) -> a));
        Map<String, CurriculumResponse.SchoolCurriculumProgress> curriculumByInstitution =
                curriculum.getSchoolProgress().stream()
                        .collect(Collectors.toMap(
                                CurriculumResponse.SchoolCurriculumProgress::getInstitutionId, s -> s, (a, b) -> a));

        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"Institution", "Code", "Students", "Average Score", "Pass Rate %",
                "Attendance Rate %", "Curriculum Progress %"});
        performance.getSchoolPerformance().forEach(s -> {
            AttendanceResponse.SchoolAttendance att = attendanceByInstitution.get(s.getInstitutionId());
            CurriculumResponse.SchoolCurriculumProgress cur = curriculumByInstitution.get(s.getInstitutionId());
            rows.add(new String[]{
                    s.getInstitutionName(), s.getInstitutionCode(),
                    String.valueOf(s.getStudentCount()),
                    String.valueOf(s.getAverageScore()),
                    String.valueOf(s.getPassRate()),
                    att != null ? String.valueOf(att.getAttendanceRate()) : "0.0",
                    cur != null ? String.valueOf(cur.getProgressRate()) : "0.0"});
        });
        return rows;
    }

    private String csv(String reportId, List<String[]> rows) {
        StringBuilder sb = new StringBuilder();
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(escapeCsv(row[i]));
            }
            sb.append("\r\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
