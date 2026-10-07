package com.example.support.ticket;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<SupportTicket, Long> {
    Page<SupportTicket> findByOwnerEmailIgnoreCase(String email, Pageable pageable);
    Page<SupportTicket> findByStatus(TicketStatus status, Pageable pageable);
}
