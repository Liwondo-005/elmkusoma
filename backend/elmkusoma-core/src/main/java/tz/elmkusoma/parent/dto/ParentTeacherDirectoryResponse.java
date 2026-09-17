package tz.elmkusoma.parent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParentTeacherDirectoryResponse {

    private List<TeacherItem> teachers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeacherItem {
        private String id;
        private String userId;
        private String fullName;
        private String email;
        private String phone;
        private String subject;
        private String subjectId;
        private String specialization;
    }
}
