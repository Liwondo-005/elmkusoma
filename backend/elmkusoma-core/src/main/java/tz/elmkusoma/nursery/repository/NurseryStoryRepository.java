package tz.elmkusoma.nursery.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.nursery.domain.NurseryStory;

import java.util.List;
import java.util.UUID;

@Repository
public interface NurseryStoryRepository extends JpaRepository<NurseryStory, UUID> {
    List<NurseryStory> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);
    List<NurseryStory> findByClassGroupIdAndStoryTypeAndIsDeletedFalse(UUID classGroupId, NurseryStory.StoryType storyType);
    List<NurseryStory> findByClassGroupIdAndIsPublishedAndIsDeletedFalse(UUID classGroupId, Boolean isPublished);
}
