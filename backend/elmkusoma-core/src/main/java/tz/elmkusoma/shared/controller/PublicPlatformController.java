package tz.elmkusoma.shared.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.domain.Course;
import tz.elmkusoma.course.repository.CourseRepository;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public (unauthenticated) platform statistics and course discovery.
 * Backs the marketing homepage — all values are real database counts,
 * served under the already-permitted {@code /v1/public/**} prefix.
 * No PII, no institution-scoped data, no internal identifiers are exposed.
 */
@RestController
@RequestMapping("/v1/public")
@RequiredArgsConstructor
@Tag(name = "Public Platform", description = "Anonymous statistics and published course discovery")
public class PublicPlatformController {

    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final CourseRepository courseRepository;
    private final LiveClassRepository liveClassRepository;

    @GetMapping("/stats")
    @Operation(summary = "Public platform statistics (real counts, no PII)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        long learners = userRepository.countByRoleAndIsDeletedFalse(User.Role.STUDENT)
                + userRepository.countByRoleAndIsDeletedFalse(User.Role.LEARNER)
                + userRepository.countByRoleAndIsDeletedFalse(User.Role.OTHER_LEARNER);
        long instructors = userRepository.countByRoleAndIsDeletedFalse(User.Role.TEACHER);
        long institutions = institutionRepository.countByIsDeletedFalse();
        long courses = courseRepository.countByIsDeletedFalse();
        long liveClasses = liveClassRepository.countByIsDeletedFalse();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("learners", learners);
        stats.put("instructors", instructors);
        stats.put("institutions", institutions);
        stats.put("courses", courses);
        stats.put("liveClasses", liveClasses);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/courses")
    @Operation(summary = "Published courses for anonymous visitors (safe fields only)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCourses(
            @RequestParam(name = "limit", defaultValue = "6") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 12));
        List<Course> published = courseRepository.findAllPublishedAndIsDeletedFalse();

        // Prefer featured courses, then fill with the most recently listed ones.
        List<Course> ordered = new ArrayList<>();
        for (Course c : published) {
            if (Boolean.TRUE.equals(c.getIsFeatured())) ordered.add(c);
        }
        for (Course c : published) {
            if (!ordered.contains(c)) ordered.add(c);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Course c : ordered.subList(0, Math.min(safeLimit, ordered.size()))) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.getId());
            item.put("title", c.getTitle());
            item.put("description", c.getDescription());
            item.put("level", c.getLevel());
            item.put("category", c.getCategory());
            item.put("thumbnailUrl", c.getThumbnailUrl());
            result.add(item);
        }
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
