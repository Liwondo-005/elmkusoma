package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.administration.domain.ProviderServiceEntitlement;

import java.util.List;
import java.util.UUID;

public interface ProviderServiceEntitlementRepository extends JpaRepository<ProviderServiceEntitlement, UUID> {
    List<ProviderServiceEntitlement> findByProviderIdAndIsDeletedFalse(UUID providerId);
    long countByStatusAndIsDeletedFalse(String status);
    java.util.Optional<ProviderServiceEntitlement> findByProviderIdAndServiceIdAndIsDeletedFalse(UUID providerId, UUID serviceId);
}
