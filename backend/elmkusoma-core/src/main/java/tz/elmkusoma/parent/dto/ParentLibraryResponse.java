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
public class ParentLibraryResponse {

    private List<LibraryCategory> categories;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LibraryCategory {
        private String name;
        private String description;
        private List<LibraryItem> items;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LibraryItem {
        private String id;
        private String title;
        private String description;
        private String resourceType;
        private String fileUrl;
        private String subject;
        private String targetGroup;
    }
}
