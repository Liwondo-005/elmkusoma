package tz.elmkusoma.academic.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.academic.domain.AcademicYear;
import tz.elmkusoma.academic.domain.ClassGroup;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.academic.domain.Grade;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.domain.Term;
import tz.elmkusoma.academic.dto.AcademicYearRequest;
import tz.elmkusoma.academic.dto.ClassGroupRequest;
import tz.elmkusoma.academic.dto.GradeRequest;
import tz.elmkusoma.academic.dto.SubjectRequest;
import tz.elmkusoma.academic.dto.TermRequest;
import tz.elmkusoma.academic.service.AcademicService;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/academic")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER','INSTITUTION_ADMIN','ADMIN')")
public class AcademicController {

    private final AcademicService academicService;

    // ── Academic Year ──────────────────────────────────────────────

    @PostMapping("/years")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<AcademicYear>> createAcademicYear(
            @Valid @RequestBody AcademicYearRequest request,
            @RequestAttribute("institutionId") UUID institutionId) {
        AcademicYear year = academicService.createAcademicYear(request, institutionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Academic year created", year));
    }

    @GetMapping("/years")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AcademicYear>>> getAcademicYears(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestParam(required = false) EducationLevel educationLevel) {
        List<AcademicYear> years = educationLevel != null
                ? academicService.getAcademicYearsByLevel(institutionId, educationLevel)
                : academicService.getAcademicYears(institutionId);
        return ResponseEntity.ok(ApiResponse.success(years));
    }

    @GetMapping("/years/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AcademicYear>> getAcademicYear(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID institutionId) {
        AcademicYear year = academicService.getAcademicYear(id);
        if (!institutionId.equals(year.getInstitutionId())) {
            // §41/§60: object-level denial across institutions — indistinguishable from not-found
            throw new ResourceNotFoundException("AcademicYear", "id", id);
        }
        return ResponseEntity.ok(ApiResponse.success(year));
    }

    // ── Term ───────────────────────────────────────────────────────

    @PostMapping("/years/{academicYearId}/terms")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Term>> createTerm(
            @PathVariable UUID academicYearId,
            @Valid @RequestBody TermRequest request,
            @RequestAttribute("institutionId") UUID institutionId) {
        Term term = academicService.createTerm(academicYearId, request, institutionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Term created", term));
    }

    @GetMapping("/years/{academicYearId}/terms")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<Term>>> getTerms(
            @PathVariable UUID academicYearId,
            @RequestAttribute("institutionId") UUID institutionId) {
        // §41: the parent year must belong to the caller's institution
        if (!institutionId.equals(academicService.getAcademicYear(academicYearId).getInstitutionId())) {
            throw new ResourceNotFoundException("AcademicYear", "id", academicYearId);
        }
        List<Term> terms = academicService.getTerms(academicYearId);
        return ResponseEntity.ok(ApiResponse.success(terms));
    }

    // ── Grade ──────────────────────────────────────────────────────

    @PostMapping("/grades")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Grade>> createGrade(
            @Valid @RequestBody GradeRequest request,
            @RequestAttribute("institutionId") UUID institutionId) {
        Grade grade = academicService.createGrade(request, institutionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Grade created", grade));
    }

    @GetMapping("/grades")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<Grade>>> getGrades(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestParam(required = false) EducationLevel educationLevel) {
        List<Grade> grades = educationLevel != null
                ? academicService.getGradesByLevel(institutionId, educationLevel)
                : academicService.getGrades(institutionId);
        return ResponseEntity.ok(ApiResponse.success(grades));
    }

    @GetMapping("/grades/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Grade>> getGrade(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID institutionId) {
        Grade grade = academicService.getGrade(id);
        if (!institutionId.equals(grade.getInstitutionId())) {
            // §41/§60: object-level denial across institutions — indistinguishable from not-found
            throw new ResourceNotFoundException("Grade", "id", id);
        }
        return ResponseEntity.ok(ApiResponse.success(grade));
    }

    // ── Subject ────────────────────────────────────────────────────

    @PostMapping("/subjects")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Subject>> createSubject(
            @Valid @RequestBody SubjectRequest request,
            @RequestAttribute("institutionId") UUID institutionId) {
        Subject subject = academicService.createSubject(request, institutionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subject created", subject));
    }

    @GetMapping("/subjects")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<Subject>>> getSubjects(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestParam(required = false) EducationLevel educationLevel) {
        List<Subject> subjects = educationLevel != null
                ? academicService.getSubjectsByLevel(institutionId, educationLevel)
                : academicService.getSubjects(institutionId);
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }

    @GetMapping("/subjects/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Subject>> getSubject(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID institutionId) {
        Subject subject = academicService.getSubject(id, institutionId);
        return ResponseEntity.ok(ApiResponse.success(subject));
    }

    // ── Class Group ────────────────────────────────────────────────

    @PostMapping("/classes")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ClassGroup>> createClassGroup(
            @Valid @RequestBody ClassGroupRequest request,
            @RequestAttribute("institutionId") UUID institutionId) {
        ClassGroup classGroup = academicService.createClassGroup(request, institutionId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Class group created", classGroup));
    }

    @GetMapping("/classes")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ClassGroup>>> getClassGroups(
            @RequestAttribute("institutionId") UUID institutionId,
            @RequestParam(required = false) UUID gradeId,
            @RequestParam(required = false) UUID termId) {
        List<ClassGroup> classes;
        if (gradeId != null && termId != null) {
            // §41: referenced grade/term must belong to the caller's institution
            if (!institutionId.equals(academicService.getGrade(gradeId).getInstitutionId())
                    || !institutionId.equals(academicService.getTerm(termId).getInstitutionId())) {
                throw new ForbiddenException("Access denied");
            }
            classes = academicService.getClassGroupsByGradeAndTerm(gradeId, termId);
        } else {
            classes = academicService.getClassGroups(institutionId);
        }
        return ResponseEntity.ok(ApiResponse.success(classes));
    }

    @GetMapping("/classes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ClassGroup>> getClassGroup(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID institutionId) {
        ClassGroup classGroup = academicService.getClassGroup(id);
        if (!institutionId.equals(classGroup.getInstitutionId())) {
            // §41/§60: object-level denial across institutions — indistinguishable from not-found
            throw new ResourceNotFoundException("ClassGroup", "id", id);
        }
        return ResponseEntity.ok(ApiResponse.success(classGroup));
    }
}
