package tz.elmkusoma.oversight.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.course.domain.Announcement;
import tz.elmkusoma.course.repository.AnnouncementRepository;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.service.NotificationService;
import tz.elmkusoma.oversight.domain.District;
import tz.elmkusoma.oversight.domain.Region;
import tz.elmkusoma.oversight.dto.AnnouncementRequest;
import tz.elmkusoma.oversight.dto.AnnouncementResponse;
import tz.elmkusoma.oversight.repository.DistrictRepository;
import tz.elmkusoma.oversight.repository.RegionRepository;
import tz.elmkusoma.shared.domain.Institution;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.InstitutionRepository;
import tz.elmkusoma.shared.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Nationaladmin.md §23 — jurisdictional announcements.
 *
 * <p>Creation is persisted on the existing {@code announcements} table and
 * delivered through the existing {@link NotificationService}
 * ({@code learner_notifications}) — no second notification system.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OversightAnnouncementService {

    public static final String AUDIENCE_NATIONWIDE = "NATIONWIDE";
    public static final String AUDIENCE_REGION = "REGION";
    public static final String AUDIENCE_DISTRICT = "DISTRICT";

    private final AnnouncementRepository announcementRepository;
    private final RegionRepository regionRepository;
    private final DistrictRepository districtRepository;
    private final InstitutionRepository institutionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final OversightService oversightService;

    @Transactional
    public AnnouncementResponse create(AnnouncementRequest request, UUID actorUserId) {
        String audience = request.getAudienceType() == null
                ? null : request.getAudienceType().trim().toUpperCase();
        if (!AUDIENCE_NATIONWIDE.equals(audience)
                && !AUDIENCE_REGION.equals(audience)
                && !AUDIENCE_DISTRICT.equals(audience)) {
            throw new ForbiddenException("audience type", "create");
        }

        UUID regionId = null;
        UUID districtId = null;
        if (AUDIENCE_REGION.equals(audience)) {
            regionId = Optional.ofNullable(request.getAudienceRegionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Region", "id", "required"));
            if (!regionRepository.existsById(regionId)) {
                throw new ResourceNotFoundException("Region", "id", regionId);
            }
        } else if (AUDIENCE_DISTRICT.equals(audience)) {
            UUID requestedDistrictId = Optional.ofNullable(request.getAudienceDistrictId())
                    .orElseThrow(() -> new ResourceNotFoundException("District", "id", "required"));
            District district = districtRepository.findById(requestedDistrictId)
                    .orElseThrow(() -> new ResourceNotFoundException("District", "id", requestedDistrictId));
            districtId = district.getId();
            regionId = district.getRegionId();
        }

        LocalDateTime now = LocalDateTime.now();
        boolean scheduled = request.getScheduledAt() != null && request.getScheduledAt().isAfter(now);
        String status = scheduled ? "SCHEDULED" : "PUBLISHED";

        Announcement announcement = Announcement.builder()
                .authorId(actorUserId)
                .title(request.getTitle().trim())
                .content(request.getContent())
                .priority(normalizePriority(request.getPriority()))
                .audienceType(audience)
                .audienceRegionId(regionId)
                .audienceDistrictId(districtId)
                .status(status)
                .scheduledAt(request.getScheduledAt())
                .publishedAt(scheduled ? null : now)
                .build();
        announcement = announcementRepository.save(announcement);

        if (!scheduled) {
            fanOut(announcement);
        }
        return toResponse(announcement);
    }

    /** Scope-filtered listing: nationwide + matching audience + in-scope institution announcements. */
    public List<AnnouncementResponse> list(UUID regionId, UUID districtId) {
        Set<UUID> scopeInstitutionIds =
                new HashSet<>(oversightService.getInstitutionIdsInJurisdiction(regionId, districtId));

        List<Announcement> visible = new ArrayList<>();
        for (Announcement announcement : announcementRepository.findAllAndIsDeletedFalse()) {
            String audience = announcement.getAudienceType();
            if (audience == null) {
                // Legacy institution-scoped announcement — show only if in scope.
                if (announcement.getInstitutionId() != null
                        && scopeInstitutionIds.contains(announcement.getInstitutionId())) {
                    visible.add(announcement);
                }
                continue;
            }
            switch (audience) {
                case AUDIENCE_NATIONWIDE -> visible.add(announcement);
                case AUDIENCE_REGION -> {
                    if (announcement.getAudienceRegionId() != null
                            && announcement.getAudienceRegionId().equals(regionId)) {
                        visible.add(announcement);
                    } else if (districtId != null && announcement.getAudienceRegionId() != null
                            && districtRepository.findById(districtId)
                            .map(d -> announcement.getAudienceRegionId().equals(d.getRegionId()))
                            .orElse(false)) {
                        visible.add(announcement);
                    }
                }
                case AUDIENCE_DISTRICT -> {
                    if (districtId != null
                            && districtId.equals(announcement.getAudienceDistrictId())) {
                        visible.add(announcement);
                    } else if (regionId != null && announcement.getAudienceDistrictId() != null
                            && districtRepository.findById(announcement.getAudienceDistrictId())
                            .map(d -> regionId.equals(d.getRegionId()))
                            .orElse(false)) {
                        visible.add(announcement);
                    }
                }
                default -> { /* unknown audience — skip */ }
            }
        }

        return visible.stream()
                .sorted(Comparator.comparing(
                        (Announcement a) -> a.getPublishedAt() != null ? a.getPublishedAt() : a.getCreatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** Publishes SCHEDULED announcements whose time has arrived (wired into CoreScheduler). */
    @Transactional
    public int publishDueAnnouncements() {
        List<Announcement> due = announcementRepository.findDueScheduled(LocalDateTime.now());
        for (Announcement announcement : due) {
            announcement.setStatus("PUBLISHED");
            announcement.setPublishedAt(LocalDateTime.now());
            announcementRepository.save(announcement);
            fanOut(announcement);
            log.info("Published scheduled announcement {} ({})", announcement.getId(), announcement.getAudienceType());
        }
        return due.size();
    }

    // ------------------------------------------------------------------ helpers

    private void fanOut(Announcement announcement) {
        Set<UUID> targetUserIds = audienceUserIds(announcement).stream()
                .map(User::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (UUID userId : targetUserIds) {
            try {
                notificationService.notifyUser(userId,
                        announcement.getTitle(),
                        announcement.getContent(),
                        "ANNOUNCEMENT",
                        "ANNOUNCEMENT",
                        announcement.getId());
            } catch (Exception e) {
                log.warn("Announcement fan-out failed for user {}: {}", userId, e.getMessage());
            }
        }
        log.info("Announcement {} delivered to {} user(s)", announcement.getId(), targetUserIds.size());
    }

    private List<User> audienceUserIds(Announcement announcement) {
        String audience = announcement.getAudienceType();
        if (AUDIENCE_NATIONWIDE.equals(audience)) {
            return userRepository.findByIsActiveTrueAndIsDeletedFalse();
        }

        Set<UUID> institutionIds = new HashSet<>();
        if (AUDIENCE_REGION.equals(audience) && announcement.getAudienceRegionId() != null) {
            institutionRepository.findByRegionIdAndIsDeletedFalse(announcement.getAudienceRegionId())
                    .forEach(i -> institutionIds.add(i.getId()));
        } else if (AUDIENCE_DISTRICT.equals(audience) && announcement.getAudienceDistrictId() != null) {
            institutionRepository.findByDistrictIdAndIsDeletedFalse(announcement.getAudienceDistrictId())
                    .forEach(i -> institutionIds.add(i.getId()));
        }

        Map<UUID, User> users = new LinkedHashMap<>();
        if (AUDIENCE_REGION.equals(audience) && announcement.getAudienceRegionId() != null) {
            userRepository.findByRegionIdAndIsActiveTrueAndIsDeletedFalse(announcement.getAudienceRegionId())
                    .forEach(u -> users.put(u.getId(), u));
        } else if (AUDIENCE_DISTRICT.equals(audience) && announcement.getAudienceDistrictId() != null) {
            userRepository.findByDistrictIdAndIsActiveTrueAndIsDeletedFalse(announcement.getAudienceDistrictId())
                    .forEach(u -> users.put(u.getId(), u));
        }
        for (UUID institutionId : institutionIds) {
            userRepository.findAllByInstitutionId(institutionId)
                    .forEach(u -> users.put(u.getId(), u));
        }
        return new ArrayList<>(users.values());
    }

    public AnnouncementResponse toResponse(Announcement announcement) {
        String regionName = announcement.getAudienceRegionId() != null
                ? regionRepository.findById(announcement.getAudienceRegionId()).map(Region::getName).orElse(null)
                : null;
        String districtName = announcement.getAudienceDistrictId() != null
                ? districtRepository.findById(announcement.getAudienceDistrictId()).map(District::getName).orElse(null)
                : null;
        String authorName = announcement.getAuthorId() != null
                ? userRepository.findById(announcement.getAuthorId()).map(User::getFullName).orElse(null)
                : null;
        String institutionName = announcement.getInstitutionId() != null
                ? institutionRepository.findById(announcement.getInstitutionId()).map(Institution::getName).orElse(null)
                : null;

        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .content(announcement.getContent())
                .priority(announcement.getPriority())
                .audienceType(announcement.getAudienceType())
                .audienceRegionId(announcement.getAudienceRegionId())
                .audienceRegionName(regionName)
                .audienceDistrictId(announcement.getAudienceDistrictId())
                .audienceDistrictName(districtName)
                .status(announcement.getStatus())
                .scheduledAt(announcement.getScheduledAt())
                .publishedAt(announcement.getPublishedAt())
                .createdAt(announcement.getCreatedAt())
                .authorName(authorName)
                .institutionName(institutionName)
                .build();
    }

    private String normalizePriority(String priority) {
        if (priority == null) return "NORMAL";
        String p = priority.trim().toUpperCase();
        return switch (p) {
            case "LOW", "NORMAL", "HIGH", "URGENT" -> p;
            default -> "NORMAL";
        };
    }
}
