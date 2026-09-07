package tz.elmkusoma.certificate.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptResponse {

    private UUID id;
    private UUID institutionId;
    private UUID studentId;
    private UUID issuedBy;
    private String serialNumber;
    private String academicYear;
    private String term;
    private String status;
    private Integer totalSubjects;
    private BigDecimal averageScore;
    private Integer classRank;
    private String remarks;
    private LocalDateTime generatedAt;
    private LocalDateTime issuedAt;
    private List<TranscriptEntryResponse> entries;
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TranscriptEntryResponse {

        private UUID id;
        private String subjectName;
        private String subjectCode;
        private BigDecimal score;
        private String grade;
        private String remarks;
    }
}
