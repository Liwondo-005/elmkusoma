package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.AdminUserPermission;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdminUserPermissionRepository extends JpaRepository<AdminUserPermission, UUID> {

    @Query("SELECT p.permission FROM AdminUserPermission p " +
           "WHERE p.userId = :userId AND p.isDeleted = false ORDER BY p.permission")
    List<String> findPermissionsByUserId(@Param("userId") UUID userId);

    void deleteByUserIdAndIsDeletedFalse(UUID userId);
}