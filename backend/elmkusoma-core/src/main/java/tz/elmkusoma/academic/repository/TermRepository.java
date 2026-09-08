package tz.elmkusoma.academic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.academic.domain.Term;

import java.util.List;
import java.util.UUID;

@Repository
public interface TermRepository extends JpaRepository<Term, UUID> {

    List<Term> findByAcademicYearIdAndIsDeletedFalse(UUID academicYearId);

    List<Term> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);
}
