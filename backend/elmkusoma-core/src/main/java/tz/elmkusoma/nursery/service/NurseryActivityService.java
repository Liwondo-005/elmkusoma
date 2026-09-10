package tz.elmkusoma.nursery.service;

import tz.elmkusoma.nursery.dto.request.CreateNurseryActivityRequest;
import tz.elmkusoma.nursery.dto.response.NurseryActivityResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface NurseryActivityService {

    NurseryActivityResponse create(UUID institutionId, UUID conductedBy, CreateNurseryActivityRequest request);

    List<NurseryActivityResponse> getByClassGroupId(UUID classGroupId, UUID institutionId);

    List<NurseryActivityResponse> getByClassAndDate(UUID classGroupId, LocalDate date, UUID institutionId);

    NurseryActivityResponse getById(UUID id, UUID institutionId);

    NurseryActivityResponse update(UUID id, UUID institutionId, CreateNurseryActivityRequest request);

    void delete(UUID id, UUID institutionId);

    List<NurseryActivityResponse> getByType(String activityType, UUID institutionId);
}