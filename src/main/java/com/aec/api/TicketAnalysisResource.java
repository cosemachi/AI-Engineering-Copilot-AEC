package com.aec.api;

import com.aec.application.TicketAnalysisService;
import com.aec.application.request.AnalyzeTicketCommand;
import com.aec.domain.TicketAnalysis;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/ticket/analyze")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class TicketAnalysisResource {

    private final TicketAnalysisService ticketAnalysisService;

    public TicketAnalysisResource(TicketAnalysisService ticketAnalysisService) {
        this.ticketAnalysisService = ticketAnalysisService;
    }

    @POST
    public TicketAnalysis analyze(TicketAnalysisRequest request) {
        return ticketAnalysisService.analyze(new AnalyzeTicketCommand(request.source(), request.identifier()));
    }

    public record TicketAnalysisRequest(String source, String identifier) {
    }
}
