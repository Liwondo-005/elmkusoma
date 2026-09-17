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
import tz.elmkusoma.parent.domain.Achievement;
import tz.elmkusoma.parent.domain.LearningGoal;
import tz.elmkusoma.parent.domain.SupportTicket;
import tz.elmkusoma.parent.domain.SupportTicketMessage;
import tz.elmkusoma.parent.dto.*;
import tz.elmkusoma.parent.dto.response.*;
import tz.elmkusoma.parent.repository.AchievementRepository;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.parent.repository.LearningGoalRepository;
import tz.elmkusoma.parent.service.*;

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

    private final ParentDashboardService dashboardService;
    private final ParentIntelligenceService intelligenceService;
    private final ParentPaymentService paymentService;
    private final ParentCalendarService calendarService;
    private final ParentLibraryService libraryService;
    private final ParentSupportService supportService;
    private final AchievementRepository achievementRepository;
    private final LearningGoalRepository learningGoalRepository;
    private final EntitlementRepository entitlementRepository;
    private final LiveClassRepository liveClassRepository;

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

    @PostMapping("/payments/{paymentId}/verify")
    @Operation(summary = "Verify a payment (admin/backend only)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPayment(
            @PathVariable UUID paymentId,
            @RequestBody Map<String, String> body) {
        String providerReference = body.getOrDefault("providerReference", "manual");
        var payment = paymentService.verifyPayment(paymentId, providerReference);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "paymentId", payment.getId().toString(),
                "status", payment.getStatus()
        )));
    }

    // ── Achievements ───────────────────────────────────────────────────

    @GetMapping("/children/{studentId}/achievements")
    @Operation(summary = "Get achievements for a specific child")
    public ResponseEntity<ApiResponse<ParentAchievementResponse>> getChildAchievements(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
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
            @PathVariable UUID ticketId) {
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
        SupportTicketMessage message = supportService.addMessage(ticketId, userId, body.get("message"));
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    // ── Live Classes ──────────────────────────────────────────────────

    @GetMapping("/children/{studentId}/live-classes")
    @Operation(summary = "Get live classes for a specific child's institution")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getChildLiveClasses(
            @PathVariable UUID studentId,
            HttpServletRequest request) {
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
}
