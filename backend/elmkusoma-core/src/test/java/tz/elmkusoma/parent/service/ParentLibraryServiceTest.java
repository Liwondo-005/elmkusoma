package tz.elmkusoma.parent.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.parent.dto.ParentLibraryResponse;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * B4 (teaching-learning ecosystem audit): the parent library must load only the
 * caller's institution resources — the legacy unscoped listing leaked every
 * tenant's library to this parent-facing endpoint.
 */
@ExtendWith(MockitoExtension.class)
class ParentLibraryServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @InjectMocks
    private ParentLibraryService service;

    @Test
    void getLibraryScopesResourcesToCallerInstitution() {
        UUID institutionId = UUID.randomUUID();
        Resource own = Resource.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .uploadedBy(UUID.randomUUID())
                .title("Algebra study guide")
                .resourceType(Resource.ResourceType.PDF)
                .visibility(Resource.ResourceVisibility.INSTITUTION)
                .sortOrder(0)
                .isDeleted(false)
                .createdBy("teacher@example.com")
                .build();
        when(resourceRepository.findByInstitutionIdAndIsDeletedFalse(institutionId))
                .thenReturn(List.of(own));

        ParentLibraryResponse response = service.getLibrary(institutionId);

        assertNotNull(response);
        assertFalse(response.getCategories().isEmpty(), "institution resources must surface as categories");
        verify(resourceRepository).findByInstitutionIdAndIsDeletedFalse(institutionId);
        verify(resourceRepository, never()).findAllAndIsDeletedFalse();
    }

    @Test
    void getLibraryReturnsEmptyCategoriesForEmptyInstitution() {
        UUID institutionId = UUID.randomUUID();
        when(resourceRepository.findByInstitutionIdAndIsDeletedFalse(institutionId))
                .thenReturn(List.of());

        ParentLibraryResponse response = service.getLibrary(institutionId);

        assertNotNull(response);
        assertTrue(response.getCategories().isEmpty());
        verify(resourceRepository, never()).findAllAndIsDeletedFalse();
    }
}
