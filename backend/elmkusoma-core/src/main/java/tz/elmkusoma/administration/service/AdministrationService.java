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
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

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

        CustomRole role = CustomRole.builder()
                .name(request.getName())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .isSystemRole(false)
                .isActive(true)
                .build();
        role.setInstitutionId(institutionId);

        roleRepository.save(role);

        // Save permissions
        if (request.getPermissions() != null && !request.getPermissions().isEmpty()) {
            for (String permission : request.getPermissions()) {
                RolePermission rolePermission = RolePermission.builder()
                        .roleId(role.getId())
                        .permission(permission)
                        .build();
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
            return DashboardResponse.builder()
                    .institutionId(institutionId)
                    .totalStudents(data.get("totalStudents") != null ? ((Number) data.get("totalStudents")).longValue() : 0L)
                    .totalTeachers(data.get("totalTeachers") != null ? ((Number) data.get("totalTeachers")).longValue() : 0L)
                    .totalParents(data.get("totalParents") != null ? ((Number) data.get("totalParents")).longValue() : 0L)
                    .activeStudents(data.get("activeStudents") != null ? ((Number) data.get("activeStudents")).longValue() : 0L)
                    .certificatesIssued(data.get("certificatesIssued") != null ? ((Number) data.get("certificatesIssued")).longValue() : 0L)
                    .pendingImportJobs(data.get("pendingImportJobs") != null ? ((Number) data.get("pendingImportJobs")).longValue() : 0L)
                    .totalCourses(data.get("totalCourses") != null ? ((Number) data.get("totalCourses")).longValue() : 0L)
                    .publishedCourses(data.get("publishedCourses") != null ? ((Number) data.get("publishedCourses")).longValue() : 0L)
                    .draftCourses(data.get("draftCourses") != null ? ((Number) data.get("draftCourses")).longValue() : 0L)
                    .totalModules(data.get("totalModules") != null ? ((Number) data.get("totalModules")).longValue() : 0L)
                    .totalLessons(data.get("totalLessons") != null ? ((Number) data.get("totalLessons")).longValue() : 0L)
                    .liveClassesScheduled(data.get("liveClassesScheduled") != null ? ((Number) data.get("liveClassesScheduled")).longValue() : 0L)
                    .build();
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

        DashboardResponse response = DashboardResponse.builder()
                .institutionId(institutionId)
                .totalStudents(totalStudents)
                .totalTeachers(totalTeachers)
                .totalParents(totalParents)
                .activeStudents(activeStudents)
                .certificatesIssued(0L)
                .pendingImportJobs(pendingJobs)
                .totalCourses(totalCourses)
                .publishedCourses(publishedCourses)
                .draftCourses(draftCourses)
                .totalModules(totalModules)
                .totalLessons(totalLessons)
                .liveClassesScheduled(0L)
                .build();

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

        DashboardSnapshot snapshot = DashboardSnapshot.builder()
                .institutionId(institutionId)
                .snapshotType("overview")
                .snapshotData(snapshotData)
                .generatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        snapshotRepository.save(snapshot);

        return response;
    }

    // ── Data Import ──

    public ImportJobResponse createImportJob(String importType, String fileName, UUID institutionId,
                                              UUID userId, String userEmail, String userRole) {
        DataImportJob job = DataImportJob.builder()
                .importedBy(userId)
                .importType(importType)
                .fileName(fileName)
                .status(DataImportJob.ImportStatus.PENDING)
                .build();
        job.setInstitutionId(institutionId);

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
}
