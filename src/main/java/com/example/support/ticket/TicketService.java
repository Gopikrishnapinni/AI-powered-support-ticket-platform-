package com.example.support.ticket;

import com.example.support.user.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final UserRepository users;

    public TicketService(TicketRepository tickets, UserRepository users) {
        this.tickets = tickets; this.users = users;
    }

    public SupportTicket create(String email, String subject, String description) {
        User owner = user(email);
        SupportTicket ticket = new SupportTicket();
        ticket.setOwner(owner); ticket.setSubject(subject); ticket.setDescription(description);
        return tickets.save(ticket);
    }

    public Page<SupportTicket> list(String email, boolean elevated, TicketStatus status, Pageable pageable) {
        if (status != null && elevated) return tickets.findByStatus(status, pageable);
        return tickets.findByOwnerEmailIgnoreCase(email, pageable);
    }

    public SupportTicket get(Long id, String email, boolean elevated) {
        SupportTicket ticket = tickets.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found"));
        if (!elevated && !ticket.getOwner().getEmail().equalsIgnoreCase(email)) throw new AccessDeniedException("Ticket access denied");
        return ticket;
    }

    public SupportTicket status(Long id, String email, boolean elevated, TicketStatus status) {
        SupportTicket ticket = get(id, email, elevated);
        ticket.setStatus(status); return tickets.save(ticket);
    }

    private User user(String email) {
        return users.findByEmailIgnoreCase(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
