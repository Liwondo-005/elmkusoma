package tz.elmkusoma.learner.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.learner.domain.LearnerNotification;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.shared.domain.InstitutionMembership;
import tz.elmkusoma.shared.repository.InstitutionMembershipRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final LearnerNotificationRepository notificationRepository;
    private final InstitutionMembershipRepository membershipRepository;

    @Transactional
    public void notifyUser(UUID userId, String title, String message,
                           String notificationType, String targetType, UUID targetId) {
        LearnerNotification notification = LearnerNotification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .notificationType(notificationType)
                .targetType(targetType)
                .targetId(targetId)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyInstitutionStudentsExcluding(UUID institutionId, UUID excludeUserId,
                                                   String title, String message,
                                                   String notificationType, String targetType, UUID targetId) {
        List<InstitutionMembership> members = membershipRepository
                .findByInstitutionIdAndIsActiveTrue(institutionId);

        List<LearnerNotification> notifications = new ArrayList<>();

        for (InstitutionMembership member : members) {
            if (!member.getUserId().equals(excludeUserId)
                    && member.getRole() == InstitutionMembership.Role.STUDENT) {
                LearnerNotification notification = LearnerNotification.builder()
                        .userId(member.getUserId())
                        .title(title)
                        .message(message)
                        .notificationType(notificationType)
                        .targetType(targetType)
                        .targetId(targetId)
                        .build();
                notifications.add(notification);
            }
        }

        if (!notifications.isEmpty()) {
            notificationRepository.saveAll(notifications);
        }

        log.info("Sent {} notifications to {}/{} members of institution {}",
                notificationType, notifications.size(), members.size(), institutionId);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<LearnerNotification> unread = notificationRepository
                .findByUserIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        for (LearnerNotification n : unread) {
            n.setIsRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalseAndIsDeletedFalse(userId);
    }

    public List<LearnerNotification> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
    }
}
