package tz.elmkusoma.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tz.elmkusoma.administration.domain.AdminDelegation;
import tz.elmkusoma.administration.repository.AdminDelegationRepository;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CoreScheduler {

    private final EventPublisherService eventPublisherService;
    private final AdminDelegationRepository delegationRepository;

    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredTokens() {
        log.info("Token cleanup executed");
    }

    @Scheduled(fixedRate = 300000)
    public void expireDelegations() {
        List<AdminDelegation> expired = delegationRepository.findAll().stream()
                .filter(d -> "ACTIVE".equals(d.getStatus()))
                .filter(d -> d.getExpiresAt() != null && d.getExpiresAt().isBefore(LocalDateTime.now()))
                .toList();
        for (AdminDelegation del : expired) {
            del.setStatus("EXPIRED");
            del.setRevokedAt(LocalDateTime.now());
            del.setRevocationReason("Auto-expired: past expiration date");
            delegationRepository.save(del);
            log.info("Auto-expired delegation: {} -> {}", del.getDelegatorId(), del.getDelegateId());
        }
        if (!expired.isEmpty()) {
            log.info("Expired {} delegations that were past their expiration date", expired.size());
        }
    }

    @Scheduled(fixedRate = 60000)
    public void healthCheck() {
        log.debug("Core health check");
    }
}
