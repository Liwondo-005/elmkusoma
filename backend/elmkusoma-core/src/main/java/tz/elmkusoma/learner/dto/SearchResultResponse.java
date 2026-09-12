package tz.elmkusoma.learner.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultResponse {

    private List<CourseSummaryResponse> courses;
    private List<ResourceSearchResult> resources;
    private List<LiveClassSearchResult> liveClasses;
}
