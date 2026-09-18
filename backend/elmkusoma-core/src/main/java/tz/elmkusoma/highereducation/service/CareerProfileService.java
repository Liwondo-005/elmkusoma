package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.highereducation.domain.CareerProfile;
import tz.elmkusoma.highereducation.dto.CareerProfileDTO;
import tz.elmkusoma.highereducation.repository.CareerProfileRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CareerProfileService {

    private final CareerProfileRepository repository;

    public CareerProfileDTO getStudentProfile(UUID studentId) {
        return repository.findByStudentIdAndIsDeletedFalse(studentId)
                .map(this::toDTO)
                .orElse(null);
    }

    public CareerProfileDTO createOrUpdate(UUID studentId, CareerProfileDTO dto) {
        CareerProfile existing = repository.findByStudentIdAndIsDeletedFalse(studentId).orElse(null);
        if (existing != null) {
            existing.setCareerObjective(dto.getCareerObjective());
            existing.setTargetIndustry(dto.getTargetIndustry());
            existing.setTargetRole(dto.getTargetRole());
            existing.setSkills(dto.getSkills());
            existing.setCertifications(dto.getCertifications());
            existing.setExperienceSummary(dto.getExperienceSummary());
            existing.setCvFileUrl(dto.getCvFileUrl());
            existing.setLinkedinUrl(dto.getLinkedinUrl());
            existing.setPortfolioUrl(dto.getPortfolioUrl());
            existing.setIsPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false);
            return toDTO(repository.save(existing));
        }
        CareerProfile entity = CareerProfile.builder()
                .studentId(studentId)
                .careerObjective(dto.getCareerObjective())
                .targetIndustry(dto.getTargetIndustry())
                .targetRole(dto.getTargetRole())
                .skills(dto.getSkills())
                .certifications(dto.getCertifications())
                .experienceSummary(dto.getExperienceSummary())
                .cvFileUrl(dto.getCvFileUrl())
                .linkedinUrl(dto.getLinkedinUrl())
                .portfolioUrl(dto.getPortfolioUrl())
                .isPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false)
                .institutionId(dto.getInstitutionId())
                .build();
        return toDTO(repository.save(entity));
    }

    private CareerProfileDTO toDTO(CareerProfile e) {
        return CareerProfileDTO.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .careerObjective(e.getCareerObjective())
                .targetIndustry(e.getTargetIndustry())
                .targetRole(e.getTargetRole())
                .skills(e.getSkills())
                .certifications(e.getCertifications())
                .experienceSummary(e.getExperienceSummary())
                .cvFileUrl(e.getCvFileUrl())
                .linkedinUrl(e.getLinkedinUrl())
                .portfolioUrl(e.getPortfolioUrl())
                .isPublic(e.getIsPublic())
                .institutionId(e.getInstitutionId())
                .build();
    }
}
