package tz.elmkusoma.parent.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.parent.domain.Parent;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<Parent, UUID> {

    /**
     * Locks the payer row for the duration of the transaction.
     *
     * <p>Subscription activation needs a row that always exists to serialise against, because on a
     * payer's very first purchase there is no subscription row yet and so nothing to lock. Two
     * concurrent first purchases would both read "no current period" and both insert. Locking the
     * parent first makes the loser wait, re-read, and chain onto the winner's period instead.
     *
     * <p>The parent row is stable and already present, so this costs nothing extra and cannot
     * deadlock against the subscription lock: every path takes parent before subscription.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Parent p WHERE p.id = :id")
    Optional<Parent> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT p FROM Parent p WHERE p.institutionId = :institutionId AND p.isDeleted = false")
    List<Parent> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT p FROM Parent p WHERE p.id = :id AND p.institutionId = :institutionId AND p.isDeleted = false")
    Optional<Parent> findByIdAndInstitutionId(@Param("id") UUID id, @Param("institutionId") UUID institutionId);

    @Query("SELECT p FROM Parent p WHERE p.userId = :userId AND p.institutionId = :institutionId AND p.isDeleted = false")
    Optional<Parent> findByUserIdAndInstitutionId(@Param("userId") UUID userId, @Param("institutionId") UUID institutionId);

    boolean existsByUserIdAndInstitutionIdAndIsDeletedFalse(UUID userId, UUID institutionId);

    @Query("SELECT p FROM Parent p WHERE p.userId = :userId AND p.isDeleted = false")
    List<Parent> findAllByUserId(@Param("userId") UUID userId);

    @Query("SELECT p FROM Parent p WHERE p.userId = :userId AND p.isDeleted = false")
    Optional<Parent> findByUserIdAndIsDeletedFalse(@Param("userId") UUID userId);
}
