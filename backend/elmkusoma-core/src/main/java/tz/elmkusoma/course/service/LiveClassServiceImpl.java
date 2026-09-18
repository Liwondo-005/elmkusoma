package tz.elmkusoma.course.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.attendance.domain.AttendanceRecord;
import tz.elmkusoma.attendance.repository.AttendanceRecordRepository;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.domain.LiveClass.LiveClassStatus;
import tz.elmkusoma.course.domain.LiveClassSessionType;
import tz.elmkusoma.course.dto.CreateLiveClassRequest;
import tz.elmkusoma.course.dto.LiveClassResponse;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.liveclass.domain.LiveClassParticipant;
import tz.elmkusoma.liveclass.repository.LiveClassParticipantRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LiveClassServiceImpl implements LiveClassService {

    private final LiveClassRepository liveClassRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;
    private final LiveClassParticipantRepository participantRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LiveClassResponse> getTeacherLiveClasses(UUID teacherId) {
        return liveClassRepository.findByTeacherIdAndIsDeletedFalse(teacherId).stream()
                .filter(lc -> !"CANCELLED".equals(lc.getStatus()))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiveClassResponse> getUpcomingClasses(UUID institutionId) {
        return liveClassRepository.findByInstitutionIdAndIsDeletedFalse(institutionId).stream()
                .filter(lc -> "SCHEDULED".equals(lc.getStatus()))
                .filter(lc -> lc.getScheduledAt() != null && lc.getScheduledAt().isAfter(LocalDateTime.now()))
                .sorted(Comparator.comparing(LiveClass::getScheduledAt))
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LiveClassResponse> getLiveClassesByStatus(UUID institutionId, String status) {
        return liveClassRepository.findByInstitutionIdAndStatusAndIsDeletedFalse(institutionId, status).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public LiveClassResponse createLiveClass(UUID teacherId, UUID institutionId, CreateLiveClassRequest request) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));

        LocalDateTime scheduledAt = parseDateTime(request.getScheduledAt());
        if (scheduledAt == null) {
            throw new IllegalArgumentException("Scheduled time is required");
        }
        if (scheduledAt.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Scheduled time must be in the future");
        }

        int duration = request.getDurationMinutes() != null ? request.getDurationMinutes() : 60;
        LocalDateTime endTime = scheduledAt.plusMinutes(duration);
        List<LiveClass> overlaps = liveClassRepository.findOverlappingForTeacher(
                teacherId, scheduledAt.minusMinutes(1), endTime.plusMinutes(1));
        if (!overlaps.isEmpty()) {
            LiveClass conflict = overlaps.get(0);
            throw new IllegalArgumentException("Schedule conflict: you already have \"" + conflict.getTitle()
                    + "\" scheduled at " + conflict.getScheduledAt());
        }

        LiveClass liveClass = LiveClass.builder()
                .teacherId(teacherId)
                .title(request.getTitle())
                .description(request.getDescription())
                .scheduledAt(scheduledAt)
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 60)
                .status(LiveClassStatus.SCHEDULED.name())
                .subjectId(request.getSubjectId())
                .maxParticipants(request.getMaxParticipants())
                .classGroupId(request.getClassGroupId())
                .recordingEnabled(Boolean.TRUE.equals(request.getRecordingEnabled()))
                .sessionType(request.getSessionType() != null ? LiveClassSessionType.valueOf(request.getSessionType()) : LiveClassSessionType.LECTURE)
                .build();
        liveClass.setInstitutionId(institutionId);

        LiveClass saved = liveClassRepository.save(liveClass);
        return mapToResponse(saved);
    }

    @Override
    public LiveClassResponse updateLiveClass(UUID teacherId, UUID liveClassId, CreateLiveClassRequest request) {
        LiveClass liveClass = liveClassRepository.findById(liveClassId)
                .filter(lc -> lc.getTeacherId().equals(teacherId) && !lc.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("LiveClass", "id", liveClassId));

        String currentStatus = liveClass.getStatus();
        if (LiveClassStatus.IN_PROGRESS.name().equals(currentStatus)
                || LiveClassStatus.LIVE.name().equals(currentStatus)
                || LiveClassStatus.COMPLETED.name().equals(currentStatus)) {
            throw new IllegalArgumentException("Cannot edit a " + currentStatus.toLowerCase().replace('_', ' ') + " class");
        }

        if (request.getTitle() != null) liveClass.setTitle(request.getTitle());
        if (request.getDescription() != null) liveClass.setDescription(request.getDescription());
        if (request.getScheduledAt() != null) {
            LocalDateTime newScheduledAt = parseDateTime(request.getScheduledAt());
            if (newScheduledAt != null && newScheduledAt.isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Scheduled time must be in the future");
            }
            liveClass.setScheduledAt(newScheduledAt);
        }
        if (request.getDurationMinutes() != null) liveClass.setDurationMinutes(request.getDurationMinutes());
        if (request.getSubjectId() != null) liveClass.setSubjectId(request.getSubjectId());
        if (request.getClassGroupId() != null) liveClass.setClassGroupId(request.getClassGroupId());
        if (request.getMaxParticipants() != null) liveClass.setMaxParticipants(request.getMaxParticipants());
        if (request.getRecordingEnabled() != null) liveClass.setRecordingEnabled(request.getRecordingEnabled());
        if (request.getSessionType() != null) {
            liveClass.setSessionType(LiveClassSessionType.valueOf(request.getSessionType()));
        }

        if (liveClass.getScheduledAt() != null) {
            int dur = liveClass.getDurationMinutes() != null ? liveClass.getDurationMinutes() : 60;
            LocalDateTime endTime = liveClass.getScheduledAt().plusMinutes(dur);
            List<LiveClass> overlaps = liveClassRepository.findOverlappingForTeacher(
                    teacherId, liveClass.getScheduledAt().minusMinutes(1), endTime.plusMinutes(1));
            overlaps.removeIf(lc -> lc.getId().equals(liveClassId));
            if (!overlaps.isEmpty()) {
                LiveClass conflict = overlaps.get(0);
                throw new IllegalArgumentException("Schedule conflict: you already have \"" + conflict.getTitle()
                        + "\" scheduled at " + conflict.getScheduledAt());
            }
        }

        LiveClass saved = liveClassRepository.save(liveClass);
        return mapToResponse(saved);
    }

    @Override
    public void cancelLiveClass(UUID teacherId, UUID liveClassId) {
        LiveClass liveClass = liveClassRepository.findById(liveClassId)
                .filter(lc -> lc.getTeacherId().equals(teacherId) && !lc.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("LiveClass", "id", liveClassId));

        String currentStatus = liveClass.getStatus();
        if (LiveClassStatus.COMPLETED.name().equals(currentStatus)) {
            throw new IllegalArgumentException("Cannot cancel a completed class");
        }
        if (LiveClassStatus.CANCELLED.name().equals(currentStatus)) {
            throw new IllegalArgumentException("Class is already cancelled");
        }

        liveClass.setStatus(LiveClassStatus.CANCELLED.name());
        liveClassRepository.save(liveClass);
    }

    @Override
    public LiveClassResponse startSession(UUID teacherId, UUID liveClassId) {
        LiveClass liveClass = liveClassRepository.findById(liveClassId)
                .filter(lc -> lc.getTeacherId().equals(teacherId) && !lc.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("LiveClass", "id", liveClassId));

        if (!LiveClassStatus.SCHEDULED.name().equals(liveClass.getStatus())) {
            throw new IllegalArgumentException("Only SCHEDULED classes can be started. Current status: " + liveClass.getStatus());
        }

        liveClass.setStatus(LiveClassStatus.IN_PROGRESS.name());
        LiveClass saved = liveClassRepository.save(liveClass);
        return mapToResponse(saved);
    }

    @Override
    public LiveClassResponse endSession(UUID teacherId, UUID liveClassId, UUID markedBy) {
        LiveClass liveClass = liveClassRepository.findById(liveClassId)
                .filter(lc -> lc.getTeacherId().equals(teacherId) && !lc.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("LiveClass", "id", liveClassId));

        if (!LiveClassStatus.IN_PROGRESS.name().equals(liveClass.getStatus())) {
            throw new IllegalArgumentException("Only IN_PROGRESS classes can be ended. Current status: " + liveClass.getStatus());
        }

        liveClass.setStatus(LiveClassStatus.COMPLETED.name());
        LiveClass saved = liveClassRepository.save(liveClass);

        createAttendanceFromParticipants(saved, markedBy);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LiveClassResponse getLiveClassById(UUID liveClassId) {
        LiveClass liveClass = liveClassRepository.findById(liveClassId)
                .filter(lc -> !lc.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("LiveClass", "id", liveClassId));
        return mapToResponse(liveClass);
    }

    private void createAttendanceFromParticipants(LiveClass liveClass, UUID markedBy) {
        if (liveClass.getClassGroupId() == null) {
            log.warn("No classGroupId on LiveClass {}, skipping attendance creation", liveClass.getId());
            return;
        }

        List<LiveClassParticipant> participants = participantRepository
                .findByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(liveClass.getId());

        LocalDate today = LocalDate.now();
        UUID classGroupId = liveClass.getClassGroupId();

        for (LiveClassParticipant participant : participants) {
            boolean alreadyExists = attendanceRecordRepository
                    .findByClassGroupIdAndAttendanceDateAndIsDeletedFalse(classGroupId, today)
                    .stream()
                    .anyMatch(r -> r.getStudentId().equals(participant.getUserId()));

            if (!alreadyExists) {
                AttendanceRecord record = AttendanceRecord.builder()
                        .institutionId(liveClass.getInstitutionId())
                        .studentId(participant.getUserId())
                        .classGroupId(classGroupId)
                        .attendanceDate(today)
                        .status(AttendanceRecord.AttendanceStatus.PRESENT)
                        .markedBy(markedBy)
                        .remarks("Auto-recorded from live class participation")
                        .build();
                attendanceRecordRepository.save(record);
            }
        }

        log.info("Created attendance records for {} participants in live class {}",
                participants.size(), liveClass.getId());
    }

    private LiveClassResponse mapToResponse(LiveClass liveClass) {
        String subjectName = null;
        if (liveClass.getSubjectId() != null) {
            subjectName = subjectRepository.findById(liveClass.getSubjectId())
                    .map(Subject::getName).orElse(null);
        }

        String teacherName = null;
        Teacher teacher = teacherRepository.findById(liveClass.getTeacherId()).orElse(null);
        if (teacher != null) {
            User user = userRepository.findById(teacher.getUserId()).orElse(null);
            if (user != null) {
                teacherName = user.getFullName();
            }
        }

        long currentParticipants = participantRepository
                .countByLiveClassIdAndIsDeletedFalseAndLeftAtIsNull(liveClass.getId());

        return LiveClassResponse.builder()
                .id(liveClass.getId())
                .title(liveClass.getTitle())
                .description(liveClass.getDescription())
                .scheduledAt(liveClass.getScheduledAt() != null ? liveClass.getScheduledAt().toString() : null)
                .durationMinutes(liveClass.getDurationMinutes())
                .status(liveClass.getStatus())
                .maxParticipants(liveClass.getMaxParticipants())
                .subjectName(subjectName)
                .teacherName(teacherName)
                .teacherId(liveClass.getTeacherId())
                .subjectId(liveClass.getSubjectId())
                .classGroupId(liveClass.getClassGroupId())
                .recordingUrl(liveClass.getRecordingUrl())
                .recordingEnabled(Boolean.TRUE.equals(liveClass.getRecordingEnabled()))
                .sessionType(liveClass.getSessionType() != null ? liveClass.getSessionType().name() : "LECTURE")
                .currentParticipants((int) currentParticipants)
                .canJoin("IN_PROGRESS".equals(liveClass.getStatus()) || "LIVE".equals(liveClass.getStatus()))
                .createdAt(liveClass.getCreatedAt() != null ? liveClass.getCreatedAt().toString() : null)
                .build();
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr);
            } catch (Exception ex) {
                throw new IllegalArgumentException("Invalid date/time format: " + dateTimeStr
                        + ". Expected format: yyyy-MM-ddTHH:mm:ss");
            }
        }
    }
}
