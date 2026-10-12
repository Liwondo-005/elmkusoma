package tz.elmkusoma.enrollment.service;

import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.enrollment.domain.Enrollment;
import tz.elmkusoma.enrollment.dto.request.EnrollmentRequest;
import tz.elmkusoma.enrollment.dto.request.TransferRequest;
import tz.elmkusoma.enrollment.dto.response.EnrollmentResponse;
import tz.elmkusoma.enrollment.dto.response.TransferResponse;

import java.util.List;
import java.util.UUID;

public interface EnrollmentService {

    EnrollmentResponse enroll(UUID institutionId, EnrollmentRequest request);

    PageResponse<EnrollmentResponse> getEnrollments(UUID institutionId, int page, int size);

    List<EnrollmentResponse> getEnrollmentsByStudent(UUID studentId);

    /** Audit B-22: the caller's own enrollments, resolved from the authenticated user. */
    List<EnrollmentResponse> getEnrollmentsByUserId(UUID userId);

    /** Audit Phase 6: approval step for a placement created as PENDING. */
    EnrollmentResponse approveEnrollment(UUID institutionId, UUID enrollmentId);

    EnrollmentResponse rejectEnrollment(UUID institutionId, UUID enrollmentId, String reason);

    List<EnrollmentResponse> getEnrollmentsByClass(UUID classGroupId);

    EnrollmentResponse updateStatus(UUID enrollmentId, Enrollment.EnrollmentStatus status);

    TransferResponse transfer(UUID enrollmentId, UUID institutionId, TransferRequest request, UUID performedBy);

    List<TransferResponse> getTransferHistory(UUID enrollmentId);
}
