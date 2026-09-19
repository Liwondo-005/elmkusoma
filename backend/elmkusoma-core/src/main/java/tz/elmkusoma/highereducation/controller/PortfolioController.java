package tz.elmkusoma.highereducation.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.highereducation.domain.PortfolioItemType;
import tz.elmkusoma.highereducation.dto.PortfolioDTO;
import tz.elmkusoma.highereducation.dto.PortfolioItemDTO;
import tz.elmkusoma.highereducation.service.PortfolioService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<PortfolioDTO>>> listPortfolios(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<PortfolioDTO> portfolios = portfolioService.getPortfolios(institutionId);
        return ResponseEntity.ok(ApiResponse.success(portfolios));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<PortfolioDTO>> getPortfolio(@PathVariable UUID id) {
        PortfolioDTO portfolio = portfolioService.getPortfolio(id);
        return ResponseEntity.ok(ApiResponse.success(portfolio));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<PortfolioDTO>> getStudentPortfolio(@PathVariable UUID studentId) {
        PortfolioDTO portfolio = portfolioService.getStudentPortfolio(studentId);
        return ResponseEntity.ok(ApiResponse.success(portfolio));
    }

    @GetMapping("/student/{studentId}/public")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<PortfolioDTO>> getPublicPortfolio(@PathVariable UUID studentId) {
        PortfolioDTO portfolio = portfolioService.getPublicPortfolio(studentId);
        return ResponseEntity.ok(ApiResponse.success(portfolio));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<PortfolioDTO>> createPortfolio(
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody PortfolioDTO dto) {
        dto.setInstitutionId(institutionId);
        PortfolioDTO created = portfolioService.createPortfolio(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Portfolio created", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<PortfolioDTO>> updatePortfolio(
            @PathVariable UUID id,
            @Valid @RequestBody PortfolioDTO dto) {
        PortfolioDTO updated = portfolioService.updatePortfolio(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Portfolio updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(@PathVariable UUID id) {
        portfolioService.deletePortfolio(id);
        return ResponseEntity.ok(ApiResponse.success("Portfolio deleted", null));
    }

    // ── Portfolio Items ──────────────────────────────────────────

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<PortfolioItemDTO>> addItem(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody PortfolioItemDTO dto) {
        dto.setPortfolioId(id);
        dto.setInstitutionId(institutionId);
        PortfolioItemDTO created = portfolioService.addItem(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Portfolio item added", created));
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<PortfolioItemDTO>>> getItems(
            @PathVariable UUID id,
            @RequestParam(required = false) PortfolioItemType itemType) {
        List<PortfolioItemDTO> items = itemType != null
                ? portfolioService.getItemsByType(id, itemType)
                : portfolioService.getItems(id);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<PortfolioItemDTO>> updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody PortfolioItemDTO dto) {
        PortfolioItemDTO updated = portfolioService.updateItem(itemId, dto);
        return ResponseEntity.ok(ApiResponse.success("Portfolio item updated", updated));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable UUID itemId) {
        portfolioService.deleteItem(itemId);
        return ResponseEntity.ok(ApiResponse.success("Portfolio item deleted", null));
    }
}
