package tz.elmkusoma.event.service;

import tz.elmkusoma.event.dto.*;

import java.util.List;
import java.util.UUID;

public interface EventService {

    List<EventResponse> getEvents(UUID institutionId, String status, String eventType, String category);

    List<EventResponse> searchEvents(UUID institutionId, String query);

    EventResponse getEventById(UUID eventId, UUID currentUserId);

    EventResponse getEventByIdForAdmin(UUID eventId);

    List<EventResponse> getUpcomingEvents(UUID institutionId);

    List<EventResponse> getPastEvents(UUID institutionId);

    EventResponse createEvent(UUID institutionId, UUID organizerId, EventRequest request);

    EventResponse updateEvent(UUID eventId, EventRequest request);

    void deleteEvent(UUID eventId);

    EventRegistrationResponse registerForEvent(UUID eventId, UUID userId);

    void cancelRegistration(UUID eventId, UUID userId, String reason);

    List<EventRegistrationResponse> getUserRegistrations(UUID userId);

    List<EventRegistrationResponse> getEventRegistrations(UUID eventId, UUID requesterId);

    boolean isUserRegistered(UUID eventId, UUID userId);

    List<EventMaterialResponse> getEventMaterials(UUID eventId);

    List<EventMaterialResponse> getPublicEventMaterials(UUID eventId);

    EventMaterialResponse addEventMaterial(EventMaterialRequest request, UUID uploadedBy);

    void deleteEventMaterial(UUID materialId);

    List<EventResponse> getRegisteredEvents(UUID userId, UUID institutionId);

    List<EventResponse> getRegisteredPastEvents(UUID userId, UUID institutionId);
}
