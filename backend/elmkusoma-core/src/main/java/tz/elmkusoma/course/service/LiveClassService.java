package tz.elmkusoma.course.service;

import tz.elmkusoma.course.dto.CreateLiveClassRequest;
import tz.elmkusoma.course.dto.LiveClassResponse;

import java.util.List;
import java.util.UUID;

public interface LiveClassService {

    List<LiveClassResponse> getTeacherLiveClasses(UUID teacherId);

    LiveClassResponse createLiveClass(UUID teacherId, UUID institutionId, CreateLiveClassRequest request);

    LiveClassResponse updateLiveClass(UUID teacherId, UUID liveClassId, CreateLiveClassRequest request);

    void cancelLiveClass(UUID teacherId, UUID liveClassId);

    LiveClassResponse startSession(UUID teacherId, UUID liveClassId);

    LiveClassResponse endSession(UUID teacherId, UUID liveClassId, UUID markedBy);

    LiveClassResponse getLiveClassById(UUID liveClassId);

    /**
     * Link an existing authorized Lesson to this teacher's live class
     * (Lesson ↔ Live Class). Validates lesson ownership/scope and prevents
     * duplicate links to the same lesson.
     */
    LiveClassResponse linkLesson(UUID teacherId, UUID liveClassId, UUID lessonId);

    /** Remove the Lesson association from this teacher's live class. */
    LiveClassResponse unlinkLesson(UUID teacherId, UUID liveClassId);

    List<LiveClassResponse> getUpcomingClasses(UUID institutionId);

    List<LiveClassResponse> getLiveClassesByStatus(UUID institutionId, String status);
}
