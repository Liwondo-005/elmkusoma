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
import tz.elmkusoma.grading.domain.GradeBoundary;
import tz.elmkusoma.grading.dto.request.CreateGradeBoundaryRequest;
import tz.elmkusoma.grading.dto.response.GradeBoundaryResponse;
import tz.elmkusoma.grading.repository.GradeBoundaryRepository;
import tz.elmkusoma.grading.service.impl.GradeBoundaryServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cross-tenant hardening of {@link GradeBoundaryServiceImpl}: read / update /
 * delete are institution-scoped through the shared {@link OwnershipGuard} (403),
 * INSTITUTION_ADMIN is NOT exempt, and only platform ADMIN bypasses the check.
 */
@ExtendWith(MockitoExtension.class)
class GradeBoundaryOwnershipSecurityTest {

    @Mock
    private GradeBoundaryRepository gradeBoundaryRepository;
    @Spy
    private OwnershipGuard ownershipGuard = new OwnershipGuard();

    @InjectMocks
    private GradeBoundaryServiceImpl service;

    private UUID boundaryId;
    private UUID scaleId;
    private UUID institutionId;
    private UUID otherInstitutionId;

    @BeforeEach
    void setUp() {
        boundaryId = UUID.randomUUID();
        scaleId = UUID.randomUUID();
        institutionId = UUID.randomUUID();
        otherInstitutionId = UUID.randomUUID();
    }

    private GradeBoundary boundary(UUID ownerInstitutionId) {
        return GradeBoundary.builder()
                .id(boundaryId)
                .gradingScaleId(scaleId)
                .institutionId(ownerInstitutionId)
                .gradeLabel("A")
                .gradeName("Excellent")
                .minPercentage(BigDecimal.valueOf(75))
                .maxPercentage(BigDecimal.valueOf(100))
                .gpaPoints(BigDecimal.valueOf(5))
                .sortOrder(1)
                .isDeleted(false)
                .build();
    }

    private CreateGradeBoundaryRequest request() {
        CreateGradeBoundaryRequest request = new CreateGradeBoundaryRequest();
        request.setGradingScaleId(scaleId);
        request.setGradeLabel("B");
        request.setMinPercentage(BigDecimal.valueOf(50));
        request.setMaxPercentage(BigDecimal.valueOf(74));
        return request;
    }

    // ── getById ──

    @Test
    void getById_sameInstitution_returnsBoundary() {
        when(gradeBoundaryRepository.findById(boundaryId))
                .thenReturn(Optional.of(boundary(institutionId)));

        GradeBoundaryResponse response = service.getById(boundaryId, institutionId, "INSTITUTION_ADMIN");

        assertEquals(boundaryId, response.getId());
        assertEquals("A", response.getGradeLabel());
    }

    @Test
    void getById_foreignInstitution_throwsForbidden() {
        when(gradeBoundaryRepository.findById(boundaryId))
                .thenReturn(Optional.of(boundary(otherInstitutionId)));

        assertThrows(ForbiddenException.class,
                () -> service.getById(boundaryId, institutionId, "INSTITUTION_ADMIN"));
    }

    @Test
    void getById_platformAdmin_bypassesInstitutionCheck() {
        when(gradeBoundaryRepository.findById(boundaryId))
                .thenReturn(Optional.of(boundary(otherInstitutionId)));

        GradeBoundaryResponse response = service.getById(boundaryId, institutionId, "ADMIN");

        assertEquals(boundaryId, response.getId());
    }

    @Test
    void getById_boundaryWithoutInstitution_failsClosed() {
        when(gradeBoundaryRepository.findById(boundaryId))
                .thenReturn(Optional.of(boundary(null)));

        assertThrows(ForbiddenException.class,
                () -> service.getById(boundaryId, institutionId, "INSTITUTION_ADMIN"));
    }

