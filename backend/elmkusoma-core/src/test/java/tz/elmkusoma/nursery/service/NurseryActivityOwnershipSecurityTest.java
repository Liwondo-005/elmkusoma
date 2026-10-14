package tz.elmkusoma.nursery.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.repository.ClassGroupRepository;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nursery.domain.NurseryActivity;
import tz.elmkusoma.nursery.dto.request.CreateNurseryActivityRequest;
import tz.elmkusoma.nursery.dto.response.NurseryActivityResponse;
import tz.elmkusoma.nursery.repository.NurseryActivityRepository;
import tz.elmkusoma.nursery.service.impl.NurseryActivityServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Institution scoping of {@link NurseryActivityServiceImpl}: class-keyed reads
 * and id-based reads must resolve the owning ClassGroup and fail closed across
 * tenants (403) before touching the repository.
 */
@ExtendWith(MockitoExtension.class)
class NurseryActivityOwnershipSecurityTest {

    @Mock
    private NurseryActivityRepository nurseryActivityRepository;
    @Mock
    private ClassGroupRepository classGroupRepository;

    /**
     * A real instance, spied. OwnershipGuard is a concrete collaborator, not an
     * interface, so {@code @InjectMocks} would otherwise pass null and every
     * ownership check would NPE instead of failing closed.
     */
    @Spy
    private OwnershipGuard ownershipGuard = new OwnershipGuard();

    @InjectMocks
    private NurseryActivityServiceImpl service;

    private UUID institutionId;
    private UUID otherInstitutionId;
    private UUID classGroupId;
    private UUID activityId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        otherInstitutionId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
        activityId = UUID.randomUUID();
    }

    private ClassGroup classGroupOwnedBy(UUID ownerInstitutionId) {
        return ClassGroup.builder()
                .id(classGroupId)
                .institutionId(ownerInstitutionId)
                .name("Kidu A")
                .build();
    }

    private NurseryActivity activity() {
        return NurseryActivity.builder()
                .id(activityId)
                .classGroupId(classGroupId)
                .activityName("Counting game")
                .activityType(NurseryActivity.ActivityType.EDUCATIONAL)
                .activityDate(LocalDate.of(2026, 1, 5))
                .status(NurseryActivity.ActivityStatus.PLANNED)
                .isDeleted(false)
                .build();
    }

    // ── getByClassGroupId ──

    @Test
    void getByClassGroupId_crossInstitutionClassGroupIsRejected() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class,
                () -> service.getByClassGroupId(institutionId, classGroupId));

        verify(nurseryActivityRepository, never()).findByClassGroupIdAndIsDeletedFalse(any());
    }

    @Test
    void getByClassGroupId_unknownClassGroupIsNotFound() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getByClassGroupId(institutionId, classGroupId));

        verify(nurseryActivityRepository, never()).findByClassGroupIdAndIsDeletedFalse(any());
    }

    @Test
    void getByClassGroupId_sameInstitutionIsAllowed() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(institutionId)));
        when(nurseryActivityRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId))
                .thenReturn(List.of());

        assertDoesNotThrow(() -> service.getByClassGroupId(institutionId, classGroupId));

        verify(nurseryActivityRepository).findByClassGroupIdAndIsDeletedFalse(classGroupId);
    }

    // ── getByClassAndDate ──

    @Test
    void getByClassAndDate_crossInstitutionClassGroupIsRejected() {
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getByClassAndDate(
                institutionId, classGroupId, LocalDate.of(2026, 1, 5)));

        verify(nurseryActivityRepository, never())
                .findByClassGroupIdAndActivityDateAndIsDeletedFalse(any(), any());
    }

    @Test
    void getByClassAndDate_unknownClassGroupIsNotFound() {
        when(classGroupRepository.findById(classGroupId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getByClassAndDate(
                institutionId, classGroupId, LocalDate.of(2026, 1, 5)));
    }

    // ── getById ──

    @Test
    void getById_crossInstitutionActivityIsRejected() {
        when(nurseryActivityRepository.findById(activityId)).thenReturn(Optional.of(activity()));
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.getById(institutionId, activityId));
    }

    @Test
    void getById_unknownActivityIsNotFound() {
        when(nurseryActivityRepository.findById(activityId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getById(institutionId, activityId));
    }

    @Test
    void getById_sameInstitutionActivityIsReadable() {
        when(nurseryActivityRepository.findById(activityId)).thenReturn(Optional.of(activity()));
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(institutionId)));

        NurseryActivityResponse response = service.getById(institutionId, activityId);

        assertEquals("Counting game", response.getActivityName());
    }

    // ── update ──

    @Test
    void update_crossInstitutionActivityIsRejectedWithoutSave() {
        when(nurseryActivityRepository.findById(activityId)).thenReturn(Optional.of(activity()));
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(otherInstitutionId)));

        CreateNurseryActivityRequest request = new CreateNurseryActivityRequest();
        request.setActivityName("Hijacked");

        assertThrows(ForbiddenException.class,
                () -> service.update(institutionId, activityId, request));

        verify(nurseryActivityRepository, never()).save(any());
    }

    @Test
    void update_unknownActivityIsNotFound() {
        when(nurseryActivityRepository.findById(activityId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(
                institutionId, activityId, new CreateNurseryActivityRequest()));

        verify(nurseryActivityRepository, never()).save(any());
    }

    // ── delete ──

    @Test
    void delete_crossInstitutionActivityIsRejectedWithoutSave() {
        when(nurseryActivityRepository.findById(activityId)).thenReturn(Optional.of(activity()));
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.delete(institutionId, activityId));

        verify(nurseryActivityRepository, never()).save(any());
    }

    @Test
    void delete_sameInstitutionSoftDeletes() {
        NurseryActivity activity = activity();
        when(nurseryActivityRepository.findById(activityId)).thenReturn(Optional.of(activity));
        when(classGroupRepository.findById(classGroupId))
                .thenReturn(Optional.of(classGroupOwnedBy(institutionId)));
        when(nurseryActivityRepository.save(any(NurseryActivity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.delete(institutionId, activityId);

        assertTrue(activity.getIsDeleted());
        verify(nurseryActivityRepository).save(activity);
    }

    // ── getByType ──

    @Test
    void getByType_scopesQueryToCallerInstitutionNotUnscopedTypeFinder() {
        when(nurseryActivityRepository.findByInstitutionIdAndIsDeletedFalse(institutionId))
                .thenReturn(List.of(activity()));

        List<?> result = service.getByType(institutionId, "EDUCATIONAL");

        assertEquals(1, result.size());
        verify(nurseryActivityRepository).findByInstitutionIdAndIsDeletedFalse(institutionId);
        verify(nurseryActivityRepository, never()).findByActivityTypeAndIsDeletedFalse(any());
    }

    @Test
    void getByType_filtersByRequestedType() {
        NurseryActivity song = NurseryActivity.builder()
                .id(UUID.randomUUID())
                .classGroupId(classGroupId)
                .activityName("Ten little fingers")
                .activityType(NurseryActivity.ActivityType.SONG)
                .activityDate(LocalDate.of(2026, 1, 5))
                .status(NurseryActivity.ActivityStatus.PLANNED)
                .isDeleted(false)
                .build();
        when(nurseryActivityRepository.findByInstitutionIdAndIsDeletedFalse(institutionId))
                .thenReturn(List.of(activity(), song));

        List<?> result = service.getByType(institutionId, "SONG");

        assertEquals(1, result.size());
        assertEquals("Ten little fingers",
                ((NurseryActivityResponse) result.get(0)).getActivityName());
    }
}
