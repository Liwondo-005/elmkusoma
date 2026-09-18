package tz.elmkusoma.oversight.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CurriculumResponse {
    private Double overallProgress;
    private Long totalLessons;
    private Long completedLessons;
    private Long schoolsOnTrack;
    private Long schoolsBehind;
    private List<SubjectProgress> subjectProgress;
    private List<SchoolCurriculumProgress> schoolProgress;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubjectProgress {
        private String subjectName;
        private String subjectCode;
        private Long totalLessons;
        private Long completedLessons;
        private Double progressRate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SchoolCurriculumProgress {
        private String institutionId;
        private String institutionName;
        private String institutionCode;
        private Long totalLessons;
        private Long completedLessons;
        private Double progressRate;
        private String status;
    }
}
