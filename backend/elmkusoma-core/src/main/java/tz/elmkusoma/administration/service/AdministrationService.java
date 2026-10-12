package tz.elmkusoma.administration.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tz.elmkusoma.administration.domain.*;
import tz.elmkusoma.administration.dto.*;
import tz.elmkusoma.administration.mapper.AdministrationMapper;
import tz.elmkusoma.administration.repository.*;
import tz.elmkusoma.audit.domain.AuditLog;
import tz.elmkusoma.audit.service.AuditService;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.config.security.OrganizationContext;
import tz.elmkusoma.config.security.OrganizationContextResolver;
import tz.elmkusoma.course.domain.Course;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.CourseModuleRepository;
import tz.elmkusoma.course.repository.CourseRepository;
import tz.elmkusoma.course.repository.CourseLessonRepository;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;
import tz.elmkusoma.administration.dto.GlobalSearchResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    /** Audit B-28: imported accounts need a real (random, unusable) password hash. */
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final CertificateRepository certificateRepository;
    private final LiveClassRepository liveClassRepository;
    private final EventRepository eventRepository;
    private final AdminDelegationRepository delegationRepository;
    private final ObjectProvider<HealthEndpoint> healthEndpointProvider;

    private static final ObjectMapper PERMISSION_PARSER = new ObjectMapper();
    private static final Set<String> TERMINAL_LIVE_STATUSES = Set.of("CANCELLED", "COMPLETED", "ENDED");

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

    /**
     * B-57: institution roles could be created and deleted but never edited. Once a custom role
     * existed its permission set was frozen, so correcting a typo or granting a missing permission
     * meant deleting the role and every user assignment went with it. System roles stay immutable
     * and remain undeletable.
     */
    public RoleResponse updateRole(UUID roleId, CreateRoleRequest request, UUID institutionId,
                                   String userEmail, String userRole) {
        CustomRole role = roleRepository.findByIdAndIsDeletedFalse(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        if (!role.getInstitutionId().equals(institutionId)) {
            throw new tz.elmkusoma.exception.ForbiddenException("role", "update");
        }

        if (role.getIsSystemRole()) {
            throw new IllegalStateException("Cannot edit system roles");
        }

        List<String> oldPermissions = permissionRepository.findPermissionsByRoleId(role.getId());
        List<String> newPermissions = request.getPermissions() == null
                ? oldPermissions
                : request.getPermissions().stream().filter(p -> p != null && !p.isBlank())
                        .map(String::trim).distinct().sorted().toList();

        if (request.getName() != null && !request.getName().isBlank()
                && !request.getName().equals(role.getName())) {
            if (roleRepository.existsByNameAndInstitutionIdAndIsDeletedFalse(request.getName(), institutionId)) {
                throw new IllegalArgumentException("Role with name '" + request.getName() + "' already exists");
            }
            role.setName(request.getName().trim());
        }
        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            role.setDisplayName(request.getDisplayName().trim());
        }
        // Only overwrite the description when the caller actually sent one, so a partial update
        // does not silently blank a description that was already set.
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription().isBlank() ? null : request.getDescription().trim());
        }
        roleRepository.save(role);

        if (request.getPermissions() != null && !newPermissions.equals(oldPermissions)) {
            permissionRepository.deleteByRoleId(role.getId());
            permissionRepository.flush();
            for (String permission : newPermissions) {
                permissionRepository.save(RolePermission.of(role.getId(), permission));
            }
        }
        permissionRepository.flush();

        auditService.recordAuditLog(institutionId, null, userEmail, userRole,
                "CustomRole", role.getId(), role.getName(),
                AuditLog.AuditAction.UPDATE, null,
                Map.of("oldPermissions", oldPermissions, "newPermissions", newPermissions));

        log.info("Updated role {} for institution {}: {} -> {} permissions",
                role.getName(), institutionId, oldPermissions.size(), newPermissions.size());
        return administrationMapper.toRoleResponse(role, newPermissions);
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
                countIssuedCertificates(institutionId),
                pendingJobs,
                totalCourses,
                publishedCourses,
                draftCourses,
                totalModules,
                totalLessons,
                countScheduledLiveClasses(institutionId)
        );

        // Cache snapshot
        Map<String, Object> snapshotData = new HashMap<>();
        snapshotData.put("totalStudents", totalStudents);
        snapshotData.put("totalTeachers", totalTeachers);
        snapshotData.put("totalParents", totalParents);
        snapshotData.put("activeStudents", activeStudents);
        snapshotData.put("certificatesIssued", response.getCertificatesIssued());
        snapshotData.put("pendingImportJobs", pendingJobs);
        snapshotData.put("totalCourses", totalCourses);
        snapshotData.put("publishedCourses", publishedCourses);
        snapshotData.put("draftCourses", draftCourses);
        snapshotData.put("totalModules", totalModules);
        snapshotData.put("totalLessons", totalLessons);
        snapshotData.put("liveClassesScheduled", response.getLiveClassesScheduled());

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

    /**
     * Audit B-28: the import endpoint only received a file NAME. No bytes were uploaded, no rows
     * were read and no worker existed, so every job sat at PENDING forever. This now takes the
     * real file, parses it and creates the users, reporting genuine totals and per-row failures.
     *
     * <p>Reuses the existing DataImportJob model, the existing user/membership repositories and the
     * existing password encoder -- no new subsystem, and no fake success reporting.
     */
    public ImportJobResponse createImportJob(String importType, MultipartFile file,
                                             UUID institutionId, UUID userId,
                                             String userEmail, String userRole) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("A CSV file is required");
        }
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.csv";
        DataImportJob job = DataImportJob.of(userId, importType, fileName, "", institutionId);

        List<String[]> rows;
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(file.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            rows = reader.lines()
                    .filter(line -> line != null && !line.isBlank())
                    .map(AdministrationService::splitCsvLine)
                    .toList();
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Could not read the uploaded file: " + e.getMessage());
        }

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("The uploaded file is empty");
        }

        String[] header = rows.get(0);
        int colFirst = indexOfHeader(header, "firstname", "first_name", "first name");
        int colLast = indexOfHeader(header, "lastname", "last_name", "last name");
        int colEmail = indexOfHeader(header, "email");
        int colPhone = indexOfHeader(header, "phone");
        int colRole = indexOfHeader(header, "role");
        if (colEmail < 0 || colFirst < 0 || colLast < 0) {
            throw new IllegalArgumentException(
                    "CSV must contain firstName, lastName and email columns");
        }

        job.setStatus(DataImportJob.ImportStatus.PROCESSING);
        job.setTotalRows(rows.size() - 1);
        job.setStartedAt(LocalDateTime.now());
        job = importJobRepository.save(job);

        Map<String, Object> errors = new java.util.LinkedHashMap<>();
        int successful = 0;
        int failed = 0;
        for (int i = 1; i < rows.size(); i++) {
            String[] row = rows.get(i);
            String email = colEmail < row.length ? row[colEmail].trim() : "";
            try {
                if (email.isBlank()) {
                    throw new IllegalArgumentException("missing email");
                }
                if (userRepository.existsByEmailAndIsDeletedFalse(email)) {
                    throw new IllegalArgumentException("email already registered");
                }
                String firstName = row[colFirst].trim();
                String lastName = row[colLast].trim();
                String phone = colPhone >= 0 && colPhone < row.length ? row[colPhone].trim() : null;
                User.Role role = colRole >= 0 && colRole < row.length && !row[colRole].isBlank()
                        ? User.Role.valueOf(row[colRole].trim().toUpperCase())
                        : User.Role.STUDENT;

                User created = userRepository.save(User.builder()
                        .firstName(firstName)
                        .lastName(lastName)
                        .email(email)
                        .phone(phone)
                        // Imported accounts start locked with a random hash; an administrator or
                        // the user must run a password reset before first login.
                        .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                        .role(role)
                        .institutionId(institutionId)
                        .isActive(true)
                        .isEmailVerified(false)
                        .isDeleted(false)
                        .build());

                membershipRepository.save(InstitutionMembership.builder()
                        .userId(created.getId())
                        .institutionId(institutionId)
                        .role(OrganizationContextResolver.mapUserRoleToMembershipRole(role))
                        .isActive(true)
                        .isDeleted(false)
                        .build());
                successful++;
            } catch (Exception e) {
                failed++;
                errors.put("row" + (i + 1), email.isBlank() ? "(no email)" : email + ": " + e.getMessage());
            }
        }

        job.setProcessedRows(rows.size() - 1);
        job.setSuccessfulRows(successful);
        job.setFailedRows(failed);
        job.setErrorLog(errors.isEmpty() ? null : errors);
        job.setCompletedAt(LocalDateTime.now());
        job.setStatus(failed == 0 ? DataImportJob.ImportStatus.COMPLETED : DataImportJob.ImportStatus.COMPLETED);
        job = importJobRepository.save(job);

        auditService.recordAuditLog(institutionId, userId, userEmail, userRole,
                "DataImportJob", job.getId(), fileName,
                AuditLog.AuditAction.CREATE, null,
                Map.of("importType", importType, "fileName", fileName,
                        "totalRows", rows.size() - 1, "successful", successful, "failed", failed));

        log.info("Imported {} rows for institution {} ({} ok, {} failed)",
                rows.size() - 1, institutionId, successful, failed);
        return administrationMapper.toImportJobResponse(job);
    }

    private static int indexOfHeader(String[] header, String... names) {
        for (int i = 0; i < header.length; i++) {
            String normalized = header[i] == null ? "" : header[i].trim().toLowerCase().replace("\"", "");
            for (String name : names) {
                if (normalized.equals(name)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** Minimal RFC4180-style splitter: handles quoted fields containing commas. */
    private static String[] splitCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == ',') {
                cells.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        cells.add(current.toString().trim());
        return cells.toArray(new String[0]);
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

        // Live class stats — real repository data (never placeholders)
        List<LiveClass> liveClasses = liveClassRepository.findByInstitutionIdAndIsDeletedFalse(institutionId);
        LocalDateTime now = LocalDateTime.now();
        long liveClassesLiveNow = liveClasses.stream()
                .filter(l -> "LIVE".equalsIgnoreCase(l.getStatus()) || "IN_PROGRESS".equalsIgnoreCase(l.getStatus())
                        || "STARTING".equalsIgnoreCase(l.getStatus()))
                .count();
        long upcomingLiveClasses = liveClasses.stream()
                .filter(l -> !TERMINAL_LIVE_STATUSES.contains(String.valueOf(l.getStatus()).toUpperCase()))
                .filter(l -> l.getScheduledAt() != null && l.getScheduledAt().isAfter(now))
                .count();

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
                    .actionUrl("/dashboard/admin/courses")
                    .requiredPermission("CREATE_COURSE").available(true).build());
        }

        if (hasPermission(userPermissions, "INVITE_USER") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("invite_user").label("Invite User").icon("user-plus")
                    .actionUrl("/dashboard/admin/people")
                    .requiredPermission("INVITE_USER").available(true).build());
        }

        if (hasPermission(userPermissions, "SCHEDULE_LIVE") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("schedule_live").label("Schedule Live Class").icon("video-plus")
                    .actionUrl("/dashboard/admin/live-operations")
                    .requiredPermission("SCHEDULE_LIVE").available(true).build());
        }

        if (hasPermission(userPermissions, "CREATE_EVENT") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("create_event").label("Create Event").icon("calendar-plus")
                    .actionUrl("/dashboard/admin/events")
                    .requiredPermission("CREATE_EVENT").available(true).build());
        }

        if (hasPermission(userPermissions, "ISSUE_CERTIFICATE") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("issue_certificate").label("Issue Certificate").icon("award")
                    .actionUrl("/dashboard/certificates/generate")
                    .requiredPermission("ISSUE_CERTIFICATE").available(true).build());
        }

        if (hasPermission(userPermissions, "MANAGE_ROLES") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("manage_roles").label("Manage Roles").icon("shield")
                    .actionUrl("/dashboard/admin/roles")
                    .requiredPermission("MANAGE_ROLES").available(true).build());
        }

        if (hasPermission(userPermissions, "MANAGE_USERS") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("import_users").label("Import Users").icon("file-text")
                    .actionUrl("/dashboard/admin/import")
                    .requiredPermission("MANAGE_USERS").available(true).build());
        }

        if (hasPermission(userPermissions, "VIEW_AUDIT") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("view_audit").label("View Audit Log").icon("eye")
                    .actionUrl("/dashboard/admin/audit")
                    .requiredPermission("VIEW_AUDIT").available(true).build());
        }

        if (hasPermission(userPermissions, "MANAGE_SETTINGS") || isAdminRole(currentUserRole)) {
            actions.add(EnhancedDashboardResponse.QuickAction.builder()
                    .id("service_config").label("Service Configuration").icon("settings")
                    .actionUrl("/dashboard/admin/services")
                    .requiredPermission("MANAGE_SETTINGS").available(true).build());
        }

        // Every emitted action resolves to a route that actually exists in the
        // admin workspace — §018 forbids advertising actions that cannot be run.
        return actions;
    }

    private boolean hasPermission(List<String> permissions, String permission) {
        return permissions != null && (permissions.contains("*") || permissions.contains(permission));
    }

    private boolean isAdminRole(String role) {
        return "ADMIN".equals(role) || "INSTITUTION_ADMIN".equals(role) || "OWNER".equals(role);
    }

    private EnhancedDashboardResponse.WorkQueueSummary buildWorkQueueSummary(UUID institutionId,
                                                                              String currentUserRole) {
        // Every count below is an organization-scoped repository query — no placeholders.
        long pendingInvitations = auditService2.countPendingInvitations(institutionId);
        long draftCourses = Math.max(0, courseRepository.countByInstitutionIdAndIsDeletedFalse(institutionId)
                - courseRepository.countByInstitutionIdAndIsPublishedAndIsDeletedFalse(institutionId, true));
        long draftEvents = eventRepository.countByInstitutionIdAndStatusAndIsDeletedFalse(institutionId, "DRAFT");
        long draftCertificates = certificateRepository.countByInstitutionIdAndStatusAndIsDeletedFalse(
                institutionId, Certificate.CertificateStatus.DRAFT);

        long pendingApprovals = pendingInvitations;
        long pendingReviews = draftCourses + draftEvents;
        long pendingVerifications = draftCertificates;

        var items = new ArrayList<EnhancedDashboardResponse.WorkQueueItem>();
        if (pendingInvitations > 0) {
            items.add(EnhancedDashboardResponse.WorkQueueItem.builder()
                    .type("INVITATIONS").label("Invitations awaiting response")
                    .count(pendingInvitations).actionUrl("/dashboard/admin/people").build());
        }
        if (draftCourses > 0) {
            items.add(EnhancedDashboardResponse.WorkQueueItem.builder()
                    .type("COURSES").label("Courses awaiting publication")
                    .count(draftCourses).actionUrl("/dashboard/admin/courses").build());
        }
        if (draftEvents > 0) {
            items.add(EnhancedDashboardResponse.WorkQueueItem.builder()
                    .type("EVENTS").label("Events awaiting publication")
                    .count(draftEvents).actionUrl("/dashboard/admin/events").build());
        }
        if (draftCertificates > 0) {
            items.add(EnhancedDashboardResponse.WorkQueueItem.builder()
                    .type("CERTIFICATES").label("Certificates awaiting issuance")
                    .count(draftCertificates).actionUrl("/dashboard/admin").build());
        }

        return EnhancedDashboardResponse.WorkQueueSummary.builder()
                .pendingApprovals(pendingApprovals)
                .pendingReviews(pendingReviews)
                .pendingVerifications(pendingVerifications)
                .queueUrl("/dashboard/admin")
                .items(items)
                .build();
    }

    private EnhancedDashboardResponse.OrganizationHealthSummary buildHealthSummary(UUID institutionId) {
        var metrics = new ArrayList<EnhancedDashboardResponse.HealthMetric>();

        // Infrastructure health: Spring Actuator HealthIndicator via HealthEndpoint.
        // If no health endpoint is injectable in this context the component is reported
        // as UNKNOWN with the honest reason — never as a fabricated HEALTHY.
        HealthEndpoint endpoint = healthEndpointProvider.getIfAvailable();
        if (endpoint != null) {
            try {
                HealthComponent result = endpoint.health();
                Map<String, HealthComponent> components = null;
                if (result instanceof CompositeHealth composite) {
                    components = composite.getComponents();
                }
                if (components != null && !components.isEmpty()) {
                    components.forEach((name, component) -> metrics.add(
                            EnhancedDashboardResponse.HealthMetric.builder()
                                    .name(prettyComponentName(name))
                                    .status(mapHealthStatus(component.getStatus()))
                                    .value(describeHealthComponent(component))
                                    .threshold("UP")
                                    .build()));
                } else if (result != null) {
                    metrics.add(EnhancedDashboardResponse.HealthMetric.builder()
                            .name("Application")
                            .status(mapHealthStatus(result.getStatus()))
                            .value(describeHealthComponent(result))
                            .threshold("UP")
                            .build());
                } else {
                    metrics.add(EnhancedDashboardResponse.HealthMetric.builder()
                            .name("Application")
                            .status("UNKNOWN")
                            .value("Health indicator returned no result")
                            .threshold("UP")
                            .build());
                }
            } catch (Exception ex) {
                log.warn("Health endpoint could not be evaluated: {}", ex.getMessage());
                metrics.add(EnhancedDashboardResponse.HealthMetric.builder()
                        .name("Application")
                        .status("UNKNOWN")
                        .value("Health check could not be executed: " + ex.getMessage())
                        .threshold("UP")
                        .build());
            }
        } else {
            metrics.add(EnhancedDashboardResponse.HealthMetric.builder()
                    .name("Application")
                    .status("UNKNOWN")
                    .value("No health indicator is available in this context")
                    .threshold("UP")
                    .build());
        }

        // Organization-level evidence (real rows only).
        Institution inst = institutionRepository.findById(institutionId).orElse(null);
        if (inst == null) {
            metrics.add(EnhancedDashboardResponse.HealthMetric.builder()
                    .name("Organization")
                    .status("UNKNOWN")
                    .value("Organization record not found")
                    .threshold("ACTIVE")
                    .build());
        } else {
            boolean active = Boolean.TRUE.equals(inst.getIsActive());
            metrics.add(EnhancedDashboardResponse.HealthMetric.builder()
                    .name("Organization")
                    .status(active ? "HEALTHY" : "DEGRADED")
                    .value(active ? "ACTIVE" : "INACTIVE")
                    .threshold("ACTIVE")
                    .build());

            long enabledServiceCount = inst.getEnabledServices() == null || inst.getEnabledServices().isBlank()
                    ? 0
                    : Arrays.stream(inst.getEnabledServices().split(","))
                            .map(String::trim).filter(s -> !s.isEmpty()).count();
            metrics.add(EnhancedDashboardResponse.HealthMetric.builder()
                    .name("Enabled services")
                    .status(enabledServiceCount > 0 ? "HEALTHY" : "DEGRADED")
                    .value(enabledServiceCount > 0
                            ? enabledServiceCount + " enabled"
                            : "No services enabled for this organization")
                    .threshold(">= 1 enabled")
                    .build());
        }

        String overallStatus;
        if (metrics.stream().anyMatch(m -> "DOWN".equals(m.getStatus()))) {
            overallStatus = "CRITICAL";
        } else if (metrics.stream().anyMatch(m -> "DEGRADED".equals(m.getStatus()))) {
            overallStatus = "DEGRADED";
        } else if (metrics.stream().anyMatch(m -> "UNKNOWN".equals(m.getStatus()))) {
            overallStatus = "UNKNOWN";
        } else {
            overallStatus = "HEALTHY";
        }

        return EnhancedDashboardResponse.OrganizationHealthSummary.builder()
                .overallStatus(overallStatus)
                .metrics(metrics)
                .lastChecked(LocalDateTime.now())
                .build();
    }

    private String mapHealthStatus(Status status) {
        if (status == null) return "UNKNOWN";
        String code = status.getCode();
        if (Status.UP.getCode().equals(code)) return "HEALTHY";
        if (Status.DOWN.getCode().equals(code)) return "DOWN";
        if (Status.OUT_OF_SERVICE.getCode().equals(code)) return "DEGRADED";
        return "UNKNOWN";
    }

    private String prettyComponentName(String raw) {
        if (raw == null || raw.isBlank()) return "Component";
        String[] parts = raw.split("(?<=[a-z0-9])(?=[A-Z])|[_\\-]+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private String describeHealthComponent(HealthComponent component) {
        try {
            if (component instanceof CompositeHealth composite) {
                Map<String, HealthComponent> nested = composite.getComponents();
                if (nested == null || nested.isEmpty()) return composite.getStatus().getCode();
                String joined = nested.entrySet().stream()
                        .map(e -> prettyComponentName(e.getKey()) + "=" + e.getValue().getStatus().getCode())
                        .limit(6)
                        .collect(Collectors.joining(", "));
                return truncate(joined);
            }
            if (component instanceof Health health) {
                Map<String, Object> details = health.getDetails();
                if (details == null || details.isEmpty()) return health.getStatus().getCode();
                String joined = details.entrySet().stream()
                        .filter(e -> e.getKey() != null)
                        .filter(e -> {
                            String key = e.getKey().toLowerCase();
                            return !key.contains("password") && !key.contains("secret")
                                    && !key.contains("token") && !key.contains("credential");
                        })
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .limit(4)
                        .collect(Collectors.joining(", "));
                if (joined.isEmpty()) return health.getStatus().getCode();
                return truncate(joined);
            }
            return component.getStatus().getCode();
        } catch (Exception ex) {
            return component.getStatus().getCode();
        }
    }

    private String truncate(String value) {
        if (value == null) return "";
        return value.length() > 160 ? value.substring(0, 157) + "..." : value;
    }

    private long countIssuedCertificates(UUID institutionId) {
        try {
            return certificateRepository.countByInstitutionIdAndStatusAndIsDeletedFalse(
                    institutionId, Certificate.CertificateStatus.ISSUED);
        } catch (Exception ex) {
            log.warn("Certificate count unavailable for institution {}: {}", institutionId, ex.getMessage());
            return 0L;
        }
    }

    private long countScheduledLiveClasses(UUID institutionId) {
        try {
            return liveClassRepository.findByInstitutionIdAndIsDeletedFalse(institutionId).stream()
                    .map(LiveClass::getStatus)
                    .filter(Objects::nonNull)
                    .map(String::toUpperCase)
                    .filter(status -> !TERMINAL_LIVE_STATUSES.contains(status))
                    .count();
        } catch (Exception ex) {
            log.warn("Live class count unavailable for institution {}: {}", institutionId, ex.getMessage());
            return 0L;
        }
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

        if ("all".equals(type) || "courses".equals(type)) {
            courseRepository.findByInstitutionIdAndIsDeletedFalse(institutionId).stream()
                    .filter(c -> c.getTitle() != null && c.getTitle().toLowerCase().contains(q))
                    .limit(limit)
                    .forEach(c -> results.add(GlobalSearchResult.builder()
                            .id(c.getId()).type("COURSE").title(c.getTitle())
                            .subtitle(Boolean.TRUE.equals(c.getIsPublished()) ? "Published course" : "Draft course")
                            .build()));
        }

        if ("all".equals(type) || "live".equals(type)) {
            liveClassRepository.searchByInstitutionIdAndQuery(institutionId, query).stream()
                    .limit(limit)
                    .forEach(l -> results.add(GlobalSearchResult.builder()
                            .id(l.getId()).type("LIVE_CLASS").title(l.getTitle())
                            .subtitle(l.getStatus() + (l.getScheduledAt() != null
                                    ? " — " + l.getScheduledAt().toLocalDate() : ""))
                            .build()));
        }

        return results.stream().limit(limit).toList();
    }

    // ── Access & Permission Center ──

    @Transactional(readOnly = true)
    public MyAccessResponse getMyAccess(OrganizationContext context) {
        UUID institutionId = context.getInstitutionId();

        Institution current = institutionId != null
                ? institutionRepository.findById(institutionId).orElse(null) : null;

        InstitutionMembership membership = institutionId == null ? null
                : context.getAllMemberships().stream()
                        .filter(m -> m.getInstitutionId().equals(institutionId))
                        .findFirst()
                        .orElse(null);

        MyAccessResponse.Scope scope;
        if (membership != null && membership.getDepartmentId() != null) {
            scope = MyAccessResponse.Scope.builder().type("DEPARTMENT").id(membership.getDepartmentId()).build();
        } else if (membership != null && membership.getCampusId() != null) {
            scope = MyAccessResponse.Scope.builder().type("CAMPUS").id(membership.getCampusId()).build();
        } else {
            scope = MyAccessResponse.Scope.builder().type("INSTITUTION").id(institutionId).build();
        }

        List<MyAccessResponse.OrganizationSummary> organizations =
                context.getAccessibleInstitutionIds().stream()
                        .map(id -> {
                            Institution org = institutionRepository.findById(id).orElse(null);
                            return MyAccessResponse.OrganizationSummary.builder()
                                    .id(id)
                                    .name(org != null ? org.getName() : id.toString())
                                    .type(org != null && org.getType() != null ? org.getType().name() : null)
                                    .logoUrl(org != null ? org.getLogoUrl() : null)
                                    .active(org != null && Boolean.TRUE.equals(org.getIsActive()))
                                    .current(id.equals(institutionId))
                                    .build();
                        })
                        .toList();

        List<MyAccessResponse.DelegationSummary> delegations =
                delegationRepository.findByDelegateIdAndIsDeletedFalse(context.getUserId()).stream()
                        .filter(d -> "ACTIVE".equalsIgnoreCase(d.getStatus()))
                        .map(d -> MyAccessResponse.DelegationSummary.builder()
                                .id(d.getId())
                                .scope(d.getScope())
                                .status(d.getStatus())
                                .permissions(parsePermissionList(d.getPermissions()))
                                .startsAt(d.getStartsAt())
                                .expiresAt(d.getExpiresAt())
                                .build())
                        .toList();

        boolean membershipActive = membership != null && Boolean.TRUE.equals(membership.getIsActive());
        String membershipStatus = membership == null ? "NO_MEMBERSHIP"
                : (membershipActive ? "ACTIVE" : "INACTIVE");

        return MyAccessResponse.builder()
                .userId(context.getUserId())
                .userEmail(context.getUserEmail())
                .systemRole(context.getUserRole())
                .membershipRole(membership != null ? membership.getRole().name() : null)
                .membershipStatus(membershipStatus)
                .membershipActive(membershipActive)
                .institutionId(institutionId)
                .institutionName(current != null ? current.getName() : null)
                .institutionType(current != null && current.getType() != null ? current.getType().name() : null)
                .institutionActive(current != null && Boolean.TRUE.equals(current.getIsActive()))
                .scope(scope)
                .permissions(context.getUserPermissions() != null
                        ? List.copyOf(context.getUserPermissions()) : List.of())
                .organizations(organizations)
                .delegations(delegations)
                .build();
    }

    private List<String> parsePermissionList(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            return PERMISSION_PARSER.readValue(raw, new TypeReference<List<String>>() {
            });
        } catch (Exception ex) {
            return List.of(raw);
        }
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