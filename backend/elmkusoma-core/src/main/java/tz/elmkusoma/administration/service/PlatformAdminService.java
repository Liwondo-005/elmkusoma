package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.dto.*;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PlatformAdminService {

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final LiveClassRepository liveClassRepository;
    private final CertificateRepository certificateRepository;
    private final SecurityEventRepository securityEventRepository;
    private final AuditLogRepository auditLogRepository;

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
        long unresolvedSecurityEvents = securityEventRepository.countByResolvedAndIsDeletedFalse(false);

        return PlatformDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .totalParents(totalParents)
                .totalInstitutions(totalInstitutions)
                .totalLiveClasses(totalLiveClasses)
                .activeLiveClasses(activeLiveClasses)
                .totalPayments(0L)
                .totalCertificates(totalCertificates)
                .unresolvedSecurityEvents(unresolvedSecurityEvents)
                .build();
    }

    @Transactional(readOnly = true)
    public List<AttentionItemResponse> getAttentionItems() {
        List<AttentionItemResponse> items = new ArrayList<>();

        long unresolvedSecurity = securityEventRepository.countByResolvedAndIsDeletedFalse(false);
        if (unresolvedSecurity > 0) {
            items.add(AttentionItemResponse.builder()
                    .severity("HIGH")
                    .title("Unresolved Security Events")
                    .description(unresolvedSecurity + " security event(s) require attention")
                    .category("SECURITY")
                    .actionUrl("/dashboard/platform-admin/security")
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
        long totalUsers = userRepository.countByIsDeletedFalse();
        long totalInstitutions = institutionRepository.countByIsDeletedFalse();

        return PlatformHealthResponse.builder()
                .databaseStatus("Operational")
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
        user.setIsActive(active);
        userRepository.save(user);
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
                .totalStudents(0)
                .totalTeachers(0)
                .createdAt(inst.getCreatedAt())
                .build();
    }

    public InstitutionSummaryResponse updateInstitutionStatus(UUID institutionId, boolean active) {
        Institution inst = institutionRepository.findByIdAndIsDeletedFalse(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", institutionId));
        inst.setIsActive(active);
        institutionRepository.save(inst);
        log.info("Institution {} {} by platform admin", institutionId, active ? "activated" : "suspended");
        return toInstitutionSummary(inst);
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

    // ── Payments (stub) ──

    @Transactional(readOnly = true)
    public PageResponse<PaymentSummaryResponse> listPayments(int page, int size, String status, UUID institutionId) {
        return new PageResponse<>(Collections.emptyList(), 0, size, 0, 0, true, true);
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
        return securityEventRepository.findByResolvedFalseAndIsDeletedFalse().stream()
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
        SecurityEvent event = securityEventRepository.findByIdAndIsDeletedFalse(eventId)
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
        Page<AuditLog> logs = auditLogRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

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

        if (type == null || type.equalsIgnoreCase("user") || type.equalsIgnoreCase("all")) {
            Page<User> users = userRepository.findBySearchTermAndIsDeletedFalse(query, PageRequest.of(0, limit));
            results.addAll(users.getContent().stream()
                    .map(u -> GlobalSearchResult.builder()
                            .type("USER")
                            .id(u.getId())
                            .title(u.getFirstName() + " " + u.getLastName())
                            .subtitle(u.getEmail())
                            .build())
                    .toList());
        }

        if (type == null || type.equalsIgnoreCase("institution") || type.equalsIgnoreCase("all")) {
            Page<Institution> institutions = institutionRepository.findBySearchTermAndIsDeletedFalse(query, PageRequest.of(0, limit));
            results.addAll(institutions.getContent().stream()
                    .map(i -> GlobalSearchResult.builder()
                            .type("INSTITUTION")
                            .id(i.getId())
                            .title(i.getName())
                            .subtitle(i.getCode())
                            .build())
                    .toList());
        }

        return results.stream().limit(limit).toList();
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
                .createdAt(inst.getCreatedAt())
                .build();
    }
}
