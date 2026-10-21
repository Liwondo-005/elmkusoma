package tz.elmkusoma.teacher.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.administration.domain.VerificationRecord;
import tz.elmkusoma.administration.repository.VerificationRecordRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherVerificationStateServiceTest {

    @Mock private VerificationRecordRepository verificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeacherRepository teacherRepository;

    @InjectMocks private TeacherVerificationStateService stateService;

    private static VerificationRecord record(String status, LocalDateTime createdAt) {
        VerificationRecord rec = VerificationRecord.builder().status(status).build();
        rec.setId(UUID.randomUUID());
        rec.setCreatedAt(createdAt);
        return rec;
    }

    private static User user(User.Role role, boolean active) {
        return User.builder().id(UUID.randomUUID()).role(role).isActive(active).build();
    }

    private void stubUser(User u) {
        lenient().when(userRepository.findByIdAndIsDeletedFalse(u.getId())).thenReturn(Optional.of(u));
    }

    private void stubHistory(UUID userId, List<VerificationRecord> records) {
        lenient().when(verificationRepository.findByEntityTypeAndEntityIdAndIsDeletedFalse("TEACHER", userId))
                .thenReturn(records);
    }

    // ── derivation ────────────────────────────────────────────────────────────────────

    @Test
    void deriveStatus_withoutAnyRecord_isProvisional() {
        assertEquals("PROVISIONAL", TeacherVerificationStateService.deriveStatus(List.of()));
    }

    @Test
    void deriveStatus_newestApplicationRow_isTheSubState() {
        assertEquals("PENDING", TeacherVerificationStateService.deriveStatus(
                List.of(record("PENDING", LocalDateTime.now()))));
        assertEquals("CHANGES_REQUIRED", TeacherVerificationStateService.deriveStatus(
                List.of(record("CHANGES_REQUIRED", LocalDateTime.now()))));
    }

    @Test
    void deriveStatus_approvalWinsOverOlderApplications() {
        assertEquals("APPROVED", TeacherVerificationStateService.deriveStatus(List.of(
                record("PENDING", LocalDateTime.now().minusDays(2)),
                record("APPROVED", LocalDateTime.now().minusDays(1)))));
    }

    /** Suspension must not be self-liftable by filing a fresh application afterwards. */
    @Test
    void deriveStatus_suspensionOutranksNewerPendingApplication() {
        assertEquals("SUSPENDED", TeacherVerificationStateService.deriveStatus(List.of(
                record("APPROVED", LocalDateTime.now().minusDays(3)),
                record("SUSPENDED", LocalDateTime.now().minusDays(2)),
                record("PENDING", LocalDateTime.now().minusDays(1)))));
    }

    /** Reactivation writes a fresh APPROVED row, which outranks the earlier suspension. */
    @Test
    void deriveStatus_reactivationOutranksOlderSuspension() {
        assertEquals("APPROVED", TeacherVerificationStateService.deriveStatus(List.of(
                record("SUSPENDED", LocalDateTime.now().minusDays(2)),
                record("APPROVED", LocalDateTime.now().minusDays(1)))));
    }

    @Test
    void effectiveStatus_isOrderIndependentAndRepositoryDriven() {
        UUID userId = UUID.randomUUID();
        stubHistory(userId, List.of(
                record("PENDING", LocalDateTime.now()),
                record("SUSPENDED", LocalDateTime.now().minusHours(1))));
        assertEquals("SUSPENDED", stateService.effectiveStatus(userId));
    }

    // ── guards ────────────────────────────────────────────────────────────────────────

    @Test
    void requirePublishAllowed_nonTeacherRolesAreExempt() {
        User admin = user(User.Role.INSTITUTION_ADMIN, true);
        stubUser(admin);
        assertDoesNotThrow(() -> stateService.requirePublishAllowed(admin.getId()));
    }

    @Test
    void requirePublishAllowed_provisionalTeacherIsForbidden() {
        User teacher = user(User.Role.TEACHER, true);
        stubUser(teacher);
        stubHistory(teacher.getId(), List.of());
        assertThrows(ForbiddenException.class, () -> stateService.requirePublishAllowed(teacher.getId()));
    }

    @Test
    void requirePublishAllowed_approvedTeacherPasses() {
        User teacher = user(User.Role.TEACHER, true);
        stubUser(teacher);
        stubHistory(teacher.getId(), List.of(record("APPROVED", LocalDateTime.now())));
        assertDoesNotThrow(() -> stateService.requirePublishAllowed(teacher.getId()));
    }

    @Test
    void requirePublishAllowed_suspendedTeacherIsForbiddenWithDedicatedMessage() {
        User teacher = user(User.Role.TEACHER, true);
        stubUser(teacher);
        stubHistory(teacher.getId(), List.of(record("SUSPENDED", LocalDateTime.now())));
        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> stateService.requirePublishAllowed(teacher.getId()));
        assertTrue(ex.getMessage().contains("suspended"), ex.getMessage());
    }

    @Test
    void requirePublishAllowed_inactiveAccountFailsClosed() {
        User teacher = user(User.Role.TEACHER, false);
        stubUser(teacher);
        assertThrows(ForbiddenException.class, () -> stateService.requirePublishAllowed(teacher.getId()));
    }

    @Test
    void requirePublishAllowed_stringOverloadFailsClosedOnGarbage() {
        assertThrows(ForbiddenException.class, () -> stateService.requirePublishAllowed("not-a-uuid"));
        assertThrows(ForbiddenException.class, () -> stateService.requirePublishAllowed((String) null));
    }

    @Test
    void requireTeacherVerifiedByTeacherRow_unknownRowIsNotFound() {
        when(teacherRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> stateService.requireTeacherVerifiedByTeacherRow(UUID.randomUUID()));
    }

    @Test
    void requireTeacherVerifiedByTeacherRow_mapsToTheOwningUser() {
        UUID teacherRowId = UUID.randomUUID();
        User teacher = user(User.Role.TEACHER, true);
        Teacher row = Teacher.builder().userId(teacher.getId()).build();
        row.setId(teacherRowId);
        when(teacherRepository.findById(teacherRowId)).thenReturn(Optional.of(row));
        stubUser(teacher);
        stubHistory(teacher.getId(), List.of(record("APPROVED", LocalDateTime.now())));
        assertDoesNotThrow(() -> stateService.requireTeacherVerifiedByTeacherRow(teacherRowId));
    }

    // ── document matrix ───────────────────────────────────────────────────────────────

    @Test
    void requiredDocuments_unionsAcrossDeclaredServiceCategories() {
        List<String> required = stateService.requiredDocuments(List.of("LIVE_CLASS", "VOCATIONAL_MODULE"));
        assertTrue(required.containsAll(List.of(
                "TEACHING_QUALIFICATION", "CERTIFICATE", "COMPETENCY_EVIDENCE")));
        assertEquals(3, required.size());
    }

    @Test
    void requiredDocuments_unknownCategoryIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> stateService.requiredDocuments(List.of("NOT_A_SERVICE")));
    }

    @Test
    void normalizeServiceTypes_uppercasesAndDedupes() {
        assertEquals(List.of("LIVE_CLASS", "COURSE"),
                TeacherVerificationStateService.normalizeServiceTypes(
                        java.util.Arrays.asList(" live_class ", "COURSE", "live_class", null, " ")));
    }
}
