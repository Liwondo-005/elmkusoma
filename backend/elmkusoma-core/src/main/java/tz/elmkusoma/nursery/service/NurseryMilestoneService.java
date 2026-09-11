package tz.elmkusoma.nursery.service;

import tz.elmkusoma.nursery.dto.request.CreateNurseryMilestoneRequest;
import tz.elmkusoma.nursery.dto.response.NurseryMilestoneResponse;

import java.util.List;
import java.util.UUID;

public interface NurseryMilestoneService {

    NurseryMilestoneResponse create(UUID institutionId, CreateNurseryMilestoneRequest request);

    List<NurseryMilestoneResponse> getByStudentId(UUID studentId);

    List<NurseryMilestoneResponse> getByStudentAndCategory(UUID studentId, String category);

    NurseryMilestoneResponse getById(UUID id);

    NurseryMilestoneResponse update(UUID id, CreateNurseryMilestoneRequest request);

    void delete(UUID id);
}