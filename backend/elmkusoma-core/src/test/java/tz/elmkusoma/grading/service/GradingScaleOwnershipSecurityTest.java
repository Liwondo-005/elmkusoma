package tz.elmkusoma.grading.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.grading.domain.GradingScale;
import tz.elmkusoma.grading.dto.request.CreateGradingScaleRequest;
import tz.elmkusoma.grading.dto.response.GradingScaleResponse;
import tz.elmkusoma.grading.repository.GradeBoundaryRepository;
import tz.elmkusoma.grading.repository.GradingScaleRepository;
import tz.elmkusoma.grading.service.impl.GradingScaleServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cross-tenant hardening of {@link GradingScaleServiceImpl}: read / update /
 * delete are institution-scoped through the shared {@link OwnershipGuard} (403),
 * INSTITUTION_ADMIN is NOT exempt, and only platform ADMIN bypasses the check.
 */
@ExtendWith(MockitoExtension.class)
class GradingScaleOwnershipSecurityTest {

    @Mock
    private GradingScaleRepository gradingScaleRepository;
    @Mock
    private GradeBoundaryRepository gradeBoundaryRepository;
    @Spy
    private OwnershipGuard ownershipGuard = new OwnershipGuard();

    @InjectMocks
    private GradingScaleServiceImpl service;

    private UUID scaleId;
    private UUID institutionId;
    private UUID otherInstitutionId;

    @BeforeEach
    void setUp() {
        scaleId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        otherInstitutionId = UUID.randomUUID();
    }

    private GradingScale scale(UUID ownerInstitutionId) {
        return GradingScale.builder()
                .id(scaleId)
                .institutionId(ownerInstitutionId)
                .name("O-Level")
                .scaleType(GradingScale.ScaleType.PERCENTAGE)
                .isDefault(false)
                .isActive(true)
                .isDeleted(false)
                .build();
    }

    private CreateGradingScaleRequest request() {
        CreateGradingScaleRequest request = new CreateGradingScaleRequest();
        request.setName("Updated");
        request.setScaleType("PERCENTAGE");
        return request;
    }

    // ── getById ──

    @Test
    void getById_sameInstitution_returnsScale() {
        when(gradingScaleRepository.findById(scaleId)).thenReturn(Optional.of(scale(institutionId)));
        when(gradeBoundaryRepository.findByGradingScaleIdAndIsDeletedFalse(scaleId))
                .thenReturn(List.of());

        GradingScaleResponse response = service.getById(scaleId, institutionId, "INSTITUTION_ADMIN");

        assertEquals(scaleId, response.getId());
        assertEquals("O-Level", response.getName());
    }

    @Test
    void getById_foreignInstitution_throwsForbidden() {
        when(gradingScaleRepository.findById(scaleId))
                .thenReturn(Optional.of(scale(otherInstitutionId)));

        assertThrows(ForbiddenException.class,
                () -> service.getById(scaleId, institutionId, "INSTITUTION_ADMIN"));
    }

    @Test
    void getById_institutionAdmin_isNotExemptFromInstitutionCheck() {
        when(gradingScaleRepository.findById(scaleId))
                .thenReturn(Optional.of(scale(otherInstitutionId)));

        assertThrows(ForbiddenException.class,
                () -> service.getById(scaleId, institutionId, "INSTITUTION_ADMIN"));
    }

    @Test
    void getById_platformAdmin_bypassesInstitutionCheck() {
        when(gradingScaleRepository.findById(scaleId)).thenReturn(Optional.of(scale(otherInstitutionId)));
        when(gradeBoundaryRepository.findByGradingScaleIdAndIsDeletedFalse(scaleId))
                .thenReturn(List.of());

        GradingScaleResponse response = service.getById(scaleId, institutionId, "ADMIN");

        assertEquals(scaleId, response.getId());
    }

    @Test
    void getById_scaleWithoutInstitution_failsClosed() {
        when(gradingScaleRepository.findById(scaleId)).thenReturn(Optional.of(scale(null)));

        assertThrows(ForbiddenException.class,
                () -> service.getById(scaleId, institutionId, "INSTITUTION_ADMIN"));
    }

    @Test
    void getById_missingRow_throwsNotFound() {
        when(gradingScaleRepository.findById(scaleId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getById(scaleId, institutionId, "INSTITUTION_ADMIN"));
    }

    // ── update ──

    @Test
    void update_sameInstitution_savesChanges() {
        when(gradingScaleRepository.findById(scaleId)).thenReturn(Optional.of(scale(institutionId)));
        when(gradingScaleRepository.save(any(GradingScale.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(gradeBoundaryRepository.findByGradingScaleIdAndIsDeletedFalse(scaleId))
                .thenReturn(List.of());

        GradingScaleResponse response = service.update(scaleId, institutionId, "INSTITUTION_ADMIN", request());

        assertEquals("Updated", response.getName());
    }

    @Test
    void update_foreignInstitution_forbidden_neverSaves() {
        when(gradingScaleRepository.findById(scaleId))
                .thenReturn(Optional.of(scale(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.update(
                scaleId, institutionId, "INSTITUTION_ADMIN", request()));

        verify(gradingScaleRepository, never()).save(any(GradingScale.class));
    }

    @Test
    void update_platformAdmin_mayUpdateForeignScale() {
        when(gradingScaleRepository.findById(scaleId))
                .thenReturn(Optional.of(scale(otherInstitutionId)));
        when(gradingScaleRepository.save(any(GradingScale.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(gradeBoundaryRepository.findByGradingScaleIdAndIsDeletedFalse(scaleId))
                .thenReturn(List.of());

        GradingScaleResponse response = service.update(scaleId, institutionId, "ADMIN", request());

        assertEquals("Updated", response.getName());
    }

    @Test
    void update_missingRow_throwsNotFound() {
        when(gradingScaleRepository.findById(scaleId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(
                scaleId, institutionId, "INSTITUTION_ADMIN", request()));
    }

    // ── delete ──

    @Test
    void delete_sameInstitution_softDeletes() {
        GradingScale scale = scale(institutionId);
        when(gradingScaleRepository.findById(scaleId)).thenReturn(Optional.of(scale));
        when(gradingScaleRepository.save(any(GradingScale.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.delete(scaleId, institutionId, "INSTITUTION_ADMIN");

        assertTrue(scale.getIsDeleted());
        verify(gradingScaleRepository).save(scale);
    }

    @Test
    void delete_foreignInstitution_forbidden_neverSaves() {
        when(gradingScaleRepository.findById(scaleId))
                .thenReturn(Optional.of(scale(otherInstitutionId)));

        assertThrows(ForbiddenException.class,
                () -> service.delete(scaleId, institutionId, "INSTITUTION_ADMIN"));

        verify(gradingScaleRepository, never()).save(any(GradingScale.class));
    }

    @Test
    void delete_platformAdmin_mayDeleteForeignScale() {
        GradingScale scale = scale(otherInstitutionId);
        when(gradingScaleRepository.findById(scaleId)).thenReturn(Optional.of(scale));
        when(gradingScaleRepository.save(any(GradingScale.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.delete(scaleId, institutionId, "ADMIN");

        assertTrue(scale.getIsDeleted());
    }

    @Test
    void delete_missingRow_throwsNotFound() {
        when(gradingScaleRepository.findById(scaleId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.delete(scaleId, institutionId, "INSTITUTION_ADMIN"));
    }
}
