package tz.elmkusoma.media.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.media.domain.MediaFile;

import java.util.List;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    List<MediaFile> findByInstitutionIdAndIsDeletedFalse(Long institutionId);

    List<MediaFile> findByUserIdAndIsDeletedFalse(Long userId);

    List<MediaFile> findByInstitutionIdAndContentTypeAndIsDeletedFalse(Long institutionId, String contentType);

    List<MediaFile> findByInstitutionIdAndContentTypeAndIsDeletedFalseOrderByCreatedAtDesc(Long institutionId, String contentType);
}
