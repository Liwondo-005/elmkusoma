package tz.elmkusoma.parent.service;

import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.parent.dto.request.LinkStudentRequest;
import tz.elmkusoma.parent.dto.request.ParentNotificationPreferenceRequest;
import tz.elmkusoma.parent.dto.request.ParentRequest;
import tz.elmkusoma.parent.dto.response.ParentNotificationPreferenceResponse;
import tz.elmkusoma.parent.dto.response.ParentResponse;
import tz.elmkusoma.parent.dto.response.ParentStudentResponse;

import java.util.List;
import java.util.UUID;

public interface ParentService {

    ParentResponse createParent(UUID institutionId, ParentRequest request);

    ParentResponse getParent(UUID institutionId, UUID parentId);

    PageResponse<ParentResponse> listParents(UUID institutionId, int page, int size);

    ParentResponse updateParent(UUID institutionId, UUID parentId, ParentRequest request);

    void deleteParent(UUID institutionId, UUID parentId);

    ParentStudentResponse linkStudent(UUID institutionId, UUID parentId, LinkStudentRequest request);

    List<ParentStudentResponse> getChildren(UUID institutionId, UUID parentId);

    void unlinkStudent(UUID institutionId, UUID linkId);

    ParentNotificationPreferenceResponse getNotificationPreferences(UUID institutionId, UUID parentId);

    ParentNotificationPreferenceResponse updateNotificationPreferences(UUID institutionId, UUID parentId, ParentNotificationPreferenceRequest request);
}
