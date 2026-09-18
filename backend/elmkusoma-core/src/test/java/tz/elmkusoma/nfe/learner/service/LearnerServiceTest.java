package tz.elmkusoma.nfe.learner.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.common.OwnershipGuard;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nfe.learner.domain.NfeLearner;
import tz.elmkusoma.nfe.learner.dto.LearnerRequest;
import tz.elmkusoma.nfe.learner.dto.LearnerResponse;
import tz.elmkusoma.nfe.learner.repository.NfeLearnerRepository;
import tz.elmkusoma.nfe.learner.service.impl.LearnerServiceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LearnerServiceTest {

    @Mock
    private NfeLearnerRepository learnerRepository;
    @Mock
    private OwnershipGuard ownershipGuard;

    @InjectMocks
    private LearnerServiceImpl learnerService;

    private UUID institutionId;
    private UUID learnerId;
    private UUID providerId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        learnerId = UUID.randomUUID();
        providerId = UUID.randomUUID();
        userId = UUID.randomUUID();
    }

    @Test
    void createLearner_shouldSaveAndReturnResponse() {
        LearnerRequest request = LearnerRequest.builder()
                .providerId(providerId)
                .userId(userId)
                .participantNumber("P001")
                .occupation("Teacher")
                .organization("School A")
                .status("ACTIVE")
                .build();

        NfeLearner savedLearner = NfeLearner.builder()
                .providerId(providerId)
                .userId(userId)
                .participantNumber("P001")
                .occupation("Teacher")
                .organization("School A")
                .status("ACTIVE")
                .build();
        savedLearner.setId(learnerId);
        savedLearner.setInstitutionId(institutionId);

        when(learnerRepository.save(any(NfeLearner.class))).thenReturn(savedLearner);

        LearnerResponse response = learnerService.createLearner(institutionId, request);

        assertNotNull(response);
        assertEquals(providerId, response.getProviderId());
        assertEquals(userId, response.getUserId());
        assertEquals("P001", response.getParticipantNumber());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(institutionId, response.getInstitutionId());
        verify(learnerRepository).save(any(NfeLearner.class));
    }

    @Test
    void createLearner_whenStatusNull_shouldDefaultToActive() {
        LearnerRequest request = LearnerRequest.builder()
                .providerId(providerId)
                .userId(userId)
                .participantNumber("P001")
                .build();

        NfeLearner savedLearner = NfeLearner.builder()
                .providerId(providerId)
                .userId(userId)
                .participantNumber("P001")
                .status("ACTIVE")
                .build();
        savedLearner.setId(learnerId);
        savedLearner.setInstitutionId(institutionId);

        when(learnerRepository.save(any(NfeLearner.class))).thenReturn(savedLearner);

        LearnerResponse response = learnerService.createLearner(institutionId, request);

        assertNotNull(response);
        assertEquals("ACTIVE", response.getStatus());
    }

    @Test
    void getLearner_shouldVerifyOwnershipAndReturn() {
        NfeLearner learner = NfeLearner.builder()
                .providerId(providerId)
                .userId(userId)
                .participantNumber("P001")
                .status("ACTIVE")
                .build();
        learner.setId(learnerId);
        learner.setInstitutionId(institutionId);

        when(learnerRepository.findByIdAndInstitutionId(learnerId, institutionId))
                .thenReturn(Optional.of(learner));

        LearnerResponse response = learnerService.getLearner(institutionId, learnerId);

        assertNotNull(response);
        assertEquals(learnerId, response.getId());
        verify(ownershipGuard).verifyInstitution(institutionId, institutionId);
    }

    @Test
    void getLearner_whenNotFound_shouldThrow() {
        UUID nonExistentId = UUID.randomUUID();
        when(learnerRepository.findByIdAndInstitutionId(nonExistentId, institutionId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> learnerService.getLearner(institutionId, nonExistentId));
    }

    @Test
    void updateLearner_shouldUpdateFieldsAndSave() {
        NfeLearner learner = NfeLearner.builder()
                .providerId(providerId)
                .userId(userId)
                .participantNumber("P001")
                .occupation("Teacher")
                .status("ACTIVE")
                .build();
        learner.setId(learnerId);
        learner.setInstitutionId(institutionId);

        when(learnerRepository.findByIdAndInstitutionId(learnerId, institutionId))
                .thenReturn(Optional.of(learner));
        when(learnerRepository.save(any(NfeLearner.class))).thenReturn(learner);

        LearnerRequest request = LearnerRequest.builder()
                .occupation("Principal")
                .organization("School B")
                .build();

        LearnerResponse response = learnerService.updateLearner(institutionId, learnerId, request);

        assertNotNull(response);
        assertEquals("Principal", response.getOccupation());
        assertEquals("School B", response.getOrganization());
        verify(ownershipGuard).verifyInstitution(institutionId, institutionId);
        verify(learnerRepository).save(any(NfeLearner.class));
    }

    @Test
    void deleteLearner_shouldSoftDelete() {
        NfeLearner learner = NfeLearner.builder()
                .providerId(providerId)
                .userId(userId)
                .status("ACTIVE")
                .build();
        learner.setId(learnerId);
        learner.setInstitutionId(institutionId);
        learner.setIsDeleted(false);

        when(learnerRepository.findByIdAndInstitutionId(learnerId, institutionId))
                .thenReturn(Optional.of(learner));
        when(learnerRepository.save(any(NfeLearner.class))).thenReturn(learner);

        learnerService.deleteLearner(institutionId, learnerId);

        assertTrue(learner.getIsDeleted());
        verify(ownershipGuard).verifyInstitution(institutionId, institutionId);
        verify(learnerRepository).save(learner);
    }

    @Test
    void getLearnersByProvider_shouldReturnFilteredList() {
        NfeLearner learner1 = NfeLearner.builder()
                .providerId(providerId)
                .userId(userId)
                .status("ACTIVE")
                .build();
        learner1.setId(UUID.randomUUID());
        learner1.setInstitutionId(institutionId);

        NfeLearner learner2 = NfeLearner.builder()
                .providerId(providerId)
                .userId(UUID.randomUUID())
                .status("ACTIVE")
                .build();
        learner2.setId(UUID.randomUUID());
        learner2.setInstitutionId(institutionId);

        when(learnerRepository.findByProviderId(providerId))
                .thenReturn(List.of(learner1, learner2));

        List<LearnerResponse> responses = learnerService.getLearnersByProvider(institutionId, providerId);

        assertNotNull(responses);
        assertEquals(2, responses.size());
    }

    @Test
    void getLearnersByProvider_shouldFilterByInstitution() {
        NfeLearner learner1 = NfeLearner.builder()
                .providerId(providerId)
                .userId(userId)
                .status("ACTIVE")
                .build();
        learner1.setId(UUID.randomUUID());
        learner1.setInstitutionId(institutionId);

        NfeLearner learnerFromOtherInst = NfeLearner.builder()
                .providerId(providerId)
                .userId(UUID.randomUUID())
                .status("ACTIVE")
                .build();
        learnerFromOtherInst.setId(UUID.randomUUID());
        learnerFromOtherInst.setInstitutionId(UUID.randomUUID());

        when(learnerRepository.findByProviderId(providerId))
                .thenReturn(List.of(learner1, learnerFromOtherInst));

        List<LearnerResponse> responses = learnerService.getLearnersByProvider(institutionId, providerId);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(institutionId, responses.get(0).getInstitutionId());
    }

    @Test
    void listLearners_shouldReturnPagedResults() {
        NfeLearner learner = NfeLearner.builder()
                .providerId(providerId)
                .userId(userId)
                .status("ACTIVE")
                .build();
        learner.setId(learnerId);
        learner.setInstitutionId(institutionId);

        when(learnerRepository.findAllByInstitutionId(institutionId))
                .thenReturn(List.of(learner));

        PageResponse<LearnerResponse> response = learnerService.listLearners(institutionId, 0, 10);

        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertEquals(1, response.getTotalElements());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }
}
