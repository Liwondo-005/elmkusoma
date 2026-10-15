package tz.elmkusoma.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.pricing.domain.PricingPolicy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PricingPolicyRepository extends JpaRepository<PricingPolicy, UUID> {

    List<PricingPolicy> findByStatusAndIsDeletedFalseOrderByPolicyVersionDesc(String status);

    Optional<PricingPolicy> findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
            String serviceType, String status);

    Optional<PricingPolicy> findFirstByEducationLevelAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
            String educationLevel, String status);

    boolean existsByServiceTypeAndStatusAndIsDeletedFalse(String serviceType, String status);

    boolean existsByEducationLevelAndStatusAndIsDeletedFalse(String educationLevel, String status);
}
