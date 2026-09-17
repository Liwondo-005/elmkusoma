package tz.elmkusoma.oversight.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObserverJoinResponse {
    private String websocketUrl;
    private String token;
    private String liveClassId;
    private String title;
    private String institutionName;
    private String subjectName;
    private String teacherName;
}