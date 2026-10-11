package tz.elmkusoma.administration.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.common.PageResponse;
import tz.elmkusoma.shared.dto.NewsArticleAdminView;
import tz.elmkusoma.shared.dto.NewsArticleUpsertRequest;
import tz.elmkusoma.shared.service.NewsArticleService;

import java.util.UUID;

/**
 * Platform Admin management of public news.
 *
 * <p>Gated by {@code hasRole('ADMIN')}, matching every other {@code /v1/platform-admin/**}
 * controller and the URL-level fence in SecurityConfig. No new role or permission string was
 * introduced.</p>
 *
 * <p>Lifecycle transitions are separate endpoints rather than a {@code status} field on the save
 * payload. That is what stops an ordinary edit from publishing an article by accident, and it is
 * why each of publish, unpublish and archive can be audited as its own action.</p>
 */
@RestController
@RequestMapping("/v1/platform-admin/news")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Platform Administration", description = "Public news and announcement management")
public class NewsArticleAdminController {

    private final NewsArticleService newsService;

    @GetMapping
    @Operation(summary = "List, search and filter news articles")
    public ResponseEntity<ApiResponse<PageResponse<NewsArticleAdminView>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) String q) {
        Page<NewsArticleAdminView> result = newsService.adminSearch(status, category, featured, q, page, size);
        return ResponseEntity.ok(ApiResponse.success(new PageResponse<>(
                result.getContent(), result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(),
                result.isFirst(), result.isLast())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "A single article including draft-only fields")
    public ResponseEntity<ApiResponse<NewsArticleAdminView>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(newsService.adminGet(id)));
    }

    @PostMapping
    @Operation(summary = "Create an article as a draft")
    public ResponseEntity<ApiResponse<NewsArticleAdminView>> create(
            @Valid @RequestBody NewsArticleUpsertRequest request,
            HttpServletRequest httpRequest) {
        NewsArticleAdminView created = newsService.create(request, actor(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Article saved as draft", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an article")
    public ResponseEntity<ApiResponse<NewsArticleAdminView>> update(
            @PathVariable UUID id,
            @Valid @RequestBody NewsArticleUpsertRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "Article updated", newsService.update(id, request, actor(httpRequest))));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish an article, stamping the publication time on the server")
    public ResponseEntity<ApiResponse<NewsArticleAdminView>> publish(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "Article published", newsService.publish(id, actor(httpRequest))));
    }

    @PostMapping("/{id}/unpublish")
    @Operation(summary = "Withdraw an article from the public site without discarding it")
    public ResponseEntity<ApiResponse<NewsArticleAdminView>> unpublish(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "Article unpublished", newsService.unpublish(id, actor(httpRequest))));
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "Archive an article out of the working set")
    public ResponseEntity<ApiResponse<NewsArticleAdminView>> archive(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                "Article archived", newsService.archive(id, actor(httpRequest))));
    }

    @PostMapping("/{id}/featured")
    @Operation(summary = "Mark or unmark an article as featured")
    public ResponseEntity<ApiResponse<NewsArticleAdminView>> setFeatured(
            @PathVariable UUID id,
            @Valid @RequestBody FeaturedRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success(
                request.isFeatured() ? "Article featured" : "Article unfeatured",
                newsService.setFeatured(id, request.isFeatured(), actor(httpRequest))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an article")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID id, HttpServletRequest httpRequest) {
        newsService.delete(id, actor(httpRequest));
        return ResponseEntity.ok(ApiResponse.success("Article deleted", null));
    }

    private static String actor(HttpServletRequest request) {
        Object email = request.getAttribute("userEmail");
        return email != null ? email.toString() : "admin";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeaturedRequest {

        @jakarta.validation.constraints.NotNull(message = "featured is required")
        private Boolean featured;

        public boolean isFeatured() {
            return Boolean.TRUE.equals(featured);
        }
    }
}