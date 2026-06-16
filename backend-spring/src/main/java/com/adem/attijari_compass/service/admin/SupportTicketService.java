package com.adem.attijari_compass.service.admin;

import com.adem.attijari_compass.dto.admin.SupportTicketCreateRequest;
import com.adem.attijari_compass.dto.admin.SupportTicketDto;
import com.adem.attijari_compass.entity.AuditStatus;
import com.adem.attijari_compass.entity.SupportTicket;
import com.adem.attijari_compass.entity.SupportTicketStatus;
import com.adem.attijari_compass.entity.User;
import com.adem.attijari_compass.exception.ResourceNotFoundException;
import com.adem.attijari_compass.repository.SupportTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SupportTicketService {
    private final SupportTicketRepository supportTicketRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public SupportTicketDto create(SupportTicketCreateRequest request, User user) {
        SupportTicket ticket = SupportTicket.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .subject(request.subject())
                .category(request.category())
                .message(request.message())
                .status(SupportTicketStatus.NEW)
                .build();
        return toDto(supportTicketRepository.save(ticket));
    }

    public List<SupportTicketDto> mine(User user) {
        return supportTicketRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId()).stream().map(this::toDto).toList();
    }

    public List<SupportTicketDto> findAll() {
        return supportTicketRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    public SupportTicketDto findById(Long id) {
        return toDto(getTicket(id));
    }

    @Transactional
    public SupportTicketDto updateStatus(Long id, SupportTicketStatus status, User actor) {
        SupportTicket ticket = getTicket(id);
        ticket.setStatus(status);
        if (status == SupportTicketStatus.RESOLVED || status == SupportTicketStatus.CLOSED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        auditLogService.log(actor, "SUPPORT_TICKET_STATUS_CHANGED", "SUPPORT", AuditStatus.SUCCESS,
                "Statut ticket support modifie: ticketId=" + id + ", status=" + status.name());
        return toDto(ticket);
    }

    @Transactional
    public SupportTicketDto reply(Long id, String reply, User actor) {
        SupportTicket ticket = getTicket(id);
        ticket.setAdminReply(reply);
        ticket.setStatus(SupportTicketStatus.IN_PROGRESS);
        auditLogService.log(actor, "SUPPORT_TICKET_REPLIED", "SUPPORT", AuditStatus.SUCCESS,
                "Reponse admin ajoutee au ticketId=" + id);
        return toDto(ticket);
    }

    private SupportTicket getTicket(Long id) {
        return supportTicketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Support ticket not found with id: " + id));
    }

    private SupportTicketDto toDto(SupportTicket ticket) {
        return new SupportTicketDto(
                ticket.getId(), ticket.getUserId(), ticket.getUserEmail(), ticket.getSubject(),
                ticket.getCategory(), ticket.getMessage(), ticket.getStatus(), ticket.getAdminReply(),
                ticket.getCreatedAt(), ticket.getUpdatedAt(), ticket.getResolvedAt()
        );
    }
}
