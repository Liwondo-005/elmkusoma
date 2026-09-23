package tz.elmkusoma.event.domain;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum EventStatus {
    DRAFT, REVIEW, PUBLISHED, REGISTRATION_OPEN, REGISTRATION_CLOSED,
    PREPARING, STARTING, LIVE, ENDING, ENDED,
    RECORDING, PROCESSING, REPLAY_AVAILABLE,
    CANCELLED, RESCHEDULED, FULL, FAILED;

    private static final Map<EventStatus, Set<EventStatus>> TRANSITIONS = buildTransitions();

    private static Map<EventStatus, Set<EventStatus>> buildTransitions() {
        Map<EventStatus, Set<EventStatus>> map = new EnumMap<>(EventStatus.class);
        map.put(DRAFT, EnumSet.of(REVIEW, PUBLISHED, CANCELLED, RESCHEDULED));
        map.put(REVIEW, EnumSet.of(DRAFT, PUBLISHED, CANCELLED, RESCHEDULED));
        map.put(PUBLISHED, EnumSet.of(DRAFT, REGISTRATION_OPEN, REGISTRATION_CLOSED,
                CANCELLED, RESCHEDULED, LIVE));
        map.put(REGISTRATION_OPEN, EnumSet.of(PUBLISHED, REGISTRATION_CLOSED, FULL,
                PREPARING, CANCELLED, RESCHEDULED, LIVE));
        map.put(REGISTRATION_CLOSED, EnumSet.of(REGISTRATION_OPEN, PREPARING,
                CANCELLED, RESCHEDULED, LIVE));
        map.put(PREPARING, EnumSet.of(STARTING, LIVE, CANCELLED, FAILED));
        map.put(STARTING, EnumSet.of(LIVE, ENDED, CANCELLED, FAILED));
        map.put(LIVE, EnumSet.of(ENDING, ENDED, CANCELLED, FAILED));
        map.put(ENDING, EnumSet.of(ENDED, FAILED));
        map.put(ENDED, EnumSet.of(RECORDING, PROCESSING, FAILED));
        map.put(RECORDING, EnumSet.of(PROCESSING, ENDED, FAILED));
        map.put(PROCESSING, EnumSet.of(REPLAY_AVAILABLE, ENDED, FAILED));
        map.put(REPLAY_AVAILABLE, EnumSet.of(PROCESSING, ENDED));
        map.put(FULL, EnumSet.of(REGISTRATION_OPEN, REGISTRATION_CLOSED, PREPARING,
                CANCELLED, RESCHEDULED));
        map.put(CANCELLED, EnumSet.noneOf(EventStatus.class));
        map.put(RESCHEDULED, EnumSet.noneOf(EventStatus.class));
        map.put(FAILED, EnumSet.of(RECORDING, PROCESSING, CANCELLED, ENDED));
        return Collections.unmodifiableMap(map);
    }

    public boolean canTransitionTo(EventStatus next) {
        if (next == null) {
            return false;
        }
        if (next == this) {
            return true;
        }
        Set<EventStatus> allowed = TRANSITIONS.getOrDefault(this, EnumSet.noneOf(EventStatus.class));
        return allowed.contains(next);
    }

    public void assertCanTransitionTo(EventStatus next) {
        if (!canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Invalid event status transition: " + this + " -> " + next);
        }
    }

    public static EventStatus fromString(String status) {
        if (status == null || status.isBlank()) {
            return DRAFT;
        }
        String normalized = status.trim().toUpperCase().replace(' ', '_');
        if ("COMPLETED".equals(normalized)) {
            return ENDED;
        }
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid event status: " + status);
        }
    }
}
