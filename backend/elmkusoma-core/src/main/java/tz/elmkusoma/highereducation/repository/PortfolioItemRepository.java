package tz.elmkusoma.highereducation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.highereducation.domain.PortfolioItem;
import tz.elmkusoma.highereducation.domain.PortfolioItemType;

import java.util.List;
import java.util.UUID;

@Repository
public interface PortfolioItemRepository extends JpaRepository<PortfolioItem, UUID> {

    List<PortfolioItem> findByPortfolioIdAndIsDeletedFalse(UUID portfolioId);

    List<PortfolioItem> findByPortfolioIdAndItemTypeAndIsDeletedFalse(UUID portfolioId, PortfolioItemType itemType);

    long countByPortfolioIdAndIsDeletedFalse(UUID portfolioId);
}
