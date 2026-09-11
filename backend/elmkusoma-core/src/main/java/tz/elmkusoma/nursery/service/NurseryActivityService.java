package tz.elmkusoma.nursery.service;

import tz.elmkusoma.nursery.dto.request.CreateNurseryActivityRequest;
import tz.elmkusoma.nursery.dto.response.NurseryActivityResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface NurseryActivityService {

    NurseryActivityResponse create(UUID institutionId, UUID conductedBy, CreateNurseryActivityRequest request);

    List<NurseryActivityResponse> getByClassGroupId(UUID classGroupId);

    List<NurseryActivityResponse> getByClassAndDate(UUID classGroupId, LocalDate date);

    NurseryActivityResponse getById(UUID id);

    NurseryActivityResponse update(UUID id, CreateNurseryActivityRequest request);

    void delete(UUID id);

    List<NurseryActivityResponse> getByType(String activityType);
}