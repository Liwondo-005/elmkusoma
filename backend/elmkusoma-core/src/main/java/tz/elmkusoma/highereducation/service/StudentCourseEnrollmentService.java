package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.StudentCourseEnrollment;
import tz.elmkusoma.highereducation.domain.EnrollmentStatus;
import tz.elmkusoma.highereducation.dto.StudentCourseEnrollmentDTO;
import tz.elmkusoma.highereducation.repository.StudentCourseEnrollmentRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentCourseEnrollmentService {

    private final StudentCourseEnrollmentRepository repository;

    public List<StudentCourseEnrollmentDTO> getStudentEnrollments(UUID studentId) {
        return repository.findByStudentIdAndIsDeletedFalse(studentId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<StudentCourseEnrollmentDTO> getActiveEnrollments(UUID studentId) {
        return repository.findByStudentIdAndStatusAndIsDeletedFalse(studentId, "ENROLLED").stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public long countActiveEnrollments(UUID studentId) {
        return repository.countByStudentIdAndStatusAndIsDeletedFalse(studentId, "ENROLLED");
    }

    public StudentCourseEnrollmentDTO create(StudentCourseEnrollmentDTO dto) {
        StudentCourseEnrollment entity = StudentCourseEnrollment.builder()
                .studentId(dto.getStudentId())
                .courseId(dto.getCourseId())
                .programmeId(dto.getProgrammeId())
                .semester(dto.getSemester())
                .academicYear(dto.getAcademicYear())
                .creditHours(dto.getCreditHours())
                .status(dto.getStatus() != null ? dto.getStatus() : EnrollmentStatus.ENROLLED)
                .enrolledDate(dto.getEnrolledDate())
                .grade(dto.getGrade())
                .gradePoints(dto.getGradePoints())
                .instructorId(dto.getInstructorId())
                .institutionId(dto.getInstitutionId())
                .build();
        return toDTO(repository.save(entity));
    }

    public StudentCourseEnrollmentDTO update(UUID id, StudentCourseEnrollmentDTO dto) {
        StudentCourseEnrollment entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudentCourseEnrollment", "id", id));
        entity.setGrade(dto.getGrade());
        entity.setGradePoints(dto.getGradePoints());
        entity.setStatus(dto.getStatus());
        entity.setCompletedDate(dto.getCompletedDate());
        return toDTO(repository.save(entity));
    }

    private StudentCourseEnrollmentDTO toDTO(StudentCourseEnrollment e) {
        return StudentCourseEnrollmentDTO.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .courseId(e.getCourseId())
                .programmeId(e.getProgrammeId())
                .semester(e.getSemester())
                .academicYear(e.getAcademicYear())
                .creditHours(e.getCreditHours())
                .status(e.getStatus())
                .enrolledDate(e.getEnrolledDate())
                .completedDate(e.getCompletedDate())
                .grade(e.getGrade())
                .gradePoints(e.getGradePoints())
                .instructorId(e.getInstructorId())
                .institutionId(e.getInstitutionId())
                .build();
    }
}
