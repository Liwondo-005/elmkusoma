package tz.elmkusoma.shared.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    long countByIsDeletedFalse();

    long countByIsActiveAndIsDeletedFalse(boolean isActive);

    @Query("SELECT i FROM Institution i WHERE i.isDeleted = false AND (LOWER(i.name) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(i.code) LIKE LOWER(CONCAT('%', :query, '%'))) ORDER BY i.name")
    Page<Institution> findBySearchTermAndIsDeletedFalse(@Param("query") String query, Pageable pageable);

    Optional<Institution> findByIdAndIsDeletedFalse(UUID id);

    long countByRegionIdAndIsDeletedFalse(UUID regionId);

    long countByDistrictIdAndIsDeletedFalse(UUID districtId);

    List<Institution> findByRegionIdAndIsDeletedFalse(UUID regionId);

    List<Institution> findByDistrictIdAndIsDeletedFalse(UUID districtId);

    @Query("SELECT COUNT(i) FROM Institution i WHERE i.id IN :institutionIds AND i.isDeleted = false")
    long countByInstitutionIdsAndIsDeletedFalse(@Param("institutionIds") List<UUID> institutionIds);

    @Query(value = "SELECT COUNT(*) FROM (SELECT a.institution_id FROM institutions i " +
            "JOIN attendance_summary a ON a.institution_id = i.id " +
            "WHERE i.id IN :institutionIds AND i.is_deleted = false AND a.is_deleted = false " +
            "GROUP BY a.institution_id HAVING AVG(a.attendance_percentage) < :threshold) sub", nativeQuery = true)
    long countByInstitutionIdsAndAttendanceBelow(@Param("institutionIds") List<UUID> institutionIds, @Param("threshold") double threshold);
}