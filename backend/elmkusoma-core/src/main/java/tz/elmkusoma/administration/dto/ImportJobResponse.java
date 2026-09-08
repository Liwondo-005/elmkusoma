package tz.elmkusoma.administration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportJobResponse {

    private UUID id;
    private UUID institutionId;
    private UUID importedBy;
    private String importType;
    private String fileName;
    private String status;
    private Integer totalRows;
    private Integer processedRows;
    private Integer successfulRows;
    private Integer failedRows;
    private Map<String, Object> errorLog;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}
