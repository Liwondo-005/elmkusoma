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

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParentSupportService {

    private final SupportTicketRepository ticketRepository;
    private final SupportTicketMessageRepository messageRepository;

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

    @Transactional
    public SupportTicketItem createTicket(UUID userId, UUID institutionId, String subject,
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
        return toTicketItem(ticket);
    }

    @Transactional
    public SupportTicketMessage addMessage(UUID ticketId, UUID senderId, String message) {
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
        return messageRepository.findByTicketIdAndIsDeletedFalseOrderByCreatedAtAsc(ticketId);
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
        long msgCount = messageRepository.countByTicketIdAndIsDeletedFalse(t.getId());
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
