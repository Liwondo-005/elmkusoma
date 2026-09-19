package tz.elmkusoma.shared.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.shared.domain.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByEmailAndIsDeletedFalse(String email);

    @Query("SELECT u FROM User u JOIN InstitutionMembership m ON u.id = m.userId WHERE m.institutionId = :institutionId AND m.isActive = true AND u.isDeleted = false")
    java.util.List<User> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    long countByIsDeletedFalse();

    long countByRoleAndIsDeletedFalse(User.Role role);

    long countByIsActiveAndIsDeletedFalse(boolean isActive);

    Page<User> findAllByIsDeletedFalse(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND (LOWER(u.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY u.createdAt DESC")
    Page<User> findBySearchTermAndIsDeletedFalse(@Param("query") String query, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u JOIN InstitutionMembership m ON u.id = m.userId WHERE m.institutionId = :institutionId AND m.isActive = true AND u.isDeleted = false AND u.role = :role")
    long countByInstitutionIdAndRoleAndIsDeletedFalse(@Param("institutionId") UUID institutionId, @Param("role") User.Role role);

    Optional<User> findByIdAndIsDeletedFalse(UUID id);

    Page<User> findByRoleAndIsDeletedFalse(User.Role role, Pageable pageable);
}
