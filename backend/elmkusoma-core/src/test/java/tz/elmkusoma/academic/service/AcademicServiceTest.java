package tz.elmkusoma.academic.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.academic.domain.*;
import tz.elmkusoma.academic.dto.*;
import tz.elmkusoma.academic.repository.*;
import tz.elmkusoma.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AcademicServiceTest {

    @Mock
    private AcademicYearRepository academicYearRepository;
    @Mock
    private TermRepository termRepository;
    @Mock
    private GradeRepository gradeRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private ClassGroupRepository classGroupRepository;

    @InjectMocks
    private AcademicService academicService;

    private UUID institutionId;
    private UUID yearId;
    private UUID termId;
    private UUID gradeId;
    private UUID subjectId;
    private UUID classGroupId;

    @BeforeEach
    void setUp() {
        institutionId = UUID.randomUUID();
        yearId = UUID.randomUUID();
        termId = UUID.randomUUID();
        gradeId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
        classGroupId = UUID.randomUUID();
    }

    @Test
    void createAcademicYear_shouldReturnSavedYear() {
        AcademicYearRequest request = new AcademicYearRequest();
        request.setInstitutionId(institutionId);
        request.setEducationLevel(EducationLevel.PRIMARY);
        request.setYearLabel("2025/2026");
        request.setStartDate(LocalDate.of(2025, 1, 1));
        request.setEndDate(LocalDate.of(2025, 12, 31));
        request.setIsCurrent(false);

        AcademicYear saved = AcademicYear.builder()
                .institutionId(institutionId)
                .educationLevel(EducationLevel.PRIMARY)
                .yearLabel("2025/2026")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .isCurrent(false)
                .isActive(true)
                .build();
        saved.setId(yearId);

        when(academicYearRepository.save(any(AcademicYear.class))).thenReturn(saved);

        AcademicYear result = academicService.createAcademicYear(request);

        assertNotNull(result);
        assertEquals(institutionId, result.getInstitutionId());
        assertEquals(EducationLevel.PRIMARY, result.getEducationLevel());
        assertEquals("2025/2026", result.getYearLabel());
        assertEquals(yearId, result.getId());
        verify(academicYearRepository).save(any(AcademicYear.class));
    }

    @Test
    void getAcademicYears_shouldReturnInstitutionScopedYears() {
        AcademicYear year1 = AcademicYear.builder()
                .institutionId(institutionId)
                .educationLevel(EducationLevel.PRIMARY)
                .yearLabel("2025/2026")
                .build();
        year1.setId(UUID.randomUUID());

        AcademicYear year2 = AcademicYear.builder()
                .institutionId(institutionId)
                .educationLevel(EducationLevel.SECONDARY)
                .yearLabel("2025/2026")
                .build();
        year2.setId(UUID.randomUUID());

        when(academicYearRepository.findByInstitutionIdAndIsDeletedFalse(institutionId))
                .thenReturn(List.of(year1, year2));

        List<AcademicYear> result = academicService.getAcademicYears(institutionId);

        assertEquals(2, result.size());
        assertEquals(institutionId, result.get(0).getInstitutionId());
        assertEquals(institutionId, result.get(1).getInstitutionId());
        verify(academicYearRepository).findByInstitutionIdAndIsDeletedFalse(institutionId);
    }

    @Test
    void getAcademicYear_whenNotFound_shouldThrowResourceNotFoundException() {
        UUID nonExistentId = UUID.randomUUID();
        when(academicYearRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> academicService.getAcademicYear(nonExistentId));
        assertTrue(exception.getMessage().contains("AcademicYear"));
    }

    @Test
    void createTerm_shouldValidateAcademicYearExistsAndSave() {
        AcademicYear year = AcademicYear.builder()
                .institutionId(institutionId)
                .yearLabel("2025/2026")
                .build();
        year.setId(yearId);

        when(academicYearRepository.findById(yearId)).thenReturn(Optional.of(year));

        TermRequest request = new TermRequest();
        request.setInstitutionId(institutionId);
        request.setName("Term 1");
        request.setTermNumber(1);
        request.setStartDate(LocalDate.of(2025, 1, 1));
        request.setEndDate(LocalDate.of(2025, 4, 30));

        Term savedTerm = Term.builder()
                .institutionId(institutionId)
                .academicYearId(yearId)
                .name("Term 1")
                .termNumber(1)
                .isActive(true)
                .build();
        savedTerm.setId(termId);

        when(termRepository.save(any(Term.class))).thenReturn(savedTerm);

        Term result = academicService.createTerm(yearId, request);

        assertNotNull(result);
        assertEquals(yearId, result.getAcademicYearId());
        assertEquals("Term 1", result.getName());
        verify(termRepository).save(any(Term.class));
    }

    @Test
    void createTerm_whenAcademicYearNotFound_shouldThrow() {
        UUID nonExistentYearId = UUID.randomUUID();
        when(academicYearRepository.findById(nonExistentYearId)).thenReturn(Optional.empty());

        TermRequest request = new TermRequest();
        request.setInstitutionId(institutionId);
        request.setName("Term 1");
        request.setTermNumber(1);
        request.setStartDate(LocalDate.of(2025, 1, 1));
        request.setEndDate(LocalDate.of(2025, 4, 30));

        assertThrows(ResourceNotFoundException.class,
                () -> academicService.createTerm(nonExistentYearId, request));
    }

    @Test
    void createGrade_shouldSaveWithInstitutionScope() {
        GradeRequest request = new GradeRequest();
        request.setInstitutionId(institutionId);
        request.setEducationLevel(EducationLevel.PRIMARY);
        request.setName("Grade 1");
        request.setCode("G1");
        request.setSortOrder(1);

        Grade savedGrade = Grade.builder()
                .institutionId(institutionId)
                .educationLevel(EducationLevel.PRIMARY)
                .name("Grade 1")
                .code("G1")
                .sortOrder(1)
                .isActive(true)
                .build();
        savedGrade.setId(gradeId);

        when(gradeRepository.save(any(Grade.class))).thenReturn(savedGrade);

        Grade result = academicService.createGrade(request);

        assertNotNull(result);
        assertEquals(institutionId, result.getInstitutionId());
        assertEquals("Grade 1", result.getName());
        verify(gradeRepository).save(any(Grade.class));
    }

    @Test
    void getGrades_shouldFilterByInstitutionId() {
        Grade grade = Grade.builder()
                .institutionId(institutionId)
                .name("Grade 1")
                .build();
        grade.setId(gradeId);

        when(gradeRepository.findByInstitutionIdAndIsDeletedFalse(institutionId))
                .thenReturn(List.of(grade));

        List<Grade> result = academicService.getGrades(institutionId);

        assertEquals(1, result.size());
        assertEquals(institutionId, result.get(0).getInstitutionId());
    }

    @Test
    void createSubject_shouldSaveWithInstitutionScope() {
        SubjectRequest request = new SubjectRequest();
        request.setInstitutionId(institutionId);
        request.setEducationLevel(EducationLevel.PRIMARY);
        request.setName("Mathematics");
        request.setCode("MATH");
        request.setDescription("Basic Mathematics");

        Subject savedSubject = Subject.builder()
                .institutionId(institutionId)
                .educationLevel(EducationLevel.PRIMARY)
                .name("Mathematics")
                .code("MATH")
                .description("Basic Mathematics")
                .isActive(true)
                .build();
        savedSubject.setId(subjectId);

        when(subjectRepository.save(any(Subject.class))).thenReturn(savedSubject);

        Subject result = academicService.createSubject(request);

        assertNotNull(result);
        assertEquals(institutionId, result.getInstitutionId());
        assertEquals("Mathematics", result.getName());
    }

    @Test
    void createClassGroup_shouldValidateAllReferences() {
        Grade grade = Grade.builder().institutionId(institutionId).build();
        grade.setId(gradeId);
        AcademicYear year = AcademicYear.builder().institutionId(institutionId).build();
        year.setId(yearId);
        Term term = Term.builder().institutionId(institutionId).build();
        term.setId(termId);

        when(gradeRepository.findById(gradeId)).thenReturn(Optional.of(grade));
        when(academicYearRepository.findById(yearId)).thenReturn(Optional.of(year));
        when(termRepository.findById(termId)).thenReturn(Optional.of(term));

        ClassGroupRequest request = new ClassGroupRequest();
        request.setInstitutionId(institutionId);
        request.setGradeId(gradeId);
        request.setAcademicYearId(yearId);
        request.setTermId(termId);
        request.setName("Class 1A");
        request.setSection("A");
        request.setCapacity(40);

        ClassGroup savedGroup = ClassGroup.builder()
                .institutionId(institutionId)
                .gradeId(gradeId)
                .academicYearId(yearId)
                .termId(termId)
                .name("Class 1A")
                .section("A")
                .capacity(40)
                .isActive(true)
                .build();
        savedGroup.setId(classGroupId);

        when(classGroupRepository.save(any(ClassGroup.class))).thenReturn(savedGroup);

        ClassGroup result = academicService.createClassGroup(request);

        assertNotNull(result);
        assertEquals(institutionId, result.getInstitutionId());
        assertEquals("Class 1A", result.getName());
        verify(classGroupRepository).save(any(ClassGroup.class));
    }

    @Test
    void getClassGroup_whenNotFound_shouldThrow() {
        UUID nonExistentId = UUID.randomUUID();
        when(classGroupRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> academicService.getClassGroup(nonExistentId));
    }
}
