package tz.elmkusoma.learner.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.domain.*;
import tz.elmkusoma.course.repository.*;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.learning.domain.LessonProgress;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.repository.LessonProgressRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learner.domain.*;
import tz.elmkusoma.learner.dto.*;
import tz.elmkusoma.learner.repository.*;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/learner")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OTHER_LEARNER')")
@Tag(name = "General Learner", description = "General Learner / Participant workspace")
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
    private final UserRepository userRepository;

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
    @Operation(summary = "Browse all published courses across institutions")
    public ResponseEntity<ApiResponse<List<CourseSummaryResponse>>> browseCourses() {
        List<CourseSummaryResponse> courses = courseRepository.findAllPublishedAndIsDeletedFalse()
                .stream().map(this::toCourseSummaryResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(courses));
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
        LearnerEnrollment enrollment = LearnerEnrollment.builder()
                .userId(userId)
                .courseId(courseId)
                .institutionId(course.getInstitutionId())
                .enrolledAt(LocalDateTime.now())
                .progressPercentage(0.0)
                .build();
        enrollment = enrollmentRepository.save(enrollment);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Enrolled successfully", toEnrollmentResponse(enrollment)));
    }

    @GetMapping("/me/enrollments")
    @Operation(summary = "List my enrolled courses")
    public ResponseEntity<ApiResponse<List<EnrollmentResponse>>> myEnrollments(
            @RequestAttribute("userId") UUID userId) {
        List<EnrollmentResponse> enrollments = enrollmentRepository.findByUserIdAndIsDeletedFalseOrderByEnrolledAtDesc(userId)
                .stream().map(this::toEnrollmentResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(enrollments));
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
            e.setProgressPercentage(progress);
            if (progress >= 100.0) e.setCompletedAt(LocalDateTime.now());
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
    @Operation(summary = "Browse all resources across institutions")
    public ResponseEntity<ApiResponse<List<Resource>>> browseResources() {
        List<Resource> resources = resourceRepository.findAllAndIsDeletedFalse();
        return ResponseEntity.ok(ApiResponse.success(resources));
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
    @Operation(summary = "Browse all active live classes")
    public ResponseEntity<ApiResponse<List<LiveClass>>> browseLiveClasses() {
        List<LiveClass> classes = liveClassRepository.findAllActiveAndIsDeletedFalse();
        return ResponseEntity.ok(ApiResponse.success(classes));
    }

    @GetMapping("/live-classes/{id}")
    @Operation(summary = "Get live class detail")
    public ResponseEntity<ApiResponse<LiveClass>> getLiveClassDetail(@PathVariable UUID id) {
        return liveClassRepository.findById(id)
                .filter(lc -> !Boolean.TRUE.equals(lc.getIsDeleted()))
                .map(lc -> ResponseEntity.ok(ApiResponse.success(lc)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Live class not found")));
    }

    // ── Announcements ────────────────────────────────────────────────────

    @GetMapping("/announcements")
    @Operation(summary = "Browse all announcements")
    public ResponseEntity<ApiResponse<List<Announcement>>> browseAnnouncements() {
        List<Announcement> announcements = announcementRepository.findAllAndIsDeletedFalse();
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
        String targetType = body.get("targetType");
        UUID targetId = UUID.fromString(body.get("targetId"));
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
    }

    @DeleteMapping("/me/bookmarks/{id}")
    @Operation(summary = "Remove bookmark")
    public ResponseEntity<ApiResponse<String>> removeBookmark(
            @RequestAttribute("userId") UUID userId,
            @PathVariable UUID id) {
        return bookmarkRepository.findById(id)
                .filter(b -> b.getUserId().equals(userId))
                .map(b -> {
                    b.setIsDeleted(true);
                    bookmarkRepository.save(b);
                    return ResponseEntity.ok(ApiResponse.success("Bookmark removed", (String) null));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Bookmark not found")));
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
    @Operation(summary = "List my certificates")
    public ResponseEntity<ApiResponse<List<Certificate>>> myCertificates(
            @RequestAttribute("userId") UUID userId) {
        List<Certificate> certificates = certificateRepository.findAllByStudentId(userId);
        return ResponseEntity.ok(ApiResponse.success(certificates));
    }

    // ── Search ───────────────────────────────────────────────────────────

    @GetMapping("/search")
    @Operation(summary = "Search courses, resources, and live classes")
    public ResponseEntity<ApiResponse<SearchResultResponse>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "ALL") String type) {
        String query = q.toLowerCase();
        SearchResultResponse result;

        if ("COURSE".equalsIgnoreCase(type)) {
            result = SearchResultResponse.builder()
                    .courses(courseRepository.searchByTitleAndIsDeletedFalse(query)
                            .stream().map(this::toCourseSummaryResponse).collect(Collectors.toList()))
                    .resources(Collections.emptyList())
                    .liveClasses(Collections.emptyList())
                    .build();
        } else if ("RESOURCE".equalsIgnoreCase(type)) {
            result = SearchResultResponse.builder()
                    .courses(Collections.emptyList())
                    .resources(resourceRepository.searchByTitleAndIsDeletedFalse(query)
                            .stream().map(this::toResourceSearchResult).collect(Collectors.toList()))
                    .liveClasses(Collections.emptyList())
                    .build();
        } else if ("LIVE_CLASS".equalsIgnoreCase(type)) {
            result = SearchResultResponse.builder()
                    .courses(Collections.emptyList())
                    .resources(Collections.emptyList())
                    .liveClasses(liveClassRepository.searchByTitleAndIsDeletedFalse(query)
                            .stream().map(this::toLiveClassSearchResult).collect(Collectors.toList()))
                    .build();
        } else {
            result = SearchResultResponse.builder()
                    .courses(courseRepository.searchByTitleAndIsDeletedFalse(query)
                            .stream().map(this::toCourseSummaryResponse).collect(Collectors.toList()))
                    .resources(resourceRepository.searchByTitleAndIsDeletedFalse(query)
                            .stream().map(this::toResourceSearchResult).collect(Collectors.toList()))
                    .liveClasses(liveClassRepository.searchByTitleAndIsDeletedFalse(query)
                            .stream().map(this::toLiveClassSearchResult).collect(Collectors.toList()))
                    .build();
        }
        return ResponseEntity.ok(ApiResponse.success(result));
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
        return EnrollmentResponse.builder()
                .id(e.getId())
                .courseId(e.getCourseId())
                .courseTitle(course != null ? course.getTitle() : null)
                .courseDescription(course != null ? course.getDescription() : null)
                .courseThumbnailUrl(course != null ? course.getThumbnailUrl() : null)
                .courseLevel(course != null ? course.getLevel() : null)
                .courseCategory(course != null ? course.getCategory() : null)
                .enrolledAt(e.getEnrolledAt())
                .completedAt(e.getCompletedAt())
                .progressPercentage(e.getProgressPercentage())
                .build();
    }

    private BookmarkResponse toBookmarkResponse(Bookmark b) {
        return BookmarkResponse.builder()
                .id(b.getId())
                .targetType(b.getTargetType())
                .targetId(b.getTargetId())
                .createdAt(b.getCreatedAt())
                .build();
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
}
