package tz.elmkusoma.course.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.academic.domain.Subject;
import tz.elmkusoma.academic.repository.SubjectRepository;
import tz.elmkusoma.course.domain.Announcement;
import tz.elmkusoma.course.dto.AnnouncementResponse;
import tz.elmkusoma.course.dto.CreateAnnouncementRequest;
import tz.elmkusoma.course.repository.AnnouncementRepository;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getTeacherAnnouncements(UUID authorId) {
        return announcementRepository.findByAuthorIdAndIsDeletedFalse(authorId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public AnnouncementResponse createAnnouncement(UUID authorId, UUID institutionId, CreateAnnouncementRequest request) {
        Announcement announcement = Announcement.builder()
                .authorId(authorId)
                .title(request.getTitle())
                .content(request.getContent())
                .classGroupId(request.getClassGroupId())
                .subjectId(request.getSubjectId())
                .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                .build();
        announcement.setInstitutionId(institutionId);

        Announcement saved = announcementRepository.save(announcement);
        return mapToResponse(saved);
    }

    @Override
    public AnnouncementResponse updateAnnouncement(UUID authorId, UUID announcementId, CreateAnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .filter(a -> a.getAuthorId().equals(authorId) && !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", announcementId));

        if (request.getTitle() != null) announcement.setTitle(request.getTitle());
        if (request.getContent() != null) announcement.setContent(request.getContent());
        if (request.getClassGroupId() != null) announcement.setClassGroupId(request.getClassGroupId());
        if (request.getSubjectId() != null) announcement.setSubjectId(request.getSubjectId());
        if (request.getPriority() != null) announcement.setPriority(request.getPriority());

        Announcement saved = announcementRepository.save(announcement);
        return mapToResponse(saved);
    }

    @Override
    public void deleteAnnouncement(UUID authorId, UUID announcementId) {
        Announcement announcement = announcementRepository.findById(announcementId)
                .filter(a -> a.getAuthorId().equals(authorId) && !a.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", announcementId));

        announcement.setIsDeleted(true);
        announcementRepository.save(announcement);
    }

    private AnnouncementResponse mapToResponse(Announcement announcement) {
        String authorName = null;
        User author = userRepository.findById(announcement.getAuthorId()).orElse(null);
        if (author != null) {
            authorName = author.getFullName();
        }

        String subjectName = null;
        if (announcement.getSubjectId() != null) {
            subjectName = subjectRepository.findById(announcement.getSubjectId())
                    .map(Subject::getName).orElse(null);
        }

        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .classGroupId(announcement.getClassGroupId())
                .subjectId(announcement.getSubjectId())
                .subjectName(subjectName)
                .priority(announcement.getPriority())
                .authorName(authorName)
                .authorId(announcement.getAuthorId())
                .createdAt(announcement.getCreatedAt())
                .build();
    }
}
