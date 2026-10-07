package com.example.support.ticket;

import com.example.support.ai.AiAnalysis;
import com.example.support.ai.AiService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private final TicketService service; private final AiService ai;
    public TicketController(TicketService service, AiService ai) { this.service = service; this.ai = ai; }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TicketView create(Authentication auth, @Valid @RequestBody CreateTicket request) {
        return TicketView.from(service.create(auth.getName(), request.subject(), request.description()));
    }

    @GetMapping
    public Page<TicketView> list(Authentication auth, @RequestParam(required = false) TicketStatus status,
                                 @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        boolean elevated = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_AGENT") || a.getAuthority().equals("ROLE_ADMIN"));
        return service.list(auth.getName(), elevated, status, pageable).map(TicketView::from);
    }

    @GetMapping("/{id}")
    public TicketView get(Authentication auth, @PathVariable Long id) {
        return TicketView.from(service.get(id, auth.getName(), elevated(auth)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketView status(Authentication auth, @PathVariable Long id, @RequestParam TicketStatus status) {
        return TicketView.from(service.status(id, auth.getName(), true, status));
    }

    @PostMapping("/{id}/ai/analyze")
    public AiAnalysis analyze(Authentication auth, @PathVariable Long id) {
        return ai.analyze(service.get(id, auth.getName(), elevated(auth)));
    }

    private boolean elevated(Authentication a) { return a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_AGENT") || x.getAuthority().equals("ROLE_ADMIN")); }
    public record CreateTicket(@NotBlank String subject, @NotBlank String description) {}
    public record TicketView(Long id, String subject, String description, TicketStatus status, TicketPriority priority,
                             String category, String sentiment, String suggestedResponse, String owner, String assignee) {
        static TicketView from(SupportTicket t) { return new TicketView(t.getId(), t.getSubject(), t.getDescription(), t.getStatus(), t.getPriority(), t.getCategory(), t.getSentiment(), t.getSuggestedResponse(), t.getOwner().getEmail(), t.getAssignee() == null ? null : t.getAssignee().getEmail()); }
    }
}
