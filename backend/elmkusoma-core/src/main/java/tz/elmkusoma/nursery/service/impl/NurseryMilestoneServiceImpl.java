package tz.elmkusoma.nursery.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.exception.ResourceNotFoundException;
import tz.elmkusoma.nursery.domain.NurseryMilestone;
import tz.elmkusoma.nursery.dto.request.CreateNurseryMilestoneRequest;
import tz.elmkusoma.nursery.dto.response.NurseryMilestoneResponse;
import tz.elmkusoma.nursery.repository.NurseryMilestoneRepository;
import tz.elmkusoma.nursery.service.NurseryMilestoneService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class NurseryMilestoneServiceImpl implements NurseryMilestoneService {

    private final NurseryMilestoneRepository nurseryMilestoneRepository;

    @Override
    public NurseryMilestoneResponse create(UUID institutionId, CreateNurseryMilestoneRequest request) {
        NurseryMilestone milestone = NurseryMilestone.builder()
                .institutionId(institutionId)
                .studentId(request.getStudentId())
                .category(NurseryMilestone.MilestoneCategory.valueOf(request.getCategory()))
                .milestoneName(request.getMilestoneName())
                .description(request.getDescription())
                .expectedAgeMonths(request.getExpectedAgeMonths())
                .achievedDate(request.getAchievedDate())
                .status(NurseryMilestone.MilestoneStatus.valueOf(request.getStatus()))
                .evidenceNotes(request.getEvidenceNotes())
                .build();

        NurseryMilestone saved = nurseryMilestoneRepository.save(milestone);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NurseryMilestoneResponse> getByStudentId(UUID studentId) {
        return nurseryMilestoneRepository.findByStudentIdAndIsDeletedFalse(studentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<NurseryMilestoneResponse> getByStudentAndCategory(UUID studentId, String category) {
        return nurseryMilestoneRepository.findByStudentAndCategory(
                        studentId, NurseryMilestone.MilestoneCategory.valueOf(category))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public NurseryMilestoneResponse getById(UUID id) {
        NurseryMilestone milestone = nurseryMilestoneRepository.findById(id)
                .filter(m -> !m.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Nursery milestone not found"));
        return mapToResponse(milestone);
    }

    @Override
    public NurseryMilestoneResponse update(UUID id, CreateNurseryMilestoneRequest request) {
        NurseryMilestone milestone = nurseryMilestoneRepository.findById(id)
                .filter(m -> !m.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Nursery milestone not found"));

        milestone.setCategory(NurseryMilestone.MilestoneCategory.valueOf(request.getCategory()));
        milestone.setMilestoneName(request.getMilestoneName());
        milestone.setDescription(request.getDescription());
        milestone.setExpectedAgeMonths(request.getExpectedAgeMonths());
        milestone.setAchievedDate(request.getAchievedDate());
        milestone.setStatus(NurseryMilestone.MilestoneStatus.valueOf(request.getStatus()));
        milestone.setEvidenceNotes(request.getEvidenceNotes());

        NurseryMilestone saved = nurseryMilestoneRepository.save(milestone);
        return mapToResponse(saved);
    }

    @Override
    public void delete(UUID id) {
        NurseryMilestone milestone = nurseryMilestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nursery milestone not found"));
        milestone.setIsDeleted(true);
        nurseryMilestoneRepository.save(milestone);
    }

    private NurseryMilestoneResponse mapToResponse(NurseryMilestone milestone) {
        return NurseryMilestoneResponse.builder()
                .id(milestone.getId())
                .studentId(milestone.getStudentId())
                .category(milestone.getCategory().name())
                .milestoneName(milestone.getMilestoneName())
                .description(milestone.getDescription())
                .expectedAgeMonths(milestone.getExpectedAgeMonths())
                .achievedDate(milestone.getAchievedDate())
                .status(milestone.getStatus().name())
                .observedBy(milestone.getObservedBy())
                .evidenceNotes(milestone.getEvidenceNotes())
                .build();
    }
}