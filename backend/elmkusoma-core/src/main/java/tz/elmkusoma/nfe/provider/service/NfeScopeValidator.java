package tz.elmkusoma.nfe.provider.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nfe.learner.repository.NfeLearnerRepository;
import tz.elmkusoma.nfe.program.repository.NfeProgramRepository;
import tz.elmkusoma.nfe.provider.domain.EducationProvider;
import tz.elmkusoma.nfe.provider.repository.EducationProviderRepository;
import tz.elmkusoma.nfe.session.repository.NfeSessionRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.repository.InstitutionRepository;

import java.util.UUID;

/**
 * Server-side scope validation for the NFE provider stack (audit B-15 / B-16 / B-12).
 *
 * <p>Before this component the seven provider services trusted ids supplied by the caller:
 * a {@code providerId} from the path or body was written verbatim, so a row could point at
 * another tenant's provider, and {@code sessionId}/{@code learnerId}/{@code programId}
 * references were never checked against the caller's institution at all. This validator
 * turns those references into server-derived, tenant-checked values.
 *
 * <p>It reuses the existing repositories and exception vocabulary
 * ({@link ResourceNotFoundException} for a foreign or missing row, {@link ForbiddenException}
 * for a suspended organisation). No new authorization system is introduced.
 */
@Component
@RequiredArgsConstructor
public class NfeScopeValidator {

    private final EducationProviderRepository providerRepository;
    private final NfeProgramRepository programRepository;
    private final NfeSessionRepository sessionRepository;
    private final NfeLearnerRepository learnerRepository;
    private final InstitutionRepository institutionRepository;
    private final tz.elmkusoma.course.repository.CourseRepository courseRepository;
    private final tz.elmkusoma.student.repository.StudentRepository studentRepository;
    private final tz.elmkusoma.shared.repository.UserRepository userRepository;

