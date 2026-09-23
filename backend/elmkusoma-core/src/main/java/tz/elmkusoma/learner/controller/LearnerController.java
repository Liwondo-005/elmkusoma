package tz.elmkusoma.learner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.domain.*;
import tz.elmkusoma.course.repository.*;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.course.dto.LiveClassResponse;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.Certificate.CertificateStatus;
import tz.elmkusoma.certificate.domain.Certificate.CertificateType;
import tz.elmkusoma.certificate.domain.CertificateTemplate;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.certificate.repository.CertificateTemplateRepository;
import tz.elmkusoma.learning.domain.LessonProgress;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.repository.LessonProgressRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learner.domain.*;
import tz.elmkusoma.learner.dto.*;
import tz.elmkusoma.learner.repository.*;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/learner")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OTHER_LEARNER', 'TEACHER', 'STUDENT', 'INSTITUTION_ADMIN', 'ADMIN')")
@Tag(name = "General Learner", description = "General Learner / Participant workspace")
@Slf4j
public class LearnerController {

    private final GeneralLearnerProfileRepository profileRepository;
    private final BookmarkRepository bookmarkRepository;
    private final LearnerNotificationRepository notificationRepository;
    private final LearnerEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseLessonRepository courseLessonRepository;
    private final ResourceRepository resourceRepository;
    private final LiveClassRepository liveClassRepository;
    private final AnnouncementRepository announcementRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateTemplateRepository certificateTemplateRepository;
    private final UserRepository userRepository;
    private final LiveClassParticipantRepository liveClassParticipantRepository;
    private final tz.elmkusoma.teacher.repository.TeacherRepository teacherRepository;
    private final tz.elmkusoma.academic.repository.SubjectRepository subjectRepository;

    // ── Profile ──────────────────────────────────────────────────────────

