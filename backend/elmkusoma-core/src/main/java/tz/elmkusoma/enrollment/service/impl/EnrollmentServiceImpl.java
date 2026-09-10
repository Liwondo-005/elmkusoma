package tz.elmkusoma.enrollment.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.security.OwnershipGuard;
import tz.elmkusoma.enrollment.domain.Enrollment;
import tz.elmkusoma.enrollment.domain.TransferRecord;
import tz.elmkusoma.enrollment.dto.request.EnrollmentRequest;
import tz.elmkusoma.enrollment.dto.request.TransferRequest;
import tz.elmkusoma.enrollment.dto.response.EnrollmentResponse;
import tz.elmkusoma.enrollment.dto.response.TransferResponse;
import tz.elmkusoma.enrollment.repository.EnrollmentRepository;
import tz.elmkusoma.enrollment.repository.TransferRecordRepository;
import tz.elmkusoma.enrollment.service.EnrollmentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final TransferRecordRepository transferRecordRepository;

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
    public List<EnrollmentResponse> getEnrollmentsByStudent(UUID studentId, UUID institutionId) {
        return enrollmentRepository.findByStudentIdAndIsDeletedFalse(studentId).stream()
                .filter(e -> e.getInstitutionId().equals(institutionId))
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EnrollmentResponse> getEnrollmentsByClass(UUID classGroupId, UUID institutionId) {
        return enrollmentRepository.findByClassGroupIdAndIsDeletedFalse(classGroupId).stream()
                .filter(e -> e.getInstitutionId().equals(institutionId))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public EnrollmentResponse updateStatus(UUID enrollmentId, UUID institutionId, Enrollment.EnrollmentStatus status) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .filter(e -> !e.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
        OwnershipGuard.verifyInstitution(enrollment.getInstitutionId(), institutionId, "enrollment");

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
        OwnershipGuard.verifyInstitution(enrollment.getInstitutionId(), institutionId, "enrollment");

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
    public List<TransferResponse> getTransferHistory(UUID enrollmentId, UUID institutionId) {
        Enrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .filter(e -> !e.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + enrollmentId));
        OwnershipGuard.verifyInstitution(enrollment.getInstitutionId(), institutionId, "enrollment");
        return transferRecordRepository.findByEnrollmentIdAndIsDeletedFalse(enrollmentId).stream()
                .map(this::toTransferResponse)
                .toList();
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
