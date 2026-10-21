package tz.elmkusoma.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * The names of the service types the platform currently sells.
     *
     * <p>Names and nothing else. This feeds an institution admin's bundle editor, which needs to
     * offer only services it can actually price - but the amounts, margins and policy versions
     * behind them are platform pricing policy, not something an institution admin reads. The
     * projection keeps that out of the response at the query level rather than relying on a
     * serializer to drop it.</p>
     *
     * <p>DISTINCT because several policy versions of one service type can be ACTIVE in the
     * historical sense; the editor wants each name once.</p>
     */
    @Query("SELECT DISTINCT p.serviceType FROM PricingPolicy p "
            + "WHERE p.status = :status AND p.isDeleted = false AND p.serviceType IS NOT NULL "
            + "ORDER BY p.serviceType")
    List<String> findDistinctServiceTypesByStatus(@Param("status") String status);
}
