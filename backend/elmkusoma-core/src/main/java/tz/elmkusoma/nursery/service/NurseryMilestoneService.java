package tz.elmkusoma.nursery.service;

import tz.elmkusoma.nursery.dto.request.CreateNurseryMilestoneRequest;
import tz.elmkusoma.nursery.dto.response.NurseryMilestoneResponse;

import java.util.List;
import java.util.UUID;

public interface NurseryMilestoneService {

    NurseryMilestoneResponse create(UUID institutionId, CreateNurseryMilestoneRequest request);

    List<NurseryMilestoneResponse> getByStudentId(UUID studentId, UUID institutionId);

    List<NurseryMilestoneResponse> getByStudentAndCategory(UUID studentId, String category, UUID institutionId);

    NurseryMilestoneResponse getById(UUID id, UUID institutionId);

    NurseryMilestoneResponse update(UUID id, UUID institutionId, CreateNurseryMilestoneRequest request);

    void delete(UUID id, UUID institutionId);
}