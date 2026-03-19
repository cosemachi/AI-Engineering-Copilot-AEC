package com.aec.infrastructure.ticket;

import com.aec.application.AecException;
import com.aec.application.port.TicketSource;
import com.aec.domain.Ticket;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.nio.file.Path;

@ApplicationScoped
public class JsonTicketSource implements TicketSource {

    private final ObjectMapper objectMapper;

    public JsonTicketSource(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String sourceName() {
        return "json";
    }

    @Override
    public Ticket fetch(String identifier) {
        try {
            return objectMapper.readValue(Path.of(identifier).toFile(), Ticket.class);
        } catch (IOException e) {
            throw new AecException("Failed to load ticket JSON from " + identifier, e);
        }
    }
}
