package tz.elmkusoma.shared.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.shared.domain.Institution;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, UUID> {

    Optional<Institution> findByCodeAndIsDeletedFalse(String code);

    boolean existsByCodeAndIsDeletedFalse(String code);

    List<Institution> findByIsDeletedFalse();

    Page<Institution> findByIsDeletedFalse(Pageable pageable);

    Page<Institution> findByIsActiveTrueAndIsDeletedFalse(Pageable pageable);
}
