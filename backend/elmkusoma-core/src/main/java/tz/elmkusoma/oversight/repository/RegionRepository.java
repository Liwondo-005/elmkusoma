package tz.elmkusoma.oversight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.oversight.domain.Region;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, java.util.UUID> {
    List<Region> findByIsDeletedFalseOrderByCreatedAtDesc();
    Optional<Region> findByCodeAndIsDeletedFalse(String code);
}
