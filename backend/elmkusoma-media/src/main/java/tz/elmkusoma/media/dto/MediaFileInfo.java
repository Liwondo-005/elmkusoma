package tz.elmkusoma.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFileInfo {
    private String objectName;
    private String contentType;
    private long size;
    private Instant lastModified;
}
