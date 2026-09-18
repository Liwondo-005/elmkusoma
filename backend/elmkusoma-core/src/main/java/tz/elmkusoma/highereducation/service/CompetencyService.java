package tz.elmkusoma.highereducation.service;

import tz.elmkusoma.highereducation.dto.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface CompetencyService {

    List<CompetencyDTO> getAllCompetencies(UUID institutionId);

    CompetencyDTO getCompetencyById(UUID id, UUID institutionId);

    CompetencyDTO createCompetency(UUID institutionId, CompetencyDTO dto);

    CompetencyDTO updateCompetency(UUID id, UUID institutionId, CompetencyDTO dto);

    void deleteCompetency(UUID id, UUID institutionId);

    List<StudentCompetencyDTO> getStudentCompetencies(UUID studentId, UUID institutionId);

    CompetencyRecordDTO updateCompetencyRecord(UUID studentId, UUID competencyId, String status, String evidence, UUID assessedBy);

    CompetencySummaryDTO getCompetencySummary(UUID studentId, UUID institutionId);

    CompetencyAssessmentDTO linkAssessmentToCompetency(UUID competencyId, UUID assessmentId, Integer weight);

    List<CompetencyDTO> getCompetenciesForSubject(UUID subjectId);
}
