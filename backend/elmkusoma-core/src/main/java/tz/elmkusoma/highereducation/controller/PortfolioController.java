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
import tz.elmkusoma.highereducation.service.HighEdIdentity;
import tz.elmkusoma.highereducation.service.PortfolioService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/education/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final tz.elmkusoma.highereducation.repository.PortfolioRepository portfolioRepository;
    private final tz.elmkusoma.highereducation.repository.PortfolioItemRepository portfolioItemRepository;
    private final HighEdIdentity highEdIdentity;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<PortfolioDTO>>> listPortfolios(
            @RequestHeader("X-Institution-Id") UUID institutionId) {
        List<PortfolioDTO> portfolios = portfolioService.getPortfolios(institutionId);
        return ResponseEntity.ok(ApiResponse.success(portfolios));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<PortfolioDTO>> getPortfolio(
            @PathVariable UUID id,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        PortfolioDTO portfolio = portfolioService.getPortfolio(id);
        assertPortfolioReadable(portfolio, callerUserId, userRole, serverInstitutionId, id);
        return ResponseEntity.ok(ApiResponse.success(portfolio));
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'OTHER_LEARNER', 'ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<PortfolioDTO>> getStudentPortfolio(
            @PathVariable UUID studentId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        PortfolioDTO portfolio = portfolioService.getStudentPortfolio(learnerId);
        return ResponseEntity.ok(ApiResponse.success(portfolio));
    }

    @GetMapping("/student/{studentId}/public")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<PortfolioDTO>> getPublicPortfolio(
            @PathVariable UUID studentId,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute("userRole") String userRole,
            @RequestAttribute("institutionId") UUID serverInstitutionId) {
        UUID learnerId = highEdIdentity.resolveStudentId(callerUserId, userRole, studentId);
        highEdIdentity.assertStudentInInstitution(learnerId, serverInstitutionId);
        PortfolioDTO portfolio = portfolioService.getPublicPortfolio(learnerId);
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
            @Valid @RequestBody PortfolioDTO dto,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        verifyPortfolioAccess(id, serverInstitutionId, userRole);
        PortfolioDTO updated = portfolioService.updatePortfolio(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Portfolio updated", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deletePortfolio(
            @PathVariable UUID id,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        verifyPortfolioAccess(id, serverInstitutionId, userRole);
        portfolioService.deletePortfolio(id);
        return ResponseEntity.ok(ApiResponse.success("Portfolio deleted", null));
    }

    // ── Portfolio Items ──────────────────────────────────────────

    @PostMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<PortfolioItemDTO>> addItem(
            @PathVariable UUID id,
            @RequestHeader("X-Institution-Id") UUID institutionId,
            @Valid @RequestBody PortfolioItemDTO dto,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        verifyPortfolioAccess(id, serverInstitutionId, userRole);
        dto.setPortfolioId(id);
        dto.setInstitutionId(serverInstitutionId != null ? serverInstitutionId : institutionId);
        PortfolioItemDTO created = portfolioService.addItem(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Portfolio item added", created));
    }

    @GetMapping("/{id}/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN', 'TEACHER', 'INSTRUCTOR', 'STUDENT', 'OTHER_LEARNER')")
    public ResponseEntity<ApiResponse<List<PortfolioItemDTO>>> getItems(
            @PathVariable UUID id,
            @RequestParam(required = false) PortfolioItemType itemType,
            @RequestAttribute("userId") UUID callerUserId,
            @RequestAttribute(value = "userRole", required = false) String userRole,
            @RequestAttribute(value = "institutionId", required = false) UUID serverInstitutionId) {
        assertPortfolioReadable(portfolioService.getPortfolio(id), callerUserId, userRole, serverInstitutionId, id);
        List<PortfolioItemDTO> items = itemType != null
                ? portfolioService.getItemsByType(id, itemType)
                : portfolioService.getItems(id);
        return ResponseEntity.ok(ApiResponse.success(items));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<PortfolioItemDTO>> updateItem(
            @PathVariable UUID itemId,
            @Valid @RequestBody PortfolioItemDTO dto,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        verifyPortfolioItemAccess(itemId, serverInstitutionId, userRole);
        PortfolioItemDTO updated = portfolioService.updateItem(itemId, dto);
        return ResponseEntity.ok(ApiResponse.success("Portfolio item updated", updated));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTITUTION_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteItem(
            @PathVariable UUID itemId,
            @RequestAttribute("institutionId") UUID serverInstitutionId,
            @RequestAttribute("userRole") String userRole) {
        verifyPortfolioItemAccess(itemId, serverInstitutionId, userRole);
        portfolioService.deleteItem(itemId);
        return ResponseEntity.ok(ApiResponse.success("Portfolio item deleted", null));
    }

    private void assertPortfolioReadable(PortfolioDTO portfolio, UUID callerUserId, String userRole,
                                         UUID serverInstitutionId, UUID id) {
        if (HighEdIdentity.isLearner(userRole)) {
            if (portfolio.getStudentId() != null) {
                try {
                    highEdIdentity.resolveStudentId(callerUserId, userRole, portfolio.getStudentId());
                } catch (tz.elmkusoma.exception.ForbiddenException e) {
                    throw new tz.elmkusoma.exception.ResourceNotFoundException("Portfolio", "id", id);
                }
            } else if (serverInstitutionId == null || !serverInstitutionId.equals(portfolio.getInstitutionId())) {
                throw new tz.elmkusoma.exception.ResourceNotFoundException("Portfolio", "id", id);
            }
            return;
        }
        verifyPortfolioAccess(id, serverInstitutionId, userRole);
    }

    private void verifyPortfolioAccess(UUID id, UUID serverInstitutionId, String userRole) {
        if ("ADMIN".equals(userRole)) {
            return;
        }
        portfolioRepository.findById(id)
                .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                .ifPresent(p -> {
                    if (p.getInstitutionId() == null || !p.getInstitutionId().equals(serverInstitutionId)) {
                        throw new tz.elmkusoma.exception.ForbiddenException("Portfolio", "access");
                    }
                });
    }

    private void verifyPortfolioItemAccess(UUID itemId, UUID serverInstitutionId, String userRole) {
        if ("ADMIN".equals(userRole)) {
            return;
        }
        portfolioItemRepository.findById(itemId)
                .filter(i -> !Boolean.TRUE.equals(i.getIsDeleted()))
                .ifPresent(i -> {
                    if (i.getInstitutionId() == null || !i.getInstitutionId().equals(serverInstitutionId)) {
                        throw new tz.elmkusoma.exception.ForbiddenException("Portfolio item", "access");
                    }
                });
    }
}
