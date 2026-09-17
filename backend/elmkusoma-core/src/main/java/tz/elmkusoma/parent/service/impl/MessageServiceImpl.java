package tz.elmkusoma.parent.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.parent.domain.Message;
import tz.elmkusoma.parent.domain.Parent;
import tz.elmkusoma.parent.domain.ParentNotificationPreference;
import tz.elmkusoma.parent.dto.request.SendMessageRequest;
import tz.elmkusoma.parent.dto.request.ParentNotificationPreferenceRequest;
import tz.elmkusoma.parent.dto.response.MessageResponse;
import tz.elmkusoma.parent.dto.response.ParentNotificationPreferenceResponse;
import tz.elmkusoma.parent.repository.MessageRepository;
import tz.elmkusoma.parent.repository.ParentNotificationPreferenceRepository;
import tz.elmkusoma.parent.repository.ParentRepository;
import tz.elmkusoma.parent.service.MessageService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ParentRepository parentRepository;
    private final UserRepository userRepository;
    private final ParentNotificationPreferenceRepository notificationPreferenceRepository;

    private Parent resolveParentStrict(UUID userId) {
        List<Parent> allParents = parentRepository.findAllByUserId(userId);
        if (allParents.isEmpty()) {
            throw new ResourceNotFoundException("Parent profile", "userId", userId);
        }
        return allParents.get(0);
    }

    private String resolveUserName(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null ? user.getFullName() : "Unknown";
    }

    private MessageResponse mapToResponse(Message msg) {
        return MessageResponse.builder()
                .id(msg.getId().toString())
                .senderId(msg.getSenderId().toString())
                .senderName(resolveUserName(msg.getSenderId()))
                .recipientId(msg.getRecipientId().toString())
                .recipientName(resolveUserName(msg.getRecipientId()))
                .subject(msg.getSubject())
                .body(msg.getBody())
                .status(msg.getStatus())
                .isRead(msg.getIsRead())
                .messageType(msg.getMessageType())
                .createdAt(msg.getCreatedAt())
                .build();
    }

    @Override
    public List<MessageResponse> getInboxMessages(UUID userId) {
        Parent parent = resolveParentStrict(userId);
        return messageRepository.findByRecipientIdAndIsDeletedFalseOrderByCreatedAtDesc(parent.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<MessageResponse> getSentMessages(UUID userId) {
        Parent parent = resolveParentStrict(userId);
        return messageRepository.findBySenderIdAndIsDeletedFalseOrderByCreatedAtDesc(parent.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public MessageResponse getMessageById(UUID userId, UUID messageId) {
        Parent parent = resolveParentStrict(userId);
        Message msg = messageRepository.findById(messageId)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .filter(m -> m.getSenderId().equals(parent.getId()) || m.getRecipientId().equals(parent.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));
        return mapToResponse(msg);
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(UUID userId, SendMessageRequest request) {
        Parent parent = resolveParentStrict(userId);

        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new ResourceNotFoundException("Recipient", "id", request.getRecipientId()));

        Message msg = Message.builder()
                .senderId(parent.getId())
                .recipientId(recipient.getId())
                .subject(request.getSubject())
                .body(request.getBody())
                .status("SENT")
                .isRead(false)
                .messageType(request.getMessageType() != null ? request.getMessageType() : "DIRECT")
                .build();

        msg = messageRepository.save(msg);
        return mapToResponse(msg);
    }

    @Override
    @Transactional
    public void markAsRead(UUID userId, UUID messageId) {
        Parent parent = resolveParentStrict(userId);
        Message msg = messageRepository.findById(messageId)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .filter(m -> m.getRecipientId().equals(parent.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        msg.setIsRead(true);
        msg.setStatus("READ");
        messageRepository.save(msg);
    }

    @Override
    @Transactional
    public void deleteMessage(UUID userId, UUID messageId) {
        Parent parent = resolveParentStrict(userId);
        Message msg = messageRepository.findById(messageId)
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .filter(m -> m.getSenderId().equals(parent.getId()) || m.getRecipientId().equals(parent.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));

        msg.setIsDeleted(true);
        messageRepository.save(msg);
    }

    @Override
    public long getUnreadCount(UUID userId) {
        Parent parent = resolveParentStrict(userId);
        return messageRepository.countByRecipientIdAndIsDeletedFalseAndIsReadFalse(parent.getId());
    }

    @Override
    public ParentNotificationPreferenceResponse getNotificationPreferences(UUID userId) {
        Parent parent = resolveParentStrict(userId);
        ParentNotificationPreference prefs = notificationPreferenceRepository.findByParentId(parent.getId())
                .orElseGet(() -> {
                    ParentNotificationPreference newPrefs = ParentNotificationPreference.builder()
                            .parentId(parent.getId())
                            .attendanceAlerts(true)
                            .gradeAlerts(true)
                            .feeAlerts(true)
                            .generalAnnouncements(true)
                            .smsEnabled(false)
                            .emailEnabled(true)
                            .pushEnabled(true)
                            .build();
                    return notificationPreferenceRepository.save(newPrefs);
                });

        return ParentNotificationPreferenceResponse.builder()
                .id(prefs.getId())
                .parentId(prefs.getParentId())
                .attendanceAlerts(prefs.getAttendanceAlerts())
                .gradeAlerts(prefs.getGradeAlerts())
                .feeAlerts(prefs.getFeeAlerts())
                .generalAnnouncements(prefs.getGeneralAnnouncements())
                .smsEnabled(prefs.getSmsEnabled())
                .emailEnabled(prefs.getEmailEnabled())
                .pushEnabled(prefs.getPushEnabled())
                .build();
    }

    @Override
    @Transactional
    public ParentNotificationPreferenceResponse updateNotificationPreferences(UUID userId, ParentNotificationPreferenceRequest request) {
        Parent parent = resolveParentStrict(userId);
        ParentNotificationPreference prefs = notificationPreferenceRepository.findByParentId(parent.getId())
                .orElseGet(() -> ParentNotificationPreference.builder()
                        .parentId(parent.getId())
                        .build());

        if (request.getAttendanceAlerts() != null) prefs.setAttendanceAlerts(request.getAttendanceAlerts());
        if (request.getGradeAlerts() != null) prefs.setGradeAlerts(request.getGradeAlerts());
        if (request.getFeeAlerts() != null) prefs.setFeeAlerts(request.getFeeAlerts());
        if (request.getGeneralAnnouncements() != null) prefs.setGeneralAnnouncements(request.getGeneralAnnouncements());
        if (request.getSmsEnabled() != null) prefs.setSmsEnabled(request.getSmsEnabled());
        if (request.getEmailEnabled() != null) prefs.setEmailEnabled(request.getEmailEnabled());
        if (request.getPushEnabled() != null) prefs.setPushEnabled(request.getPushEnabled());

        prefs = notificationPreferenceRepository.save(prefs);

        return ParentNotificationPreferenceResponse.builder()
                .id(prefs.getId())
                .parentId(prefs.getParentId())
                .attendanceAlerts(prefs.getAttendanceAlerts())
                .gradeAlerts(prefs.getGradeAlerts())
                .feeAlerts(prefs.getFeeAlerts())
                .generalAnnouncements(prefs.getGeneralAnnouncements())
                .smsEnabled(prefs.getSmsEnabled())
                .emailEnabled(prefs.getEmailEnabled())
                .pushEnabled(prefs.getPushEnabled())
                .build();
    }
}
