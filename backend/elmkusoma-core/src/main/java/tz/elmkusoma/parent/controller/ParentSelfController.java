package tz.elmkusoma.parent.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.learning.domain.LessonProgress;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.repository.LessonProgressRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.parent.domain.Achievement;
import tz.elmkusoma.parent.domain.LearningGoal;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.SupportTicket;
import tz.elmkusoma.parent.domain.SupportTicketMessage;
import tz.elmkusoma.parent.dto.ParentSupportResponse;
import tz.elmkusoma.parent.dto.ParentIntelligenceResponse;
import tz.elmkusoma.parent.dto.ParentCalendarResponse;
import tz.elmkusoma.parent.dto.ParentPaymentResponse;
import tz.elmkusoma.parent.dto.ParentAchievementResponse;
import tz.elmkusoma.parent.dto.ParentGoalResponse;
import tz.elmkusoma.parent.dto.ParentLibraryResponse;
import tz.elmkusoma.parent.dto.ParentActivityResponse;
import tz.elmkusoma.parent.dto.ParentTeacherDirectoryResponse;
import tz.elmkusoma.parent.dto.ParentSubjectPerformanceResponse;
import tz.elmkusoma.parent.dto.request.ParentNotificationPreferenceRequest;
import tz.elmkusoma.parent.dto.request.SendMessageRequest;
import tz.elmkusoma.parent.dto.response.*;
import tz.elmkusoma.parent.repository.AchievementRepository;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.LearningGoalRepository;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.parent.repository.SupportTicketRepository;
import tz.elmkusoma.parent.service.*;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.student.domain.StudentClassAssignment;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.domain.TeacherAssignment;
import tz.elmkusoma.teacher.repository.TeacherAssignmentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;
import tz.elmkusoma.assessment.domain.Assessment;
import tz.elmkusoma.assessment.domain.AssessmentResult;
import tz.elmkusoma.assessment.repository.AssessmentRepository;
import tz.elmkusoma.assessment.repository.AssessmentResultRepository;
import tz.elmkusoma.grading.domain.ReportCard;
import tz.elmkusoma.grading.domain.SubjectGrade;
import tz.elmkusoma.grading.repository.ReportCardRepository;
import tz.elmkusoma.grading.repository.SubjectGradeRepository;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.learner.domain.LearnerNotification;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/my")
@RequiredArgsConstructor
@PreAuthorize("hasRole('PARENT')")
@Tag(name = "Parent Self-Service", description = "Parent dashboard and family management endpoints")
public class ParentSelfController {

    private static final Logger log = LoggerFactory.getLogger(ParentSelfController.class);

    private final ParentDashboardService dashboardService;
    private final ParentIntelligenceService intelligenceService;
    private final ParentPaymentService paymentService;
    private final ParentCalendarService calendarService;
    private final ParentLibraryService libraryService;
    private final ParentSupportService supportService;
    private final MessageService messageService;
    private final ParentRepository parentRepository;
    private final ParentStudentLinkRepository studentLinkRepository;
    private final SupportTicketRepository supportTicketRepository;
    private final AchievementRepository achievementRepository;
    private final LearningGoalRepository learningGoalRepository;
    private final EntitlementRepository entitlementRepository;
    private final LiveClassRepository liveClassRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final ReportCardRepository reportCardRepository;
    private final SubjectGradeRepository subjectGradeRepository;
    private final TeacherRepository teacherRepository;
    private final TeacherAssignmentRepository teacherAssignmentRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final ResourceRepository resourceRepository;
    private final LearnerNotificationRepository learnerNotificationRepository;
    private final StudentClassAssignmentRepository studentClassAssignmentRepository;

    private UUID getCurrentUserId(HttpServletRequest request) {
        Object userIdAttr = request.getAttribute("userId");
        if (userIdAttr instanceof UUID uuid) {
            return uuid;
        }
        throw new RuntimeException("User not authenticated");
    }

