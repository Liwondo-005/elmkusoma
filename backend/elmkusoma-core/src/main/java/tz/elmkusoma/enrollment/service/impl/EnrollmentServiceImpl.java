package tz.elmkusoma.enrollment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.enrollment.domain.Enrollment;
import tz.elmkusoma.enrollment.domain.TransferRecord;
import tz.elmkusoma.enrollment.dto.request.EnrollmentRequest;
import tz.elmkusoma.enrollment.dto.request.TransferRequest;
import tz.elmkusoma.enrollment.dto.response.EnrollmentResponse;
import tz.elmkusoma.enrollment.dto.response.TransferResponse;
import tz.elmkusoma.enrollment.repository.EnrollmentRepository;
import tz.elmkusoma.enrollment.repository.TransferRecordRepository;
import tz.elmkusoma.enrollment.service.EnrollmentService;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.repository.ParentStudentLinkRepository;
import tz.elmkusoma.student.domain.Student;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final TransferRecordRepository transferRecordRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final ParentStudentLinkRepository parentStudentLinkRepository;

    @Override
    public EnrollmentResponse enroll(UUID institutionId, EnrollmentRequest request) {
        boolean alreadyEnrolled = enrollmentRepository
                .existsByStudentIdAndClassGroupIdAndAcademicYearIdAndStatusAndIsDeletedFalse(
                        request.getStudentId(), request.getClassGroupId(),
                        request.getAcademicYearId(), Enrollment.EnrollmentStatus.ENROLLED);

        if (alreadyEnrolled) {
            throw new IllegalArgumentException("Student is already enrolled in this class for the given academic year");
        }

        Enrollment enrollment = Enrollment.builder()
                .institutionId(institutionId)
                .studentId(request.getStudentId())
                .classGroupId(request.getClassGroupId())
                .academicYearId(request.getAcademicYearId())
                .status(Enrollment.EnrollmentStatus.ENROLLED)
                .enrolledAt(LocalDateTime.now())
                .build();

        return toResponse(enrollmentRepository.save(enrollment));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<EnrollmentResponse> getEnrollments(UUID institutionId, int page, int size) {
        Page<Enrollment> enrollmentPage = enrollmentRepository.findByInstitutionIdAndIsDeletedFalse(
                institutionId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<EnrollmentResponse> content = enrollmentPage.getContent().stream()
                .map(this::toResponse)
                .toList();

        return new PageResponse<>(
                content,
                enrollmentPage.getNumber(),
                enrollmentPage.getSize(),
                enrollmentPage.getTotalElements(),
                enrollmentPage.getTotalPages(),
                enrollmentPage.isFirst(),
                enrollmentPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getEnrollmentsByStudent(UUID studentId) {
        requireStudentAccess(studentId);
        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId);
        String role = callerRole();
        if (role == null || "ADMIN".equals(role) || "STUDENT".equals(role)
                || "OTHER_LEARNER".equals(role) || "PARENT".equals(role)) {
            return enrollments.stream()
                    .map(this::toResponse)
                    .toList();
        }
        UUID callerInstitutionId = callerInstitutionId();
        return enrollments.stream()
                .filter(e -> callerInstitutionId != null && callerInstitutionId.equals(e.getInstitutionId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getEnrollmentsByClass(UUID classGroupId) {
        List<Enrollment> enrollments = enrollmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId);
        String role = callerRole();
        if (role == null && RequestContextHolder.getRequestAttributes() != null) {
            // §48: an HTTP request without a resolved role must fail closed
            throw new ForbiddenException("enrollment", "access");
        }
        if (role == null || "ADMIN".equals(role)) {
            return enrollments.stream()
                    .map(this::toResponse)
                    .toList();
        }
        UUID callerInstitutionId = callerInstitutionId();
        return enrollments.stream()
                .filter(e -> callerInstitutionId != null && callerInstitutionId.equals(e.getInstitutionId()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EnrollmentResponse updateStatus(UUID enrollmentId, Enrollment.EnrollmentStatus status) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .filter(e -> !e.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));

        requireEnrollmentAccess(enrollment);

        enrollment.setStatus(status);

        switch (status) {
            case WITHDRAWN -> enrollment.setWithdrawnAt(LocalDateTime.now());
            case COMPLETED -> enrollment.setCompletedAt(LocalDateTime.now());
            default -> { /* no-op */ }
        }

        return toResponse(enrollmentRepository.save(enrollment));
    }

    @Override
    public TransferResponse transfer(UUID enrollmentId, UUID institutionId, TransferRequest request, UUID performedBy) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .filter(e -> !e.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));

        if (!institutionId.equals(enrollment.getInstitutionId())) {
            throw new ForbiddenException("enrollment", "transfer");
        }
        requireEnrollmentAccess(enrollment);

        TransferRecord transfer = TransferRecord.builder()
                .institutionId(institutionId)
                .enrollmentId(enrollmentId)
                .fromClassGroupId(enrollment.getClassGroupId())
                .toClassGroupId(request.getToClassGroupId())
                .reason(request.getReason())
                .transferredAt(LocalDateTime.now())
                .transferredBy(performedBy)
                .build();

        enrollment.setClassGroupId(request.getToClassGroupId());
        enrollmentRepository.save(enrollment);

        return toTransferResponse(transferRecordRepository.save(transfer));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferResponse> getTransferHistory(UUID enrollmentId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .filter(e -> !e.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));

        requireEnrollmentAccess(enrollment);

        return transferRecordRepository.findByEnrollmentIdAndIsDeletedFalse(enrollmentId).stream()
                .map(this::toTransferResponse)
                .toList();
    }

    private String callerRole() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        Object role = attrs.getAttribute("userRole", RequestAttributes.SCOPE_REQUEST);
        return role instanceof String s ? s : null;
    }

    private UUID callerUserId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        Object id = attrs.getAttribute("userId", RequestAttributes.SCOPE_REQUEST);
        return id instanceof UUID uuid ? uuid : null;
    }

    private UUID callerInstitutionId() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        Object id = attrs.getAttribute("institutionId", RequestAttributes.SCOPE_REQUEST);
        return id instanceof UUID uuid ? uuid : null;
    }

    private UUID ownStudentId(UUID callerUserId) {
        if (callerUserId == null) {
            return null;
        }
        return studentRepository.findByUserIdAndIsDeletedFalse(callerUserId)
                .map(Student::getId)
                .orElse(null);
    }

    private boolean isLinkedChild(UUID callerUserId, UUID studentId) {
        if (callerUserId == null || studentId == null) {
            return false;
        }
        return parentRepository.findAllByUserId(callerUserId).stream()
                .map(Parent::getId)
                .anyMatch(parentId -> parentStudentLinkRepository
                        .existsByParentIdAndStudentIdAndIsDeletedFalse(parentId, studentId));
    }

    private void requireStudentAccess(UUID studentId) {
        String role = callerRole();
        if (role == null) {
            if (RequestContextHolder.getRequestAttributes() != null) {
                // §48: an HTTP request without a resolved role must fail closed
                throw new ForbiddenException("enrollment", "access");
            }
            return; // no request context — internal programmatic access
        }
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("STUDENT".equals(role) || "OTHER_LEARNER".equals(role)) {
            if (!studentId.equals(ownStudentId(callerUserId()))) {
                throw new ForbiddenException("enrollment", "access");
            }
            return;
        }
        if ("PARENT".equals(role)) {
            if (!isLinkedChild(callerUserId(), studentId)) {
                throw new ForbiddenException("enrollment", "access");
            }
        }
    }

    private void requireEnrollmentAccess(Enrollment enrollment) {
        String role = callerRole();
        if (role == null) {
            if (RequestContextHolder.getRequestAttributes() != null) {
                // §48: an HTTP request without a resolved role must fail closed
                throw new ForbiddenException("enrollment", "access");
            }
            return; // no request context — internal programmatic access
        }
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("STUDENT".equals(role)) {
            if (!enrollment.getStudentId().equals(ownStudentId(callerUserId()))) {
                throw new ForbiddenException("enrollment", "access");
            }
            return;
        }
        if ("PARENT".equals(role)) {
            if (!isLinkedChild(callerUserId(), enrollment.getStudentId())) {
                throw new ForbiddenException("enrollment", "access");
            }
            return;
        }
        UUID callerInstitutionId = callerInstitutionId();
        if (callerInstitutionId == null || !callerInstitutionId.equals(enrollment.getInstitutionId())) {
            throw new ForbiddenException("enrollment", "access");
        }
    }

    private EnrollmentResponse toResponse(Enrollment e) {
        return EnrollmentResponse.builder()
                .id(e.getId())
                .studentId(e.getStudentId())
                .classGroupId(e.getClassGroupId())
                .academicYearId(e.getAcademicYearId())
                .status(e.getStatus())
                .enrolledAt(e.getEnrolledAt())
                .withdrawnAt(e.getWithdrawnAt())
                .withdrawReason(e.getWithdrawReason())
                .completedAt(e.getCompletedAt())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private TransferResponse toTransferResponse(TransferRecord t) {
        return TransferResponse.builder()
                .id(t.getId())
                .enrollmentId(t.getEnrollmentId())
                .fromClassGroupId(t.getFromClassGroupId())
                .toClassGroupId(t.getToClassGroupId())
                .reason(t.getReason())
                .transferredAt(t.getTransferredAt())
                .transferredBy(t.getTransferredBy())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
