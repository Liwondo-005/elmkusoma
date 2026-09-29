package tz.elmkusoma.grading.service;

import tz.elmkusoma.grading.dto.request.GenerateReportCardRequest;
import tz.elmkusoma.grading.dto.response.ReportCardResponse;

import java.util.List;
import java.util.UUID;

public interface ReportCardService {

    /**
     * Generate (or regenerate) a report card for a student in a term. Aggregates the
     * student's graded assignment submissions and assessment results, computes total,
     * average percentage, overall grade and GPA from the grading scale boundaries, and
     * refreshes per-class ranks for the term.
     */
    ReportCardResponse generate(UUID institutionId, GenerateReportCardRequest request);

    ReportCardResponse getById(UUID id);

    List<ReportCardResponse> getByStudentId(UUID studentId);

    List<ReportCardResponse> getByTermId(UUID termId);

    ReportCardResponse getByStudentAndTerm(UUID studentId, UUID termId);

    /** Batch read: every report card of the given students (one query, for class lists). */
    List<ReportCardResponse> getByStudentIds(List<UUID> studentIds);

    /** Update status; PUBLISHED releases the card (notifies the learner, audited). */
    ReportCardResponse updateStatus(UUID id, String status, String userEmail, String userRole);

    void calculateClassRanks(UUID termId);
}
