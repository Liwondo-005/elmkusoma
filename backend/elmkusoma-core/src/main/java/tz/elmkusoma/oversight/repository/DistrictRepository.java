package tz.elmkusoma.oversight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.oversight.domain.District;

import java.util.List;
import java.util.Optional;

@Repository
public interface DistrictRepository extends JpaRepository<District, java.util.UUID> {
    List<District> findByRegionIdAndIsDeletedFalse(java.util.UUID regionId);
    List<District> findByIsDeletedFalseOrderByCreatedAtDesc();
    Optional<District> findByCodeAndIsDeletedFalse(String code);
    long countByRegionIdAndIsDeletedFalse(java.util.UUID regionId);
}
