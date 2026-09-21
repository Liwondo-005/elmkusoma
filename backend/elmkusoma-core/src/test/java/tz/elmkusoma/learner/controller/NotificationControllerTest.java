package tz.elmkusoma.learner.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.learner.domain.LearnerNotification;
import tz.elmkusoma.learner.service.NotificationService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    void getNotifications_returnsList() {
        UUID userId = UUID.randomUUID();
        when(notificationService.getNotifications(userId)).thenReturn(List.of(
                new LearnerNotification()
        ));

        ResponseEntity<ApiResponse<List<LearnerNotification>>> result =
                notificationController.getNotifications(userId);

        assertEquals(200, result.getStatusCode().value());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getData().size());
        verify(notificationService).getNotifications(userId);
    }

    @Test
    void getUnreadCount_returnsCount() {
        UUID userId = UUID.randomUUID();
        when(notificationService.getUnreadCount(userId)).thenReturn(5L);

        ResponseEntity<ApiResponse<Map<String, Long>>> result =
                notificationController.getUnreadCount(userId);

        assertEquals(200, result.getStatusCode().value());
        assertEquals(5L, result.getBody().getData().get("count"));
    }

    @Test
    void markAsRead_callsService() {
        UUID notificationId = UUID.randomUUID();

        ResponseEntity<ApiResponse<Void>> result =
                notificationController.markAsRead(notificationId);

        assertEquals(200, result.getStatusCode().value());
        verify(notificationService).markAsRead(notificationId);
    }

    @Test
    void markAllAsRead_callsService() {
        UUID userId = UUID.randomUUID();

        ResponseEntity<ApiResponse<Void>> result =
                notificationController.markAllAsRead(userId);

        assertEquals(200, result.getStatusCode().value());
        verify(notificationService).markAllAsRead(userId);
    }
}
