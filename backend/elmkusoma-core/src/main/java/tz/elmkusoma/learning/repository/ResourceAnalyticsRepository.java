package tz.elmkusoma.learning.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tz.elmkusoma.learning.domain.ResourceAnalytics;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ResourceAnalyticsRepository extends JpaRepository<ResourceAnalytics, UUID> {

    Page<ResourceAnalytics> findByResourceIdOrderByDateDesc(UUID resourceId, Pageable pageable);

    List<ResourceAnalytics> findByResourceIdAndDateBetween(UUID resourceId, java.time.LocalDate startDate, java.time.LocalDate endDate);

    @Query("SELECT ra FROM ResourceAnalytics ra WHERE ra.resourceId = :resourceId ORDER BY ra.date DESC")
    List<ResourceAnalytics> findByResourceIdOrderByDateDesc(@Param("resourceId") UUID resourceId);

    @Query("SELECT NEW tz.elmkusoma.learning.dto.ResourceAnalyticsSummary(" +
            "SUM(ra.viewCount), SUM(ra.downloadCount), SUM(ra.uniqueViewers), " +
            "SUM(ra.uniqueDownloaders), SUM(ra.totalWatchTimeSeconds), AVG(ra.avgWatchTimeSeconds)) " +
            "FROM ResourceAnalytics ra WHERE ra.resourceId = :resourceId AND ra.date BETWEEN :startDate AND :endDate")
    tz.elmkusoma.learning.dto.ResourceAnalyticsSummary getSummary(
            @Param("resourceId") UUID resourceId,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate);

    long countByResourceId(UUID resourceId);
}