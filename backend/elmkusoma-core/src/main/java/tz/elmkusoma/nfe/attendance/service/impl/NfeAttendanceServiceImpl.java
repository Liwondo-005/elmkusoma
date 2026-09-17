package tz.elmkusoma.nfe.attendance.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.nfe.attendance.domain.NfeAttendance;
import tz.elmkusoma.nfe.attendance.dto.AttendanceRequest;
import tz.elmkusoma.nfe.attendance.dto.AttendanceResponse;
import tz.elmkusoma.nfe.attendance.repository.NfeAttendanceRepository;
import tz.elmkusoma.nfe.attendance.service.NfeAttendanceService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NfeAttendanceServiceImpl implements NfeAttendanceService {

    private final NfeAttendanceRepository attendanceRepository;

    @Override
    public AttendanceResponse recordAttendance(UUID institutionId, UUID providerId, AttendanceRequest request) {
        NfeAttendance attendance = NfeAttendance.builder()
                .providerId(providerId)
                .sessionId(request.getSessionId())
                .learnerId(request.getLearnerId())
                .status(NfeAttendance.AttendanceStatus.valueOf(request.getStatus()))
                .checkInTime(request.getCheckInTime())
                .checkOutTime(request.getCheckOutTime())
                .remarks(request.getRemarks())
                .markedBy(request.getMarkedBy())
                .build();
        attendance.setInstitutionId(institutionId);

        NfeAttendance saved = attendanceRepository.save(attendance);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse getAttendance(UUID institutionId, UUID attendanceId) {
        NfeAttendance attendance = attendanceRepository.findByIdAndInstitutionId(attendanceId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("NFE Attendance", "id", attendanceId));
        return mapToResponse(attendance);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> listAttendanceBySession(UUID institutionId, UUID sessionId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        List<NfeAttendance> allAttendance = attendanceRepository.findBySessionId(sessionId, institutionId);

        int start = Math.min(page * size, allAttendance.size());
        int end = Math.min((page + 1) * size, allAttendance.size());
        List<NfeAttendance> paged = allAttendance.subList(start, end);

        PageImpl<NfeAttendance> attendancePage = new PageImpl<>(
                paged, pageable, allAttendance.size());

        List<AttendanceResponse> content = attendancePage.getContent().stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
                content,
                attendancePage.getNumber(),
                attendancePage.getSize(),
                attendancePage.getTotalElements(),
                attendancePage.getTotalPages(),
                attendancePage.isFirst(),
                attendancePage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByLearner(UUID institutionId, UUID learnerId) {
        return attendanceRepository.findByLearnerId(learnerId, institutionId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendanceByProvider(UUID institutionId, UUID providerId) {
        return attendanceRepository.findByProviderId(providerId, institutionId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public AttendanceResponse updateAttendance(UUID institutionId, UUID attendanceId, AttendanceRequest request) {
        NfeAttendance attendance = attendanceRepository.findByIdAndInstitutionId(attendanceId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("NFE Attendance", "id", attendanceId));

        if (request.getSessionId() != null) attendance.setSessionId(request.getSessionId());
        if (request.getLearnerId() != null) attendance.setLearnerId(request.getLearnerId());
        if (request.getStatus() != null) attendance.setStatus(NfeAttendance.AttendanceStatus.valueOf(request.getStatus()));
        if (request.getCheckInTime() != null) attendance.setCheckInTime(request.getCheckInTime());
        if (request.getCheckOutTime() != null) attendance.setCheckOutTime(request.getCheckOutTime());
        if (request.getRemarks() != null) attendance.setRemarks(request.getRemarks());
        if (request.getMarkedBy() != null) attendance.setMarkedBy(request.getMarkedBy());

        NfeAttendance saved = attendanceRepository.save(attendance);
        return mapToResponse(saved);
    }

    @Override
    public void deleteAttendance(UUID institutionId, UUID attendanceId) {
        NfeAttendance attendance = attendanceRepository.findByIdAndInstitutionId(attendanceId, institutionId)
                .orElseThrow(() -> new ResourceNotFoundException("NFE Attendance", "id", attendanceId));
        attendance.setIsDeleted(true);
        attendanceRepository.save(attendance);
    }

    private AttendanceResponse mapToResponse(NfeAttendance attendance) {
        return AttendanceResponse.builder()
                .id(attendance.getId())
                .institutionId(attendance.getInstitutionId())
                .providerId(attendance.getProviderId())
                .sessionId(attendance.getSessionId())
                .learnerId(attendance.getLearnerId())
                .status(attendance.getStatus().name())
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .remarks(attendance.getRemarks())
                .markedBy(attendance.getMarkedBy())
                .createdAt(attendance.getCreatedAt())
                .build();
    }
}
