package tz.elmkusoma.grading.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.grading.domain.ReportCard;
import tz.elmkusoma.grading.domain.ReportCard.ReportCardStatus;
import tz.elmkusoma.grading.dto.request.GenerateReportCardRequest;
import tz.elmkusoma.grading.dto.response.ReportCardResponse;
import tz.elmkusoma.grading.repository.ReportCardRepository;
import tz.elmkusoma.grading.service.ReportCardService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReportCardServiceImpl implements ReportCardService {

    private final ReportCardRepository reportCardRepository;

    @Override
    @Transactional
    public ReportCardResponse generate(UUID institutionId, GenerateReportCardRequest request) {
        ReportCard reportCard = ReportCard.builder()
                .institutionId(institutionId)
                .studentId(request.getStudentId())
                .termId(request.getTermId())
                .gradingScaleId(request.getGradingScaleId())
                .remarks(request.getRemarks())
                .status(ReportCardStatus.DRAFT)
                .build();

        reportCard = reportCardRepository.save(reportCard);
        return toResponse(reportCard);
    }

    @Override
    public ReportCardResponse getById(UUID id) {
        ReportCard reportCard = reportCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report card not found: " + id));
        return toResponse(reportCard);
    }

    @Override
    public List<ReportCardResponse> getByStudentId(UUID studentId) {
        return reportCardRepository.findByStudentIdAndIsDeletedFalse(studentId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<ReportCardResponse> getByTermId(UUID termId) {
        return reportCardRepository.findByTermIdAndIsDeletedFalse(termId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public ReportCardResponse getByStudentAndTerm(UUID studentId, UUID termId) {
        ReportCard reportCard = reportCardRepository
                .findByStudentIdAndTermIdAndIsDeletedFalse(studentId, termId)
                .orElseThrow(() -> new RuntimeException("Report card not found for student " + studentId + " in term " + termId));
        return toResponse(reportCard);
    }

    @Override
    @Transactional
    public ReportCardResponse updateStatus(UUID id, String status) {
        ReportCard reportCard = reportCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Report card not found: " + id));
        reportCard.setStatus(ReportCardStatus.valueOf(status));
        if (ReportCardStatus.PUBLISHED.name().equals(status)) {
            reportCard.setPublishedAt(LocalDateTime.now());
        }
        reportCard = reportCardRepository.save(reportCard);
        return toResponse(reportCard);
    }

    @Override
    @Transactional
    public void calculateClassRanks(UUID termId) {
        List<ReportCard> ranked = reportCardRepository.findRankedByTermId(termId);
        int total = ranked.size();
        for (int i = 0; i < ranked.size(); i++) {
            ReportCard rc = ranked.get(i);
            rc.setClassRank(i + 1);
            rc.setTotalStudentsInClass(total);
        }
        reportCardRepository.saveAll(ranked);
    }

    private ReportCardResponse toResponse(ReportCard rc) {
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
