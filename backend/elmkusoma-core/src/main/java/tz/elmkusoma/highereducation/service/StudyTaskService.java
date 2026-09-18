package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.StudyTask;
import tz.elmkusoma.highereducation.domain.StudyTaskPriority;
import tz.elmkusoma.highereducation.domain.StudyTaskType;
import tz.elmkusoma.highereducation.dto.StudyTaskDTO;
import tz.elmkusoma.highereducation.repository.StudyTaskRepository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class StudyTaskService {

    private final StudyTaskRepository studyTaskRepository;

    public StudyTaskDTO createStudyTask(StudyTaskDTO dto) {
        StudyTask task = StudyTask.builder()
                .institutionId(dto.getInstitutionId())
                .studentId(dto.getStudentId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .taskType(dto.getTaskType() != null ? dto.getTaskType() : StudyTaskType.STUDY)
                .priority(dto.getPriority() != null ? dto.getPriority() : StudyTaskPriority.MEDIUM)
                .subjectId(dto.getSubjectId())
                .scheduledDate(dto.getScheduledDate())
                .scheduledTime(dto.getScheduledTime())
                .durationMinutes(dto.getDurationMinutes())
                .isCompleted(dto.getIsCompleted() != null ? dto.getIsCompleted() : false)
                .notes(dto.getNotes())
                .build();
        StudyTask saved = studyTaskRepository.save(task);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<StudyTaskDTO> getStudyTasks(UUID institutionId) {
        return studyTaskRepository.findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudyTaskDTO getStudyTask(UUID id) {
        StudyTask task = studyTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudyTask", "id", id));
        return toDTO(task);
    }

    public StudyTaskDTO updateStudyTask(UUID id, StudyTaskDTO dto) {
        StudyTask task = studyTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudyTask", "id", id));
        task.setStudentId(dto.getStudentId());
        task.setTitle(dto.getTitle());
        task.setDescription(dto.getDescription());
        task.setTaskType(dto.getTaskType());
        task.setPriority(dto.getPriority());
        task.setSubjectId(dto.getSubjectId());
        task.setScheduledDate(dto.getScheduledDate());
        task.setScheduledTime(dto.getScheduledTime());
        task.setDurationMinutes(dto.getDurationMinutes());
        task.setIsCompleted(dto.getIsCompleted());
        task.setNotes(dto.getNotes());
        StudyTask saved = studyTaskRepository.save(task);
        return toDTO(saved);
    }

    public void deleteStudyTask(UUID id) {
        StudyTask task = studyTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudyTask", "id", id));
        task.setIsDeleted(true);
        studyTaskRepository.save(task);
    }

    @Transactional(readOnly = true)
    public List<StudyTaskDTO> getStudentTasks(UUID studentId, UUID institutionId) {
        return studyTaskRepository.findByStudentIdAndIsDeletedFalse(studentId)
                .stream()
                .filter(t -> institutionId.equals(t.getInstitutionId()))
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudyTaskDTO> getTodayTasks(UUID studentId) {
        return studyTaskRepository.findByStudentIdAndScheduledDateAndIsDeletedFalse(studentId, LocalDate.now())
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudyTaskDTO> getWeekTasks(UUID studentId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
        return studyTaskRepository.findByStudentIdAndScheduledDateBetweenAndIsDeletedFalse(studentId, weekStart, weekEnd)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public StudyTaskDTO completeStudyTask(UUID id) {
        StudyTask task = studyTaskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StudyTask", "id", id));
        task.setIsCompleted(true);
        task.setCompletedDate(LocalDateTime.now());
        StudyTask saved = studyTaskRepository.save(task);
        return toDTO(saved);
    }

    private StudyTaskDTO toDTO(StudyTask task) {
        return StudyTaskDTO.builder()
                .id(task.getId())
                .institutionId(task.getInstitutionId())
                .studentId(task.getStudentId())
                .title(task.getTitle())
                .description(task.getDescription())
                .taskType(task.getTaskType())
                .priority(task.getPriority())
                .subjectId(task.getSubjectId())
                .scheduledDate(task.getScheduledDate())
                .scheduledTime(task.getScheduledTime())
                .durationMinutes(task.getDurationMinutes())
                .isCompleted(task.getIsCompleted())
                .completedDate(task.getCompletedDate())
                .notes(task.getNotes())
                .build();
    }
}