    @Test
    void getById_missingRow_throwsNotFound() {
        when(gradeBoundaryRepository.findById(boundaryId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getById(boundaryId, institutionId, "INSTITUTION_ADMIN"));
    }

    // ── getByGradingScaleId ──

    @Test
    void getByGradingScaleId_sameInstitution_returnsBoundaries() {
        when(gradeBoundaryRepository.findByGradingScaleIdAndIsDeletedFalse(scaleId))
                .thenReturn(List.of(boundary(institutionId)));

        List<GradeBoundaryResponse> response =
                service.getByGradingScaleId(scaleId, institutionId, "INSTITUTION_ADMIN");

        assertEquals(1, response.size());
        assertEquals("A", response.get(0).getGradeLabel());
    }

    @Test
    void getByGradingScaleId_foreignInstitution_throwsForbidden() {
        when(gradeBoundaryRepository.findByGradingScaleIdAndIsDeletedFalse(scaleId))
                .thenReturn(List.of(boundary(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () ->
                service.getByGradingScaleId(scaleId, institutionId, "INSTITUTION_ADMIN"));
    }

    @Test
    void getByGradingScaleId_platformAdmin_readsForeignBoundaries() {
        when(gradeBoundaryRepository.findByGradingScaleIdAndIsDeletedFalse(scaleId))
                .thenReturn(List.of(boundary(otherInstitutionId)));

        List<GradeBoundaryResponse> response =
                service.getByGradingScaleId(scaleId, institutionId, "ADMIN");

        assertEquals(1, response.size());
    }

    @Test
    void getByGradingScaleId_emptyList_returnsEmptyWithoutCheck() {
        when(gradeBoundaryRepository.findByGradingScaleIdAndIsDeletedFalse(scaleId))
                .thenReturn(List.of());

        List<GradeBoundaryResponse> response =
                service.getByGradingScaleId(scaleId, institutionId, "INSTITUTION_ADMIN");

        assertTrue(response.isEmpty());
        verifyNoInteractions(ownershipGuard);
    }

    // ── update ──

    @Test
    void update_sameInstitution_savesChanges() {
        when(gradeBoundaryRepository.findById(boundaryId))
                .thenReturn(Optional.of(boundary(institutionId)));
        when(gradeBoundaryRepository.save(any(GradeBoundary.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        GradeBoundaryResponse response = service.update(
                boundaryId, institutionId, "INSTITUTION_ADMIN", request());

        assertEquals("B", response.getGradeLabel());
    }

    @Test
    void update_foreignInstitution_forbidden_neverSaves() {
        when(gradeBoundaryRepository.findById(boundaryId))
                .thenReturn(Optional.of(boundary(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () -> service.update(
                boundaryId, institutionId, "INSTITUTION_ADMIN", request()));

        verify(gradeBoundaryRepository, never()).save(any(GradeBoundary.class));
    }

    @Test
    void update_missingRow_throwsNotFound() {
        when(gradeBoundaryRepository.findById(boundaryId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.update(
                boundaryId, institutionId, "INSTITUTION_ADMIN", request()));
    }

    // ── delete ──

    @Test
    void delete_sameInstitution_softDeletes() {
        GradeBoundary boundary = boundary(institutionId);
        when(gradeBoundaryRepository.findById(boundaryId)).thenReturn(Optional.of(boundary));
        when(gradeBoundaryRepository.save(any(GradeBoundary.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.delete(boundaryId, institutionId, "INSTITUTION_ADMIN");

        assertTrue(boundary.getIsDeleted());
        verify(gradeBoundaryRepository).save(boundary);
    }

    @Test
    void delete_foreignInstitution_forbidden_neverSaves() {
        when(gradeBoundaryRepository.findById(boundaryId))
                .thenReturn(Optional.of(boundary(otherInstitutionId)));

        assertThrows(ForbiddenException.class, () ->
                service.delete(boundaryId, institutionId, "INSTITUTION_ADMIN"));

        verify(gradeBoundaryRepository, never()).save(any(GradeBoundary.class));
    }

    @Test
    void delete_platformAdmin_mayDeleteForeignBoundary() {
        GradeBoundary boundary = boundary(otherInstitutionId);
        when(gradeBoundaryRepository.findById(boundaryId)).thenReturn(Optional.of(boundary));
        when(gradeBoundaryRepository.save(any(GradeBoundary.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.delete(boundaryId, institutionId, "ADMIN");

        assertTrue(boundary.getIsDeleted());
    }

    @Test
    void delete_missingRow_throwsNotFound() {
        when(gradeBoundaryRepository.findById(boundaryId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.delete(boundaryId, institutionId, "INSTITUTION_ADMIN"));
    }
}
