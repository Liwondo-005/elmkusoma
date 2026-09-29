package tz.elmkusoma.grading.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.assessment.domain.Assessment;
import tz.elmkusoma.assessment.domain.AssessmentResult;
import tz.elmkusoma.assessment.repository.AssessmentRepository;
import tz.elmkusoma.assessment.repository.AssessmentResultRepository;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.common.ClassAccessGuard;
import tz.elmkusoma.common.exception.ResourceNotFoundException;
import tz.elmkusoma.grading.domain.GradeBoundary;
import tz.elmkusoma.grading.domain.ReportCard;
import tz.elmkusoma.grading.domain.ReportCard.ReportCardStatus;
import tz.elmkusoma.grading.dto.request.GenerateReportCardRequest;
import tz.elmkusoma.grading.dto.response.ReportCardResponse;
import tz.elmkusoma.grading.repository.GradeBoundaryRepository;
import tz.elmkusoma.grading.repository.ReportCardRepository;
import tz.elmkusoma.grading.service.GradeBoundaryService;
import tz.elmkusoma.grading.service.ReportCardService;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.learning.domain.Assignment;
import tz.elmkusoma.learning.domain.AssignmentSubmission;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.learning.repository.AssignmentSubmissionRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.academic.domain.AcademicYear;
import tz.elmkusoma.academic.repository.AcademicYearRepository;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.academic.domain.Term;
import tz.elmkusoma.academic.repository.TermRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportCardServiceImpl implements ReportCardService {

    private final ReportCardRepository reportCardRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TermRepository termRepository;
    private final ClassGroupRepository classGroupRepository;
    private final StudentClassAssignmentRepository studentClassAssignmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentResultRepository resultRepository;
    private final GradeBoundaryRepository gradeBoundaryRepository;
    private final GradeBoundaryService gradeBoundaryService;
    private final NotificationService notificationService;
    private final AuditService auditService;
    private final ClassAccessGuard classAccessGuard;

    @Override
    public ReportCardResponse generate(UUID institutionId, GenerateReportCardRequest request) {
        Term term = termRepository.findById(request.getTermId())
                .orElseThrow(() -> new ResourceNotFoundException("Term not found"));
        Student student = studentRepository.findById(request.getStudentId())
                .filter(s -> !Boolean.TRUE.equals(s.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));
        UUID classGroupId = classAccessGuard.resolveClassGroupIdForStudent(student.getId());

        // ── Aggregate the student's graded work across assignments + assessments ──
        BigDecimal obtained = BigDecimal.ZERO;
        BigDecimal obtainable = BigDecimal.ZERO;

        Map<UUID, Assignment> assignmentCache = new HashMap<>();
        for (AssignmentSubmission submission : submissionRepository.findByStudentIdAndIsDeletedFalse(student.getId())) {
            if (submission.getGrade() == null || Boolean.TRUE.equals(submission.getIsDraft())) {
                continue;
            }
            Assignment assignment = assignmentCache.computeIfAbsent(
                    submission.getAssignmentId(), id -> assignmentRepository.findById(id).orElse(null));
            if (assignment == null) {
                continue;
            }
            obtained = obtained.add(BigDecimal.valueOf(submission.getGrade()));
            obtainable = obtainable.add(BigDecimal.valueOf(
                    assignment.getTotalMarks() != null ? assignment.getTotalMarks() : 0));
        }

        Map<UUID, Assessment> assessmentCache = new HashMap<>();
        for (AssessmentResult result : resultRepository.findByStudentIdAndIsDeletedFalse(student.getId())) {
            Assessment assessment = assessmentCache.computeIfAbsent(
                    result.getAssessmentId(), id -> assessmentRepository.findById(id).orElse(null));
            if (assessment == null) {
                continue;
            }
            obtained = obtained.add(BigDecimal.valueOf(
                    result.getTotalScore() != null ? result.getTotalScore() : 0));
            obtainable = obtainable.add(BigDecimal.valueOf(
                    assessment.getTotalMarks() != null ? assessment.getTotalMarks() : 0));
        }

        // ── Derive average percentage, overall grade and GPA from the boundaries ──
        BigDecimal percentage = null;
        String overallGrade = null;
        BigDecimal gpa = null;
        if (obtainable.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal pct = obtained.multiply(BigDecimal.valueOf(100))
                    .divide(obtainable, 2, RoundingMode.HALF_UP);
            percentage = pct;
            overallGrade = gradeBoundaryService.calculateGradeForPercentage(
                    request.getGradingScaleId(), pct);
            gpa = gradeBoundaryRepository.findBoundariesByScaleId(request.getGradingScaleId())
                    .stream()
                    .filter(b -> pct.compareTo(b.getMinPercentage()) >= 0
                            && pct.compareTo(b.getMaxPercentage()) <= 0)
                    .map(GradeBoundary::getGpaPoints)
                    .findFirst()
                    .orElse(null);
        }

        // ── Upsert: regenerate for the same student + term instead of duplicating ──
        ReportCard reportCard = reportCardRepository
                .findByStudentIdAndTermIdAndIsDeletedFalse(request.getStudentId(), request.getTermId())
                .orElseGet(() -> ReportCard.builder()
                        .institutionId(institutionId)
                        .studentId(request.getStudentId())
                        .termId(request.getTermId())
                        .status(ReportCardStatus.DRAFT)
                        .build());

        reportCard.setAcademicYearId(term.getAcademicYearId());
        reportCard.setGradingScaleId(request.getGradingScaleId());
        reportCard.setClassGroupId(classGroupId);
        reportCard.setTotalMarks(obtained);
        reportCard.setAverageMark(percentage);
        reportCard.setOverallGrade(overallGrade);
        reportCard.setGpa(gpa);
        if (request.getRemarks() != null) {
            reportCard.setRemarks(request.getRemarks());
        }

        ReportCard saved = reportCardRepository.save(reportCard);

        calculateClassRanks(request.getTermId());
        auditSafely(saved.getInstitutionId(), null, null, "ReportCard", saved.getId(),
                student.getId().toString(), AuditLog.AuditAction.CREATE, null,
                Map.of("termId", String.valueOf(request.getTermId()),
                        "averageMark", String.valueOf(saved.getAverageMark()),
                        "overallGrade", String.valueOf(saved.getOverallGrade())));
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportCardResponse getById(UUID id) {
        ReportCard reportCard = reportCardRepository.findById(id)
                .filter(rc -> !rc.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Report card not found"));
        return mapToResponse(reportCard);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCardResponse> getByStudentId(UUID studentId) {
        return reportCardRepository.findByStudentIdAndIsDeletedFalse(studentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCardResponse> getByTermId(UUID termId) {
        return reportCardRepository.findByTermIdAndIsDeletedFalse(termId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportCardResponse getByStudentAndTerm(UUID studentId, UUID termId) {
        ReportCard reportCard = reportCardRepository.findByStudentIdAndTermIdAndIsDeletedFalse(studentId, termId)
                .orElseThrow(() -> new ResourceNotFoundException("Report card not found for student in term"));
        return mapToResponse(reportCard);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCardResponse> getByStudentIds(List<UUID> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }
        return reportCardRepository.findByStudentIdInAndIsDeletedFalse(studentIds)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReportCardResponse updateStatus(UUID id, String status, String userEmail, String userRole) {
        ReportCard reportCard = reportCardRepository.findById(id)
                .filter(rc -> !rc.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Report card not found"));
        String oldStatus = reportCard.getStatus() != null ? reportCard.getStatus().name() : null;
        reportCard.setStatus(ReportCardStatus.valueOf(status));
        boolean released = ReportCardStatus.PUBLISHED.name().equals(status);
        if (released) {
            reportCard.setPublishedAt(LocalDateTime.now());
        }
        ReportCard saved = reportCardRepository.save(reportCard);

        if (released) {
            notifyLearnerOfRelease(saved);
        }
        auditSafely(saved.getInstitutionId(), userEmail, userRole, "ReportCard", saved.getId(),
                String.valueOf(saved.getStudentId()), AuditLog.AuditAction.UPDATE,
                Map.of("status", String.valueOf(oldStatus)),
                Map.of("status", status, "source", "REPORT_CARD_RELEASE"));
        return mapToResponse(saved);
    }

    /**
     * Ranks report cards within each class group for the term (highest average first).
     * Cards without a class group are left unranked.
     */
    @Override
    @Transactional
    public void calculateClassRanks(UUID termId) {
        List<ReportCard> cards = reportCardRepository.findByTermIdAndIsDeletedFalse(termId);
        Map<UUID, List<ReportCard>> byClass = cards.stream()
                .filter(rc -> rc.getClassGroupId() != null)
                .collect(Collectors.groupingBy(ReportCard::getClassGroupId));

        for (List<ReportCard> group : byClass.values()) {
            group.sort(Comparator.comparing(
                    ReportCard::getAverageMark, Comparator.nullsLast(Comparator.reverseOrder())));
            int total = group.size();
            for (int i = 0; i < group.size(); i++) {
                ReportCard rc = group.get(i);
                rc.setClassRank(i + 1);
                rc.setTotalStudentsInClass(total);
            }
        }
        reportCardRepository.saveAll(cards);
    }

    /** Tells the learner their report card is now visible. */
    private void notifyLearnerOfRelease(ReportCard reportCard) {
        try {
            Student student = studentRepository.findById(reportCard.getStudentId()).orElse(null);
            if (student == null || student.getUserId() == null) {
                return;
            }
            notificationService.notifyUser(student.getUserId(),
                    "Report card released",
                    "Your report card is now available. Open Grades to view your results.",
                    "GRADE_RELEASED", "reportCard", reportCard.getId());
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(ReportCardServiceImpl.class)
                    .warn("Failed to send GRADE_RELEASED for report card {}: {}",
                            reportCard.getId(), ex.getMessage());
        }
    }

    /** Audit write that can never fail the business transaction (null-safe in unit tests). */
    private void auditSafely(UUID institutionId, String userEmail, String userRole,
                             String entityType, UUID entityId, String entityName,
                             AuditLog.AuditAction action,
                             Map<String, Object> oldValues, Map<String, Object> newValues) {
        try {
            if (auditService == null) {
                return;
            }
            UUID actorId = userEmail == null ? null
                    : userRepository.findByEmailAndIsDeletedFalse(userEmail).map(User::getId).orElse(null);
            auditService.recordAuditLog(institutionId, actorId, userEmail, userRole,
                    entityType, entityId, entityName, action, oldValues, newValues);
        } catch (Exception ex) {
            org.slf4j.LoggerFactory.getLogger(ReportCardServiceImpl.class)
                    .warn("Audit write failed for {} {}: {}", entityType, entityId, ex.getMessage());
        }
    }

    private ReportCardResponse mapToResponse(ReportCard rc) {
        String studentName = null;
        String admissionNumber = null;
        String className = null;
        String termName = null;
        String academicYearName = null;

        Student student = studentRepository.findById(rc.getStudentId()).orElse(null);
        if (student != null) {
            admissionNumber = student.getAdmissionNumber();
            User user = userRepository.findById(student.getUserId()).orElse(null);
            if (user != null) {
                studentName = user.getFirstName() + " " + user.getLastName();
            }
        }

        UUID resolvedClassGroupId = rc.getClassGroupId();
        if (resolvedClassGroupId == null) {
            resolvedClassGroupId = classAccessGuard.resolveClassGroupIdForStudent(rc.getStudentId());
        }
        if (resolvedClassGroupId != null) {
            ClassGroup classGroup = classGroupRepository.findById(resolvedClassGroupId).orElse(null);
            if (classGroup != null) {
                className = classGroup.getName();
            }
        }

        if (rc.getTermId() != null) {
            Term term = termRepository.findById(rc.getTermId()).orElse(null);
            if (term != null) termName = term.getName();
        }
        if (rc.getAcademicYearId() != null) {
            AcademicYear year = academicYearRepository.findById(rc.getAcademicYearId()).orElse(null);
            if (year != null) academicYearName = year.getYearLabel();
        }

        return ReportCardResponse.builder()
                .id(rc.getId())
                .studentId(rc.getStudentId())
                .studentName(studentName)
                .admissionNumber(admissionNumber)
                .className(className)
                .academicYearId(rc.getAcademicYearId())
                .academicYear(academicYearName)
                .termId(rc.getTermId())
                .term(termName)
                .termName(termName)
                .gradingScaleId(rc.getGradingScaleId())
                .totalMarks(rc.getTotalMarks())
                .averageMark(rc.getAverageMark())
                .overallGrade(rc.getOverallGrade())
                .gpa(rc.getGpa())
                .classRank(rc.getClassRank())
                .totalStudentsInClass(rc.getTotalStudentsInClass())
                .remarks(rc.getRemarks())
                .status(rc.getStatus().name())
                .publishedAt(rc.getPublishedAt())
                .createdAt(rc.getCreatedAt())
                .build();
    }
}
