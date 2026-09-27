package tz.elmkusoma.learning.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.learning.domain.Resource;
import tz.elmkusoma.learning.domain.ResourceAnalytics;
import tz.elmkusoma.learning.domain.VideoTutorial;
import tz.elmkusoma.learning.domain.VideoTutorialProgress;
import tz.elmkusoma.learning.dto.ResourceAnalyticsSummary;
import tz.elmkusoma.learning.dto.VideoTutorialAnalyticsResponse;
import tz.elmkusoma.learning.repository.ResourceAnalyticsRepository;
import tz.elmkusoma.learning.repository.ResourceRepository;
import tz.elmkusoma.learning.repository.VideoTutorialProgressRepository;
import tz.elmkusoma.learning.repository.VideoTutorialRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceAnalyticsService {

    private final ResourceAnalyticsRepository analyticsRepository;
    private final ResourceRepository resourceRepository;
    private final VideoTutorialRepository videoTutorialRepository;
    private final VideoTutorialProgressRepository progressRepository;

    @Transactional(readOnly = true)
    public ResourceAnalyticsSummary getSummary(UUID resourceId, UUID institutionId, LocalDate startDate, LocalDate endDate) {
        assertResourceAccess(resourceId, institutionId);
        LocalDate from = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate to = endDate != null ? endDate : LocalDate.now();
        ResourceAnalyticsSummary summary = analyticsRepository.getSummary(resourceId, from, to);
        if (summary == null) {
            return emptySummary();
        }
        summary.setTotalViews(summary.getTotalViews() != null ? summary.getTotalViews() : 0L);
        summary.setTotalDownloads(summary.getTotalDownloads() != null ? summary.getTotalDownloads() : 0L);
        summary.setUniqueViewers(summary.getUniqueViewers() != null ? summary.getUniqueViewers() : 0L);
        summary.setUniqueDownloaders(summary.getUniqueDownloaders() != null ? summary.getUniqueDownloaders() : 0L);
        summary.setTotalWatchTimeSeconds(summary.getTotalWatchTimeSeconds() != null ? summary.getTotalWatchTimeSeconds() : 0L);
        summary.setAvgWatchTimeSeconds(summary.getAvgWatchTimeSeconds() != null ? summary.getAvgWatchTimeSeconds() : 0.0);
        return summary;
    }

    @Transactional(readOnly = true)
    public List<ResourceAnalytics> getDaily(UUID resourceId, UUID institutionId, LocalDate startDate, LocalDate endDate) {
        assertResourceAccess(resourceId, institutionId);
        LocalDate from = startDate != null ? startDate : LocalDate.now().minusDays(30);
        LocalDate to = endDate != null ? endDate : LocalDate.now();
        return analyticsRepository.findByResourceIdAndDateBetween(resourceId, from, to);
    }

    @Transactional
    public ResourceAnalyticsSummary recordView(UUID resourceId, UUID institutionId) {
        assertResourceAccess(resourceId, institutionId);
        ResourceAnalytics daily = findOrCreate(resourceId, institutionId, LocalDate.now());
        daily.incrementView();
        analyticsRepository.save(daily);
        return getSummary(resourceId, institutionId, LocalDate.now().minusDays(30), LocalDate.now());
    }

    @Transactional
    public ResourceAnalyticsSummary recordDownload(UUID resourceId, UUID institutionId) {
        assertResourceAccess(resourceId, institutionId);
        ResourceAnalytics daily = findOrCreate(resourceId, institutionId, LocalDate.now());
        daily.incrementDownload();
        analyticsRepository.save(daily);
        return getSummary(resourceId, institutionId, LocalDate.now().minusDays(30), LocalDate.now());
    }

    @Transactional(readOnly = true)
    public VideoTutorialAnalyticsResponse getVideoAnalytics(UUID videoTutorialId, UUID institutionId) {
        VideoTutorial video = videoTutorialRepository.findById(videoTutorialId)
                .filter(v -> institutionId == null || institutionId.equals(v.getInstitutionId()))
                .orElseThrow(() -> new RuntimeException("Video tutorial not found"));

        List<VideoTutorialProgress> progresses =
                progressRepository.findByVideoTutorialIdAndIsDeletedFalse(videoTutorialId);

        long totalStudents = progresses.size();
        long completedStudents = progresses.stream().filter(p -> Boolean.TRUE.equals(p.getCompleted())).count();
        double completionRate = totalStudents == 0 ? 0.0
                : BigDecimal.valueOf(completedStudents * 100.0 / totalStudents)
                        .setScale(2, RoundingMode.HALF_UP).doubleValue();
        double avgCompletion = totalStudents == 0 ? 0.0
                : BigDecimal.valueOf(progresses.stream()
                        .mapToDouble(p -> p.getCompletionPercentage() != null ? p.getCompletionPercentage() : 0.0)
                        .average().orElse(0.0))
                        .setScale(2, RoundingMode.HALF_UP).doubleValue();
        long totalWatchTime = progresses.stream()
                .mapToLong(p -> p.getTotalWatchTimeSeconds() != null ? p.getTotalWatchTimeSeconds() : 0L)
                .sum();

        return VideoTutorialAnalyticsResponse.builder()
                .videoTutorialId(video.getId())
                .title(video.getTitle())
                .totalStudents(totalStudents)
                .completedStudents(completedStudents)
                .completionRate(completionRate)
                .avgCompletionPercentage(avgCompletion)
                .totalWatchTimeSeconds(totalWatchTime)
                .build();
    }

    private ResourceAnalytics findOrCreate(UUID resourceId, UUID institutionId, LocalDate date) {
        return analyticsRepository
                .findByResourceIdAndDateBetween(resourceId, date, date)
                .stream()
                .findFirst()
                .orElseGet(() -> analyticsRepository.save(ResourceAnalytics.builder()
                        .resourceId(resourceId)
                        .institutionId(institutionId)
                        .date(date)
                        .viewCount(0)
                        .downloadCount(0)
                        .uniqueViewers(0)
                        .uniqueDownloaders(0)
                        .totalWatchTimeSeconds(0L)
                        .avgWatchTimeSeconds(BigDecimal.ZERO)
                        .build()));
    }

    private void assertResourceAccess(UUID resourceId, UUID institutionId) {
        resourceRepository.findById(resourceId)
                .filter(r -> institutionId == null || institutionId.equals(r.getInstitutionId()))
                .orElseThrow(() -> new RuntimeException("Resource not found"));
    }

    private ResourceAnalyticsSummary emptySummary() {
        return ResourceAnalyticsSummary.builder()
                .totalViews(0L).totalDownloads(0L).uniqueViewers(0L)
                .uniqueDownloaders(0L).totalWatchTimeSeconds(0L).avgWatchTimeSeconds(0.0)
                .build();
    }
}
