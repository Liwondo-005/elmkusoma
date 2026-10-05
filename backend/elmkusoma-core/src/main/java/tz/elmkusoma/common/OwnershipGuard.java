package tz.elmkusoma.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tz.elmkusoma.exception.ForbiddenException;

import java.util.UUID;

@Component
@Slf4j
public class OwnershipGuard {

    public void verifyInstitution(UUID resourceInstitutionId, UUID requestInstitutionId) {
        if (resourceInstitutionId == null || requestInstitutionId == null) {
            log.warn("Ownership check denied: null institution ID");
            throw new ForbiddenException("You do not have access to this resource");
        }
        if (!resourceInstitutionId.equals(requestInstitutionId)) {
            log.warn("Ownership violation: resource institution {} != request institution {}",
                    resourceInstitutionId, requestInstitutionId);
            throw new ForbiddenException("You do not have access to this resource");
        }
    }

    public void verifyUser(UUID resourceUserId, UUID requestUserId) {
        if (resourceUserId == null || requestUserId == null) {
            log.warn("Ownership check denied: null user ID");
            throw new ForbiddenException("You do not have access to this resource");
        }
        if (!resourceUserId.equals(requestUserId)) {
            log.warn("Ownership violation: resource user {} != request user {}",
                    resourceUserId, requestUserId);
            throw new ForbiddenException("You do not have access to this resource");
        }
    }

    public void verifyProvider(UUID resourceProviderId, UUID requestProviderId) {
        if (resourceProviderId == null || requestProviderId == null) {
            log.warn("Ownership check denied: null provider ID");
            throw new ForbiddenException("You do not have access to this provider resource");
        }
        if (!resourceProviderId.equals(requestProviderId)) {
            log.warn("Ownership violation: resource provider {} != request provider {}",
                    resourceProviderId, requestProviderId);
            throw new ForbiddenException("You do not have access to this provider resource");
        }
    }
}
