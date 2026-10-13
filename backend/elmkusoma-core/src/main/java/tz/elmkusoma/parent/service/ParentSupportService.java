package tz.elmkusoma.parent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.elmkusoma.parent.domain.SupportTicket;
import tz.elmkusoma.parent.domain.SupportTicketMessage;
import tz.elmkusoma.parent.dto.ParentSupportResponse;
import tz.elmkusoma.parent.dto.ParentSupportResponse.SupportTicketItem;
import tz.elmkusoma.parent.repository.SupportTicketMessageRepository;
import tz.elmkusoma.parent.repository.SupportTicketRepository;
import tz.elmkusoma.shared.service.CommunicationIdempotencyService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentSupportService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketMessageRepository messageRepository;
    private final CommunicationIdempotencyService idempotency;

    public ParentSupportResponse getTickets(UUID userId) {
        List<SupportTicket> tickets = ticketRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId);
        long openCount = ticketRepository.countByUserIdAndStatusAndIsDeletedFalse(userId, "OPEN");
        long resolvedCount = ticketRepository.countByUserIdAndStatusAndIsDeletedFalse(userId, "RESOLVED");

        return ParentSupportResponse.builder()
                .tickets(tickets.stream().map(this::toTicketItem).collect(Collectors.toList()))
                .openCount(openCount)
                .resolvedCount(resolvedCount)
                .build();
    }

    /**
     * Creates a ticket at most once per idempotency key.
     *
     * <p>Retrying must not file a second case. The key is scoped to this parent and this
     * operation, so two parents whose browsers mint the same UUID do not collide, and reusing a
     * key for a different subject is refused rather than silently returning the first ticket.</p>
     */
    @Transactional
    public SupportTicketItem createTicket(UUID userId, UUID institutionId, String subject,
                                          String description, String category, String priority,
                                          String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return createTicketRow(userId, institutionId, subject, description, category, priority);
        }
        return idempotency.execute(
                CommunicationIdempotencyService.actor(userId),
                CommunicationIdempotencyService.OP_TICKET_CREATE,
                idempotencyKey,
                CommunicationIdempotencyService.hashOf(subject, description, category, priority),
                // A replay re-reads the committed ticket rather than trusting a stored DTO, so
                // the response always reflects the row as it stands now.
                prior -> ticketRepository.findByIdAndIsDeletedFalse(prior.resourceId())
                        .map(this::toTicketItem)
                        .orElseThrow(() -> new IllegalStateException(
                                "Idempotency key matched a ticket that no longer exists")),
                () -> createTicketRow(userId, institutionId, subject, description, category, priority),
                item -> findTicketId(item)
        ).result();
    }

    /** Creates a ticket with no idempotency key. */
    @Transactional
    public SupportTicketItem createTicket(UUID userId, UUID institutionId, String subject,
                                          String description, String category, String priority) {
        return createTicket(userId, institutionId, subject, description, category, priority, null);
    }

    private SupportTicketItem createTicketRow(UUID userId, UUID institutionId, String subject,
                                              String description, String category, String priority) {
        SupportTicket created = persistTicket(userId, institutionId, subject,
                description, category, priority);
        return toTicketItem(created);
    }

    private SupportTicket persistTicket(UUID userId, UUID institutionId, String subject,
                                        String description, String category, String priority) {
        SupportTicket ticket = SupportTicket.builder()
                .userId(userId)
                .institutionId(institutionId)
                .subject(subject)
                .description(description)
                .category(category)
                .priority(priority != null ? priority : "NORMAL")
                .status("OPEN")
                .build();
        ticket = ticketRepository.save(ticket);

        SupportTicketMessage initialMessage = SupportTicketMessage.builder()
                .ticketId(ticket.getId())
                .senderId(userId)
                .message(description)
                .isInternal(false)
                .build();
        messageRepository.save(initialMessage);

        log.info("Support ticket created: {} by user {}", ticket.getId(), userId);
        return ticket;
    }

    /**
     * The ticket id behind a created item, so the idempotency ledger can replay it.
     *
     * <p>{@code SupportTicketItem} carries its id as a string, and {@code createTicket} has just
     * written the row, so re-reading it is a single indexed lookup rather than a guess.</p>
     */
    private UUID findTicketId(SupportTicketItem item) {
        return UUID.fromString(item.getId());
    }

    /**
     * Adds a message at most once per idempotency key.
     *
     * <p>The duplicate this prevents is a parent on a slow connection tapping "send" twice.
     * Without it the thread gains two identical messages and the admin answers the second.</p>
     */
    @Transactional
    public SupportTicketMessage addMessage(UUID ticketId, UUID senderId, String message, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return persistMessage(ticketId, senderId, message);
        }
        return idempotency.execute(
                CommunicationIdempotencyService.actor(senderId),
                CommunicationIdempotencyService.OP_TICKET_MESSAGE,
                idempotencyKey,
                CommunicationIdempotencyService.hashOf(ticketId.toString(), message),
                prior -> messageRepository.findById(prior.resourceId())
                        .orElseThrow(() -> new IllegalStateException(
                                "Idempotency key matched a message that no longer exists")),
                () -> persistMessage(ticketId, senderId, message),
                SupportTicketMessage::getId
        ).result();
    }

    /** Adds a message with no idempotency key. */
    @Transactional
    public SupportTicketMessage addMessage(UUID ticketId, UUID senderId, String message) {
        return addMessage(ticketId, senderId, message, null);
    }

    private SupportTicketMessage persistMessage(UUID ticketId, UUID senderId, String message) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        SupportTicketMessage msg = SupportTicketMessage.builder()
                .ticketId(ticketId)
                .senderId(senderId)
                .message(message)
                .isInternal(false)
                .build();
        return messageRepository.save(msg);
    }

    public List<SupportTicketMessage> getMessages(UUID ticketId) {
        // Excludes internal notes. This is the requester's view of their own ticket; staff
        // notes about them are not theirs to read. Previously every message was returned here.
        return messageRepository.findVisibleToRequester(ticketId);
    }

    @Transactional
    public SupportTicketItem resolveTicket(UUID ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));
        ticket.setStatus("RESOLVED");
        ticket.setResolvedAt(java.time.LocalDateTime.now());
        ticketRepository.save(ticket);
        return toTicketItem(ticket);
    }

    private SupportTicketItem toTicketItem(SupportTicket t) {
        long msgCount = messageRepository.countVisibleToRequester(t.getId());
        return SupportTicketItem.builder()
                .id(t.getId().toString())
                .subject(t.getSubject())
                .description(t.getDescription())
                .category(t.getCategory())
                .priority(t.getPriority())
                .status(t.getStatus())
                .createdAt(t.getCreatedAt())
                .resolvedAt(t.getResolvedAt())
                .messageCount((int) msgCount)
                .build();
    }
}
