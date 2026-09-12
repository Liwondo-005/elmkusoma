package tz.elmkusoma.learner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceSearchResult {

    private UUID id;
    private String title;
    private String description;
    private String resourceType;
    private String fileUrl;
}
