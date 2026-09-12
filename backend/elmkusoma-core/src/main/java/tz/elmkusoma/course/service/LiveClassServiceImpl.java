package tz.elmkusoma.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.dto.CreateLiveClassRequest;
import tz.elmkusoma.course.dto.LiveClassResponse;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;
import tz.elmkusoma.teacher.domain.Teacher;
import tz.elmkusoma.teacher.repository.TeacherRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class LiveClassServiceImpl implements LiveClassService {

    private final LiveClassRepository liveClassRepository;
    private final TeacherRepository teacherRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LiveClassResponse> getTeacherLiveClasses(UUID teacherId) {
        return liveClassRepository.findByTeacherIdAndIsDeletedFalse(teacherId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public LiveClassResponse createLiveClass(UUID teacherId, UUID institutionId, CreateLiveClassRequest request) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "id", teacherId));

        LiveClass liveClass = LiveClass.builder()
                .teacherId(teacherId)
                .title(request.getTitle())
                .description(request.getDescription())
                .scheduledAt(parseDateTime(request.getScheduledAt()))
                .durationMinutes(request.getDurationMinutes() != null ? request.getDurationMinutes() : 60)
                .status("SCHEDULED")
                .meetingUrl(request.getMeetingUrl())
                .subjectId(request.getSubjectId())
                .maxParticipants(request.getMaxParticipants())
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

        if (request.getTitle() != null) liveClass.setTitle(request.getTitle());
        if (request.getDescription() != null) liveClass.setDescription(request.getDescription());
        if (request.getScheduledAt() != null) liveClass.setScheduledAt(parseDateTime(request.getScheduledAt()));
        if (request.getDurationMinutes() != null) liveClass.setDurationMinutes(request.getDurationMinutes());
        if (request.getMeetingUrl() != null) liveClass.setMeetingUrl(request.getMeetingUrl());
        if (request.getSubjectId() != null) liveClass.setSubjectId(request.getSubjectId());
        if (request.getMaxParticipants() != null) liveClass.setMaxParticipants(request.getMaxParticipants());

        LiveClass saved = liveClassRepository.save(liveClass);
        return mapToResponse(saved);
    }

    @Override
    public void cancelLiveClass(UUID teacherId, UUID liveClassId) {
        LiveClass liveClass = liveClassRepository.findById(liveClassId)
                .filter(lc -> lc.getTeacherId().equals(teacherId) && !lc.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("LiveClass", "id", liveClassId));

        liveClass.setStatus("CANCELLED");
        liveClassRepository.save(liveClass);
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

        return LiveClassResponse.builder()
                .id(liveClass.getId())
                .title(liveClass.getTitle())
                .description(liveClass.getDescription())
                .scheduledAt(liveClass.getScheduledAt() != null ? liveClass.getScheduledAt().toString() : null)
                .durationMinutes(liveClass.getDurationMinutes())
                .status(liveClass.getStatus())
                .meetingUrl(liveClass.getMeetingUrl())
                .maxParticipants(liveClass.getMaxParticipants())
                .subjectName(subjectName)
                .teacherName(teacherName)
                .teacherId(liveClass.getTeacherId())
                .build();
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.parse(dateTimeStr);
        }
    }
}
