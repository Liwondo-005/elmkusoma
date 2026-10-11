package tz.elmkusoma.shared.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.shared.dto.NewsArticleView;
import tz.elmkusoma.shared.service.NewsArticleService;

import java.util.List;

/**
 * Public news reading. Anonymous, like the rest of {@code /v1/public/**}.
 *
 * <p>There is deliberately no "include drafts" parameter and no per-article status filter here.
 * Public eligibility is decided by the repository query - published, due and unexpired - so there
 * is no input a caller could supply to widen it.</p>
 */
@RestController
@RequestMapping("/v1/public/news")
@RequiredArgsConstructor
@Tag(name = "Public Site", description = "Published news and announcements")
public class PublicNewsController {

    private final NewsArticleService newsService;

    @GetMapping
    @Operation(summary = "Published news, paginated, in featured/priority/recency order")
    public ResponseEntity<ApiResponse<PageResponse<NewsArticleView>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        Page<NewsArticleView> result = newsService.publicPage(page, size);
        return ResponseEntity.ok(ApiResponse.success(new PageResponse<>(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast())));
    }

    @GetMapping("/latest")
    @Operation(summary = "Most recent published news for the landing page")
    public ResponseEntity<ApiResponse<List<NewsArticleView>>> latest(
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(ApiResponse.success(newsService.publicLatest(limit)));
    }

    @GetMapping("/{slug}")
    @Operation(summary = "A single published article by slug")
    public ResponseEntity<ApiResponse<NewsArticleView>> bySlug(@PathVariable String slug) {
        // An unpublished, archived, not-yet-due or expired slug raises ResourceNotFoundException,
        // which the global handler renders as 404. Deliberately not 403: telling an anonymous
        // caller that a draft exists is itself a disclosure.
        return ResponseEntity.ok(ApiResponse.success(newsService.publicBySlug(slug)));
    }
}