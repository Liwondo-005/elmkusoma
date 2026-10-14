package tz.elmkusoma.nursery.service;

import tz.elmkusoma.nursery.dto.request.CreateNurseryMilestoneRequest;
import tz.elmkusoma.nursery.dto.response.NurseryMilestoneResponse;

import java.util.List;
import java.util.UUID;

/**
 * Reads and writes are tenant-scoped: every method takes the caller's
 * institutionId (server-derived) and fails closed across tenants (403).
 */
public interface NurseryMilestoneService {

    NurseryMilestoneResponse create(UUID institutionId, CreateNurseryMilestoneRequest request);

    List<NurseryMilestoneResponse> getByStudentId(UUID institutionId, UUID studentId);

    List<NurseryMilestoneResponse> getByStudentAndCategory(UUID institutionId, UUID studentId, String category);

    NurseryMilestoneResponse getById(UUID institutionId, UUID id);

    NurseryMilestoneResponse update(UUID institutionId, UUID id, CreateNurseryMilestoneRequest request);

    void delete(UUID institutionId, UUID id);
}