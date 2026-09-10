package tz.elmkusoma.academic.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.*;
import tz.elmkusoma.academic.dto.*;
import tz.elmkusoma.academic.repository.*;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.security.OwnershipGuard;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AcademicService {

    private final AcademicYearRepository academicYearRepository;
    private final TermRepository termRepository;
    private final GradeRepository gradeRepository;
    private final SubjectRepository subjectRepository;
    private final ClassGroupRepository classGroupRepository;

    // ── Academic Year ──────────────────────────────────────────────

    public AcademicYear createAcademicYear(AcademicYearRequest request) {
        AcademicYear year = AcademicYear.builder()
                .institutionId(request.getInstitutionId())
                .educationLevel(request.getEducationLevel())
                .yearLabel(request.getYearLabel())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isCurrent(request.getIsCurrent())
                .isActive(true)
                .build();
        return academicYearRepository.save(year);
    }

    @Transactional(readOnly = true)
    public List<AcademicYear> getAcademicYears(UUID institutionId) {
        return academicYearRepository.findByInstitutionIdAndIsDeletedFalse(institutionId);
    }

    @Transactional(readOnly = true)
    public AcademicYear getAcademicYear(UUID id, UUID institutionId) {
        AcademicYear year = academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", id));
        OwnershipGuard.verifyInstitution(year.getInstitutionId(), institutionId, "academic year");
        return year;
    }

    @Transactional(readOnly = true)
    public List<AcademicYear> getAcademicYearsByLevel(UUID institutionId, EducationLevel level) {
        return academicYearRepository.findByInstitutionIdAndEducationLevelAndIsDeletedFalse(institutionId, level);
    }

    // ── Term ───────────────────────────────────────────────────────

    public Term createTerm(UUID academicYearId, TermRequest request) {
        getAcademicYear(academicYearId, request.getInstitutionId()); // validate exists
        Term term = Term.builder()
                .institutionId(request.getInstitutionId())
                .academicYearId(academicYearId)
                .name(request.getName())
                .termNumber(request.getTermNumber())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(true)
                .build();
        return termRepository.save(term);
    }

    @Transactional(readOnly = true)
    public List<Term> getTerms(UUID academicYearId) {
        return termRepository.findByAcademicYearIdAndIsDeletedFalse(academicYearId);
    }

    @Transactional(readOnly = true)
    public Term getTerm(UUID id, UUID institutionId) {
        Term term = termRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Term", "id", id));
        OwnershipGuard.verifyInstitution(term.getInstitutionId(), institutionId, "term");
        return term;
    }

    // ── Grade ──────────────────────────────────────────────────────

    public Grade createGrade(GradeRequest request) {
        Grade grade = Grade.builder()
                .institutionId(request.getInstitutionId())
                .educationLevel(request.getEducationLevel())
                .name(request.getName())
                .code(request.getCode())
                .sortOrder(request.getSortOrder())
                .isActive(true)
                .build();
        return gradeRepository.save(grade);
    }

    @Transactional(readOnly = true)
    public List<Grade> getGrades(UUID institutionId) {
        return gradeRepository.findByInstitutionIdAndIsDeletedFalse(institutionId);
    }

    @Transactional(readOnly = true)
    public List<Grade> getGradesByLevel(UUID institutionId, EducationLevel level) {
        return gradeRepository.findByInstitutionIdAndEducationLevelAndIsDeletedFalse(institutionId, level);
    }

    @Transactional(readOnly = true)
    public Grade getGrade(UUID id, UUID institutionId) {
        Grade grade = gradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Grade", "id", id));
        OwnershipGuard.verifyInstitution(grade.getInstitutionId(), institutionId, "grade");
        return grade;
    }

    // ── Subject ────────────────────────────────────────────────────

    public Subject createSubject(SubjectRequest request) {
        Subject subject = Subject.builder()
                .institutionId(request.getInstitutionId())
                .educationLevel(request.getEducationLevel())
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .isActive(true)
                .build();
        return subjectRepository.save(subject);
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjects(UUID institutionId) {
        return subjectRepository.findByInstitutionIdAndIsDeletedFalse(institutionId);
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjectsByLevel(UUID institutionId, EducationLevel level) {
        return subjectRepository.findByInstitutionIdAndEducationLevelAndIsDeletedFalse(institutionId, level);
    }

    @Transactional(readOnly = true)
    public Subject getSubject(UUID id, UUID institutionId) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subject", "id", id));
        OwnershipGuard.verifyInstitution(subject.getInstitutionId(), institutionId, "subject");
        return subject;
    }

    // ── Class Group ────────────────────────────────────────────────

    public ClassGroup createClassGroup(ClassGroupRequest request) {
        getGrade(request.getGradeId(), request.getInstitutionId());
        getAcademicYear(request.getAcademicYearId(), request.getInstitutionId());
        getTerm(request.getTermId(), request.getInstitutionId());

        ClassGroup classGroup = ClassGroup.builder()
                .institutionId(request.getInstitutionId())
                .gradeId(request.getGradeId())
                .academicYearId(request.getAcademicYearId())
                .termId(request.getTermId())
                .name(request.getName())
                .section(request.getSection())
                .capacity(request.getCapacity())
                .classTeacherId(request.getClassTeacherId())
                .isActive(true)
                .build();
        return classGroupRepository.save(classGroup);
    }

    @Transactional(readOnly = true)
    public List<ClassGroup> getClassGroups(UUID institutionId) {
        return classGroupRepository.findByInstitutionIdAndIsDeletedFalse(institutionId);
    }

    @Transactional(readOnly = true)
    public List<ClassGroup> getClassGroupsByGradeAndTerm(UUID gradeId, UUID termId) {
        return classGroupRepository.findByGradeIdAndTermIdAndIsDeletedFalse(gradeId, termId);
    }

    @Transactional(readOnly = true)
    public ClassGroup getClassGroup(UUID id, UUID institutionId) {
        ClassGroup classGroup = classGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassGroup", "id", id));
        OwnershipGuard.verifyInstitution(classGroup.getInstitutionId(), institutionId, "class group");
        return classGroup;
    }
}
