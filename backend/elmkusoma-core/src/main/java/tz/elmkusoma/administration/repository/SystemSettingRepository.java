package tz.elmkusoma.administration.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.administration.domain.SystemSetting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, UUID> {

    Optional<SystemSetting> findByInstitutionIdAndSettingKeyAndIsDeletedFalse(UUID institutionId, String settingKey);

    @Query("SELECT s FROM SystemSetting s WHERE s.institutionId = :institutionId AND s.isDeleted = false")
    List<SystemSetting> findAllByInstitutionId(@Param("institutionId") UUID institutionId);

    @Query("SELECT s FROM SystemSetting s WHERE s.institutionId = :institutionId AND s.isPublic = true AND s.isDeleted = false")
    List<SystemSetting> findPublicByInstitutionId(@Param("institutionId") UUID institutionId);

    boolean existsByInstitutionIdAndSettingKeyAndIsDeletedFalse(UUID institutionId, String settingKey);
}
