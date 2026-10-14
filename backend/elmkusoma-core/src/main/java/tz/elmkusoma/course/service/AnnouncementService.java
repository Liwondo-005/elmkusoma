package tz.elmkusoma.course.service;

import tz.elmkusoma.course.dto.AnnouncementResponse;
import tz.elmkusoma.course.dto.CreateAnnouncementRequest;

import java.util.List;
import java.util.UUID;

public interface AnnouncementService {

    List<AnnouncementResponse> getTeacherAnnouncements(UUID authorId);

    /**
     * @param userRole the authenticated role; drives the teaching-assignment half of the
     *                 audience check, since naming a class in your own institution is not
     *                 the same as being entitled to address it
     */
    AnnouncementResponse createAnnouncement(UUID authorId, UUID institutionId, String userRole,
                                            CreateAnnouncementRequest request);

    AnnouncementResponse updateAnnouncement(UUID authorId, UUID announcementId, String userRole,
                                            CreateAnnouncementRequest request);

    void deleteAnnouncement(UUID authorId, UUID announcementId);
}
