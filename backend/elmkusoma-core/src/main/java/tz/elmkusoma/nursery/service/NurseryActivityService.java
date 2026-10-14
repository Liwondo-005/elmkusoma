package tz.elmkusoma.nursery.service;

import tz.elmkusoma.nursery.dto.request.CreateNurseryActivityRequest;
import tz.elmkusoma.nursery.dto.response.NurseryActivityResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Reads and writes are tenant-scoped: every method takes the caller's
 * institutionId (server-derived) and fails closed across tenants (403).
 */
public interface NurseryActivityService {

    NurseryActivityResponse create(UUID institutionId, UUID conductedBy, CreateNurseryActivityRequest request);

    List<NurseryActivityResponse> getByClassGroupId(UUID institutionId, UUID classGroupId);

    List<NurseryActivityResponse> getByClassAndDate(UUID institutionId, UUID classGroupId, LocalDate date);

    NurseryActivityResponse getById(UUID institutionId, UUID id);

    NurseryActivityResponse update(UUID institutionId, UUID id, CreateNurseryActivityRequest request);

    void delete(UUID institutionId, UUID id);

    List<NurseryActivityResponse> getByType(UUID institutionId, String activityType);
}