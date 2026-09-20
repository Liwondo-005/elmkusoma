package tz.elmkusoma.administration.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.administration.domain.AdminDelegation;

import java.util.List;
import java.util.UUID;

public interface AdminDelegationRepository extends JpaRepository<AdminDelegation, UUID> {
    List<AdminDelegation> findByDelegatorIdAndIsDeletedFalse(UUID delegatorId);
    List<AdminDelegation> findByDelegateIdAndIsDeletedFalse(UUID delegateId);
    long countByStatusAndIsDeletedFalse(String status);
}
