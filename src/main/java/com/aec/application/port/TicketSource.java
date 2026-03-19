package com.aec.application.port;

import com.aec.domain.Ticket;

public interface TicketSource {
    String sourceName();
    Ticket fetch(String identifier);
}
