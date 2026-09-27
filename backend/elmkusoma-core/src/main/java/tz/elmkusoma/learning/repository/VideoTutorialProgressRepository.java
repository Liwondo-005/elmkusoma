package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.VideoTutorialProgress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoTutorialProgressRepository extends JpaRepository<VideoTutorialProgress, UUID> {

    Optional<VideoTutorialProgress> findByVideoTutorialIdAndStudentId(UUID videoTutorialId, UUID studentId);

    List<VideoTutorialProgress> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<VideoTutorialProgress> findByVideoTutorialIdAndIsDeletedFalse(UUID videoTutorialId);

    long countByVideoTutorialIdAndIsDeletedFalse(UUID videoTutorialId);

    long countByVideoTutorialIdAndCompletedTrue(UUID videoTutorialId);
}