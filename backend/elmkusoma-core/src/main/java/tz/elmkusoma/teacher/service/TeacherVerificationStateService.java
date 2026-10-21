package tz.elmkusoma.teacher.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.administration.domain.VerificationRecord;
import tz.elmkusoma.administration.repository.VerificationRecordRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Teacher formal-verification state machine and the shared publish/delivery guard.
 *
 * <p>State is <b>derived</b>, never stored on the teacher row: it is the most recent
 * {@code verification_records} row of entity type {@code TEACHER} for the user, with one
 * precedence rule — the newest {@code APPROVED} or {@code SUSPENDED} row wins over any
 * newer {@code PENDING} row, so a suspended teacher cannot lift their own suspension by
 * submitting a fresh application (the Platform Admin must reactivate first, which writes
 * a new {@code APPROVED} row). This reuses the existing governance table (V63) for the
 * full teacher workflow, so no schema change is required.</p>
 *
 * <p>Effective states: {@code PROVISIONAL} (no rows yet), {@code PENDING},
 * {@code CHANGES_REQUIRED}, {@code REJECTED}, {@code APPROVED}, {@code SUSPENDED}.</p>
 *
 * <p>The guard methods here are the single enforcement point used by every publish and
 * delivery path (live-class start, event publish/start-live, learning-offering publish,
 * lesson/video/resource publish). Enforcement is backend-authoritative: hiding buttons in
 * the UI is not security.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TeacherVerificationStateService {

    public static final String ENTITY_TYPE = "TEACHER";

    public static final String STATUS_PROVISIONAL = "PROVISIONAL";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_CHANGES_REQUIRED = "CHANGES_REQUIRED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_SUSPENDED = "SUSPENDED";

    /**
     * Preliminary automated check: which documents a teacher must attach per service
     * category, keyed on the existing {@code PricingService.BAND_GOVERNED} taxonomy.
     * Requirement: documents required by service category — never one identical set for
     * every teacher. Enforced at submission time as a preliminary check only; the
     * Platform Admin's review remains the authority that grants formal verification.
     */
    public static final Map<String, List<String>> REQUIRED_DOCUMENTS_BY_SERVICE_TYPE = buildRequiredDocuments();

    private static Map<String, List<String>> buildRequiredDocuments() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        List<String> qualificationPlusCertificate = List.of("TEACHING_QUALIFICATION", "CERTIFICATE");
        map.put("LIVE_CLASS", qualificationPlusCertificate);
        map.put("LIVE_SESSION", qualificationPlusCertificate);
        map.put("LIVE_EVENT", qualificationPlusCertificate);
        map.put("VOCATIONAL_MODULE", List.of("TEACHING_QUALIFICATION", "COMPETENCY_EVIDENCE"));
        map.put("ACADEMIC_MODULE", List.of("TEACHING_QUALIFICATION", "COMPETENCY_EVIDENCE"));
        map.put("COURSE", List.of("TEACHING_QUALIFICATION"));
        map.put("RESOURCE", List.of("TEACHING_QUALIFICATION"));
        map.put("REPLAY", List.of("TEACHING_QUALIFICATION"));
        return Map.copyOf(map);
    }

    private final VerificationRecordRepository verificationRepository;
    private final UserRepository userRepository;
    private final TeacherRepository teacherRepository;

    /** Full verification history for a teacher user, oldest first. */
    @Transactional(readOnly = true)
    public List<VerificationRecord> history(UUID userId) {
        List<VerificationRecord> records =
                new ArrayList<>(verificationRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse(ENTITY_TYPE, userId));
        records.sort(Comparator
                .comparing(VerificationRecord::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(VerificationRecord::getId, Comparator.nullsFirst(Comparator.naturalOrder())));
        return records;
    }

    /**
     * Derived effective state. See class javadoc for the suspension precedence rule.
     */
    @Transactional(readOnly = true)
    public String effectiveStatus(UUID userId) {
        return deriveStatus(history(userId));
    }

    /** Pure derivation over an unordered record list; package-visible for unit testing. */
    static String deriveStatus(List<VerificationRecord> records) {
        // Walk newest → oldest: the newest formal outcome (APPROVED/SUSPENDED) outranks any
        // newer application rows; otherwise the newest row's own status is the sub-state.
        List<VerificationRecord> newestFirst = new ArrayList<>(records);
        newestFirst.sort(Comparator
                .comparing(VerificationRecord::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(VerificationRecord::getId, Comparator.nullsFirst(Comparator.naturalOrder()))
                .reversed());
        String newestStatus = null;
        for (VerificationRecord record : newestFirst) {
            if (newestStatus == null) {
                newestStatus = record.getStatus();
            }
            if (STATUS_APPROVED.equals(record.getStatus()) || STATUS_SUSPENDED.equals(record.getStatus())) {
                return record.getStatus();
            }
        }
        return newestStatus != null ? newestStatus : STATUS_PROVISIONAL;
    }

    @Transactional(readOnly = true)
    public boolean isFormallyVerified(UUID userId) {
        return STATUS_APPROVED.equals(effectiveStatus(userId));
    }

    /**
     * Role-aware publish/delivery gate for an authenticated actor. Only TEACHER actors are
     * subject to the gate; platform/institution admins and providers keep their existing
     * governance paths. A missing or inactive (account-suspended) user fails closed.
     */
    @Transactional(readOnly = true)
    public void requirePublishAllowed(UUID actorUserId) {
        User user = requireUser(actorUserId);
        assertAccountActive(user);
        if (user.getRole() != User.Role.TEACHER) {
            return;
        }
        assertFormallyVerified(user);
    }

    /** String overload for controllers that bind {@code userId} as a String. Fails closed. */
    public void requirePublishAllowed(String actorUserIdRaw) {
        UUID actorUserId;
        try {
            actorUserId = UUID.fromString(actorUserIdRaw);
        } catch (Exception notUuid) {
            throw new ForbiddenException("Formal teacher verification is required to publish or deliver teaching services");
        }
        requirePublishAllowed(actorUserId);
    }

    /**
     * Unconditional teacher gate for delivery operations where the actor is always a
     * teacher (starting one's own live class, keyed by the teacher row).
     */
    @Transactional(readOnly = true)
    public void requireTeacherVerified(UUID userId) {
        User user = requireUser(userId);
        assertAccountActive(user);
        if (user.getRole() != User.Role.TEACHER) {
            throw new ForbiddenException("Teacher verification is required");
        }
        assertFormallyVerified(user);
    }

    /** Map a teachers.id row to its user and apply {@link #requireTeacherVerified}. */
    @Transactional(readOnly = true)
    public void requireTeacherVerifiedByTeacherRow(UUID teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .filter(t -> !Boolean.TRUE.equals(t.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));
        requireTeacherVerified(teacher.getUserId());
    }

    /** Union of the required document types for the given service categories. */
    public List<String> requiredDocuments(Collection<String> serviceTypes) {
        Set<String> required = new LinkedHashSet<>();
        for (String serviceType : normalizeServiceTypes(serviceTypes)) {
            List<String> docs = REQUIRED_DOCUMENTS_BY_SERVICE_TYPE.get(serviceType);
            if (docs == null) {
                throw new IllegalArgumentException("Unknown service type: " + serviceType
                        + ". Supported types: " + REQUIRED_DOCUMENTS_BY_SERVICE_TYPE.keySet());
            }
            required.addAll(docs);
        }
        return List.copyOf(required);
    }

    /** Normalized (trimmed, upper-cased, de-duplicated, order-preserving) service types. */
    public static List<String> normalizeServiceTypes(Collection<String> serviceTypes) {
        Set<String> normalized = new LinkedHashSet<>();
        if (serviceTypes != null) {
            for (String serviceType : serviceTypes) {
                if (serviceType != null && !serviceType.isBlank()) {
                    normalized.add(serviceType.trim().toUpperCase());
                }
            }
        }
        return List.copyOf(normalized);
    }

    private User requireUser(UUID userId) {
        if (userId == null) {
            throw new ForbiddenException("Formal teacher verification is required to publish or deliver teaching services");
        }
        return userRepository.findByIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private void assertAccountActive(User user) {
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new ForbiddenException("Account is suspended");
        }
    }

    private void assertFormallyVerified(User user) {
        String state = effectiveStatus(user.getId());
        if (STATUS_SUSPENDED.equals(state)) {
            throw new ForbiddenException("Teacher verification is suspended. Contact the platform administrator.");
        }
        if (!STATUS_APPROVED.equals(state)) {
            throw new ForbiddenException(
                    "Formal teacher verification is required to publish or deliver teaching services");
        }
    }
}
