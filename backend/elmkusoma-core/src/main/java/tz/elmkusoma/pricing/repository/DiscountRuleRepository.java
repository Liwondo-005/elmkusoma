package tz.elmkusoma.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.pricing.domain.DiscountRule;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiscountRuleRepository extends JpaRepository<DiscountRule, UUID> {

    List<DiscountRule> findByStatusAndIsDeletedFalseOrderByPolicyVersionDesc(String status);

    Optional<DiscountRule> findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
            String serviceType, String status);

    boolean existsByServiceTypeAndStatusAndIsDeletedFalse(String serviceType, String status);

    List<DiscountRule> findByStatusAndServiceTypeAndIsDeletedFalseAndValidFromIsLessThanEqualAndValidUntilIsGreaterThan(
            String status, String serviceType, LocalDateTime now, LocalDateTime now2);
}
