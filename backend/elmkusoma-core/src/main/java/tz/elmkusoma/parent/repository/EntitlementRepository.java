package tz.elmkusoma.parent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
    boolean existsByStudentIdAndServiceTypeAndServiceIdAndStatusAndIsDeletedFalse(UUID studentId, String serviceType, UUID serviceId, String status);
    List<Entitlement> findByStudentIdAndServiceTypeAndIsDeletedFalse(UUID studentId, String serviceType);
    long countByStudentIdAndStatusAndIsDeletedFalse(UUID studentId, String status);
}
