package tz.elmkusoma.grading.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.exception.ResourceNotFoundException;
import tz.elmkusoma.grading.domain.ReportCard;
import tz.elmkusoma.grading.dto.request.GenerateReportCardRequest;
import tz.elmkusoma.grading.dto.response.ReportCardResponse;
import tz.elmkusoma.grading.repository.ReportCardRepository;
import tz.elmkusoma.grading.service.ReportCardService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportCardServiceImpl implements ReportCardService {

    private final ReportCardRepository reportCardRepository;

    @Override
    public ReportCardResponse generate(UUID institutionId, GenerateReportCardRequest request) {
        ReportCard reportCard = ReportCard.builder()
                .studentId(request.getStudentId())
                .termId(request.getTermId())
                .gradingScaleId(request.getGradingScaleId())
                .remarks(request.getRemarks())
                .status(ReportCard.ReportCardStatus.DRAFT)
                .build();

        ReportCard saved = reportCardRepository.save(reportCard);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ReportCardResponse getById(UUID id) {
        ReportCard reportCard = reportCardRepository.findById(id)
                .filter(rc -> !rc.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Report card not found"));
        return mapToResponse(reportCard);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCardResponse> getByStudentId(UUID studentId) {
        return reportCardRepository.findByStudentIdAndIsDeletedFalse(studentId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportCardResponse> getByTermId(UUID termId) {
        return reportCardRepository.findByTermIdAndIsDeletedFalse(termId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ReportCardResponse getByStudentAndTerm(UUID studentId, UUID termId) {
        ReportCard reportCard = reportCardRepository.findByStudentIdAndTermIdAndIsDeletedFalse(studentId, termId)
                .orElseThrow(() -> new ResourceNotFoundException("Report card not found for student in term"));
        return mapToResponse(reportCard);
    }

    @Override
    public ReportCardResponse updateStatus(UUID id, String status) {
        ReportCard reportCard = reportCardRepository.findById(id)
                .filter(rc -> !rc.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Report card not found"));
        reportCard.setStatus(ReportCard.ReportCardStatus.valueOf(status));
        if (ReportCard.ReportCardStatus.PUBLISHED.name().equals(status)) {
            reportCard.setPublishedAt(LocalDateTime.now());
        }
        ReportCard saved = reportCardRepository.save(reportCard);
        return mapToResponse(saved);
    }

    @Override
    public void calculateClassRanks(UUID termId) {
        List<ReportCard> ranked = reportCardRepository.findRankedByTermId(termId);
        for (int i = 0; i < ranked.size(); i++) {
            ranked.get(i).setClassRank(i + 1);
        }
        reportCardRepository.saveAll(ranked);
    }

    private ReportCardResponse mapToResponse(ReportCard rc) {
        return ReportCardResponse.builder()
                .id(rc.getId())
                .studentId(rc.getStudentId())
                .academicYearId(rc.getAcademicYearId())
                .termId(rc.getTermId())
                .gradingScaleId(rc.getGradingScaleId())
                .totalMarks(rc.getTotalMarks())
                .averageMark(rc.getAverageMark())
                .overallGrade(rc.getOverallGrade())
                .gpa(rc.getGpa())
                .classRank(rc.getClassRank())
                .totalStudentsInClass(rc.getTotalStudentsInClass())
                .remarks(rc.getRemarks())
                .status(rc.getStatus().name())
                .publishedAt(rc.getPublishedAt())
                .createdAt(rc.getCreatedAt())
                .build();
    }
}
