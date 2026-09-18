package tz.elmkusoma.primary.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.primary.domain.PortfolioItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, UUID> {

    List<PortfolioItem> findByStudentIdAndInstitutionIdAndIsDeletedFalseOrderByDisplayOrderAscCreatedAtDesc(
            UUID studentId, UUID institutionId);

    Optional<PortfolioItem> findByIdAndStudentIdAndIsDeletedFalse(UUID id, UUID studentId);
}
