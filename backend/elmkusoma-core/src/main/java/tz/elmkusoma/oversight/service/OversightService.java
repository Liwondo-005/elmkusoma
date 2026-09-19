package tz.elmkusoma.oversight.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.AcademicYearRepository;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.academic.repository.TermRepository;
import tz.elmkusoma.assessment.domain.Assessment;
import tz.elmkusoma.assessment.repository.AssessmentRepository;
import tz.elmkusoma.attendance.domain.AttendanceSummary;
import tz.elmkusoma.attendance.repository.AttendanceSummaryRepository;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.grading.domain.ReportCard;
import tz.elmkusoma.grading.repository.ReportCardRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OversightService {

    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final InstitutionMembershipRepository membershipRepository;
    private final ClassGroupRepository classGroupRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TermRepository termRepository;
    private final AttendanceSummaryRepository attendanceSummaryRepository;
    private final ReportCardRepository reportCardRepository;
    private final LessonRepository lessonRepository;
    private final LiveClassRepository liveClassRepository;
    private final AssessmentRepository assessmentRepository;
    private final SubjectRepository subjectRepository;

    public OversightDashboardResponse getDashboard(UUID regionId, UUID districtId) {
        String jurisdictionType;
        String jurisdictionName = "";
        String jurisdictionCode = "";

        if (districtId != null) {
            jurisdictionType = "district";
            District district = districtRepository.findById(districtId).orElse(null);
            if (district != null) {
                jurisdictionName = district.getName();
                jurisdictionCode = district.getCode();
            }
        } else if (regionId != null) {
            jurisdictionType = "region";
            Region region = regionRepository.findById(regionId).orElse(null);
            if (region != null) {
                jurisdictionName = region.getName();
                jurisdictionCode = region.getCode();
            }
        } else {
            jurisdictionType = "national";
            jurisdictionName = "Tanzania";
            jurisdictionCode = "TZA";
        }

        UUID effectiveRegionId = regionId;
        UUID effectiveDistrictId = districtId;

        List<UUID> institutionIds = getInstitutionIdsInJurisdiction(effectiveRegionId, effectiveDistrictId);

        long totalInstitutions = institutionIds.size();
        long totalTeachers = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(institutionIds, InstitutionMembership.Role.TEACHER);
        long totalStudents = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(institutionIds, InstitutionMembership.Role.STUDENT);
        long totalUsers = membershipRepository.countByInstitutionIdsAndIsDeletedFalse(institutionIds);

        long totalClasses = classGroupRepository.countByInstitutionIdsAndIsDeletedFalse(institutionIds);
        long totalLessons = lessonRepository.countByInstitutionIdsAndIsDeletedFalse(institutionIds);
        long activeLiveClasses = liveClassRepository.countByInstitutionIdsAndStatusAndIsDeletedFalse(institutionIds, "IN_PROGRESS");

        long totalRegions = jurisdictionType.equals("national") ? regionRepository.count() : 1;
        long totalDistricts = jurisdictionType.equals("national") ? districtRepository.count() :
                jurisdictionType.equals("region") ? districtRepository.countByRegionIdAndIsDeletedFalse(effectiveRegionId) : 1;

        Double attendanceRate = calculateAttendanceRate(institutionIds);
        Double averagePerformance = calculateAveragePerformance(institutionIds);
        Double curriculumProgress = calculateCurriculumProgress(institutionIds);

        List<OversightAlert> recentAlerts = generateAlerts(institutionIds);
        long alertsCount = (long) recentAlerts.size();

        List<TopRegionStats> topRegions = getTopRegions(effectiveRegionId, institutionIds);

        JurisdictionSummary jurisdictionSummary = JurisdictionSummary.builder()
                .type(jurisdictionType)
                .name(jurisdictionName)
                .code(jurisdictionCode)
                .build();

        return OversightDashboardResponse.builder()
                .totalInstitutions(totalInstitutions)
                .totalTeachers(totalTeachers)
                .totalStudents(totalStudents)
                .totalUsers(totalUsers)
                .totalClasses(totalClasses)
                .totalLessons(totalLessons)
                .activeLiveClasses(activeLiveClasses)
                .totalRegions(totalRegions)
                .totalDistricts(totalDistricts)
                .attendanceRate(attendanceRate)
                .averagePerformance(averagePerformance)
                .curriculumProgress(curriculumProgress)
                .alertsCount(alertsCount)
                .jurisdictionSummary(jurisdictionSummary)
                .topRegions(topRegions)
                .recentAlerts(recentAlerts)
                .build();
    }

    private List<UUID> getInstitutionIdsInJurisdiction(UUID regionId, UUID districtId) {
        if (districtId != null) {
            return institutionRepository.findByDistrictIdAndIsDeletedFalse(districtId)
                    .stream().map(i -> i.getId()).collect(Collectors.toList());
        } else if (regionId != null) {
            return institutionRepository.findByRegionIdAndIsDeletedFalse(regionId)
                    .stream().map(i -> i.getId()).collect(Collectors.toList());
        } else {
            return institutionRepository.findByIsDeletedFalse()
                    .stream().map(i -> i.getId()).collect(Collectors.toList());
        }
    }

    private Double calculateAttendanceRate(List<UUID> institutionIds) {
        if (institutionIds.isEmpty()) return 0.0;

        UUID currentTermId = getCurrentTermId();
        if (currentTermId == null) return 0.0;

        List<AttendanceSummary> summaries = attendanceSummaryRepository.findByInstitutionIdsAndTermIdAndIsDeletedFalse(institutionIds, currentTermId);
        if (summaries.isEmpty()) return 0.0;

        double totalPercentage = summaries.stream()
                .mapToDouble(s -> s.getAttendancePercentage() != null ? s.getAttendancePercentage().doubleValue() : 0.0)
                .sum();
        return Math.round(totalPercentage / summaries.size() * 10.0) / 10.0;
    }

    private Double calculateAveragePerformance(List<UUID> institutionIds) {
        if (institutionIds.isEmpty()) return 0.0;

        UUID currentTermId = getCurrentTermId();
        if (currentTermId == null) return 0.0;

        List<ReportCard> reportCards = reportCardRepository.findByInstitutionIdsAndTermIdAndIsDeletedFalse(institutionIds, currentTermId);
        if (reportCards.isEmpty()) return 0.0;

        double totalAverage = reportCards.stream()
                .mapToDouble(rc -> rc.getAverageMark() != null ? rc.getAverageMark().doubleValue() : 0.0)
                .sum();
        return Math.round(totalAverage / reportCards.size() * 10.0) / 10.0;
    }

    private Double calculateCurriculumProgress(List<UUID> institutionIds) {
        if (institutionIds.isEmpty()) return 0.0;

        long totalLessons = lessonRepository.countByInstitutionIdsAndIsDeletedFalse(institutionIds);
        long completedLessons = lessonRepository.countByInstitutionIdsAndPublishedAndIsDeletedFalse(institutionIds, true);

        if (totalLessons == 0) return 0.0;
        return Math.round((double) completedLessons / totalLessons * 1000.0) / 10.0;
    }

    private UUID getCurrentTermId() {
        return termRepository.findCurrentTerm(java.time.LocalDate.now())
                .map(t -> t.getId())
                .orElse(null);
    }

    private List<OversightAlert> generateAlerts(List<UUID> institutionIds) {
        List<OversightAlert> alerts = new ArrayList<>();
        UUID currentTermId = getCurrentTermId();
        if (currentTermId == null) return alerts;

        List<AttendanceSummary> lowAttendance = attendanceSummaryRepository.findStudentsWithLowAttendance(currentTermId, 75.0);
        for (AttendanceSummary summary : lowAttendance) {
            if (institutionIds.contains(summary.getInstitutionId())) {
                String institutionName = institutionRepository.findById(summary.getInstitutionId())
                        .map(i -> i.getName())
                        .orElse("Unknown");
                alerts.add(OversightAlert.builder()
                        .id(UUID.randomUUID().toString())
                        .type("LOW_ATTENDANCE")
                        .message("Student attendance below 75%")
                        .severity("HIGH")
                        .institutionName(institutionName)
                        .institutionId(summary.getInstitutionId().toString())
                        .timestamp(LocalDateTime.now())
                        .build());
            }
        }

        List<ReportCard> lowPerformance = reportCardRepository.findByInstitutionIdsAndTermIdAndAverageMarkBelow(institutionIds, currentTermId, 40.0);
        for (ReportCard rc : lowPerformance) {
            String institutionName = institutionRepository.findById(rc.getInstitutionId())
                    .map(i -> i.getName())
                    .orElse("Unknown");
            alerts.add(OversightAlert.builder()
                    .id(UUID.randomUUID().toString())
                    .type("LOW_PERFORMANCE")
                    .message("Student average below 40%")
                    .severity("HIGH")
                    .institutionName(institutionName)
                    .institutionId(rc.getInstitutionId().toString())
                    .timestamp(LocalDateTime.now())
                    .build());
        }

        return alerts.stream().limit(10).collect(Collectors.toList());
    }

    private List<TopRegionStats> getTopRegions(UUID effectiveRegionId, List<UUID> institutionIds) {
        List<Region> regions;
        if (effectiveRegionId != null) {
            Region r = regionRepository.findById(effectiveRegionId).orElse(null);
            regions = r != null ? List.of(r) : List.of();
        } else {
            regions = regionRepository.findByIsDeletedFalseOrderByCreatedAtDesc();
        }

        return regions.stream()
                .limit(10)
                .map(r -> {
                    List<UUID> regionInstitutionIds = institutionRepository.findByRegionIdAndIsDeletedFalse(r.getId())
                            .stream().map(i -> i.getId()).collect(Collectors.toList());
                    long instCount = regionInstitutionIds.size();
                    long teacherCount = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(regionInstitutionIds, InstitutionMembership.Role.TEACHER);
                    long studentCount = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(regionInstitutionIds, InstitutionMembership.Role.STUDENT);
                    Double attendanceRate = instCount > 0 ? calculateAttendanceRate(regionInstitutionIds) : 0.0;
                    Double avgPerformance = instCount > 0 ? calculateAveragePerformance(regionInstitutionIds) : 0.0;

                    return TopRegionStats.builder()
                            .regionName(r.getName())
                            .regionCode(r.getCode())
                            .institutionCount(instCount)
                            .teacherCount(teacherCount)
                            .studentCount(studentCount)
                            .attendanceRate(attendanceRate)
                            .averagePerformance(avgPerformance)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<RegionResponse> getAllRegions() {
        return regionRepository.findByIsDeletedFalseOrderByCreatedAtDesc().stream()
                .map(r -> {
                    long instCount = institutionRepository.countByRegionIdAndIsDeletedFalse(r.getId());
                    long teacherCount = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(
                            institutionRepository.findByRegionIdAndIsDeletedFalse(r.getId()).stream().map(i -> i.getId()).collect(Collectors.toList()),
                            InstitutionMembership.Role.TEACHER);
                    long studentCount = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(
                            institutionRepository.findByRegionIdAndIsDeletedFalse(r.getId()).stream().map(i -> i.getId()).collect(Collectors.toList()),
                            InstitutionMembership.Role.STUDENT);
                    return RegionResponse.builder()
                            .id(r.getId())
                            .name(r.getName())
                            .code(r.getCode())
                            .isActive(r.getIsActive())
                            .institutionCount(instCount)
                            .teacherCount(teacherCount)
                            .studentCount(studentCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<DistrictResponse> getDistrictsByRegion(UUID regionId) {
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RuntimeException("Region not found"));
        return districtRepository.findByRegionIdAndIsDeletedFalse(regionId).stream()
                .map(d -> {
                    List<UUID> instIds = institutionRepository.findByDistrictIdAndIsDeletedFalse(d.getId())
                            .stream().map(i -> i.getId()).collect(Collectors.toList());
                    long instCount = instIds.size();
                    long teacherCount = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(instIds, InstitutionMembership.Role.TEACHER);
                    long studentCount = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(instIds, InstitutionMembership.Role.STUDENT);
                    return DistrictResponse.builder()
                            .id(d.getId())
                            .name(d.getName())
                            .code(d.getCode())
                            .regionId(d.getRegionId())
                            .regionName(region.getName())
                            .isActive(d.getIsActive())
                            .institutionCount(instCount)
                            .teacherCount(teacherCount)
                            .studentCount(studentCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<InstitutionSummary> getInstitutionsByRegion(UUID regionId) {
        return institutionRepository.findByRegionIdAndIsDeletedFalse(regionId).stream()
                .map(inst -> {
                    List<UUID> instIds = List.of(inst.getId());
                    long teacherCount = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(instIds, InstitutionMembership.Role.TEACHER);
                    long studentCount = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(instIds, InstitutionMembership.Role.STUDENT);
                    return InstitutionSummary.builder()
                            .id(inst.getId())
                            .name(inst.getName())
                            .code(inst.getCode())
                            .type(inst.getType().name())
                            .isActive(inst.getIsActive())
                            .teacherCount(teacherCount)
                            .studentCount(studentCount)
                            .districtName(inst.getDistrictId() != null ? districtRepository.findById(inst.getDistrictId()).map(District::getName).orElse("") : "")
                            .regionName(inst.getRegionId() != null ? regionRepository.findById(inst.getRegionId()).map(Region::getName).orElse("") : "")
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<InstitutionSummary> getInstitutionsByDistrict(UUID districtId) {
        return institutionRepository.findByDistrictIdAndIsDeletedFalse(districtId).stream()
                .map(inst -> {
                    List<UUID> instIds = List.of(inst.getId());
                    long teacherCount = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(instIds, InstitutionMembership.Role.TEACHER);
                    long studentCount = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(instIds, InstitutionMembership.Role.STUDENT);
                    return InstitutionSummary.builder()
                            .id(inst.getId())
                            .name(inst.getName())
                            .code(inst.getCode())
                            .type(inst.getType().name())
                            .isActive(inst.getIsActive())
                            .teacherCount(teacherCount)
                            .studentCount(studentCount)
                            .districtName(districtRepository.findById(districtId).map(District::getName).orElse(""))
                            .regionName(inst.getRegionId() != null ? regionRepository.findById(inst.getRegionId()).map(Region::getName).orElse("") : "")
                            .build();
                })
                .collect(Collectors.toList());
    }

    public InstitutionDetailResponse getInstitutionDetail(UUID institutionId) {
        return institutionRepository.findById(institutionId)
                .map(inst -> {
                    List<UUID> instIds = List.of(inst.getId());
                    long teacherCount = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(instIds, InstitutionMembership.Role.TEACHER);
                    long studentCount = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(instIds, InstitutionMembership.Role.STUDENT);
                    long classCount = classGroupRepository.countByInstitutionIdsAndIsDeletedFalse(instIds);
                    long lessonCount = lessonRepository.countByInstitutionIdsAndIsDeletedFalse(instIds);
                    long activeLiveClasses = liveClassRepository.countByInstitutionIdsAndStatusAndIsDeletedFalse(instIds, "IN_PROGRESS");

                    UUID currentTermId = getCurrentTermId();
                    Double attendanceRate = currentTermId != null ? calculateAttendanceRate(instIds) : 0.0;
                    Double averagePerformance = currentTermId != null ? calculateAveragePerformance(instIds) : 0.0;
                    Double curriculumProgress = currentTermId != null ? calculateCurriculumProgress(instIds) : 0.0;

                    return InstitutionDetailResponse.builder()
                            .id(inst.getId())
                            .name(inst.getName())
                            .code(inst.getCode())
                            .type(inst.getType().name())
                            .isActive(inst.getIsActive())
                            .teacherCount(teacherCount)
                            .studentCount(studentCount)
                            .classCount(classCount)
                            .lessonCount(lessonCount)
                            .activeLiveClasses(activeLiveClasses)
                            .attendanceRate(attendanceRate)
                            .averagePerformance(averagePerformance)
                            .curriculumProgress(curriculumProgress)
                            .address(inst.getAddress())
                            .city(inst.getCity())
                            .regionName(inst.getRegionId() != null ? regionRepository.findById(inst.getRegionId()).map(Region::getName).orElse("") : "")
                            .districtName(inst.getDistrictId() != null ? districtRepository.findById(inst.getDistrictId()).map(District::getName).orElse("") : "")
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Institution not found"));
    }

    public PerformanceResponse getPerformance(UUID regionId, UUID districtId) {
        List<UUID> institutionIds = getInstitutionIdsInJurisdiction(regionId, districtId);
        UUID currentTermId = getCurrentTermId();

        Double overallAverage = currentTermId != null ? calculateAveragePerformance(institutionIds) : 0.0;
        Double passRate = currentTermId != null ? calculatePassRate(institutionIds, currentTermId) : 0.0;

        List<ReportCard> reportCards = currentTermId != null
                ? reportCardRepository.findByInstitutionIdsAndTermIdAndIsDeletedFalse(institutionIds, currentTermId)
                : List.of();
        Long totalReportCards = (long) reportCards.size();

        // Get assessments count
        Long totalAssessments = assessmentRepository.countByInstitutionIdsAndIsDeletedFalse(institutionIds);

        List<PerformanceResponse.SchoolPerformance> schoolPerformance = institutionIds.stream()
                .map(instId -> {
                    List<UUID> ids = List.of(instId);
                    Double avg = currentTermId != null ? calculateAveragePerformance(ids) : 0.0;
                    Double pr = currentTermId != null ? calculatePassRate(ids, currentTermId) : 0.0;
                    Long students = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(ids, InstitutionMembership.Role.STUDENT);
                    Long assessments = assessmentRepository.countByInstitutionIdsAndIsDeletedFalse(ids);
                    String name = institutionRepository.findById(instId).map(i -> i.getName()).orElse("Unknown");
                    String code = institutionRepository.findById(instId).map(i -> i.getCode()).orElse("");

                    return PerformanceResponse.SchoolPerformance.builder()
                            .institutionId(instId.toString())
                            .institutionName(name)
                            .institutionCode(code)
                            .averageScore(avg)
                            .passRate(pr)
                            .studentCount(students)
                            .assessmentCount(assessments)
                            .build();
                })
                .collect(Collectors.toList());

        List<PerformanceResponse.SubjectPerformance> subjectPerformance = subjectRepository.findByInstitutionIdsAndIsDeletedFalse(institutionIds).stream()
                .map(subj -> {
                    Long assessments = assessmentRepository.countBySubjectIdAndInstitutionIdsAndIsDeletedFalse(subj.getId(), institutionIds);
                    Double avg = assessments > 0 ? calculateSubjectAverage(subj.getId(), institutionIds, currentTermId) : 0.0;
                    Double pr = assessments > 0 ? calculateSubjectPassRate(subj.getId(), institutionIds, currentTermId) : 0.0;

                    return PerformanceResponse.SubjectPerformance.builder()
                            .subjectName(subj.getName())
                            .subjectCode(subj.getCode())
                            .averageScore(avg)
                            .passRate(pr)
                            .assessmentCount(assessments)
                            .build();
                })
                .collect(Collectors.toList());

        return PerformanceResponse.builder()
                .overallAverage(overallAverage)
                .passRate(passRate)
                .totalAssessments(totalAssessments)
                .totalReportCards(totalReportCards)
                .schoolPerformance(schoolPerformance)
                .subjectPerformance(subjectPerformance)
                .build();
    }

    public AttendanceResponse getAttendance(UUID regionId, UUID districtId) {
        List<UUID> institutionIds = getInstitutionIdsInJurisdiction(regionId, districtId);
        UUID currentTermId = getCurrentTermId();

        Double overallRate = currentTermId != null ? calculateAttendanceRate(institutionIds) : 0.0;
        Long totalStudents = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(institutionIds, InstitutionMembership.Role.STUDENT);
        Long schoolsAtRisk = institutionRepository.countByInstitutionIdsAndAttendanceBelow(institutionIds, 75.0);

        List<AttendanceResponse.SchoolAttendance> schoolAttendance = institutionIds.stream()
                .map(instId -> {
                    List<UUID> ids = List.of(instId);
                    Double rate = currentTermId != null ? calculateAttendanceRate(ids) : 0.0;
                    Long students = membershipRepository.countByInstitutionIdsAndRoleAndIsDeletedFalse(ids, InstitutionMembership.Role.STUDENT);
                    Long absent = attendanceSummaryRepository.countByInstitutionIdsAndTermIdAndAttendanceBelow(ids, currentTermId, 75.0);
                    Long late = attendanceSummaryRepository.countByInstitutionIdsAndTermIdAndStatus(ids, currentTermId, "LATE");
                    String name = institutionRepository.findById(instId).map(i -> i.getName()).orElse("Unknown");
                    String code = institutionRepository.findById(instId).map(i -> i.getCode()).orElse("");

                    return AttendanceResponse.SchoolAttendance.builder()
                            .institutionId(instId.toString())
                            .institutionName(name)
                            .institutionCode(code)
                            .studentCount(students)
                            .attendanceRate(rate)
                            .absentCount(absent)
                            .lateCount(late)
                            .build();
                })
                .collect(Collectors.toList());

        List<AttendanceResponse.LowAttendanceStudent> lowAttendanceStudents = new ArrayList<>();
        if (currentTermId != null) {
            List<AttendanceSummary> low = attendanceSummaryRepository.findStudentsWithLowAttendance(currentTermId, 75.0);
            for (AttendanceSummary summary : low) {
                if (institutionIds.contains(summary.getInstitutionId())) {
                    String instName = institutionRepository.findById(summary.getInstitutionId()).map(i -> i.getName()).orElse("Unknown");
                    lowAttendanceStudents.add(AttendanceResponse.LowAttendanceStudent.builder()
                            .studentId(summary.getStudentId().toString())
                            .studentName("Student " + summary.getStudentId().toString().substring(0, 8))
                            .institutionName(instName)
                            .attendanceRate(summary.getAttendancePercentage() != null ? summary.getAttendancePercentage().doubleValue() : 0.0)
                            .daysAbsent(summary.getDaysAbsent() != null ? summary.getDaysAbsent().longValue() : 0L)
                            .build());
                }
            }
        }

        return AttendanceResponse.builder()
                .overallRate(overallRate)
                .totalStudents(totalStudents)
                .schoolsAtRisk(schoolsAtRisk)
                .dailyTrend(List.of())
                .schoolAttendance(schoolAttendance)
                .lowAttendanceStudents(lowAttendanceStudents)
                .build();
    }

    public CurriculumResponse getCurriculum(UUID regionId, UUID districtId) {
        List<UUID> institutionIds = getInstitutionIdsInJurisdiction(regionId, districtId);
        UUID currentTermId = getCurrentTermId();

        Long totalLessons = lessonRepository.countByInstitutionIdsAndIsDeletedFalse(institutionIds);
        Long completedLessons = lessonRepository.countByInstitutionIdsAndPublishedAndIsDeletedFalse(institutionIds, true);

        Double overallProgress = totalLessons > 0 ? Math.round((double) completedLessons / totalLessons * 1000.0) / 10.0 : 0.0;

        Long schoolsOnTrack = 0L;
        Long schoolsBehind = 0L;

        List<CurriculumResponse.SchoolCurriculumProgress> schoolProgress = institutionIds.stream()
                .map(instId -> {
                    List<UUID> ids = List.of(instId);
                    Long total = lessonRepository.countByInstitutionIdsAndIsDeletedFalse(ids);
                    Long completed = lessonRepository.countByInstitutionIdsAndPublishedAndIsDeletedFalse(ids, true);
                    Double progress = total > 0 ? Math.round((double) completed / total * 1000.0) / 10.0 : 0.0;
                    String status = progress >= 80 ? "ON_TRACK" : progress >= 50 ? "BEHIND" : "AT_RISK";
                    String name = institutionRepository.findById(instId).map(i -> i.getName()).orElse("Unknown");
                    String code = institutionRepository.findById(instId).map(i -> i.getCode()).orElse("");

                    return CurriculumResponse.SchoolCurriculumProgress.builder()
                            .institutionId(instId.toString())
                            .institutionName(name)
                            .institutionCode(code)
                            .totalLessons(total)
                            .completedLessons(completed)
                            .progressRate(progress)
                            .status(status)
                            .build();
                })
                .collect(Collectors.toList());

        schoolsOnTrack = schoolProgress.stream().filter(s -> "ON_TRACK".equals(s.getStatus())).count();
        schoolsBehind = schoolProgress.stream().filter(s -> !"ON_TRACK".equals(s.getStatus())).count();

        List<CurriculumResponse.SubjectProgress> subjectProgress = subjectRepository.findByInstitutionIdsAndIsDeletedFalse(institutionIds).stream()
                .map(subj -> {
                    Long total = lessonRepository.countByInstitutionIdsAndSubjectIdAndIsDeletedFalse(institutionIds, subj.getId());
                    Long completed = lessonRepository.countByInstitutionIdsAndSubjectIdAndPublishedAndIsDeletedFalse(institutionIds, subj.getId(), true);
                    Double progress = total > 0 ? Math.round((double) completed / total * 1000.0) / 10.0 : 0.0;

                    return CurriculumResponse.SubjectProgress.builder()
                            .subjectName(subj.getName())
                            .subjectCode(subj.getCode())
                            .totalLessons(total)
                            .completedLessons(completed)
                            .progressRate(progress)
                            .build();
                })
                .collect(Collectors.toList());

        return CurriculumResponse.builder()
                .overallProgress(overallProgress)
                .totalLessons(totalLessons)
                .completedLessons(completedLessons)
                .schoolsOnTrack(schoolsOnTrack)
                .schoolsBehind(schoolsBehind)
                .subjectProgress(subjectProgress)
                .schoolProgress(schoolProgress)
                .build();
    }

    public AssessmentsResponse getAssessments(UUID regionId, UUID districtId) {
        List<UUID> institutionIds = getInstitutionIdsInJurisdiction(regionId, districtId);
        UUID currentTermId = getCurrentTermId();

        Long totalAssessments = assessmentRepository.countByInstitutionIdsAndIsDeletedFalse(institutionIds);
        Long completedAssessments = assessmentRepository.countByInstitutionIdsAndStatusAndIsDeletedFalse(institutionIds, "COMPLETED");
        Double averageScore = currentTermId != null ? calculateAveragePerformance(institutionIds) : 0.0;
        Double passRate = currentTermId != null ? calculatePassRate(institutionIds, currentTermId) : 0.0;
        Long pendingGrading = assessmentRepository.countByInstitutionIdsAndStatusAndIsDeletedFalse(institutionIds, "PENDING_GRADING");

        List<AssessmentsResponse.SchoolAssessment> schoolAssessments = institutionIds.stream()
                .map(instId -> {
                    List<UUID> ids = List.of(instId);
                    Long count = assessmentRepository.countByInstitutionIdsAndIsDeletedFalse(ids);
                    Long completed = assessmentRepository.countByInstitutionIdsAndStatusAndIsDeletedFalse(ids, "COMPLETED");
                    Double avg = currentTermId != null ? calculateAveragePerformance(ids) : 0.0;
                    Double pr = currentTermId != null ? calculatePassRate(ids, currentTermId) : 0.0;
                    String name = institutionRepository.findById(instId).map(i -> i.getName()).orElse("Unknown");
                    String code = institutionRepository.findById(instId).map(i -> i.getCode()).orElse("");

                    return AssessmentsResponse.SchoolAssessment.builder()
                            .institutionId(instId.toString())
                            .institutionName(name)
                            .institutionCode(code)
                            .assessmentCount(count)
                            .completedCount(completed)
                            .averageScore(avg)
                            .passRate(pr)
                            .build();
                })
                .collect(Collectors.toList());

        List<AssessmentsResponse.RecentAssessment> recentAssessments = assessmentRepository.findRecentByInstitutionIds(institutionIds, 10).stream()
                .map(a -> {
                    UUID instId = a.getInstitutionId();
                    String instName = instId != null ? institutionRepository.findById(instId).map(Institution::getName).orElse("Unknown") : "Unknown";
                    String subjName = a.getSubjectId() != null ? subjectRepository.findById(a.getSubjectId()).map(Subject::getName).orElse("Unknown") : "Unknown";
                    return AssessmentsResponse.RecentAssessment.builder()
                            .id(a.getId().toString())
                            .title(a.getTitle())
                            .institutionName(instName)
                            .subjectName(subjName)
                            .scheduledDate(a.getStartsAt() != null ? a.getStartsAt().toString() : "")
                            .status(Boolean.TRUE.equals(a.getIsPublished()) ? "PUBLISHED" : "DRAFT")
                            .participantCount(0L)
                            .build();
                })
                .collect(Collectors.toList());

        return AssessmentsResponse.builder()
                .totalAssessments(totalAssessments)
                .completedAssessments(completedAssessments)
                .averageScore(averageScore)
                .passRate(passRate)
                .pendingGrading(pendingGrading)
                .schoolAssessments(schoolAssessments)
                .recentAssessments(recentAssessments)
                .build();
    }

    public LiveClassesResponse getLiveClasses(UUID regionId, UUID districtId) {
        List<UUID> institutionIds = getInstitutionIdsInJurisdiction(regionId, districtId);

        LocalDate today = LocalDate.now();
        LocalDateTime weekStart = today.with(DayOfWeek.MONDAY).atStartOfDay();
        LocalDateTime weekEnd = weekStart.plusWeeks(1).minusNanos(1);

        Long liveNow = liveClassRepository.countByInstitutionIdsAndStatusAndIsDeletedFalse(institutionIds, "IN_PROGRESS");
        Long scheduledToday = liveClassRepository.countByInstitutionIdsAndStatusAndScheduledToday(institutionIds, "SCHEDULED", today);
        Long completedToday = liveClassRepository.countByInstitutionIdsAndStatusAndCompletedToday(institutionIds, "COMPLETED", today);
        Long totalThisWeek = liveClassRepository.countByInstitutionIdsAndScheduledThisWeek(institutionIds, weekStart, weekEnd);

        List<LiveClassesResponse.LiveClassSummary> liveClasses = liveClassRepository.findByInstitutionIdsAndIsDeletedFalseOrderByScheduledAt(institutionIds).stream()
                .map(lc -> {
                    String instName = lc.getInstitutionId() != null
                            ? institutionRepository.findById(lc.getInstitutionId()).map(Institution::getName).orElse("Unknown")
                            : "Unknown";
                    String subjName = lc.getSubjectId() != null
                            ? subjectRepository.findById(lc.getSubjectId()).map(Subject::getName).orElse("Unknown")
                            : "Unknown";
                    String teacherName = lc.getTeacherId() != null
                            ? userRepository.findById(lc.getTeacherId()).map(User::getFullName).orElse("Unknown")
                            : "Unknown";
                    return LiveClassesResponse.LiveClassSummary.builder()
                            .id(lc.getId().toString())
                            .title(lc.getTitle())
                            .institutionName(instName)
                            .institutionId(lc.getInstitutionId().toString())
                            .subjectName(subjName)
                            .teacherName(teacherName)
                            .scheduledAt(lc.getScheduledAt().toString())
                            .durationMinutes(lc.getDurationMinutes())
                            .status(lc.getStatus())
                            .participantCount(0L)
                            .maxParticipants(lc.getMaxParticipants() != null ? lc.getMaxParticipants().longValue() : 0L)
                            .build();
                })
                .collect(Collectors.toList());

        return LiveClassesResponse.builder()
                .liveNow(liveNow)
                .scheduledToday(scheduledToday)
                .completedToday(completedToday)
                .totalThisWeek(totalThisWeek)
                .liveClasses(liveClasses)
                .build();
    }

    public List<ReportSummary> getReports() {
        return List.of(
                ReportSummary.builder().id("school-performance").title("School Performance Report")
                        .description("Comprehensive academic performance analysis across all schools in jurisdiction")
                        .type("Academic").icon("Award").color("bg-purple-500/10 text-purple-600").available(true).build(),
                ReportSummary.builder().id("attendance-report").title("Attendance Report")
                        .description("Detailed attendance analytics with trends, patterns, and at-risk identification")
                        .type("Attendance").icon("ClipboardList").color("bg-teal-500/10 text-teal-600").available(true).build(),
                ReportSummary.builder().id("teacher-activity").title("Teacher Activity Report")
                        .description("Teacher workload, class assignments, live class sessions, and engagement metrics")
                        .type("HR").icon("Users").color("bg-blue-500/10 text-blue-600").available(true).build(),
                ReportSummary.builder().id("student-statistics").title("Student Statistics Report")
                        .description("Enrollment trends, demographics, progression, and outcome analytics")
                        .type("Demographics").icon("GraduationCap").color("bg-green-500/10 text-green-600").available(true).build(),
                ReportSummary.builder().id("assessment-report").title("Assessment Report")
                        .description("Assessment activity, completion rates, score distributions, and grading analytics")
                        .type("Assessment").icon("FileText").color("bg-indigo-500/10 text-indigo-600").available(true).build(),
                ReportSummary.builder().id("curriculum-progress").title("Curriculum Progress Report")
                        .description("Lesson completion rates, topic coverage, and curriculum pacing analysis")
                        .type("Curriculum").icon("BookOpen").color("bg-pink-500/10 text-pink-600").available(true).build(),
                ReportSummary.builder().id("live-class-activity").title("Live Class Activity Report")
                        .description("Live class schedules, participation rates, teacher performance, and session analytics")
                        .type("Live Classes").icon("Video").color("bg-red-500/10 text-red-600").available(true).build(),
                ReportSummary.builder().id("school-comparison").title("School Comparison Report")
                        .description("Side-by-side school comparison across all key metrics and indicators")
                        .type("Comparison").icon("BarChart3").color("bg-orange-500/10 text-orange-600").available(true).build()
        );
    }

    public AlertsResponse getAlerts(UUID regionId, UUID districtId) {
        List<UUID> institutionIds = getInstitutionIdsInJurisdiction(regionId, districtId);
        UUID currentTermId = getCurrentTermId();

        List<AlertsResponse.Alert> alerts = new ArrayList<>();

        if (currentTermId != null) {
            List<AttendanceSummary> lowAttendance = attendanceSummaryRepository.findStudentsWithLowAttendance(currentTermId, 75.0);
            for (AttendanceSummary summary : lowAttendance) {
                if (institutionIds.contains(summary.getInstitutionId())) {
                    String instName = institutionRepository.findById(summary.getInstitutionId()).map(i -> i.getName()).orElse("Unknown");
                    alerts.add(AlertsResponse.Alert.builder()
                            .id(UUID.randomUUID().toString())
                            .type("LOW_ATTENDANCE")
                            .title("Low Attendance Alert")
                            .message("Student attendance below 75% threshold")
                            .severity("HIGH")
                            .institutionName(instName)
                            .institutionId(summary.getInstitutionId().toString())
                            .indicator("Attendance Rate")
                            .value(summary.getAttendancePercentage() != null ? String.valueOf(summary.getAttendancePercentage()) : "0")
                            .threshold("75%")
                            .timestamp(LocalDateTime.now())
                            .status("NEW")
                            .build());
                }
            }

            List<ReportCard> lowPerformance = reportCardRepository.findByInstitutionIdsAndTermIdAndAverageMarkBelow(institutionIds, currentTermId, 40.0);
            for (ReportCard rc : lowPerformance) {
                String instName = institutionRepository.findById(rc.getInstitutionId()).map(i -> i.getName()).orElse("Unknown");
                alerts.add(AlertsResponse.Alert.builder()
                        .id(UUID.randomUUID().toString())
                        .type("LOW_PERFORMANCE")
                        .title("Low Performance Alert")
                        .message("Student average below 40%")
                        .severity("HIGH")
                        .institutionName(instName)
                        .institutionId(rc.getInstitutionId().toString())
                        .indicator("Average Score")
                        .value(rc.getAverageMark() != null ? String.valueOf(rc.getAverageMark()) : "0")
                        .threshold("40%")
                        .timestamp(LocalDateTime.now())
                        .status("NEW")
                        .build());
            }
        }

        Long total = (long) alerts.size();
        Long high = alerts.stream().filter(a -> "HIGH".equals(a.getSeverity())).count();
        Long medium = alerts.stream().filter(a -> "MEDIUM".equals(a.getSeverity())).count();
        Long low = alerts.stream().filter(a -> "LOW".equals(a.getSeverity())).count();
        Long new_ = alerts.stream().filter(a -> "NEW".equals(a.getStatus())).count();
        Long acknowledged = alerts.stream().filter(a -> "ACKNOWLEDGED".equals(a.getStatus())).count();
        Long resolved = alerts.stream().filter(a -> "RESOLVED".equals(a.getStatus())).count();

        return AlertsResponse.builder()
                .alerts(alerts)
                .summary(AlertsResponse.AlertSummary.builder()
                        .total((long) alerts.size())
                        .high(high)
                        .medium(medium)
                        .low(low)
                        .new_(new_)
                        .acknowledged(acknowledged)
                        .resolved(resolved)
                        .build())
                .build();
    }

    private Double calculatePassRate(List<UUID> institutionIds, UUID termId) {
        List<ReportCard> reportCards = reportCardRepository.findByInstitutionIdsAndTermIdAndIsDeletedFalse(institutionIds, termId);
        if (reportCards.isEmpty()) return 0.0;
        long passed = reportCards.stream().filter(rc -> rc.getAverageMark() != null && rc.getAverageMark().doubleValue() >= 50.0).count();
        return Math.round((double) passed / reportCards.size() * 1000.0) / 10.0;
    }

    private Double calculateSubjectAverage(UUID subjectId, List<UUID> institutionIds, UUID termId) {
        List<Assessment> assessments = assessmentRepository.findBySubjectIdAndInstitutionIdsAndIsDeletedFalse(subjectId, institutionIds);
        if (assessments.isEmpty()) return 0.0;
        double total = assessments.stream()
                .mapToDouble(a -> a.getTotalMarks() != null ? a.getTotalMarks().doubleValue() : 0.0)
                .sum();
        return Math.round(total / assessments.size() * 10.0) / 10.0;
    }

    private Double calculateSubjectPassRate(UUID subjectId, List<UUID> institutionIds, UUID termId) {
        List<ReportCard> reportCards = reportCardRepository.findBySubjectIdAndInstitutionIdsAndTermIdAndIsDeletedFalse(subjectId, institutionIds, termId);
        if (reportCards.isEmpty()) return 0.0;
        long passed = reportCards.stream().filter(rc -> rc.getAverageMark() != null && rc.getAverageMark().doubleValue() >= 50.0).count();
        return Math.round((double) passed / reportCards.size() * 1000.0) / 10.0;
    }

    public ObserverJoinResponse getObserverJoinUrl(UUID userId, UUID liveClassId) {
        // Verify user is authority
        String role = userRepository.findById(userId)
                .map(u -> u.getRole().name())
                .orElse(null);
        if (!isAuthorityRole(role)) {
            throw new tz.elmkusoma.exception.ForbiddenException("observer", "join");
        }

        // Get live class details
        LiveClass liveClass = liveClassRepository.findById(liveClassId)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElseThrow(() -> new tz.elmkusoma.exception.ResourceNotFoundException("LiveClass", "id", liveClassId));

        // Verify live class is in progress
        if (!"IN_PROGRESS".equals(liveClass.getStatus())) {
            throw new IllegalStateException("Live class is not currently in session");
        }

        // Verify jurisdiction - observer must have access to the live class's institution
        UUID institutionId = liveClass.getInstitutionId();
        UUID userRegionId = userRepository.findById(userId).map(User::getRegionId).orElse(null);
        UUID userDistrictId = userRepository.findById(userId).map(User::getDistrictId).orElse(null);

        if (!verifyJurisdictionAccess(userId, institutionId)) {
            throw new tz.elmkusoma.exception.ForbiddenException("observer", "join");
        }

        // Build WebSocket URL - the frontend will connect to /ws/live-class/{liveClassId}?token={jwt}&role=OBSERVER
        String baseUrl = "ws://localhost:8080"; // TODO: make configurable
        String websocketUrl = baseUrl + "/ws/live-class/" + liveClassId + "?role=OBSERVER";

        String institutionName = institutionRepository.findById(institutionId)
                .map(Institution::getName).orElse("Unknown");
        String subjectName = liveClass.getSubjectId() != null
                ? subjectRepository.findById(liveClass.getSubjectId()).map(Subject::getName).orElse("Unknown")
                : "Unknown";
        String teacherName = liveClass.getTeacherId() != null
                ? userRepository.findById(liveClass.getTeacherId()).map(User::getFullName).orElse("Unknown")
                : "Unknown";

        return ObserverJoinResponse.builder()
                .websocketUrl(websocketUrl)
                .token("") // Frontend will add JWT from localStorage
                .liveClassId(liveClassId.toString())
                .title(liveClass.getTitle())
                .institutionName(institutionName)
                .subjectName(subjectName)
                .teacherName(teacherName)
                .build();
    }

    private boolean isAuthorityRole(String role) {
        return "NATIONAL_ADMIN".equals(role) ||
                "REGIONAL_ADMIN".equals(role) ||
                "DISTRICT_ADMIN".equals(role);
    }

    private boolean verifyJurisdictionAccess(UUID userId, UUID institutionId) {
        // Check if user has membership in the institution
        return membershipRepository.existsByUserIdAndInstitutionIdAndIsActiveTrue(userId, institutionId);
    }
}