    @GetMapping("/me/profile")
    @Operation(summary = "Get or create learner profile")
    public ResponseEntity<ApiResponse<LearnerProfileResponse>> getProfile(
            @RequestAttribute("userId") UUID userId) {
        GeneralLearnerProfile profile = profileRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseGet(() -> {
                    GeneralLearnerProfile newProfile = GeneralLearnerProfile.builder()
                            .userId(userId)
                            .build();
                    return profileRepository.save(newProfile);
                });
        User user = userRepository.findById(userId).orElse(null);
        LearnerProfileResponse response = LearnerProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .interests(profile.getInterests())
                .bio(profile.getBio())
                .avatarUrl(profile.getAvatarUrl())
                .learningGoal(profile.getLearningGoal())
                .userFullName(user != null ? user.getFullName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .createdAt(profile.getCreatedAt())
                .build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/me/profile")
    @Operation(summary = "Update learner profile")
    public ResponseEntity<ApiResponse<LearnerProfileResponse>> updateProfile(
            @RequestAttribute("userId") UUID userId,
            @RequestBody Map<String, String> body) {
        GeneralLearnerProfile profile = profileRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseGet(() -> GeneralLearnerProfile.builder().userId(userId).build());
        if (body.containsKey("interests")) profile.setInterests(body.get("interests"));
        if (body.containsKey("bio")) profile.setBio(body.get("bio"));
        if (body.containsKey("avatarUrl")) profile.setAvatarUrl(body.get("avatarUrl"));
        if (body.containsKey("learningGoal")) profile.setLearningGoal(body.get("learningGoal"));
        profile = profileRepository.save(profile);
        User user = userRepository.findById(userId).orElse(null);
        LearnerProfileResponse response = LearnerProfileResponse.builder()
                .id(profile.getId())
                .userId(profile.getUserId())
                .interests(profile.getInterests())
                .bio(profile.getBio())
                .avatarUrl(profile.getAvatarUrl())
                .learningGoal(profile.getLearningGoal())
                .userFullName(user != null ? user.getFullName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .createdAt(profile.getCreatedAt())
                .build();
        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }

    // ── Dashboard ────────────────────────────────────────────────────────

    @GetMapping("/me/dashboard")
    @Operation(summary = "Get learner dashboard")
    public ResponseEntity<ApiResponse<LearnerDashboardResponse>> getDashboard(
            @RequestAttribute("userId") UUID userId) {
        long enrolled = enrollmentRepository.countByUserIdAndIsDeletedFalse(userId);
        long completed = enrollmentRepository.countByUserIdAndCompletedAtIsNotNullAndIsDeletedFalse(userId);

        List<LearnerEnrollment> allEnrollments = enrollmentRepository.findByUserIdAndIsDeletedFalseOrderByEnrolledAtDesc(userId);
        double overallProgress = allEnrollments.isEmpty() ? 0.0 :
                allEnrollments.stream().mapToDouble(e -> e.getProgressPercentage() != null ? e.getProgressPercentage() : 0.0).average().orElse(0.0);

        List<EnrollmentResponse> recentEnrollments = allEnrollments.stream().limit(5).map(this::toEnrollmentResponse).collect(Collectors.toList());
        List<EnrollmentResponse> continueLearning = allEnrollments.stream()
                .filter(e -> e.getProgressPercentage() != null && e.getProgressPercentage() > 0 && e.getProgressPercentage() < 100)
                .limit(5).map(this::toEnrollmentResponse).collect(Collectors.toList());

        List<Course> published = courseRepository.findAllPublishedAndIsDeletedFalse();
        Set<UUID> enrolledCourseIds = allEnrollments.stream().map(LearnerEnrollment::getCourseId).collect(Collectors.toSet());
        List<CourseSummaryResponse> recommended = published.stream()
                .filter(c -> !enrolledCourseIds.contains(c.getId()))
                .limit(6).map(this::toCourseSummaryResponse).collect(Collectors.toList());

        long unread = notificationRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(userId);

        LearnerDashboardResponse dashboard = LearnerDashboardResponse.builder()
                .enrolledCourses(enrolled)
                .completedCourses(completed)
                .overallProgress(overallProgress)
                .recentEnrollments(recentEnrollments)
                .continueLearning(continueLearning)
                .recommended(recommended)
                .unreadNotifications(unread)
                .build();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    // ── Courses ──────────────────────────────────────────────────────────

    @GetMapping("/courses")
    @Operation(summary = "Browse published courses for institution")
    public ResponseEntity<ApiResponse<Page<CourseSummaryResponse>>> browseCourses(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Course> coursePage = courseRepository.findByInstitutionIdAndStatusAndIsDeletedFalse(institutionId, "PUBLISHED", PageRequest.of(page, size));
        Page<CourseSummaryResponse> response = coursePage.map(this::toCourseSummaryResponse);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/courses/{id}")
    @Operation(summary = "Get course detail with modules")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseDetail(@PathVariable UUID id) {
        Course course = courseRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElse(null);
        if (course == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Course not found"));
        }
        List<CourseModule> modules = courseModuleRepository.findByCourseIdAndIsDeletedFalseOrderBySortOrder(id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("course", toCourseSummaryResponse(course));
        result.put("modules", modules.stream().map(m -> {
            Map<String, Object> mod = new LinkedHashMap<>();
            mod.put("id", m.getId());
            mod.put("title", m.getTitle());
            mod.put("description", m.getDescription());
            mod.put("sortOrder", m.getSortOrder());
            mod.put("lessonCount", courseLessonRepository.countByModuleIdAndIsDeletedFalse(m.getId()));
            return mod;
        }).collect(Collectors.toList()));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/courses/{courseId}/modules")
    @Operation(summary = "Get course modules with lesson counts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCourseModules(@PathVariable UUID courseId) {
        List<CourseModule> modules = courseModuleRepository.findByCourseIdAndIsDeletedFalseOrderBySortOrder(courseId);
        List<Map<String, Object>> result = modules.stream().map(m -> {
            Map<String, Object> mod = new LinkedHashMap<>();
            mod.put("id", m.getId());
            mod.put("title", m.getTitle());
            mod.put("description", m.getDescription());
            mod.put("sortOrder", m.getSortOrder());
            mod.put("lessonCount", courseLessonRepository.countByModuleIdAndIsDeletedFalse(m.getId()));
            return mod;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/courses/modules/{moduleId}/lessons")
    @Operation(summary = "Get module lessons")
    public ResponseEntity<ApiResponse<List<CourseLesson>>> getModuleLessons(@PathVariable UUID moduleId) {
        List<CourseLesson> lessons = courseLessonRepository.findByModuleIdAndIsDeletedFalseOrderBySortOrder(moduleId);
        return ResponseEntity.ok(ApiResponse.success(lessons));
    }

    // ── Enrollments ──────────────────────────────────────────────────────

    @PostMapping("/me/enrollments")
    @Operation(summary = "Enroll in a course")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> enroll(
            @RequestAttribute("userId") UUID userId,
            @RequestBody Map<String, UUID> body) {
        UUID courseId = body.get("courseId");
        if (courseId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("courseId is required"));
        }
        Course course = courseRepository.findById(courseId)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElse(null);
        if (course == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Course not found"));
        }
        Optional<LearnerEnrollment> existing = enrollmentRepository.findByUserIdAndCourseIdAndIsDeletedFalse(userId, courseId);
        if (existing.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success("Already enrolled", toEnrollmentResponse(existing.get())));
        }
        LearnerEnrollment enrollment = new LearnerEnrollment();
        enrollment.setUserId(userId);
        enrollment.setCourseId(courseId);
        enrollment.setInstitutionId(course.getInstitutionId());
        enrollment.setEnrolledAt(LocalDateTime.now());
        enrollment.setProgressPercentage(0.0);
        enrollment = enrollmentRepository.save(enrollment);

        LearnerNotification notification = LearnerNotification.builder()
                .userId(userId)
                .title("Enrolled in " + course.getTitle())
                .message("You have successfully enrolled in " + course.getTitle() + ". Start learning now!")
                .notificationType("ENROLLMENT")
                .targetType("course")
                .targetId(courseId)
                .build();
        notificationRepository.save(notification);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Enrolled successfully", toEnrollmentResponse(enrollment)));
    }

    @GetMapping("/me/enrollments")
    @Operation(summary = "List my enrolled courses")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> myEnrollments(
            @RequestAttribute("userId") UUID userId) {
        List<LearnerEnrollment> enrollments = enrollmentRepository.findByUserIdAndIsDeletedFalseOrderByEnrolledAtDesc(userId);
        enrollments.sort((a, b) -> {
            if (a.getLastAccessedAt() == null && b.getLastAccessedAt() == null) return b.getEnrolledAt().compareTo(a.getEnrolledAt());
            if (a.getLastAccessedAt() == null) return 1;
            if (b.getLastAccessedAt() == null) return -1;
            return b.getLastAccessedAt().compareTo(a.getLastAccessedAt());
        });
        List<EnrollmentResponse> response = enrollments.stream().map(this::toEnrollmentResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/me/enrollments/{courseId}")
    @Operation(summary = "Get enrollment for specific course")
    public ResponseEntity<ApiResponse<EnrollmentResponse>> myEnrollmentForCourse(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID courseId) {
        return enrollmentRepository.findByUserIdAndCourseIdAndIsDeletedFalse(userId, courseId)
                .map(e -> ResponseEntity.ok(ApiResponse.success(toEnrollmentResponse(e))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Not enrolled")));
    }

    // ── Progress ─────────────────────────────────────────────────────────

    @GetMapping("/me/progress/{courseId}")
    @Operation(summary = "Get course progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCourseProgress(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID courseId) {
        enrollmentRepository.findByUserIdAndCourseIdAndIsDeletedFalse(userId, courseId).ifPresent(e -> {
            e.setLastAccessedAt(LocalDateTime.now());
            enrollmentRepository.save(e);
        });
        long totalLessons = courseLessonRepository.countByCourseIdAndIsDeletedFalse(courseId);
        // Use userId as studentId for OTHER_LEARNER lesson progress tracking
        long completedLessons = lessonProgressRepository.findByStudentIdAndIsDeletedFalse(userId).stream()
                .filter(lp -> lp.getCompletionPercentage() != null && lp.getCompletionPercentage() >= 100.0)
                .count();
        // Filter to only lessons belonging to this course
        List<CourseModule> modules = courseModuleRepository.findByCourseIdAndIsDeletedFalseOrderBySortOrder(courseId);
        Set<UUID> moduleIds = modules.stream().map(CourseModule::getId).collect(Collectors.toSet());
        List<CourseLesson> allLessons = new ArrayList<>();
        for (UUID moduleId : moduleIds) {
            allLessons.addAll(courseLessonRepository.findByModuleIdAndIsDeletedFalseOrderBySortOrder(moduleId));
        }
        Set<UUID> lessonIds = allLessons.stream().map(CourseLesson::getId).collect(Collectors.toSet());
        long total = allLessons.size();
        long completed = lessonProgressRepository.findByStudentIdAndIsDeletedFalse(userId).stream()
                .filter(lp -> lessonIds.contains(lp.getLessonId()))
                .filter(lp -> lp.getCompletionPercentage() != null && lp.getCompletionPercentage() >= 100.0)
                .count();
        double progress = total == 0 ? 0.0 : (completed * 100.0 / total);

        // Update enrollment progress
        enrollmentRepository.findByUserIdAndCourseIdAndIsDeletedFalse(userId, courseId).ifPresent(e -> {
            boolean wasIncomplete = e.getProgressPercentage() == null || e.getProgressPercentage() < 100.0;
            e.setProgressPercentage(progress);
            if (progress >= 100.0 && wasIncomplete) {
                e.setCompletedAt(LocalDateTime.now());
                Course course = courseRepository.findById(courseId).orElse(null);

                LearnerNotification completionNotification = LearnerNotification.builder()
                        .userId(userId)
                        .title("Course Completed!")
                        .message("Congratulations! You have completed " + (course != null ? course.getTitle() : "the course") + ". Check your certificates.")
                        .notificationType("COURSE_COMPLETION")
                        .targetType("course")
                        .targetId(courseId)
                        .build();
                notificationRepository.save(completionNotification);

                try {
                    List<CertificateTemplate> templates = certificateTemplateRepository.findAllByInstitutionId(course != null ? course.getInstitutionId() : null);
                    if (templates != null && !templates.isEmpty()) {
                        CertificateTemplate template = templates.get(0);
                        User user = userRepository.findById(userId).orElse(null);
                        String studentName = user != null ? user.getFullName() : "Student";
                        String serialNumber = "CERT-" + System.currentTimeMillis() + "-" + userId.toString().substring(0, 8);
                        String verificationCode = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

                        Certificate certificate = Certificate.builder()
                                .templateId(template.getId())
                                .studentId(userId)
                                .issuedBy(userId)
                                .serialNumber(serialNumber)
                                .certificateType(CertificateType.COMPLETION)
                                .title("Certificate of Completion - " + (course != null ? course.getTitle() : ""))
                                .description("Awarded for successfully completing " + (course != null ? course.getTitle() : "the course"))
                                .studentName(studentName)
                                .courseOrProgramme(course != null ? course.getTitle() : null)
                                .completionDate(LocalDate.now())
                                .issueDate(LocalDateTime.now())
                                .status(CertificateStatus.ISSUED)
                                .verificationCode(verificationCode)
                                .build();
                        if (course != null) certificate.setInstitutionId(course.getInstitutionId());
                        certificateRepository.save(certificate);

                        LearnerNotification certNotification = LearnerNotification.builder()
                                .userId(userId)
                                .title("Certificate Issued!")
                                .message("Your certificate for " + (course != null ? course.getTitle() : "the course") + " is ready. Verification code: " + verificationCode)
                                .notificationType("CERTIFICATE")
                                .targetType("certificate")
                                .targetId(certificate.getId())
                                .build();
                        notificationRepository.save(certNotification);
                    }
                } catch (Exception ex) {
                    log.warn("Failed to auto-issue certificate for user {} course {}: {}", userId, courseId, ex.getMessage());
                }
            }
            enrollmentRepository.save(e);
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalLessons", total);
        result.put("completedLessons", completed);
        result.put("progressPercentage", progress);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/me/progress")
    @Operation(summary = "Update lesson progress")
    public ResponseEntity<ApiResponse<String>> updateProgress(
            @RequestAttribute("userId") UUID userId,
            @RequestBody Map<String, Object> body) {
        UUID lessonId = UUID.fromString((String) body.get("lessonId"));
        Double percentage = body.get("completionPercentage") != null ?
                Double.parseDouble(body.get("completionPercentage").toString()) : 0.0;

        LessonProgress progress = lessonProgressRepository.findByLessonIdAndStudentIdAndIsDeletedFalse(lessonId, userId)
                .orElseGet(() -> LessonProgress.builder()
                        .lessonId(lessonId)
                        .studentId(userId)
                        .startedAt(LocalDateTime.now())
                        .build());
        progress.setCompletionPercentage(percentage);
        if (percentage >= 100.0 && progress.getCompletedAt() == null) {
            progress.setCompletedAt(LocalDateTime.now());
        }
        lessonProgressRepository.save(progress);
        return ResponseEntity.ok(ApiResponse.success("Progress updated", null));
    }

    // ── Resources ────────────────────────────────────────────────────────

    @GetMapping("/resources")
    @Operation(summary = "Browse all resources with optional type filter")
    public ResponseEntity<ApiResponse<Page<Resource>>> browseResources(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            Page<Resource> resources;
            if (type != null && !type.isEmpty() && !"all".equalsIgnoreCase(type)) {
                try {
                    Resource.ResourceType resourceType = Resource.ResourceType.valueOf(type.toUpperCase());
                    List<Resource> filtered = resourceRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                            .stream()
                            .filter(r -> r.getResourceType() == resourceType)
                            .collect(Collectors.toList());
                    List<Resource> paged = filtered.stream().skip((long) page * size).limit(size).collect(Collectors.toList());
                    resources = new org.springframework.data.domain.PageImpl<>(paged, PageRequest.of(page, size), filtered.size());
                } catch (IllegalArgumentException e) {
                    resources = resourceRepository.findByInstitutionIdAndIsDeletedFalse(institutionId, PageRequest.of(page, size));
                }
            } else {
                resources = resourceRepository.findByInstitutionIdAndIsDeletedFalse(institutionId, PageRequest.of(page, size));
            }
            return ResponseEntity.ok(ApiResponse.success(resources));
        } catch (Exception ex) {
            log.error("Failed to browse resources: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to browse resources"));
        }
    }

    @GetMapping("/resources/{id}")
    @Operation(summary = "Get resource detail")
    public ResponseEntity<ApiResponse<Resource>> getResourceDetail(@PathVariable UUID id) {
        return resourceRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Resource not found")));
    }

    // ── Live Classes ─────────────────────────────────────────────────────

    @GetMapping("/live-classes")
    @Operation(summary = "Browse all active live classes in user's institution")
    public ResponseEntity<ApiResponse<List<LiveClassResponse>>> browseLiveClasses(
            @RequestAttribute("userId") UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found"));
        }
        if (user.getInstitutionId() == null) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        List<LiveClassResponse> classes = liveClassRepository.findByInstitutionIdAndIsDeletedFalse(user.getInstitutionId())
                .stream()
                .filter(lc -> !"CANCELLED".equals(lc.getStatus()))
                .map(this::toLiveClassResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(classes));
    }

    @GetMapping("/live-classes/{id}")
    @Operation(summary = "Get live class detail")
    public ResponseEntity<ApiResponse<LiveClassResponse>> getLiveClassDetail(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getInstitutionId() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found"));
        }
        return liveClassRepository.findById(id)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .filter(lc -> user.getInstitutionId().equals(lc.getInstitutionId()))
                .map(lc -> ResponseEntity.ok(ApiResponse.success(toLiveClassResponse(lc))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Live class not found")));
    }

    @PostMapping("/live-classes/{id}/join")
    @Operation(summary = "Record joining a live class")
    public ResponseEntity<ApiResponse<Map<String, Object>>> joinLiveClass(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId) {
        LiveClass liveClass = liveClassRepository.findById(id)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Live class not found"));
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("User not found"));
        }
        LiveClassParticipant existing = liveClassParticipantRepository
                .findByLiveClassIdAndUserIdAndIsDeletedFalse(id, userId).orElse(null);
        if (existing == null) {
            LiveClassParticipant participant = LiveClassParticipant.builder()
                    .liveClassId(id)
                    .userId(userId)
                    .role("STUDENT")
                    .joinedAt(LocalDateTime.now())
                    .build();
            liveClassParticipantRepository.save(participant);
        } else if (existing.getLeftAt() != null) {
            existing.setLeftAt(null);
            existing.setJoinedAt(LocalDateTime.now());
            liveClassParticipantRepository.save(existing);
        }
        long totalJoined = liveClassParticipantRepository.countByLiveClassIdAndIsDeletedFalse(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "joined",
                "totalParticipants", totalJoined
        )));
    }

    @PostMapping("/live-classes/{id}/leave")
    @Operation(summary = "Record leaving a live class")
    public ResponseEntity<ApiResponse<Void>> leaveLiveClass(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID userId) {
        LiveClassParticipant participant = liveClassParticipantRepository
                .findByLiveClassIdAndUserIdAndIsDeletedFalse(id, userId).orElse(null);
        if (participant != null) {
            participant.setLeftAt(LocalDateTime.now());
            liveClassParticipantRepository.save(participant);
        }
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/live-classes/{id}/participants")
    @Operation(summary = "Get participants for a live class")
    public ResponseEntity<ApiResponse<List<LiveClassParticipant>>> getLiveClassParticipants(@PathVariable UUID id) {
        List<LiveClassParticipant> participants = liveClassParticipantRepository.findByLiveClassIdAndIsDeletedFalse(id);
        return ResponseEntity.ok(ApiResponse.success(participants));
    }

    // ── Announcements ────────────────────────────────────────────────────

    @GetMapping("/announcements")
    @Operation(summary = "Browse all announcements for institution")
    public ResponseEntity<ApiResponse<List<Announcement>>> browseAnnouncements(
            @RequestAttribute("institutionId") UUID institutionId) {
        List<Announcement> announcements = announcementRepository.findByInstitutionIdAndIsDeletedFalse(institutionId);
        return ResponseEntity.ok(ApiResponse.success(announcements));
    }

    // ── Bookmarks ────────────────────────────────────────────────────────

    @GetMapping("/me/bookmarks")
    @Operation(summary = "List bookmarks")
    public ResponseEntity<ApiResponse<List<BookmarkResponse>>> myBookmarks(
            @RequestAttribute("userId") UUID userId) {
        List<BookmarkResponse> bookmarks = bookmarkRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toBookmarkResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(bookmarks));
    }

    @PostMapping("/me/bookmarks")
    @Operation(summary = "Create bookmark")
    public ResponseEntity<ApiResponse<BookmarkResponse>> createBookmark(
            @RequestAttribute("userId") UUID userId,
            @RequestBody Map<String, String> body) {
        try {
            String targetType = body.get("targetType");
            String targetIdStr = body.get("targetId");
            if (targetType == null || targetType.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("targetType is required"));
            }
            if (targetIdStr == null || targetIdStr.isBlank()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("targetId is required"));
            }
            UUID targetId = UUID.fromString(targetIdStr);
            if (bookmarkRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsDeletedFalse(userId, targetType, targetId)) {
                return ResponseEntity.ok(ApiResponse.success("Already bookmarked", null));
            }
            Bookmark bookmark = Bookmark.builder()
                    .userId(userId)
                    .targetType(targetType)
                    .targetId(targetId)
                    .build();
            bookmark = bookmarkRepository.save(bookmark);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Bookmarked", toBookmarkResponse(bookmark)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid targetId format"));
        } catch (Exception ex) {
            log.error("Failed to create bookmark: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create bookmark"));
        }
    }

    @DeleteMapping("/me/bookmarks/{id}")
    @Operation(summary = "Remove bookmark")
    public ResponseEntity<ApiResponse<String>> removeBookmark(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        try {
            return bookmarkRepository.findById(id)
                    .filter(b -> b.getUserId().equals(userId))
                    .map(b -> {
                        b.setIsDeleted(true);
                        bookmarkRepository.save(b);
                        return ResponseEntity.ok(ApiResponse.success("Bookmark removed", (String) null));
                    })
                    .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Bookmark not found")));
        } catch (Exception ex) {
            log.error("Failed to remove bookmark {}: {}", id, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to remove bookmark"));
        }
    }

    @GetMapping("/me/bookmarks/check")
    @Operation(summary = "Check if bookmarked")
    public ResponseEntity<ApiResponse<Boolean>> checkBookmark(
            @RequestAttribute("userId") UUID userId,
            @RequestParam String targetType,
            @RequestParam UUID targetId) {
        boolean exists = bookmarkRepository.existsByUserIdAndTargetTypeAndTargetIdAndIsDeletedFalse(userId, targetType, targetId);
        return ResponseEntity.ok(ApiResponse.success(exists));
    }

    // ── Notifications ────────────────────────────────────────────────────

    @GetMapping("/me/notifications")
    @Operation(summary = "List notifications")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> myNotifications(
            @RequestAttribute("userId") UUID userId) {
        List<NotificationResponse> notifications = notificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                .stream().map(this::toNotificationResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/me/notifications/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<Long>> unreadCount(
            @RequestAttribute("userId") UUID userId) {
        long count = notificationRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PutMapping("/me/notifications/{id}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<ApiResponse<String>> markAsRead(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        return notificationRepository.findById(id)
                .filter(n -> n.getUserId().equals(userId))
                .map(n -> {
                    n.setIsRead(true);
                    notificationRepository.save(n);
                    return ResponseEntity.ok(ApiResponse.success("Marked as read", (String) null));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Notification not found")));
    }

    @PutMapping("/me/notifications/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<String>> markAllAsRead(
            @RequestAttribute("userId") UUID userId) {
        List<LearnerNotification> unread = notificationRepository.findByUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
        return ResponseEntity.ok(ApiResponse.success("All marked as read", null));
    }

    // ── Certificates ─────────────────────────────────────────────────────

    @GetMapping("/me/certificates")
    @Operation(summary = "List my issued certificates")
    public ResponseEntity<ApiResponse<List<Certificate>>> myCertificates(
            @RequestAttribute("userId") UUID userId) {
        List<Certificate> certificates = certificateRepository.findIssuedByStudentId(userId);
        return ResponseEntity.ok(ApiResponse.success(certificates));
    }

    @GetMapping("/me/certificates/{certificateId}")
    @Operation(summary = "Get my certificate detail")
    public ResponseEntity<ApiResponse<Certificate>> myCertificateDetail(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID certificateId) {
        return certificateRepository.findById(certificateId)
                .filter(c -> c.getStudentId().equals(userId) && !Boolean.TRUE.equals(c.getIsDeleted()))
                .map(c -> ResponseEntity.ok(ApiResponse.success(c)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Certificate not found")));
    }

    // ── Search ───────────────────────────────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Search courses, resources, live classes, and announcements with filters")
    public ResponseEntity<ApiResponse<SearchResultResponse>> search(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestParam String q,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) UUID provider,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            String query = q.toLowerCase();
            LocalDateTime fromDate = parseDate(dateFrom);
            LocalDateTime toDate = parseDate(dateTo) != null ? parseDate(dateTo).plusDays(1) : null;
            SearchResultResponse result;

            if ("COURSE".equalsIgnoreCase(type)) {
                List<Course> filteredCourses = courseRepository.searchPublishedByInstitutionWithAllFilters(institutionId, query, level, category, fromDate, toDate)
                        .stream().limit(size).skip((long) page * size).collect(Collectors.toList());
                result = SearchResultResponse.builder()
                        .courses(filteredCourses.stream().map(this::toCourseSummaryResponse).collect(Collectors.toList()))
                        .resources(Collections.emptyList())
                        .liveClasses(Collections.emptyList())
                        .announcements(Collections.emptyList())
                        .build();
            } else if ("RESOURCE".equalsIgnoreCase(type)) {
                List<Resource> resources = resourceRepository.searchByInstitutionIdAndIsDeletedFalse(institutionId, query)
                        .stream().limit(size).skip((long) page * size).collect(Collectors.toList());
                result = SearchResultResponse.builder()
                        .courses(Collections.emptyList())
                        .resources(resources.stream().map(this::toResourceSearchResult).collect(Collectors.toList()))
                        .liveClasses(Collections.emptyList())
                        .announcements(Collections.emptyList())
                        .build();
            } else if ("LIVE_CLASS".equalsIgnoreCase(type)) {
                result = SearchResultResponse.builder()
                        .courses(Collections.emptyList())
                        .resources(Collections.emptyList())
                        .liveClasses(liveClassRepository.searchByInstitutionIdAndQuery(institutionId, query)
                                .stream().limit(size).skip((long) page * size).map(this::toLiveClassSearchResult).collect(Collectors.toList()))
                        .announcements(Collections.emptyList())
                        .build();
            } else if ("ANNOUNCEMENT".equalsIgnoreCase(type)) {
                result = SearchResultResponse.builder()
                        .courses(Collections.emptyList())
                        .resources(Collections.emptyList())
                        .liveClasses(Collections.emptyList())
                        .announcements(announcementRepository.searchByInstitutionIdAndQuery(institutionId, query)
                                .stream().limit(size).skip((long) page * size).map(this::toAnnouncementSearchResult).collect(Collectors.toList()))
                        .build();
            } else {
                List<Course> filteredCourses = courseRepository.searchPublishedByInstitutionWithAllFilters(institutionId, query, level, category, fromDate, toDate)
                        .stream().limit(size).skip((long) page * size).collect(Collectors.toList());
                result = SearchResultResponse.builder()
                        .courses(filteredCourses.stream().map(this::toCourseSummaryResponse).collect(Collectors.toList()))
                        .resources(resourceRepository.searchByInstitutionIdAndIsDeletedFalse(institutionId, query)
                                .stream().limit(size).skip((long) page * size).map(this::toResourceSearchResult).collect(Collectors.toList()))
                        .liveClasses(liveClassRepository.searchByInstitutionIdAndQuery(institutionId, query)
                                .stream().limit(size).skip((long) page * size).map(this::toLiveClassSearchResult).collect(Collectors.toList()))
                        .announcements(announcementRepository.searchByInstitutionIdAndQuery(institutionId, query)
                                .stream().limit(size).skip((long) page * size).map(this::toAnnouncementSearchResult).collect(Collectors.toList()))
                        .build();
            }

            if ("oldest".equalsIgnoreCase(sort)) {
                if (result.getCourses() != null) Collections.reverse(result.getCourses());
                if (result.getResources() != null) Collections.reverse(result.getResources());
                if (result.getAnnouncements() != null) Collections.reverse(result.getAnnouncements());
            } else if ("az".equalsIgnoreCase(sort)) {
                if (result.getCourses() != null) result.getCourses().sort(Comparator.comparing(CourseSummaryResponse::getTitle));
                if (result.getResources() != null) result.getResources().sort(Comparator.comparing(ResourceSearchResult::getTitle));
                if (result.getAnnouncements() != null) result.getAnnouncements().sort(Comparator.comparing(AnnouncementSearchResult::getTitle));
            }

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception ex) {
            log.error("Search failed for query '{}': {}", q, ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Search failed"));
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    // ── Related Content ──────────────────────────────────────────────────

    @GetMapping("/courses/{id}/related")
    @Operation(summary = "Get related courses based on level and category")
    public ResponseEntity<ApiResponse<List<CourseSummaryResponse>>> getRelatedCourses(@PathVariable UUID id) {
        Course course = courseRepository.findById(id)
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .orElse(null);
        if (course == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Course not found"));
        }
        List<Course> related = courseRepository.findRelatedPublishedCourses(
                id,
                course.getTitle() != null ? course.getTitle().substring(0, Math.min(3, course.getTitle().length())) : "",
                course.getLevel(),
                course.getCategory()
        );
        List<CourseSummaryResponse> response = related.stream()
                .limit(6)
                .map(this::toCourseSummaryResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/resources/{id}/related")
    @Operation(summary = "Get related resources based on subject")
    public ResponseEntity<ApiResponse<List<ResourceSearchResult>>> getRelatedResources(@PathVariable UUID id) {
        Resource resource = resourceRepository.findById(id)
                .filter(r -> !Boolean.TRUE.equals(r.getIsDeleted()))
                .orElse(null);
        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Resource not found"));
        }
        String query = resource.getTitle() != null ? resource.getTitle().substring(0, Math.min(3, resource.getTitle().length())) : "";
        List<Resource> related = resourceRepository.findRelatedResources(id, resource.getSubjectId(), query);
        List<ResourceSearchResult> response = related.stream()
                .limit(6)
                .map(this::toResourceSearchResult)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/live-classes/{id}/related")
    @Operation(summary = "Get related live classes based on subject")
    public ResponseEntity<ApiResponse<List<LiveClassSearchResult>>> getRelatedLiveClasses(@PathVariable UUID id) {
        LiveClass liveClass = liveClassRepository.findById(id)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .orElse(null);
        if (liveClass == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Live class not found"));
        }
        String query = liveClass.getTitle() != null ? liveClass.getTitle().substring(0, Math.min(3, liveClass.getTitle().length())) : "";
        List<LiveClass> related = liveClassRepository.findRelatedLiveClasses(id, liveClass.getSubjectId(), query);
        List<LiveClassSearchResult> response = related.stream()
                .limit(6)
                .map(this::toLiveClassSearchResult)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Goals ────────────────────────────────────────────────────────

    @GetMapping("/me/goals")
    @Operation(summary = "List learning goals")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>>
        getGoals(@RequestAttribute("userId") UUID userId) {
        return ResponseEntity.ok(ApiResponse.success(List.of()));
    }

    @PostMapping("/me/goals")
    @Operation(summary = "Create a learning goal")
    public ResponseEntity<ApiResponse<Map<String, Object>>>
        createGoal(@RequestAttribute("userId") UUID userId, @RequestBody Map<String, Object> body) {
        Map<String, Object> goal = new java.util.HashMap<>(body);
        goal.put("id", UUID.randomUUID().toString());
        goal.put("userId", userId.toString());
        return ResponseEntity.status(201).body(ApiResponse.success(goal));
    }

    // ── Mapping Helpers ──────────────────────────────────────────────────

    private CourseSummaryResponse toCourseSummaryResponse(Course c) {
        return CourseSummaryResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .description(c.getDescription())
                .thumbnailUrl(c.getThumbnailUrl())
                .level(c.getLevel())
                .category(c.getCategory())
                .isPublished(c.getIsPublished())
                .isFeatured(c.getIsFeatured())
                .moduleCount(courseModuleRepository.countByCourseIdAndIsDeletedFalse(c.getId()))
                .lessonCount(courseLessonRepository.countByCourseIdAndIsDeletedFalse(c.getId()))
                .build();
    }

    private EnrollmentResponse toEnrollmentResponse(LearnerEnrollment e) {
        Course course = courseRepository.findById(e.getCourseId()).orElse(null);
        EnrollmentResponse response = new EnrollmentResponse();
        response.setId(e.getId());
        response.setCourseId(e.getCourseId());
        response.setCourseTitle(course != null ? course.getTitle() : null);
        response.setCourseDescription(course != null ? course.getDescription() : null);
        response.setCourseThumbnailUrl(course != null ? course.getThumbnailUrl() : null);
        response.setCourseLevel(course != null ? course.getLevel() : null);
        response.setCourseCategory(course != null ? course.getCategory() : null);
        response.setEnrolledAt(e.getEnrolledAt());
        response.setCompletedAt(e.getCompletedAt());
        response.setProgressPercentage(e.getProgressPercentage());
        response.setLastAccessedAt(e.getLastAccessedAt());
        return response;
    }

    private BookmarkResponse toBookmarkResponse(Bookmark b) {
        String targetTitle = resolveTargetTitle(b.getTargetType(), b.getTargetId());
        return BookmarkResponse.builder()
                .id(b.getId())
                .targetType(b.getTargetType())
                .targetId(b.getTargetId())
                .targetTitle(targetTitle)
                .targetAvailable(targetTitle != null)
                .createdAt(b.getCreatedAt())
                .build();
    }

    private String resolveTargetTitle(String targetType, UUID targetId) {
        if (targetType == null || targetId == null) return null;
        try {
            switch (targetType.toLowerCase()) {
                case "course":
                    return courseRepository.findById(targetId).map(Course::getTitle).orElse(null);
                case "resource":
                    return resourceRepository.findById(targetId).map(Resource::getTitle).orElse(null);
                case "liveclass":
                case "live_class":
                    return liveClassRepository.findById(targetId).map(LiveClass::getTitle).orElse(null);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    private NotificationResponse toNotificationResponse(LearnerNotification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .message(n.getMessage())
                .notificationType(n.getNotificationType())
                .targetType(n.getTargetType())
                .targetId(n.getTargetId())
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private ResourceSearchResult toResourceSearchResult(Resource r) {
        return ResourceSearchResult.builder()
                .id(r.getId())
                .title(r.getTitle())
                .description(r.getDescription())
                .resourceType(r.getResourceType().name())
                .fileUrl(r.getFileUrl())
                .build();
    }

    private LiveClassSearchResult toLiveClassSearchResult(LiveClass lc) {
        return LiveClassSearchResult.builder()
                .id(lc.getId())
                .title(lc.getTitle())
                .description(lc.getDescription())
                .status(lc.getStatus())
                .scheduledAt(lc.getScheduledAt())
                .durationMinutes(lc.getDurationMinutes())
                .build();
    }

    private LiveClassResponse toLiveClassResponse(LiveClass lc) {
        String teacherName = null;
        if (lc.getTeacherId() != null) {
            teacherName = teacherRepository.findById(lc.getTeacherId())
                    .flatMap(t -> userRepository.findById(t.getUserId()))
                    .map(User::getFullName)
                    .orElse(null);
        }
        String subjectName = null;
        if (lc.getSubjectId() != null) {
            subjectName = subjectRepository.findById(lc.getSubjectId())
                    .map(s -> s.getName())
                    .orElse(null);
        }
        return LiveClassResponse.builder()
                .id(lc.getId())
                .title(lc.getTitle())
                .description(lc.getDescription())
                .scheduledAt(lc.getScheduledAt() != null ? lc.getScheduledAt().toString() : null)
                .durationMinutes(lc.getDurationMinutes())
                .status(lc.getStatus())
                .maxParticipants(lc.getMaxParticipants())
                .teacherId(lc.getTeacherId())
                .subjectId(lc.getSubjectId())
                .teacherName(teacherName)
                .subjectName(subjectName)
                .recordingUrl(lc.getRecordingUrl())
                .recordingEnabled(Boolean.TRUE.equals(lc.getRecordingEnabled()))
                .sessionType(lc.getSessionType() != null ? lc.getSessionType().name() : "LECTURE")
                .canJoin("IN_PROGRESS".equals(lc.getStatus()) || "LIVE".equals(lc.getStatus()))
                .createdAt(lc.getCreatedAt() != null ? lc.getCreatedAt().toString() : null)
                .build();
    }

    private AnnouncementSearchResult toAnnouncementSearchResult(Announcement a) {
        return AnnouncementSearchResult.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .priority(a.getPriority())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
