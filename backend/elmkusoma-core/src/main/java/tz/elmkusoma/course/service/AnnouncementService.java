package tz.elmkusoma.course.service;

import tz.elmkusoma.course.dto.AnnouncementResponse;
import tz.elmkusoma.course.dto.CreateAnnouncementRequest;

import java.util.List;
import java.util.UUID;

public interface AnnouncementService {

    List<AnnouncementResponse> getTeacherAnnouncements(UUID authorId);

    AnnouncementResponse createAnnouncement(UUID authorId, UUID institutionId, CreateAnnouncementRequest request);

    AnnouncementResponse updateAnnouncement(UUID authorId, UUID announcementId, CreateAnnouncementRequest request);

    void deleteAnnouncement(UUID authorId, UUID announcementId);
}
