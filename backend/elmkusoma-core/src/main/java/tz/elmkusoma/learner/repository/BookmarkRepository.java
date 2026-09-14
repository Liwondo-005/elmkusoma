package tz.elmkusoma.learner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learner.domain.Bookmark;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {

    List<Bookmark> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    Optional<Bookmark> findByUserIdAndTargetTypeAndTargetIdAndIsDeletedFalse(UUID userId, String targetType, UUID targetId);

    boolean existsByUserIdAndTargetTypeAndTargetIdAndIsDeletedFalse(UUID userId, String targetType, UUID targetId);

    long countByUserIdAndIsDeletedFalse(UUID userId);
}
