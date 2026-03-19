package com.aec.application;

import com.aec.application.port.LlmProvider;
import com.aec.application.port.TicketSource;
import com.aec.application.request.AnalyzeTicketCommand;
import com.aec.domain.Ticket;
import com.aec.domain.TicketAnalysis;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class TicketAnalysisService {

    private static final Logger LOG = Logger.getLogger(TicketAnalysisService.class);

    private final ProviderRegistry providerRegistry;
    private final String llmProviderName;

    public TicketAnalysisService(
            ProviderRegistry providerRegistry,
            @ConfigProperty(name = "aec.ai.provider") String llmProviderName) {
        this.providerRegistry = providerRegistry;
        this.llmProviderName = llmProviderName;
    }

    public TicketAnalysis analyze(AnalyzeTicketCommand command) {
        TicketSource source = providerRegistry.ticketSource(command.source());
        Ticket ticket = source.fetch(command.identifier());
        LlmProvider provider = providerRegistry.llmProvider(llmProviderName);
        LOG.infof("Analyzing ticket %s from source %s with provider %s",
                ticket.id(), source.sourceName(), provider.providerName());
        TicketAnalysis analysis = provider.analyzeTicket(ticket);
        LOG.infof("Ticket analysis completed for %s: %d risks, %d dependencies",
                ticket.id(), analysis.risks().size(), analysis.dependencies().size());
        return analysis;
    }
}
