package tz.elmkusoma.shared.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.shared.domain.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndIsDeletedFalse(String email);

    boolean existsByEmailAndIsDeletedFalse(String email);

    @Query("SELECT u FROM User u WHERE u.institutionId = :institutionId AND u.isDeleted = false")
    java.util.List<User> findAllByInstitutionId(@Param("institutionId") UUID institutionId);
}