    /**
     * Ensures the organisation is in a state where providers may write.
     *
     * <p>Deliberately keyed on {@code isActive}/{@code isDeleted} and a SUSPENDED status rather
     * than on {@code isVerified}: most institutions have no status row yet, so gating on
     * verification would lock out legitimately active organisations. Platform approval remains
     * visible through {@code is_verified} without blocking delivery.
     */
    @Transactional(readOnly = true)
    public void requireWritableInstitution(UUID institutionId) {
        if (institutionId == null) {
            throw new ForbiddenException("No organization scope resolved for this request");
        }
        Institution institution = institutionRepository.findByIdAndIsDeletedFalse(institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", institutionId));
        if (Boolean.FALSE.equals(institution.getIsActive())) {
            throw new ForbiddenException("This organization is not active. Provider operations are suspended.");
        }
        if ("SUSPENDED".equalsIgnoreCase(String.valueOf(institution.getStatus()))
                || "DEACTIVATED".equalsIgnoreCase(String.valueOf(institution.getStatus()))
                || "ARCHIVED".equalsIgnoreCase(String.valueOf(institution.getStatus()))) {
            throw new ForbiddenException("This organization is " + institution.getStatus()
                    + ". Provider operations are not permitted.");
        }
    }

    /**
     * Validates that {@code providerId} is a live provider inside the caller's institution and
     * returns it. A foreign, unknown or soft-deleted provider yields 404 -- never a row that
     * silently references another tenant.
     */
    /**
     * Per-provider ownership enforcement (audit B-14).
     *
     * <p>When the authenticated caller holds an explicit provider scope (memberships with
     * provider_id), only that provider may be touched. Callers without an explicit scope keep the
     * previous institution-wide behaviour, so single-provider organisations are unaffected.
     */
@Transactional(readOnly = true)
    public java.util.Set<UUID> ownedProviderIds() {
        Object value = currentRequestAttribute("ownedProviderIds");
        if (value instanceof java.util.Set<?> set) {
            java.util.Set<UUID> ids = new java.util.LinkedHashSet<>();
            for (Object o : set) {
                if (o instanceof UUID u) {
                    ids.add(u);
                }
            }
            return ids;
        }
        return java.util.Collections.emptySet();
    }

    private Object currentRequestAttribute(String name) {
        var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getAttribute(name, org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
    }

    @Transactional(readOnly = true)
    public EducationProvider requireOwnedProvider(UUID institutionId, UUID providerId) {
        requireWritableInstitution(institutionId);
        if (providerId == null) {
            throw new ResourceNotFoundException("Education Provider", "id", null);
        }
        EducationProvider provider = providerRepository.findByIdAndInstitutionId(providerId, institutionId)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Education Provider", "id", providerId));

        java.util.Set<UUID> owned = ownedProviderIds();
        if (!owned.isEmpty() && !owned.contains(provider.getId())) {
            // Reuse the purpose-built guard that existed but was never called.
            new tz.elmkusoma.common.OwnershipGuard().verifyProvider(provider.getId(),
                    owned.iterator().next());
        }
        return provider;
    }

    /** Same as {@link #requireOwnedProvider} but tolerates a null id (optional FK). */
    @Transactional(readOnly = true)
    public UUID validateOptionalProvider(UUID institutionId, UUID providerId) {
        if (providerId == null) {
            return null;
        }
        return requireOwnedProvider(institutionId, providerId).getId();
    }

    @Transactional(readOnly = true)
    public UUID requireOwnedProgram(UUID institutionId, UUID programId) {
        if (programId == null) {
            return null;
        }
        return programRepository.findByIdAndInstitutionId(programId, institutionId)
                .map(p -> p.getId())
                .orElseThrow(() -> new ResourceNotFoundException("NFE Program", "id", programId));
    }

    @Transactional(readOnly = true)
    public UUID requireOwnedSession(UUID institutionId, UUID sessionId) {
        if (sessionId == null) {
            return null;
        }
        return sessionRepository.findByIdAndInstitutionId(sessionId, institutionId)
                .map(s -> s.getId())
                .orElseThrow(() -> new ResourceNotFoundException("NFE Session", "id", sessionId));
    }

    @Transactional(readOnly = true)
    public UUID requireOwnedLearner(UUID institutionId, UUID learnerId) {
        if (learnerId == null) {
            return null;
        }
        return learnerRepository.findByIdAndInstitutionId(learnerId, institutionId)
                .map(l -> l.getId())
                .orElseThrow(() -> new ResourceNotFoundException("NFE Learner", "id", learnerId));
    }

    /**
     * Ecosystem link validation (audit B-11 / X-2): a provider may only attach a program to a
     * course that belongs to the SAME organisation. Prevents a provider from claiming another
     * tenant's catalogue.
     */
    @Transactional(readOnly = true)
    public UUID requireOwnedCourse(UUID institutionId, UUID courseId) {
        if (courseId == null) {
            return null;
        }
        return courseRepository.findByIdAndIsDeletedFalse(courseId)
                .filter(c -> c.getInstitutionId() == null || c.getInstitutionId().equals(institutionId))
                .map(c -> c.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
    }

    /**
     * Resolves (and records) the K-12 student profile for a provider learner, so provider
     * participation becomes visible in the learner workspace and vice versa (audit X-2/X-3).
     */
    @Transactional
    public UUID resolveStudentLink(UUID institutionId, UUID userId, UUID requestedStudentId) {
        if (userId == null) {
            return requestedStudentId;
        }
        java.util.Optional<tz.elmkusoma.shared.domain.User> user =
                userRepository.findByIdAndIsDeletedFalse(userId);
        if (user.isEmpty()) {
            return requestedStudentId;
        }
        // Prefer the profile the caller supplied, but only when it really belongs to this user.
        if (requestedStudentId != null) {
            boolean belongsToUser = studentRepository.findByIdAndIsDeletedFalse(requestedStudentId)
                    .map(s -> s.getUserId() != null && s.getUserId().equals(userId))
                    .orElse(false);
            if (belongsToUser) {
                return requestedStudentId;
            }
        }
        return studentRepository.findByUserIdAndIsDeletedFalse(userId)
                .map(s -> s.getId())
                .orElse(requestedStudentId);
    }
}