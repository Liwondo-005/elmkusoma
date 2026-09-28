package tz.elmkusoma.media.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.media.domain.MediaFile;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    List<MediaFile> findByUserIdAndIsDeletedFalse(UUID userId);

    List<MediaFile> findByInstitutionIdAndContentTypeAndIsDeletedFalse(UUID institutionId, String contentType);

    List<MediaFile> findByInstitutionIdAndContentTypeAndIsDeletedFalseOrderByCreatedAtDesc(UUID institutionId, String contentType);
}
