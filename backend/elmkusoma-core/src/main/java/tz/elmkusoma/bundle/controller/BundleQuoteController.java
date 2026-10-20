package tz.elmkusoma.bundle.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tz.elmkusoma.bundle.service.BundleTopUpService;
import tz.elmkusoma.common.ApiResponse;
import tz.elmkusoma.parent.service.ParentAuthorizationService;
import tz.elmkusoma.student.repository.StudentRepository;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * What this learner would actually pay for a bundle they partly already hold (BR-03).
 *
 * <p>This exists as its own endpoint rather than being folded into the catalog detail for one
 * reason: the honest price depends on who is asking and what they already hold. A catalog listing
 * that quoted a single figure would either overstate what an existing learner owes or understate
 * what a new one does, and the figure that reaches checkout must be the one the server computes
 * for this learner at this moment.
 *
 * <p>The quote is advisory for display. The charge is still settled at purchase time against the
 * authoritative pricing policy, so a policy change between quote and purchase is caught by the
 * backend rather than silently accepted.
 *
 * <p>Who may ask is not incidental to that. The quote is computed from the learner's existing
 * entitlements, which means it discloses what a learner already holds, and the checkout it feeds
 * is keyed on the learner it is computed for. The endpoint therefore only ever answers for the
 * caller's own child or for the caller's own learner record.
 */
@Slf4j
@RestController
@RequestMapping("/v1/bundles")
@RequiredArgsConstructor
@Tag(name = "Bundles")
@PreAuthorize("isAuthenticated()")
public class BundleQuoteController {

    private final BundleTopUpService bundleTopUpService;
    private final ParentAuthorizationService parentAuthorizationService;
    private final StudentRepository studentRepository;

    @GetMapping("/{bundleId}/quote")
    @Operation(summary = "Price a bundle for one learner, charging only for what they do not hold.")
    public ResponseEntity<ApiResponse<BundleTopUpService.TopUpQuote>> quote(
            @PathVariable UUID bundleId,
            @RequestParam UUID studentId,
            jakarta.servlet.http.HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("User not authenticated");
        }

        // The learner in the query must be the caller's own child, or the caller's own learner
        // record. This endpoint used to price any learner id any authenticated user cared to
        // name, so the price it returned also disclosed which services that learner already held,
        // and the checkout fed from it ran on the same unowned id. Purchase itself was always
        // protected at the parent endpoint; this was the hole beside it.
        //
        // The learner's own record is checked first, and through a lookup that answers false
        // rather than throwing when the caller has no parent profile - otherwise every learner who
        // tried to price themselves would be turned away by the parent check before their own
        // branch was ever reached.
        boolean owns = studentRepository.findByUserIdAndIsDeletedFalse(userId)
                .map(s -> s.getId().equals(studentId)).orElse(false)
                || parentAuthorizationService.isLinkedParentOf(userId, studentId);
        if (!owns) {
            log.warn("IDOR blocked: user={} attempted to quote bundle={} for unauthorized learner={}",
                    userId, bundleId, studentId);
            throw new tz.elmkusoma.exception.ResourceNotFoundException(
                    "Student link", "studentId", studentId);
        }

        // Always priced against the moment of the request. There is deliberately no way to name
        // another time: an override that let a caller pick the instant would let them be quoted
        // against entitlements that already lapsed or have not started, and no test needs it -
        // the pricing tests drive the service directly, where the clock is an ordinary argument.
        return ResponseEntity.ok(ApiResponse.success(
                bundleTopUpService.quote(bundleId, studentId, LocalDateTime.now())));
    }
}
