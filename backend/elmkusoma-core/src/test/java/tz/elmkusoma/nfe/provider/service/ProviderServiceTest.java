package tz.elmkusoma.nfe.provider.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nfe.provider.domain.EducationProvider;
import tz.elmkusoma.nfe.provider.dto.ProviderRequest;
import tz.elmkusoma.nfe.provider.dto.ProviderResponse;
import tz.elmkusoma.nfe.provider.repository.EducationProviderRepository;
import tz.elmkusoma.nfe.provider.service.impl.ProviderServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    @Mock
    private EducationProviderRepository providerRepository;

    @InjectMocks
    private ProviderServiceImpl providerService;

    private UUID institutionId;
    private UUID providerId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        providerId = UUID.randomUUID();
    }

    @Test
    void createProvider_shouldSaveAndReturnResponse() {
        ProviderRequest request = ProviderRequest.builder()
                .name("Tech Academy")
                .providerType("TRAINING")
                .description("Technology training provider")
                .email("info@techacademy.com")
                .phone("+255123456789")
                .build();

        when(providerRepository.existsByNameAndInstitutionIdAndIsDeletedFalse("Tech Academy", institutionId))
                .thenReturn(false);

        EducationProvider savedProvider = EducationProvider.builder()
                .name("Tech Academy")
                .providerType(EducationProvider.ProviderType.TRAINING)
                .description("Technology training provider")
                .email("info@techacademy.com")
                .phone("+255123456789")
                .isActive(true)
                .isVerified(false)
                .build();
        savedProvider.setId(providerId);
        savedProvider.setInstitutionId(institutionId);

        when(providerRepository.save(any(EducationProvider.class))).thenReturn(savedProvider);

        ProviderResponse response = providerService.createProvider(institutionId, request);

        assertNotNull(response);
        assertEquals("Tech Academy", response.getName());
        assertEquals("TRAINING", response.getProviderType());
        assertEquals(true, response.getIsActive());
        assertEquals(false, response.getIsVerified());
        assertEquals(institutionId, response.getInstitutionId());
        verify(providerRepository).save(any(EducationProvider.class));
    }

    @Test
    void createProvider_whenNameExists_shouldThrow() {
        ProviderRequest request = ProviderRequest.builder()
                .name("Tech Academy")
                .providerType("TRAINING")
                .build();

        when(providerRepository.existsByNameAndInstitutionIdAndIsDeletedFalse("Tech Academy", institutionId))
                .thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> providerService.createProvider(institutionId, request));
        assertTrue(exception.getMessage().contains("already exists"));
    }

    @Test
    void getProvider_shouldReturnScopedProvider() {
        EducationProvider provider = EducationProvider.builder()
                .name("Tech Academy")
                .providerType(EducationProvider.ProviderType.TRAINING)
                .isActive(true)
                .build();
        provider.setId(providerId);
        provider.setInstitutionId(institutionId);

        when(providerRepository.findByIdAndInstitutionId(providerId, institutionId))
                .thenReturn(Optional.of(provider));

        ProviderResponse response = providerService.getProvider(institutionId, providerId);

        assertNotNull(response);
        assertEquals("Tech Academy", response.getName());
        assertEquals(providerId, response.getId());
        assertEquals(institutionId, response.getInstitutionId());
    }

    @Test
    void getProvider_whenNotFound_shouldThrow() {
        UUID nonExistentId = UUID.randomUUID();
        when(providerRepository.findByIdAndInstitutionId(nonExistentId, institutionId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> providerService.getProvider(institutionId, nonExistentId));
    }

    @Test
    void updateProvider_shouldUpdateFieldsAndSave() {
        EducationProvider provider = EducationProvider.builder()
                .name("Tech Academy")
                .providerType(EducationProvider.ProviderType.TRAINING)
                .description("Old description")
                .isActive(true)
                .build();
        provider.setId(providerId);
        provider.setInstitutionId(institutionId);

        when(providerRepository.findByIdAndInstitutionId(providerId, institutionId))
                .thenReturn(Optional.of(provider));
        when(providerRepository.save(any(EducationProvider.class))).thenReturn(provider);

        ProviderRequest request = ProviderRequest.builder()
                .description("Updated description")
                .phone("+255987654321")
                .build();

        ProviderResponse response = providerService.updateProvider(institutionId, providerId, request);

        assertNotNull(response);
        assertEquals("Updated description", response.getDescription());
        assertEquals("+255987654321", response.getPhone());
        verify(providerRepository).save(any(EducationProvider.class));
    }

    @Test
    void deleteProvider_shouldSoftDelete() {
        EducationProvider provider = EducationProvider.builder()
                .name("Tech Academy")
                .providerType(EducationProvider.ProviderType.TRAINING)
                .build();
        provider.setId(providerId);
        provider.setInstitutionId(institutionId);
        provider.setIsDeleted(false);

        when(providerRepository.findByIdAndInstitutionId(providerId, institutionId))
                .thenReturn(Optional.of(provider));
        when(providerRepository.save(any(EducationProvider.class))).thenReturn(provider);

        providerService.deleteProvider(institutionId, providerId);

        assertTrue(provider.getIsDeleted());
        verify(providerRepository).save(provider);
    }

    @Test
    void deleteProvider_whenNotFound_shouldThrow() {
        UUID nonExistentId = UUID.randomUUID();
        when(providerRepository.findByIdAndInstitutionId(nonExistentId, institutionId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> providerService.deleteProvider(institutionId, nonExistentId));
    }

    @Test
    void getActiveProviders_shouldReturnOnlyActiveProviders() {
        EducationProvider active1 = EducationProvider.builder()
                .name("Active Provider 1")
                .providerType(EducationProvider.ProviderType.TRAINING)
                .isActive(true)
                .build();
        active1.setId(UUID.randomUUID());

        EducationProvider active2 = EducationProvider.builder()
                .name("Active Provider 2")
                .providerType(EducationProvider.ProviderType.COMPANY)
                .isActive(true)
                .build();
        active2.setId(UUID.randomUUID());

        when(providerRepository.findActiveByInstitutionId(institutionId))
                .thenReturn(List.of(active1, active2));

        List<ProviderResponse> responses = providerService.getActiveProviders(institutionId);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        verify(providerRepository).findActiveByInstitutionId(institutionId);
    }

    @Test
    void listProviders_shouldReturnPagedResults() {
        EducationProvider provider = EducationProvider.builder()
                .name("Tech Academy")
                .providerType(EducationProvider.ProviderType.TRAINING)
                .isActive(true)
                .build();
        provider.setId(providerId);
        provider.setInstitutionId(institutionId);

        when(providerRepository.findAllByInstitutionId(institutionId))
                .thenReturn(List.of(provider));

        PageResponse<ProviderResponse> response = providerService.listProviders(institutionId, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(1, response.getTotalElements());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }
}
