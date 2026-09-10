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

    List<EnrollmentResponse> getEnrollmentsByStudent(UUID studentId, UUID institutionId);

    List<EnrollmentResponse> getEnrollmentsByClass(UUID classGroupId, UUID institutionId);

    EnrollmentResponse updateStatus(UUID enrollmentId, UUID institutionId, Enrollment.EnrollmentStatus status);

    TransferResponse transfer(UUID enrollmentId, UUID institutionId, TransferRequest request, UUID performedBy);

    List<TransferResponse> getTransferHistory(UUID enrollmentId, UUID institutionId);
}
