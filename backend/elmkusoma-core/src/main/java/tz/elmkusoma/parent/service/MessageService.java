package tz.elmkusoma.parent.service;

import tz.elmkusoma.parent.dto.request.SendMessageRequest;
import tz.elmkusoma.parent.dto.request.ParentNotificationPreferenceRequest;
import tz.elmkusoma.parent.dto.response.MessageResponse;
import tz.elmkusoma.parent.dto.response.ParentNotificationPreferenceResponse;

import java.util.List;
import java.util.UUID;

public interface MessageService {

    List<MessageResponse> getInboxMessages(UUID userId);

    List<MessageResponse> getSentMessages(UUID userId);

    MessageResponse getMessageById(UUID userId, UUID messageId);

    MessageResponse sendMessage(UUID userId, SendMessageRequest request);

    void markAsRead(UUID userId, UUID messageId);

    void deleteMessage(UUID userId, UUID messageId);

    long getUnreadCount(UUID userId);

    ParentNotificationPreferenceResponse getNotificationPreferences(UUID userId);

    ParentNotificationPreferenceResponse updateNotificationPreferences(UUID userId, ParentNotificationPreferenceRequest request);
}
