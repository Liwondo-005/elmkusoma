package tz.elmkusoma.student.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.domain.StudentClassAssignment;
import tz.elmkusoma.student.domain.StudentStatus;
import tz.elmkusoma.student.dto.AssignClassRequest;
import tz.elmkusoma.student.dto.StudentRequest;
import tz.elmkusoma.student.dto.StudentResponse;
import tz.elmkusoma.student.repository.StudentClassAssignmentRepository;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {

    private final StudentRepository studentRepository;
    private final StudentClassAssignmentRepository assignmentRepository;
    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;

    public StudentResponse createStudent(StudentRequest request) {
        if (studentRepository.existsByUserIdAndIsDeletedFalse(request.getUserId())) {
            throw new IllegalArgumentException("Student profile already exists for this user");
        }

        Institution institution = institutionRepository.findById(request.getInstitutionId())
                .orElseThrow(() -> new ResourceNotFoundException("Institution", "id", request.getInstitutionId()));

        String admissionNumber = generateAdmissionNumber(institution.getCode(), request.getInstitutionId());

        Student student = Student.builder()
                .institutionId(request.getInstitutionId())
                .userId(request.getUserId())
                .admissionNumber(admissionNumber)
                .status(request.getStatus() != null ? request.getStatus() : StudentStatus.ACTIVE)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .city(request.getCity())
                .region(request.getRegion())
                .nationalId(request.getNationalId())
                .bloodGroup(request.getBloodGroup())
                .medicalNotes(request.getMedicalNotes())
                .guardianName(request.getGuardianName())
                .guardianPhone(request.getGuardianPhone())
                .guardianRelationship(request.getGuardianRelationship())
                .enrollmentDate(java.time.LocalDate.now())
                .build();

        student = studentRepository.save(student);
        return toResponse(student);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudent(UUID id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));
        return toResponse(student);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentByAdmissionNumber(String admissionNumber) {
        Student student = studentRepository.findByAdmissionNumberAndIsDeletedFalse(admissionNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "admissionNumber", admissionNumber));
        return toResponse(student);
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getStudents(UUID institutionId, UUID classId) {
        if (classId != null) {
            List<StudentClassAssignment> assignments =
                    assignmentRepository.findByClassGroupIdAndIsDeletedFalse(classId);
            return assignments.stream()
                    .map(a -> studentRepository.findById(a.getStudentId()).orElse(null))
                    .filter(s -> s != null && !s.getIsDeleted())
                    .map(this::toResponse)
                    .toList();
        }
        return studentRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> searchStudents(UUID institutionId, String query) {
        return studentRepository.search(institutionId, query)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public StudentResponse updateStudent(UUID id, StudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", id));

        if (request.getDateOfBirth() != null) student.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null) student.setGender(request.getGender());
        if (request.getAddress() != null) student.setAddress(request.getAddress());
        if (request.getCity() != null) student.setCity(request.getCity());
        if (request.getRegion() != null) student.setRegion(request.getRegion());
        if (request.getNationalId() != null) student.setNationalId(request.getNationalId());
        if (request.getBloodGroup() != null) student.setBloodGroup(request.getBloodGroup());
        if (request.getMedicalNotes() != null) student.setMedicalNotes(request.getMedicalNotes());
        if (request.getGuardianName() != null) student.setGuardianName(request.getGuardianName());
        if (request.getGuardianPhone() != null) student.setGuardianPhone(request.getGuardianPhone());
        if (request.getGuardianRelationship() != null) student.setGuardianRelationship(request.getGuardianRelationship());
        if (request.getStatus() != null) student.setStatus(request.getStatus());

        student = studentRepository.save(student);

        // Update user fields if provided
        User user = userRepository.findById(student.getUserId()).orElse(null);
        if (user != null) {
            boolean updated = false;
            if (request.getFirstName() != null) { user.setFirstName(request.getFirstName()); updated = true; }
            if (request.getMiddleName() != null) { user.setMiddleName(request.getMiddleName()); updated = true; }
            if (request.getLastName() != null) { user.setLastName(request.getLastName()); updated = true; }
            if (request.getPhone() != null) { user.setPhone(request.getPhone()); updated = true; }
            if (updated) userRepository.save(user);
        }

        return toResponse(student);
    }

    public StudentClassAssignment assignToClass(UUID studentId, AssignClassRequest request) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "id", studentId));

        assignmentRepository.findByStudentIdAndClassGroupIdAndIsActiveTrueAndIsDeletedFalse(
                        studentId, request.getClassGroupId())
                .ifPresent(existing -> {
                    existing.setIsActive(false);
                    assignmentRepository.save(existing);
                });

        StudentClassAssignment assignment = StudentClassAssignment.builder()
                .institutionId(student.getInstitutionId())
                .studentId(studentId)
                .classGroupId(request.getClassGroupId())
                .academicYearId(request.getAcademicYearId())
                .termId(request.getTermId())
                .assignedDate(LocalDateTime.now())
                .isActive(true)
                .build();
        return assignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public long countStudents(UUID institutionId) {
        return studentRepository.countByInstitutionId(institutionId);
    }

    @Transactional(readOnly = true)
    public long countActiveStudents(UUID institutionId) {
        return studentRepository.countByInstitutionIdAndStatus(institutionId, StudentStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public UUID getStudentIdByUserId(UUID userId) {
        return studentRepository.findByUserIdAndIsDeletedFalse(userId)
                .map(Student::getId)
                .orElse(null);
    }

    // ── Helpers ────────────────────────────────────────────────────

    private String generateAdmissionNumber(String institutionCode, UUID institutionId) {
        long count = studentRepository.countByInstitutionId(institutionId) + 1;
        String year = String.valueOf(Year.now().getValue());
        return String.format("%s-%s-%05d", institutionCode, year, count);
    }

    private StudentResponse toResponse(Student student) {
        User user = userRepository.findById(student.getUserId()).orElse(null);

        String firstName = user != null ? user.getFirstName() : null;
        String middleName = user != null ? user.getMiddleName() : null;
        String lastName = user != null ? user.getLastName() : null;
        String email = user != null ? user.getEmail() : null;
        String phone = user != null ? user.getPhone() : null;

        return StudentResponse.builder()
                .id(student.getId())
                .userId(student.getUserId())
                .admissionNumber(student.getAdmissionNumber())
                .status(student.getStatus())
                .firstName(firstName)
                .middleName(middleName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .address(student.getAddress())
                .city(student.getCity())
                .region(student.getRegion())
                .guardianName(student.getGuardianName())
                .guardianPhone(student.getGuardianPhone())
                .guardianRelationship(student.getGuardianRelationship())
                .enrollmentDate(student.getEnrollmentDate())
                .institutionId(student.getInstitutionId())
                .build();
    }
}
