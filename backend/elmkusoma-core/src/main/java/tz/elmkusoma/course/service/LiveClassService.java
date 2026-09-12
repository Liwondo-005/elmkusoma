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
}
