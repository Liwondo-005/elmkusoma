package tz.elmkusoma.liveclass.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.liveclass.service.LiveClassRecordingAccessService;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final LiveClassRecordingAccessService recordingAccess;

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
        LocalDateTime windowFrom = today.minusDays(7).atStartOfDay();
        LocalDateTime windowTo = today.plusDays(7).atTime(LocalTime.MAX);
        List<LiveClass> classes = new ArrayList<>(liveClassRepository
                .findByInstitutionIdAndScheduledAtBetween(institutionId, windowFrom, windowTo));

        // Eligible open learning sessions from ANY school surface alongside the school's own
        // sessions — cross-institution learning without changing the learner's affiliation.
        // Same time window, deduplicated against rows the school query already returned.
        Set<UUID> seen = new HashSet<>();
        classes.forEach(lc -> seen.add(lc.getId()));
        for (LiveClass open : liveClassRepository.findOpenLearningSessions()) {
            if (open.getScheduledAt() != null
                    && !open.getScheduledAt().isBefore(windowFrom)
                    && !open.getScheduledAt().isAfter(windowTo)
                    && seen.add(open.getId())) {
                classes.add(open);
            }
        }

        // The institution is a tenant boundary, not an audience: membership alone must not hand
        // a learner every class-specific session in the school. This applies the same rule that
        // already gates joining the session, so discovery and access cannot disagree. Filtering
        // before the sort and the pagination window below keeps a page from being filled with
        // sessions the caller was never entitled to see.
        classes = classes.stream()
                .filter(lc -> mayView(lc, request))
                .collect(Collectors.toList());

        // Surface running classes first, then scheduled ones, so the paginated
        // window below can never push an IN_PROGRESS class off the first page.
        classes.sort(Comparator
                .comparingInt((LiveClass lc) -> statusPriority(lc.getStatus()))
                .thenComparing(LiveClass::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder())));

        // Pagination is attacker-controlled input: a negative size would make Stream.limit throw
        // and a runaway size would pull the whole institution into one response. Bound both.
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        List<Map<String, Object>> result = classes.stream()
                .limit((long) (safePage + 1) * safeSize)
                .skip((long) safePage * safeSize)
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
                    // Real server-derived recording signal for the discovery UI. A finished
                    // recording resolves recording_url to its storage URL; a failed one is
                    // cleared to null by the completion/failure handlers. The raw URL is not
                    // exposed here: replay viewing stays entitlement-checked server side.
                    map.put("hasRecording", lc.getRecordingUrl() != null && !lc.getRecordingUrl().isBlank());
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
                .filter(lc -> mayView(lc, request))
                .filter(lc -> "IN_PROGRESS".equals(lc.getStatus()) || "LIVE".equals(lc.getStatus()) || "STARTING".equals(lc.getStatus()))
                .map(lc -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", lc.getId().toString());
                    map.put("title", lc.getTitle());
                    map.put("sessionType", lc.getSessionType() != null ? lc.getSessionType().name() : "LECTURE");
                    map.put("status", lc.getStatus());
                    map.put("scheduledAt", lc.getScheduledAt() != null ? lc.getScheduledAt().toString() : null);
                    map.put("hasRecording", lc.getRecordingUrl() != null && !lc.getRecordingUrl().isBlank());
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
                .filter(lc -> mayView(lc, request))
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

    /**
     * Whether the caller is entitled to be shown this session at all.
     *
     * <p>Delegates to the very rule that already gates <em>entering</em> a session, so what a
     * learner is listed and what a learner may join cannot drift apart: an admin sees everything
     * in scope, a teacher only a class they own, and a learner either a session in their own
     * class or one that declares no audience and is therefore open to the whole institution.
     * Institution membership by itself grants nothing here.</p>
     *
     * <p>Only learner roles are narrowed. The live-now list is shared with teachers, admins and
     * parents for the dashboard badge, and narrowing those roles would be an unrequested behaviour
     * change; their reach stays exactly as it was.</p>
     *
     * <p>Fail-closed: a caller whose role cannot be resolved is treated as a learner rather than
     * being waved through.</p>
     */
    private boolean mayView(LiveClass liveClass, HttpServletRequest request) {
        UUID institutionId = getInstitutionId(request);
        // isAllowed() documents that its caller has already scoped the class to the caller's
        // tenant. The repository query does that, but an authorization decision should not rest on
        // one layer alone, so the tenant boundary is restated here - and fails closed rather than
        // failing open if either side is unresolved.
        if (liveClass == null) {
            return false;
        }
        // The single deliberate cross-tenant exception: a genuine open learning session
        // (PUBLISHED + PUBLIC + no declared class audience) is eligible for learners of every
        // school and for independent learners. Every other class still needs an exact match.
        boolean openSession = liveClass.isOpenLearningSession();
        if (!openSession && (institutionId == null || !institutionId.equals(liveClass.getInstitutionId()))) {
            return false;
        }
        String role = getUserRole(request);
        if (role != null && !isLearnerRole(role)) {
            return true;
        }
        return recordingAccess.isAllowed(liveClass, getUserId(request), institutionId, role);
    }

    private static boolean isLearnerRole(String role) {
        return "STUDENT".equals(role) || "OTHER_LEARNER".equals(role) || "LEARNER".equals(role);
    }

    private UUID getUserId(HttpServletRequest request) {
        Object id = request.getAttribute("userId");
        return id instanceof UUID uuid ? uuid : null;
    }

    /**
     * The role driving the audience decision. Falls back to the authenticated authorities so a
     * missing request attribute can never turn into an authorization bypass.
     */
    private String getUserRole(HttpServletRequest request) {
        Object role = request.getAttribute("userRole");
        if (role instanceof String s && !s.isBlank()) {
            return s;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String value = authority.getAuthority();
                if (value != null && value.startsWith("ROLE_")) {
                    return value.substring("ROLE_".length());
                }
            }
        }
        return null;
    }

    private UUID getInstitutionId(HttpServletRequest request) {
        Object instIdAttr = request.getAttribute("institutionId");
        if (instIdAttr instanceof UUID uuid) return uuid;
        return null;
    }
}
