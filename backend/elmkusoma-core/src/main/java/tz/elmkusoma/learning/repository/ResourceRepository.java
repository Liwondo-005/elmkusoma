package tz.elmkusoma.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.Resource;

import java.util.List;
import java.util.UUID;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, UUID> {

    List<Resource> findBySubjectIdAndIsDeletedFalse(UUID subjectId);

    List<Resource> findByClassGroupIdAndIsDeletedFalse(UUID classGroupId);

    List<Resource> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);
}
