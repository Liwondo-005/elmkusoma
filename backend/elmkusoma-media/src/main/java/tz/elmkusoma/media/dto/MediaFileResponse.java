package tz.elmkusoma.media.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFileResponse {
    private Long id;
    private String fileName;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private String url;
    private String thumbnailUrl;
    private LocalDateTime uploadedAt;
}
