package com.example.support.ai;

import com.example.support.ticket.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AiService {
    private final TicketRepository tickets;
    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public AiService(TicketRepository tickets, RestClient.Builder builder, ObjectMapper objectMapper,
                     @Value("${app.ai.base-url}") String baseUrl,
                     @Value("${app.ai.api-key:}") String apiKey,
                     @Value("${app.ai.model:gpt-4o-mini}") String model) {
        this.tickets = tickets; this.objectMapper = objectMapper; this.apiKey = apiKey; this.model = model;
        this.client = builder.baseUrl(baseUrl).build();
    }

    public AiAnalysis analyze(SupportTicket ticket) {
        AiAnalysis result = apiKey.isBlank() ? localAnalysis(ticket) : llmAnalysis(ticket);
        ticket.setCategory(result.category()); ticket.setPriority(result.priority());
        ticket.setSentiment(result.sentiment()); ticket.setSuggestedResponse(result.suggestedResponse());
        tickets.save(ticket);
        return result;
    }

    private AiAnalysis llmAnalysis(SupportTicket ticket) {
        String prompt = """
                Analyze this support ticket. Return only JSON with fields category, priority (LOW, MEDIUM, HIGH, URGENT),
                sentiment, and suggestedResponse. Ticket subject: %s. Description: %s
                """.formatted(ticket.getSubject(), ticket.getDescription());
        Map<?, ?> response = client.post().uri("/v1/chat/completions").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(Map.of("model", model, "temperature", 0.2, "messages",
                        List.of(Map.of("role", "user", "content", prompt))))
                .retrieve().body(Map.class);
        if (response == null) throw new IllegalStateException("AI provider returned an empty response");
        Object message = ((Map<?, ?>) ((List<?>) response.get("choices")).getFirst()).get("message");
        String content = String.valueOf(((Map<?, ?>) message).get("content"));
        try {
            JsonNode json = objectMapper.readTree(content);
            return new AiAnalysis(json.path("category").asText("General"),
                    parsePriority(json.path("priority").asText("MEDIUM")),
                    json.path("sentiment").asText("neutral"),
                    json.path("suggestedResponse").asText("Thanks for contacting support. We are reviewing your request."));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("AI provider returned invalid ticket analysis JSON", ex);
        }
    }

    private AiAnalysis localAnalysis(SupportTicket ticket) {
        String text = (ticket.getSubject() + " " + ticket.getDescription()).toLowerCase();
        TicketPriority priority = text.contains("urgent") || text.contains("outage") ? TicketPriority.URGENT
                : text.contains("error") || text.contains("failed") ? TicketPriority.HIGH : TicketPriority.MEDIUM;
        String category = text.contains("billing") || text.contains("payment") ? "Billing"
                : text.contains("login") || text.contains("password") ? "Account" : "General";
        return new AiAnalysis(category, priority, "neutral",
                "Thanks for contacting support. We have received your ticket and will follow up shortly.");
    }

    private static TicketPriority parsePriority(String value) {
        try { return TicketPriority.valueOf(value.trim().toUpperCase()); }
        catch (IllegalArgumentException ex) { return TicketPriority.MEDIUM; }
    }

}
