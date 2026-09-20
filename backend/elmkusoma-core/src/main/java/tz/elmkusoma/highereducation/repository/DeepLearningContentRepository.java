package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.elmkusoma.highereducation.domain.DeepContentType;
import tz.elmkusoma.highereducation.domain.DeepLearningContent;

import java.util.List;
import java.util.UUID;

public interface DeepLearningContentRepository extends JpaRepository<DeepLearningContent, UUID> {
    List<DeepLearningContent> findByStudentIdOrderByCreatedAtDesc(UUID studentId);
    List<DeepLearningContent> findByStudentIdAndCourseId(UUID studentId, UUID courseId);
    List<DeepLearningContent> findByStudentIdAndContentType(UUID studentId, DeepContentType contentType);
}
