package tz.elmkusoma.highereducation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.highereducation.domain.DeepLearningContent;
import tz.elmkusoma.highereducation.domain.DeepContentType;
import tz.elmkusoma.highereducation.dto.DeepLearningContentDTO;
import tz.elmkusoma.highereducation.repository.DeepLearningContentRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DeepLearningContentService {

    private final DeepLearningContentRepository contentRepository;

    public DeepLearningContentDTO createContent(DeepLearningContentDTO dto) {
        DeepLearningContent content = DeepLearningContent.builder()
                .studentId(dto.getStudentId())
                .courseId(dto.getCourseId())
                .moduleId(dto.getModuleId())
                .title(dto.getTitle())
                .contentType(DeepContentType.valueOf(
                        dto.getContentType() != null ? dto.getContentType() : "ARTICLE"))
                .contentText(dto.getContentText())
                .fileUrl(dto.getFileUrl())
                .difficultyLevel(dto.getDifficultyLevel() != null ? dto.getDifficultyLevel() : "INTERMEDIATE")
                .tags(dto.getTags())
                .isCompleted(dto.getIsCompleted() != null ? dto.getIsCompleted() : false)
                .timeSpentMinutes(dto.getTimeSpentMinutes() != null ? dto.getTimeSpentMinutes() : 0)
                .notes(dto.getNotes())
                .institutionId(dto.getInstitutionId() != null ? dto.getInstitutionId() : UUID.randomUUID())
                .build();
        return toDTO(contentRepository.save(content));
    }

    public DeepLearningContentDTO getContent(UUID id) {
        DeepLearningContent content = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found"));
        return toDTO(content);
    }

    public List<DeepLearningContentDTO> getStudentContent(UUID studentId) {
        return contentRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<DeepLearningContentDTO> getStudentCourseContent(UUID studentId, UUID courseId) {
        return contentRepository.findByStudentIdAndCourseId(studentId, courseId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public DeepLearningContentDTO updateContent(UUID id, DeepLearningContentDTO dto) {
        DeepLearningContent content = contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found"));
        if (dto.getTitle() != null) content.setTitle(dto.getTitle());
        if (dto.getContentText() != null) content.setContentText(dto.getContentText());
        if (dto.getIsCompleted() != null) content.setIsCompleted(dto.getIsCompleted());
        if (dto.getTimeSpentMinutes() != null) content.setTimeSpentMinutes(dto.getTimeSpentMinutes());
        if (dto.getNotes() != null) content.setNotes(dto.getNotes());
        if (dto.getTags() != null) content.setTags(dto.getTags());
        return toDTO(contentRepository.save(content));
    }

    public void deleteContent(UUID id) {
        if (!contentRepository.existsById(id))
            throw new ResourceNotFoundException("Content not found");
        contentRepository.deleteById(id);
    }

    private DeepLearningContentDTO toDTO(DeepLearningContent c) {
        return DeepLearningContentDTO.builder()
                .id(c.getId()).studentId(c.getStudentId()).courseId(c.getCourseId())
                .moduleId(c.getModuleId()).title(c.getTitle()).contentType(c.getContentType().name())
                .contentText(c.getContentText()).fileUrl(c.getFileUrl())
                .difficultyLevel(c.getDifficultyLevel()).tags(c.getTags())
                .isCompleted(c.getIsCompleted()).timeSpentMinutes(c.getTimeSpentMinutes())
                .notes(c.getNotes())
                .build();
    }
}
