package tz.elmkusoma.shared.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.shared.domain.Institution;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InstitutionRepository extends JpaRepository<Institution, UUID> {

    Optional<Institution> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByEmailAddress(String emailAddress);

    Page<Institution> findByStatus(Institution.InstitutionStatus status, Pageable pageable);

    @Query("SELECT i FROM Institution i WHERE " +
           "(LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(i.code) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Institution> search(@Param("query") String query, Pageable pageable);
}