    private UUID getCurrentInstitutionId(HttpServletRequest request) {
        Object attr = request.getAttribute("institutionId");
        if (attr instanceof UUID uuid) {
            return uuid;
        }
        return null;
    }

    private Parent resolveParent(UUID userId) {
        List<Parent> parents = parentRepository.findAllByUserId(userId);
        if (parents.isEmpty()) {
            log.warn("Parent profile not found for userId={}", userId);
            throw new tz.elmkusoma.exception.ResourceNotFoundException("Parent profile", "userId", userId);
        }
        return parents.get(0);
    }

    private void requireChildAccess(UUID userId, UUID studentId) {
        Parent parent = resolveParent(userId);
        boolean authorized = studentLinkRepository
                .existsByParentIdAndStudentIdAndIsDeletedFalse(parent.getId(), studentId);
        if (!authorized) {
            log.warn("IDOR blocked: parent={} attempted access to unauthorized child={}", parent.getId(), studentId);
            throw new tz.elmkusoma.exception.ResourceNotFoundException("Student link", "studentId", studentId);
        }
    }

    private void requireTicketAccess(UUID userId, UUID ticketId) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> {
                    log.warn("Ticket not found: ticketId={}", ticketId);
                    return new tz.elmkusoma.exception.ResourceNotFoundException("Support ticket", "id", ticketId);
                });
        if (!ticket.getUserId().equals(userId)) {
            log.warn("IDOR blocked: user={} attempted access to unauthorized ticket={}", userId, ticketId);
            throw new tz.elmkusoma.exception.ResourceNotFoundException("Support ticket", "id", ticketId);
        }
    }

    // ── Core Dashboard ─────────────────────────────────────────────────

    @GetMapping("/overview")
    @Operation(summary = "Get family overview with all children and today's actions")
    public ResponseEntity<ApiResponse<FamilyOverviewResponse>> getFamilyOverview(
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        FamilyOverviewResponse overview = dashboardService.getFamilyOverview(userId);
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/children")
    @Operation(summary = "Get all children linked to the authenticated parent")
    public ResponseEntity<ApiResponse<List<ChildOverviewResponse>>> getMyChildren(
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        List<ChildOverviewResponse> children = dashboardService.getMyChildren(userId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    @GetMapping("/children/{studentId}")
    @Operation(summary = "Get detailed overview for a specific child")
    public ResponseEntity<ApiResponse<ChildOverviewResponse>> getChildOverview(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        ChildOverviewResponse overview = dashboardService.getChildOverview(userId, studentId);
        return ResponseEntity.ok(ApiResponse.success(overview));
    }

    @GetMapping("/children/{studentId}/attendance")
    @Operation(summary = "Get attendance details for a specific child")
    public ResponseEntity<ApiResponse<ParentAttendanceResponse>> getChildAttendance(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        ParentAttendanceResponse attendance = dashboardService.getChildAttendance(userId, studentId);
        return ResponseEntity.ok(ApiResponse.success(attendance));
    }

    @GetMapping("/children/{studentId}/assignments")
    @Operation(summary = "Get assignments for a specific child")
    public ResponseEntity<ApiResponse<ParentAssignmentResponse>> getChildAssignments(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        ParentAssignmentResponse assignments = dashboardService.getChildAssignments(userId, studentId);
        return ResponseEntity.ok(ApiResponse.success(assignments));
    }

    @GetMapping("/children/{studentId}/results")
    @Operation(summary = "Get results and report cards for a specific child")
    public ResponseEntity<ApiResponse<ParentResultResponse>> getChildResults(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        ParentResultResponse results = dashboardService.getChildResults(userId, studentId);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    // ── Intelligence ───────────────────────────────────────────────────

    @GetMapping("/children/{studentId}/intelligence")
    @Operation(summary = "Get parent intelligence: attention signals, positive signals, weekly brief, recommendations, upcoming")
    public ResponseEntity<ApiResponse<ParentIntelligenceResponse>> getChildIntelligence(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        requireChildAccess(userId, studentId);
        UUID institutionId = getCurrentInstitutionId(request);
        ParentIntelligenceResponse intelligence = intelligenceService.getIntelligence(studentId, institutionId);
        return ResponseEntity.ok(ApiResponse.success(intelligence));
    }

    // ── Calendar ───────────────────────────────────────────────────────

    @GetMapping("/children/{studentId}/calendar")
    @Operation(summary = "Get family education calendar for a specific child")
    public ResponseEntity<ApiResponse<ParentCalendarResponse>> getChildCalendar(
            @PathVariable UUID studentId,
            @RequestParam(defaultValue = "30") int daysAhead,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        requireChildAccess(userId, studentId);
        UUID institutionId = getCurrentInstitutionId(request);
        ParentCalendarResponse calendar = calendarService.getCalendar(studentId, institutionId, daysAhead);
        return ResponseEntity.ok(ApiResponse.success(calendar));
    }

    // ── Payments ───────────────────────────────────────────────────────

    @GetMapping("/payments")
    @Operation(summary = "Get payment overview and history")
    public ResponseEntity<ApiResponse<ParentPaymentResponse>> getPayments(
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        ParentPaymentResponse payments = paymentService.getPayments(userId);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @GetMapping("/children/{studentId}/payments")
    @Operation(summary = "Get payment history for a specific child")
    public ResponseEntity<ApiResponse<List<ParentPaymentResponse.PaymentItem>>> getChildPayments(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        requireChildAccess(userId, studentId);
        List<ParentPaymentResponse.PaymentItem> payments = paymentService.getPaymentsByStudent(studentId);
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping("/payments/initiate")
    @Operation(summary = "Initiate a payment for a service")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initiatePayment(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        UUID institutionId = getCurrentInstitutionId(request);

        UUID studentId = UUID.fromString((String) body.get("studentId"));
        java.math.BigDecimal amount = new java.math.BigDecimal(body.get("amount").toString());
        String serviceType = (String) body.get("serviceType");
        UUID serviceId = body.get("serviceId") != null ? UUID.fromString((String) body.get("serviceId")) : null;
        String description = (String) body.getOrDefault("description", "");

        var payment = paymentService.initiatePayment(userId, studentId, institutionId,
                amount, serviceType, serviceId, description);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "paymentId", payment.getId().toString(),
                "status", payment.getStatus(),
                "amount", payment.getAmount()
        )));
    }

    // NOTE: Payment verification is admin-only. Use POST /v1/parents/payments/{paymentId}/verify

    // ── Achievements ───────────────────────────────────────────────────

    @GetMapping("/children/{studentId}/achievements")
    @Operation(summary = "Get achievements for a specific child")
    public ResponseEntity<ApiResponse<ParentAchievementResponse>> getChildAchievements(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        requireChildAccess(userId, studentId);
        List<Achievement> achievements = achievementRepository
                .findByStudentIdAndIsDeletedFalseOrderByAchievedAtDesc(studentId);

        List<ParentAchievementResponse.AchievementItem> items = achievements.stream()
                .map(a -> ParentAchievementResponse.AchievementItem.builder()
                        .id(a.getId().toString())
                        .title(a.getTitle())
                        .description(a.getDescription())
                        .achievementType(a.getAchievementType())
                        .icon(a.getIcon())
                        .color(a.getColor())
                        .relatedEntityType(a.getRelatedEntityType())
                        .relatedEntityId(a.getRelatedEntityId() != null ? a.getRelatedEntityId().toString() : null)
                        .achievedAt(a.getAchievedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(ParentAchievementResponse.builder()
                .achievements(items)
                .totalAchievements(items.size())
                .build()));
    }

    // ── Learning Goals ─────────────────────────────────────────────────

    @GetMapping("/children/{studentId}/goals")
    @Operation(summary = "Get learning goals for a specific child")
    public ResponseEntity<ApiResponse<ParentGoalResponse>> getChildGoals(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        requireChildAccess(userId, studentId);
        List<LearningGoal> goals = learningGoalRepository
                .findByStudentIdAndIsDeletedFalseOrderByCreatedAtDesc(studentId);

        long activeCount = goals.stream().filter(g -> "ACTIVE".equals(g.getStatus())).count();
        long completedCount = goals.stream().filter(g -> "COMPLETED".equals(g.getStatus())).count();

        List<ParentGoalResponse.GoalItem> items = goals.stream()
                .map(g -> ParentGoalResponse.GoalItem.builder()
                        .id(g.getId().toString())
                        .title(g.getTitle())
                        .description(g.getDescription())
                        .goalType(g.getGoalType())
                        .status(g.getStatus())
                        .progressPercentage(g.getProgressPercentage())
                        .targetDate(g.getTargetDate())
                        .completedAt(g.getCompletedAt())
                        .relatedEntityType(g.getRelatedEntityType())
                        .relatedEntityId(g.getRelatedEntityId() != null ? g.getRelatedEntityId().toString() : null)
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(ParentGoalResponse.builder()
                .goals(items)
                .activeCount(activeCount)
                .completedCount(completedCount)
                .build()));
    }

    // ── Learning Library ───────────────────────────────────────────────

    @GetMapping("/library")
    @Operation(summary = "Get family learning library")
    public ResponseEntity<ApiResponse<ParentLibraryResponse>> getLibrary(
            HttpServletRequest request) {
        UUID institutionId = getCurrentInstitutionId(request);
        ParentLibraryResponse library = libraryService.getLibrary(institutionId);
        return ResponseEntity.ok(ApiResponse.success(library));
    }

    // ── Entitlements ───────────────────────────────────────────────────

    @GetMapping("/children/{studentId}/entitlements")
    @Operation(summary = "Get active entitlements for a specific child")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getChildEntitlements(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        requireChildAccess(userId, studentId);
        var entitlements = entitlementRepository.findByStudentIdAndIsDeletedFalse(studentId);
        List<Map<String, Object>> items = entitlements.stream()
                .map(e -> Map.<String, Object>of(
                        "id", e.getId().toString(),
                        "serviceType", e.getServiceType(),
                        "serviceId", e.getServiceId().toString(),
                        "status", e.getStatus(),
                        "startsAt", e.getStartsAt().toString(),
                        "expiresAt", e.getExpiresAt() != null ? e.getExpiresAt().toString() : null
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    // ── Support ────────────────────────────────────────────────────────

    @GetMapping("/support/tickets")
    @Operation(summary = "Get support tickets")
    public ResponseEntity<ApiResponse<ParentSupportResponse>> getTickets(
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        ParentSupportResponse tickets = supportService.getTickets(userId);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @PostMapping("/support/tickets")
    @Operation(summary = "Create a support ticket")
    public ResponseEntity<ApiResponse<ParentSupportResponse.SupportTicketItem>> createTicket(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        UUID institutionId = getCurrentInstitutionId(request);

        ParentSupportResponse.SupportTicketItem ticket = supportService.createTicket(
                userId, institutionId,
                body.get("subject"),
                body.get("description"),
                body.get("category"),
                body.get("priority"));

        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @GetMapping("/support/tickets/{ticketId}/messages")
    @Operation(summary = "Get messages for a support ticket")
    public ResponseEntity<ApiResponse<List<SupportTicketMessage>>> getTicketMessages(
            @PathVariable UUID ticketId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        requireTicketAccess(userId, ticketId);
        List<SupportTicketMessage> messages = supportService.getMessages(ticketId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/support/tickets/{ticketId}/messages")
    @Operation(summary = "Add a message to a support ticket")
    public ResponseEntity<ApiResponse<SupportTicketMessage>> addTicketMessage(
            @PathVariable UUID ticketId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        requireTicketAccess(userId, ticketId);
        SupportTicketMessage message = supportService.addMessage(ticketId, userId, body.get("message"));
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // ── Live Classes ──────────────────────────────────────────────────

    @GetMapping("/children/{studentId}/live-classes")
    @Operation(summary = "Get live classes for a specific child's institution")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getChildLiveClasses(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        requireChildAccess(userId, studentId);
        UUID institutionId = getCurrentInstitutionId(request);
        if (institutionId == null) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }

        List<Map<String, Object>> liveClasses = new java.util.ArrayList<>();
        var classes = liveClassRepository.findByInstitutionIdAndIsDeletedFalse(institutionId);
        for (var lc : classes) {
            if (!"CANCELLED".equals(lc.getStatus())) {
                liveClasses.add(Map.of(
                        "id", lc.getId().toString(),
                        "title", lc.getTitle() != null ? lc.getTitle() : "",
                        "status", lc.getStatus() != null ? lc.getStatus() : "",
                        "scheduledAt", lc.getScheduledAt() != null ? lc.getScheduledAt().toString() : "",
                        "durationMinutes", lc.getDurationMinutes() != null ? lc.getDurationMinutes() : 60
                ));
            }
        }
        return ResponseEntity.ok(ApiResponse.success(liveClasses));
    }

    // ── Assessments ──────────────────────────────────────────────────

    @GetMapping("/children/{studentId}/assessments")
    @Operation(summary = "Get assessments for a specific child")
    public ResponseEntity<ApiResponse<ParentAssessmentResponse>> getChildAssessments(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        requireChildAccess(userId, studentId);
        Student student = studentRepository.findById(studentId).orElse(null);
        User user = student != null ? userRepository.findById(student.getUserId()).orElse(null) : null;
        String studentName = user != null ? user.getFullName() : "";

        List<Assessment> allAssessments = assessmentRepository.findByInstitutionIdAndIsDeletedFalse(
                getCurrentInstitutionId(request));

        List<AssessmentResult> results = assessmentResultRepository.findByStudentIdAndIsDeletedFalse(studentId);

        List<ParentAssessmentResponse.AssessmentItem> completed = results.stream()
                .map(r -> {
                    Assessment a = assessmentRepository.findById(r.getAssessmentId()).orElse(null);
                    String subjectName = "";
                    if (a != null && a.getSubjectId() != null) {
                        Subject s = subjectRepository.findById(a.getSubjectId()).orElse(null);
                        subjectName = s != null ? s.getName() : "";
                    }
                    return ParentAssessmentResponse.AssessmentItem.builder()
                            .id(r.getId().toString())
                            .title(a != null ? a.getTitle() : "")
                            .subject(subjectName)
                            .subjectId(a != null && a.getSubjectId() != null ? a.getSubjectId().toString() : null)
                            .totalMarks(a != null ? a.getTotalMarks() : null)
                            .passMarks(a != null ? a.getPassMarks() : null)
                            .score(r.getTotalScore())
                            .isPassed(r.getIsPassed())
                            .percentage(a != null && a.getTotalMarks() != null && r.getTotalScore() != null
                                    ? (double) r.getTotalScore() * 100.0 / a.getTotalMarks() : null)
                            .createdAt(r.getCreatedAt())
                            .status("COMPLETED")
                            .build();
                })
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(ParentAssessmentResponse.builder()
                .studentName(studentName)
                .completed(completed)
                .upcoming(List.of())
                .build()));
    }

    // ── Learning Activity Timeline ──────────────────────────────────

    @GetMapping("/children/{studentId}/activity")
    @Operation(summary = "Get learning activity timeline for a specific child")
    public ResponseEntity<ApiResponse<ParentActivityResponse>> getChildActivity(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        requireChildAccess(userId, studentId);
        List<ParentActivityResponse.ActivityItem> activities = new java.util.ArrayList<>();

        List<LessonProgress> progressList = lessonProgressRepository.findByStudentIdAndIsDeletedFalse(studentId);
        for (LessonProgress lp : progressList) {
            if (lp.getCompletedAt() != null) {
                activities.add(ParentActivityResponse.ActivityItem.builder()
                        .id(lp.getId().toString())
                        .type("LESSON_COMPLETED")
                        .title("Completed a lesson")
                        .description("Lesson progress: " + (lp.getCompletionPercentage() != null ? Math.round(lp.getCompletionPercentage()) + "%" : ""))
                        .timestamp(lp.getCompletedAt())
                        .relatedEntityType("lesson")
                        .relatedEntityId(lp.getLessonId() != null ? lp.getLessonId().toString() : null)
                        .status("COMPLETED")
                        .build());
            } else if (lp.getStartedAt() != null) {
                activities.add(ParentActivityResponse.ActivityItem.builder()
                        .id(lp.getId().toString())
                        .type("LESSON_STARTED")
                        .title("Started a lesson")
                        .description("Lesson progress: " + (lp.getCompletionPercentage() != null ? Math.round(lp.getCompletionPercentage()) + "%" : "0%"))
                        .timestamp(lp.getStartedAt())
                        .relatedEntityType("lesson")
                        .relatedEntityId(lp.getLessonId() != null ? lp.getLessonId().toString() : null)
                        .status("IN_PROGRESS")
                        .build());
            }
        }

        List<Achievement> achievements = achievementRepository.findByStudentIdAndIsDeletedFalseOrderByAchievedAtDesc(studentId);
        for (Achievement ach : achievements) {
            activities.add(ParentActivityResponse.ActivityItem.builder()
                    .id(ach.getId().toString())
                    .type("ACHIEVEMENT")
                    .title(ach.getTitle())
                    .description(ach.getDescription())
                    .timestamp(ach.getAchievedAt())
                    .relatedEntityType(ach.getRelatedEntityType())
                    .relatedEntityId(ach.getRelatedEntityId() != null ? ach.getRelatedEntityId().toString() : null)
                    .status("ACHIEVED")
                    .build());
        }

        activities.sort((a, b) -> {
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });

        return ResponseEntity.ok(ApiResponse.success(ParentActivityResponse.builder()
                .activities(activities)
                .build()));
    }

    // ── Teacher Directory ──────────────────────────────────────────

    @GetMapping("/children/{studentId}/teachers")
    @Operation(summary = "Get teachers for a specific child's classes")
    public ResponseEntity<ApiResponse<ParentTeacherDirectoryResponse>> getChildTeachers(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        requireChildAccess(userId, studentId);
        Student student = studentRepository.findById(studentId).orElse(null);
        UUID institutionId = getCurrentInstitutionId(request);

        List<ParentTeacherDirectoryResponse.TeacherItem> teachers = new java.util.ArrayList<>();

        if (student != null) {
            List<StudentClassAssignment> classAssignments = studentClassAssignmentRepository
                    .findByStudentIdAndIsDeletedFalse(studentId);

            java.util.Set<UUID> seenTeacherIds = new java.util.HashSet<>();

            for (StudentClassAssignment sca : classAssignments) {
                List<TeacherAssignment> assignments = teacherAssignmentRepository
                        .findAllByClassGroupId(sca.getClassGroupId());

                for (TeacherAssignment ta : assignments) {
                    if (seenTeacherIds.contains(ta.getTeacherId())) continue;
                    seenTeacherIds.add(ta.getTeacherId());

                Teacher teacher = teacherRepository.findById(ta.getTeacherId()).orElse(null);
                if (teacher == null) continue;

                User teacherUser = userRepository.findById(teacher.getUserId()).orElse(null);
                String subjectName = "";
                if (ta.getSubjectId() != null) {
                    Subject s = subjectRepository.findById(ta.getSubjectId()).orElse(null);
                    subjectName = s != null ? s.getName() : "";
                }

                teachers.add(ParentTeacherDirectoryResponse.TeacherItem.builder()
                        .id(teacher.getId().toString())
                        .userId(teacher.getUserId().toString())
                        .fullName(teacherUser != null ? teacherUser.getFullName() : "")
                        .email(teacherUser != null ? teacherUser.getEmail() : "")
                        .phone(teacherUser != null ? teacherUser.getPhone() : "")
                        .subject(subjectName)
                        .subjectId(ta.getSubjectId() != null ? ta.getSubjectId().toString() : null)
                        .specialization(teacher.getSpecialization())
                        .build());
                }
            }
        }

        return ResponseEntity.ok(ApiResponse.success(ParentTeacherDirectoryResponse.builder()
                .teachers(teachers)
                .build()));
    }

    // ── Subject Performance ────────────────────────────────────────

    @GetMapping("/children/{studentId}/subject-performance")
    @Operation(summary = "Get subject performance breakdown for a specific child")
    public ResponseEntity<ApiResponse<ParentSubjectPerformanceResponse>> getChildSubjectPerformance(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        requireChildAccess(userId, studentId);
        List<ReportCard> reportCards = reportCardRepository.findByStudentIdAndIsDeletedFalse(studentId);
        java.util.Map<String, ParentSubjectPerformanceResponse.SubjectItem> subjectMap = new java.util.LinkedHashMap<>();

        for (ReportCard rc : reportCards) {
            List<SubjectGrade> grades = subjectGradeRepository.findByReportCardIdAndIsDeletedFalse(rc.getId());
            for (SubjectGrade sg : grades) {
                String subjectId = sg.getSubjectId() != null ? sg.getSubjectId().toString() : "unknown";
                Subject subject = sg.getSubjectId() != null ? subjectRepository.findById(sg.getSubjectId()).orElse(null) : null;
                String subjectName = subject != null ? subject.getName() : "Unknown";

                subjectMap.computeIfAbsent(subjectId, k -> ParentSubjectPerformanceResponse.SubjectItem.builder()
                        .subjectId(subjectId)
                        .subjectName(subjectName)
                        .totalAssessments(0)
                        .completedAssessments(0)
                        .build());

                ParentSubjectPerformanceResponse.SubjectItem item = subjectMap.get(subjectId);
                if (sg.getMarksObtained() != null) {
                    item.setTotalAssessments(item.getTotalAssessments() + 1);
                    item.setCompletedAssessments(item.getCompletedAssessments() + 1);
                }
            }
        }

        for (ParentSubjectPerformanceResponse.SubjectItem item : subjectMap.values()) {
            List<SubjectGrade> allGrades = subjectGradeRepository.findBySubjectIdAndIsDeletedFalse(
                    UUID.fromString(item.getSubjectId()));
            List<SubjectGrade> studentGrades = allGrades.stream()
                    .filter(sg -> reportCards.stream().anyMatch(rc -> rc.getId().equals(sg.getReportCardId()) && rc.getStudentId().equals(studentId)))
                    .collect(Collectors.toList());

            if (!studentGrades.isEmpty()) {
                double avg = studentGrades.stream()
                        .filter(sg -> sg.getMarksObtained() != null)
                        .mapToDouble(sg -> sg.getMarksObtained().doubleValue())
                        .average().orElse(0.0);
                item.setAverageMark(Math.round(avg * 10.0) / 10.0);

                if (avg >= 80) item.setGrade("A");
                else if (avg >= 70) item.setGrade("B");
                else if (avg >= 60) item.setGrade("C");
                else if (avg >= 50) item.setGrade("D");
                else item.setGrade("F");
            }
        }

        return ResponseEntity.ok(ApiResponse.success(ParentSubjectPerformanceResponse.builder()
                .subjects(new java.util.ArrayList<>(subjectMap.values()))
                .build()));
    }

    // ── Learning Progress (per-course) ─────────────────────────────

    @GetMapping("/children/{studentId}/learning-progress")
    @Operation(summary = "Get per-course learning progress for a specific child")
    public ResponseEntity<ApiResponse<ParentLearningProgressResponse>> getChildLearningProgress(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        ParentLearningProgressResponse progress = dashboardService.getChildLearningProgress(userId, studentId);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }

    // ── Notifications ──────────────────────────────────────────────

    @GetMapping("/notifications")
    @Operation(summary = "Get notifications for the parent")
    public ResponseEntity<ApiResponse<ParentNotificationResponse>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);

        List<LearnerNotification> notifications =
                learnerNotificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        long unreadCount = notifications.stream().filter(n -> !Boolean.TRUE.equals(n.getIsRead())).count();

        List<ParentNotificationResponse.NotificationItem> items = notifications.stream()
                .skip((long) page * size)
                .limit(size)
                .map(n -> ParentNotificationResponse.NotificationItem.builder()
                        .id(n.getId().toString())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .category(n.getNotificationType())
                        .priority("NORMAL")
                        .targetType(n.getTargetType())
                        .targetId(n.getTargetId() != null ? n.getTargetId().toString() : null)
                        .isRead(Boolean.TRUE.equals(n.getIsRead()))
                        .createdAt(n.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(ParentNotificationResponse.builder()
                .notifications(items)
                .unreadCount(unreadCount)
                .build()));
    }

    @PutMapping("/notifications/{notificationId}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<String>> markNotificationRead(
            @PathVariable UUID notificationId) {
        learnerNotificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            learnerNotificationRepository.save(n);
        });
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    // ── Parent Profile Update ──────────────────────────────────────

    @PutMapping("/profile")
    @Operation(summary = "Update parent profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
        }

        if (body.containsKey("firstName")) user.setFirstName((String) body.get("firstName"));
        if (body.containsKey("lastName")) user.setLastName((String) body.get("lastName"));
        if (body.containsKey("phone")) user.setPhone((String) body.get("phone"));
        userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "updated",
                "fullName", user.getFullName()
        )));
    }

    // ── Messages ──────────────────────────────────────────────────────

    @GetMapping("/messages")
    @Operation(summary = "Get inbox messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        List<MessageResponse> messages = messageService.getInboxMessages(userId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @GetMapping("/messages/sent")
    @Operation(summary = "Get sent messages")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getSentMessages(
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        List<MessageResponse> messages = messageService.getSentMessages(userId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/messages")
    @Operation(summary = "Send a message")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @RequestBody SendMessageRequest body,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        MessageResponse message = messageService.sendMessage(userId, body);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PutMapping("/messages/{messageId}/read")
    @Operation(summary = "Mark a message as read")
    public ResponseEntity<ApiResponse<String>> markMessageRead(
            @PathVariable UUID messageId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        messageService.markAsRead(userId, messageId);
        return ResponseEntity.ok(ApiResponse.success("Marked as read", null));
    }

    @DeleteMapping("/messages/{messageId}")
    @Operation(summary = "Delete a message")
    public ResponseEntity<ApiResponse<String>> deleteMessage(
            @PathVariable UUID messageId,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        messageService.deleteMessage(userId, messageId);
        return ResponseEntity.ok(ApiResponse.success("Message deleted", null));
    }

    // ── Notification Preferences ──────────────────────────────────────

    @GetMapping("/notification-preferences")
    @Operation(summary = "Get notification preferences")
    public ResponseEntity<ApiResponse<ParentNotificationPreferenceResponse>> getNotificationPreferences(
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        ParentNotificationPreferenceResponse prefs = messageService.getNotificationPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(prefs));
    }

    @PutMapping("/notification-preferences")
    @Operation(summary = "Update notification preferences")
    public ResponseEntity<ApiResponse<ParentNotificationPreferenceResponse>> updateNotificationPreferences(
            @RequestBody ParentNotificationPreferenceRequest body,
            HttpServletRequest request) {
        UUID userId = getCurrentUserId(request);
        ParentNotificationPreferenceResponse prefs = messageService.updateNotificationPreferences(userId, body);
        return ResponseEntity.ok(ApiResponse.success(prefs));
    }
}
