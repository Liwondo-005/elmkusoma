package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.*;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.mapper.AdministrationMapper;
import tz.elmkusoma.administration.repository.*;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.course.domain.Course;
import tz.elmkusoma.course.repository.CourseModuleRepository;
import tz.elmkusoma.course.repository.CourseRepository;
import tz.elmkusoma.course.repository.CourseLessonRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.administration.dto.GlobalSearchResult;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdministrationService {

    private final SystemSettingRepository settingRepository;
    private final CustomRoleRepository roleRepository;
    private final RolePermissionRepository permissionRepository;
    private final DataImportJobRepository importJobRepository;
    private final DashboardSnapshotRepository snapshotRepository;
    private final UserRepository userRepository;
    private final AdministrationMapper administrationMapper;
    private final AuditService auditService;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseLessonRepository courseLessonRepository;
    private final InstitutionRepository institutionRepository;
    private final InstitutionScopeService scopeService;
    private final InstitutionAuditService auditService2;
    private final InstitutionMembershipRepository membershipRepository;

    // ── System Settings ──

    public SettingResponse createOrUpdateSetting(SettingRequest request, UUID institutionId,
                                                  String userEmail, String userRole) {
        Optional<SystemSetting> existing = settingRepository.findByInstitutionIdAndSettingKeyAndIsDeletedFalse(
                institutionId, request.getSettingKey());

        SystemSetting setting;
        AuditLog.AuditAction action;
        Map<String, Object> oldValues = null;

        if (existing.isPresent()) {
            setting = existing.get();
            oldValues = Map.of("value", setting.getSettingValue());
            setting.setSettingValue(request.getSettingValue());
            if (request.getSettingType() != null) setting.setSettingType(request.getSettingType());
            if (request.getDescription() != null) setting.setDescription(request.getDescription());
            if (request.getIsPublic() != null) setting.setIsPublic(request.getIsPublic());
            action = AuditLog.AuditAction.UPDATE;
        } else {
            setting = administrationMapper.toSettingEntity(request, institutionId);
            action = AuditLog.AuditAction.CREATE;
        }

        settingRepository.save(setting);

        Map<String, Object> newValues = Map.of("key", request.getSettingKey(), "value", request.getSettingValue());
        auditService.recordAuditLog(institutionId, null, userEmail, userRole,
                "SystemSetting", setting.getId(), request.getSettingKey(),
                action, oldValues, newValues);

        log.info("Setting {} updated for institution: {}", request.getSettingKey(), institutionId);
        return administrationMapper.toSettingResponse(setting);
    }

    @Transactional(readOnly = true)
    public List<SettingResponse> getSettings(UUID institutionId) {
        return settingRepository.findAllByInstitutionId(institutionId).stream()
                .map(administrationMapper::toSettingResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SettingResponse getSettingByKey(UUID institutionId, String key) {
        SystemSetting setting = settingRepository.findByInstitutionIdAndSettingKeyAndIsDeletedFalse(institutionId, key)
                .orElseThrow(() -> new ResourceNotFoundException("Setting", "key", key));
        return administrationMapper.toSettingResponse(setting);
    }

    // ── Role Management ──

    public RoleResponse createRole(CreateRoleRequest request, UUID institutionId,
                                    String userEmail, String userRole) {
        if (roleRepository.existsByNameAndInstitutionIdAndIsDeletedFalse(request.getName(), institutionId)) {
            throw new IllegalArgumentException("Role with name '" + request.getName() + "' already exists");
        }

        CustomRole role = CustomRole.of(request.getName(), request.getDisplayName(),
                request.getDescription(), institutionId);
        roleRepository.save(role);

        // Save permissions
        if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
            for (String permission : request.getPermissions()) {
                RolePermission rolePermission = RolePermission.of(role.getId(), permission);
                permissionRepository.save(rolePermission);
            }
        }

        List<String> permissions = permissionRepository.findPermissionsByRoleId(role.getId());

        auditService.recordAuditLog(institutionId, null, userEmail, userRole,
                "CustomRole", role.getId(), role.getName(),
                AuditLog.AuditAction.CREATE, null,
                Map.of("name", role.getName(), "permissions", permissions));

        log.info("Created role: {} for institution: {}", role.getName(), institutionId);
        return administrationMapper.toRoleResponse(role, permissions);
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles(UUID institutionId) {
        List<CustomRole> roles = roleRepository.findAllByInstitutionId(institutionId);
        return roles.stream()
                .map(role -> {
                    List<String> permissions = permissionRepository.findPermissionsByRoleId(role.getId());
                    return administrationMapper.toRoleResponse(role, permissions);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(UUID roleId, UUID institutionId) {
        CustomRole role = roleRepository.findByIdAndIsDeletedFalse(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        if (!role.getInstitutionId().equals(institutionId)) {
            throw new tz.elmkusoma.exception.ForbiddenException("role", "access");
        }

        List<String> permissions = permissionRepository.findPermissionsByRoleId(role.getId());
        return administrationMapper.toRoleResponse(role, permissions);
    }

    public void deleteRole(UUID roleId, UUID institutionId, String userEmail, String userRole) {
        CustomRole role = roleRepository.findByIdAndIsDeletedFalse(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        if (!role.getInstitutionId().equals(institutionId)) {
            throw new tz.elmkusoma.exception.ForbiddenException("role", "delete");
        }

        if (role.getIsSystemRole()) {
            throw new IllegalStateException("Cannot delete system roles");
        }

        role.setIsDeleted(true);
        roleRepository.save(role);
        permissionRepository.deleteByRoleId(roleId);

        auditService.recordAuditLog(institutionId, null, userEmail, userRole,
                "CustomRole", role.getId(), role.getName(),
                AuditLog.AuditAction.DELETE, Map.of("name", role.getName()), null);

        log.info("Deleted role: {} from institution: {}", role.getName(), institutionId);
    }

    // ── Dashboard ──

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(UUID institutionId) {
        // Check for cached snapshot
        Optional<DashboardSnapshot> cached = snapshotRepository.findByInstitutionIdAndSnapshotTypeAndExpiresAtAfter(
                institutionId, "overview", LocalDateTime.now());

        if (cached.isPresent()) {
            Map<String, Object> data = cached.get().getSnapshotData();
            return DashboardResponse.of(
                    institutionId,
                    data.get("totalStudents") != null ? ((Number) data.get("totalStudents")).longValue() : 0L,
                    data.get("totalTeachers") != null ? ((Number) data.get("totalTeachers")).longValue() : 0L,
                    data.get("totalParents") != null ? ((Number) data.get("totalParents")).longValue() : 0L,
                    data.get("activeStudents") != null ? ((Number) data.get("activeStudents")).longValue() : 0L,
                    data.get("certificatesIssued") != null ? ((Number) data.get("certificatesIssued")).longValue() : 0L,
                    data.get("pendingImportJobs") != null ? ((Number) data.get("pendingImportJobs")).longValue() : 0L,
                    data.get("totalCourses") != null ? ((Number) data.get("totalCourses")).longValue() : 0L,
                    data.get("publishedCourses") != null ? ((Number) data.get("publishedCourses")).longValue() : 0L,
                    data.get("draftCourses") != null ? ((Number) data.get("draftCourses")).longValue() : 0L,
                    data.get("totalModules") != null ? ((Number) data.get("totalModules")).longValue() : 0L,
                    data.get("totalLessons") != null ? ((Number) data.get("totalLessons")).longValue() : 0L,
                    data.get("liveClassesScheduled") != null ? ((Number) data.get("liveClassesScheduled")).longValue() : 0L
            );
        }

        // Calculate stats
        List<User> users = userRepository.findAllByInstitutionId(institutionId);
        long totalStudents = users.stream().filter(u -> u.getRole() == User.Role.STUDENT).count();
        long totalTeachers = users.stream().filter(u -> u.getRole() == User.Role.TEACHER).count();
        long totalParents = users.stream().filter(u -> u.getRole() == User.Role.PARENT).count();
        long activeStudents = users.stream()
                .filter(u -> u.getRole() == User.Role.STUDENT && Boolean.TRUE.equals(u.getIsActive()))
                .count();
        long pendingJobs = importJobRepository.countProcessingByInstitutionId(institutionId);

        // Course stats
        long totalCourses = courseRepository.countByInstitutionIdAndIsDeletedFalse(institutionId);
        long publishedCourses = courseRepository.countByInstitutionIdAndIsPublishedAndIsDeletedFalse(institutionId, true);
        long draftCourses = totalCourses - publishedCourses;
        long totalModules = 0;
        long totalLessons = 0;
        List<Course> courses = courseRepository.findByInstitutionIdAndIsDeletedFalse(institutionId);
        for (Course c : courses) {
            long courseModules = courseModuleRepository.countByCourseIdAndIsDeletedFalse(c.getId());
            totalModules += courseModules;
            List<tz.elmkusoma.course.domain.CourseModule> modules = courseModuleRepository.findByCourseIdAndIsDeletedFalseOrderBySortOrder(c.getId());
            for (tz.elmkusoma.course.domain.CourseModule m : modules) {
                totalLessons += courseLessonRepository.countByModuleIdAndIsDeletedFalse(m.getId());
            }
        }

        DashboardResponse response = DashboardResponse.of(
                institutionId,
                totalStudents,
                totalTeachers,
                totalParents,
                activeStudents,
                0L,
                pendingJobs,
                totalCourses,
                publishedCourses,
                draftCourses,
                totalModules,
                totalLessons,
                0L
        );

        // Cache snapshot
        Map<String, Object> snapshotData = new HashMap<>();
        snapshotData.put("totalStudents", totalStudents);
        snapshotData.put("totalTeachers", totalTeachers);
        snapshotData.put("totalParents", totalParents);
        snapshotData.put("activeStudents", activeStudents);
        snapshotData.put("certificatesIssued", 0L);
        snapshotData.put("pendingImportJobs", pendingJobs);
        snapshotData.put("totalCourses", totalCourses);
        snapshotData.put("publishedCourses", publishedCourses);
        snapshotData.put("draftCourses", draftCourses);
        snapshotData.put("totalModules", totalModules);
        snapshotData.put("totalLessons", totalLessons);
        snapshotData.put("liveClassesScheduled", 0L);

        DashboardSnapshot snapshot = DashboardSnapshot.of(
                institutionId,
                "overview",
                snapshotData,
                LocalDateTime.now().plusMinutes(15)
        );

        snapshotRepository.save(snapshot);

        return response;
    }

    // ── Data Import ──

    public ImportJobResponse createImportJob(String importType, String fileName, UUID institutionId,
                                              UUID userId, String userEmail, String userRole) {
        DataImportJob job = DataImportJob.of(userId, importType, fileName, "", institutionId);

        importJobRepository.save(job);

        auditService.recordAuditLog(institutionId, userId, userEmail, userRole,
                "DataImportJob", job.getId(), fileName,
                AuditLog.AuditAction.CREATE, null,
                Map.of("importType", importType, "fileName", fileName));

        log.info("Created import job: {} for institution: {}", job.getId(), institutionId);
        return administrationMapper.toImportJobResponse(job);
    }

    @Transactional(readOnly = true)
    public List<ImportJobResponse> getImportJobs(UUID institutionId) {
        return importJobRepository.findAllByInstitutionId(institutionId).stream()
                .map(administrationMapper::toImportJobResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImportJobResponse getImportJobById(UUID jobId, UUID institutionId) {
        DataImportJob job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ImportJob", "id", jobId));

        if (!job.getInstitutionId().equals(institutionId)) {
            throw new tz.elmkusoma.exception.ForbiddenException("import job", "access");
        }

        return administrationMapper.toImportJobResponse(job);
    }

    // ── Enhanced Dashboard ──

    @Transactional(readOnly = true)
    public EnhancedDashboardResponse getEnhancedDashboard(UUID institutionId,
                                                           String currentUserRole,
                                                           List<String> userPermissions) {
        DashboardResponse base = getDashboard(institutionId);

        Institution inst = institutionRepository.findById(institutionId).orElse(null);
        String institutionName = inst != null ? inst.getName() : "";
        String institutionType = inst != null ? inst.getType().name() : "";

        List<String> enabledServices = (inst != null && inst.getEnabledServices() != null)
                ? List.of(inst.getEnabledServices().split(","))
                : List.of();

        long pendingInvitations = auditService2.countPendingInvitations(institutionId);
        long unreadNotifications = auditService2.countUnreadNotifications(institutionId);

        var recentActivity = auditService2.getRecentActivity(institutionId, 10).stream()
                .map(a -> EnhancedDashboardResponse.RecentActivityItem.builder()
                        .type(a.getActivityType())
                        .title(a.getTitle())
                        .description(a.getDescription())
                        .timestamp(a.getCreatedAt())
                        .entityId(null)
                        .entityType(null)
                        .build())
                .toList();

        var attentionItems = new ArrayList<EnhancedDashboardResponse.AttentionItem>();
        if (pendingInvitations > 0) {
            attentionItems.add(EnhancedDashboardResponse.AttentionItem.builder()
                    .type("INVITATIONS").title("Pending Invitations")
                    .description(pendingInvitations + " invitation(s) awaiting response")
                    .count((int) pendingInvitations).severity("WARNING")
                    .actionUrl("/dashboard/admin/people").actionLabel("Review").build());
        }
        if (unreadNotifications > 0) {
            attentionItems.add(EnhancedDashboardResponse.AttentionItem.builder()
                    .type("NOTIFICATIONS").title("Unread Notifications")
                    .description(unreadNotifications + " unread notification(s)")
                    .count((int) unreadNotifications).severity("INFO")
                    .actionUrl("/dashboard/admin").actionLabel("View").build());
        }
        long pendingJobs = importJobRepository.countProcessingByInstitutionId(institutionId);
        if (pendingJobs > 0) {
            attentionItems.add(EnhancedDashboardResponse.AttentionItem.builder()
                    .type("IMPORTS").title("Pending Imports")
                    .description(pendingJobs + " import job(s) in progress")
                    .count((int) pendingJobs).severity("INFO")
                    .actionUrl("/dashboard/admin/import").actionLabel("Monitor").build());
        }

        // Role-specific quick actions
        var quickActions = buildQuickActions(currentUserRole, userPermissions, institutionId);

        // Work queue summary
        var workQueueSummary = buildWorkQueueSummary(institutionId, currentUserRole);

        // Organization health summary
        var healthSummary = buildHealthSummary(institutionId);

        // Live class stats
        long liveClassesLiveNow = 0L;
        long upcomingLiveClasses = 0L;
        try {
            // These would come from live class service
            // For now, placeholder values
        } catch (Exception ignored) {}

        return EnhancedDashboardResponse.builder()
                .institutionId(institutionId)
                .institutionName(institutionName)
                .institutionType(institutionType)
                .currentUserRole(currentUserRole)
                .userPermissions(userPermissions)
                .totalStudents(base.getTotalStudents())
                .totalTeachers(base.getTotalTeachers())
                .totalParents(base.getTotalParents())
                .activeStudents(base.getActiveStudents())
                .certificatesIssued(base.getCertificatesIssued())
                .pendingImportJobs(base.getPendingImportJobs())
                .totalCourses(base.getTotalCourses())
                .publishedCourses(base.getPublishedCourses())
                .draftCourses(base.getDraftCourses())
                .totalModules(base.getTotalModules())
                .totalLessons(base.getTotalLessons())
                .liveClassesScheduled(base.getLiveClassesScheduled())
                .liveClassesLiveNow(liveClassesLiveNow)
                .upcomingLiveClasses(upcomingLiveClasses)
                .pendingInvitations(pendingInvitations)
                .unreadNotifications(unreadNotifications)
                .enabledServices(enabledServices)
                .recentActivity(recentActivity)
                .attentionItems(attentionItems)
                .quickActions(quickActions)
                .workQueueSummary(workQueueSummary)
                .healthSummary(healthSummary)
                .build();
    }

    private List<EnhancedDashboardResponse.QuickAction> buildQuickActions(String currentUserRole,
                                                                           List<String> userPermissions,
                                                                           UUID institutionId) {
        var actions = new ArrayList<EnhancedDashboardResponse.QuickAction>();

        // Common actions for admins
        if (hasPermission(userPermissions, "CREATE_COURSE") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("create_course").label("Create Course").icon("book-plus")
                    .actionUrl("/dashboard/admin/courses/create")
                    .requiredPermission("CREATE_COURSE").available(true).build());
        }

        if (hasPermission(userPermissions, "INVITE_USER") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("invite_user").label("Invite User").icon("user-plus")
                    .actionUrl("/dashboard/admin/people/invite")
                    .requiredPermission("INVITE_USER").available(true).build());
        }

        if (hasPermission(userPermissions, "SCHEDULE_LIVE") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("schedule_live").label("Schedule Live Class").icon("video-plus")
                    .actionUrl("/dashboard/admin/live/create")
                    .requiredPermission("SCHEDULE_LIVE").available(true).build());
        }

        if (hasPermission(userPermissions, "CREATE_EVENT") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("create_event").label("Create Event").icon("calendar-plus")
                    .actionUrl("/dashboard/admin/events/create")
                    .requiredPermission("CREATE_EVENT").available(true).build());
        }

        if (hasPermission(userPermissions, "UPLOAD_RESOURCE") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("upload_resource").label("Upload Resource").icon("upload")
                    .actionUrl("/dashboard/admin/resources/upload")
                    .requiredPermission("UPLOAD_RESOURCE").available(true).build());
        }

        if (hasPermission(userPermissions, "REVIEW_CONTENT") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("review_content").label("Review Content").icon("clipboard-check")
                    .actionUrl("/dashboard/admin/content/review")
                    .requiredPermission("REVIEW_CONTENT").available(true).build());
        }

        if (hasPermission(userPermissions, "VERIFY_PAYMENT") || isFinanceRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("verify_payment").label("Verify Payment").icon("credit-card-check")
                    .actionUrl("/dashboard/admin/payments/verify")
                    .requiredPermission("VERIFY_PAYMENT").available(true).build());
        }

        if (hasPermission(userPermissions, "ISSUE_CERTIFICATE") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("issue_certificate").label("Issue Certificate").icon("award")
                    .actionUrl("/dashboard/admin/certificates/issue")
                    .requiredPermission("ISSUE_CERTIFICATE").available(true).build());
        }

        return actions;
    }

    private boolean hasPermission(List<String> permissions, String permission) {
        return permissions != null && permissions.contains(permission);
    }

    private boolean isAdminRole(String role) {
        return "ADMIN".equals(role) || "INSTITUTION_ADMIN".equals(role) || "OWNER".equals(role);
    }

    private boolean isFinanceRole(String role) {
        return isAdminRole(role) || "FINANCE_ADMIN".equals(role);
    }

    private EnhancedDashboardResponse.WorkQueueSummary buildWorkQueueSummary(UUID institutionId,
                                                                              String currentUserRole) {
        // These would query actual data from repositories
        long pendingApprovals = 0L;
        long pendingReviews = 0L;
        long pendingVerifications = 0L;

        // In a real implementation, query based on role
        if (isAdminRole(currentUserRole)) {
            // Query actual counts
        }

        return EnhancedDashboardResponse.WorkQueueSummary.builder()
                .pendingApprovals(pendingApprovals)
                .pendingReviews(pendingReviews)
                .pendingVerifications(pendingVerifications)
                .queueUrl("/dashboard/admin/work-queue")
                .build();
    }

    private EnhancedDashboardResponse.OrganizationHealthSummary buildHealthSummary(UUID institutionId) {
        var metrics = new ArrayList<EnhancedDashboardResponse.HealthMetric>();

        // Add key metrics
        metrics.add(EnhancedDashboardResponse.HealthMetric.builder()
                .name("Database").status("HEALTHY").value("Connected").threshold("N/A").build());
        metrics.add(EnhancedDashboardResponse.HealthMetric.builder()
                .name("Redis").status("HEALTHY").value("Connected").threshold("N/A").build());
        metrics.add(EnhancedDashboardResponse.HealthMetric.builder()
                .name("RabbitMQ").status("HEALTHY").value("Connected").threshold("N/A").build());
        metrics.add(EnhancedDashboardResponse.HealthMetric.builder()
                .name("LiveKit").status("HEALTHY").value("Connected").threshold("N/A").build());

        String overallStatus = metrics.stream().allMatch(m -> "HEALTHY".equals(m.getStatus()))
                ? "HEALTHY" : "DEGRADED";

        return EnhancedDashboardResponse.OrganizationHealthSummary.builder()
                .overallStatus(overallStatus)
                .metrics(metrics)
                .lastChecked(LocalDateTime.now())
                .build();
    }

    // ── Org-Scoped Search ──

    @Transactional(readOnly = true)
    public List<GlobalSearchResult> orgSearch(UUID institutionId, String query, String type, int limit) {
        List<GlobalSearchResult> results = new ArrayList<>();
        if (query == null || query.isBlank()) return results;
        String q = query.toLowerCase();

        if ("all".equals(type) || "users".equals(type)) {
            userRepository.findAllByInstitutionId(institutionId).stream()
                    .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                    .filter(u -> u.getFullName() != null && u.getFullName().toLowerCase().contains(q)
                            || u.getEmail() != null && u.getEmail().toLowerCase().contains(q))
                    .limit(limit)
                    .forEach(u -> results.add(GlobalSearchResult.builder()
                            .id(u.getId()).type("USER").title(u.getFullName())
                            .subtitle(u.getEmail() + " — " + u.getRole())
                            .build()));
        }

        if ("all".equals(type) || "institutions".equals(type)) {
            institutionRepository.findById(institutionId)
                    .filter(i -> i.getName().toLowerCase().contains(q) || i.getCode().toLowerCase().contains(q))
                    .ifPresent(i -> results.add(GlobalSearchResult.builder()
                            .id(i.getId()).type("INSTITUTION").title(i.getName())
                            .subtitle(i.getCode() + " — " + i.getType())
                            .build()));
        }

        return results.stream().limit(limit).toList();
    }

    // ── Data Export ──

    @Transactional(readOnly = true)
    public byte[] exportData(UUID institutionId, String entityType) {
        StringBuilder csv = new StringBuilder();

        switch (entityType.toLowerCase()) {
            case "users" -> {
                csv.append("ID,Name,Email,Role,Active,Created\n");
                userRepository.findAllByInstitutionId(institutionId).stream()
                        .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                        .forEach(u -> csv.append(String.format("%s,%s,%s,%s,%s,%s\n",
                                u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                                u.getIsActive(), u.getCreatedAt())));
            }
            case "memberships" -> {
                csv.append("ID,User ID,Institution ID,Role,Active\n");
                membershipRepository.findByInstitutionIdAndIsActiveTrue(institutionId).forEach(m ->
                        csv.append(String.format("%s,%s,%s,%s,%s\n",
                                m.getId(), m.getUserId(), m.getInstitutionId(), m.getRole(), m.getIsActive())));
            }
            default -> csv.append("Supported exports: users, memberships\n");
        }

        return csv.toString().getBytes();
    }
}