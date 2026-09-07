package tz.elmkusoma.learning.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {

    private UUID id;
    private UUID subjectId;
    private UUID classGroupId;
    private String title;
    private String description;
    private String fileUrl;
    private String resourceType;
}
