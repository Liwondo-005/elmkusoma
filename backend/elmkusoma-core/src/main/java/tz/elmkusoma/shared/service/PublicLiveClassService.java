package tz.elmkusoma.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.event.domain.Replay;
import tz.elmkusoma.event.repository.ReplayRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.dto.PublicLiveClassView;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Public live-class discovery: Live Now, Upcoming and Past Classes for anonymous visitors.
 *
 * <p>Eligibility is decided by {@link LiveClassRepository}, not here. This service only shapes the
 * result and resolves display names, so there is no code path where a class becomes public without
 * passing the repository's PUBLISHED + PUBLIC predicate.</p>
 *
 * <p>Every "is this live" decision is made against the server clock. Nothing in this chain trusts
 * the browser's clock or a client-side countdown, so a visitor whose device time is wrong still
 * sees the same session set as everyone else.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicLiveClassService {

    private static final int MAX_LIMIT = 24;

    /**
     * The session states that mean "happening now".
     *
     * <p>Service-degraded states are deliberately excluded: a session reporting SERVICE_DEGRADED
     * is not a class a visitor should be sent to join, and a suspended session must never appear
     * live.</p>
     */
    private static final List<String> LIVE_STATUSES = List.of("STARTING", "IN_PROGRESS", "LIVE", "ENDING");

    /** Terminal states eligible for Past Classes. CANCELLED is excluded: not a completed event. */
    private static final List<String> ENDED_STATUSES = List.of("COMPLETED", "ENDED");

    private final LiveClassRepository liveClassRepository;
    private final ReplayRepository replayRepository;
    private final UserRepository userRepository;
    private final InstitutionRepository institutionRepository;
    private final SubjectRepository subjectRepository;
    private final TeacherRepository teacherRepository;

    @Transactional(readOnly = true)
    public List<PublicLiveClassView> liveNow(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return map(liveClassRepository.findPublicLiveNow(now, LIVE_STATUSES, bounded(limit)), now);
    }

    @Transactional(readOnly = true)
    public List<PublicLiveClassView> upcoming(int limit) {
        LocalDateTime now = LocalDateTime.now();
        return map(liveClassRepository.findPublicUpcoming(now, bounded(limit)), now);
    }

    @Transactional(readOnly = true)
    public List<PublicLiveClassView> past(int limit) {
        return map(liveClassRepository.findPublicPast(ENDED_STATUSES, bounded(limit)), LocalDateTime.now());
    }

    /** True when anything is publicly discoverable at all. Lets the section hide rather than look empty. */
    @Transactional(readOnly = true)
    public boolean hasAnyPublic() {
        return liveClassRepository.hasAnyPubliclyDiscoverable();
    }

    private static Pageable bounded(int limit) {
        return PageRequest.of(0, Math.max(1, Math.min(limit, MAX_LIMIT)));
    }

    /**
     * Resolves display names in bulk.
     *
     * <p>One query per dimension rather than one per class: a naive implementation would issue a
     * teacher lookup, a subject lookup and an institution lookup for every card, which is the N+1
     * the brief warns about and which is also an easy way to leak per-row timing information.</p>
     */
    private List<PublicLiveClassView> map(List<LiveClass> classes, LocalDateTime now) {
        if (classes.isEmpty()) {
            return List.of();
        }

        Set<UUID> teacherIds = classes.stream()
                .map(LiveClass::getTeacherId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> teachers = teacherNamesByTeacherId(teacherIds);

        Set<UUID> institutionIds = classes.stream()
                .map(LiveClass::getInstitutionId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> institutions = institutionIds.isEmpty()
                ? Map.of()
                : institutionRepository.findAllById(institutionIds).stream()
                        .collect(Collectors.toMap(Institution::getId, Institution::getName,
                                (a, b) -> a, HashMap::new));
        institutions.values().removeIf(Objects::isNull);

        Set<UUID> subjectIds = classes.stream()
                .map(LiveClass::getSubjectId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, String> subjects = subjectIds.isEmpty()
                ? Map.of()
                : subjectRepository.findAllById(subjectIds).stream()
                        .collect(Collectors.toMap(Subject::getId, Subject::getName,
                                (a, b) -> a, HashMap::new));
        subjects.values().removeIf(Objects::isNull);

        Map<UUID, Replay> replays = replayStatusByClassId(classes);

        return classes.stream()
                .map(lc -> toView(lc, teachers, institutions, subjects, replays, now))
                .toList();
    }

    /**
     * Resolves a display name per live class.
     *
     * <p>{@code LiveClass.teacherId} references {@code teachers.id}, not {@code users.id}, so the
     * name cannot be read by querying users with that id directly. Two fixed queries are needed:
     * teachers by id, then their users.</p>
     */
    private Map<UUID, String> teacherNamesByTeacherId(Set<UUID> teacherIds) {
        if (teacherIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, UUID> userIdByTeacherId = teacherRepository.findAllById(teacherIds).stream()
                .filter(t -> t.getUserId() != null)
                .collect(Collectors.toMap(Teacher::getId, Teacher::getUserId, (a, b) -> a));

        Map<UUID, String> nameByUserId = userRepository.findAllById(new HashSet<>(userIdByTeacherId.values())).stream()
                .collect(Collectors.toMap(User::getId, PublicLiveClassService::displayName,
                        (a, b) -> a, HashMap::new));
        // HashMap allows null values, which displayName deliberately produces for an unnamed user.
        nameByUserId.values().removeIf(Objects::isNull);

        Map<UUID, String> names = new HashMap<>();
        userIdByTeacherId.forEach((teacherId, userId) -> {
            String name = nameByUserId.get(userId);
            if (name != null) {
                names.put(teacherId, name);
            }
        });
        return names;
    }

    /**
     * A recording counts as available only when the replay row says AVAILABLE.
     *
     * <p>Three separate conditions must hold, because each covers a different way a replay can
     * exist without being watchable:</p>
     * <ul>
     *   <li>the live class has a recording URL at all;</li>
     *   <li>that URL is not an {@code egress:} handle, which is the not-yet-published marker the
     *       recording pipeline uses;</li>
     *   <li>a Replay row exists with status AVAILABLE, so a PROCESSING or FAILED replay never
     *       produces a replay button.</li>
     * </ul>
     */
    private Map<UUID, Replay> replayStatusByClassId(List<LiveClass> classes) {
        List<UUID> classIds = classes.stream().map(LiveClass::getId).toList();
        return replayRepository
                .findByLiveSessionIdInAndStatusAndIsDeletedFalse(classIds, Replay.STATUS_AVAILABLE)
                .stream()
                .collect(Collectors.toMap(Replay::getLiveSessionId, Function.identity(), (a, b) -> a));
    }

    private boolean hasPlayableRecording(LiveClass lc, Map<UUID, Replay> replays) {
        String url = lc.getRecordingUrl();
        boolean hasUsableUrl = url != null && !url.isBlank() && !url.startsWith("egress:");
        return hasUsableUrl && replays.containsKey(lc.getId());
    }

    private PublicLiveClassView toView(LiveClass lc,
                                       Map<UUID, String> teachers,
                                       Map<UUID, String> institutions,
                                       Map<UUID, String> subjects,
                                       Map<UUID, Replay> replays,
                                       LocalDateTime now) {
        return PublicLiveClassView.builder()
                .id(lc.getId().toString())
                .title(lc.getTitle())
                .description(lc.getDescription())
                .subjectName(lc.getSubjectId() == null ? null : subjects.get(lc.getSubjectId()))
                .teacherName(lc.getTeacherId() == null ? null : teachers.get(lc.getTeacherId()))
                .institutionName(lc.getInstitutionId() == null ? null : institutions.get(lc.getInstitutionId()))
                .coverImageUrl(lc.getCoverImageUrl())
                .scheduledAt(lc.getScheduledAt())
                .startedAt(lc.getStartedAt())
                .endedAt(lc.getEndedAt())
                .timezone(lc.getTimezone())
                .durationMinutes(lc.getDurationMinutes())
                .status(lc.getStatus())
                .maxParticipants(lc.getMaxParticipants())
                .hasRecording(hasPlayableRecording(lc, replays))
                .build();
    }

    /**
     * "First Last", or whichever part exists.
     *
     * <p>Never a placeholder. A teacher with no name recorded yields null and the card omits the
     * line, rather than inventing something like "ELMKUSOMA Faculty".</p>
     */
    private static String displayName(User user) {
        String first = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String last = user.getLastName() == null ? "" : user.getLastName().trim();
        String combined = (first + " " + last).trim();
        return combined.isEmpty() ? null : combined;
    }
}
