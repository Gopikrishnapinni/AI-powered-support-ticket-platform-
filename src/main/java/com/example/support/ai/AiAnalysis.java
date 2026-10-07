package com.example.support.ai;

import com.example.support.ticket.*;

public record AiAnalysis(String category, TicketPriority priority, String sentiment, String suggestedResponse) {
    public static AiAnalysis from(SupportTicket ticket) {
        return new AiAnalysis(ticket.getCategory(), ticket.getPriority(), ticket.getSentiment(), ticket.getSuggestedResponse());
    }
}
