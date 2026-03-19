package com.aec.infrastructure.ticket;

import com.aec.application.AecException;
import com.aec.application.port.TicketSource;
import com.aec.domain.Ticket;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class JiraTicketSource implements TicketSource {

    @Override
    public String sourceName() {
        return "jira";
    }

    @Override
    public Ticket fetch(String identifier) {
        throw new AecException("Jira source is reserved for future implementation. Use json or github for MVP.");
    }
}
