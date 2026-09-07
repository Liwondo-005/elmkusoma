package tz.elmkusoma.certificate.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateTranscriptRequest {

    @NotNull(message = "Student ID is required")
    private UUID studentId;

    private String academicYear;

    private String term;

    private List<TranscriptEntryRequest> entries;

    private String remarks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TranscriptEntryRequest {

        @NotBlank(message = "Subject name is required")
        private String subjectName;

        private String subjectCode;

        private java.math.BigDecimal score;

        private String grade;

        private String remarks;
    }
}
