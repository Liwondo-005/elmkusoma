package tz.elmkusoma.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.pricing.domain.CommissionPolicy;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommissionPolicyRepository extends JpaRepository<CommissionPolicy, UUID> {

    List<CommissionPolicy> findByStatusAndIsDeletedFalseOrderByPolicyVersionDesc(String status);

    Optional<CommissionPolicy> findFirstByServiceTypeAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
            String serviceType, String status);

    Optional<CommissionPolicy> findFirstByProviderIdAndStatusAndIsDeletedFalseOrderByPolicyVersionDesc(
            UUID providerId, String status);

    boolean existsByServiceTypeAndStatusAndIsDeletedFalse(String serviceType, String status);
}
