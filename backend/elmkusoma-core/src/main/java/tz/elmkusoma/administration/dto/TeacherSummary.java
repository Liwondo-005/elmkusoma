package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSummary {
    private UUID id;
    private String fullName;
    private String email;
    private UUID institutionId;
    private String institutionName;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
