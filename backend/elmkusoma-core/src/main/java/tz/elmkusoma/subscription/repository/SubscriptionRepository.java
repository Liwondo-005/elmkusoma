package tz.elmkusoma.subscription.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.subscription.domain.Subscription;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /**
     * Idempotency key for activation. A duplicated confirmation - a retried
     * webhook, a double-clicked poll - resolves to the subscription that already
     * exists instead of opening a second one.
     */
    Optional<Subscription> findByPaymentIdAndIsDeletedFalse(UUID paymentId);

    List<Subscription> findAllByStudentIdAndIsDeletedFalseOrderByPeriodStartDesc(UUID studentId);

    List<Subscription> findAllByPayerIdAndIsDeletedFalseOrderByPeriodStartDesc(UUID payerId);

    /**
     * Subscriptions still marked active whose paid period has elapsed. Driven by
     * a sweep for reporting; access itself is evaluated on read, so a delayed or
     * missed sweep cannot grant or deny anything.
     */
    List<Subscription> findAllByStatusAndPeriodEndLessThanEqualAndIsDeletedFalse(
            String status, LocalDateTime now);

    /**
     * Row-locked read, so two concurrent confirmations of the same purchase
     * serialise here rather than both deciding nothing exists yet.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") UUID id);

    /**
     * The payer's still-active periods for this service, row-locked, latest first.
     *
     * <p>Returns a list rather than a single row because a chained renewal
     * legitimately leaves several ACTIVE rows in a row: the period that ends today,
     * the one that starts today, and so on. They are adjacent, never overlapping.
     * Ordering by {@code periodEnd} descending identifies which one a new renewal
     * must follow.</p>
     *
     * <p>{@code serviceId} is compared through COALESCE so that a service-level
     * purchase (null serviceId) matches the same key the database exclusion
     * constraint uses.</p>
     *
     * <p>The lock is what makes concurrent renewals safe. Two callbacks arriving
     * together both wait here, and the second one then sees the row the first one
     * inserted, so the two cannot both decide to start from the same end date and
     * create a spurious overlap. When the list comes back empty - a genuine first
     * purchase - there is no row to lock, and correctness there rests on the
     * exclusion constraint rejecting a racing insert rather than on this lock.</p>
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subscription s WHERE s.payerId = :payerId "
            + "AND s.serviceType = :serviceType "
            + "AND COALESCE(s.serviceId, :nullServiceId) = COALESCE(:serviceId, :nullServiceId) "
            + "AND s.status = 'ACTIVE' AND s.isDeleted = false "
            + "ORDER BY s.periodEnd DESC")
    List<Subscription> findActivePeriodsForPayerAndServiceForUpdate(
            @Param("payerId") UUID payerId,
            @Param("serviceType") String serviceType,
            @Param("serviceId") UUID serviceId,
            @Param("nullServiceId") UUID nullServiceId);
}