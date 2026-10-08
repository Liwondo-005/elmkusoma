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
import tz.elmkusoma.event.dto.EventRequest;
import tz.elmkusoma.event.dto.EventResponse;
import tz.elmkusoma.event.service.EventService;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.config.security.OrganizationContextResolver;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import tz.elmkusoma.course.repository.CourseRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.liveclass.repository.MediaAssetRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.parent.repository.PaymentRepository;
import tz.elmkusoma.parent.repository.EntitlementRepository;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import tz.elmkusoma.identity.domain.PasswordResetToken;
import tz.elmkusoma.identity.repository.PasswordResetTokenRepository;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlatformAdminService {

    /** Platform-scope audit rows must reference the national HQ institution (audit_logs.institution_id NOT NULL + FK). */
    static final UUID PLATFORM_INSTITUTION_ID = UUID.fromString("a0000000-0000-0000-0000-000000000001");

    private static final com.fasterxml.jackson.databind.ObjectMapper PERMISSIONS_JSON =
            new com.fasterxml.jackson.databind.ObjectMapper();

    /**
     * admin_delegations.permissions is JSONB NOT NULL. Whatever the API caller sends
     * (bare token, comma list, or existing JSON), store a valid JSON document so the
     * insert can never fail with an invalid jsonb input syntax.
     */
    static String normalizePermissions(String raw) {
        if (raw == null || raw.isBlank()) return "[]";
        String trimmed = raw.trim();
        try {
            PERMISSIONS_JSON.readTree(trimmed);
            return trimmed;
        } catch (Exception notJson) {
            try {
                return PERMISSIONS_JSON.writeValueAsString(trimmed);
            } catch (Exception impossible) {
                return "[]";
            }
        }
    }

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
    private final LiveClassParticipantRepository liveClassParticipantRepository;
    private final ResourceRepository resourceRepository;
    private final ProviderServiceEntitlementRepository providerEntitlementRepository;
    private final ContentReportRepository contentReportRepository;
    private final tz.elmkusoma.parent.repository.SupportTicketRepository supportTicketRepository;
    private final PlatformFeatureRepository featureRepository;
    private final RolePermissionRepository rolePermissionRepository;
    // Audit X-5: provider delivery counters for the platform registry.
    private final tz.elmkusoma.nfe.provider.repository.EducationProviderRepository educationProviderRepository;
    private final tz.elmkusoma.nfe.program.repository.NfeProgramRepository nfeProgramRepository;
    private final tz.elmkusoma.nfe.learner.repository.NfeLearnerRepository nfeLearnerRepository;
    private final tz.elmkusoma.nfe.session.repository.NfeSessionRepository nfeSessionRepository;
    private final tz.elmkusoma.nfe.certificate.repository.NfeCertificateRepository nfeCertificateRepository;
    private final tz.elmkusoma.administration.repository.AdminUserPermissionRepository adminUserPermissionRepository;
    private final DashboardSnapshotRepository snapshotRepository;
    private final tz.elmkusoma.learner.repository.LearnerNotificationRepository learnerNotificationRepository;
    private final PlatformIntegrationService integrationService;
    private final BackupStatusService backupStatusService;
    private final InstitutionMembershipRepository membershipRepository;
    private final tz.elmkusoma.config.security.PermissionCacheService permissionCacheService;
    private final tz.elmkusoma.event.service.EventService eventService;
    private final tz.elmkusoma.identity.repository.PasswordResetTokenRepository passwordResetTokenRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    // ── Command Center ──

    @Transactional(readOnly = true)
    public PlatformDashboardResponse getPlatformDashboard() {
        long totalUsers = userRepository.countByIsDeletedFalse();
        long totalStudents = userRepository.countByRoleAndIsDeletedFalse(User.Role.STUDENT);
        long totalTeachers = userRepository.countByRoleAndIsDeletedFalse(User.Role.TEACHER);
        long totalParents = userRepository.countByRoleAndIsDeletedFalse(User.Role.PARENT);
        long totalInstitutions = institutionRepository.countByIsDeletedFalse();
        long totalLiveClasses = liveClassRepository.countByIsDeletedFalse();
        long activeLiveClasses = liveClassRepository.countByStatusInAndIsDeletedFalse(List.of("IN_PROGRESS", "LIVE", "STARTING"));
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

        long activeLiveClasses = liveClassRepository.countByStatusInAndIsDeletedFalse(List.of("IN_PROGRESS", "LIVE", "STARTING"));
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
        long activeUsers = 0; long activeInstitutions = 0;
        try {
            totalUsers = userRepository.countByIsDeletedFalse();
            totalInstitutions = institutionRepository.countByIsDeletedFalse();
            activeUsers = userRepository.countByIsActiveAndIsDeletedFalse(true);
            activeInstitutions = institutionRepository.countByIsActiveAndIsDeletedFalse(true);
        } catch (Exception e) {
            dbStatus = "Failing";
            log.warn("Health DB probe failed: {}", e.getMessage());
        }
        String livekit = "UNKNOWN";
        String storage = "UNKNOWN";
        String payments = "UNKNOWN";
        String notifications = "UNKNOWN";
        try {
            livekit = integrationService.probe("livekit").getConnectionStatus();
            storage = integrationService.probe("storage").getConnectionStatus();
            payments = integrationService.probe("payment").getConnectionStatus();
            notifications = integrationService.probe("notification").getConnectionStatus();
        } catch (Exception e) {
            log.warn("Health integration probes degraded: {}", e.getMessage());
        }
        String heartbeat = configRepository.findByConfigKeyAndIsDeletedFalse("ops.scheduler.heartbeat")
                .map(tz.elmkusoma.administration.domain.PlatformConfigEntry::getConfigValue).orElse(null);
        boolean jobsOk = heartbeat != null
                && java.time.LocalDateTime.parse(heartbeat.replace(" ", "T")).isAfter(LocalDateTime.now().minusMinutes(5));
        return PlatformHealthResponse.builder()
                .databaseStatus(dbStatus)
                .apiStatus("Operational".equals(dbStatus) ? "Operational" : "Degraded")
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .totalInstitutions(totalInstitutions)
                .activeInstitutions(activeInstitutions)
                .livekitStatus(livekit)
                .storageStatus(storage)
                .backgroundJobsStatus(jobsOk ? "Operational" : (heartbeat == null ? "UNKNOWN" : "Stale"))
                .notificationsStatus(notifications)
                .paymentsStatus(payments)
                .realtimeStatus(livekit)
                .mediaStatus(storage)
                .heartbeatAt(heartbeat)
                .build();
    }

    // ── Users ──

    @Transactional(readOnly = true)
    public PageResponse<UserSummaryResponse> listUsers(int page, int size, String role, String search) {
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
        writeAudit(user.getInstitutionId() != null ? user.getInstitutionId() : PLATFORM_INSTITUTION_ID,
                "USER", userId, user.getEmail(), "UPDATE",
                Map.of("isActive", !active), Map.of("isActive", active));
        log.info("User {} {} by platform admin", userId, active ? "activated" : "suspended");
        return toUserSummary(user);
    }

    /** Audit B-14: binds provider roles to the institution's single provider record, when it exists. */
    private UUID resolveProviderScope(User.Role role, UUID institutionId) {
        if (role != User.Role.PROVIDER_ADMIN && role != User.Role.PROVIDER_STAFF) {
            return null;
        }
        return educationProviderRepository.findAllByInstitutionId(institutionId).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .map(tz.elmkusoma.nfe.provider.domain.EducationProvider::getId)
                .findFirst()
                .orElse(null);
    }

    /** Audit B-02: roles whose authority is scoped to one organization. */
    private static boolean requiresInstitution(User.Role role) {
        if (role == null) {
            return false;
        }
        return switch (role) {
            case INSTITUTION_ADMIN, PROVIDER_ADMIN, PROVIDER_STAFF, TEACHER, INSTRUCTOR,
                 NATIONAL_ADMIN, REGIONAL_ADMIN, DISTRICT_ADMIN -> true;
            default -> false;
        };
    }

    @Transactional
    public UserSummaryResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }
        // Audit B-02: organisation-scoped roles were creatable without an institution, producing an
        // account that logs in but resolves no scope -- every provider/administration API call then
        // failed with 400 and the provider workspace was unreachable. Reject it at the boundary.
        if (requiresInstitution(request.getRole()) && request.getInstitutionId() == null) {
            throw new IllegalArgumentException(
                    "institutionId is required when creating a user with role " + request.getRole().name());
        }
        if (request.getInstitutionId() != null
                && !institutionRepository.existsByIdAndIsDeletedFalse(request.getInstitutionId())) {
            throw new IllegalArgumentException("Institution " + request.getInstitutionId() + " does not exist");
        }
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                // §11: provisioning binds the account to its organization scope — without this
                // the user resolves no institution and every scope check fails open or denies.
                .institutionId(request.getInstitutionId())
                .isActive(true)
                .isEmailVerified(false)
                .build();
        user = userRepository.save(user);
        // §11: an active membership makes the account visible to the people directory,
        // scope services and permission computation for the provisioned organization.
        if (request.getInstitutionId() != null) {
            InstitutionMembership membership = InstitutionMembership.builder()
                    .userId(user.getId())
                    .institutionId(request.getInstitutionId())
                    .role(OrganizationContextResolver.mapUserRoleToMembershipRole(request.getRole()))
                    .isActive(true)
                    .isDeleted(false)
                    // Per-provider scope (B-14): a provider account is bound to the institution's
                    // provider record so its authority is that provider, not the whole institution.
                    .providerId(resolveProviderScope(request.getRole(), request.getInstitutionId()))
                    .build();
            membershipRepository.save(membership);
        }
        writeAudit(PLATFORM_INSTITUTION_ID, "USER", user.getId(), user.getEmail(), "CREATE",
                Map.of(), Map.of("email", user.getEmail(), "role", user.getRole().name()));
        log.info("User created by platform admin: {}", user.getEmail());
        return toUserSummary(user);
    }

    @Transactional
    public UserSummaryResponse updateUser(UUID userId, UpdateUserRequest request) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        String oldRole = user.getRole() != null ? user.getRole().name() : null;
        
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            if (!request.getIsActive() && User.Role.ADMIN.equals(user.getRole())) {
                long activeAdmins = userRepository.countByRoleAndIsDeletedFalse(User.Role.ADMIN);
                long activeCount = userRepository.findByRoleAndIsDeletedFalse(User.Role.ADMIN, PageRequest.of(0, 1000)).getContent().stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
                if (activeCount <= 1) {
                    throw new IllegalStateException("Cannot suspend the last active Platform Admin");
                }
            }
            user.setIsActive(request.getIsActive());
        }
        if (request.getIsEmailVerified() != null) user.setIsEmailVerified(request.getIsEmailVerified());
        
        userRepository.save(user);
        writeAudit(user.getInstitutionId() != null ? user.getInstitutionId() : PLATFORM_INSTITUTION_ID,
                "USER", userId, user.getEmail(), "UPDATE",
                auditValues("role", oldRole), auditValues("role", user.getRole() != null ? user.getRole().name() : null));
        log.info("User updated by platform admin: {}", user.getEmail());
        return toUserSummary(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (User.Role.ADMIN.equals(user.getRole())) {
            long activeAdmins = userRepository.countByRoleAndIsDeletedFalse(User.Role.ADMIN);
            long activeCount = userRepository.findByRoleAndIsDeletedFalse(User.Role.ADMIN, PageRequest.of(0, 1000)).getContent().stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
            if (activeCount <= 1) {
                throw new IllegalStateException("Cannot delete the last active Platform Admin");
            }
        }
        user.setIsDeleted(true);
        userRepository.save(user);
        writeAudit(user.getInstitutionId() != null ? user.getInstitutionId() : PLATFORM_INSTITUTION_ID,
                "USER", userId, user.getEmail(), "DELETE", Map.of(), Map.of());
        log.info("User deleted by platform admin: {}", user.getEmail());
    }

    @Transactional
    public void resetUserPassword(UUID userId, String newPassword) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        // Invalidate every previously issued session/token for this account.
        long currentVersion = user.getSecurityVersion() != null ? user.getSecurityVersion() : 1L;
        user.setSecurityVersion(currentVersion + 1);
        userRepository.save(user);
        writeAudit(user.getInstitutionId() != null ? user.getInstitutionId() : PLATFORM_INSTITUTION_ID,
                "USER", userId, user.getEmail(), "RESET_PASSWORD", Map.of(), Map.of());
        log.info("Password reset by platform admin for user: {}", user.getEmail());
    }

    /**
     * SHA-256 hex of an opaque token. Reset tokens are stored hashed, exactly
     * like refresh-token revocation hashes.
     */
    private static String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    @Transactional
    public void sendPasswordResetLink(UUID userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(hashToken(UUID.randomUUID().toString() + UUID.randomUUID().toString()))
                .userId(user.getId())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);
        // TODO: Send email with reset link
        log.info("Password reset link sent to user: {}", user.getEmail());
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
                .regionId(inst.getRegionId())
                .districtId(inst.getDistrictId())
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

    private static Map<String, Object> auditValues(Object... keyValues) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            values.put((String) keyValues[i], keyValues[i + 1]);
        }
        return values;
    }

    private void writeAudit(UUID institutionId, String entityType, UUID entityId, String entityName, String action,
                            Map<String, Object> oldValues, Map<String, Object> newValues) {
        AuditLog al = new AuditLog();
        al.setInstitutionId(institutionId != null ? institutionId : PLATFORM_INSTITUTION_ID);
        al.setEntityType(entityType);
        al.setEntityId(entityId);
        al.setEntityName(entityName);
        al.setAction(AuditLog.AuditAction.valueOf(action));
        al.setOldValues(oldValues);
        al.setNewValues(newValues);
        applyRequestActor(al);
        auditLogRepository.save(al);
    }

    /**
     * Best-effort actor + request attribution for audit rows: the authenticated user
     * (userId/userEmail/userRole set by {@code JwtRequestAttributeFilter}) plus request
     * metadata. No-ops outside an HTTP request (unit tests, schedulers) so audits still record.
     */
    private void applyRequestActor(AuditLog al) {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                if (al.getUserId() == null && attrs.getAttribute("userId",
                        org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST) instanceof UUID uid) {
                    al.setUserId(uid);
                }
                if (al.getUserEmail() == null && attrs.getAttribute("userEmail",
                        org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST) instanceof String email) {
                    al.setUserEmail(email);
                }
                if (al.getUserRole() == null && attrs.getAttribute("userRole",
                        org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST) instanceof String role) {
                    al.setUserRole(role);
                }
                jakarta.servlet.http.HttpServletRequest req = sra.getRequest();
                al.setIpAddress(req.getRemoteAddr());
                al.setUserAgent(req.getHeader("User-Agent"));
                al.setRequestMethod(req.getMethod());
                al.setRequestUrl(req.getRequestURI());
            }
            if (al.getUserEmail() == null) {
                var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal()
                        instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
                    al.setUserEmail(userDetails.getUsername());
                    al.setUserRole(userDetails.getAuthorities().stream().findFirst().map(a -> {
                        String s = a.getAuthority();
                        return s.startsWith("ROLE_") ? s.substring(5) : s;
                    }).orElse(null));
                }
            }
        } catch (Exception e) {
            log.debug("Audit actor context unavailable: {}", e.getMessage());
        }
    }

    // ── Live Classes ──

    @Transactional(readOnly = true)
    public PageResponse<LiveClassSummaryResponse> listLiveClasses(int page, int size, String status) {
        Page<LiveClass> classes;
        if (status != null && !status.isBlank()) {
            String wanted = status.trim().toUpperCase();
            List<String> statuses = "LIVE".equals(wanted) || "STARTING".equals(wanted)
                    ? List.of(wanted, "IN_PROGRESS")
                    : List.of(wanted);
            classes = liveClassRepository.findByStatusInAndIsDeletedFalse(statuses, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt")));
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
                        .currentParticipants((int) liveClassParticipantRepository
                                .countByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(lc.getId()))
                        .createdAt(lc.getCreatedAt())
                        .build())
                .toList();

        return new PageResponse<>(content, classes.getNumber(), classes.getSize(), classes.getTotalElements(), classes.getTotalPages(), classes.isFirst(), classes.isLast());
    }

    // ── Payments ──

    @Transactional(readOnly = true)
    public PageResponse<PaymentSummaryResponse> listPayments(int page, int size, String status) {
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

    /**
     * Range sentinels for the platform certificate search. Postgres cannot infer the type of a
     * parameter that only appears in an IS NULL check (error 42P18), so omitted date filters are
     * bound as this wide inclusive range instead of NULL. certificates.issue_date is NOT NULL,
     * therefore no row can be excluded by the sentinels.
     */
    static final LocalDateTime FILTER_FROM_SENTINEL = LocalDateTime.of(1000, 1, 1, 0, 0);
    static final LocalDateTime FILTER_TO_SENTINEL = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    @Transactional(readOnly = true)
    public PageResponse<CertificateSummaryResponse> listCertificates(int page, int size,
                                                                     String search, String status, String type,
                                                                     UUID institutionId,
                                                                     LocalDateTime fromDate, LocalDateTime toDate) {
        Certificate.CertificateStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = Certificate.CertificateStatus.valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                statusEnum = null; // unknown status -> treated as no filter (matches previous behaviour)
            }
        }
        Certificate.CertificateType typeEnum = null;
        if (type != null && !type.isBlank()) {
            try {
                typeEnum = Certificate.CertificateType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                typeEnum = null;
            }
        }
        // '' (never null): an untyped null :search would reach Postgres as bytea and break LOWER/LIKE.
        String searchTrim = (search == null || search.isBlank()) ? "" : search.trim();

        Page<Certificate> certs = certificateRepository.searchCertificates(
                searchTrim, statusEnum, typeEnum, institutionId,
                fromDate != null ? fromDate : FILTER_FROM_SENTINEL,
                toDate != null ? toDate : FILTER_TO_SENTINEL,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        Map<UUID, String> institutionNames = new HashMap<>();
        certs.getContent().stream()
                .map(Certificate::getInstitutionId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .forEach(id -> institutionRepository.findById(id)
                        .ifPresent(inst -> institutionNames.put(id, inst.getName())));

        List<CertificateSummaryResponse> content = certs.getContent().stream()
                .map(c -> CertificateSummaryResponse.builder()
                        .id(c.getId())
                        .studentId(c.getStudentId())
                        .serialNumber(c.getSerialNumber())
                        .certificateNumber(c.getCertificateNumber())
                        .title(c.getTitle())
                        .studentName(c.getStudentName())
                        .certificateType(c.getCertificateType() != null ? c.getCertificateType().name() : null)
                        .courseOrProgramme(c.getCourseOrProgramme())
                        .institutionId(c.getInstitutionId())
                        .institutionName(institutionNames.get(c.getInstitutionId()))
                        .issueDate(c.getIssueDate())
                        .expiryDate(c.getExpiryDate())
                        .status(c.getStatus() != null ? c.getStatus().name() : null)
                        .createdAt(c.getCreatedAt())
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
        writeAudit(event.getInstitutionId(), "SECURITY_EVENT", eventId,
                event.getEventType() != null ? event.getEventType().name() : "SecurityEvent", "UPDATE",
                Map.of("resolved", false), Map.of("resolved", true));
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
    public List<AuditLogResponse> getAuditLogs(int page, int size, String action, String entityType, UUID entityId) {
        Page<AuditLog> logs;
        PageRequest pr = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean hasAction = action != null && !action.isBlank();
        boolean hasEntity = entityType != null && !entityType.isBlank();
        try {
            if (entityId != null) {
                // Filter by a specific resource (e.g. one certificate's audit trail)
                logs = auditLogRepository.findByEntityIdAndIsDeletedFalse(entityId, pr);
            } else if (hasAction && hasEntity) {
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
        if (type == null || type.equalsIgnoreCase("course") || type.equalsIgnoreCase("all")) {
            try {
                List<tz.elmkusoma.course.domain.Course> cs = courseRepository.searchByTitleAndIsDeletedFalse(query).stream().limit(limit).toList();
                results.addAll(cs.stream().map(c -> GlobalSearchResult.builder().type("COURSE").id(c.getId()).title(c.getTitle()).subtitle(c.getCategory()).build()).toList());
            } catch (Exception e) { log.debug("Search course failed: {}", e.getMessage()); }
        }
        if (type == null || type.equalsIgnoreCase("event") || type.equalsIgnoreCase("all")) {
            try {
                List<tz.elmkusoma.event.domain.Event> evs = eventRepository.findByIsDeletedFalse(PageRequest.of(0, 100)).getContent().stream()
                        .filter(e -> e.getTitle() != null && e.getTitle().toLowerCase().contains(q)).limit(limit).toList();
                results.addAll(evs.stream().map(e -> GlobalSearchResult.builder().type("EVENT").id(e.getId()).title(e.getTitle()).subtitle(e.getStatus()).build()).toList());
            } catch (Exception e) { log.debug("Search event failed: {}", e.getMessage()); }
        }
        if (type == null || type.equalsIgnoreCase("resource") || type.equalsIgnoreCase("all")) {
            try {
                List<tz.elmkusoma.learning.domain.Resource> rs = resourceRepository.findByIsDeletedFalse(PageRequest.of(0, 100)).getContent().stream()
                        .filter(r -> r.getTitle() != null && r.getTitle().toLowerCase().contains(q)).limit(limit).toList();
                results.addAll(rs.stream().map(r -> GlobalSearchResult.builder().type("RESOURCE").id(r.getId()).title(r.getTitle()).subtitle(r.getResourceType() != null ? r.getResourceType().name() : null).build()).toList());
            } catch (Exception e) { log.debug("Search resource failed: {}", e.getMessage()); }
        }
        if (type == null || type.equalsIgnoreCase("media") || type.equalsIgnoreCase("all")) {
            try {
                List<tz.elmkusoma.liveclass.domain.MediaAsset> ms = mediaAssetRepository.findByIsDeletedFalse(PageRequest.of(0, 100)).getContent().stream()
                        .filter(m -> m.getTitle() != null && m.getTitle().toLowerCase().contains(q)).limit(limit).toList();
                results.addAll(ms.stream().map(m -> GlobalSearchResult.builder().type("MEDIA").id(m.getId()).title(m.getTitle()).subtitle(m.getMediaType()).build()).toList());
            } catch (Exception e) { log.debug("Search media failed: {}", e.getMessage()); }
        }
        if (type == null || type.equalsIgnoreCase("entitlement") || type.equalsIgnoreCase("all")) {
            try {
                List<tz.elmkusoma.parent.domain.Entitlement> ens = entitlementRepository.findAllByIsDeletedFalse(PageRequest.of(0, 100)).getContent().stream()
                        .filter(e -> e.getServiceType() != null && e.getServiceType().toLowerCase().contains(q)).limit(limit).toList();
                results.addAll(ens.stream().map(e -> GlobalSearchResult.builder().type("ENTITLEMENT").id(e.getId()).title(String.valueOf(e.getServiceType())).subtitle(e.getStatus()).build()).toList());
            } catch (Exception e) { log.debug("Search entitlement failed: {}", e.getMessage()); }
        }
        if (type == null || type.equalsIgnoreCase("audit") || type.equalsIgnoreCase("all")) {
            try {
                List<AuditLog> als = auditLogRepository.findAll(PageRequest.of(0, 100)).getContent().stream()
                        .filter(a -> (a.getEntityType() != null && a.getEntityType().toLowerCase().contains(q))
                                || (a.getEntityName() != null && a.getEntityName().toLowerCase().contains(q)))
                        .limit(limit).toList();
                results.addAll(als.stream().map(a -> GlobalSearchResult.builder().type("AUDIT").id(a.getId())
                        .title(a.getEntityType() + " " + (a.getAction() != null ? a.getAction() : ""))
                        .subtitle(a.getEntityName()).build()).toList());
            } catch (Exception e) { log.debug("Search audit failed: {}", e.getMessage()); }
        }
        if (type == null || type.equalsIgnoreCase("teacher") || type.equalsIgnoreCase("all")) {
            try {
                Page<User> teachers = userRepository.findByRoleAndIsDeletedFalse(User.Role.TEACHER, PageRequest.of(0, 100));
                List<User> tFiltered = teachers.getContent().stream()
                        .filter(u -> u.getEmail() != null && u.getEmail().toLowerCase().contains(q)
                                || (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(q))
                                || (u.getLastName() != null && u.getLastName().toLowerCase().contains(q)))
                        .limit(limit).toList();
                results.addAll(tFiltered.stream().map(u -> GlobalSearchResult.builder().type("TEACHER").id(u.getId())
                        .title(u.getFullName()).subtitle(u.getEmail()).build()).toList());
            } catch (Exception e) { log.debug("Search teacher failed: {}", e.getMessage()); }
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
        writeAudit(PLATFORM_INSTITUTION_ID, "SERVICE", svc.getId(), svc.getName(), "CREATE",
                Map.of(), Map.of("code", String.valueOf(svc.getCode()),
                        "category", String.valueOf(svc.getCategory())));
        log.info("Platform service created: {}", svc.getCode());
        return toServiceSummary(svc);
    }

    public ServiceSummaryResponse updateService(UUID id, ServiceCreateRequest req) {
        PlatformService svc = platformServiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlatformService", "id", id));
        Map<String, Object> oldVals = Map.of(
                "name", String.valueOf(svc.getName()),
                "category", String.valueOf(svc.getCategory()),
                "isActive", String.valueOf(svc.getIsActive()));
        if (req.getName() != null) svc.setName(req.getName());
        if (req.getDescription() != null) svc.setDescription(req.getDescription());
        if (req.getCategory() != null) svc.setCategory(req.getCategory());
        if (req.getIsActive() != null) svc.setIsActive(req.getIsActive());
        if (req.getRequiresVerification() != null) svc.setRequiresVerification(req.getRequiresVerification());
        if (req.getMaxSeats() != null) svc.setMaxSeats(req.getMaxSeats());
        if (req.getMonthlyPrice() != null) svc.setMonthlyPrice(req.getMonthlyPrice());
        platformServiceRepository.save(svc);
        writeAudit(PLATFORM_INSTITUTION_ID, "SERVICE", id, svc.getName(), "UPDATE",
                oldVals, Map.of("name", String.valueOf(svc.getName()),
                        "category", String.valueOf(svc.getCategory()),
                        "isActive", String.valueOf(svc.getIsActive())));
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

    private static final Map<String, Set<String>> INCIDENT_TRANSITIONS = Map.of(
            "DETECTED", Set.of("INVESTIGATING"),
            "INVESTIGATING", Set.of("CONTAINED", "RESOLVED"),
            "CONTAINED", Set.of("RESOLVED"),
            "RESOLVED", Set.of("REVIEWED"),
            "REVIEWED", Set.of()
    );

    public IncidentSummaryResponse createIncident(IncidentCreateRequest req) {
        PlatformIncident inc = PlatformIncident.builder()
                .title(req.getTitle()).description(req.getDescription()).category(req.getCategory())
                .severity(req.getSeverity() != null ? req.getSeverity() : "MEDIUM")
                .status("DETECTED").affectedService(req.getAffectedService())
                .detectedAt(LocalDateTime.now()).build();
        incidentRepository.save(inc);
        writeAudit(PLATFORM_INSTITUTION_ID, "INCIDENT", inc.getId(), inc.getTitle(), "CREATE",
                Map.of(), Map.of("status", "DETECTED", "severity", inc.getSeverity()));
        log.info("Incident created: {}", inc.getId());
        return toIncidentSummary(inc);
    }

    public IncidentSummaryResponse updateIncidentStatus(UUID id, String newStatus, String notes) {
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        String target = newStatus.toUpperCase();
        if (!"ACKNOWLEDGED".equals(target) && !INCIDENT_TRANSITIONS.containsKey(target)) {
            throw new IllegalArgumentException("Invalid incident status: " + target
                    + ". Allowed: DETECTED, INVESTIGATING, CONTAINED, RESOLVED, REVIEWED");
        }
        if ("ACKNOWLEDGED".equals(target)) target = "INVESTIGATING";
        PlatformIncident inc = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PlatformIncident", "id", id));
        String current = inc.getStatus() != null ? inc.getStatus() : "DETECTED";
        if (!current.equals(target)) {
            Set<String> allowed = INCIDENT_TRANSITIONS.getOrDefault(current, Set.of());
            if (!allowed.contains(target)) {
                throw new IllegalStateException("Invalid incident transition: " + current + " → " + target);
            }
            inc.setStatus(target);
            LocalDateTime now = LocalDateTime.now();
            switch (target) {
                case "INVESTIGATING" -> inc.setAcknowledgedAt(now);
                case "CONTAINED" -> inc.setContainedAt(now);
                case "RESOLVED" -> { inc.setResolvedAt(now); if (notes != null) inc.setResolutionNotes(notes); }
                case "REVIEWED" -> inc.setReviewedAt(now);
                default -> { }
            }
            if (notes != null && !"RESOLVED".equals(target)) inc.setResolutionNotes(notes);
            incidentRepository.save(inc);
            writeAudit(PLATFORM_INSTITUTION_ID, "INCIDENT", id, inc.getTitle(), "UPDATE",
                    Map.of("status", current), Map.of("status", target,
                            "notes", notes != null ? notes : ""));
            log.info("Incident {} status: {} → {}", id, current, target);
        }
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
        boolean sensitive = Boolean.TRUE.equals(entry.getIsSensitive());
        String oldValue = sensitive ? "****" : String.valueOf(entry.getConfigValue());
        String newValue = sensitive ? "****" : String.valueOf(value);
        entry.setConfigValue(value);
        entry.setLastModifiedBy(modifiedBy);
        configRepository.save(entry);
        writeAudit(PLATFORM_INSTITUTION_ID, "PLATFORM_CONFIG", entry.getId(), key, "UPDATE",
                Map.of("value", oldValue),
                Map.of("value", newValue, "modifiedBy", String.valueOf(modifiedBy)));
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
        writeAudit(PLATFORM_INSTITUTION_ID, "NOTIFICATION", notif.getId(), notif.getTitle(), "CREATE",
                Map.of(), Map.of("notificationType", String.valueOf(notif.getNotificationType()),
                        "targetAudience", String.valueOf(notif.getTargetAudience()),
                        "sentBy", String.valueOf(sentBy)));
        log.info("Platform notification sent: {} by {}", notif.getTitle(), sentBy);
        return NotificationSummaryResponse.builder()
                .id(notif.getId()).title(notif.getTitle()).message(notif.getMessage())
                .notificationType(notif.getNotificationType()).priority(notif.getPriority())
                .targetAudience(notif.getTargetAudience()).sentBy(notif.getSentBy())
                .sentAt(notif.getSentAt()).readCount(0).build();
    }

    // ── Delegations ──

    /** Authority types supported by the platform governance model. Do not invent others in the UI. */
    public static final Set<String> DELEGATION_AUTHORITIES = Set.of(
            "PROVIDER_VERIFICATION", "INSTITUTION_REVIEW", "COMPLIANCE_REVIEW",
            "INCIDENT_MANAGEMENT", "PLATFORM_SUPPORT", "CONTENT_GOVERNANCE",
            "SERVICE_GOVERNANCE", "GENERAL_ADMIN");

    /** Granular permission tokens usable inside a delegation. */
    public static final Set<String> DELEGATION_PERMISSIONS = Set.of(
            "VIEW", "REVIEW", "APPROVE", "VERIFY", "REJECT", "SUSPEND", "REACTIVATE", "MANAGE");

    private static final Set<String> TERMINAL_DELEGATION_STATUSES = Set.of("REVOKED", "EXPIRED", "REJECTED");

    private String resolveUserName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(User::getFullName).orElse(null);
    }

    private String resolveUserEmail(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(User::getEmail).orElse(null);
    }

    private List<UUID> parseResourceIds(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            com.fasterxml.jackson.databind.JsonNode node = PERMISSIONS_JSON.readTree(json);
            List<UUID> ids = new ArrayList<>();
            if (node.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode el : node) {
                    try { ids.add(UUID.fromString(el.asText())); } catch (Exception ignored) {}
                }
            }
            return ids;
        } catch (Exception e) {
            return List.of();
        }
    }

    private Set<String> parsePermissionTokens(String json) {
        Set<String> tokens = new HashSet<>();
        if (json == null || json.isBlank()) return tokens;
        String trimmed = json.trim();
        try {
            com.fasterxml.jackson.databind.JsonNode node = PERMISSIONS_JSON.readTree(trimmed);
            if (node.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode el : node) tokens.add(el.asText().trim().toUpperCase());
            } else {
                tokens.add(node.asText().trim().toUpperCase());
            }
        } catch (Exception e) {
            for (String part : trimmed.split("[,;\\s]+")) {
                if (!part.isBlank()) tokens.add(part.trim().toUpperCase());
            }
        }
        return tokens;
    }

    private boolean isDelegationEffective(AdminDelegation d, LocalDateTime now) {
        if (!"ACTIVE".equals(d.getStatus())) return false;
        if (d.getStartsAt() != null && now.isBefore(d.getStartsAt())) return false;
        if (d.getExpiresAt() != null && !now.isBefore(d.getExpiresAt())) return false;
        return true;
    }

    private DelegationSummaryResponse toDelegationSummary(AdminDelegation d) {
        return DelegationSummaryResponse.builder()
                .id(d.getId()).delegatorId(d.getDelegatorId()).delegatorName(resolveUserName(d.getDelegatorId()))
                .delegateId(d.getDelegateId()).delegateName(resolveUserName(d.getDelegateId()))
                .permissions(d.getPermissions()).scope(d.getScope()).authority(d.getAuthority())
                .status(d.getStatus()).startsAt(d.getStartsAt()).expiresAt(d.getExpiresAt())
                .createdAt(d.getCreatedAt()).build();
    }

    @Transactional(readOnly = true)
    public List<DelegationSummaryResponse> listDelegations() {
        return delegationRepository.findAll().stream()
                .filter(d -> !Boolean.TRUE.equals(d.getIsDeleted()))
                .filter(d -> !"EXPIRED".equals(d.getStatus()) || d.getExpiresAt() == null || d.getExpiresAt().isAfter(LocalDateTime.now()))
                .map(this::toDelegationSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public DelegationDetailResponse getDelegation(UUID id) {
        AdminDelegation d = delegationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminDelegation", "id", id));
        List<UUID> resourceIds = parseResourceIds(d.getResourceIds());
        List<String> resourceNames = resourceIds.stream()
                .map(rid -> institutionRepository.findById(rid).map(Institution::getName).orElse(rid.toString()))
                .toList();
        return DelegationDetailResponse.builder()
                .id(d.getId()).delegatorId(d.getDelegatorId())
                .delegatorName(resolveUserName(d.getDelegatorId())).delegatorEmail(resolveUserEmail(d.getDelegatorId()))
                .delegateId(d.getDelegateId())
                .delegateName(resolveUserName(d.getDelegateId())).delegateEmail(resolveUserEmail(d.getDelegateId()))
                .authority(d.getAuthority()).permissions(d.getPermissions()).scope(d.getScope())
                .resourceIds(resourceIds).resourceNames(resourceNames).status(d.getStatus())
                .reason(d.getReason()).notes(d.getNotes())
                .startsAt(d.getStartsAt()).expiresAt(d.getExpiresAt())
                .approvedBy(d.getApprovedBy()).approvedByName(resolveUserName(d.getApprovedBy()))
                .approvedAt(d.getApprovedAt()).rejectedAt(d.getRejectedAt()).rejectionReason(d.getRejectionReason())
                .revokedAt(d.getRevokedAt()).revokedBy(d.getRevokedBy())
                .revokedByName(resolveUserName(d.getRevokedBy())).revocationReason(d.getRevocationReason())
                .createdAt(d.getCreatedAt())
                .currentlyEffective(isDelegationEffective(d, LocalDateTime.now()))
                .build();
    }

    public DelegationSummaryResponse createDelegation(DelegationCreateRequest req) {
        if (req.getDelegatorId() == null || req.getDelegateId() == null) {
            throw new IllegalArgumentException("delegatorId and delegateId are required");
        }
        if (req.getDelegatorId().equals(req.getDelegateId())) {
            throw new IllegalArgumentException("Delegator and delegate must be different users");
        }
        userRepository.findByIdAndIsDeletedFalse(req.getDelegatorId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", req.getDelegatorId()));
        userRepository.findByIdAndIsDeletedFalse(req.getDelegateId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", req.getDelegateId()));
        String authority = req.getAuthority() != null ? req.getAuthority().trim().toUpperCase() : "GENERAL_ADMIN";
        if (!DELEGATION_AUTHORITIES.contains(authority)) {
            throw new IllegalArgumentException("Unsupported authority: " + req.getAuthority()
                    + ". Allowed: " + DELEGATION_AUTHORITIES);
        }
        Set<String> tokens = parsePermissionTokens(req.getPermissions());
        if (tokens.isEmpty()) {
            throw new IllegalArgumentException("At least one permission is required");
        }
        for (String token : tokens) {
            if (!DELEGATION_PERMISSIONS.contains(token)) {
                throw new IllegalArgumentException("Unsupported permission: " + token
                        + ". Allowed: " + DELEGATION_PERMISSIONS);
            }
        }
        String resourceJson = null;
        if (req.getResourceIds() != null && !req.getResourceIds().isEmpty()) {
            try {
                resourceJson = PERMISSIONS_JSON.writeValueAsString(req.getResourceIds().stream().map(UUID::toString).toList());
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid resourceIds");
            }
        }
        boolean requiresApproval = Boolean.TRUE.equals(req.getRequiresApproval());
        AdminDelegation del = AdminDelegation.builder()
                .delegatorId(req.getDelegatorId()).delegateId(req.getDelegateId())
                .permissions(normalizePermissions(req.getPermissions()))
                .scope(req.getScope() != null && !req.getScope().isBlank() ? req.getScope().trim().toUpperCase() : "PLATFORM")
                .authority(authority).reason(req.getReason()).notes(req.getNotes()).resourceIds(resourceJson)
                .status(requiresApproval ? "PENDING_APPROVAL" : "ACTIVE")
                .startsAt(req.getStartsAt() != null ? req.getStartsAt() : LocalDateTime.now())
                .expiresAt(req.getExpiresAt()).build();
        if (del.getExpiresAt() != null && !del.getExpiresAt().isAfter(del.getStartsAt())) {
            throw new IllegalArgumentException("expiresAt must be after startsAt");
        }
        delegationRepository.save(del);
        writeAudit(PLATFORM_INSTITUTION_ID, "DELEGATION", del.getId(), authority + " / " + del.getScope(), "CREATE",
                Map.of(), Map.of("delegatorId", String.valueOf(del.getDelegatorId()),
                        "delegateId", String.valueOf(del.getDelegateId()),
                        "authority", authority,
                        "permissions", String.valueOf(del.getPermissions()),
                        "status", del.getStatus()));
        log.info("Admin delegation created: {} -> {} ({} / {})", del.getDelegatorId(), del.getDelegateId(), authority, del.getScope());
        return toDelegationSummary(del);
    }

    public DelegationSummaryResponse approveDelegation(UUID id, UUID decidedBy, String reason) {
        AdminDelegation del = delegationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminDelegation", "id", id));
        if (!"PENDING_APPROVAL".equals(del.getStatus())) {
            throw new IllegalStateException("Only PENDING_APPROVAL delegations can be approved (current: " + del.getStatus() + ")");
        }
        del.setStatus("ACTIVE");
        del.setApprovedBy(decidedBy);
        del.setApprovedAt(LocalDateTime.now());
        if (reason != null && !reason.isBlank()) del.setNotes(appendNote(del.getNotes(), "Approval note: " + reason));
        delegationRepository.save(del);
        writeAudit(PLATFORM_INSTITUTION_ID, "DELEGATION", id, del.getAuthority() + " / " + del.getScope(), "UPDATE",
                Map.of("status", "PENDING_APPROVAL"),
                Map.of("status", "ACTIVE", "approvedBy", String.valueOf(decidedBy)));
        log.info("Admin delegation approved: {}", id);
        return toDelegationSummary(del);
    }

    public DelegationSummaryResponse rejectDelegation(UUID id, UUID decidedBy, String reason) {
        AdminDelegation del = delegationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminDelegation", "id", id));
        if (!"PENDING_APPROVAL".equals(del.getStatus())) {
            throw new IllegalStateException("Only PENDING_APPROVAL delegations can be rejected (current: " + del.getStatus() + ")");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A rejection reason is required");
        }
        del.setStatus("REJECTED");
        del.setRejectedAt(LocalDateTime.now());
        del.setRejectionReason(reason);
        delegationRepository.save(del);
        writeAudit(PLATFORM_INSTITUTION_ID, "DELEGATION", id, del.getAuthority() + " / " + del.getScope(), "UPDATE",
                Map.of("status", "PENDING_APPROVAL"),
                Map.of("status", "REJECTED", "rejectedBy", String.valueOf(decidedBy), "reason", reason));
        log.info("Admin delegation rejected: {}", id);
        return toDelegationSummary(del);
    }

    public DelegationSummaryResponse extendDelegation(UUID id, LocalDateTime expiresAt, String reason) {
        AdminDelegation del = delegationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminDelegation", "id", id));
        if (TERMINAL_DELEGATION_STATUSES.contains(del.getStatus())) {
            throw new IllegalStateException("Terminal delegations (" + del.getStatus() + ") cannot be extended. Create a new delegation instead.");
        }
        if (expiresAt == null || (del.getExpiresAt() != null && !expiresAt.isAfter(del.getExpiresAt()))) {
            throw new IllegalArgumentException("New expiry must be after the current expiry");
        }
        LocalDateTime oldExpiry = del.getExpiresAt();
        del.setExpiresAt(expiresAt);
        if ("EXPIRED".equals(del.getStatus())) {
            del.setStatus("ACTIVE");
        }
        if (reason != null && !reason.isBlank()) del.setNotes(appendNote(del.getNotes(), "Extended: " + reason));
        delegationRepository.save(del);
        writeAudit(PLATFORM_INSTITUTION_ID, "DELEGATION", id, del.getAuthority() + " / " + del.getScope(), "UPDATE",
                Map.of("expiresAt", String.valueOf(oldExpiry)),
                Map.of("expiresAt", String.valueOf(expiresAt), "reason", reason != null ? reason : ""));
        log.info("Admin delegation extended: {} -> {}", id, expiresAt);
        return toDelegationSummary(del);
    }

    private String appendNote(String existing, String addition) {
        if (existing == null || existing.isBlank()) return addition;
        return existing + "\n" + addition;
    }

    public void revokeDelegation(UUID id, UUID revokedBy, String reason) {
        AdminDelegation del = delegationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdminDelegation", "id", id));
        if (TERMINAL_DELEGATION_STATUSES.contains(del.getStatus())) {
            throw new IllegalStateException("Delegation is already " + del.getStatus() + " and cannot be revoked");
        }
        String oldStatus = del.getStatus();
        del.setStatus("REVOKED");
        del.setRevokedAt(LocalDateTime.now());
        del.setRevokedBy(revokedBy);
        del.setRevocationReason(reason);
        delegationRepository.save(del);
        writeAudit(PLATFORM_INSTITUTION_ID, "DELEGATION", id, String.valueOf(del.getScope()), "UPDATE",
                Map.of("status", oldStatus),
                Map.of("status", "REVOKED", "revokedBy", String.valueOf(revokedBy),
                        "reason", reason != null ? reason : ""));
        log.info("Admin delegation revoked: {}", id);
    }

    /**
     * Backend authorization check: does the delegate currently hold the given authority
     * (with the required permission token) over the given scope/resource?
     * Scope formats: PLATFORM | NATIONAL | REGION:&lt;name&gt; | DISTRICT:&lt;name&gt;
     * | INSTITUTION:&lt;uuid&gt; | PROVIDER:&lt;uuid&gt;. When resourceIds are set on the
     * delegation, the resource must additionally be listed there.
     */
    @Transactional(readOnly = true)
    public boolean hasDelegatedAuthority(UUID delegateId, String authority, String requiredPermission,
                                         UUID resourceId, String region) {
        if (delegateId == null || authority == null) return false;
        LocalDateTime now = LocalDateTime.now();
        return delegationRepository.findByDelegateIdAndIsDeletedFalse(delegateId).stream()
                .filter(d -> isDelegationEffective(d, now))
                .filter(d -> authority.equalsIgnoreCase(d.getAuthority()))
                .filter(d -> {
                    Set<String> tokens = parsePermissionTokens(d.getPermissions());
                    return tokens.contains("MANAGE")
                            || (requiredPermission != null && tokens.contains(requiredPermission.toUpperCase()));
                })
                .anyMatch(d -> scopeCovers(d, resourceId, region)
                        && resourcesCover(d, resourceId));
    }

    private boolean scopeCovers(AdminDelegation d, UUID resourceId, String region) {
        String scope = d.getScope() != null ? d.getScope().trim().toUpperCase() : "PLATFORM";
        if ("PLATFORM".equals(scope) || "NATIONAL".equals(scope)) return true;
        if (scope.startsWith("REGION:") && region != null) {
            return scope.substring("REGION:".length()).trim().equalsIgnoreCase(region.trim());
        }
        if (scope.startsWith("DISTRICT:") && region != null) {
            return scope.substring("DISTRICT:".length()).trim().equalsIgnoreCase(region.trim());
        }
        if ((scope.startsWith("INSTITUTION:") || scope.startsWith("PROVIDER:")) && resourceId != null) {
            String idPart = scope.substring(scope.indexOf(':') + 1).trim();
            try {
                return UUID.fromString(idPart).equals(resourceId);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private boolean resourcesCover(AdminDelegation d, UUID resourceId) {
        List<UUID> ids = parseResourceIds(d.getResourceIds());
        if (ids.isEmpty()) return true;
        return resourceId != null && ids.contains(resourceId);
    }

    @Transactional(readOnly = true)
    public List<AdminDelegation> findEffectiveDelegations(UUID delegateId, String authority) {
        LocalDateTime now = LocalDateTime.now();
        return delegationRepository.findByDelegateIdAndIsDeletedFalse(delegateId).stream()
                .filter(d -> isDelegationEffective(d, now))
                .filter(d -> authority == null || authority.equalsIgnoreCase(d.getAuthority()))
                .toList();
    }

    // ── Provider Governance (providers ARE institutions of provider types — no duplicate model) ──

    public static final Set<String> PROVIDER_TYPES = Set.of(
            "TRAINING_PROVIDER", "PROFESSIONAL_BODY", "COMPANY", "NGO",
            "GOVERNMENT", "CONTENT_PROVIDER", "EVENT_PROVIDER");

    private static final Set<String> PLATFORM_ADMIN_ROLES = Set.of(
            "ADMIN", "NATIONAL_ADMIN", "REGIONAL_ADMIN", "DISTRICT_ADMIN");

    private boolean isProviderType(Institution inst) {
        return inst.getType() != null && PROVIDER_TYPES.contains(inst.getType().name());
    }

    private String resolveVerificationStatus(UUID institutionId) {
        List<VerificationRecord> records =
                verificationRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse("INSTITUTION", institutionId);
        if (records.isEmpty()) return "NONE";
        VerificationRecord latest = records.stream()
                .max(Comparator.comparing(VerificationRecord::getSubmittedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
        if (latest == null) return "NONE";
        String status = latest.getStatus() != null ? latest.getStatus().toUpperCase() : "NONE";
        if ("APPROVED".equals(status)) return "VERIFIED";
        return status;
    }

    private int countInstitutionAdmins(UUID institutionId) {
        return (int) membershipRepository.findByInstitutionIdAndIsActiveTrue(institutionId).stream()
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .count();
    }

    private ProviderRegistryItem toProviderRegistryItem(Institution inst) {
        // Audit X-5: surface the provider's real delivery activity so the platform can see what a
        // provider has actually produced, not only its verification paperwork.
        UUID institutionId = inst.getId();
        int programs = (int) nfeProgramRepository.countByInstitutionId(institutionId);
        int learners = (int) nfeLearnerRepository.countByInstitutionId(institutionId);
        int sessions = (int) nfeSessionRepository.countByInstitutionId(institutionId);
        int certificates = (int) nfeCertificateRepository.countByInstitutionId(institutionId);
        var providerRows = educationProviderRepository.findAllByInstitutionId(institutionId).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .toList();
        return ProviderRegistryItem.builder()
                .id(inst.getId()).name(inst.getName()).code(inst.getCode())
                .type(inst.getType() != null ? inst.getType().name() : null)
                .city(inst.getCity()).region(inst.getRegion())
                .isActive(inst.getIsActive()).status(inst.getStatus())
                .verificationStatus(resolveVerificationStatus(institutionId))
                .adminCount(countInstitutionAdmins(institutionId))
                .programs(programs)
                .learners(learners)
                .sessions(sessions)
                .certificates(certificates)
                .providerRecordPresent(!providerRows.isEmpty())
                .providerVerified(providerRows.stream()
                        .anyMatch(p -> Boolean.TRUE.equals(p.getIsVerified())))
                .createdAt(inst.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProviderRegistryItem> listProviders(String search, String type, String status,
                                                            String verification, int page, int size) {
        List<Institution> providers = institutionRepository.findByIsDeletedFalse().stream()
                .filter(this::isProviderType)
                .toList();
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            providers = providers.stream()
                    .filter(i -> (i.getName() != null && i.getName().toLowerCase().contains(q))
                            || (i.getCode() != null && i.getCode().toLowerCase().contains(q))
                            || (i.getCity() != null && i.getCity().toLowerCase().contains(q)))
                    .toList();
        }
        if (type != null && !type.isBlank()) {
            providers = providers.stream()
                    .filter(i -> i.getType() != null && i.getType().name().equalsIgnoreCase(type.trim()))
                    .toList();
        }
        if (status != null && !status.isBlank()) {
            String s = status.trim().toUpperCase();
            providers = providers.stream()
                    .filter(i -> s.equals(i.getStatus() != null ? i.getStatus().toUpperCase() : "ACTIVE"))
                    .toList();
        }
        List<ProviderRegistryItem> items = providers.stream()
                .map(this::toProviderRegistryItem)
                .toList();
        if (verification != null && !verification.isBlank()) {
            String v = verification.trim().toUpperCase();
            items = items.stream()
                    .filter(i -> v.equals(i.getVerificationStatus()))
                    .toList();
        }
        items = items.stream()
                .sorted(Comparator.comparing(ProviderRegistryItem::getName,
                        Comparator.nullsFirst(String::compareToIgnoreCase)))
                .toList();
        int total = items.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<ProviderRegistryItem> content = items.subList(from, to);
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PageResponse<>(content, page, size, total, totalPages, page == 0, to >= total);
    }

    @Transactional(readOnly = true)
    public ProviderDetailResponse getProviderDetail(UUID providerId) {
        Institution inst = institutionRepository.findByIdAndIsDeletedFalse(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", providerId));
        if (!isProviderType(inst)) {
            throw new IllegalArgumentException("Institution " + providerId + " is not a provider (type=" +
                    (inst.getType() != null ? inst.getType().name() : null) + ")");
        }
        List<ProviderVerificationItem> history =
                verificationRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse("INSTITUTION", providerId).stream()
                        .sorted(Comparator.comparing(VerificationRecord::getSubmittedAt,
                                Comparator.nullsFirst(Comparator.reverseOrder())))
                        .map(v -> ProviderVerificationItem.builder()
                                .id(v.getId()).verificationType(v.getVerificationType()).status(v.getStatus())
                                .submittedBy(v.getSubmittedBy()).submittedByName(resolveUserName(v.getSubmittedBy()))
                                .submittedAt(v.getSubmittedAt())
                                .reviewedBy(v.getReviewedBy()).reviewedByName(resolveUserName(v.getReviewedBy()))
                                .reviewedAt(v.getReviewedAt()).notes(v.getNotes()).documents(v.getDocuments())
                                .build())
                        .toList();
        List<ProviderQuotaResponse> entitlements =
                providerEntitlementRepository.findByProviderIdAndIsDeletedFalse(providerId).stream()
                        .map(ent -> {
                            PlatformService svc = platformServiceRepository.findById(ent.getServiceId()).orElse(null);
                            return ProviderQuotaResponse.builder()
                                    .id(ent.getId()).providerId(ent.getProviderId()).serviceId(ent.getServiceId())
                                    .serviceName(svc != null ? svc.getName() : null)
                                    .serviceCode(svc != null ? svc.getCode() : null)
                                    .status(ent.getStatus())
                                    .seatsUsed(ent.getSeatsUsed()).maxSeats(ent.getMaxSeats())
                                    .expiresAt(ent.getExpiresAt()).createdAt(ent.getCreatedAt())
                                    .build();
                        })
                        .toList();
        List<String> compliance = buildComplianceFlags(inst, history);
        List<AuditLogResponse> recentAudit = auditLogRepository
                .findByEntityIdAndIsDeletedFalse(providerId, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent().stream()
                .map(log -> AuditLogResponse.builder()
                        .id(log.getId()).userId(log.getUserId()).performedBy(log.getUserEmail())
                        .userRole(log.getUserRole())
                        .action(log.getAction() != null ? log.getAction().name() : null)
                        .entityType(log.getEntityType()).entityId(log.getEntityId()).entityName(log.getEntityName())
                        .oldValues(log.getOldValues()).newValues(log.getNewValues())
                        .ipAddress(log.getIpAddress()).createdAt(log.getCreatedAt())
                        .build())
                .toList();
        return ProviderDetailResponse.builder()
                .id(inst.getId()).name(inst.getName()).code(inst.getCode())
                .type(inst.getType() != null ? inst.getType().name() : null)
                .description(inst.getDescription()).address(inst.getAddress()).city(inst.getCity())
                .region(inst.getRegion()).country(inst.getCountry()).phone(inst.getPhone())
                .email(inst.getEmail()).website(inst.getWebsite()).logoUrl(inst.getLogoUrl())
                .isActive(inst.getIsActive()).status(inst.getStatus())
                .enabledServices(inst.getEnabledServices())
                .verificationStatus(resolveVerificationStatus(providerId))
                .approvedAt(inst.getApprovedAt()).approvedBy(inst.getApprovedBy())
                .createdAt(inst.getCreatedAt()).updatedAt(inst.getUpdatedAt())
                .admins(listInstitutionMembers(providerId))
                .verificationHistory(history)
                .serviceEntitlements(entitlements)
                .complianceFlags(compliance)
                .recentAudit(recentAudit)
                .build();
    }

    /** Honest compliance flags derived only from real persisted state — never invented. */
    private List<String> buildComplianceFlags(Institution inst, List<ProviderVerificationItem> history) {
        List<String> flags = new ArrayList<>();
        if ((inst.getEmail() == null || inst.getEmail().isBlank())
                && (inst.getPhone() == null || inst.getPhone().isBlank())) {
            flags.add("MISSING_CONTACT");
        }
        if (Boolean.FALSE.equals(inst.getIsActive()) || "SUSPENDED".equalsIgnoreCase(inst.getStatus())) {
            flags.add("SUSPENDED");
        }
        if (history.stream().anyMatch(v -> "PENDING".equalsIgnoreCase(v.getStatus()))) {
            flags.add("VERIFICATION_PENDING");
        }
        if (!history.isEmpty() && history.stream().noneMatch(v -> "APPROVED".equalsIgnoreCase(v.getStatus()))
                && history.stream().noneMatch(v -> "PENDING".equalsIgnoreCase(v.getStatus()))) {
            flags.add("NOT_VERIFIED");
        }
        if (history.stream().anyMatch(v -> "CHANGES_REQUIRED".equalsIgnoreCase(v.getStatus()))) {
            flags.add("CHANGES_REQUESTED");
        }
        if (history.isEmpty() && inst.getApprovedAt() == null) {
            flags.add("NEVER_VERIFIED");
        }
        if (countInstitutionAdmins(inst.getId()) == 0) {
            flags.add("NO_ADMINS");
        }
        return flags;
    }

    @Transactional(readOnly = true)
    public List<ProviderAttentionResponse> getProviderAttention() {
        List<ProviderAttentionResponse> out = new ArrayList<>();
        String base = "/dashboard/platform-admin/providers/";
        for (Institution inst : institutionRepository.findByIsDeletedFalse()) {
            if (!isProviderType(inst)) continue;
            String url = base + inst.getId();
            if ("SUSPENDED".equalsIgnoreCase(inst.getStatus()) || Boolean.FALSE.equals(inst.getIsActive())) {
                out.add(ProviderAttentionResponse.builder()
                        .providerId(inst.getId()).providerName(inst.getName())
                        .severity("HIGH").category("SUSPENDED")
                        .title("Provider suspended").description(inst.getName() + " is currently suspended.")
                        .actionUrl(url).build());
            }
            List<VerificationRecord> records =
                    verificationRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse("INSTITUTION", inst.getId());
            boolean pending = records.stream().anyMatch(r -> "PENDING".equalsIgnoreCase(r.getStatus()));
            boolean changes = records.stream().anyMatch(r -> "CHANGES_REQUIRED".equalsIgnoreCase(r.getStatus()));
            boolean verified = records.stream().anyMatch(r -> "APPROVED".equalsIgnoreCase(r.getStatus()))
                    || inst.getApprovedAt() != null;
            if (pending) {
                out.add(ProviderAttentionResponse.builder()
                        .providerId(inst.getId()).providerName(inst.getName())
                        .severity("HIGH").category("PENDING_VERIFICATION")
                        .title("Verification pending").description(inst.getName() + " has a pending verification request.")
                        .actionUrl(url).build());
            }
            if (changes) {
                out.add(ProviderAttentionResponse.builder()
                        .providerId(inst.getId()).providerName(inst.getName())
                        .severity("MEDIUM").category("CHANGES_REQUESTED")
                        .title("Changes requested").description(inst.getName() + " must resubmit verification evidence.")
                        .actionUrl(url).build());
            }
            if (!verified && !pending && Boolean.TRUE.equals(inst.getIsActive())) {
                out.add(ProviderAttentionResponse.builder()
                        .providerId(inst.getId()).providerName(inst.getName())
                        .severity("MEDIUM").category("NEVER_VERIFIED")
                        .title("Never verified").description(inst.getName() + " is active but has no verification record.")
                        .actionUrl(url).build());
            }
            if ((inst.getEmail() == null || inst.getEmail().isBlank())
                    && (inst.getPhone() == null || inst.getPhone().isBlank())) {
                out.add(ProviderAttentionResponse.builder()
                        .providerId(inst.getId()).providerName(inst.getName())
                        .severity("LOW").category("MISSING_INFORMATION")
                        .title("Missing contact information").description(inst.getName() + " has no email or phone on file.")
                        .actionUrl(url).build());
            }
        }
        return out;
    }

    /**
     * Delegation-enforced provider verification review (spec §033).
     * The actor must be a platform-level admin OR hold an ACTIVE delegation with
     * authority PROVIDER_VERIFICATION covering the entity's scope. Frontend button
     * hiding is NOT security — this check runs on every call.
     */
    public VerificationSummaryResponse reviewProviderVerification(UUID verificationId, UUID actorId,
                                                                  String status, String notes) {
        VerificationRecord rec = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("VerificationRecord", "id", verificationId));
        if (!"PENDING".equalsIgnoreCase(rec.getStatus()) && !"CHANGES_REQUIRED".equalsIgnoreCase(rec.getStatus())) {
            throw new IllegalStateException("Verification " + verificationId + " is already " + rec.getStatus());
        }
        String action = status != null ? status.trim().toUpperCase() : "";
        if (!Set.of("APPROVED", "REJECTED", "CHANGES_REQUIRED").contains(action)) {
            throw new IllegalArgumentException("Invalid review status: " + status);
        }
        String requiredPermission = "APPROVED".equals(action) ? "VERIFY"
                : "REJECTED".equals(action) ? "REJECT" : "REVIEW";
        UUID resourceId = rec.getEntityId();
        String region = null;
        if ("INSTITUTION".equalsIgnoreCase(rec.getEntityType())) {
            region = institutionRepository.findById(rec.getEntityId()).map(Institution::getRegion).orElse(null);
        } else if ("PROVIDER".equalsIgnoreCase(rec.getEntityType())) {
            UUID resolved = membershipRepository.findByUserIdAndIsActiveTrue(rec.getEntityId()).stream()
                    .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                    .map(InstitutionMembership::getInstitutionId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(rec.getEntityId());
            resourceId = resolved;
            if (!resolved.equals(rec.getEntityId())) {
                region = institutionRepository.findById(resolved).map(Institution::getRegion).orElse(null);
            }
        }
        final UUID scopeResourceId = resourceId;
        final String scopeRegion = region;
        boolean platformAdmin = false;
        if (actorId != null) {
            platformAdmin = userRepository.findById(actorId)
                    .map(u -> u.getRole() != null && PLATFORM_ADMIN_ROLES.contains(u.getRole().name()))
                    .orElse(false);
        }
        AdminDelegation usedDelegation = null;
        if (!platformAdmin) {
            if (actorId == null) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Authentication required to review verifications");
            }
            usedDelegation = findEffectiveDelegations(actorId, "PROVIDER_VERIFICATION").stream()
                    .filter(d -> {
                        Set<String> tokens = parsePermissionTokens(d.getPermissions());
                        return tokens.contains("MANAGE") || tokens.contains(requiredPermission);
                    })
                    .filter(d -> scopeCovers(d, scopeResourceId, scopeRegion) && resourcesCover(d, scopeResourceId))
                    .findFirst()
                    .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                            "Access denied: no active PROVIDER_VERIFICATION delegation covers this entity"));
        }
        VerificationSummaryResponse result = reviewVerification(verificationId, actorId, action, notes);
        if (usedDelegation != null) {
            writeAudit(PLATFORM_INSTITUTION_ID, "DELEGATION", usedDelegation.getId(),
                    "PROVIDER_VERIFICATION delegation used", "UPDATE",
                    Map.of("verificationId", verificationId.toString()),
                    Map.of("verificationId", verificationId.toString(),
                            "decision", action,
                            "delegateId", String.valueOf(actorId)));
            log.info("Delegated verification review: delegate={} verification={} decision={}",
                    actorId, verificationId, action);
        }
        return result;
    }

    /**
     * Tasks awaiting the given officer under their ACTIVE PROVIDER_VERIFICATION
     * delegations. Same scope/permission rules as the enforced review path, so the
     * UI can only ever offer what the backend will authorize.
     */
    @Transactional(readOnly = true)
    public List<DelegatedTaskResponse> listDelegatedTasks(UUID actorId) {
        if (actorId == null) {
            throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        }
        List<DelegatedTaskResponse> tasks = new ArrayList<>();
        List<AdminDelegation> delegations = findEffectiveDelegations(actorId, "PROVIDER_VERIFICATION");
        List<VerificationRecord> pending = verificationRepository.findByStatusAndIsDeletedFalse("PENDING");
        for (AdminDelegation d : delegations) {
            Set<String> tokens = parsePermissionTokens(d.getPermissions());
            boolean canDecide = tokens.contains("MANAGE") || tokens.contains("VERIFY")
                    || tokens.contains("REVIEW") || tokens.contains("APPROVE") || tokens.contains("REJECT");
            if (!canDecide) continue;
            for (VerificationRecord rec : pending) {
                if (!"INSTITUTION".equalsIgnoreCase(rec.getEntityType())
                        && !"PROVIDER".equalsIgnoreCase(rec.getEntityType())) {
                    continue;
                }
                UUID resourceId = rec.getEntityId();
                String region = null;
                if ("INSTITUTION".equalsIgnoreCase(rec.getEntityType())) {
                    region = institutionRepository.findById(rec.getEntityId()).map(Institution::getRegion).orElse(null);
                } else {
                    UUID resolved = membershipRepository.findByUserIdAndIsActiveTrue(rec.getEntityId()).stream()
                            .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                            .map(InstitutionMembership::getInstitutionId)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(rec.getEntityId());
                    resourceId = resolved;
                    if (!resolved.equals(rec.getEntityId())) {
                        region = institutionRepository.findById(resolved).map(Institution::getRegion).orElse(null);
                    }
                }
                if (!scopeCovers(d, resourceId, region) || !resourcesCover(d, resourceId)) continue;
                String entityName = null;
                if ("INSTITUTION".equalsIgnoreCase(rec.getEntityType())) {
                    entityName = institutionRepository.findById(rec.getEntityId()).map(Institution::getName).orElse(null);
                } else {
                    entityName = userRepository.findById(rec.getEntityId()).map(User::getFullName).orElse(null);
                }
                tasks.add(DelegatedTaskResponse.builder()
                        .verificationId(rec.getId()).verificationType(rec.getVerificationType())
                        .entityType(rec.getEntityType()).entityId(rec.getEntityId()).entityName(entityName)
                        .status(rec.getStatus()).submittedAt(rec.getSubmittedAt())
                        .delegationId(d.getId()).authority(d.getAuthority()).scope(d.getScope())
                        .build());
            }
        }
        return tasks;
    }

    // ── Verifications ──

    private static final Set<String> VERIFIABLE_ENTITY_TYPES = Set.of("INSTITUTION", "PROVIDER", "SERVICE");

    public VerificationSummaryResponse submitVerification(VerificationSubmitRequest req) {
        String entityType = req.getEntityType() != null ? req.getEntityType().trim().toUpperCase() : null;
        if (entityType == null || !VERIFIABLE_ENTITY_TYPES.contains(entityType)) {
            throw new IllegalArgumentException("entityType must be one of " + VERIFIABLE_ENTITY_TYPES);
        }
        if (req.getEntityId() == null) {
            throw new IllegalArgumentException("entityId is required");
        }
        if (req.getVerificationType() == null || req.getVerificationType().isBlank()) {
            throw new IllegalArgumentException("verificationType is required");
        }
        switch (entityType) {
            case "INSTITUTION" -> institutionRepository.findByIdAndIsDeletedFalse(req.getEntityId())
                    .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", req.getEntityId()));
            case "PROVIDER" -> userRepository.findByIdAndIsDeletedFalse(req.getEntityId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", req.getEntityId()));
            case "SERVICE" -> platformServiceRepository.findById(req.getEntityId())
                    .orElseThrow(() -> new ResourceNotFoundException("PlatformService", "id", req.getEntityId()));
            default -> throw new IllegalArgumentException("Unsupported entityType: " + entityType);
        }
        // Only an undecided PENDING request blocks resubmission. A CHANGES_REQUIRED
        // decision closes that round; the provider may submit fresh evidence, which
        // creates a new PENDING record while history is preserved for audit.
        boolean pendingExists = verificationRepository
                .findByEntityTypeAndEntityIdAndIsDeletedFalse(entityType, req.getEntityId()).stream()
                .anyMatch(v -> "PENDING".equalsIgnoreCase(v.getStatus()));
        if (pendingExists) {
            throw new IllegalStateException("A pending verification request already exists for this entity");
        }
        VerificationRecord rec = VerificationRecord.builder()
                .entityType(entityType).entityId(req.getEntityId())
                .verificationType(req.getVerificationType().trim().toUpperCase())
                .status("PENDING").submittedBy(req.getSubmittedBy())
                .submittedAt(LocalDateTime.now())
                .documents(req.getDocuments() != null && !req.getDocuments().isBlank() ? req.getDocuments() : null)
                .notes(req.getNotes())
                .build();
        if ("INSTITUTION".equals(entityType)) {
            rec.setInstitutionId(req.getEntityId());
        } else if ("PROVIDER".equals(entityType)) {
            UUID providerInstitution = membershipRepository.findByUserIdAndIsActiveTrue(req.getEntityId()).stream()
                    .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                    .map(InstitutionMembership::getInstitutionId)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            rec.setInstitutionId(providerInstitution != null ? providerInstitution : PLATFORM_INSTITUTION_ID);
        } else {
            rec.setInstitutionId(PLATFORM_INSTITUTION_ID);
        }
        verificationRepository.save(rec);
        writeAudit(rec.getInstitutionId() != null ? rec.getInstitutionId() : PLATFORM_INSTITUTION_ID,
                "VerificationRecord", rec.getId(), entityType + " verification submitted", "CREATE",
                Map.of(), Map.of("entityType", entityType, "entityId", req.getEntityId().toString(),
                        "verificationType", rec.getVerificationType()));
        log.info("Verification submitted: {} {} {}", rec.getId(), entityType, req.getEntityId());
        return VerificationSummaryResponse.builder()
                .id(rec.getId()).entityType(rec.getEntityType()).entityId(rec.getEntityId())
                .verificationType(rec.getVerificationType()).status(rec.getStatus())
                .submittedBy(rec.getSubmittedBy()).submittedAt(rec.getSubmittedAt())
                .createdAt(rec.getCreatedAt()).build();
    }

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
                        inst.setApprovedAt(LocalDateTime.now());
                        if (rec.getReviewedBy() != null) {
                            inst.setApprovedBy(rec.getReviewedBy().toString());
                        }
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

    @Transactional
    public EventResponse createEvent(EventRequest request) {
        UUID platformAdminUserId = UUID.fromString("b0000000-0000-0000-0000-000000000099");
        return eventService.createEvent(PLATFORM_INSTITUTION_ID, platformAdminUserId, request);
    }

    @Transactional
    public EventResponse updateEvent(UUID id, EventRequest request) {
        return eventService.updateEvent(id, request);
    }

    @Transactional
    public void deleteEvent(UUID id) {
        eventService.deleteEvent(id, true);
    }

    @Transactional
    public EventResponse publishEvent(UUID id) {
        return eventService.publishEvent(id, PLATFORM_INSTITUTION_ID);
    }

    @Transactional
    public EventResponse cancelEvent(UUID id, String reason) {
        return eventService.cancelEvent(id, PLATFORM_INSTITUTION_ID, reason);
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
            "OPEN", Set.of("ASSIGNED", "INVESTIGATING", "ACTION_REQUIRED", "RESOLVED", "CLOSED"),
            "ASSIGNED", Set.of("INVESTIGATING", "ACTION_REQUIRED", "RESOLVED", "CLOSED", "OPEN"),
            "INVESTIGATING", Set.of("ACTION_REQUIRED", "RESOLVED", "CLOSED"),
            "ACTION_REQUIRED", Set.of("INVESTIGATING", "RESOLVED", "CLOSED"),
            "RESOLVED", Set.of("CLOSED", "INVESTIGATING", "ACTION_REQUIRED"),
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

    private static final Set<String> REPORT_ACTIONS = Set.of("REVIEWING", "RESOLVED", "DISMISSED", "APPEALED");

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

    // ── BATCH 13: Admins, Role Permissions, Offboarding, Bulk, Features, Delivery, Snapshots ──

    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN", "INSTITUTION_ADMIN", "NATIONAL_ADMIN",
            "REGIONAL_ADMIN", "DISTRICT_ADMIN", "PROVIDER_ADMIN");

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> listAdmins() {
        List<AdminAccountResponse> out = new ArrayList<>();
        for (String roleName : ADMIN_ROLES) {
            User.Role role;
            try { role = User.Role.valueOf(roleName); } catch (IllegalArgumentException e) { continue; }
            userRepository.findByRoleAndIsDeletedFalse(role, PageRequest.of(0, 500)).forEach(u -> {
                List<String> perms = List.of();
                try {
                    perms = adminUserPermissionRepository.findPermissionsByUserId(u.getId());
                } catch (Exception e) { log.debug("No permissions for user {}: {}", u.getId(), e.getMessage()); }
                Long recent = null;
                try {
                    recent = auditLogRepository.findByUserId(u.getId(), PageRequest.of(0, 1)).getTotalElements();
                } catch (Exception e) { log.debug("Audit count failed: {}", e.getMessage()); }
                out.add(AdminAccountResponse.builder()
                        .userId(u.getId()).email(u.getEmail()).fullName(u.getFullName())
                        .role(roleName).assignedRoleName(roleName)
                        .permissions(perms)
                        .scope(u.getInstitutionId() != null ? u.getInstitutionId().toString() : "PLATFORM")
                        .institutionId(u.getInstitutionId())
                        .isActive(u.getIsActive()).createdAt(u.getCreatedAt())
                        .createdBy(u.getCreatedBy()).lastModifiedAt(u.getUpdatedAt())
                        .recentActionCount(recent)
                        .build());
            });
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<String> getRolePermissions(UUID roleId) {
        return rolePermissionRepository.findPermissionsByRoleId(roleId);
    }

    /**
     * Per-account permission matrix (audit B-10). Keyed strictly by {@code users.id} so it can
     * never collide with {@code custom_roles.id} the way the shared role_permissions column could.
     */
    @Transactional(readOnly = true)
    public List<String> getAdminUserPermissions(UUID userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return adminUserPermissionRepository.findPermissionsByUserId(user.getId());
    }

    @Transactional
    public List<String> updateAdminUserPermissions(UUID userId, RolePermissionUpdateRequest req, String actor) {
        if (req == null || req.getPermissions() == null) {
            throw new IllegalArgumentException("permissions list is required");
        }
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        List<String> old = adminUserPermissionRepository.findPermissionsByUserId(userId);
        adminUserPermissionRepository.deleteByUserIdAndIsDeletedFalse(userId);
        adminUserPermissionRepository.flush();
        for (String p : req.getPermissions()) {
            if (p != null && !p.isBlank()) {
                adminUserPermissionRepository.save(
                        tz.elmkusoma.administration.domain.AdminUserPermission.of(userId, p.trim(), actor));
            }
        }
        adminUserPermissionRepository.flush();

        writeAudit(user.getInstitutionId() != null ? user.getInstitutionId() : PLATFORM_INSTITUTION_ID,
                "ADMIN_PERMISSION", userId, user.getEmail(), "UPDATE",
                Map.of("permissions", String.join(",", old)),
                Map.of("permissions", String.join(",", req.getPermissions())));
        log.info("Admin permissions updated for {}: {} -> {} permissions", user.getEmail(), old.size(),
                req.getPermissions().size());
        return adminUserPermissionRepository.findPermissionsByUserId(userId);
    }

    public List<String> updateRolePermissions(UUID roleId, RolePermissionUpdateRequest req) {
        if (req == null || req.getPermissions() == null) {
            throw new IllegalArgumentException("permissions list is required");
        }
        List<String> old = rolePermissionRepository.findPermissionsByRoleId(roleId);
        rolePermissionRepository.deleteByRoleId(roleId);
        for (String p : req.getPermissions()) {
            if (p != null && !p.isBlank()) {
                rolePermissionRepository.save(RolePermission.of(roleId, p.trim()));
            }
        }
        writeAudit(PLATFORM_INSTITUTION_ID, "ROLE_PERMISSION", roleId, "Role permissions", "UPDATE",
                Map.of("permissions", String.join(",", old)),
                Map.of("permissions", String.join(",", req.getPermissions())));
        log.info("Role permissions updated for {}: {} → {} permissions", roleId, old.size(), req.getPermissions().size());
        return rolePermissionRepository.findPermissionsByRoleId(roleId);
    }

    public SupportTicketResponse assignSupportTicket(UUID ticketId, UUID assigneeId) {
        tz.elmkusoma.parent.domain.SupportTicket t = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("SupportTicket", "id", ticketId));
        if (assigneeId != null) {
            User assignee = userRepository.findByIdAndIsDeletedFalse(assigneeId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", assigneeId));
            if (!ADMIN_ROLES.contains(assignee.getRole().name()) && !User.Role.ADMIN.equals(assignee.getRole())) {
                throw new IllegalStateException("Assignee must be an admin-role user");
            }
        }
        UUID oldAssignee = t.getAssignedTo();
        t.setAssignedTo(assigneeId);
        if (assigneeId != null && "OPEN".equals(t.getStatus())) {
            t.setStatus("ASSIGNED");
        }
        supportTicketRepository.save(t);
        writeAudit(t.getInstitutionId(), "SUPPORT_TICKET", ticketId, t.getSubject(), "UPDATE",
                Map.of("assignedTo", String.valueOf(oldAssignee)),
                Map.of("assignedTo", String.valueOf(assigneeId), "status", t.getStatus()));
        log.info("Support ticket {} assigned to {}", ticketId, assigneeId);
        return SupportTicketResponse.builder()
                .id(t.getId()).userId(t.getUserId()).title(t.getSubject()).description(t.getDescription())
                .category(t.getCategory()).priority(t.getPriority()).status(t.getStatus())
                .assignedTo(t.getAssignedTo()).resolvedAt(t.getResolvedAt()).createdAt(t.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public OffboardingChecklistResponse offboardingChecklist(UUID institutionId) {
        Institution inst = institutionRepository.findByIdAndIsDeletedFalse(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", institutionId));
        List<OffboardingChecklistResponse.OffboardingStep> steps = new ArrayList<>();
        long users = userRepository.findAllByInstitutionId(institutionId).size();
        steps.add(step("Export user directory", users > 0 ? "PENDING" : "DONE",
                users + " member(s) discoverable for export"));
        long courses = 0;
        try { courses = courseRepository.countByInstitutionIdAndIsDeletedFalse(institutionId); } catch (Exception e) { }
        steps.add(step("Export learning content", "DONE", courses + " course(s)"));
        long activeEnts = 0;
        try {
            activeEnts = providerEntitlementRepository.findByProviderIdAndIsDeletedFalse(institutionId).stream()
                    .filter(e -> "ACTIVE".equals(e.getStatus())).count();
        } catch (Exception e) { }
        steps.add(step("Revoke service entitlements", activeEnts == 0 ? "DONE" : "PENDING",
                activeEnts + " active entitlement(s) still open"));
        String lifecycle = inst.getStatus() != null ? inst.getStatus() : "ACTIVE";
        steps.add(step("Lifecycle transition", "ARCHIVED".equals(lifecycle) ? "DONE" : "PENDING",
                "Current status: " + lifecycle + " (target ARCHIVED)"));
        steps.add(step("Notify stakeholders", "PENDING",
                "Send offboarding notice via platform notifications"));
        long auditRecords = countInstitutionAudit(institutionId);
        steps.add(step("Audit trail review", "DONE",
                auditRecords + " audit record(s) retained per data retention policy"));
        return OffboardingChecklistResponse.builder()
                .institutionId(institutionId).institutionName(inst.getName())
                .lifecycleStatus(lifecycle).steps(steps).build();
    }

    private OffboardingChecklistResponse.OffboardingStep step(String name, String status, String detail) {
        return OffboardingChecklistResponse.OffboardingStep.builder()
                .step(name).status(status).detail(detail).build();
    }

    @Transactional(readOnly = true)
    public long countInstitutionAudit(UUID institutionId) {
        try { return auditLogRepository.countByInstitutionId(institutionId); }
        catch (Exception e) { return 0; }
    }

    public Map<String, Object> bulkContentAction(BulkContentActionRequest req) {
        if (req == null || req.getType() == null || req.getAction() == null || req.getIds() == null || req.getIds().isEmpty()) {
            throw new IllegalArgumentException("type, action and non-empty ids are required");
        }
        String type = req.getType().toUpperCase();
        String action = req.getAction().toUpperCase();
        Set<String> allowed = Set.of("PUBLISH", "UNPUBLISH", "ARCHIVE", "RESTORE");
        if (!allowed.contains(action)) {
            throw new IllegalArgumentException("action must be one of " + allowed);
        }
        int affected = 0;
        List<String> failures = new ArrayList<>();
        for (UUID id : req.getIds()) {
            try {
                switch (type) {
                    case "COURSE" -> {
                        var c = courseRepository.findById(id).orElse(null);
                        if (c == null || Boolean.TRUE.equals(c.getIsDeleted())) { failures.add(id + ": not found"); break; }
                        switch (action) {
                            case "PUBLISH" -> c.setIsPublished(true);
                            case "UNPUBLISH" -> c.setIsPublished(false);
                            case "ARCHIVE" -> { c.setIsPublished(false); c.setIsDeleted(true); }
                            case "RESTORE" -> { c.setIsDeleted(false); }
                            default -> { }
                        }
                        courseRepository.save(c);
                        affected++;
                    }
                    case "EVENT" -> {
                        var e = eventRepository.findById(id).orElse(null);
                        if (e == null || Boolean.TRUE.equals(e.getIsDeleted())) { failures.add(id + ": not found"); break; }
                        switch (action) {
                            case "PUBLISH" -> e.setStatus("PUBLISHED");
                            case "UNPUBLISH" -> e.setStatus("DRAFT");
                            case "ARCHIVE" -> { e.setStatus("CANCELLED"); e.setIsDeleted(true); }
                            case "RESTORE" -> { e.setIsDeleted(false); e.setStatus("DRAFT"); }
                            default -> { }
                        }
                        eventRepository.save(e);
                        affected++;
                    }
                    case "RESOURCE" -> {
                        var r = resourceRepository.findById(id).orElse(null);
                        if (r == null || Boolean.TRUE.equals(r.getIsDeleted())) { failures.add(id + ": not found"); break; }
                        switch (action) {
                            case "PUBLISH" -> { }
                            case "UNPUBLISH" -> { }
                            case "ARCHIVE" -> r.setIsDeleted(true);
                            case "RESTORE" -> r.setIsDeleted(false);
                            default -> { }
                        }
                        resourceRepository.save(r);
                        affected++;
                    }
                    case "MEDIA" -> {
                        var m = mediaAssetRepository.findById(id).orElse(null);
                        if (m == null || Boolean.TRUE.equals(m.getIsDeleted())) { failures.add(id + ": not found"); break; }
                        switch (action) {
                            case "PUBLISH" -> { }
                            case "UNPUBLISH" -> { }
                            case "ARCHIVE" -> m.setIsDeleted(true);
                            case "RESTORE" -> m.setIsDeleted(false);
                            default -> { }
                        }
                        mediaAssetRepository.save(m);
                        affected++;
                    }
                    default -> throw new IllegalArgumentException("type must be COURSE, EVENT, RESOURCE or MEDIA");
                }
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (Exception ex) {
                failures.add(id + ": " + ex.getMessage());
            }
        }
        writeAudit(PLATFORM_INSTITUTION_ID, "BULK_CONTENT", PLATFORM_INSTITUTION_ID, "Bulk " + type, "UPDATE",
                Map.of(), Map.of("type", type, "action", action,
                        "affected", String.valueOf(affected), "requested", String.valueOf(req.getIds().size())));
        log.info("Bulk {} on {} {}s: {} affected, {} failed", action, affected, type, affected, failures.size());
        Map<String, Object> result = new HashMap<>();
        result.put("affected", affected);
        result.put("requested", req.getIds().size());
        result.put("failures", failures);
        return result;
    }

    private static final Set<String> FEATURE_STATUSES = Set.of("PLANNED", "DEVELOPMENT", "TESTING",
            "ROLLOUT", "ACTIVE", "DEPRECATED", "RETIRED");
    private static final Map<String, Set<String>> FEATURE_TRANSITIONS = Map.of(
            "PLANNED", Set.of("DEVELOPMENT", "RETIRED"),
            "DEVELOPMENT", Set.of("TESTING", "PLANNED"),
            "TESTING", Set.of("ROLLOUT", "DEVELOPMENT"),
            "ROLLOUT", Set.of("ACTIVE", "TESTING"),
            "ACTIVE", Set.of("DEPRECATED"),
            "DEPRECATED", Set.of("ACTIVE", "RETIRED"),
            "RETIRED", Set.of()
    );

    @Transactional(readOnly = true)
    public List<FeatureStatusResponse> listFeatures() {
        return featureRepository.findByIsDeletedFalse().stream()
                .map(f -> FeatureStatusResponse.builder()
                        .key(f.getFeatureKey()).name(f.getName()).status(f.getStatus())
                        .description(f.getDescription()).updatedAt(f.getUpdatedAt())
                        .build())
                .toList();
    }

    public FeatureStatusResponse updateFeatureStatus(String key, String status) {
        if (status == null || !FEATURE_STATUSES.contains(status.toUpperCase())) {
            throw new IllegalArgumentException("status must be one of " + FEATURE_STATUSES);
        }
        String target = status.toUpperCase();
        PlatformFeature f = featureRepository.findByFeatureKeyAndIsDeletedFalse(key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature", "key", key));
        String current = f.getStatus();
        if (!current.equals(target)) {
            if (!FEATURE_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
                throw new IllegalStateException("Invalid feature transition: " + current + " → " + target);
            }
            f.setStatus(target);
            featureRepository.save(f);
            writeAudit(PLATFORM_INSTITUTION_ID, "FEATURE", f.getId(), f.getName(), "UPDATE",
                    Map.of("status", current), Map.of("status", target));
            log.info("Feature {} status: {} → {}", key, current, target);
        }
        return FeatureStatusResponse.builder()
                .key(f.getFeatureKey()).name(f.getName()).status(f.getStatus())
                .description(f.getDescription()).updatedAt(f.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public CommunicationDeliveryResponse communicationDelivery() {
        Long platformTotal = null;
        Long platformReadSum = null;
        Long learnerTotal = null;
        Long learnerRead = null;
        try {
            platformTotal = notificationRepository.countByIsDeletedFalse();
            platformReadSum = notificationRepository.findAll().stream()
                    .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                    .mapToLong(n -> n.getReadCount() != null ? n.getReadCount() : 0).sum();
        } catch (Exception e) { log.warn("Platform notification stats unavailable: {}", e.getMessage()); }
        try {
            learnerTotal = learnerNotificationRepository.count();
            learnerRead = learnerNotificationRepository.findAll().stream()
                    .filter(n -> !Boolean.TRUE.equals(n.getIsDeleted()))
                    .filter(n -> Boolean.TRUE.equals(n.getIsRead())).count();
        } catch (Exception e) { log.warn("Learner notification stats unavailable: {}", e.getMessage()); }
        Double rate = null;
        if (learnerTotal != null && learnerTotal > 0 && learnerRead != null) {
            rate = Math.round((learnerRead * 1000.0) / learnerTotal) / 10.0;
        }
        return CommunicationDeliveryResponse.builder()
                .platformNotifications(platformTotal)
                .learnerNotifications(learnerTotal)
                .learnerRead(learnerRead)
                .learnerUnread(learnerTotal != null && learnerRead != null ? learnerTotal - learnerRead : null)
                .learnerReadRate(rate)
                .platformReadCountSum(platformReadSum)
                .deliveryNote(platformTotal == null && learnerTotal == null
                        ? "Data unavailable" : "Counts from live notification tables")
                .build();
    }

    @Transactional(readOnly = true)
    public List<AnalyticsSnapshotResponse> analyticsSnapshots(int limit) {
        return snapshotRepository.findAll(PageRequest.of(0, Math.max(1, Math.min(limit, 100)),
                        Sort.by(Sort.Direction.DESC, "generatedAt"))).stream()
                .map(s -> AnalyticsSnapshotResponse.builder()
                        .id(s.getId()).snapshotType(s.getSnapshotType())
                        .generatedAt(s.getGeneratedAt()).data(s.getSnapshotData())
                        .build())
                .toList();
    }

    public AnalyticsSnapshotResponse createAnalyticsSnapshot() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", userRepository.countByIsDeletedFalse());
        data.put("totalInstitutions", institutionRepository.countByIsDeletedFalse());
        data.put("totalCourses", courseRepository.countByIsDeletedFalse());
        data.put("totalPayments", paymentRepository.countByIsDeletedFalse());
        data.put("totalCertificates", certificateRepository.countByIsDeletedFalse());
        data.put("activeLiveClasses", liveClassRepository.countByStatusInAndIsDeletedFalse(List.of("IN_PROGRESS", "LIVE", "STARTING")));
        DashboardSnapshot s = DashboardSnapshot.of(PLATFORM_INSTITUTION_ID, "PLATFORM_ANALYTICS",
                data, LocalDateTime.now().plusDays(90));
        s.setGeneratedAt(LocalDateTime.now());
        snapshotRepository.save(s);
        writeAudit(PLATFORM_INSTITUTION_ID, "ANALYTICS_SNAPSHOT", s.getId(), "PLATFORM_ANALYTICS", "CREATE",
                Map.of(), Map.of("keys", String.valueOf(data.keySet())));
        return AnalyticsSnapshotResponse.builder()
                .id(s.getId()).snapshotType(s.getSnapshotType())
                .generatedAt(s.getGeneratedAt()).data(s.getSnapshotData())
                .build();
    }

    public Map<String, Object> revokeCertificatePlatform(UUID certificateId, String reason, String actor) {
        Certificate cert = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate", "id", certificateId));
        if (Boolean.TRUE.equals(cert.getIsDeleted())) {
            throw new ResourceNotFoundException("Certificate", "id", certificateId);
        }
        if (cert.getStatus() == Certificate.CertificateStatus.REVOKED) {
            throw new IllegalStateException("Certificate is already revoked");
        }
        String old = cert.getStatus() != null ? cert.getStatus().name() : "ISSUED";
        cert.setStatus(Certificate.CertificateStatus.REVOKED);
        cert.setRevokedReason(reason != null && !reason.isBlank() ? reason.trim() : null);
        cert.setRevokedAt(LocalDateTime.now());
        certificateRepository.save(cert);
        writeAudit(cert.getInstitutionId(), "CERTIFICATE", certificateId, cert.getSerialNumber(), "UPDATE",
                Map.of("status", old), Map.of("status", "REVOKED", "reason", reason != null ? reason : "",
                        "by", actor != null ? actor : "platform-admin"));
        log.info("Certificate {} revoked by platform admin {}", certificateId, actor);
        Map<String, Object> out = new HashMap<>();
        out.put("id", certificateId);
        out.put("status", "REVOKED");
        out.put("serialNumber", cert.getSerialNumber());
        out.put("reason", reason);
        return out;
    }

    @Transactional(readOnly = true)
    public BackupStatusResponse backupStatus() {
        return backupStatusService.getBackupStatus();
    }

    // ── Organization Members & Roles ──

    public List<OrgMemberResponse> listInstitutionMembers(UUID institutionId) {
        if (!institutionRepository.existsById(institutionId)) {
            throw new ResourceNotFoundException("Institution", "id", institutionId);
        }
        return membershipRepository.findByInstitutionIdAndIsActiveTrue(institutionId).stream()
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .map(this::toOrgMember)
                .toList();
    }

    public OrgMemberResponse updateInstitutionMemberRole(UUID institutionId, UUID userId, String newRole) {
        InstitutionMembership.Role role;
        try {
            role = InstitutionMembership.Role.valueOf(newRole.trim().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid role: " + newRole);
        }
        InstitutionMembership membership = membershipRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .filter(m -> institutionId.equals(m.getInstitutionId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Institution membership", "userId", userId));

        String oldRole = membership.getRole() != null ? membership.getRole().name() : null;
        membership.setRole(role);
        membershipRepository.save(membership);

        try {
            permissionCacheService.invalidateUserPermissions(userId, institutionId);
            permissionCacheService.invalidateUserRole(userId, institutionId);
            permissionCacheService.invalidateMembership(userId, institutionId);
        } catch (Exception cacheEx) {
            log.warn("Permission cache invalidation skipped (cache unavailable): {}", cacheEx.getMessage());
        }

        Map<String, Object> oldVals = new HashMap<>();
        oldVals.put("membershipRole", oldRole != null ? oldRole : "");
        Map<String, Object> newVals = new HashMap<>();
        newVals.put("membershipRole", role.name());
        writeAudit(institutionId, "USER", userId, "Organization member role", "UPDATE", oldVals, newVals);
        log.info("Organization member role changed: user {} in institution {} : {} -> {}",
                userId, institutionId, oldRole, role.name());

        return toOrgMember(membership);
    }

    private OrgMemberResponse toOrgMember(InstitutionMembership m) {
        User u = userRepository.findById(m.getUserId()).orElse(null);
        return OrgMemberResponse.builder()
                .userId(m.getUserId())
                .fullName(u != null ? u.getFullName() : null)
                .email(u != null ? u.getEmail() : null)
                .phone(u != null ? u.getPhone() : null)
                .userRole(u != null && u.getRole() != null ? u.getRole().name() : null)
                .membershipRole(m.getRole() != null ? m.getRole().name() : null)
                .isActive(m.getIsActive())
                .userActive(u != null ? u.getIsActive() : null)
                .joinedAt(m.getCreatedAt())
                .build();
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
                // Audit B-02: this was hard-coded to null, which hid the "account provisioned with no
                // organization" failure from the admin UI that creates the account.
                .institutionId(user.getInstitutionId())
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
                .regionId(inst.getRegionId())
                .districtId(inst.getDistrictId())
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

    @Transactional
    public AdminAccountResponse createAdmin(CreateAdminRequest request) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new IllegalArgumentException("User with email " + request.getEmail() + " already exists");
        }
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .institutionId(request.getInstitutionId())
                .isActive(true)
                .isEmailVerified(false)
                .build();
        user = userRepository.save(user);
        writeAudit(PLATFORM_INSTITUTION_ID, "ADMIN", user.getId(), user.getEmail(), "CREATE",
                Map.of(), auditValues("email", user.getEmail(), "role", user.getRole().name(), "institutionId", request.getInstitutionId()));
        log.info("Admin created: {}", user.getEmail());
        return toAdminAccount(user);
    }

    @Transactional
    public AdminAccountResponse updateAdmin(UUID userId, UpdateAdminRequest request) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        String oldRole = user.getRole() != null ? user.getRole().name() : null;
        UUID oldInstitutionId = user.getInstitutionId();

        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getIsActive() != null) {
            if (!request.getIsActive() && User.Role.ADMIN.equals(user.getRole())) {
                long activeAdmins = userRepository.countByRoleAndIsDeletedFalse(User.Role.ADMIN);
                long activeCount = userRepository.findByRoleAndIsDeletedFalse(User.Role.ADMIN, PageRequest.of(0, 1000)).getContent().stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
                if (activeCount <= 1) {
                    throw new IllegalStateException("Cannot suspend the last active Platform Admin");
                }
            }
            user.setIsActive(request.getIsActive());
        }
        if (request.getInstitutionId() != null) {
            user.setInstitutionId(request.getInstitutionId());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        userRepository.save(user);
        writeAudit(PLATFORM_INSTITUTION_ID, "ADMIN", userId, user.getEmail(), "UPDATE",
                auditValues("role", oldRole, "institutionId", oldInstitutionId),
                auditValues("role", user.getRole() != null ? user.getRole().name() : null, "institutionId", user.getInstitutionId()));
        log.info("Admin updated: {}", user.getEmail());
        return toAdminAccount(user);
    }

    @Transactional
    public void deleteAdmin(UUID userId) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        if (User.Role.ADMIN.equals(user.getRole())) {
            long activeAdmins = userRepository.countByRoleAndIsDeletedFalse(User.Role.ADMIN);
            long activeCount = userRepository.findByRoleAndIsDeletedFalse(User.Role.ADMIN, PageRequest.of(0, 1000)).getContent().stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
            if (activeCount <= 1) {
                throw new IllegalStateException("Cannot delete the last active Platform Admin");
            }
        }
        user.setIsDeleted(true);
        userRepository.save(user);
        writeAudit(PLATFORM_INSTITUTION_ID, "ADMIN", userId, user.getEmail(), "DELETE", Map.of(), Map.of());
        log.info("Admin deleted: {}", user.getEmail());
    }

    @Transactional
    public AdminAccountResponse updateAdminRole(UUID userId, String newRole, String actorEmail) {
        User user = userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        String oldRole = user.getRole() != null ? user.getRole().name() : null;
        try {
            User.Role role = User.Role.valueOf(newRole.toUpperCase());
            user.setRole(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + newRole);
        }
        userRepository.save(user);
        writeAudit(PLATFORM_INSTITUTION_ID, "ADMIN", userId, user.getEmail(), "ROLE_CHANGE",
                auditValues("role", oldRole), auditValues("role", newRole.toUpperCase()));
        log.info("Admin role updated: {} -> {}", user.getEmail(), newRole);
        return toAdminAccount(user);
    }

    private AdminAccountResponse toAdminAccount(User user) {
        List<String> permissions = rolePermissionRepository.findPermissionsByRoleId(user.getId());
        String scope = user.getInstitutionId() != null ? user.getInstitutionId().toString() : "PLATFORM";
        return AdminAccountResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .assignedRoleName(user.getRole() != null ? user.getRole().name() : null)
                .permissions(permissions)
                .scope(scope)
                .institutionId(user.getInstitutionId())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private IncidentSummaryResponse toIncidentSummary(PlatformIncident i) {
        return IncidentSummaryResponse.builder()
                .id(i.getId()).title(i.getTitle()).description(i.getDescription()).category(i.getCategory())
                .severity(i.getSeverity()).status(i.getStatus()).affectedService(i.getAffectedService())
                .assignedTo(i.getAssignedTo()).detectedAt(i.getDetectedAt()).resolvedAt(i.getResolvedAt())
                .createdAt(i.getCreatedAt()).build();
    }

}
