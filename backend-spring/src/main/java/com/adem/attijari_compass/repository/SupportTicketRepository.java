package com.adem.attijari_compass.repository;

import com.adem.attijari_compass.entity.SupportTicket;
import com.adem.attijari_compass.entity.SupportTicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    List<SupportTicket> findAllByOrderByCreatedAtDesc();
    long countByStatus(SupportTicketStatus status);
}
