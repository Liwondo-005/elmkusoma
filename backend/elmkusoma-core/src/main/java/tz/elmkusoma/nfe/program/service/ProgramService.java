package tz.elmkusoma.nfe.program.service;

import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.nfe.program.dto.ProgramRequest;
import tz.elmkusoma.nfe.program.dto.ProgramResponse;

import java.util.List;
import java.util.UUID;

public interface ProgramService {

    ProgramResponse createProgram(UUID institutionId, ProgramRequest request);

    ProgramResponse getProgram(UUID institutionId, UUID programId);

    PageResponse<ProgramResponse> listPrograms(UUID institutionId, int page, int size);

    ProgramResponse updateProgram(UUID institutionId, UUID programId, ProgramRequest request);

    void deleteProgram(UUID institutionId, UUID programId);

    List<ProgramResponse> getProgramsByProvider(UUID institutionId, UUID providerId);

    List<ProgramResponse> getPublishedPrograms(UUID institutionId);
}
