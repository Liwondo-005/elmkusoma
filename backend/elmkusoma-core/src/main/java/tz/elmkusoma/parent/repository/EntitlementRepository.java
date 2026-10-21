package tz.elmkusoma.parent.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.Entitlement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EntitlementRepository extends JpaRepository<Entitlement, UUID> {
    List<Entitlement> findByUserIdAndIsDeletedFalse(UUID userId);
    List<Entitlement> findByStudentIdAndIsDeletedFalse(UUID studentId);
    Optional<Entitlement> findByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(UUID studentId, String serviceType, UUID serviceId);
    /**
     * Every live row for one (learner, service) key, newest periods kept alongside older ones.
     *
     * <p>A list, not an {@link Optional}: nothing constrains this key to a single row, and the
     * repurchase-after-expiry rule needs to see the elapsed period so it can close it out rather
     * than trip over a "more than one row" failure at the moment money has already been taken.
     */
    List<Entitlement> findAllByStudentIdAndServiceTypeAndServiceIdAndIsDeletedFalse(UUID studentId, String serviceType, UUID serviceId);
    /**
     * Entitlements whose stored status still says ACTIVE although their period has already ended.
     *
     * <p>Nothing else in the codebase ever flips this status, so without this query an elapsed
     * entitlement stays ACTIVE forever and every count, filter or report that trusts the string
     * overstates live access.
     */
    List<Entitlement> findAllByStatusAndExpiresAtLessThanEqualAndIsDeletedFalse(String status, java.time.LocalDateTime now);
    boolean existsByStudentIdAndServiceTypeAndServiceIdAndStatusAndIsDeletedFalse(UUID studentId, String serviceType, UUID serviceId, String status);
    List<Entitlement> findByStudentIdAndServiceTypeAndIsDeletedFalse(UUID studentId, String serviceType);
    long countByStudentIdAndStatusAndIsDeletedFalse(UUID studentId, String status);
    Page<Entitlement> findByStatusAndIsDeletedFalse(String status, Pageable pageable);
    Page<Entitlement> findAllByIsDeletedFalse(Pageable pageable);
    long countByIsDeletedFalse();

    /**
     * Serialises grant attempts for one (learner, service) pair for the rest of this transaction.
     *
     * <p>This exists because a pessimistic row lock cannot do the job. The grant path reads whether
     * the learner already holds a service and then inserts; when no row exists yet there is nothing
     * for {@code SELECT ... FOR UPDATE} to lock, so two transactions both read "not held", both
     * insert, and the learner ends up holding the same access twice. Locking the existing rows is
     * precisely the case where locking does nothing.
     *
     * <p>A transaction-scoped advisory lock is held by the database, not by a JVM, so it holds
     * across every application instance rather than per process. It serialises rather than rejects:
     * a legitimate second purchase after an expiry still proceeds once the first commits, which a
     * blanket uniqueness constraint on the grant would wrongly forbid. A hash collision only costs
     * a little needless waiting and cannot merge two different grants.
     *
     * <p>Released automatically when the transaction commits or rolls back, including on failure,
     * so a rejected payment cannot leave a later purchase blocked.
     */
    @Query(value = "SELECT pg_advisory_xact_lock(hashtextextended(:grantKey, 0))", nativeQuery = true)
    void lockGrantKey(@Param("grantKey") String grantKey);
}
