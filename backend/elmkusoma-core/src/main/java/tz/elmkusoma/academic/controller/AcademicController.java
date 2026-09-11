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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/academic")
@RequiredArgsConstructor
public class AcademicController {

    private final AcademicService academicService;

    // ── Academic Year ──────────────────────────────────────────────

    @PostMapping("/years")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<AcademicYear>> createAcademicYear(
            @Valid @RequestBody AcademicYearRequest request) {
        AcademicYear year = academicService.createAcademicYear(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Academic year created", year));
    }

    @GetMapping("/years")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<AcademicYear>>> getAcademicYears(
            @RequestParam UUID institutionId,
            @RequestParam(required = false) EducationLevel educationLevel) {
        List<AcademicYear> years = educationLevel != null
                ? academicService.getAcademicYearsByLevel(institutionId, educationLevel)
                : academicService.getAcademicYears(institutionId);
        return ResponseEntity.ok(ApiResponse.success(years));
    }

    @GetMapping("/years/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<AcademicYear>> getAcademicYear(@PathVariable UUID id) {
        AcademicYear year = academicService.getAcademicYear(id);
        return ResponseEntity.ok(ApiResponse.success(year));
    }

    // ── Term ───────────────────────────────────────────────────────

    @PostMapping("/years/{academicYearId}/terms")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Term>> createTerm(
            @PathVariable UUID academicYearId,
            @Valid @RequestBody TermRequest request) {
        Term term = academicService.createTerm(academicYearId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Term created", term));
    }

    @GetMapping("/years/{academicYearId}/terms")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<Term>>> getTerms(
            @PathVariable UUID academicYearId) {
        List<Term> terms = academicService.getTerms(academicYearId);
        return ResponseEntity.ok(ApiResponse.success(terms));
    }

    // ── Grade ──────────────────────────────────────────────────────

    @PostMapping("/grades")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Grade>> createGrade(
            @Valid @RequestBody GradeRequest request) {
        Grade grade = academicService.createGrade(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Grade created", grade));
    }

    @GetMapping("/grades")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<Grade>>> getGrades(
            @RequestParam UUID institutionId,
            @RequestParam(required = false) EducationLevel educationLevel) {
        List<Grade> grades = educationLevel != null
                ? academicService.getGradesByLevel(institutionId, educationLevel)
                : academicService.getGrades(institutionId);
        return ResponseEntity.ok(ApiResponse.success(grades));
    }

    @GetMapping("/grades/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Grade>> getGrade(@PathVariable UUID id) {
        Grade grade = academicService.getGrade(id);
        return ResponseEntity.ok(ApiResponse.success(grade));
    }

    // ── Subject ────────────────────────────────────────────────────

    @PostMapping("/subjects")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Subject>> createSubject(
            @Valid @RequestBody SubjectRequest request) {
        Subject subject = academicService.createSubject(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subject created", subject));
    }

    @GetMapping("/subjects")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<Subject>>> getSubjects(
            @RequestParam UUID institutionId,
            @RequestParam(required = false) EducationLevel educationLevel) {
        List<Subject> subjects = educationLevel != null
                ? academicService.getSubjectsByLevel(institutionId, educationLevel)
                : academicService.getSubjects(institutionId);
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }

    @GetMapping("/subjects/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Subject>> getSubject(@PathVariable UUID id) {
        Subject subject = academicService.getSubject(id);
        return ResponseEntity.ok(ApiResponse.success(subject));
    }

    // ── Class Group ────────────────────────────────────────────────

    @PostMapping("/classes")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<ClassGroup>> createClassGroup(
            @Valid @RequestBody ClassGroupRequest request) {
        ClassGroup classGroup = academicService.createClassGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Class group created", classGroup));
    }

    @GetMapping("/classes")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<ClassGroup>>> getClassGroups(
            @RequestParam UUID institutionId,
            @RequestParam(required = false) UUID gradeId,
            @RequestParam(required = false) UUID termId) {
        List<ClassGroup> classes;
        if (gradeId != null && termId != null) {
            classes = academicService.getClassGroupsByGradeAndTerm(gradeId, termId);
        } else {
            classes = academicService.getClassGroups(institutionId);
        }
        return ResponseEntity.ok(ApiResponse.success(classes));
    }

    @GetMapping("/classes/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<ClassGroup>> getClassGroup(@PathVariable UUID id) {
        ClassGroup classGroup = academicService.getClassGroup(id);
        return ResponseEntity.ok(ApiResponse.success(classGroup));
    }
}
