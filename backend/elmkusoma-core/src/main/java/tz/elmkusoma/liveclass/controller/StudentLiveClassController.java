package tz.elmkusoma.liveclass.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
// Both prefixes are exposed: the frontend rewrites /api/v1/* -> /v1/* while some
// clients call the /api/v1/* form directly against the backend base URL.
@RequestMapping({"/api/v1/student/live-classes", "/v1/student/live-classes"})
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER')")
public class StudentLiveClassController {

    private final LiveClassRepository liveClassRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMyLiveClasses(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID institutionId = getInstitutionId(request);
        if (institutionId == null) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        LocalDate today = LocalDate.now();
        List<LiveClass> classes = liveClassRepository
                .findByInstitutionIdAndScheduledAtBetween(
                        institutionId,
                        today.minusDays(7).atStartOfDay(),
                        today.plusDays(7).atTime(LocalTime.MAX));

        // Surface running classes first, then scheduled ones, so the paginated
        // window below can never push an IN_PROGRESS class off the first page.
        classes.sort(Comparator
                .comparingInt((LiveClass lc) -> statusPriority(lc.getStatus()))
                .thenComparing(LiveClass::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder())));

        List<Map<String, Object>> result = classes.stream()
                .limit((long) (page + 1) * size)
                .skip((long) page * size)
                .map(lc -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", lc.getId().toString());
                    map.put("title", lc.getTitle());
                    map.put("description", lc.getDescription());
                    map.put("sessionType", lc.getSessionType() != null ? lc.getSessionType().name() : "LECTURE");
                    map.put("status", lc.getStatus());
                    map.put("scheduledAt", lc.getScheduledAt() != null ? lc.getScheduledAt().toString() : null);
                    map.put("durationMinutes", lc.getDurationMinutes());
                    map.put("subjectId", lc.getSubjectId() != null ? lc.getSubjectId().toString() : null);
                    map.put("subjectName", lc.getSubjectId() != null
                            ? subjectRepository.findById(lc.getSubjectId()).map(s -> s.getName()).orElse(null)
                            : null);
                    map.put("teacherName", lc.getTeacherId() != null
                            ? teacherRepository.findById(lc.getTeacherId())
                                    .flatMap(tr -> userRepository.findById(tr.getUserId()))
                                    .map(u -> u.getFullName())
                                    .orElse(null)
                            : null);
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/live-now")
    // Read-only, institution-scoped "live now" list also used by the dashboard sidebar
    // badge for every role - the class-level student-only restriction does not apply here.
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'TEACHER', 'ADMIN', 'INSTITUTION_ADMIN', 'PARENT')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLiveNow(HttpServletRequest request) {
        UUID institutionId = getInstitutionId(request);
        if (institutionId == null) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        LocalDate today = LocalDate.now();
        List<LiveClass> todaySessions = liveClassRepository
                .findByInstitutionIdAndScheduledAtBetween(
                        institutionId,
                        today.atStartOfDay(),
                        today.atTime(LocalTime.MAX));

        List<Map<String, Object>> liveNow = todaySessions.stream()
                .filter(lc -> "IN_PROGRESS".equals(lc.getStatus()) || "LIVE".equals(lc.getStatus()) || "STARTING".equals(lc.getStatus()))
                .map(lc -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", lc.getId().toString());
                    map.put("title", lc.getTitle());
                    map.put("sessionType", lc.getSessionType() != null ? lc.getSessionType().name() : "LECTURE");
                    map.put("status", lc.getStatus());
                    map.put("scheduledAt", lc.getScheduledAt() != null ? lc.getScheduledAt().toString() : null);
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(liveNow));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUpcoming(HttpServletRequest request) {
        UUID institutionId = getInstitutionId(request);
        if (institutionId == null) {
            return ResponseEntity.ok(ApiResponse.success(Collections.emptyList()));
        }

        LocalDate today = LocalDate.now();
        List<LiveClass> todaySessions = liveClassRepository
                .findByInstitutionIdAndScheduledAtBetween(
                        institutionId,
                        today.atStartOfDay(),
                        today.plusDays(7).atTime(LocalTime.MAX));

        List<Map<String, Object>> upcoming = todaySessions.stream()
                .filter(lc -> "SCHEDULED".equals(lc.getStatus()))
                .map(lc -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", lc.getId().toString());
                    map.put("title", lc.getTitle());
                    map.put("sessionType", lc.getSessionType() != null ? lc.getSessionType().name() : "LECTURE");
                    map.put("status", lc.getStatus());
                    map.put("scheduledAt", lc.getScheduledAt() != null ? lc.getScheduledAt().toString() : null);
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(upcoming));
    }

    private static int statusPriority(String status) {
        if ("IN_PROGRESS".equals(status) || "LIVE".equals(status) || "STARTING".equals(status)) return 0;
        if ("SCHEDULED".equals(status) || "PENDING".equals(status)) return 1;
        return 2;
    }

    private UUID getInstitutionId(HttpServletRequest request) {
        Object instIdAttr = request.getAttribute("institutionId");
        if (instIdAttr instanceof UUID uuid) return uuid;
        return null;
    }
}
