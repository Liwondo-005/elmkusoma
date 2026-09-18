package tz.elmkusoma.parent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tz.elmkusoma.assessment.domain.Assessment;
import tz.elmkusoma.assessment.repository.AssessmentRepository;
import tz.elmkusoma.course.domain.LiveClass;
import tz.elmkusoma.course.repository.LiveClassRepository;
import tz.elmkusoma.event.domain.Event;
import tz.elmkusoma.event.repository.EventRepository;
import tz.elmkusoma.learning.domain.Assignment;
import tz.elmkusoma.learning.repository.AssignmentRepository;
import tz.elmkusoma.parent.dto.ParentCalendarResponse;
import tz.elmkusoma.parent.dto.ParentCalendarResponse.CalendarEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentCalendarService {

    private final LiveClassRepository liveClassRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final EventRepository eventRepository;

    public ParentCalendarResponse getCalendar(UUID childId, UUID institutionId, int daysAhead) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = now.plusDays(daysAhead > 0 ? daysAhead : 30);

        List<CalendarEvent> events = new ArrayList<>();

        List<LiveClass> liveClasses = liveClassRepository
                .findByInstitutionIdAndIsDeletedFalse(institutionId)
                .stream()
                .filter(lc -> lc.getScheduledAt() != null
                        && lc.getScheduledAt().isAfter(now)
                        && lc.getScheduledAt().isBefore(endDate)
                        && !"CANCELLED".equals(lc.getStatus()))
                .collect(Collectors.toList());

        for (LiveClass lc : liveClasses) {
            events.add(CalendarEvent.builder()
                    .id(lc.getId().toString())
                    .type("LIVE_CLASS")
                    .title(lc.getTitle())
                    .description(lc.getDescription())
                    .start(lc.getScheduledAt())
                    .end(lc.getScheduledAt().plusMinutes(lc.getDurationMinutes() != null ? lc.getDurationMinutes() : 60))
                    .status(lc.getStatus())
                    .relatedEntityType("live_class")
                    .relatedEntityId(lc.getId().toString())
                    .isActionRequired(true)
                    .build());
        }

        List<Assignment> assignments = assignmentRepository
                .findByClassGroupIdAndIsDeletedFalse(null)
                .stream()
                .filter(a -> a.getDueDate() != null
                        && a.getDueDate().isAfter(now)
                        && a.getDueDate().isBefore(endDate))
                .collect(Collectors.toList());

        for (Assignment a : assignments) {
            events.add(CalendarEvent.builder()
                    .id(a.getId().toString())
                    .type("ASSIGNMENT")
                    .title(a.getTitle() + " Due")
                    .description("Assignment due date")
                    .start(a.getDueDate())
                    .end(a.getDueDate())
                    .status("PENDING")
                    .relatedEntityType("assignment")
                    .relatedEntityId(a.getId().toString())
                    .isActionRequired(true)
                    .build());
        }

        List<Assessment> assessments = assessmentRepository
                .findAll()
                .stream()
                .filter(a -> a.getCreatedAt() != null
                        && a.getCreatedAt().isAfter(now)
                        && a.getCreatedAt().isBefore(endDate))
                .collect(Collectors.toList());

        for (Assessment a : assessments) {
            events.add(CalendarEvent.builder()
                    .id(a.getId().toString())
                    .type("ASSESSMENT")
                    .title(a.getTitle())
                    .description("Assessment")
                    .start(a.getCreatedAt())
                    .end(a.getCreatedAt())
                    .status("UPCOMING")
                    .relatedEntityType("assessment")
                    .relatedEntityId(a.getId().toString())
                    .isActionRequired(false)
                    .build());
        }

        List<Event> schoolEvents = eventRepository
                .findAll()
                .stream()
                .filter(e -> !Boolean.TRUE.equals(e.getIsDeleted())
                        && e.getStartsAt() != null
                        && e.getStartsAt().isAfter(now)
                        && e.getStartsAt().isBefore(endDate))
                .collect(Collectors.toList());

        for (Event e : schoolEvents) {
            events.add(CalendarEvent.builder()
                    .id(e.getId().toString())
                    .type("SCHOOL_EVENT")
                    .title(e.getTitle())
                    .description(e.getDescription())
                    .start(e.getStartsAt())
                    .end(e.getEndsAt())
                    .status(e.getStatus())
                    .relatedEntityType("event")
                    .relatedEntityId(e.getId().toString())
                    .isActionRequired(false)
                    .build());
        }

        events.sort(Comparator.comparing(CalendarEvent::getStart));

        return ParentCalendarResponse.builder()
                .events(events)
                .build();
    }
}
