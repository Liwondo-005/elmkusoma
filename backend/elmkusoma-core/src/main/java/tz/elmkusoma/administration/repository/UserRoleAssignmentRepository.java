package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.UserRoleAssignment;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {
    List<UserRoleAssignment> findByUserIdAndIsDeletedFalse(UUID userId);
    List<UserRoleAssignment> findByRoleIdAndIsDeletedFalse(UUID roleId);
}
