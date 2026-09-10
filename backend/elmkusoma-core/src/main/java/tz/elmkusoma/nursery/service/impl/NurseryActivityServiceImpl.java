package tz.elmkusoma.nursery.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.security.OwnershipGuard;
import tz.elmkusoma.nursery.domain.NurseryActivity;
import tz.elmkusoma.nursery.dto.request.CreateNurseryActivityRequest;
import tz.elmkusoma.nursery.dto.response.NurseryActivityResponse;
import tz.elmkusoma.nursery.repository.NurseryActivityRepository;
import tz.elmkusoma.nursery.service.NurseryActivityService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NurseryActivityServiceImpl implements NurseryActivityService {

    private final NurseryActivityRepository nurseryActivityRepository;

    @Override
    public NurseryActivityResponse create(UUID institutionId, UUID conductedBy, CreateNurseryActivityRequest request) {
        NurseryActivity activity = NurseryActivity.builder()
                .institutionId(institutionId)
                .classGroupId(request.getClassGroupId())
                .activityName(request.getActivityName())
                .activityType(NurseryActivity.ActivityType.valueOf(request.getActivityType()))
                .description(request.getDescription())
                .instructions(request.getInstructions())
                .durationMinutes(request.getDurationMinutes())
                .maxParticipants(request.getMaxParticipants())
                .materialsNeeded(request.getMaterialsNeeded())
                .learningObjectives(request.getLearningObjectives())
                .ageGroup(request.getAgeGroup())
                .activityDate(request.getActivityDate())
                .conductedBy(conductedBy)
                .build();

        NurseryActivity saved = nurseryActivityRepository.save(activity);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NurseryActivityResponse> getByClassGroupId(UUID classGroupId, UUID institutionId) {
        return nurseryActivityRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId)
                .stream()
                .filter(a -> a.getInstitutionId().equals(institutionId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NurseryActivityResponse> getByClassAndDate(UUID classGroupId, java.time.LocalDate date, UUID institutionId) {
        return nurseryActivityRepository.findByClassGroupIdAndActivityDateAndIsDeletedFalse(classGroupId, date)
                .stream()
                .filter(a -> a.getInstitutionId().equals(institutionId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public NurseryActivityResponse getById(UUID id, UUID institutionId) {
        NurseryActivity activity = nurseryActivityRepository.findById(id)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Nursery activity not found"));
        OwnershipGuard.verifyInstitution(activity.getInstitutionId(), institutionId, "nursery activity");
        return mapToResponse(activity);
    }

    @Override
    public NurseryActivityResponse update(UUID id, UUID institutionId, CreateNurseryActivityRequest request) {
        NurseryActivity activity = nurseryActivityRepository.findById(id)
                .filter(a -> !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Nursery activity not found"));
        OwnershipGuard.verifyInstitution(activity.getInstitutionId(), institutionId, "nursery activity");

        activity.setActivityName(request.getActivityName());
        activity.setActivityType(NurseryActivity.ActivityType.valueOf(request.getActivityType()));
        activity.setDescription(request.getDescription());
        activity.setInstructions(request.getInstructions());
        activity.setDurationMinutes(request.getDurationMinutes());
        activity.setMaxParticipants(request.getMaxParticipants());
        activity.setMaterialsNeeded(request.getMaterialsNeeded());
        activity.setLearningObjectives(request.getLearningObjectives());
        activity.setAgeGroup(request.getAgeGroup());
        activity.setActivityDate(request.getActivityDate());

        NurseryActivity saved = nurseryActivityRepository.save(activity);
        return mapToResponse(saved);
    }

    @Override
    public void delete(UUID id, UUID institutionId) {
        NurseryActivity activity = nurseryActivityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nursery activity not found"));
        OwnershipGuard.verifyInstitution(activity.getInstitutionId(), institutionId, "nursery activity");
        activity.setIsDeleted(true);
        nurseryActivityRepository.save(activity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NurseryActivityResponse> getByType(String activityType, UUID institutionId) {
        return nurseryActivityRepository.findByActivityTypeAndIsDeletedFalse(
                        NurseryActivity.ActivityType.valueOf(activityType))
                .stream()
                .filter(a -> a.getInstitutionId().equals(institutionId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private NurseryActivityResponse mapToResponse(NurseryActivity activity) {
        return NurseryActivityResponse.builder()
                .id(activity.getId())
                .classGroupId(activity.getClassGroupId())
                .activityName(activity.getActivityName())
                .activityType(activity.getActivityType().name())
                .description(activity.getDescription())
                .instructions(activity.getInstructions())
                .durationMinutes(activity.getDurationMinutes())
                .maxParticipants(activity.getMaxParticipants())
                .materialsNeeded(activity.getMaterialsNeeded())
                .learningObjectives(activity.getLearningObjectives())
                .ageGroup(activity.getAgeGroup())
                .activityDate(activity.getActivityDate())
                .status(activity.getStatus().name())
                .createdAt(activity.getCreatedAt())
                .build();
    }
}