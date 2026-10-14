package tz.elmkusoma.pricing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.pricing.domain.CommissionPolicy;
import tz.elmkusoma.pricing.domain.DiscountRule;
import tz.elmkusoma.pricing.domain.PricingPolicy;
import tz.elmkusoma.pricing.repository.CommissionPolicyRepository;
import tz.elmkusoma.pricing.repository.DiscountRuleRepository;
import tz.elmkusoma.pricing.service.PricingPolicyAdminService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Platform Admin pricing governance.
 *
 * <p>Every route here is restricted to the Platform Admin - the existing highest-level platform
 * governance authority. There is no separate financial role, no hidden authority and no
 * backdoor: activating a price is the same ADMIN permission that governs the rest of the
 * platform, and each activation is audited with the acting user's id.</p>
 */
@RestController
@RequestMapping("/v1/platform-admin/pricing")
@RequiredArgsConstructor
@Tag(name = "Platform Admin - Pricing Governance")
@PreAuthorize("hasRole('ADMIN')")
public class PricingGovernanceController {

    private final PricingPolicyAdminService pricingPolicyAdminService;
    private final CommissionPolicyRepository commissionPolicyRepository;
    private final DiscountRuleRepository discountRuleRepository;

    @GetMapping("/policies")
    @Operation(summary = "List all pricing policies")
    public ResponseEntity<ApiResponse<List<PricingPolicy>>> listPolicies() {
        return ResponseEntity.ok(ApiResponse.success(pricingPolicyAdminService.listAll()));
    }

    @GetMapping("/policies/{status}")
    @Operation(summary = "List pricing policies by status (DRAFT, ACTIVE, RETIRED)")
    public ResponseEntity<ApiResponse<List<PricingPolicy>>> listByStatus(@PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.success(pricingPolicyAdminService.listByStatus(status)));
    }

    @PostMapping("/policies/{policyId}/activate")
    @Operation(summary = "Activate a pricing policy. Refuses a conflict rather than adjusting the price.")
    public ResponseEntity<ApiResponse<PricingPolicy>> activate(
            @PathVariable UUID policyId,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        UUID actor = currentUserId(request);
        String note = body == null ? null : body.get("note");
        return ResponseEntity.ok(ApiResponse.success(
                pricingPolicyAdminService.activate(policyId, actor, note)));
    }

    @PostMapping("/policies/{policyId}/retire")
    @Operation(summary = "Retire an active pricing policy")
    public ResponseEntity<ApiResponse<PricingPolicy>> retire(
            @PathVariable UUID policyId,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        UUID actor = currentUserId(request);
        String note = body == null ? null : body.get("note");
        return ResponseEntity.ok(ApiResponse.success(
                pricingPolicyAdminService.retire(policyId, actor, note)));
    }

    @GetMapping("/commission-policies")
    @Operation(summary = "List commission policies. A NULL rate means no decision yet, not zero.")
    public ResponseEntity<ApiResponse<List<CommissionPolicy>>> listCommissionPolicies() {
        return ResponseEntity.ok(ApiResponse.success(commissionPolicyRepository.findAll()));
    }

    @GetMapping("/discount-rules")
    @Operation(summary = "List discount rules")
    public ResponseEntity<ApiResponse<List<DiscountRule>>> listDiscountRules() {
        return ResponseEntity.ok(ApiResponse.success(discountRuleRepository.findAll()));
    }

    private UUID currentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId instanceof UUID uuid) {
            return uuid;
        }
        throw new org.springframework.security.access.AccessDeniedException(
                "An authorized Platform Admin action requires an authenticated user");
    }
}
