package tz.elmkusoma.grading.service;

import tz.elmkusoma.grading.dto.request.GenerateReportCardRequest;
import tz.elmkusoma.grading.dto.response.ReportCardResponse;

import java.util.List;
import java.util.UUID;

public interface ReportCardService {

    ReportCardResponse generate(UUID institutionId, GenerateReportCardRequest request);

    ReportCardResponse getById(UUID id);

    List<ReportCardResponse> getByStudentId(UUID studentId);

    List<ReportCardResponse> getByTermId(UUID termId);

    ReportCardResponse getByStudentAndTerm(UUID studentId, UUID termId);

    ReportCardResponse updateStatus(UUID id, String status);

    void calculateClassRanks(UUID termId);
}