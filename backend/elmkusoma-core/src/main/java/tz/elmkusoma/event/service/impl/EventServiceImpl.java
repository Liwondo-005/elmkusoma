package tz.elmkusoma.event.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.certificate.domain.Certificate;
import tz.elmkusoma.certificate.domain.Certificate.CertificateStatus;
import tz.elmkusoma.certificate.domain.Certificate.CertificateType;
import tz.elmkusoma.certificate.domain.CertificateTemplate;
import tz.elmkusoma.certificate.repository.CertificateRepository;
import tz.elmkusoma.certificate.repository.CertificateTemplateRepository;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.domain.EventMaterial;
import tz.elmkusoma.event.domain.EventRegistration;
import tz.elmkusoma.event.domain.EventStatus;
import tz.elmkusoma.event.dto.*;
import tz.elmkusoma.event.repository.EventMaterialRepository;
import tz.elmkusoma.event.repository.EventRegistrationRepository;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.event.service.EventService;
import tz.elmkusoma.exception.ForbiddenException;
import tz.elmkusoma.exception.ResourceNotFoundException;
import tz.elmkusoma.learner.domain.LearnerNotification;
import tz.elmkusoma.learner.repository.LearnerNotificationRepository;
import tz.elmkusoma.shared.domain.User;
import tz.elmkusoma.shared.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    private static final Set<EventStatus> LEARNER_VISIBLE_STATUSES = EnumSet.of(
            EventStatus.PUBLISHED, EventStatus.REGISTRATION_OPEN, EventStatus.REGISTRATION_CLOSED,
            EventStatus.PREPARING, EventStatus.STARTING, EventStatus.LIVE, EventStatus.ENDING,
            EventStatus.ENDED, EventStatus.RECORDING, EventStatus.PROCESSING,
            EventStatus.REPLAY_AVAILABLE, EventStatus.FULL);

    private static final Set<EventStatus> REGISTRATION_OPEN_STATUSES = EnumSet.of(
            EventStatus.PUBLISHED, EventStatus.REGISTRATION_OPEN);

    private final EventRepository eventRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventMaterialRepository materialRepository;
    private final UserRepository userRepository;
    private final tz.elmkusoma.shared.repository.InstitutionRepository institutionRepository;
    private final CertificateRepository certificateRepository;
    private final CertificateTemplateRepository certificateTemplateRepository;
    private final LearnerNotificationRepository notificationRepository;

    public EventServiceImpl(EventRepository eventRepository,
                            EventRegistrationRepository registrationRepository,
                            EventMaterialRepository materialRepository,
                            UserRepository userRepository,
                            tz.elmkusoma.shared.repository.InstitutionRepository institutionRepository,
                            CertificateRepository certificateRepository,
                            CertificateTemplateRepository certificateTemplateRepository,
                            LearnerNotificationRepository notificationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.materialRepository = materialRepository;
        this.userRepository = userRepository;
        this.institutionRepository = institutionRepository;
        this.certificateRepository = certificateRepository;
        this.certificateTemplateRepository = certificateTemplateRepository;
        this.notificationRepository = notificationRepository;
    }

    // ==================== Status helpers ====================

    private Event requireEvent(UUID eventId) {
        return eventRepository.findById(eventId)
                .filter(event -> !Boolean.TRUE.equals(event.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));
    }

    private EventStatus currentStatus(Event event) {
        if (event.getEventStatus() != null) {
            return event.getEventStatus();
        }
        if (event.getStatus() != null && !event.getStatus().isBlank()) {
            try {
                return EventStatus.fromString(event.getStatus());
            } catch (IllegalArgumentException ex) {
                return EventStatus.DRAFT;
            }
        }
        return EventStatus.DRAFT;
    }

    private void applyStatus(Event event, EventStatus status) {
        event.setEventStatus(status);
        event.setStatus(status.name());
    }

    private void changeStatus(Event event, EventStatus next) {
        EventStatus current = currentStatus(event);
        current.assertCanTransitionTo(next);
        applyStatus(event, next);
    }

    private void changeStatus(Event event, String rawStatus) {
        changeStatus(event, EventStatus.fromString(rawStatus));
    }

    // ==================== Access level helpers ====================

    private String accessLevelOf(Event event) {
        if (event.getAccessLevel() == null || event.getAccessLevel().isBlank()) {
            return "INSTITUTION";
        }
        return event.getAccessLevel().trim().toUpperCase();
    }

    private boolean isPublicAccess(String level) {
        return "PUBLIC".equals(level) || "AUTHENTICATED".equals(level);
    }

    private boolean isPrivateAccess(String level) {
        return "PRIVATE".equals(level) || "INVITED".equals(level) || "RESTRICTED".equals(level);
    }

    private boolean isPlatformAdmin(User.Role role) {
        return role == User.Role.ADMIN || role == User.Role.NATIONAL_ADMIN;
    }

    private boolean sameInstitution(User user, Event event) {
        return event.getInstitutionId() == null
                || user.getInstitutionId() == null
                || event.getInstitutionId().equals(user.getInstitutionId());
    }

    private void assertCanViewEvent(Event event, UUID currentUserId) {
        String level = accessLevelOf(event);
        if (isPublicAccess(level)) {
            return;
        }
        if (currentUserId == null) {
            if (isPrivateAccess(level)) {
                throw new ForbiddenException("This event is private to its organizer");
            }
            return;
        }
        if (currentUserId.equals(event.getOrganizerId())) {
            return;
        }
        User user = userRepository.findById(currentUserId).orElse(null);
        if (user == null) {
            return;
        }
        if (isPlatformAdmin(user.getRole())) {
            return;
        }
        if (isPrivateAccess(level)) {
            if (user.getRole() == User.Role.INSTITUTION_ADMIN && sameInstitution(user, event)) {
                return;
            }
            throw new ForbiddenException("This event is only visible to its organizer and administrators");
        }
        if (!sameInstitution(user, event)) {
            throw new ForbiddenException("This event is not available in your institution");
        }
    }

    private void assertCanRegister(Event event, User user) {
        String level = accessLevelOf(event);
        if (isPublicAccess(level)) {
            return;
        }
        if (isPrivateAccess(level)) {
            if (user.getId() != null && user.getId().equals(event.getOrganizerId())) {
                return;
            }
            if (isPlatformAdmin(user.getRole())
                    || (user.getRole() == User.Role.INSTITUTION_ADMIN && sameInstitution(user, event))) {
                return;
            }
            throw new ForbiddenException("Registration for this event is restricted to its organizer");
        }
        if (isPlatformAdmin(user.getRole())) {
            return;
        }
        if (event.getInstitutionId() != null && user.getInstitutionId() != null
                && !event.getInstitutionId().equals(user.getInstitutionId())) {
            throw new IllegalArgumentException("This event is not available in your institution");
        }
    }

    // ==================== Queries ====================

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
        Event event = requireEvent(eventId);
        EventStatus status = currentStatus(event);
        boolean registered = currentUserId != null
                && registrationRepository.existsByEventIdAndUserIdAndIsDeletedFalse(eventId, currentUserId);

        if (!LEARNER_VISIBLE_STATUSES.contains(status)) {
            boolean cancelledForRegistered = registered
                    && (status == EventStatus.CANCELLED || status == EventStatus.RESCHEDULED);
            if (!cancelledForRegistered) {
                throw new ResourceNotFoundException("Event", "id", eventId);
            }
            return buildLearnerResponse(event, currentUserId, registered);
        }

        assertCanViewEvent(event, currentUserId);
        return buildLearnerResponse(event, currentUserId, registered);
    }

    private EventResponse buildLearnerResponse(Event event, UUID currentUserId, boolean registered) {
        EventResponse response = mapToResponse(event);
        if (currentUserId != null) {
            response.setIsRegistered(registered);
            if (registered) {
                registrationRepository.findByEventIdAndUserIdAndIsDeletedFalse(event.getId(), currentUserId)
                        .ifPresent(reg -> response.setRegistrationStatus(reg.getStatus()));
            }
        }
        return response;
    }

    @Override
    public EventResponse getEventByIdForAdmin(UUID eventId) {
        return mapToResponse(requireEvent(eventId));
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

    // ==================== CRUD ====================

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
                .thumbnailUrl(request.getThumbnailUrl())
                .tags(request.getTags())
                .isFree(request.getIsFree() != null ? request.getIsFree() : true)
                .requiresApproval(request.getRequiresApproval() != null ? request.getRequiresApproval() : false)
                .timezone(request.getTimezone())
                .accessLevel(request.getAccessLevel())
                .presenterName(request.getPresenterName())
                .eventFormat(request.getEventFormat())
                .difficulty(request.getDifficulty())
                .targetAudience(request.getTargetAudience())
                .prerequisites(request.getPrerequisites())
                .learningOutcomes(request.getLearningOutcomes())
                .agenda(request.getAgenda())
                .rescheduledFrom(request.getRescheduledFrom())
                .build();

        applyStatus(event, request.getStatus() != null
                ? EventStatus.fromString(request.getStatus())
                : EventStatus.DRAFT);

        if (event.getEndsAt() == null && event.getDurationMinutes() != null) {
            event.setEndsAt(event.getStartsAt().plusMinutes(event.getDurationMinutes()));
        }

        event = eventRepository.save(event);
        log.info("Event created: {} by institution {}", event.getId(), institutionId);
        return mapToResponse(event);
    }

    @Override
    public EventResponse updateEvent(UUID eventId, EventRequest request) {
        Event event = requireEvent(eventId);

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
        if (request.getThumbnailUrl() != null) event.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getTags() != null) event.setTags(request.getTags());
        if (request.getIsFree() != null) event.setIsFree(request.getIsFree());
        if (request.getRequiresApproval() != null) event.setRequiresApproval(request.getRequiresApproval());
        if (request.getTimezone() != null) event.setTimezone(request.getTimezone());
        if (request.getAccessLevel() != null) event.setAccessLevel(request.getAccessLevel());
        if (request.getPresenterName() != null) event.setPresenterName(request.getPresenterName());
        if (request.getEventFormat() != null) event.setEventFormat(request.getEventFormat());
        if (request.getDifficulty() != null) event.setDifficulty(request.getDifficulty());
        if (request.getTargetAudience() != null) event.setTargetAudience(request.getTargetAudience());
        if (request.getPrerequisites() != null) event.setPrerequisites(request.getPrerequisites());
        if (request.getLearningOutcomes() != null) event.setLearningOutcomes(request.getLearningOutcomes());
        if (request.getAgenda() != null) event.setAgenda(request.getAgenda());
        if (request.getRescheduledFrom() != null) event.setRescheduledFrom(request.getRescheduledFrom());
        if (request.getStatus() != null) {
            changeStatus(event, request.getStatus());
        }

        event = eventRepository.save(event);
        log.info("Event updated: id={}, institutionId={}", event.getId(), event.getInstitutionId());
        if (request.getStatus() != null && !request.getStatus().equals(oldStatus)) {
            log.info("Event status changed: id={}, oldStatus={}, newStatus={}", eventId, oldStatus, event.getStatus());
        }
        return mapToResponse(event);
    }

    @Override
    public void deleteEvent(UUID eventId) {
        Event event = requireEvent(eventId);
        UUID institutionId = event.getInstitutionId();
        event.setIsDeleted(true);
        eventRepository.save(event);
        log.info("Event deleted: id={}, institutionId={}", eventId, institutionId);
    }

    // ==================== Registrations ====================

    @Override
    public EventRegistrationResponse registerForEvent(UUID eventId, UUID userId) {
        Event event = requireEvent(eventId);

        if (!REGISTRATION_OPEN_STATUSES.contains(currentStatus(event))) {
            throw new IllegalArgumentException("This event is not available for registration");
        }

        if (event.getStartsAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot register for a past event");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        assertCanRegister(event, user);

        java.util.Optional<EventRegistration> existing =
                registrationRepository.findByEventIdAndUserIdAndIsDeletedFalse(eventId, userId);
        if (existing.isPresent()) {
            EventRegistration reg = existing.get();
            if ("REGISTERED".equals(reg.getStatus()) || "WAITLISTED".equals(reg.getStatus())) {
                log.info("User {} already registered for event {} - returning existing registration",
                        userId, eventId);
                return mapRegistrationToResponse(reg, event);
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
            return mapRegistrationToResponse(reg, event);
        }

        if (event.getMaxParticipants() != null) {
            long registeredCount = registrationRepository.countByEventIdAndStatusAndIsDeletedFalse(eventId, "REGISTERED");
            if (registeredCount >= event.getMaxParticipants()) {
                throw new IllegalArgumentException("This event is full");
            }
        }

        EventRegistration registration = EventRegistration.builder()
                .eventId(eventId)
                .institutionId(event.getInstitutionId())
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
        EventRegistration registration = registrationRepository
                .findByEventIdAndUserIdAndIsDeletedFalse(eventId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration", "event", eventId));

        if ("CANCELLED".equals(registration.getStatus())) {
            log.info("Registration already cancelled for user {} on event {}", userId, eventId);
            return;
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
    public EventRegistration markEventAttendance(UUID eventId, UUID userId) {
        requireEvent(eventId);
        EventRegistration registration = registrationRepository
                .findByEventIdAndUserIdAndIsDeletedFalse(eventId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Registration", "user", userId + " for event " + eventId));
        if (!Boolean.TRUE.equals(registration.getAttended())) {
            registration.setAttended(true);
            registration.setAttendedAt(LocalDateTime.now());
            registration = registrationRepository.save(registration);
            log.info("Attendance marked: event={}, user={}", eventId, userId);
        }
        return registration;
    }

    @Override
    public int markEventAttendanceBulk(UUID eventId, Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return 0;
        }
        int marked = 0;
        for (UUID userId : userIds) {
            if (userId == null) {
                continue;
            }
            try {
                markEventAttendance(eventId, userId);
                marked++;
            } catch (ResourceNotFoundException ex) {
                log.warn("Attendance mark skipped - no registration for user {} on event {}", userId, eventId);
            }
        }
        return marked;
    }

    // ==================== Materials ====================

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
                .filter(m -> !Boolean.TRUE.equals(m.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Material", "id", materialId));
        material.setIsDeleted(true);
        materialRepository.save(material);
    }

    // ==================== Registered event lists ====================

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

    // ==================== Lifecycle ====================

    @Override
    public EventResponse publishEvent(UUID eventId, UUID institutionId) {
        Event event = requireEvent(eventId);
        if (institutionId != null && !institutionId.equals(event.getInstitutionId())) {
            throw new ForbiddenException("Access denied");
        }
        changeStatus(event, EventStatus.PUBLISHED);
        event = eventRepository.save(event);
        log.info("Event published: id={}", eventId);
        return mapToResponse(event);
    }

    @Override
    public EventResponse cancelEvent(UUID eventId, UUID institutionId, String reason) {
        Event event = requireEvent(eventId);
        if (institutionId != null && !institutionId.equals(event.getInstitutionId())) {
            throw new ForbiddenException("Access denied");
        }
        changeStatus(event, EventStatus.CANCELLED);
        event.setCancelledAt(LocalDateTime.now());
        event.setCancellationReason(reason);
        event = eventRepository.save(event);
        log.info("Event cancelled: id={}", eventId);
        return mapToResponse(event);
    }

    @Override
    public EventResponse startLiveEvent(UUID eventId, UUID institutionId) {
        Event event = requireEvent(eventId);
        if (institutionId != null && !institutionId.equals(event.getInstitutionId())) {
            throw new ForbiddenException("Access denied");
        }
        changeStatus(event, EventStatus.LIVE);
        event = eventRepository.save(event);
        log.info("Event started live: id={}", eventId);
        return mapToResponse(event);
    }

    @Override
    public EventResponse endLiveEvent(UUID eventId, UUID institutionId) {
        Event event = requireEvent(eventId);
        if (institutionId != null && !institutionId.equals(event.getInstitutionId())) {
            throw new ForbiddenException("Access denied");
        }
        changeStatus(event, EventStatus.ENDED);
        event = eventRepository.save(event);
        log.info("Event ended: id={}", eventId);

        issueCertificatesForEventAttendees(event);

        return mapToResponse(event);
    }

    private void issueCertificatesForEventAttendees(Event event) {
        try {
            List<EventRegistration> attendedRegistrations = registrationRepository.findByEventIdAndIsDeletedFalse(event.getId())
                    .stream()
                    .filter(reg -> Boolean.TRUE.equals(reg.getAttended()))
                    .toList();

            if (attendedRegistrations.isEmpty()) {
                log.info("No attendees to issue certificates for event: {}", event.getId());
                return;
            }

            List<CertificateTemplate> templates = certificateTemplateRepository.findByInstitutionIdAndTemplateType(
                    event.getInstitutionId(), CertificateTemplate.TemplateType.PARTICIPATION);
            CertificateTemplate template = templates.isEmpty()
                    ? createDefaultParticipationTemplate(event.getInstitutionId())
                    : templates.get(0);

            int issuedCount = 0;
            for (EventRegistration registration : attendedRegistrations) {
                if (certificateRepository.findAllByStudentId(registration.getUserId()).stream()
                        .anyMatch(c -> c.getMetadata() != null &&
                                event.getId().toString().equals(c.getMetadata().get("eventId")))) {
                    continue;
                }

                User user = userRepository.findById(registration.getUserId()).orElse(null);
                if (user == null) continue;

                String serialNumber = "CERT-EVT-" + System.currentTimeMillis() + "-" + registration.getUserId().toString().substring(0, 8);
                String verificationCode = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();

                Certificate certificate = Certificate.builder()
                        .templateId(template.getId())
                        .studentId(registration.getUserId())
                        .issuedBy(event.getOrganizerId())
                        .serialNumber(serialNumber)
                        .certificateType(CertificateType.PARTICIPATION)
                        .title("Certificate of Participation - " + event.getTitle())
                        .description("Awarded for attending " + event.getTitle())
                        .studentName(user.getFullName())
                        .courseOrProgramme(event.getTitle())
                        .completionDate(java.time.LocalDate.now())
                        .issueDate(LocalDateTime.now())
                        .status(CertificateStatus.ISSUED)
                        .verificationCode(verificationCode)
                        .instructorName(event.getPresenterName())
                        .metadata(buildCertificateMetadata(event, registration))
                        .build();
                certificate.setInstitutionId(event.getInstitutionId());

                certificateRepository.save(certificate);
                registration.setCertificateId(certificate.getId());
                registrationRepository.save(registration);
                issuedCount++;

                LearnerNotification notification = LearnerNotification.builder()
                        .userId(registration.getUserId())
                        .institutionId(event.getInstitutionId())
                        .title("Certificate Earned!")
                        .message("You've earned a certificate for attending " + event.getTitle()
                                + ". Verification code: " + verificationCode)
                        .notificationType("CERTIFICATE")
                        .targetType("certificate")
                        .targetId(certificate.getId())
                        .build();
                notificationRepository.save(notification);
            }
            log.info("Issued {} participation certificates for event: {}", issuedCount, event.getId());
        } catch (Exception ex) {
            log.warn("Failed to issue certificates for event {}: {}", event.getId(), ex.getMessage());
        }
    }

    private Map<String, Object> buildCertificateMetadata(Event event, EventRegistration registration) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("eventId", event.getId().toString());
        metadata.put("eventTitle", event.getTitle());
        metadata.put("eventType", event.getEventType());
        metadata.put("registrationId", registration.getId().toString());
        metadata.put("attendedAt", registration.getAttendedAt() != null
                ? registration.getAttendedAt().toString()
                : "");
        return metadata;
    }

    private CertificateTemplate createDefaultParticipationTemplate(UUID institutionId) {
        CertificateTemplate template = CertificateTemplate.builder()
                .institutionId(institutionId)
                .name("Event Participation Certificate")
                .description("Default template for event participation certificates")
                .templateType(CertificateTemplate.TemplateType.PARTICIPATION)
                .isActive(true)
                .build();
        return certificateTemplateRepository.save(template);
    }

    // ==================== Summary ====================

    @Override
    public Map<String, Object> getEventSummary(UUID eventId, UUID institutionId) {
        Event event = requireEvent(eventId);
        if (institutionId != null && !institutionId.equals(event.getInstitutionId())) {
            throw new ForbiddenException("Access denied");
        }

        List<EventRegistration> allRegs = registrationRepository.findByEventIdAndIsDeletedFalse(eventId);
        long registeredCount = allRegs.stream()
                .filter(reg -> !"CANCELLED".equals(reg.getStatus()))
                .count();
        long joinedCount = allRegs.stream()
                .filter(reg -> Boolean.TRUE.equals(reg.getAttended()))
                .count();
        double attendanceRate = registeredCount > 0 ? (double) joinedCount / registeredCount * 100 : 0;

        List<EventMaterial> materials = materialRepository.findByEventIdAndIsDeletedFalseOrderBySortOrderAsc(eventId);
        List<Map<String, Object>> materialList = materials.stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", m.getId().toString());
            map.put("name", m.getTitle());
            map.put("url", m.getFileUrl());
            map.put("type", m.getMaterialType());
            return map;
        }).collect(Collectors.toList());

        List<Map<String, Object>> attendanceList = allRegs.stream()
                .filter(reg -> Boolean.TRUE.equals(reg.getAttended()))
                .map(reg -> buildAttendanceRow(event, reg))
                .collect(Collectors.toList());

        Map<String, Object> summary = new HashMap<>();
        summary.put("id", event.getId().toString());
        summary.put("title", event.getTitle());
        summary.put("eventType", event.getEventType());
        summary.put("status", event.getStatus());
        summary.put("eventStatus", currentStatus(event).name());
        summary.put("startDate", event.getStartsAt());
        summary.put("durationMinutes", event.getDurationMinutes());
        summary.put("timezone", event.getTimezone());
        summary.put("presenterName", event.getPresenterName());
        summary.put("recordingUrl", event.getRecordingUrl());
        summary.put("maxCapacity", event.getMaxParticipants());
        summary.put("currentRegistrations", (int) registeredCount);
        summary.put("registeredCount", (int) registeredCount);
        summary.put("joinedCount", (int) joinedCount);
        summary.put("attendanceRate", Math.round(attendanceRate));
        summary.put("participation", Math.round(attendanceRate));
        summary.put("materials", materialList);
        summary.put("attendance", attendanceList);

        return summary;
    }

    private Map<String, Object> buildAttendanceRow(Event event, EventRegistration reg) {
        Map<String, Object> att = new HashMap<>();
        User attendee = userRepository.findById(reg.getUserId()).orElse(null);
        att.put("userId", reg.getUserId().toString());
        att.put("userName", attendee != null && attendee.getFullName() != null
                ? attendee.getFullName()
                : "Unknown");
        att.put("email", attendee != null && attendee.getEmail() != null
                ? attendee.getEmail()
                : "");
        att.put("joinTime", reg.getAttendedAt() != null ? reg.getAttendedAt().toString() : null);
        att.put("leaveTime", event.getEndsAt() != null ? event.getEndsAt().toString() : null);
        Integer durationMinutes = null;
        if (reg.getAttendedAt() != null && event.getEndsAt() != null
                && event.getEndsAt().isAfter(reg.getAttendedAt())) {
            durationMinutes = (int) ChronoUnit.MINUTES.between(reg.getAttendedAt(), event.getEndsAt());
        }
        att.put("durationMinutes", durationMinutes);
        att.put("attendedAt", reg.getAttendedAt() != null ? reg.getAttendedAt().toString() : null);
        att.put("status", reg.getStatus());
        att.put("certificateId", reg.getCertificateId() != null
                ? reg.getCertificateId().toString()
                : null);
        return att;
    }

    // ==================== Mapping ====================

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
                .eventStatus(currentStatus(event).name())
                .accessLevel(event.getAccessLevel())
                .timezone(event.getTimezone())
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
