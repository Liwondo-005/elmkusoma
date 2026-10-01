package tz.elmkusoma.oversight.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.oversight.domain.Ward;

import java.util.List;
import java.util.UUID;

@Repository
public interface WardRepository extends JpaRepository<Ward, UUID> {

    List<Ward> findByDistrictIdAndIsDeletedFalse(UUID districtId);

    List<Ward> findByDistrictIdInAndIsDeletedFalse(List<UUID> districtIds);

    long countByDistrictIdAndIsDeletedFalse(UUID districtId);
}
