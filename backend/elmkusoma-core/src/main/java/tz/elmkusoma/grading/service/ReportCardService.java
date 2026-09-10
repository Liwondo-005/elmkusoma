package tz.elmkusoma.grading.service;

import tz.elmkusoma.grading.dto.request.GenerateReportCardRequest;
import tz.elmkusoma.grading.dto.response.ReportCardResponse;

import java.util.List;
import java.util.UUID;

public interface ReportCardService {

    ReportCardResponse generate(UUID institutionId, GenerateReportCardRequest request);

    ReportCardResponse getById(UUID id, UUID institutionId);

    List<ReportCardResponse> getByStudentId(UUID studentId, UUID institutionId);

    List<ReportCardResponse> getByTermId(UUID termId, UUID institutionId);

    ReportCardResponse getByStudentAndTerm(UUID studentId, UUID termId, UUID institutionId);

    ReportCardResponse updateStatus(UUID id, UUID institutionId, String status);

    void calculateClassRanks(UUID termId);
}