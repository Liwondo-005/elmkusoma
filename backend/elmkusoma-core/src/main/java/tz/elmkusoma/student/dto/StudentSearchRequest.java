package tz.elmkusoma.student.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentSearchRequest {

    private UUID institutionId;

    private UUID classId;

    private String query;

    private String status;
}
