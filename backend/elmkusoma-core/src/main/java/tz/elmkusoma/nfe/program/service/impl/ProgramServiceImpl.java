package tz.elmkusoma.nfe.program.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nfe.program.domain.NfeProgram;
import tz.elmkusoma.nfe.program.dto.ProgramRequest;
import tz.elmkusoma.nfe.program.dto.ProgramResponse;
import tz.elmkusoma.nfe.program.repository.NfeProgramRepository;
import tz.elmkusoma.nfe.program.service.ProgramService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ProgramServiceImpl implements ProgramService {

    private final NfeProgramRepository programRepository;

    @Override
    public ProgramResponse createProgram(UUID institutionId, ProgramRequest request) {
        NfeProgram program = NfeProgram.builder()
                .providerId(UUID.fromString(request.getProviderId()))
                .title(request.getTitle())
                .description(request.getDescription())
                .programType(NfeProgram.ProgramType.valueOf(request.getProgramType()))
                .category(request.getCategory())
                .targetAudience(request.getTargetAudience())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .maxParticipants(request.getMaxParticipants())
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .build();
        program.setInstitutionId(institutionId);

        NfeProgram saved = programRepository.save(program);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProgramResponse getProgram(UUID institutionId, UUID programId) {
        NfeProgram program = programRepository.findByIdAndInstitutionId(programId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("NFE Program", "id", programId));
        return mapToResponse(program);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProgramResponse> listPrograms(UUID institutionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<NfeProgram> allPrograms = programRepository.findAllByInstitutionId(institutionId);

        int start = Math.min(page * size, allPrograms.size());
        int end = Math.min((page + 1) * size, allPrograms.size());
        List<NfeProgram> paged = allPrograms.subList(start, end);

        Page<NfeProgram> programPage = new org.springframework.data.domain.PageImpl<>(
                paged, pageable, allPrograms.size());

        List<ProgramResponse> content = programPage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                programPage.getNumber(),
                programPage.getSize(),
                programPage.getTotalElements(),
                programPage.getTotalPages(),
                programPage.isFirst(),
                programPage.isLast()
        );
    }

    @Override
    public ProgramResponse updateProgram(UUID institutionId, UUID programId, ProgramRequest request) {
        NfeProgram program = programRepository.findByIdAndInstitutionId(programId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("NFE Program", "id", programId));

        if (request.getTitle() != null) program.setTitle(request.getTitle());
        if (request.getDescription() != null) program.setDescription(request.getDescription());
        if (request.getProgramType() != null) program.setProgramType(NfeProgram.ProgramType.valueOf(request.getProgramType()));
        if (request.getCategory() != null) program.setCategory(request.getCategory());
        if (request.getTargetAudience() != null) program.setTargetAudience(request.getTargetAudience());
        if (request.getStartDate() != null) program.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) program.setEndDate(request.getEndDate());
        if (request.getMaxParticipants() != null) program.setMaxParticipants(request.getMaxParticipants());
        if (request.getIsPublished() != null) program.setIsPublished(request.getIsPublished());

        NfeProgram saved = programRepository.save(program);
        return mapToResponse(saved);
    }

    @Override
    public void deleteProgram(UUID institutionId, UUID programId) {
        NfeProgram program = programRepository.findByIdAndInstitutionId(programId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("NFE Program", "id", programId));
        program.setIsDeleted(true);
        programRepository.save(program);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramResponse> getProgramsByProvider(UUID institutionId, UUID providerId) {
        return programRepository.findByProviderId(providerId).stream()
                .filter(p -> p.getInstitutionId().equals(institutionId))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProgramResponse> getPublishedPrograms(UUID institutionId) {
        return programRepository.findPublishedByInstitutionId(institutionId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ProgramResponse mapToResponse(NfeProgram program) {
        return ProgramResponse.builder()
                .id(program.getId())
                .institutionId(program.getInstitutionId())
                .providerId(program.getProviderId())
                .title(program.getTitle())
                .description(program.getDescription())
                .programType(program.getProgramType().name())
                .category(program.getCategory())
                .targetAudience(program.getTargetAudience())
                .startDate(program.getStartDate())
                .endDate(program.getEndDate())
                .maxParticipants(program.getMaxParticipants())
                .isPublished(program.getIsPublished())
                .createdAt(program.getCreatedAt())
                .build();
    }
}
