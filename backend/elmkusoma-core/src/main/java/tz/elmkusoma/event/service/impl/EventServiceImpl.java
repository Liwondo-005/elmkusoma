package tz.elmkusoma.event.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.EventMaterial;
import tz.elmkusoma.event.domain.EventRegistration;
import tz.elmkusoma.event.dto.*;
import tz.elmkusoma.event.repository.EventMaterialRepository;
import tz.elmkusoma.event.repository.EventRegistrationRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.event.service.EventService;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventMaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final tz.elmkusoma.shared.repository.InstitutionRepository institutionRepository;

    public EventServiceImpl(EventRepository eventRepository,
                           EventRegistrationRepository registrationRepository,
                           EventMaterialRepository materialRepository,
                           UserRepository userRepository,
                           tz.elmkusoma.shared.repository.InstitutionRepository institutionRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.materialRepository = materialRepository;
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
    }

    @Override
    public List<EventResponse> getEvents(UUID institutionId, String status, String eventType, String category) {
        List<Event> events;
        if (category != null && !category.isBlank()) {
            events = eventRepository.findByCategory(institutionId, category);
        } else if (eventType != null && !eventType.isBlank()) {
            events = eventRepository.findByInstitutionIdAndEventTypeAndIsDeletedFalseOrderByStartsAtAsc(institutionId, eventType);
        } else if (status != null && !status.isBlank()) {
            events = eventRepository.findByInstitutionIdAndStatusAndIsDeletedFalseOrderByStartsAtAsc(institutionId, status);
        } else {
            events = eventRepository.findByInstitutionIdAndIsDeletedFalseOrderByStartsAtAsc(institutionId);
        }
        return events.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<EventResponse> searchEvents(UUID institutionId, String query) {
        if (query == null || query.isBlank()) {
            return getEvents(institutionId, "PUBLISHED", null, null);
        }
        List<Event> events = eventRepository.searchByQuery(institutionId, query);
        return events.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public EventResponse getEventById(UUID eventId, UUID currentUserId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (event.getIsDeleted()) {
            throw new IllegalArgumentException("Event not found");
        }
        if (!"PUBLISHED".equals(event.getStatus()) && !"COMPLETED".equals(event.getStatus())) {
            throw new IllegalArgumentException("Event not found");
        }
        EventResponse response = mapToResponse(event);
        if (currentUserId != null) {
            response.setIsRegistered(registrationRepository.existsByEventIdAndUserIdAndIsDeletedFalse(eventId, currentUserId));
            if (response.getIsRegistered()) {
                registrationRepository.findByEventIdAndUserIdAndIsDeletedFalse(eventId, currentUserId)
                        .ifPresent(reg -> response.setRegistrationStatus(reg.getStatus()));
            }
        }
        return response;
    }

    @Override
    public EventResponse getEventByIdForAdmin(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (event.getIsDeleted()) {
            throw new IllegalArgumentException("Event not found");
        }
        return mapToResponse(event);
    }

    @Override
    public List<EventResponse> getUpcomingEvents(UUID institutionId) {
        List<Event> events = eventRepository.findUpcomingPublished(institutionId, LocalDateTime.now());
        return events.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public List<EventResponse> getPastEvents(UUID institutionId) {
        LocalDateTime now = LocalDateTime.now();
        List<Event> events = eventRepository.findPastOrOngoing(institutionId, now, now.minusDays(365));
        return events.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public EventResponse createEvent(UUID institutionId, UUID organizerId, EventRequest request) {
        Event event = Event.builder()
                .institutionId(institutionId)
                .organizerId(organizerId)
                .title(request.getTitle())
                .description(request.getDescription())
                .eventType(request.getEventType())
                .category(request.getCategory())
                .location(request.getLocation())
                .meetingUrl(request.getMeetingUrl())
                .startsAt(parseDateTime(request.getStartsAt()))
                .endsAt(request.getEndsAt() != null ? parseDateTime(request.getEndsAt()) : null)
                .durationMinutes(request.getDurationMinutes())
                .maxParticipants(request.getMaxParticipants())
                .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                .thumbnailUrl(request.getThumbnailUrl())
                .tags(request.getTags())
                .isFree(request.getIsFree() != null ? request.getIsFree() : true)
                .requiresApproval(request.getRequiresApproval() != null ? request.getRequiresApproval() : false)
                .eventFormat(request.getEventFormat())
                .difficulty(request.getDifficulty())
                .targetAudience(request.getTargetAudience())
                .prerequisites(request.getPrerequisites())
                .learningOutcomes(request.getLearningOutcomes())
                .agenda(request.getAgenda())
                .build();

        if (event.getEndsAt() == null && event.getDurationMinutes() != null) {
            event.setEndsAt(event.getStartsAt().plusMinutes(event.getDurationMinutes()));
        }

        event = eventRepository.save(event);
        log.info("Event created: {} by institution {}", event.getId(), institutionId);
        return mapToResponse(event);
    }

    @Override
    public EventResponse updateEvent(UUID eventId, EventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        String oldStatus = event.getStatus();

        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getEventType() != null) event.setEventType(request.getEventType());
        if (request.getCategory() != null) event.setCategory(request.getCategory());
        if (request.getLocation() != null) event.setLocation(request.getLocation());
        if (request.getMeetingUrl() != null) event.setMeetingUrl(request.getMeetingUrl());
        if (request.getStartsAt() != null) event.setStartsAt(parseDateTime(request.getStartsAt()));
        if (request.getEndsAt() != null) event.setEndsAt(parseDateTime(request.getEndsAt()));
        if (request.getDurationMinutes() != null) event.setDurationMinutes(request.getDurationMinutes());
        if (request.getMaxParticipants() != null) event.setMaxParticipants(request.getMaxParticipants());
        if (request.getStatus() != null) event.setStatus(request.getStatus());
        if (request.getThumbnailUrl() != null) event.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getTags() != null) event.setTags(request.getTags());
        if (request.getIsFree() != null) event.setIsFree(request.getIsFree());
        if (request.getRequiresApproval() != null) event.setRequiresApproval(request.getRequiresApproval());
        if (request.getEventFormat() != null) event.setEventFormat(request.getEventFormat());
        if (request.getDifficulty() != null) event.setDifficulty(request.getDifficulty());
        if (request.getTargetAudience() != null) event.setTargetAudience(request.getTargetAudience());
        if (request.getPrerequisites() != null) event.setPrerequisites(request.getPrerequisites());
        if (request.getLearningOutcomes() != null) event.setLearningOutcomes(request.getLearningOutcomes());
        if (request.getAgenda() != null) event.setAgenda(request.getAgenda());

        event = eventRepository.save(event);
        log.info("Event updated: id={}, institutionId={}", event.getId(), event.getInstitutionId());
        if (request.getStatus() != null && !request.getStatus().equals(oldStatus)) {
            log.info("Event status changed: id={}, oldStatus={}, newStatus={}", eventId, oldStatus, request.getStatus());
        }
        return mapToResponse(event);
    }

    @Override
    public void deleteEvent(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        UUID institutionId = event.getInstitutionId();
        event.setIsDeleted(true);
        eventRepository.save(event);
        log.info("Event deleted: id={}, institutionId={}", eventId, institutionId);
    }

    @Override
    public EventRegistrationResponse registerForEvent(UUID eventId, UUID userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (!"PUBLISHED".equals(event.getStatus())) {
            throw new IllegalArgumentException("This event is not available for registration");
        }

        if (event.getStartsAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot register for a past event");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (event.getInstitutionId() != null && user.getInstitutionId() != null
                && !event.getInstitutionId().equals(user.getInstitutionId())) {
            throw new IllegalArgumentException("This event is not available in your institution");
        }

        java.util.Optional<EventRegistration> existing = registrationRepository.findByEventIdAndUserIdAndIsDeletedFalse(eventId, userId);
        if (existing.isPresent()) {
            EventRegistration reg = existing.get();
            if ("REGISTERED".equals(reg.getStatus())) {
                throw new IllegalArgumentException("You are already registered for this event");
            }
            if ("CANCELLED".equals(reg.getStatus())) {
                if (event.getMaxParticipants() != null) {
                    long registeredCount = registrationRepository.countByEventIdAndStatusAndIsDeletedFalse(eventId, "REGISTERED");
                    if (registeredCount >= event.getMaxParticipants()) {
                        throw new IllegalArgumentException("This event is full");
                    }
                }
                reg.setStatus(event.getRequiresApproval() ? "WAITLISTED" : "REGISTERED");
                reg.setCancelledAt(null);
                reg.setCancellationReason(null);
                reg.setRegisteredAt(LocalDateTime.now());
                reg = registrationRepository.save(reg);
                log.info("User {} re-registered for event {}", userId, eventId);
                return mapRegistrationToResponse(reg, event);
            }
        }

        if (event.getMaxParticipants() != null) {
            long registeredCount = registrationRepository.countByEventIdAndStatusAndIsDeletedFalse(eventId, "REGISTERED");
            if (registeredCount >= event.getMaxParticipants()) {
                throw new IllegalArgumentException("This event is full");
            }
        }

        EventRegistration registration = EventRegistration.builder()
                .eventId(eventId)
                .userId(userId)
                .status(event.getRequiresApproval() ? "WAITLISTED" : "REGISTERED")
                .registeredAt(LocalDateTime.now())
                .attended(false)
                .build();

        registration = registrationRepository.save(registration);
        log.info("User {} registered for event {}", userId, eventId);
        return mapRegistrationToResponse(registration, event);
    }

    @Override
    public void cancelRegistration(UUID eventId, UUID userId, String reason) {
        EventRegistration registration = registrationRepository.findByEventIdAndUserIdAndIsDeletedFalse(eventId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));

        if ("CANCELLED".equals(registration.getStatus())) {
            throw new IllegalArgumentException("Registration is already cancelled");
        }

        registration.setStatus("CANCELLED");
        registration.setCancelledAt(LocalDateTime.now());
        registration.setCancellationReason(reason);
        registrationRepository.save(registration);
        log.info("User {} cancelled registration for event {}", userId, eventId);
    }

    @Override
    public List<EventRegistrationResponse> getUserRegistrations(UUID userId) {
        List<EventRegistration> registrations = registrationRepository.findActiveRegistrationsByUser(userId);
        return registrations.stream().map(reg -> {
            Event event = eventRepository.findById(reg.getEventId()).orElse(null);
            return mapRegistrationToResponse(reg, event);
        }).collect(Collectors.toList());
    }

    @Override
    public List<EventRegistrationResponse> getEventRegistrations(UUID eventId, UUID requesterId) {
        List<EventRegistration> registrations = registrationRepository.findByEventIdAndIsDeletedFalse(eventId);
        return registrations.stream().map(reg -> {
            Event event = eventRepository.findById(reg.getEventId()).orElse(null);
            return mapRegistrationToResponse(reg, event);
        }).collect(Collectors.toList());
    }

    @Override
    public boolean isUserRegistered(UUID eventId, UUID userId) {
        return registrationRepository.existsByEventIdAndUserIdAndIsDeletedFalse(eventId, userId);
    }

    @Override
    public List<EventMaterialResponse> getEventMaterials(UUID eventId) {
        List<EventMaterial> materials = materialRepository.findByEventIdAndIsDeletedFalseOrderBySortOrderAsc(eventId);
        return materials.stream().map(this::mapMaterialToResponse).collect(Collectors.toList());
    }

    @Override
    public List<EventMaterialResponse> getPublicEventMaterials(UUID eventId) {
        List<EventMaterial> materials = materialRepository.findByEventIdAndIsPublicTrueAndIsDeletedFalseOrderBySortOrderAsc(eventId);
        return materials.stream().map(this::mapMaterialToResponse).collect(Collectors.toList());
    }

    @Override
    public EventMaterialResponse addEventMaterial(EventMaterialRequest request, UUID uploadedBy) {
        EventMaterial material = EventMaterial.builder()
                .eventId(request.getEventId())
                .title(request.getTitle())
                .description(request.getDescription())
                .materialType(request.getMaterialType())
                .fileUrl(request.getFileUrl())
                .fileSize(request.getFileSize())
                .durationMinutes(request.getDurationMinutes())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .isPublic(request.getIsPublic() != null ? request.getIsPublic() : true)
                .uploadedBy(uploadedBy)
                .build();

        material = materialRepository.save(material);
        log.info("Material added to event {}: {}", request.getEventId(), material.getTitle());
        return mapMaterialToResponse(material);
    }

    @Override
    public void deleteEventMaterial(UUID materialId) {
        EventMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found"));
        material.setIsDeleted(true);
        materialRepository.save(material);
    }

    @Override
    public List<EventResponse> getRegisteredEvents(UUID userId, UUID institutionId) {
        List<EventRegistration> registrations = registrationRepository.findUpcomingByUser(userId);
        return registrations.stream()
                .map(reg -> eventRepository.findById(reg.getEventId()).orElse(null))
                .filter(e -> e != null && !e.getIsDeleted() && e.getInstitutionId().equals(institutionId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<EventResponse> getRegisteredPastEvents(UUID userId, UUID institutionId) {
        List<EventRegistration> registrations = registrationRepository.findActiveRegistrationsByUser(userId);
        LocalDateTime now = LocalDateTime.now();
        return registrations.stream()
                .map(reg -> eventRepository.findById(reg.getEventId()).orElse(null))
                .filter(e -> e != null && !e.getIsDeleted() && e.getInstitutionId().equals(institutionId)
                        && e.getStartsAt().isBefore(now))
                .sorted((a, b) -> b.getStartsAt().compareTo(a.getStartsAt()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public EventResponse publishEvent(UUID eventId, UUID institutionId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (institutionId != null && !institutionId.equals(event.getInstitutionId())) {
            throw new IllegalArgumentException("Access denied");
        }
        event.setStatus("PUBLISHED");
        event = eventRepository.save(event);
        log.info("Event published: id={}", eventId);
        return mapToResponse(event);
    }

    @Override
    public EventResponse cancelEvent(UUID eventId, UUID institutionId, String reason) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (institutionId != null && !institutionId.equals(event.getInstitutionId())) {
            throw new IllegalArgumentException("Access denied");
        }
        event.setStatus("CANCELLED");
        event.setCancelledAt(LocalDateTime.now());
        event.setCancellationReason(reason);
        event = eventRepository.save(event);
        log.info("Event cancelled: id={}", eventId);
        return mapToResponse(event);
    }

    @Override
    public EventResponse startLiveEvent(UUID eventId, UUID institutionId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (institutionId != null && !institutionId.equals(event.getInstitutionId())) {
            throw new IllegalArgumentException("Access denied");
        }
        event.setStatus("LIVE");
        event.setEventStatus(tz.elmkusoma.event.domain.EventStatus.LIVE);
        event = eventRepository.save(event);
        log.info("Event started live: id={}", eventId);
        return mapToResponse(event);
    }

    @Override
    public EventResponse endLiveEvent(UUID eventId, UUID institutionId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (institutionId != null && !institutionId.equals(event.getInstitutionId())) {
            throw new IllegalArgumentException("Access denied");
        }
        event.setStatus("ENDED");
        event.setEventStatus(tz.elmkusoma.event.domain.EventStatus.ENDED);
        event = eventRepository.save(event);
        log.info("Event ended: id={}", eventId);
        return mapToResponse(event);
    }

    @Override
    public Map<String, Object> getEventSummary(UUID eventId, UUID institutionId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));
        if (institutionId != null && !institutionId.equals(event.getInstitutionId())) {
            throw new IllegalArgumentException("Access denied");
        }
        long registeredCount = registrationRepository.countByEventIdAndStatusAndIsDeletedFalse(eventId, "REGISTERED");
        List<EventRegistration> allRegs = registrationRepository.findByEventIdAndIsDeletedFalse(eventId);
        long joinedCount = allRegs.stream().filter(EventRegistration::getAttended).count();
        double attendanceRate = registeredCount > 0 ? (double) joinedCount / registeredCount * 100 : 0;

        List<EventMaterial> materials = materialRepository.findByEventIdAndIsDeletedFalseOrderBySortOrderAsc(eventId);
        List<Map<String, Object>> materialList = materials.stream().map(m -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id", m.getId().toString());
            map.put("name", m.getTitle());
            map.put("url", m.getFileUrl());
            map.put("type", m.getMaterialType());
            return map;
        }).collect(Collectors.toList());

        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("id", event.getId().toString());
        summary.put("title", event.getTitle());
        summary.put("eventType", event.getEventType());
        summary.put("status", event.getStatus());
        summary.put("startDate", event.getStartsAt());
        summary.put("durationMinutes", event.getDurationMinutes());
        summary.put("timezone", event.getTimezone());
        summary.put("presenterName", event.getPresenterName());
        summary.put("recordingUrl", event.getRecordingUrl());
        summary.put("maxCapacity", event.getMaxParticipants());
        summary.put("currentRegistrations", (int) registeredCount);
        summary.put("joinedCount", (int) joinedCount);
        summary.put("attendanceRate", Math.round(attendanceRate));
        summary.put("participation", Math.round(attendanceRate));
        summary.put("materials", materialList);
        return summary;
    }

    private EventResponse mapToResponse(Event event) {
        long registeredCount = registrationRepository.countByEventIdAndStatusAndIsDeletedFalse(event.getId(), "REGISTERED");
        int materialCount = (int) materialRepository.countByEventIdAndIsDeletedFalse(event.getId());
        boolean hasRecording = materialRepository.findByEventIdAndMaterialTypeAndIsDeletedFalseOrderBySortOrderAsc(event.getId(), "RECORDING").size() > 0
                || materialRepository.findByEventIdAndMaterialTypeAndIsDeletedFalseOrderBySortOrderAsc(event.getId(), "VIDEO").size() > 0;

        Integer availableSpots = null;
        if (event.getMaxParticipants() != null) {
            availableSpots = event.getMaxParticipants() - (int) registeredCount;
        }

        String organizerName = null;
        if (event.getOrganizerId() != null) {
            organizerName = userRepository.findById(event.getOrganizerId())
                    .map(User::getFullName)
                    .orElse("Unknown");
        }

        return EventResponse.builder()
                .id(event.getId())
                .institutionId(event.getInstitutionId())
                .organizerId(event.getOrganizerId())
                .organizerName(organizerName)
                .title(event.getTitle())
                .description(event.getDescription())
                .eventType(event.getEventType())
                .category(event.getCategory())
                .location(event.getLocation())
                .meetingUrl(event.getMeetingUrl())
                .startsAt(event.getStartsAt())
                .endsAt(event.getEndsAt())
                .durationMinutes(event.getDurationMinutes())
                .maxParticipants(event.getMaxParticipants())
                .registeredCount((int) registeredCount)
                .availableSpots(availableSpots)
                .status(event.getStatus())
                .thumbnailUrl(event.getThumbnailUrl())
                .tags(event.getTags())
                .isFree(event.getIsFree())
                .requiresApproval(event.getRequiresApproval())
                .materialCount(materialCount)
                .hasRecording(hasRecording)
                .createdAt(event.getCreatedAt())
                .eventFormat(event.getEventFormat())
                .difficulty(event.getDifficulty())
                .targetAudience(event.getTargetAudience())
                .prerequisites(event.getPrerequisites())
                .learningOutcomes(event.getLearningOutcomes())
                .agenda(event.getAgenda())
                .cancelledAt(event.getCancelledAt())
                .cancellationReason(event.getCancellationReason())
                .rescheduledFrom(event.getRescheduledFrom())
                .recordingUrl(event.getRecordingUrl())
                .recordingStatus(event.getRecordingStatus())
                .providerId(event.getProviderId())
                .presenterName(event.getPresenterName())
                .build();
    }

    private EventRegistrationResponse mapRegistrationToResponse(EventRegistration reg, Event event) {
        return EventRegistrationResponse.builder()
                .id(reg.getId())
                .eventId(reg.getEventId())
                .eventTitle(event != null ? event.getTitle() : "Unknown Event")
                .eventStartsAt(event != null ? event.getStartsAt() : null)
                .eventEndsAt(event != null ? event.getEndsAt() : null)
                .eventLocation(event != null ? event.getLocation() : null)
                .eventMeetingUrl(event != null ? event.getMeetingUrl() : null)
                .eventType(event != null ? event.getEventType() : null)
                .status(reg.getStatus())
                .registeredAt(reg.getRegisteredAt())
                .cancelledAt(reg.getCancelledAt())
                .attended(reg.getAttended())
                .build();
    }

    private EventMaterialResponse mapMaterialToResponse(EventMaterial material) {
        return EventMaterialResponse.builder()
                .id(material.getId())
                .eventId(material.getEventId())
                .title(material.getTitle())
                .description(material.getDescription())
                .materialType(material.getMaterialType())
                .fileUrl(material.getFileUrl())
                .fileSize(material.getFileSize())
                .durationMinutes(material.getDurationMinutes())
                .sortOrder(material.getSortOrder())
                .isPublic(material.getIsPublic())
                .build();
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            if (dateTimeStr.contains("T")) {
                String cleaned = dateTimeStr.replaceAll("\\.[0-9]{3}Z?$", "").replace("Z", "").trim();
                return LocalDateTime.parse(cleaned, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            return LocalDateTime.parse(dateTimeStr + "T00:00:00", DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date/time format: " + dateTimeStr);
        }
    }
}
