package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.RolePermission;

import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    @Query("SELECT rp.permission FROM RolePermission rp WHERE rp.roleId = :roleId")
    List<String> findPermissionsByRoleId(@Param("roleId") UUID roleId);

    void deleteByRoleId(UUID roleId);
}
