package tz.elmkusoma.highereducation.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tz.elmkusoma.academic.domain.EducationLevel;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.Department;
import tz.elmkusoma.highereducation.domain.Programme;
import tz.elmkusoma.highereducation.domain.ProgrammeType;
import tz.elmkusoma.highereducation.dto.ProgrammeDTO;
import tz.elmkusoma.highereducation.repository.DepartmentRepository;
import tz.elmkusoma.highereducation.repository.ProgrammeRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProgrammeServiceTest {

    @Mock private ProgrammeRepository programmeRepository;
    @Mock private DepartmentRepository departmentRepository;

    @InjectMocks private ProgrammeService programmeService;

    private static Programme programme(UUID institutionId, UUID departmentId) {
        Programme p = Programme.builder()
                .name("Computer Engineering")
                .code("CE-01")
                .programmeType(ProgrammeType.DEGREE)
                .educationLevel(EducationLevel.COLLEGE)
                .durationMonths(36)
                .departmentId(departmentId)
                .isActive(true)
                .build();
        p.setId(UUID.randomUUID());
        p.setInstitutionId(institutionId);
        p.setIsDeleted(false);
        return p;
    }

    // ---- R4: duration is expressed in months and presented in years ----------------------

    @Test
    void formatDuration_expressesMonthsInYears() {
        assertEquals("1 year", ProgrammeService.formatDuration(12));
        assertEquals("1.5 years", ProgrammeService.formatDuration(18));
        assertEquals("2 years", ProgrammeService.formatDuration(24));
        assertEquals("3 years", ProgrammeService.formatDuration(36));
        assertEquals("4 years", ProgrammeService.formatDuration(48));
        assertEquals("9 months", ProgrammeService.formatDuration(9));
    }

    @Test
    void formatDuration_returnsNullWhenUnsetOrInvalid() {
        assertNull(ProgrammeService.formatDuration(null));
        assertNull(ProgrammeService.formatDuration(0));
        assertNull(ProgrammeService.formatDuration(-6));
    }

    @Test
    void create_persistsDurationMonthsAndExposesDisplay() {
        UUID institutionId = UUID.randomUUID();
        when(programmeRepository.save(any(Programme.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProgrammeDTO request = ProgrammeDTO.builder()
                .name("Computer Engineering")
                .programmeType(ProgrammeType.DEGREE)
                .durationMonths(18)
                .build();

        ProgrammeDTO created = programmeService.create(institutionId, request);

        assertEquals(18, created.getDurationMonths());
        assertEquals("1.5 years", created.getDurationDisplay());
    }

    /**
     * Credit hours were removed from the programme model, so the API must not accept them
     * and must not persist them. Duration in months is the single source of truth.
     */
    @Test
    void programmeModel_noLongerExposesCreditHours() {
        assertFalse(exposes(ProgrammeDTO.class, "creditHours"),
                "ProgrammeDTO must not expose creditHours: programme creation uses duration in months");
        assertFalse(exposes(Programme.class, "creditHours"),
                "Programme must not expose creditHours: programme creation uses duration in months");

        UUID institutionId = UUID.randomUUID();
        when(programmeRepository.save(any(Programme.class))).thenAnswer(inv -> inv.getArgument(0));
        ProgrammeDTO created = programmeService.create(institutionId, ProgrammeDTO.builder()
                .name("Diploma Programme")
                .programmeType(ProgrammeType.DIPLOMA)
                .durationMonths(12)
                .build());
        assertEquals("1 year", created.getDurationDisplay());
    }

    /** True when the type still carries a getter or setter for the given property. */
    private static boolean exposes(Class<?> type, String property) {
        for (java.lang.reflect.Method m : type.getMethods()) {
            if (m.getName().equalsIgnoreCase("get" + property)
                    || m.getName().equalsIgnoreCase("set" + property)) {
                return true;
            }
        }
        return false;
    }

    private static boolean exposes(Object target, String property) {
        return exposes(target.getClass(), property);
    }

    // ---- R5: Department -> Programme ------------------------------------------------------

    @Test
    void create_linksProgrammeToDepartment() {
        UUID institutionId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        Department department = Department.builder().name("Computer Studies").isActive(true).build();
        department.setId(departmentId);
        department.setInstitutionId(institutionId);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(programmeRepository.save(any(Programme.class))).thenAnswer(inv -> inv.getArgument(0));

        ProgrammeDTO created = programmeService.create(institutionId, ProgrammeDTO.builder()
                .name("Computer Engineering")
                .programmeType(ProgrammeType.DEGREE)
                .durationMonths(36)
                .departmentId(departmentId)
                .build());

        assertEquals(departmentId, created.getDepartmentId());
        assertEquals("Computer Studies", created.getDepartmentName());
    }

    /**
     * A programme must not be attached to another institution's department: without this an
     * institution admin could file their programme under a foreign department.
     */
    @Test
    void create_withForeignDepartment_shouldBeForbidden() {
        UUID institutionId = UUID.randomUUID();
        UUID foreignDepartmentId = UUID.randomUUID();
        Department foreign = Department.builder().name("Foreign Dept").isActive(true).build();
        foreign.setId(foreignDepartmentId);
        foreign.setInstitutionId(UUID.randomUUID());
        when(departmentRepository.findById(foreignDepartmentId)).thenReturn(Optional.of(foreign));

        assertThrows(ForbiddenException.class, () -> programmeService.create(institutionId,
                ProgrammeDTO.builder()
                        .name("Computer Engineering")
                        .programmeType(ProgrammeType.DEGREE)
                        .departmentId(foreignDepartmentId)
                        .build()));
        verify(programmeRepository, never()).save(any(Programme.class));
    }

    @Test
    void listByDepartment_isScopedToTheInstitution() {
        UUID institutionId = UUID.randomUUID();
        UUID departmentId = UUID.randomUUID();
        Department department = Department.builder().name("Computer Studies").isActive(true).build();
        department.setId(departmentId);
        department.setInstitutionId(institutionId);
        when(departmentRepository.findById(departmentId)).thenReturn(Optional.of(department));
        when(programmeRepository.findByInstitutionIdAndDepartmentIdAndIsDeletedFalse(
                institutionId, departmentId))
                .thenReturn(List.of(programme(institutionId, departmentId)));

        List<ProgrammeDTO> result = programmeService.listByDepartment(institutionId, departmentId);

        assertEquals(1, result.size());
        assertEquals(departmentId, result.get(0).getDepartmentId());
    }

    @Test
    void listByDepartment_withForeignDepartment_shouldBeForbidden() {
        UUID institutionId = UUID.randomUUID();
        UUID foreignDepartmentId = UUID.randomUUID();
        Department foreign = Department.builder().name("Foreign").isActive(true).build();
        foreign.setId(foreignDepartmentId);
        foreign.setInstitutionId(UUID.randomUUID());
        when(departmentRepository.findById(foreignDepartmentId)).thenReturn(Optional.of(foreign));

        assertThrows(ForbiddenException.class,
                () -> programmeService.listByDepartment(institutionId, foreignDepartmentId));
    }

    @Test
    void update_canDetachDepartment() {
        UUID institutionId = UUID.randomUUID();
        UUID programmeId = UUID.randomUUID();
        Programme existing = programme(institutionId, UUID.randomUUID());
        existing.setId(programmeId);
        when(programmeRepository.findById(programmeId)).thenReturn(Optional.of(existing));
        when(programmeRepository.save(any(Programme.class))).thenAnswer(inv -> inv.getArgument(0));

        ProgrammeDTO updated = programmeService.update(programmeId, ProgrammeDTO.builder()
                .name("Computer Engineering")
                .programmeType(ProgrammeType.DEGREE)
                .durationMonths(24)
                .departmentId(null)
                .build());

        assertNull(updated.getDepartmentId());
        assertEquals("2 years", updated.getDurationDisplay());
    }

    @Test
    void getById_unknownId_shouldThrow() {
        UUID missing = UUID.randomUUID();
        when(programmeRepository.findById(missing)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> programmeService.getById(missing));
    }
}