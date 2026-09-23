package tz.elmkusoma.administration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SupportTicketResponse {
    private UUID id;
    private UUID userId;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private UUID assignedTo;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
}
