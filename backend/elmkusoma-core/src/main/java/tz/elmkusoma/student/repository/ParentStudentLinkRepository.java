package tz.elmkusoma.student.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.student.domain.ParentStudentLink;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParentStudentLinkRepository extends JpaRepository<ParentStudentLink, UUID> {

    List<ParentStudentLink> findByStudentIdAndIsDeletedFalse(UUID studentId);

    List<ParentStudentLink> findByParentUserIdAndIsDeletedFalse(UUID parentUserId);

    List<ParentStudentLink> findByParentUserIdAndIsActiveTrueAndIsDeletedFalse(UUID parentUserId);
}
