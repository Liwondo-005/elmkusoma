package tz.elmkusoma.academic.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.academic.domain.Term;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TermRepository extends JpaRepository<Term, UUID> {

    List<Term> findByAcademicYearIdAndIsDeletedFalse(UUID academicYearId);

    List<Term> findByInstitutionIdAndIsDeletedFalse(UUID institutionId);

    @Query("SELECT t FROM Term t WHERE t.startDate <= :today AND t.endDate >= :today AND t.isDeleted = false ORDER BY t.startDate DESC")
    Optional<Term> findCurrentTerm(@Param("today") LocalDate today);
}