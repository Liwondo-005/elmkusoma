package tz.elmkusoma.administration.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IncidentCreateRequest {
    private String title;
    private String description;
    private String category;
    private String severity;
    private String affectedService;
}
