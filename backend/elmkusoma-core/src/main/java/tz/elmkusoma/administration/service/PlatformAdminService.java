package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.*;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.repository.*;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.domain.SecurityEvent;
import tz.elmkusoma.audit.dto.ActivityFeedResponse;
import tz.elmkusoma.audit.repository.AuditLogRepository;
import tz.elmkusoma.audit.repository.SecurityEventRepository;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import tz.elmkusoma.course.repository.CourseRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlatformAdminService {

    /** Platform-scope audit rows must reference the national HQ institution (audit_logs.institution_id NOT NULL + FK). */
    static final UUID PLATFORM_INSTITUTION_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final LiveClassRepository liveClassRepository;
    private final CertificateRepository certificateRepository;
    private final SecurityEventRepository securityEventRepository;
    private final AuditLogRepository auditLogRepository;
    private final PaymentRepository paymentRepository;
    private final EntitlementRepository entitlementRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final PlatformServiceRepository platformServiceRepository;
    private final PlatformIncidentRepository incidentRepository;
    private final PlatformConfigRepository configRepository;
    private final PlatformNotificationRepository notificationRepository;
    private final AdminDelegationRepository delegationRepository;
    private final VerificationRecordRepository verificationRepository;
    private final CourseRepository courseRepository;
    private final EventRepository eventRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final ResourceRepository resourceRepository;
    private final ProviderServiceEntitlementRepository providerEntitlementRepository;
    private final ContentReportRepository contentReportRepository;
    private final tz.elmkusoma.parent.repository.SupportTicketRepository supportTicketRepository;

    // ── Command Center ──

    @Transactional(readOnly = true)
    public PlatformDashboardResponse getPlatformDashboard() {
        long totalUsers = userRepository.countByIsDeletedFalse();
        long totalStudents = userRepository.countByRoleAndIsDeletedFalse(User.Role.STUDENT);
        long totalTeachers = userRepository.countByRoleAndIsDeletedFalse(User.Role.TEACHER);
        long totalParents = userRepository.countByRoleAndIsDeletedFalse(User.Role.PARENT);
        long totalInstitutions = institutionRepository.countByIsDeletedFalse();
        long totalLiveClasses = liveClassRepository.countByIsDeletedFalse();
        long activeLiveClasses = liveClassRepository.countByStatusAndIsDeletedFalse("LIVE");
        long totalCertificates = certificateRepository.countByIsDeletedFalse();
        long unresolvedSecurityEvents = securityEventRepository.countByResolvedFalse();

        return PlatformDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .totalParents(totalParents)
                .totalInstitutions(totalInstitutions)
                .totalLiveClasses(totalLiveClasses)
                .activeLiveClasses(activeLiveClasses)
                .totalPayments(paymentRepository.countByIsDeletedFalse())
                .totalCertificates(totalCertificates)
                .unresolvedSecurityEvents(unresolvedSecurityEvents)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttentionItemResponse> getAttentionItems() {
        List<AttentionItemResponse> items = new ArrayList<>();

        long unresolvedSecurity = securityEventRepository.countByResolvedFalse();
        if (unresolvedSecurity > 0) {
            items.add(AttentionItemResponse.builder()
                    .severity("HIGH")
                    .title("Unresolved Security Events")
                    .description(unresolvedSecurity + " security event(s) require attention")
                    .category("SECURITY")
                    .actionUrl("/dashboard/platform-admin/security")
                    .build());
        }

        long openIncidents = incidentRepository.countByStatusAndIsDeletedFalse("DETECTED") + incidentRepository.countByStatusAndIsDeletedFalse("INVESTIGATING");
        if (openIncidents > 0) {
            items.add(AttentionItemResponse.builder()
                    .severity("HIGH")
                    .title("Open Incidents")
                    .description(openIncidents + " incident(s) under investigation")
                    .category("INCIDENT")
                    .actionUrl("/dashboard/platform-admin/incidents")
                    .build());
        }

        long pendingVerifications = verificationRepository.countByStatusAndIsDeletedFalse("PENDING");
        if (pendingVerifications > 0) {
            items.add(AttentionItemResponse.builder()
                    .severity("MEDIUM")
                    .title("Pending Verifications")
                    .description(pendingVerifications + " verification request(s) awaiting review")
                    .category("VERIFICATION")
                    .actionUrl("/dashboard/platform-admin/verifications")
                    .build());
        }

        long activeLiveClasses = liveClassRepository.countByStatusAndIsDeletedFalse("LIVE");
        if (activeLiveClasses > 0) {
            items.add(AttentionItemResponse.builder()
                    .severity("INFO")
                    .title("Active Live Sessions")
                    .description(activeLiveClasses + " live session(s) currently active")
                    .category("LIVE")
                    .actionUrl("/dashboard/platform-admin/live-classes")
                    .build());
        }

        return items;
    }

    @Transactional(readOnly = true)
    public List<ActivityFeedResponse> getRecentActivity(int page, int size) {
        Page<AuditLog> logs = auditLogRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return logs.getContent().stream()
                .map(log -> ActivityFeedResponse.of(
                        log.getId(), log.getInstitutionId(), log.getUserId(),
                        log.getUserEmail(), log.getAction() != null ? log.getAction().name() : "UNKNOWN",
                        log.getEntityType() + " was " + (log.getAction() != null ? log.getAction().name().toLowerCase() : "modified"),
                        log.getEntityType(), log.getEntityId(), log.getEntityName(),
                        log.getNewValues(), null, log.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PlatformHealthResponse getPlatformHealth() {
        String dbStatus = "Operational";
        long totalUsers = 0; long totalInstitutions = 0;
        try {
            totalUsers = userRepository.countByIsDeletedFalse();
            totalInstitutions = institutionRepository.countByIsDeletedFalse();
            // probe DB - if counts succeed, DB is operational
        } catch (Exception e) {
            dbStatus = "Failing";
            log.warn("Health DB probe failed: {}", e.getMessage());
        }
        return PlatformHealthResponse.builder()
                .databaseStatus(dbStatus)
                .apiStatus("Operational")
                .totalUsers(totalUsers)
                .activeUsers(totalUsers)
                .totalInstitutions(totalInstitutions)
                .activeInstitutions(totalInstitutions)
                .build();
    }

    // ── Users ──

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> listUsers(int page, int size, String role, String search, UUID institutionId) {
        Page<User> users;
        if (search != null && !search.isBlank()) {
            users = userRepository.findBySearchTermAndIsDeletedFalse(search, PageRequest.of(page, size, Sort.by("createdAt").descending()));
        } else if (role != null && !role.isBlank()) {
            try {
                User.Role userRole = User.Role.valueOf(role.toUpperCase());
                users = userRepository.findByRoleAndIsDeletedFalse(userRole, PageRequest.of(page, size, Sort.by("createdAt").descending()));
            } catch (IllegalArgumentException e) {
                users = userRepository.findAllByIsDeletedFalse(PageRequest.of(page, size, Sort.by("createdAt").descending()));
            }
        } else {
            users = userRepository.findAllByIsDeletedFalse(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        }

        List<UserSummaryResponse> content = users.getContent().stream()
                .map(this::toUserSummary)
                .toList();

        return new PageResponse<>(content, users.getNumber(), users.getSize(), users.getTotalElements(), users.getTotalPages(), users.isFirst(), users.isLast());
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getUser(UUID userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return toUserSummary(user);
    }

    public UserSummaryResponse updateUserStatus(UUID userId, boolean active) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (!active && User.Role.ADMIN.equals(user.getRole())) {
            long activeAdmins = userRepository.countByRoleAndIsDeletedFalse(User.Role.ADMIN);
            // count only active; if last active admin, block
            long activeCount = userRepository.findByRoleAndIsDeletedFalse(User.Role.ADMIN, PageRequest.of(0, 1000)).getContent().stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
            if (activeCount <= 1) {
                throw new IllegalStateException("Cannot suspend the last active Platform Admin - at least one recovery path must remain");
            }
        }
        user.setIsActive(active);
        userRepository.save(user);
        AuditLog al = new AuditLog();
        al.setInstitutionId(user.getInstitutionId() != null ? user.getInstitutionId() : PLATFORM_INSTITUTION_ID);
        al.setUserId(userId);
        al.setEntityType("USER");
        al.setEntityId(userId);
        al.setEntityName(user.getEmail());
        al.setAction(active ? AuditLog.AuditAction.UPDATE : AuditLog.AuditAction.UPDATE);
        al.setOldValues(Map.of("isActive", !active));
        al.setNewValues(Map.of("isActive", active));
        auditLogRepository.save(al);
        log.info("User {} {} by platform admin", userId, active ? "activated" : "suspended");
        return toUserSummary(user);
    }

    // ── Institutions ──

    @Transactional(readOnly = true)
    public PageResponse<InstitutionSummaryResponse> listInstitutions(int page, int size, String search, String type) {
        Page<Institution> institutions;
        if (search != null && !search.isBlank()) {
            institutions = institutionRepository.findBySearchTermAndIsDeletedFalse(search, PageRequest.of(page, size, Sort.by("name")));
        } else {
            institutions = institutionRepository.findByIsDeletedFalse(PageRequest.of(page, size, Sort.by("name")));
        }

        List<InstitutionSummaryResponse> content = institutions.getContent().stream()
                .map(this::toInstitutionSummary)
                .toList();

        return new PageResponse<>(content, institutions.getNumber(), institutions.getSize(), institutions.getTotalElements(), institutions.getTotalPages(), institutions.isFirst(), institutions.isLast());
    }

    @Transactional(readOnly = true)
    public InstitutionDetailResponse getInstitutionDetail(UUID institutionId) {
        Institution inst = institutionRepository.findByIdAndIsDeletedFalse(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", institutionId));

        long totalUsers = userRepository.findAllByInstitutionId(institutionId).size();
        long totalStudents = studentRepository.countByInstitutionId(institutionId);
        long totalTeachers = teacherRepository.findAllByInstitutionId(institutionId).size();

        return InstitutionDetailResponse.builder()
                .id(inst.getId())
                .name(inst.getName())
                .code(inst.getCode())
                .type(inst.getType() != null ? inst.getType().name() : null)
                .address(inst.getAddress())
                .city(inst.getCity())
                .region(inst.getRegion())
                .country(inst.getCountry())
                .phone(inst.getPhone())
                .email(inst.getEmail())
                .isActive(inst.getIsActive())
                .totalUsers(totalUsers)
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .createdAt(inst.getCreatedAt())
                .build();
    }

    public InstitutionSummaryResponse updateInstitutionStatus(UUID institutionId, boolean active) {
        Institution inst = institutionRepository.findByIdAndIsDeletedFalse(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", institutionId));
        inst.setIsActive(active);
        inst.setStatus(active ? "ACTIVE" : "SUSPENDED");
        institutionRepository.save(inst);
        writeAudit(inst.getId(), "INSTITUTION", institutionId, inst.getName(), "UPDATE",
                Map.of("isActive", !active), Map.of("isActive", active));
        log.info("Institution {} {} by platform admin", institutionId, active ? "activated" : "suspended");
        return toInstitutionSummary(inst);
    }

    // ── Institution Lifecycle (offboarding per spec: ACTIVE → SUSPENDED → DEACTIVATED → ARCHIVED) ──

    private static final Set<String> LIFECYCLE_STATUSES = Set.of("ACTIVE", "SUSPENDED", "DEACTIVATED", "ARCHIVED");
    private static final Map<String, Set<String>> LIFECYCLE_TRANSITIONS = Map.of(
            "ACTIVE", Set.of("SUSPENDED", "DEACTIVATED", "ARCHIVED"),
            "SUSPENDED", Set.of("ACTIVE", "DEACTIVATED", "ARCHIVED"),
            "DEACTIVATED", Set.of("ACTIVE", "ARCHIVED", "SUSPENDED"),
            "ARCHIVED", Set.of("ACTIVE", "SUSPENDED")
    );

    public InstitutionSummaryResponse updateInstitutionLifecycle(UUID institutionId, String status) {
        if (status == null || !LIFECYCLE_STATUSES.contains(status.toUpperCase())) {
            throw new IllegalArgumentException("Invalid lifecycle status. Allowed: " + LIFECYCLE_STATUSES);
        }
        String target = status.toUpperCase();
        Institution inst = institutionRepository.findByIdAndIsDeletedFalse(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", institutionId));
        String current = inst.getStatus() != null ? inst.getStatus() : "ACTIVE";
        if (current.equals(target)) {
            return toInstitutionSummary(inst);
        }
        if (!LIFECYCLE_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new IllegalStateException("Invalid lifecycle transition: " + current + " → " + target);
        }
        String oldStatus = current;
        inst.setStatus(target);
        inst.setIsActive("ACTIVE".equals(target));
        institutionRepository.save(inst);
        writeAudit(institutionId, "INSTITUTION", institutionId, inst.getName(), "UPDATE",
                Map.of("status", oldStatus), Map.of("status", target));
        log.info("Institution {} lifecycle: {} → {} by platform admin", institutionId, oldStatus, target);
        return toInstitutionSummary(inst);
    }

    // ── Provider Quotas / Entitlements ──

    @Transactional(readOnly = true)
    public List<ProviderQuotaResponse> listProviderQuotas(UUID providerId) {
        return providerEntitlementRepository.findByProviderIdAndIsDeletedFalse(providerId).stream()
                .map(ent -> {
                    PlatformService svc = platformServiceRepository.findById(ent.getServiceId()).orElse(null);
                    return ProviderQuotaResponse.builder()
                            .id(ent.getId())
                            .providerId(ent.getProviderId())
                            .serviceId(ent.getServiceId())
                            .serviceName(svc != null ? svc.getName() : null)
                            .serviceCode(svc != null ? svc.getCode() : null)
                            .status(ent.getStatus())
                            .seatsUsed(ent.getSeatsUsed())
                            .maxSeats(ent.getMaxSeats())
                            .expiresAt(ent.getExpiresAt())
                            .createdAt(ent.getCreatedAt())
                            .build();
                })
                .toList();
    }

    public ProviderQuotaResponse updateProviderEntitlement(UUID entitlementId, EntitlementUpdateRequest req) {
        ProviderServiceEntitlement ent = providerEntitlementRepository.findById(entitlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Entitlement", "id", entitlementId));
        Integer oldMax = ent.getMaxSeats();
        String oldStatus = ent.getStatus();
        if (req.getMaxSeats() != null) {
            int used = ent.getSeatsUsed() != null ? ent.getSeatsUsed() : 0;
            if (req.getMaxSeats() < used) {
                throw new IllegalStateException("maxSeats (" + req.getMaxSeats() + ") cannot be below seats used (" + used + ")");
            }
            ent.setMaxSeats(req.getMaxSeats());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            ent.setStatus(req.getStatus().toUpperCase());
        }
        providerEntitlementRepository.save(ent);
        writeAudit(ent.getInstitutionId(), "ENTITLEMENT", entitlementId, String.valueOf(ent.getServiceId()), "UPDATE",
                Map.of("maxSeats", String.valueOf(oldMax), "status", String.valueOf(oldStatus)),
                Map.of("maxSeats", String.valueOf(ent.getMaxSeats()), "status", String.valueOf(ent.getStatus())));
        log.info("Provider entitlement {} updated: maxSeats {} → {}, status {} → {}",
                entitlementId, oldMax, ent.getMaxSeats(), oldStatus, ent.getStatus());
        PlatformService svc = platformServiceRepository.findById(ent.getServiceId()).orElse(null);
        return ProviderQuotaResponse.builder()
                .id(ent.getId()).providerId(ent.getProviderId()).serviceId(ent.getServiceId())
                .serviceName(svc != null ? svc.getName() : null).serviceCode(svc != null ? svc.getCode() : null)
                .status(ent.getStatus()).seatsUsed(ent.getSeatsUsed()).maxSeats(ent.getMaxSeats())
                .expiresAt(ent.getExpiresAt()).createdAt(ent.getCreatedAt()).build();
    }

    private void writeAudit(UUID institutionId, String entityType, UUID entityId, String entityName, String action,
                            Map<String, Object> oldValues, Map<String, Object> newValues) {
        AuditLog al = new AuditLog();
        al.setInstitutionId(institutionId != null ? institutionId : PLATFORM_INSTITUTION_ID);
        al.setUserId(null);
        al.setEntityType(entityType);
        al.setEntityId(entityId);
        al.setEntityName(entityName);
        al.setAction(AuditLog.AuditAction.valueOf(action));
        al.setOldValues(oldValues);
        al.setNewValues(newValues);
        auditLogRepository.save(al);
    }

    // ── Live Classes ──

    @Transactional(readOnly = true)
    public PageResponse<LiveClassSummaryResponse> listLiveClasses(int page, int size, String status, UUID institutionId) {
        Page<LiveClass> classes;
        if (status != null && !status.isBlank()) {
            classes = liveClassRepository.findByStatusAndIsDeletedFalse(status.toUpperCase(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt")));
        } else {
            classes = liveClassRepository.findAllByIsDeletedFalse(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt")));
        }

        List<LiveClassSummaryResponse> content = classes.getContent().stream()
                .map(lc -> LiveClassSummaryResponse.builder()
                        .id(lc.getId())
                        .title(lc.getTitle())
                        .status(lc.getStatus())
                        .scheduledAt(lc.getScheduledAt())
                        .durationMinutes(lc.getDurationMinutes())
                        .maxParticipants(lc.getMaxParticipants())
                        .currentParticipants(0)
                        .createdAt(lc.getCreatedAt())
                        .build())
                .toList();

        return new PageResponse<>(content, classes.getNumber(), classes.getSize(), classes.getTotalElements(), classes.getTotalPages(), classes.isFirst(), classes.isLast());
    }

    // ── Payments ──

    @Transactional(readOnly = true)
    public PageResponse<PaymentSummaryResponse> listPayments(int page, int size, String status, UUID institutionId) {
        Page<tz.elmkusoma.parent.domain.Payment> payments;
        if (status != null && !status.isBlank()) {
            payments = paymentRepository.findByStatusAndIsDeletedFalse(status.toUpperCase(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else {
            payments = paymentRepository.findAllByIsDeletedFalse(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        }

        List<PaymentSummaryResponse> content = payments.getContent().stream()
                .map(p -> PaymentSummaryResponse.builder()
                        .id(p.getId())
                        .parentId(p.getParentId())
                        .studentId(p.getStudentId())
                        .amount(p.getAmount())
                        .currency(p.getCurrency())
                        .status(p.getStatus())
                        .serviceType(p.getServiceType())
                        .createdAt(p.getCreatedAt())
                        .build())
                .toList();

        return new PageResponse<>(content, payments.getNumber(), payments.getSize(), payments.getTotalElements(), payments.getTotalPages(), payments.isFirst(), payments.isLast());
    }

    // ── Certificates ──

    @Transactional(readOnly = true)
    public PageResponse<CertificateSummaryResponse> listCertificates(int page, int size, UUID institutionId) {
        Page<Certificate> certs = certificateRepository.findAllByIsDeletedFalse(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<CertificateSummaryResponse> content = certs.getContent().stream()
                .map(c -> CertificateSummaryResponse.builder()
                        .id(c.getId())
                        .studentId(c.getStudentId())
                        .serialNumber(c.getSerialNumber())
                        .title(c.getTitle())
                        .issueDate(c.getIssueDate())
                        .status(c.getStatus() != null ? c.getStatus().name() : null)
                        .build())
                .toList();

        return new PageResponse<>(content, certs.getNumber(), certs.getSize(), certs.getTotalElements(), certs.getTotalPages(), certs.isFirst(), certs.isLast());
    }

    // ── Security ──

    @Transactional(readOnly = true)
    public List<SecurityEventResponse> getUnresolvedSecurityEvents() {
        return securityEventRepository.findByResolvedFalse().stream()
                .map(e -> SecurityEventResponse.builder()
                        .id(e.getId())
                        .userId(e.getUserId())
                        .eventType(e.getEventType() != null ? e.getEventType().name() : null)
                        .severity(e.getSeverity() != null ? e.getSeverity().name() : null)
                        .description(e.getDescription())
                        .ipAddress(e.getIpAddress())
                        .resolved(e.getResolved())
                        .createdAt(e.getCreatedAt())
                        .build())
                .toList();
    }

    public SecurityEventResponse resolveSecurityEvent(UUID eventId) {
        SecurityEvent event = securityEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("SecurityEvent", "id", eventId));
        event.setResolved(true);
        event.setResolvedAt(LocalDateTime.now());
        securityEventRepository.save(event);
        log.info("Security event {} resolved by platform admin", eventId);
        return SecurityEventResponse.builder()
                .id(event.getId())
                .userId(event.getUserId())
                .eventType(event.getEventType() != null ? event.getEventType().name() : null)
                .severity(event.getSeverity() != null ? event.getSeverity().name() : null)
                .description(event.getDescription())
                .ipAddress(event.getIpAddress())
                .resolved(true)
                .createdAt(event.getCreatedAt())
                .build();
    }

    // ── Audit ──

    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAuditLogs(int page, int size, String action, String entityType) {
        Page<AuditLog> logs;
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasAction = action != null && !action.isBlank();
        boolean hasEntity = entityType != null && !entityType.isBlank();
        try {
            if (hasAction && hasEntity) {
                AuditLog.AuditAction act = AuditLog.AuditAction.valueOf(action.toUpperCase());
                logs = auditLogRepository.findByActionAndEntityTypeAndIsDeletedFalse(act, entityType.toUpperCase(), pr);
            } else if (hasAction) {
                AuditLog.AuditAction act = AuditLog.AuditAction.valueOf(action.toUpperCase());
                logs = auditLogRepository.findByActionAndIsDeletedFalse(act, pr);
            } else if (hasEntity) {
                logs = auditLogRepository.findByEntityTypeAndIsDeletedFalse(entityType.toUpperCase(), pr);
            } else {
                logs = auditLogRepository.findAll(pr);
            }
        } catch (IllegalArgumentException e) {
            logs = auditLogRepository.findAll(pr);
        }

        return logs.getContent().stream()
                .map(log -> AuditLogResponse.builder()
                        .id(log.getId())
                        .userId(log.getUserId())
                        .performedBy(log.getUserEmail())
                        .userRole(log.getUserRole())
                        .action(log.getAction() != null ? log.getAction().name() : null)
                        .entityType(log.getEntityType())
                        .entityId(log.getEntityId())
                        .entityName(log.getEntityName())
                        .oldValues(log.getOldValues())
                        .newValues(log.getNewValues())
                        .ipAddress(log.getIpAddress())
                        .createdAt(log.getCreatedAt())
                        .build())
                .toList();
    }

    // ── Global Search ──

    @Transactional(readOnly = true)
    public List<GlobalSearchResult> globalSearch(String query, String type, int limit) {
        List<GlobalSearchResult> results = new ArrayList<>();
        String q = query == null ? "" : query.toLowerCase();

        if (type == null || type.equalsIgnoreCase("user") || type.equalsIgnoreCase("all")) {
            try {
                Page<User> users = userRepository.findBySearchTermAndIsDeletedFalse(query, PageRequest.of(0, limit));
                results.addAll(users.getContent().stream()
                        .map(u -> GlobalSearchResult.builder().type("USER").id(u.getId()).title((u.getFirstName() + " " + u.getLastName()).trim()).subtitle(u.getEmail()).build()).toList());
            } catch (Exception e) { log.debug("Search users failed: {}", e.getMessage()); }
        }
        if (type == null || type.equalsIgnoreCase("institution") || type.equalsIgnoreCase("all")) {
            try {
                Page<Institution> institutions = institutionRepository.findBySearchTermAndIsDeletedFalse(query, PageRequest.of(0, limit));
                results.addAll(institutions.getContent().stream().map(i -> GlobalSearchResult.builder().type("INSTITUTION").id(i.getId()).title(i.getName()).subtitle(i.getCode()).build()).toList());
            } catch (Exception e) { log.debug("Search institutions failed: {}", e.getMessage()); }
        }
        if (type == null || type.equalsIgnoreCase("live") || type.equalsIgnoreCase("live_class") || type.equalsIgnoreCase("all")) {
            try {
                List<LiveClass> lives = liveClassRepository.findAllByIsDeletedFalse(PageRequest.of(0, 100)).getContent().stream()
                        .filter(lc -> lc.getTitle() != null && lc.getTitle().toLowerCase().contains(q)).limit(limit).toList();
                results.addAll(lives.stream().map(lc -> GlobalSearchResult.builder().type("LIVE_CLASS").id(lc.getId()).title(lc.getTitle()).subtitle(lc.getStatus()).build()).toList());
            } catch (Exception e) { log.debug("Search live failed: {}", e.getMessage()); }
        }
        if (type == null || type.equalsIgnoreCase("certificate") || type.equalsIgnoreCase("all")) {
            try {
                List<Certificate> certs = certificateRepository.findAllByIsDeletedFalse(PageRequest.of(0, 100)).getContent().stream()
                        .filter(c -> (c.getTitle() != null && c.getTitle().toLowerCase().contains(q)) || (c.getSerialNumber() != null && c.getSerialNumber().toLowerCase().contains(q))).limit(limit).toList();
                results.addAll(certs.stream().map(c -> GlobalSearchResult.builder().type("CERTIFICATE").id(c.getId()).title(c.getTitle()).subtitle(c.getSerialNumber()).build()).toList());
            } catch (Exception e) { log.debug("Search cert failed: {}", e.getMessage()); }
        }
        if (type == null || type.equalsIgnoreCase("payment") || type.equalsIgnoreCase("transaction") || type.equalsIgnoreCase("all")) {
            try {
                List<tz.elmkusoma.parent.domain.Payment> pays = paymentRepository.findAllByIsDeletedFalse(PageRequest.of(0, 100)).getContent().stream()
                        .filter(p -> p.getId().toString().contains(q) || (p.getServiceType() != null && p.getServiceType().toLowerCase().contains(q))).limit(limit).toList();
                results.addAll(pays.stream().map(p -> GlobalSearchResult.builder().type("PAYMENT").id(p.getId()).title("Payment " + p.getAmount() + " " + p.getCurrency()).subtitle(p.getStatus()).build()).toList());
            } catch (Exception e) { log.debug("Search payment failed: {}", e.getMessage()); }
        }
        if (type == null || type.equalsIgnoreCase("incident") || type.equalsIgnoreCase("all")) {
            try {
                List<PlatformIncident> incs = incidentRepository.findByIsDeletedFalseOrderByDetectedAtDesc(PageRequest.of(0, 100)).getContent().stream()
                        .filter(i -> (i.getTitle() != null && i.getTitle().toLowerCase().contains(q)) || (i.getCategory() != null && i.getCategory().toLowerCase().contains(q))).limit(limit).toList();
                results.addAll(incs.stream().map(i -> GlobalSearchResult.builder().type("INCIDENT").id(i.getId()).title(i.getTitle()).subtitle(i.getSeverity() + " · " + i.getStatus()).build()).toList());
            } catch (Exception e) { log.debug("Search incident failed: {}", e.getMessage()); }
        }
        if (type == null || type.equalsIgnoreCase("service") || type.equalsIgnoreCase("all")) {
            try {
                List<PlatformService> svcs = platformServiceRepository.findByIsDeletedFalse(PageRequest.of(0, 100)).getContent().stream()
                        .filter(s -> s.getName() != null && s.getName().toLowerCase().contains(q)).limit(limit).toList();
                results.addAll(svcs.stream().map(s -> GlobalSearchResult.builder().type("SERVICE").id(s.getId()).title(s.getName()).subtitle(s.getCategory()).build()).toList());
            } catch (Exception e) { log.debug("Search service failed: {}", e.getMessage()); }
        }

        return results.stream().limit(limit).toList();
    }

    // ── Services ──

    @Transactional(readOnly = true)
    public PageResponse<ServiceSummaryResponse> listServices(int page, int size, String category) {
        Page<PlatformService> services;
        if (category != null && !category.isBlank()) {
            services = platformServiceRepository.findByCategoryAndIsDeletedFalse(category.toUpperCase(), PageRequest.of(page, size, Sort.by("name")));
        } else {
            services = platformServiceRepository.findByIsDeletedFalse(PageRequest.of(page, size, Sort.by("name")));
        }
        List<ServiceSummaryResponse> content = services.getContent().stream().map(s -> ServiceSummaryResponse.builder()
                .id(s.getId()).name(s.getName()).code(s.getCode()).description(s.getDescription())
                .category(s.getCategory()).isActive(s.getIsActive()).requiresVerification(s.getRequiresVerification())
                .maxSeats(s.getMaxSeats()).monthlyPrice(s.getMonthlyPrice()).currency(s.getCurrency())
                .createdAt(s.getCreatedAt()).build()).toList();
        return new PageResponse<>(content, services.getNumber(), services.getSize(), services.getTotalElements(), services.getTotalPages(), services.isFirst(), services.isLast());
    }

    public ServiceSummaryResponse createService(ServiceCreateRequest req) {
        PlatformService svc = PlatformService.builder()
                .name(req.getName()).code(req.getCode()).description(req.getDescription())
                .category(req.getCategory()).isActive(true).requiresVerification(req.getRequiresVerification() != null && req.getRequiresVerification())
                .maxSeats(req.getMaxSeats()).monthlyPrice(req.getMonthlyPrice()).currency(req.getCurrency() != null ? req.getCurrency() : "TZS")
                .build();
        platformServiceRepository.save(svc);
        log.info("Platform service created: {}", svc.getCode());
        return toServiceSummary(svc);
    }

    public ServiceSummaryResponse updateService(UUID id, ServiceCreateRequest req) {
        PlatformService svc = platformServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlatformService", "id", id));
        if (req.getName() != null) svc.setName(req.getName());
        if (req.getDescription() != null) svc.setDescription(req.getDescription());
        if (req.getCategory() != null) svc.setCategory(req.getCategory());
        if (req.getIsActive() != null) svc.setIsActive(req.getIsActive());
        if (req.getRequiresVerification() != null) svc.setRequiresVerification(req.getRequiresVerification());
        if (req.getMaxSeats() != null) svc.setMaxSeats(req.getMaxSeats());
        if (req.getMonthlyPrice() != null) svc.setMonthlyPrice(req.getMonthlyPrice());
        platformServiceRepository.save(svc);
        return toServiceSummary(svc);
    }

    // ── Incidents ──

    @Transactional(readOnly = true)
    public PageResponse<IncidentSummaryResponse> listIncidents(int page, int size, String status, String severity) {
        Page<PlatformIncident> incidents;
        if (status != null && !status.isBlank()) {
            incidents = incidentRepository.findByStatusAndIsDeletedFalseOrderByDetectedAtDesc(status.toUpperCase(), PageRequest.of(page, size));
        } else if (severity != null && !severity.isBlank()) {
            incidents = incidentRepository.findBySeverityAndIsDeletedFalseOrderByDetectedAtDesc(severity.toUpperCase(), PageRequest.of(page, size));
        } else {
            incidents = incidentRepository.findByIsDeletedFalseOrderByDetectedAtDesc(PageRequest.of(page, size));
        }
        List<IncidentSummaryResponse> content = incidents.getContent().stream().map(i -> IncidentSummaryResponse.builder()
                .id(i.getId()).title(i.getTitle()).description(i.getDescription()).category(i.getCategory())
                .severity(i.getSeverity()).status(i.getStatus()).affectedService(i.getAffectedService())
                .assignedTo(i.getAssignedTo()).detectedAt(i.getDetectedAt()).resolvedAt(i.getResolvedAt())
                .createdAt(i.getCreatedAt()).build()).toList();
        return new PageResponse<>(content, incidents.getNumber(), incidents.getSize(), incidents.getTotalElements(), incidents.getTotalPages(), incidents.isFirst(), incidents.isLast());
    }

    public IncidentSummaryResponse createIncident(IncidentCreateRequest req) {
        PlatformIncident inc = PlatformIncident.builder()
                .title(req.getTitle()).description(req.getDescription()).category(req.getCategory())
                .severity(req.getSeverity() != null ? req.getSeverity() : "MEDIUM")
                .status("DETECTED").affectedService(req.getAffectedService())
                .detectedAt(LocalDateTime.now()).build();
        incidentRepository.save(inc);
        log.info("Incident created: {}", inc.getId());
        return toIncidentSummary(inc);
    }

    public IncidentSummaryResponse updateIncidentStatus(UUID id, String newStatus, String notes) {
        PlatformIncident inc = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlatformIncident", "id", id));
        inc.setStatus(newStatus.toUpperCase());
        LocalDateTime now = LocalDateTime.now();
        switch (newStatus.toUpperCase()) {
            case "ACKNOWLEDGED" -> inc.setAcknowledgedAt(now);
            case "CONTAINED" -> inc.setContainedAt(now);
            case "RESOLVED" -> { inc.setResolvedAt(now); if (notes != null) inc.setResolutionNotes(notes); }
            case "REVIEWED" -> inc.setReviewedAt(now);
        }
        if (notes != null) inc.setResolutionNotes(notes);
        incidentRepository.save(inc);
        return toIncidentSummary(inc);
    }

    // ── Platform Config ──

    @Transactional(readOnly = true)
    public List<PlatformConfigResponse> listConfig(String category) {
        List<PlatformConfigEntry> entries;
        if (category != null && !category.isBlank()) {
            entries = configRepository.findByCategoryAndIsDeletedFalse(category);
        } else {
            entries = configRepository.findByIsDeletedFalse();
        }
        return entries.stream().map(c -> PlatformConfigResponse.builder()
                .id(c.getId()).configKey(c.getConfigKey()).configValue(c.getIsSensitive() ? "****" : c.getConfigValue())
                .configType(c.getConfigType()).description(c.getDescription()).category(c.getCategory())
                .isSensitive(c.getIsSensitive()).isPublic(c.getIsPublic())
                .lastModifiedBy(c.getLastModifiedBy()).updatedAt(c.getUpdatedAt()).build()).toList();
    }

    public PlatformConfigResponse updateConfig(String key, String value, String modifiedBy) {
        PlatformConfigEntry entry = configRepository.findByConfigKeyAndIsDeletedFalse(key)
                .orElseThrow(() -> new ResourceNotFoundException("PlatformConfig", "key", key));
        entry.setConfigValue(value);
        entry.setLastModifiedBy(modifiedBy);
        configRepository.save(entry);
        return PlatformConfigResponse.builder()
                .id(entry.getId()).configKey(entry.getConfigKey())
                .configValue(entry.getIsSensitive() ? "****" : entry.getConfigValue())
                .configType(entry.getConfigType()).description(entry.getDescription())
                .category(entry.getCategory()).isSensitive(entry.getIsSensitive())
                .isPublic(entry.getIsPublic()).lastModifiedBy(entry.getLastModifiedBy())
                .updatedAt(entry.getUpdatedAt()).build();
    }

    // ── Notifications ──

    @Transactional(readOnly = true)
    public PageResponse<NotificationSummaryResponse> listNotifications(int page, int size) {
        Page<PlatformNotification> notifs = notificationRepository.findByIsDeletedFalseOrderBySentAtDesc(PageRequest.of(page, size));
        List<NotificationSummaryResponse> content = notifs.getContent().stream().map(n -> NotificationSummaryResponse.builder()
                .id(n.getId()).title(n.getTitle()).message(n.getMessage()).notificationType(n.getNotificationType())
                .priority(n.getPriority()).targetAudience(n.getTargetAudience()).targetRole(n.getTargetRole())
                .sentBy(n.getSentBy()).sentAt(n.getSentAt()).readCount(n.getReadCount()).build()).toList();
        return new PageResponse<>(content, notifs.getNumber(), notifs.getSize(), notifs.getTotalElements(), notifs.getTotalPages(), notifs.isFirst(), notifs.isLast());
    }

    public NotificationSummaryResponse sendNotification(NotificationCreateRequest req, String sentBy) {
        PlatformNotification notif = PlatformNotification.builder()
                .title(req.getTitle()).message(req.getMessage()).notificationType(req.getNotificationType())
                .priority(req.getPriority() != null ? req.getPriority() : "NORMAL")
                .targetAudience(req.getTargetAudience()).targetRole(req.getTargetRole())
                .sentBy(sentBy).sentAt(LocalDateTime.now()).build();
        notificationRepository.save(notif);
        log.info("Platform notification sent: {} by {}", notif.getTitle(), sentBy);
        return NotificationSummaryResponse.builder()
                .id(notif.getId()).title(notif.getTitle()).message(notif.getMessage())
                .notificationType(notif.getNotificationType()).priority(notif.getPriority())
                .targetAudience(notif.getTargetAudience()).sentBy(notif.getSentBy())
                .sentAt(notif.getSentAt()).readCount(0).build();
    }

    // ── Delegations ──

    @Transactional(readOnly = true)
    public List<DelegationSummaryResponse> listDelegations() {
        return delegationRepository.findAll().stream()
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .filter(d -> !"EXPIRED".equals(d.getStatus()) || d.getExpiresAt() == null || d.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(d -> DelegationSummaryResponse.builder()
                        .id(d.getId()).delegatorId(d.getDelegatorId()).delegateId(d.getDelegateId())
                        .permissions(d.getPermissions()).scope(d.getScope()).status(d.getStatus())
                        .startsAt(d.getStartsAt()).expiresAt(d.getExpiresAt())
                        .createdAt(d.getCreatedAt()).build())
                .toList();
    }

    public DelegationSummaryResponse createDelegation(DelegationCreateRequest req) {
        AdminDelegation del = AdminDelegation.builder()
                .delegatorId(req.getDelegatorId()).delegateId(req.getDelegateId())
                .permissions(req.getPermissions()).scope(req.getScope() != null ? req.getScope() : "PLATFORM")
                .status("ACTIVE").startsAt(LocalDateTime.now()).expiresAt(req.getExpiresAt()).build();
        delegationRepository.save(del);
        log.info("Admin delegation created: {} -> {}", del.getDelegatorId(), del.getDelegateId());
        return DelegationSummaryResponse.builder()
                .id(del.getId()).delegatorId(del.getDelegatorId()).delegateId(del.getDelegateId())
                .permissions(del.getPermissions()).scope(del.getScope()).status(del.getStatus())
                .startsAt(del.getStartsAt()).expiresAt(del.getExpiresAt())
                .createdAt(del.getCreatedAt()).build();
    }

    public void revokeDelegation(UUID id, UUID revokedBy, String reason) {
        AdminDelegation del = delegationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminDelegation", "id", id));
        del.setStatus("REVOKED");
        del.setRevokedAt(LocalDateTime.now());
        del.setRevokedBy(revokedBy);
        del.setRevocationReason(reason);
        delegationRepository.save(del);
        log.info("Admin delegation revoked: {}", id);
    }

    // ── Verifications ──

    @Transactional(readOnly = true)
    public List<VerificationSummaryResponse> listPendingVerifications() {
        return verificationRepository.findByStatusAndIsDeletedFalse("PENDING").stream()
                .map(v -> VerificationSummaryResponse.builder()
                        .id(v.getId()).entityType(v.getEntityType()).entityId(v.getEntityId())
                        .verificationType(v.getVerificationType()).status(v.getStatus())
                        .submittedBy(v.getSubmittedBy()).submittedAt(v.getSubmittedAt())
                        .createdAt(v.getCreatedAt()).build())
                .toList();
    }

    public VerificationSummaryResponse reviewVerification(UUID id, UUID reviewedBy, String status, String notes) {
        VerificationRecord rec = verificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("VerificationRecord", "id", id));
        rec.setStatus(status.toUpperCase());
        rec.setReviewedBy(reviewedBy);
        rec.setReviewedAt(LocalDateTime.now());
        if (notes != null) rec.setNotes(notes);
        verificationRepository.save(rec);

        if ("APPROVED".equals(status.toUpperCase())) {
            applyVerificationSideEffects(rec);
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setInstitutionId(rec.getInstitutionId() != null ? rec.getInstitutionId() : PLATFORM_INSTITUTION_ID);
        auditLog.setEntityType("VerificationRecord");
        auditLog.setEntityId(id);
        auditLog.setEntityName("Verification Review");
        auditLog.setAction(AuditLog.AuditAction.UPDATE);
        auditLog.setUserId(reviewedBy);
        auditLog.setOldValues(Map.of("status", "PENDING"));
        auditLog.setNewValues(Map.of("status", status.toUpperCase()));
        auditLogRepository.save(auditLog);

        log.info("Verification {} reviewed: {} by {}", id, status, reviewedBy);

        return VerificationSummaryResponse.builder()
                .id(rec.getId()).entityType(rec.getEntityType()).entityId(rec.getEntityId())
                .verificationType(rec.getVerificationType()).status(rec.getStatus())
                .submittedBy(rec.getSubmittedBy()).reviewedBy(rec.getReviewedBy())
                .submittedAt(rec.getSubmittedAt()).reviewedAt(rec.getReviewedAt())
                .createdAt(rec.getCreatedAt()).build();
    }

    private void applyVerificationSideEffects(VerificationRecord rec) {
        try {
            switch (rec.getEntityType().toUpperCase()) {
                case "INSTITUTION" -> {
                    institutionRepository.findById(rec.getEntityId()).ifPresent(inst -> {
                        inst.setIsActive(true);
                        institutionRepository.save(inst);
                        log.info("Activated institution {} after verification approval", rec.getEntityId());
                    });
                }
                case "PROVIDER" -> {
                    userRepository.findById(rec.getEntityId()).ifPresent(user -> {
                        user.setIsActive(true);
                        userRepository.save(user);
                        log.info("Activated provider {} after verification approval", rec.getEntityId());
                    });
                }
                case "SERVICE" -> {
                    platformServiceRepository.findById(rec.getEntityId()).ifPresent(svc -> {
                        svc.setIsActive(true);
                        platformServiceRepository.save(svc);
                        log.info("Activated service {} after verification approval", rec.getEntityId());
                    });
                }
                default -> log.debug("No side effects defined for entity type: {}", rec.getEntityType());
            }
        } catch (Exception e) {
            log.warn("Failed to apply verification side effects for {}: {}", rec.getId(), e.getMessage());
        }
    }

    // ── Entitlements ──

    @Transactional(readOnly = true)
    public PageResponse<EntitlementSummaryResponse> listEntitlements(int page, int size, String status) {
        Page<tz.elmkusoma.parent.domain.Entitlement> entPage;
        if (status != null && !status.isBlank()) {
            entPage = entitlementRepository.findByStatusAndIsDeletedFalse(status.toUpperCase(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else {
            entPage = entitlementRepository.findAllByIsDeletedFalse(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        }
        List<EntitlementSummaryResponse> content = entPage.getContent().stream().map(e -> EntitlementSummaryResponse.builder()
                .id(e.getId()).userId(e.getUserId()).studentId(e.getStudentId())
                .serviceType(e.getServiceType()).serviceId(e.getServiceId())
                .status(e.getStatus()).startsAt(e.getStartsAt()).expiresAt(e.getExpiresAt())
                .createdAt(e.getCreatedAt()).build()).toList();
        return new PageResponse<>(content, entPage.getNumber(), entPage.getSize(), entPage.getTotalElements(), entPage.getTotalPages(), entPage.isFirst(), entPage.isLast());
    }

    // ── Enhanced Dashboard ──

    @Transactional(readOnly = true)
    public EnhancedPlatformDashboardResponse getEnhancedDashboard() {
        PlatformDashboardResponse base = getPlatformDashboard();
        long openIncidents = incidentRepository.countByStatusAndIsDeletedFalse("DETECTED") + incidentRepository.countByStatusAndIsDeletedFalse("INVESTIGATING");
        long pendingVerifications = verificationRepository.countByStatusAndIsDeletedFalse("PENDING");
        long activeServices = platformServiceRepository.countByIsActiveAndIsDeletedFalse(true);
        long totalNotifications = notificationRepository.countByIsDeletedFalse();
        long activeDelegations = delegationRepository.countByStatusAndIsDeletedFalse("ACTIVE");

        return EnhancedPlatformDashboardResponse.builder()
                .totalUsers(base.getTotalUsers())
                .totalStudents(base.getTotalStudents())
                .totalTeachers(base.getTotalTeachers())
                .totalParents(base.getTotalParents())
                .totalInstitutions(base.getTotalInstitutions())
                .totalLiveClasses(base.getTotalLiveClasses())
                .activeLiveClasses(base.getActiveLiveClasses())
                .totalPayments(base.getTotalPayments())
                .totalCertificates(base.getTotalCertificates())
                .unresolvedSecurityEvents(base.getUnresolvedSecurityEvents())
                .openIncidents(openIncidents)
                .pendingVerifications(pendingVerifications)
                .activeServices(activeServices)
                .totalNotifications(totalNotifications)
                .activeDelegations(activeDelegations)
                .build();
    }

    // ── Platform Learning/Content/Event/Media/Resources ──

    @Transactional(readOnly = true)
    public PageResponse<PlatformCourseResponse> listPlatformCourses(int page, int size, String search) {
        Page<tz.elmkusoma.course.domain.Course> p;
        if (search != null && !search.isBlank()) {
            List<tz.elmkusoma.course.domain.Course> filtered = courseRepository.searchByTitleAndIsDeletedFalse(search);
            int from = Math.min(page * size, filtered.size());
            int to = Math.min(from + size, filtered.size());
            List<PlatformCourseResponse> content = filtered.subList(from, to).stream().map(c -> PlatformCourseResponse.builder()
                    .id(c.getId()).institutionId(c.getInstitutionId()).title(c.getTitle()).level(c.getLevel()).category(c.getCategory())
                    .isPublished(c.getIsPublished()).isFeatured(c.getIsFeatured()).createdAt(c.getCreatedAt()).build()).toList();
            return new PageResponse<>(content, page, size, filtered.size(), (int) Math.ceil((double) filtered.size() / size), page == 0, to >= filtered.size());
        }
        p = courseRepository.findByIsDeletedFalse(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<PlatformCourseResponse> content = p.getContent().stream().map(c -> PlatformCourseResponse.builder()
                .id(c.getId()).institutionId(c.getInstitutionId()).title(c.getTitle()).level(c.getLevel()).category(c.getCategory())
                .isPublished(c.getIsPublished()).isFeatured(c.getIsFeatured()).createdAt(c.getCreatedAt()).build()).toList();
        return new PageResponse<>(content, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast());
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformEventResponse> listPlatformEvents(int page, int size, String search) {
        Page<tz.elmkusoma.event.domain.Event> p = eventRepository.findByIsDeletedFalse(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<PlatformEventResponse> content = p.getContent().stream()
                .filter(e -> search == null || search.isBlank() || (e.getTitle() != null && e.getTitle().toLowerCase().contains(search.toLowerCase())))
                .map(e -> PlatformEventResponse.builder().id(e.getId()).institutionId(e.getInstitutionId()).title(e.getTitle()).eventType(e.getEventType()).status(e.getStatus()).startsAt(e.getStartsAt()).createdAt(e.getCreatedAt()).build()).toList();
        return new PageResponse<>(content, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast());
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformMediaResponse> listPlatformMedia(int page, int size) {
        Page<tz.elmkusoma.liveclass.domain.MediaAsset> p = mediaAssetRepository.findByIsDeletedFalse(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<PlatformMediaResponse> content = p.getContent().stream().map(m -> PlatformMediaResponse.builder()
                .id(m.getId()).institutionId(m.getInstitutionId()).title(m.getTitle()).mediaType(m.getMediaType()).status(m.getStatus()).createdAt(m.getCreatedAt()).build()).toList();
        return new PageResponse<>(content, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast());
    }

    @Transactional(readOnly = true)
    public PageResponse<PlatformResourceResponse> listPlatformResources(int page, int size) {
        Page<tz.elmkusoma.learning.domain.Resource> p = resourceRepository.findByIsDeletedFalse(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<PlatformResourceResponse> content = p.getContent().stream().map(r -> PlatformResourceResponse.builder()
                .id(r.getId()).institutionId(r.getInstitutionId()).title(r.getTitle()).resourceType(r.getResourceType() != null ? r.getResourceType().name() : null).createdAt(r.getCreatedAt()).build()).toList();
        return new PageResponse<>(content, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast());
    }

    // ── Support Cases (M26) ──

    private static final Map<String, Set<String>> TICKET_TRANSITIONS = Map.of(
            "OPEN", Set.of("ASSIGNED", "INVESTIGATING", "RESOLVED", "CLOSED"),
            "ASSIGNED", Set.of("INVESTIGATING", "RESOLVED", "CLOSED", "OPEN"),
            "INVESTIGATING", Set.of("RESOLVED", "CLOSED"),
            "RESOLVED", Set.of("CLOSED", "INVESTIGATING"),
            "CLOSED", Set.of("OPEN")
    );

    @Transactional(readOnly = true)
    public PageResponse<SupportTicketResponse> listSupportTickets(int page, int size, String status) {
        Page<tz.elmkusoma.parent.domain.SupportTicket> p;
        if (status != null && !status.isBlank()) {
            p = supportTicketRepository.findByStatusAndIsDeletedFalse(status.toUpperCase(),
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else {
            p = supportTicketRepository.findByIsDeletedFalse(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        }
        List<SupportTicketResponse> content = p.getContent().stream().map(t -> SupportTicketResponse.builder()
                .id(t.getId()).userId(t.getUserId()).title(t.getSubject()).description(t.getDescription())
                .category(t.getCategory()).priority(t.getPriority()).status(t.getStatus())
                .assignedTo(t.getAssignedTo()).resolvedAt(t.getResolvedAt()).createdAt(t.getCreatedAt())
                .build()).toList();
        return new PageResponse<>(content, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast());
    }

    public SupportTicketResponse updateSupportTicketStatus(UUID ticketId, String status) {
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        String target = status.toUpperCase();
        tz.elmkusoma.parent.domain.SupportTicket t = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", "id", ticketId));
        String current = t.getStatus() != null ? t.getStatus() : "OPEN";
        if (!current.equals(target)) {
            if (!TICKET_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
                throw new IllegalStateException("Invalid ticket transition: " + current + " → " + target);
            }
            t.setStatus(target);
            if ("RESOLVED".equals(target) || "CLOSED".equals(target)) {
                t.setResolvedAt(LocalDateTime.now());
            }
            supportTicketRepository.save(t);
            writeAudit(t.getInstitutionId(), "SUPPORT_TICKET", ticketId, t.getSubject(), "UPDATE",
                    Map.of("status", current), Map.of("status", target));
            log.info("Support ticket {} status: {} → {}", ticketId, current, target);
        }
        return SupportTicketResponse.builder()
                .id(t.getId()).userId(t.getUserId()).title(t.getSubject()).description(t.getDescription())
                .category(t.getCategory()).priority(t.getPriority()).status(t.getStatus())
                .assignedTo(t.getAssignedTo()).resolvedAt(t.getResolvedAt()).createdAt(t.getCreatedAt())
                .build();
    }

    // ── Content Moderation (M10/M11) ──

    private static final Set<String> REPORT_ACTIONS = Set.of("REVIEWING", "RESOLVED", "DISMISSED");

    @Transactional(readOnly = true)
    public PageResponse<ContentReportResponse> listContentReports(int page, int size, String status) {
        Page<ContentReport> p;
        if (status != null && !status.isBlank()) {
            p = contentReportRepository.findByStatusAndIsDeletedFalse(status.toUpperCase(),
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else {
            p = contentReportRepository.findByIsDeletedFalse(
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        }
        List<ContentReportResponse> content = p.getContent().stream().map(r -> ContentReportResponse.builder()
                .id(r.getId()).entityType(r.getEntityType()).entityId(r.getEntityId()).entityTitle(r.getEntityTitle())
                .reporterId(r.getReporterId()).reason(r.getReason()).description(r.getDescription())
                .status(r.getStatus()).resolutionNotes(r.getResolutionNotes()).resolvedBy(r.getResolvedBy())
                .resolvedAt(r.getResolvedAt()).createdAt(r.getCreatedAt())
                .build()).toList();
        return new PageResponse<>(content, p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages(), p.isFirst(), p.isLast());
    }

    public ContentReportResponse createContentReport(ContentReportCreateRequest req) {
        ContentReport r = ContentReport.builder()
                .entityType(req.getEntityType().toUpperCase())
                .entityId(req.getEntityId())
                .entityTitle(req.getEntityTitle())
                .reporterId(req.getReporterId())
                .reason(req.getReason())
                .description(req.getDescription())
                .status("OPEN")
                .build();
        r.setInstitutionId(PLATFORM_INSTITUTION_ID);
        contentReportRepository.save(r);
        writeAudit(PLATFORM_INSTITUTION_ID, "CONTENT_REPORT", r.getId(), req.getEntityTitle(), "CREATE",
                Map.of(), Map.of("entityType", r.getEntityType(), "reason", r.getReason()));
        log.info("Content report created: {} on {}/{}", r.getId(), r.getEntityType(), r.getEntityId());
        return ContentReportResponse.builder()
                .id(r.getId()).entityType(r.getEntityType()).entityId(r.getEntityId()).entityTitle(r.getEntityTitle())
                .reporterId(r.getReporterId()).reason(r.getReason()).description(r.getDescription())
                .status(r.getStatus()).createdAt(r.getCreatedAt())
                .build();
    }

    public ContentReportResponse actOnContentReport(UUID reportId, ContentReportActionRequest req) {
        if (req.getAction() == null || !REPORT_ACTIONS.contains(req.getAction().toUpperCase())) {
            throw new IllegalArgumentException("action must be one of " + REPORT_ACTIONS);
        }
        String target = req.getAction().toUpperCase();
        ContentReport r = contentReportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("ContentReport", "id", reportId));
        String old = r.getStatus();
        r.setStatus(target);
        if ("RESOLVED".equals(target) || "DISMISSED".equals(target)) {
            r.setResolvedAt(LocalDateTime.now());
            if (req.getNotes() != null) r.setResolutionNotes(req.getNotes());
        } else if (req.getNotes() != null) {
            r.setResolutionNotes(req.getNotes());
        }
        contentReportRepository.save(r);
        writeAudit(r.getInstitutionId(), "CONTENT_REPORT", reportId, r.getEntityTitle(), "UPDATE",
                Map.of("status", old), Map.of("status", target));
        log.info("Content report {} action: {} → {}", reportId, old, target);
        return ContentReportResponse.builder()
                .id(r.getId()).entityType(r.getEntityType()).entityId(r.getEntityId()).entityTitle(r.getEntityTitle())
                .reporterId(r.getReporterId()).reason(r.getReason()).description(r.getDescription())
                .status(r.getStatus()).resolutionNotes(r.getResolutionNotes()).resolvedBy(r.getResolvedBy())
                .resolvedAt(r.getResolvedAt()).createdAt(r.getCreatedAt())
                .build();
    }

    // ── Data Governance Export (M24) ──

    public String exportPlatformData(String type) {
        String t = type != null ? type.toUpperCase() : "USERS";
        StringBuilder csv = new StringBuilder();
        switch (t) {
            case "INSTITUTIONS" -> {
                csv.append("id,name,code,type,city,status,created_at\n");
                institutionRepository.findByIsDeletedFalse(PageRequest.of(0, 10000)).forEach(i ->
                        csv.append(i.getId()).append(',').append(esc(i.getName())).append(',')
                                .append(i.getCode()).append(',')
                                .append(i.getType() != null ? i.getType() : "").append(',')
                                .append(esc(i.getCity())).append(',')
                                .append(i.getStatus() != null ? i.getStatus() : (Boolean.TRUE.equals(i.getIsActive()) ? "ACTIVE" : "INACTIVE")).append(',')
                                .append(i.getCreatedAt()).append('\n'));
            }
            case "AUDIT" -> {
                csv.append("id,entity_type,entity_id,action,user_id,created_at\n");
                auditLogRepository.findAll(PageRequest.of(0, 10000)).forEach(a ->
                        csv.append(a.getId()).append(',').append(a.getEntityType()).append(',')
                                .append(a.getEntityId()).append(',').append(a.getAction()).append(',')
                                .append(a.getUserId()).append(',').append(a.getCreatedAt()).append('\n'));
            }
            default -> {
                csv.append("id,email,first_name,last_name,role,is_active,created_at\n");
                userRepository.findAllByIsDeletedFalse(PageRequest.of(0, 10000)).forEach(u ->
                        csv.append(u.getId()).append(',').append(esc(u.getEmail())).append(',')
                                .append(esc(u.getFirstName())).append(',').append(esc(u.getLastName())).append(',')
                                .append(u.getRole() != null ? u.getRole() : "").append(',')
                                .append(u.getIsActive()).append(',').append(u.getCreatedAt()).append('\n'));
            }
        }
        writeAudit(PLATFORM_INSTITUTION_ID, "EXPORT", PLATFORM_INSTITUTION_ID, "Platform Export " + t, "EXPORT",
                Map.of(), Map.of("type", t, "rows", String.valueOf(csv.toString().lines().count() - 1)));
        log.info("Platform data export: {} ({} bytes)", t, csv.length());
        return csv.toString();
    }

    private String esc(String v) {
        if (v == null) return "";
        return v.contains(",") || v.contains("\"") || v.contains("\n")
                ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }

    // ── Mappers ──

    private UserSummaryResponse toUserSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .isActive(user.getIsActive())
                .institutionId(null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private InstitutionSummaryResponse toInstitutionSummary(Institution inst) {
        return InstitutionSummaryResponse.builder()
                .id(inst.getId())
                .name(inst.getName())
                .code(inst.getCode())
                .type(inst.getType() != null ? inst.getType().name() : null)
                .city(inst.getCity())
                .region(inst.getRegion())
                .isActive(inst.getIsActive())
                .status(inst.getStatus())
                .createdAt(inst.getCreatedAt())
                .build();
    }

    private ServiceSummaryResponse toServiceSummary(PlatformService s) {
        return ServiceSummaryResponse.builder()
                .id(s.getId()).name(s.getName()).code(s.getCode()).description(s.getDescription())
                .category(s.getCategory()).isActive(s.getIsActive()).requiresVerification(s.getRequiresVerification())
                .maxSeats(s.getMaxSeats()).monthlyPrice(s.getMonthlyPrice()).currency(s.getCurrency())
                .createdAt(s.getCreatedAt()).build();
    }

    private IncidentSummaryResponse toIncidentSummary(PlatformIncident i) {
        return IncidentSummaryResponse.builder()
                .id(i.getId()).title(i.getTitle()).description(i.getDescription()).category(i.getCategory())
                .severity(i.getSeverity()).status(i.getStatus()).affectedService(i.getAffectedService())
                .assignedTo(i.getAssignedTo()).detectedAt(i.getDetectedAt()).resolvedAt(i.getResolvedAt())
                .createdAt(i.getCreatedAt()).build();
    }
}
