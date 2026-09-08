package tz.elmkusoma.parent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParentStudentResponse {

    private UUID id;
    private UUID parentId;
    private UUID studentId;
    private String studentName;
    private String admissionNumber;
    private String relationshipType;
    private Boolean isPrimary;
    private LocalDateTime createdAt;
}
