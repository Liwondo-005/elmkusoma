package tz.elmkusoma.grading.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.exception.ResourceNotFoundException;
import tz.elmkusoma.grading.domain.ReportCard;
import tz.elmkusoma.grading.domain.ReportCard.ReportCardStatus;
import tz.elmkusoma.grading.dto.request.GenerateReportCardRequest;
import tz.elmkusoma.grading.dto.response.ReportCardResponse;
import tz.elmkusoma.grading.repository.ReportCardRepository;
import tz.elmkusoma.grading.service.ReportCardService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;
import tz.elmkusoma.academic.domain.AcademicYear;
import tz.elmkusoma.academic.repository.AcademicYearRepository;
import tz.elmkusoma.academic.domain.Term;
import tz.elmkusoma.academic.repository.TermRepository;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.repository.ClassGroupRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportCardServiceImpl implements ReportCardService {

    private final ReportCardRepository reportCardRepository;
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final AcademicYearRepository academicYearRepository;
    private final TermRepository termRepository;
    private final ClassGroupRepository classGroupRepository;
    private final StudentClassAssignmentRepository studentClassAssignmentRepository;

    @Override
    public ReportCardResponse generate(UUID institutionId, GenerateReportCardRequest request) {
        ReportCard reportCard = ReportCard.builder()
                .institutionId(institutionId)
                .studentId(request.getStudentId())
                .termId(request.getTermId())
                .gradingScaleId(request.getGradingScaleId())
                .remarks(request.getRemarks())
                .status(ReportCardStatus.DRAFT)
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
    @Transactional
    public ReportCardResponse updateStatus(UUID id, String status) {
        ReportCard reportCard = reportCardRepository.findById(id)
                .filter(rc -> !rc.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Report card not found"));
        reportCard.setStatus(ReportCardStatus.valueOf(status));
        if (ReportCardStatus.PUBLISHED.name().equals(status)) {
            reportCard.setPublishedAt(LocalDateTime.now());
        }
        ReportCard saved = reportCardRepository.save(reportCard);
        return mapToResponse(saved);
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

    private ReportCardResponse mapToResponse(ReportCard rc) {
        String studentName = null;
        String admissionNumber = null;
        String className = null;
        String termName = null;
        String academicYearName = null;

        Student student = studentRepository.findById(rc.getStudentId()).orElse(null);
        if (student != null) {
            admissionNumber = student.getAdmissionNumber();
            User user = userRepository.findById(student.getUserId()).orElse(null);
            if (user != null) {
                studentName = user.getFirstName() + " " + user.getLastName();
            }
            studentClassAssignmentRepository.findByStudentIdAndIsDeletedFalse(rc.getStudentId())
                    .stream().findFirst().ifPresent(sca -> {
                    });
        }

        if (rc.getTermId() != null) {
            Term term = termRepository.findById(rc.getTermId()).orElse(null);
            if (term != null) termName = term.getName();
        }
        if (rc.getAcademicYearId() != null) {
            AcademicYear year = academicYearRepository.findById(rc.getAcademicYearId()).orElse(null);
            if (year != null) academicYearName = year.getYearLabel();
        }

        return ReportCardResponse.builder()
                .id(rc.getId())
                .studentId(rc.getStudentId())
                .studentName(studentName)
                .admissionNumber(admissionNumber)
                .academicYearId(rc.getAcademicYearId())
                .academicYear(academicYearName)
                .termId(rc.getTermId())
                .term(termName)
                .termName(termName)
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
